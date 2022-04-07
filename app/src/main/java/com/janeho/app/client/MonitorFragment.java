package com.janeho.app.client;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;


import com.janeho.app.ui.MainActivity;
import com.janeho.app.ui.R;
import com.janeho.bt.BluetoothScan;

import java.util.LinkedList;

public class MonitorFragment extends Fragment implements DataListener {
    private final static String TAG = MonitorFragment.class.getSimpleName();
    private LinkedList<Bitmap> mQueue = new LinkedList<Bitmap>();
    private static final int MAX_BUFFER = 15;
    ImageView iv_monitor;

    Bitmap mImage, mLastFrame;

    private boolean mIsOn = true;
    private SocketClient mThread;
    private Button mButton;

    private SharedPreferences mSharedPref;
    private String mSharedPrefFile = "com.ipcam.sharedprefs";
    static public final String SERVER_IP_ADDRESS = "server_ip_addr";
    static public final String SERVER_PORT_NO = "server_port_no";

    public MonitorFragment() {
        // Required empty public constructor
    }

    public static MonitorFragment newInstance() {
        return new MonitorFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.fragment_monitor, container, false);
        iv_monitor = inflatedView.findViewById(R.id.imageView_monitor);

        mSharedPref = getContext().getSharedPreferences(mSharedPrefFile, Context.MODE_PRIVATE);
        String saved_ip = mSharedPref.getString(SERVER_IP_ADDRESS, null);
        int saved_port = mSharedPref.getInt(SERVER_PORT_NO, 0);
        if (saved_ip!=null && saved_port != 0) {
            //Log.e(LOG_TAG, "onCreate(): found share perference");
            EditText et_ip = inflatedView.findViewById(R.id.editText_ip);
            EditText et_port = inflatedView.findViewById(R.id.editText_port);
            et_ip.setText(saved_ip);
            et_port.setText(String.valueOf(saved_port));
        }

        mButton = inflatedView.findViewById(R.id.button_connect);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mIsOn) {
                    EditText et_ip = inflatedView.findViewById(R.id.editText_ip);
                    EditText et_port = inflatedView.findViewById(R.id.editText_port);
                    mThread = new SocketClient(et_ip.getText().toString(), Integer.valueOf(et_port.getText().toString()));  // TODO: input validation
                    mThread.setOnDataListener(MonitorFragment.this);
                    mThread.start();

                    // SharedPreferences
                    rememberIP(et_ip.getText().toString(),Integer.valueOf(et_port.getText().toString()));

                    mIsOn = false;
                    mButton.setText("Disconnect");
                } else {
                    closeSocketClient();
                    reset();
                }

            }
        });
        return inflatedView;
    }

    private void updateUI(Bitmap bufferedImage) {

        synchronized (mQueue) {
            if (mQueue.size() ==  MAX_BUFFER) {
                mLastFrame = mQueue.poll();
            }
            if (mQueue.size() > 0) {
                mLastFrame = mQueue.poll();
            }
            mQueue.add(bufferedImage);
        }

        if (mLastFrame != null) {
            iv_monitor.post(new Runnable() {
                @Override
                public void run() {
                    iv_monitor.setImageBitmap(mLastFrame);
                }
            });
        }
        else if (mImage != null) {
            iv_monitor.post(new Runnable() {
                @Override
                public void run() {
                    iv_monitor.setImageBitmap(mImage);
                }
            });
        }
    }


    @Override
    public void onDirty(Bitmap bufferedImage) {
        updateUI(bufferedImage);
    }

    @Override
    public void onDisconnect() {
        closeSocketClient();
        reset();
    }



    @Override
    public void onConnect() {
        iv_monitor.post(new Runnable() {
            @Override
            public void run() {
                showMonitor();
            }
        });
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                displayControlFragment();
            }
        });
    }

    private void reset() {
        mButton.post(new Runnable() {
            @Override
            public void run() {
                mButton.setText("Connect");
            }
        });
        mIsOn = true;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                closeControlFragment();
                hideMonitor();
            }
        });
    }

    private void closeSocketClient() {
        if (mThread == null)
            return;

        mThread.interrupt();
        try {
            mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mThread = null;

        mQueue.clear();
    }

    @Override
    public void onStop() {
        super.onStop();
        closeSocketClient();
    }

    private void hideMonitor() {
        iv_monitor.setVisibility(View.INVISIBLE);
    }
    private void showMonitor() { iv_monitor.setVisibility(View.VISIBLE); }

    private void displayControlFragment() {
        MonitorControl monitorControl = new MonitorControl(this);
        FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.layout_control, monitorControl).commit();
    }
    private void closeControlFragment() {
        if (getActivity() == null)
            return;
        Fragment mainFragmentControl = getActivity().getSupportFragmentManager().findFragmentById(R.id.layout_control);
        if (mainFragmentControl != null) {
            FragmentTransaction fragmentTransaction =
                    getActivity().getSupportFragmentManager().beginTransaction();
            fragmentTransaction.remove(mainFragmentControl).commit();
        }
    }

    public SocketClient getSocket(){
        return mThread;
    }

    private void rememberIP(String ip, int port){
        if (ip!=null && port != 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Remember this IP address?");
            builder.setMessage("Remember this IP address?");
            builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // add to SharedPreferences
                    SharedPreferences.Editor preferencesEditor = mSharedPref.edit();
                    preferencesEditor.putString(SERVER_IP_ADDRESS, ip);
                    preferencesEditor.putInt(SERVER_PORT_NO, port);
                    preferencesEditor.apply();
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Do nothing
                    SharedPreferences.Editor preferencesEditor = mSharedPref.edit();
                    preferencesEditor.remove(SERVER_IP_ADDRESS);
                    preferencesEditor.remove(SERVER_PORT_NO);
                    preferencesEditor.apply();
                    dialog.dismiss();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
            Button btn_p = alert.getButton(DialogInterface.BUTTON_POSITIVE);
            Button btn_n = alert.getButton(DialogInterface.BUTTON_NEGATIVE);
            btn_p.setTextColor(Color.BLACK);
            btn_n.setTextColor(Color.BLACK);
        }
    }
}