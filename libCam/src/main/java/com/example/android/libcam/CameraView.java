package com.example.android.libcam;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/** A basic Camera preview class */
public class CameraView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CAMERAVIEW";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    // Camera configuration values
    public static final int PREVIEW_WIDTH = 720;
    public static final int PREVIEW_HEIGHT = 1280;
    public static final int SCREEN_ORIENTATION = 0;
    // Preview display parameters (by portrait mode)
    private Camera.Size mPreviewSize = null;

    public CameraView(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.setDisplayOrientation(SCREEN_ORIENTATION);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    /** Calculate preview size to fit output screen */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        double originalWidth = MeasureSpec.getSize(widthMeasureSpec);
        double originalHeight = MeasureSpec.getSize(heightMeasureSpec);

        // Switch width and height size for portrait preview screen.
        // Because the camera stream size always assume landscape size.
        int DISPLAY_WIDTH = PREVIEW_HEIGHT;
        int DISPLAY_HEIGHT = PREVIEW_WIDTH;
        if(mPreviewSize != null) {
            DISPLAY_WIDTH = mPreviewSize.height;
            DISPLAY_HEIGHT = mPreviewSize.width;
        }

        // Consider calculated size is overflow
        int calculatedHeight = (int)(originalWidth * DISPLAY_HEIGHT / DISPLAY_WIDTH);
        int finalWidth, finalHeight;
        if (calculatedHeight > originalHeight) {
            finalWidth = (int)(originalHeight * DISPLAY_WIDTH / DISPLAY_HEIGHT);
            finalHeight = (int) originalHeight;
        } else {
            finalWidth = (int) originalWidth;
            finalHeight = calculatedHeight;
        }

        // Set new measures
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(finalWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY));
    }
}



///** Camera preview screen for portrait mode */
//public class CameraView  extends SurfaceView implements SurfaceHolder.Callback{
//    // Camera configuration values
//    public static final int PREVIEW_WIDTH = 720;
//    public static final int PREVIEW_HEIGHT = 1280;
//    public static final int SCREEN_ORIENTATION = 0;
//
//    // Preview display parameters (by portrait mode)
//    private Camera.Size mPreviewSize = null;
//
//    // Instances
//    private Camera mCamera;
//    private Camera.PreviewCallback mPreviewCallback;
//
//    public CameraView(Context context, Camera camera){
//        super(context);
//        mCamera=camera;
//        SurfaceHolder holder=getHolder();
//        holder.addCallback(this);
//        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//    }
//
//    public void setPreviewCallback(Camera.PreviewCallback previewCallback) {
//        mPreviewCallback=previewCallback;
//    }
//
//    public Camera.Size getPreviewSize() {
//        return mPreviewSize;
//    }
//
//    /** Calculate preview size to fit output screen */
//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        double originalWidth = MeasureSpec.getSize(widthMeasureSpec);
//        double originalHeight = MeasureSpec.getSize(heightMeasureSpec);
//
//        // Switch width and height size for portrait preview screen.
//        // Because the camera stream size always assume landscape size.
//        int DISPLAY_WIDTH = PREVIEW_HEIGHT;
//        int DISPLAY_HEIGHT = PREVIEW_WIDTH;
//        if(mPreviewSize != null) {
//            DISPLAY_WIDTH = mPreviewSize.height;
//            DISPLAY_HEIGHT = mPreviewSize.width;
//        }
//
//        // Consider calculated size is overflow
//        int calculatedHeight = (int)(originalWidth * DISPLAY_HEIGHT / DISPLAY_WIDTH);
//        int finalWidth, finalHeight;
//        if (calculatedHeight > originalHeight) {
//            finalWidth = (int)(originalHeight * DISPLAY_WIDTH / DISPLAY_HEIGHT);
//            finalHeight = (int) originalHeight;
//        } else {
//            finalWidth = (int) originalWidth;
//            finalHeight = calculatedHeight;
//        }
//
//        // Set new measures
//        super.onMeasure(
//                MeasureSpec.makeMeasureSpec(finalWidth, MeasureSpec.EXACTLY),
//                MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY));
//    }
//
//    @Override
//    public void surfaceCreated(SurfaceHolder holder) {
//        try {
//            mCamera.setPreviewDisplay(holder);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void surfaceDestroyed(SurfaceHolder holder) {
//        try{
//            mCamera.stopPreview();
//            mCamera.setPreviewCallback(null);
//            mCamera.setPreviewDisplay(null);
//            mCamera=null;
//        } catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//
//
//    @Override
//    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
//        try{
//            mCamera.stopPreview();
//        } catch (Exception e){
//            e.printStackTrace();
//        }
//        try{
//            // Start set-up camera configurations
//            Camera.Parameters parameters = mCamera.getParameters();
//
//            // Set image format
//            parameters.setPreviewFormat(ImageFormat.NV21);
//
//            // Set preview size (find suitable size with configurations)
//            mPreviewSize = findSuitablePreviewSize(parameters.getSupportedPreviewSizes());
//            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
//
//            // Set Auto-Focusing if is available.
//            if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
//                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//            }
//
//            // Adapt parameters
//            mCamera.setParameters(parameters);
//
//            // Set Screen-Mode portrait
//            mCamera.setDisplayOrientation(SCREEN_ORIENTATION);
//
//            // Set preview callback
//            // When the preview updated, 'onPreviewFrame()' function is called.
//            mCamera.setPreviewCallback(mPreviewCallback);
//
//            // Show preview images
//            mCamera.startPreview();
//        }catch(Exception e){
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Find suitable preview scren size
//     * If the value of PREVIEW_WIDTH and PREVIEW_HEIGHT are not supported on chosen camera,
//     * find a size value that the most similar size and ratio.
//     */
//    private Camera.Size findSuitablePreviewSize(List<Camera.Size> supportedPreviewSize) {
//        Camera.Size previewSize = null;
//
//        double originalAspectRatio = (double)PREVIEW_WIDTH / (double)PREVIEW_HEIGHT;
//        double lastFit = Double.MAX_VALUE, currentFit;
//        for(Camera.Size s : supportedPreviewSize){
//            if(s.width==PREVIEW_WIDTH && s.height==PREVIEW_HEIGHT){
//                previewSize = s;
//                break;
//            } else if(previewSize == null) {
//                lastFit = Math.abs( ((double)s.width / (double)s.height) - originalAspectRatio);
//                previewSize = s;
//            } else {
//                currentFit = Math.abs( ((double)s.width / (double)s.height) - originalAspectRatio);
//                if( (currentFit <= lastFit) && (Math.abs(PREVIEW_WIDTH-s.width)<=Math.abs(PREVIEW_WIDTH-previewSize.width)) ) {
//                    previewSize = s;
//                    lastFit = currentFit;
//                }
//            }
//        }
//
//        return previewSize;
//    }
//
//}