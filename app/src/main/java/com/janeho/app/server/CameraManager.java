package com.janeho.app.server;

import android.content.Context;
import android.hardware.Camera;

public class CameraManager {
    private Camera mCamera;
    private Context mContext;


    public CameraManager(Context context) {
        mContext = context;
    }


    public Camera getCamera(boolean isFront) {
        if (mCamera!=null)
            releaseCamera();
        try {
            mCamera = Camera.open(isFront? 1:0);
        } catch (RuntimeException e){
            e.printStackTrace();
        }
        return mCamera;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release(); // release the camera for other applications
            mCamera = null;
        }
    }

    public void onPause() {
        releaseCamera();
    }

    public void onResume() {
        if (mCamera == null) {
            mCamera = getCamera(false);
        }

//        Toast.makeText(mContext, "preview size = " + mCamera.getParameters().getPreviewSize().width +
//                ", " + mCamera.getParameters().getPreviewSize().height, Toast.LENGTH_LONG).show();
    }

}