package com.makerlab.example.server;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/** A basic Camera preview class */
public class CameraView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CAMERAVIEW";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    // Camera configuration values
    public static final int PREVIEW_WIDTH = 720;
    public static final int PREVIEW_HEIGHT = 1280;
    public int SCREEN_ORIENTATION = 90;
    // Preview display parameters (by portrait mode)
    private Camera.Size mPreviewSize = null;
    //
    private byte[] mImageData;
    private LinkedList<byte[]> mQueue = new LinkedList<byte[]>();
    private static final int MAX_BUFFER = 15;
    private byte[] mLastFrame = null;
    private int mFrameLength = 256;

    public CameraView(Context context, Camera camera) {
        super(context);
        mCamera = camera;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        for (Camera.Size s : sizes) {
            Log.i(TAG, "preview size = " + s.width + ", " + s.height);
        }

        params.setPreviewSize(640, 480); // set preview size. smaller is better
        mCamera.setParameters(params);

        mPreviewSize = mCamera.getParameters().getPreviewSize();
        Log.i(TAG, "preview size = " + mPreviewSize.width + ", " + mPreviewSize.height);

        int format = mCamera.getParameters().getPreviewFormat();
        mFrameLength = mPreviewSize.width * mPreviewSize.height * ImageFormat.getBitsPerPixel(format) / 8;
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
        mCamera.release();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        mHolder.removeCallback(this); // unregister from old SurfaceHolder
        mHolder.addCallback(this); // register to new holder

        // stop preview before making changes
        try {
            mCamera.stopPreview();
            resetBuff();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

//        if (orientation > -1)
//            SCREEN_ORIENTATION = orientation;

        // start preview with new settings
        try {
            mCamera.setPreviewCallback(mPreviewCallback);
//            mCamera.setDisplayOrientation(SCREEN_ORIENTATION);
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
        int calculatedHeight = (int)(originalWidth/2 * DISPLAY_HEIGHT / DISPLAY_WIDTH);
        int finalWidth, finalHeight;
        if (calculatedHeight > originalHeight/2) {
            finalWidth = (int)(originalHeight/2 * DISPLAY_WIDTH / DISPLAY_HEIGHT);
            finalHeight = (int) originalHeight/2;
        } else {
            finalWidth = (int) originalWidth/2;
            finalHeight = calculatedHeight;
        }

        // Set new measures
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(finalWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY));
    }

    public int getPreviewLength() {
        return mFrameLength;
    }

    public int getPreviewWidth() {
        return mPreviewSize.width;
    }

    public int getPreviewHeight() {
        return mPreviewSize.height;
    }

    public int getPreviewOrientation() { return SCREEN_ORIENTATION; }

    public void setCamera(Camera camera) {
        mCamera = camera;
    }

    public void rotateCamera(int orientation) {
        this.surfaceChanged(mHolder, orientation, PREVIEW_WIDTH, PREVIEW_HEIGHT);
    }

    public byte[] getImageBuffer() {
        synchronized (mQueue) {
            if (mQueue.size() > 0) {
                mLastFrame = mQueue.poll();
            }
        }

        return mLastFrame;
    }

    private void resetBuff() {

        synchronized (mQueue) {
            mQueue.clear();
            mLastFrame = null;
        }
    }

    public void onPause() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
        }
        resetBuff();
    }

    public void onResume(){
        if (mCamera != null) {
            mCamera.setPreviewCallback(mPreviewCallback);
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.setDisplayOrientation(SCREEN_ORIENTATION);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.startPreview();
        }
    }

    private Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            synchronized (mQueue) {
                if (mQueue.size() == MAX_BUFFER) {
                    mQueue.poll();
                }
                mQueue.add(data);
            }
        }
    };

    private void saveYUV(byte[] byteArray) {

        YuvImage im = new YuvImage(byteArray, ImageFormat.NV21, mPreviewSize.width, mPreviewSize.height, null);
        Rect r = new Rect(0, 0, mPreviewSize.width, mPreviewSize.height);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        im.compressToJpeg(r, 100, baos);

        try {
            FileOutputStream output = new FileOutputStream(Environment.getExternalStorageDirectory() + "/yuv.jpg");
            output.write(baos.toByteArray());
            output.flush();
            output.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }

    private void saveRAW(byte[] byteArray) {
        try {
            FileOutputStream file = new FileOutputStream(new File(Environment.getExternalStorageDirectory() + "/test.yuv"));
            try {
                file.write(mImageData);
                file.flush();
                file.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
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