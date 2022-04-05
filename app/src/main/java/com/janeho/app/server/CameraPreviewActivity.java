package com.janeho.app.server;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.janeho.app.ui.R;


/**
 * Camera Preview Activity
 * control preview screen and overlays
 */
public class CameraPreviewActivity extends AppCompatActivity {

    private static final int MY_CAMERA_PERMISSION_CODE = 1000;
    private Camera mCamera;
    private CameraView camView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Full Screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Fix orientation : portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Set layout
        setContentView(R.layout.activity_camera_preview);

        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
        }
        else {
            // Initialize Camera
            mCamera = getCameraInstance();
            // Set-up preview screen
            if(mCamera != null) {
                // Create camera preview
                camView = new CameraView(this, mCamera);
                // Add view to UI
                FrameLayout preview = findViewById(R.id.frm_preview);
                preview.addView(camView);
            }
        }

    }

    /** Post-process for granted permissions */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                // Initialize Camera
                mCamera = getCameraInstance();
                // Set-up preview screen
                if(mCamera != null) {
                    // Create camera preview
                    camView = new CameraView(this, mCamera);

                    // Add view to UI
                    FrameLayout preview = findViewById(R.id.frm_preview);
                    preview.addView(camView);
                }
            } else {
                // Rejected
                Toast.makeText(this, "Camera Permission is not granted!", Toast.LENGTH_SHORT).show();
            }
            return;
        }

    }

    @Override
    protected void onDestroy() {
        try{
            if(mCamera != null) mCamera.release();
        }catch (Exception e){
            e.printStackTrace();
        }
        super.onDestroy();
    }

    /** Get facing back camera instance */
    public static Camera getCameraInstance()
    {
        int camId = -1;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); ++i) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                camId = i;
                break;
            }
        }

        if(camId == -1) return null;

        Camera c=null;
        try{
            c=Camera.open(camId);
        }catch(Exception e){
            e.printStackTrace();
        }
        return c;
    }


}
