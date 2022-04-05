package com.janeho.app.server;

import android.util.Log;

// OpenCV
public class HumanDetector extends Thread  {
    private static final String TAG = "HumanDetector";
    CameraView mPreview;
    CameraFragment mFragment;

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

}
