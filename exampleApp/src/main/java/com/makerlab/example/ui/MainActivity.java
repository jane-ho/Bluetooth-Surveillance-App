package com.makerlab.example.ui;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.android.libcam.CameraFragment;
import com.example.android.libcam.CameraPreviewActivity;
import com.example.android.libcam.CameraView;
import com.makerlab.bt.BluetoothConnect;
import com.makerlab.bt.BluetoothScan;
import com.makerlab.ui.BluetoothDevListActivity;

public class MainActivity extends AppCompatActivity implements
        BluetoothConnect.ConnectionHandler {
    static public final boolean D = BuildConfig.DEBUG;
    static public final int REQUEST_BT_GET_DEVICE = 1112;
    static public final String BLUETOOT_REMOTE_DEVICE = "bt_remote_device";
    private static final int MY_CAMERA_PERMISSION_CODE = 1000;
    static private String LOG_TAG = MainActivity.class.getSimpleName();

    private BluetoothConnect mBluetoothConnect;
    private BluetoothScan mBluetoothScan;

    private Menu mMenuSetting;
    private SharedPreferences mSharedPref;
    private String mSharedPrefFile = "com.makerlab.omni.sharedprefs";
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBluetoothConnect = new BluetoothConnect(this);
        mBluetoothConnect.setConnectionHandler(this);

        mSharedPref = getSharedPreferences(mSharedPrefFile, MODE_PRIVATE);
        String bluetothDeviceAddr = mSharedPref.getString(BLUETOOT_REMOTE_DEVICE, null);
        if (bluetothDeviceAddr != null) {
            //Log.e(LOG_TAG, "onCreate(): found share perference");
            mBluetoothScan = new BluetoothScan(this);
            BluetoothDevice mBluetoothDevice = mBluetoothScan.getBluetoothDevice(bluetothDeviceAddr);
            mBluetoothConnect.connectBluetooth(mBluetoothDevice);
            if (D)
                Log.e(LOG_TAG, "onCreate() - connecting bluetooth device");
        } else {
            if (D)
                Log.e(LOG_TAG, "onCreate()");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenuSetting = menu;
        getMenuInflater().inflate(R.menu.menu_main, mMenuSetting);
        if (mBluetoothConnect.isConnected()) {
            MenuItem menuItem = mMenuSetting.findItem(R.id.action_bluetooth_scan);
            menuItem.setEnabled(false);
            menuItem = mMenuSetting.findItem(R.id.action_bluetooth_disconnect);
            menuItem.setEnabled(true);
        }
        return true;
    }

    private void enableConnectMenuItem(boolean flag) {
        MenuItem menuItem = mMenuSetting.findItem(R.id.action_bluetooth_scan);
        menuItem.setEnabled(flag);
        menuItem = mMenuSetting.findItem(R.id.action_bluetooth_disconnect);
        menuItem.setEnabled(!flag);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_bluetooth_scan) {
            if (mBluetoothConnect.isConnected()) {
                mBluetoothConnect.disconnectBluetooth();
            }
            Intent intent = new Intent(this, BluetoothDevListActivity.class);
            startActivityForResult(intent, REQUEST_BT_GET_DEVICE);
            return true;
        }

        if (item.getItemId() == R.id.action_bluetooth_disconnect) {
            mBluetoothConnect.disconnectBluetooth();
            closeControlFragment();
            closeCamFragment();
            enableConnectMenuItem(true);
            removeSharePerf();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);

        if (requestCode == REQUEST_BT_GET_DEVICE) {
            if (resultCode == RESULT_OK) {
                BluetoothDevice bluetoothDevice = resultIntent.getParcelableExtra(BluetoothDevListActivity.EXTRA_KEY_DEVICE);
                if (bluetoothDevice != null) {
                    mBluetoothConnect.connectBluetooth(bluetoothDevice);
                    if (D)
                        Log.e(LOG_TAG, "onActivityResult() - connecting");
                }
            } else if (resultCode == RESULT_CANCELED) {
                if (D)
                    Log.e(LOG_TAG, "onActivityResult() - canceled");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothConnect.disconnectBluetooth();
        if (D)
            Log.e(LOG_TAG, "onDestroy()");
    }

    public BluetoothConnect getBluetoothConnect() {
        return mBluetoothConnect;
    }

    @Override
    public void onConnect(BluetoothConnect instant) {
        runOnUiThread(new Thread() {
            public void run() {
                Toast.makeText(getApplicationContext(), "Connecting", Toast.LENGTH_SHORT).show();
            }
        });
        if (D)
            Log.e(LOG_TAG, "onConnect() - Connecting");
    }

    @Override
    public void onConnectionSuccess(BluetoothConnect instant) {
        SharedPreferences.Editor preferencesEditor = mSharedPref.edit();
        preferencesEditor.putString(BLUETOOT_REMOTE_DEVICE, mBluetoothConnect.getDeviceAddress());
        preferencesEditor.apply();
        if (D)
            Log.e(LOG_TAG, "onConnectionSuccess() - connected");
        runOnUiThread(new Thread() {
            public void run() {
                runOnUiThread(new Thread() {
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                        if (mMenuSetting != null) {
                            enableConnectMenuItem(false);
                        }
                        displayControlFragment();
                        displayCamFragment();
                    }
                });
            }
        });
    }

    @Override
    public void onConnectionFail(BluetoothConnect instant) {
        runOnUiThread(new Thread() {
            public void run() {
                Toast.makeText(getApplicationContext(),
                        "Connecting fail!",
                        Toast.LENGTH_LONG).show();
            }
        });
        removeSharePerf();
        if (D)
            Log.e(LOG_TAG, "onConnectionFail()");
        runOnUiThread(new Thread() {
            public void run() {
                closeControlFragment();
                closeCamFragment();
                Toast.makeText(getApplicationContext(), "Failed to connect!", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDisconnected(BluetoothConnect instant) {
        runOnUiThread(new Thread() {
            public void run() {
                Toast.makeText(getApplicationContext(), "Disconnected!", Toast.LENGTH_LONG).show();
                closeControlFragment();
                closeCamFragment();
                enableConnectMenuItem(true);
            }
        });
        if (D)
            Log.e(LOG_TAG, "onDisconnected()");
    }

    private void removeSharePerf() {
        SharedPreferences.Editor preferencesEditor = mSharedPref.edit();
        preferencesEditor.remove(BLUETOOT_REMOTE_DEVICE);
        preferencesEditor.apply();
    }

    private void displayControlFragment() {
        MainFragmentControl mainFragmentControl = MainFragmentControl.newInstance();
        // hide the main activity layout containing static fragment
//        final View view = findViewById(R.id.layout_main);
//        view.setVisibility(View.INVISIBLE);
        //
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.layout_main, mainFragmentControl).commit();

    }
    private void displayCamFragment() {
        // hide the main activity layout containing static fragment
//        final View view = findViewById(R.id.layout_main);
//        view.setVisibility(View.INVISIBLE);
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
        }
        else {
            CameraFragment cameraFragment = CameraFragment.newInstance();
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.layout_cam, cameraFragment,"CAMERAFRAGMENT").commit();
        }


    }

    private void closeControlFragment() {
        Fragment mainFragmentControl = getSupportFragmentManager().findFragmentById(R.id.layout_main);
        if (mainFragmentControl != null) {
            FragmentTransaction fragmentTransaction =
                    getSupportFragmentManager().beginTransaction();
//            fragmentTransaction.remove(mainFragmentControl).commit();
            fragmentTransaction.replace(R.id.layout_main, MainFragmentFrontPage.newInstance()).commit();
        }
        if (D)
            Log.e(LOG_TAG, "closeControlFragment() :");

        // show the main activity layout containing static fragment
//        View view = findViewById(R.id.layout_main);
//        view.setVisibility(View.VISIBLE);
    }
    private void closeCamFragment() {
        CameraFragment cameraFragment = (CameraFragment) getSupportFragmentManager().findFragmentByTag("CAMERAFRAGMENT");
        if (cameraFragment != null) {
            FragmentTransaction fragmentTransaction =
                    getSupportFragmentManager().beginTransaction();
            fragmentTransaction.remove(cameraFragment).commit();
            cameraFragment.releaseCamera();
        }
        if (D)
            Log.e(LOG_TAG, "closeCamFragment() :");

        // show the main activity layout containing static fragment
//        View view = findViewById(R.id.layout_main);
//        view.setVisibility(View.VISIBLE);
    }

    /** Post-process for granted permissions */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                displayCamFragment();
            } else {
                // Rejected
                Toast.makeText(this, "Camera Permission is not granted!", Toast.LENGTH_SHORT).show();
            }
            return;
        }

    }

}