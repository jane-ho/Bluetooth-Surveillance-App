package com.example.android.libcam;

import static com.example.android.libcam.CameraPreviewActivity.getCameraInstance;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class CameraFragment extends Fragment {
    private static final int MY_CAMERA_PERMISSION_CODE = 1000;
    private Camera mCamera;
    private CameraView mPreview;

    public CameraFragment() {
        // Required empty public constructor
    }

    public static CameraFragment newInstance(String param1, String param2) {
        CameraFragment fragment = new CameraFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_camera, container, false);
        mCamera = getCameraInstance();
        if(mCamera != null) {
            mPreview = new CameraView(getContext(), mCamera);
        } else mPreview = null;
        return mPreview;
    }
    public static CameraFragment newInstance() {
        return new CameraFragment();
    }

    public void releaseCamera(){
        mCamera.release();
    }

}
