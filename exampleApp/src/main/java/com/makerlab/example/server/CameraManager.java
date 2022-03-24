package com.makerlab.example.server;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.widget.Toast;

public class CameraManager {
    private Camera mCamera;
    private Context mContext;


    public CameraManager(Context context) {     // TODO: check camera permission
        mContext = context;
//        mCamera = getCameraInstance();
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

    public int[] getPreviewSize(){
        return new int[]{mCamera.getParameters().getPreviewSize().width, mCamera.getParameters().getPreviewSize().height};
    }

    /** A safe way to get an instance of the Camera object. */
    private static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private Camera getFrontCameraInstance(){
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for ( int camIdx = 0; camIdx < cameraCount; camIdx++ ) {
            Camera.getCameraInfo( camIdx, cameraInfo );
            if ( cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT  ) {
                try {
                    cam = Camera.open( camIdx );
                    break;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }

        return cam;
    }


}