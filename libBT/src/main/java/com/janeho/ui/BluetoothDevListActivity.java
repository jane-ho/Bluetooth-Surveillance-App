package com.janeho.ui;


import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.janeho.bt.BluetoothScan;


public class BluetoothDevListActivity extends AppCompatActivity
        implements BluetoothScan.ResultHandler, View.OnClickListener {
    static public final String EXTRA_KEY_ADDRESS = "address";
    static public final String EXTRA_KEY_DEVICE = "device";
    static private String LOG_TAG =BluetoothDevListActivity.class.getSimpleName();;

    private RecyclerView mRecyclerView;
    private BluetoothDevRecyclerView.Adapter mAdapter;
    private boolean mIsScanning = false;
    private Button mButtonScan;
    private BluetoothScan mbluetoothScan;
    private BluetoothDevice selectedDevice;
    private ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_dev_list);
        //
        mButtonScan = findViewById(R.id.buttonBtScan);
        mButtonScan.setOnClickListener(this);
        // Get a handle to the RecyclerView.
        mRecyclerView = findViewById(R.id.recyclerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Create an adapter and supply the data to be displayed.
        mAdapter = new BluetoothDevRecyclerView.Adapter(this);
        mRecyclerView.setAdapter(mAdapter);
        //
        mbluetoothScan = new BluetoothScan(this);
        mbluetoothScan.setResultHandler(this);

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: // the up button pressed
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onClick(View v) {
        if (v.getId() == R.id.buttonBtScan) {
            if (!mIsScanning) {
                progressBar.setVisibility(View.VISIBLE);
                mAdapter.clearBluetoothDeviceList();
                mIsScanning = mbluetoothScan.start();
                if (mIsScanning) {
                    mButtonScan.setText(getString(R.string.button_stop_label));
                }
            } else {
                progressBar.setVisibility(View.INVISIBLE);
                mbluetoothScan.stop();
                mIsScanning = false;
                mButtonScan.setText(getString(R.string.button_scan_label));
            }
            return;
        }
        //bluetooth device is selected from recycle view
        TextView textView = v.findViewById(R.id.deviceAddr);
        String address = textView.getText().toString();
        //
        selectedDevice = mAdapter.getBluetoothDevice(address);
        mAdapter.clearBluetoothDeviceList();
        //
        Intent intent = new Intent();
        intent.putExtra(EXTRA_KEY_ADDRESS, address);
        if (selectedDevice != null) {
            intent.putExtra(EXTRA_KEY_DEVICE, selectedDevice);
        }
        setResult(RESULT_OK, intent);
        finish();
        Log.e(LOG_TAG, "onClick() - selected device:" + address);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mbluetoothScan.stop();

    }

    @Override
    public void setResult(BluetoothDevice bluetoothDevice) {
        mAdapter.addBluetoothDevice(bluetoothDevice);
    }

    @Override
    public void onPostResult() {
        runOnUiThread(new Thread() {
            public void run() {
                mIsScanning = false;
                mButtonScan.setText(getString(R.string.button_scan_label));
                progressBar.setVisibility(View.INVISIBLE);
            }
        });

    }
}
