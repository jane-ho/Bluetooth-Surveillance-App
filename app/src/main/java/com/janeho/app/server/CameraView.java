package com.janeho.app.server;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

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


        // start preview with new settings
        try {
            mCamera.setPreviewCallback(mPreviewCallback);
            Camera.Parameters params = mCamera.getParameters();
            params.setPreviewSize(640, 480); // set preview size. smaller is better
            mCamera.setParameters(params);
            mCamera.setDisplayOrientation(SCREEN_ORIENTATION);
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
        surfaceChanged(mHolder, PixelFormat.RGB_888,mPreviewSize.width,mPreviewSize.height);
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
            try {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
            } catch (RuntimeException e){
                e.printStackTrace();
            }
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


}