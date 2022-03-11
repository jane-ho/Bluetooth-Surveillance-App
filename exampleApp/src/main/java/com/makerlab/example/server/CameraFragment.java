package com.makerlab.example.server;

import static android.content.Context.POWER_SERVICE;
import static android.content.Context.SENSOR_SERVICE;
import static android.content.Context.WIFI_SERVICE;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.example.android.libcam.R;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class CameraFragment extends Fragment {
    private static final String TAG = "CameraFragment";
    private static final int MY_CAMERA_PERMISSION_CODE = 1000;
    private Camera mCamera;
    private CameraView mPreview;

    private CameraManager mCameraManager;
    private boolean mIsOn = true;
    private SocketServer mThread;
    private Button mButton;
    private String mIP;
    private int mPort = 8888;

    PowerManager.WakeLock wakeLock;

    public CameraFragment() {
        // Required empty public constructor
    }

    public static CameraFragment newInstance() {
        return new CameraFragment();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_camera, container, false);
        View inflatedView = inflater.inflate(R.layout.fragment_camera, container, false);

        // WakeLock
        PowerManager pm = (PowerManager) getActivity().getApplicationContext().getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "myapp:camwakelock");

        mButton = (Button) inflatedView.findViewById(R.id.button_start);
        mButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        if (mIsOn) {
                            mThread = new SocketServer(mPreview, mPort, CameraFragment.this);

                            mIsOn = false;
                            mButton.setText("Stop");

                            wakeLock.acquire();
                        } else {
                            closeSocketServer();
                            reset();
                        }
                    }
                }
        );
        TextView textview_info = inflatedView.findViewById(R.id.textview_info);
        WifiManager wm = (WifiManager) getContext().getApplicationContext().getSystemService(WIFI_SERVICE);
        mIP = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        textview_info.setText(mIP+":"+Integer.toString(mPort));

        mCameraManager = new CameraManager(getContext());
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraView(getContext(), mCameraManager.getCamera());
        FrameLayout container_preview = (FrameLayout) inflatedView.findViewById(R.id.container_preview);
        container_preview.addView(mPreview);

//        // detect screen rotation
//        SensorEventListener m_sensorEventListener = new SensorEventListener() {
//            @Override
//            public void onSensorChanged(SensorEvent event) {
//                if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//                    // Portrait
//                    mPreview.rotateCamera(90);
//                    Log.d(TAG, "onSensorChanged: rotate camera 90");
//                }
//                else {
//                    // Landscape
//                    mPreview.rotateCamera(0);
//                    Log.d(TAG, "onSensorChanged: rotate camera 0");
//                }
//            }
//            @Override
//            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
//        };
//        SensorManager sm = (SensorManager) getActivity().getApplicationContext().getSystemService(SENSOR_SERVICE);
//        sm.registerListener(m_sensorEventListener, sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_NORMAL);

        return inflatedView;
    }

    private void showPublicIPAddress(TextView textview){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                    try{
                        String urlstr = "https://api.ipify.org/?format=json";
                        URL url = new URL(urlstr);
                        URLConnection request = url.openConnection();
                        request.connect();
                        JsonParser jp = new JsonParser(); //from gson
                        JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent())); //Convert the input stream to a json element
                        JsonObject obj = root.getAsJsonObject();
                        JsonElement element = obj.get("ip");
                        mIP = element.getAsString();
                    } catch (IOException e) {
                        WifiManager wm = (WifiManager) getContext().getApplicationContext().getSystemService(WIFI_SERVICE);
                        mIP = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
                    } finally {
                        textview.post(new Runnable() {
                            @Override
                            public void run() {
                                textview.setText(mIP+":"+Integer.toString(mPort));
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public void releaseCamera(){
//        mCamera.release();
//        mCameraManager.onPause();
    }

    private void reset() {
        mButton.setText("Start");
        mIsOn = true;
    }

    private void closeSocketServer() {
        if (mThread == null)
            return;

        mThread.interrupt();
        try {
            mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mThread = null;
        wakeLock.release();
    }

    @Override
    public void onStart() {
        super.onStart();
        mCameraManager.onResume();
        mPreview.setCamera(mCameraManager.getCamera());
        mPreview.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        closeSocketServer();
        mPreview.onPause();
        mCameraManager.onPause();              // release the camera immediately on pause event
        reset();
    }



}
