package com.janeho.app.client;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;


import com.janeho.app.ui.R;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class MonitorControl extends Fragment implements View.OnClickListener {

    private static final String LOG_TAG = MonitorControl.class.getSimpleName();
    private final int buttionID[] = {
            0, // dummy value
            R.id.forwardButton, R.id.rightButton,
            R.id.backwardButton, R.id.leftButton,
            R.id.centerButton,
//            R.id.portraitButton,
//            R.id.landscapeRightButton, R.id.landscapeLeftButton,
    };

    private Timer mTimer;
    private SocketClient mSocket;
    private Queue<byte[]> mQueue = new LinkedList<>();
    private MonitorFragment parent;

    public MonitorControl(MonitorFragment fragment) {
        this.parent = fragment;
    }

    public void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        Log.e(LOG_TAG, "onCreate()");
//        mPlainTextProtocol = new PlainTextProtocol();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_monitor_control, container, false);
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
    public void onClick(View view) {
        view.setEnabled(false);
        synchronized (mQueue) {
            byte[] payload = new byte[0];
            Button b = (Button) view;
            try {
                payload = (b.getHint().toString()+"\r\n").getBytes("iso8859-1");    // TODO: payload content
                mQueue.add(payload);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        view.setEnabled(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (parent==null) Log.d("MonitorControl", "onStart: parent is null");
        mSocket = parent.getSocket();
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new DataSendTimerTask(), 1000, 250);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mTimer != null){
            mTimer.cancel();
        }
        mSocket = null;
    }

    class DataSendTimerTask extends TimerTask {
        private String LOG_TAG = DataSendTimerTask.class.getSimpleName();

        @Override
        public void run() {
            if (mSocket == null) {
                return;
            }
            synchronized (mQueue) {
                if (!mQueue.isEmpty()) {
                    mSocket.send(mQueue.remove());
                    Log.e(LOG_TAG, "DataSendTimerTask.run() - send");
                }
            }
        }
    }
}
