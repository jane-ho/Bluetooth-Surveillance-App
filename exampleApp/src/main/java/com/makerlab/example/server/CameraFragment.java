package com.makerlab.example.server;

import static android.content.Context.WIFI_SERVICE;

import android.hardware.Camera;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.android.libcam.R;

public class CameraFragment extends Fragment {
    private static final int MY_CAMERA_PERMISSION_CODE = 1000;
    private Camera mCamera;
    private CameraView mPreview;

    private CameraManager mCameraManager;
    private boolean mIsOn = true;
    private SocketServer mThread;
    private Button mButton;
    private String mIP;
    private int mPort = 8888;

    public CameraFragment() {
        // Required empty public constructor
    }

    public static CameraFragment newInstance() {
        return new CameraFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_camera, container, false);
        View inflatedView = inflater.inflate(R.layout.fragment_camera, container, false);
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

        return inflatedView;
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
