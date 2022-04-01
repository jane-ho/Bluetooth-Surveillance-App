package com.janeho.app.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import com.janeho.bt.BluetoothConnect;
import com.janeho.app.protocol.PlainTextProtocol;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class MainFragmentControl extends Fragment implements View.OnClickListener {
    static private String LOG_TAG = MainFragmentControl.class.getSimpleName();
    static public final boolean D = BuildConfig.DEBUG;

    private BluetoothConnect mBluetoothConnect;
    private Timer mDataSendTimer = null;
    private PlainTextProtocol mPlainTextProtocol;
    private Queue<byte[]> mQueue = new LinkedList<>();
    private final int buttionID[] = {
            0, // dummy value
            R.id.forwardButton, R.id.rightButton,
            R.id.backwardButton, R.id.leftButton,
            R.id.centerButton, R.id.portraitButton,
            R.id.landscapeRightButton, R.id.landscapeLeftButton,
    };

    public MainFragmentControl() {
        // Required empty public constructor
    }

    public static MainFragmentControl newInstance() {
        return new MainFragmentControl();
    }

    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        Log.e(LOG_TAG, "onCreate()");
        mPlainTextProtocol = new PlainTextProtocol();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_main_control, container, false);
        for (int i = 1; i < buttionID.length; i++) {
            Button button = rootView.findViewById(buttionID[i]);
            if (button != null) {
                button.setOnClickListener(this);
            }
            Log.e(LOG_TAG, "onCreateView()");
        }
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        MainActivity activity = (MainActivity) getActivity();
        mBluetoothConnect = activity.getBluetoothConnect();
        mDataSendTimer = new Timer();
        mDataSendTimer.scheduleAtFixedRate(new DataSendTimerTask(), 1000, 250);
        if (D)
            Log.e(LOG_TAG, "onStart()");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mDataSendTimer != null) {
            mDataSendTimer.cancel();
        }
        mBluetoothConnect = null;
        if (D)
            Log.e(LOG_TAG, "onStop()");
    }

    @Override
    public void onClick(View view) {
        //view.setEnabled(false);
        int buttonClicked = -1;
        for (int i = 1; i < buttionID.length; i++) {
            if (view.getId() == buttionID[i]) {
                buttonClicked = i;
                break;
            }
        }
        //view.setEnabled(true);
        synchronized (mQueue) {
            mQueue.add(mPlainTextProtocol.getPayload(buttonClicked));
            mQueue.add(mPlainTextProtocol.getPayload(0));
        }

    }

    class DataSendTimerTask extends TimerTask {
        private String LOG_TAG = DataSendTimerTask.class.getSimpleName();

        @Override
        public void run() {
            if (mBluetoothConnect == null) {
                return;
            }
            synchronized (mQueue) {
                if (!mQueue.isEmpty()) {
                    mBluetoothConnect.send(mQueue.remove());
                    Log.e(LOG_TAG, "DataSendTimerTask.run() - send");
                }
            }
        }
    }
}


