package com.janeho.app.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.janeho.app.client.MonitorFragment;

public class MonitorActivity extends AppCompatActivity {

    static private String LOG_TAG = MonitorActivity.class.getSimpleName();

    private SharedPreferences mSharedPref;
    private String mSharedPrefFile = "com.makerlab.omni.sharedprefs";

    Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor_main);

        mSharedPref = getSharedPreferences(mSharedPrefFile, MODE_PRIVATE);

        displayMonitorFragment();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        this.menu.removeItem(R.id.action_bluetooth_scan);
        this.menu.removeItem(R.id.action_bluetooth_disconnect);
        this.menu.findItem(R.id.action_switch).setTitle("Camera Mode");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_switch) {
            SharedPreferences.Editor preferencesEditor = mSharedPref.edit();
            preferencesEditor.putString("mode", "server");
            preferencesEditor.apply();
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void displayMonitorFragment() {
        MonitorFragment monitorFragment = MonitorFragment.newInstance();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.layout_monitor, monitorFragment,"MONITOR FRAGMENT").commit();
    }

}