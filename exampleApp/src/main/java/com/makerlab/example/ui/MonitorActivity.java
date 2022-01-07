package com.makerlab.example.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MonitorActivity extends AppCompatActivity {

    static private String LOG_TAG = MainActivity.class.getSimpleName();

    private SharedPreferences mSharedPref;
    private String mSharedPrefFile = "com.makerlab.omni.sharedprefs";

    Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor_main);

        mSharedPref = getSharedPreferences(mSharedPrefFile, MODE_PRIVATE);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        this.menu.findItem(R.id.action_switch).setTitle("Camera Mode");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_switch) {
            SharedPreferences.Editor preferencesEditor = mSharedPref.edit();
            preferencesEditor.putString("mode", "server");
            preferencesEditor.apply();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }


}