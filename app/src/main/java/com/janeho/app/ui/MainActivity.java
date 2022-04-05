package com.janeho.app.ui;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.janeho.app.server.CameraFragment;
import com.janeho.bt.BluetoothConnect;
import com.janeho.bt.BluetoothScan;
import com.janeho.ui.BluetoothDevListActivity;

public class MainActivity extends AppCompatActivity implements
        BluetoothConnect.ConnectionHandler {
    static public final boolean D = BuildConfig.DEBUG;
    static public final int REQUEST_BT_GET_DEVICE = 1112;
    static public final String BLUETOOTH_REMOTE_DEVICE = "bt_remote_device";
    private static final int MY_CAMERA_PERMISSION_CODE = 1000;
    static private String LOG_TAG = MainActivity.class.getSimpleName();

    private BluetoothConnect mBluetoothConnect;
    private BluetoothScan mBluetoothScan;

    private Menu mMenuSetting;
    private SharedPreferences mSharedPref;
    private String mSharedPrefFile = "com.makerlab.omni.sharedprefs";
    //
    Thread mOnBTConnectSuccessThread;
    CameraFragment cameraFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBluetoothConnect = new BluetoothConnect(this);
        mBluetoothConnect.setConnectionHandler(this);

        mSharedPref = getSharedPreferences(mSharedPrefFile, MODE_PRIVATE);
        String bleDeviceAddr = mSharedPref.getString(BLUETOOTH_REMOTE_DEVICE, null);
        if (bleDeviceAddr != null) {
            //Log.e(LOG_TAG, "onCreate(): found share perference");
            mBluetoothScan = new BluetoothScan(this);
            BluetoothDevice mBluetoothDevice = mBluetoothScan.getBluetoothDevice(bleDeviceAddr);
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

            mMenuSetting.findItem(R.id.action_switch).setTitle("Monitor Mode");
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

        if (item.getItemId() == R.id.action_switch){
            startMonitorActivity();
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
        if (D)
            Log.e(LOG_TAG, "onConnectionSuccess() - connected");

        mOnBTConnectSuccessThread = new Thread() {
            public void run() {
                // ask for remembering bluetooth device or not
                String bleDeviceAddr = mSharedPref.getString(BLUETOOTH_REMOTE_DEVICE, null);
                if (bleDeviceAddr == null || bleDeviceAddr != mBluetoothConnect.getDeviceAddress()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Remember this device?");
                    builder.setMessage("Remember this device?");
                    builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // add to SharedPreferences
                            SharedPreferences.Editor preferencesEditor = mSharedPref.edit();
                            preferencesEditor.putString(BLUETOOTH_REMOTE_DEVICE, mBluetoothConnect.getDeviceAddress());
                            preferencesEditor.apply();
                            dialog.dismiss();
                        }
                    });
                    builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing
                            dialog.dismiss();
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }

                Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                if (mMenuSetting != null) {
                    enableConnectMenuItem(false);
                }
                displayControlFragment();
                displayCamFragment();
            }
        };
        runOnUiThread(mOnBTConnectSuccessThread);
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
        preferencesEditor.remove(BLUETOOTH_REMOTE_DEVICE);
        preferencesEditor.apply();
    }

    private void displayControlFragment() {
        MainFragmentControl mainFragmentControl = MainFragmentControl.newInstance();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.layout_main, mainFragmentControl).commit();

    }
    private void displayCamFragment() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
        }
        else {
            cameraFragment = new CameraFragment(true);
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.layout_cam, cameraFragment,"CAMERA FRAGMENT").commit();
        }
    }

    private void displayCamFragmentOnly() {
        if (mBluetoothConnect!=null && mBluetoothConnect.isConnected())
            mBluetoothConnect.disconnectBluetooth();
        if (mOnBTConnectSuccessThread!=null && mOnBTConnectSuccessThread.isAlive())
            mOnBTConnectSuccessThread.interrupt();

        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
        }
        else {
            if (cameraFragment != null) {
                cameraFragment.onStop();
                cameraFragment.onDestroy();
            }
            cameraFragment = new CameraFragment(false);
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.layout_cam, cameraFragment,"CAMERA FRAGMENT");
            Fragment fragment = getSupportFragmentManager().findFragmentByTag("Front Fragment");
            fragmentTransaction.remove(fragment);
            fragmentTransaction.commit();

            MenuItem menuItem = mMenuSetting.findItem(R.id.action_bluetooth_scan);
            menuItem.setVisible(false);
            menuItem = mMenuSetting.findItem(R.id.action_bluetooth_disconnect);
            menuItem.setVisible(false);
        }
    }

    private void closeControlFragment() {
        Fragment mainFragmentControl = getSupportFragmentManager().findFragmentById(R.id.layout_main);
        if (mainFragmentControl != null) {
            FragmentTransaction fragmentTransaction =
                    getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.layout_main, MainFragmentFrontPage.newInstance()).commit();
        }
        if (D)
            Log.e(LOG_TAG, "closeControlFragment() :");
    }

    private void closeCamFragment() {
        CameraFragment cameraFragment = (CameraFragment) getSupportFragmentManager().findFragmentByTag("CAMERA FRAGMENT");
        if (cameraFragment != null) {
            FragmentTransaction fragmentTransaction =
                    getSupportFragmentManager().beginTransaction();
            fragmentTransaction.remove(cameraFragment).commit();
//            cameraFragment.onStop();
//            cameraFragment.releaseCamera();
        }
        if (D)
            Log.e(LOG_TAG, "closeCamFragment() :");
    }

    public void startMonitorActivity() {
        SharedPreferences.Editor preferencesEditor = mSharedPref.edit();
        preferencesEditor.putString("mode", "client");
        preferencesEditor.apply();
        Intent intent = new Intent(this, MonitorActivity.class);
        startActivity(intent);
    }

    public void startBluetoothScan(String tag) {
        if (tag.equals(MainFragmentFrontPage.class.getSimpleName())){
            if (mBluetoothConnect.isConnected()) {
                mBluetoothConnect.disconnectBluetooth();
            }
            Intent intent = new Intent(this, BluetoothDevListActivity.class);
            startActivityForResult(intent, REQUEST_BT_GET_DEVICE);
        }
    }

    public void startWithoutBLE(String tag) {
        if (tag.equals(MainFragmentFrontPage.class.getSimpleName())) {
            Toast.makeText(this, "Starting IP CAM without sentry...", Toast.LENGTH_SHORT).show();
            displayCamFragmentOnly();
        }
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