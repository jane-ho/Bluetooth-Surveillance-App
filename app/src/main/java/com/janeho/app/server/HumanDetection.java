package com.janeho.app.server;

import android.graphics.Camera;
import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.HOGDescriptor;

// OpenCV
public class HumanDetection {

    final String TAG = this.getClass().getSimpleName();
    HOGDescriptor hog;
    float ratio = 0.5F;
    CameraFragment parent;
    Rect[] rects;

    public HumanDetection() {
//        Mat img;
        if (OpenCVLoader.initDebug()) {
            hog = new HOGDescriptor();
            hog.setSVMDetector(HOGDescriptor.getDefaultPeopleDetector());
        }
    }

    public void detect(byte[] bArray,int h,int w) {
        if (hog != null) {

//            Log.d(TAG, "detect: dectecting... ;"+h+";"+w);
            
            // Create the OpenCV cv::Mat.
            Mat m1 = new Mat(h, w, CvType.CV_8UC4);

            // Initialise the matrix m1 with content from bArray.
            m1.put(0, 0, bArray);
            // Prepare the grayscale matrix.
            Mat m3 = new Mat(h, w, CvType.CV_8UC1);
            Imgproc.cvtColor(m1, m3, Imgproc.COLOR_BGRA2GRAY);

            MatOfRect found = new MatOfRect();
            MatOfDouble weight = new MatOfDouble();

            hog.detectMultiScale(m3, found, weight, 0, new Size(4, 4), new Size(16, 16),
                    1.05, 2, false);

//            Rect[] rects = found.toArray();
            rects = found.toArray();
            if (rects.length > 0) {
                Log.d(TAG, "HumanDetection detect: detected");
                alertPositive();

//            for (int i=0; i<rects.length; i++) {
//                new Rect(Float.floatToIntBits(rects[i].x/ratio), Float.floatToIntBits(rects[i].y/ratio),
//                        Float.floatToIntBits(rects[i].width/ratio), Float.floatToIntBits(rects[i].height/ratio));
//            }
            }
            else {
                undoPositive();
            }
//        text("Frame Rate: " + round(frameRate), 500, 50);
        }
    }

    public void setListener (CameraFragment parent) {
        this.parent = parent;
    }

    private void alertPositive(){
        if (parent!=null){
            parent.onDetected();
//            parent.draw(rects);
        }
    }
    private void undoPositive(){
        if (parent!=null){
            parent.onUndoDetected();
        }
    }

}
