package com.janeho.app.server;

import static android.content.Context.POWER_SERVICE;
import static android.content.Context.WIFI_SERVICE;

import android.hardware.Camera;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.janeho.app.ui.R;
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

    private ImageButton button_switch;
    private boolean isFrontCamera = false;
    private boolean withControl;

    private HumanDetector mHumanDetector;   // OpenCV
    public LinearLayout alertLayout;
    private Switch switch_hd;

    public CameraFragment() {
        // Required empty public constructor
        this.withControl = true;
    }

    public CameraFragment(boolean withControl) {
        // Required empty public constructor
        this.withControl = withControl;
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
                            mThread = new SocketServer(mPreview, mPort, CameraFragment.this, withControl, isFrontCamera);
                            Log.d(TAG, "started a server thread");
                            mIsOn = false;
                            mButton.setText("Stop");

                            wakeLock.acquire();
                            button_switch.setEnabled(false);

                            // OpenCV
                            if (switch_hd.isChecked()) {
                                mHumanDetector = new HumanDetector(mPreview, CameraFragment.this);
                                mHumanDetector.start();
                            }
                            switch_hd.setEnabled(false);

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
        isFrontCamera = false;
        mPreview = new CameraView(getContext(), mCameraManager.getCamera(isFrontCamera));
        FrameLayout container_preview = (FrameLayout) inflatedView.findViewById(R.id.container_preview);
        container_preview.addView(mPreview);

        // switch camera
        button_switch = inflatedView.findViewById(R.id.button_switchcam);
        button_switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPreview.onPause();
                mCameraManager.onPause();
                isFrontCamera = !isFrontCamera;
                mPreview.setCamera(mCameraManager.getCamera(isFrontCamera));
                mCameraManager.onResume();
                mPreview.onResume();
            }
        });
        if (mPreview != null)
            button_switch.setVisibility(View.VISIBLE);

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

        // OpenCV
        alertLayout = inflatedView.findViewById(R.id.alertLayout);
        switch_hd = inflatedView.findViewById(R.id.switch_hd);

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
        button_switch.setEnabled(true);
        if (mHumanDetector != null){
            mHumanDetector.interrupt();
        }
        // OpenCV
        alertLayout.setVisibility(View.INVISIBLE);
        switch_hd.setEnabled(true);

    }

    private void closeSocketServer() {
        if (mThread == null)
            return;

        mThread.interrupt();
        try {
            mThread.join();
            Log.d(TAG, "ended a server thread");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mThread = null;
        wakeLock.release();
    }

    @Override
    public void onStart() {
        super.onStart();
        mPreview.setCamera(mCameraManager.getCamera(false));
        mCameraManager.onResume();
        isFrontCamera =false;
        mPreview.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        closeSocketServer();
        mPreview.onPause();
        mCameraManager.onPause();              // release the camera immediately on pause event
        reset();
        // OpenCV
        if (mHumanDetector!=null) {
//            try {
//                mHumanDetector.onPause();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            mHumanDetector.interrupt();
        }
    }

    // OpenCV
    public void onDetected(){
        Log.d(TAG, "onDetected: ");
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    alertLayout.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(), "Suspicious!", Toast.LENGTH_SHORT).show();
//                    mThread.sendBroadcast("Suspicious!");
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
    // OpenCV
    public void onUndoDetected(){
        if (alertLayout.getVisibility() == View.VISIBLE) {
            Log.d(TAG, "onUndoDetected: ");
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    alertLayout.setVisibility(View.INVISIBLE);
                }
            });
        }
    }

}
