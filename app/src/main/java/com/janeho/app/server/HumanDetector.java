package com.janeho.app.server;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.NonNull;

import org.opencv.core.Rect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// OpenCV
public class HumanDetector extends Thread  {
    private static final String TAG = "HumanDetector";
    private SurfaceHolder mHolder;
    private Paint paint = new Paint();
    private int [] pathColorList = {Color.RED, Color.GREEN, Color.CYAN, Color.BLUE};
    CameraView mPreview;
    CameraFragment mFragment;
    Thread mHdThread;

    public HumanDetector (CameraView preview, CameraFragment frag){
        this.mFragment = frag;
        this.mPreview = preview;
    }

    @Override
    public void run() {
        super.run();

        // HOG OpenCV
        HumanDetection hd = new HumanDetection();
        hd.setListener(mFragment);
        while (!this.isInterrupted()){
            hd.detect(mPreview.getImageBuffer(), mPreview.getPreviewHeight(),mPreview.getPreviewWidth());
        }
        Log.d(TAG, "interrupted");
    }



//public class HumanDetector extends SurfaceView implements SurfaceHolder.Callback  {
//    private static final String TAG = "HumanDetector";
//    private SurfaceHolder mHolder;
//    private Paint paint = new Paint();
//    private int [] pathColorList = {Color.RED, Color.GREEN, Color.CYAN, Color.BLUE};
//    CameraView mPreview;
////    CameraFragment mFragment;
//    Thread mHdThread;
//
//    public HumanDetector (Context context, CameraView preview, CameraFragment frag){
//        super(context);
//
//        mHolder = getHolder();
//        mHolder.addCallback(this);
//
////        this.mFragment = frag;
//        this.mPreview = preview;
//        mHdThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                // HOG OpenCV
//                HumanDetection hd = new HumanDetection();
//                hd.setListener(HumanDetector.this);
//                while (!Thread.currentThread().isInterrupted()){
//                    hd.detect(mPreview.getImageBuffer(), mPreview.getPreviewHeight(),mPreview.getPreviewWidth());
//                }
//            }
//        });
//        mHdThread.run();
//    }
//
////    @Override
////    public void run() {
////        super.run();
////
////        // HOG OpenCV
////        HumanDetection hd = new HumanDetection();
////        hd.setListener(mFragment);
////        while (!this.isInterrupted()){
////            hd.detect(mPreview.getImageBuffer(), mPreview.getPreviewHeight(),mPreview.getPreviewWidth());
////        }
////    }
//
//    @Override
//    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
//        //Make surfaceView transparent
//        surfaceHolder.setFormat(PixelFormat.TRANSPARENT);
//    }
//
//    @Override
//    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int format, int w, int h) {
//
//    }
//
//    @Override
//    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
//
//    }
//
//    public void draw(Rect[] found){
//        //Get canvas via surfaceHolder(Even when the screen is not active, it will be drawn and exception may oc
//        Canvas canvas = mHolder.lockCanvas();
//        if (canvas != null) {
//            //Clear what was previously drawn
//            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
//
//            for (int i = 0; i < found.length; i++) {
//                Rect rect = found[i];
//                //Bounding box display
//                paint.setColor(pathColorList[i]);
//                paint.setStyle(Paint.Style.STROKE);
//                paint.setStrokeWidth(7f);
//                paint.setAntiAlias(false);
//                canvas.drawRect(rect.x, rect.y, rect.width, rect.height, paint);
//            }
//            if (canvas != null) {
//                mHolder.unlockCanvasAndPost(canvas);
//            } else
//                return;
//        }
//    }
//
//    public void onPause() throws InterruptedException {
//        if (mHdThread != null) {
//            mHdThread.interrupt();
//            mHdThread.join();
//        }
////        resetBuff();
//    }
//
//    public void onResume(){
//        if (mHdThread != null) {
//            mHdThread.run();
//        }
//    }




}
