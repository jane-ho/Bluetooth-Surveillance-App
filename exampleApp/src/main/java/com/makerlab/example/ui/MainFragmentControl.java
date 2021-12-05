package com.makerlab.example.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import com.makerlab.bt.BluetoothConnect;
import com.makerlab.example.protocol.DrawBotScaraGcode;
import com.makerlab.example.protocol.GoBLE;
import com.makerlab.example.protocol.PlainTextProtocol;
import com.makerlab.example.protocol.Protocol;
import com.makerlab.example.protocol.Vorpal;
import com.makerlab.example.widgets.ProtocolSelectSpinner;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class MainFragmentControl extends Fragment implements
        View.OnClickListener, AdapterView.OnItemSelectedListener {
    static private String LOG_TAG = MainFragmentControl.class.getSimpleName();
    static public final boolean D = BuildConfig.DEBUG;

    private BluetoothConnect mBluetoothConnect;
    private Timer mDataSendTimer = null;
    private int mProtocolId = -1;
    private GoBLE mGoBLE;
    private PlainTextProtocol mPlainTextProtocol;
    private Vorpal mVorpal;
    private DrawBotScaraGcode mDrawBot;
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
        mGoBLE = new GoBLE();
        mVorpal = new Vorpal();
        mPlainTextProtocol = new PlainTextProtocol();
        mDrawBot = new DrawBotScaraGcode();

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
        ProtocolSelectSpinner protocolSpinner = rootView.findViewById(R.id.protocolSpinner);
        protocolSpinner.setOnItemSelectedListener(this);
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
            switch (mProtocolId) {
                case Protocol.PLAIN_TEXT:
                    mQueue.add(mPlainTextProtocol.getPayload(buttonClicked));
                    mQueue.add(mPlainTextProtocol.getPayload(0));
                    if (D)
                        Log.e(LOG_TAG, "onClick() : plaintext : button clicked " + buttonClicked);
                    break;
                case Protocol.GOBLE:
                    final byte[] buttonMap = {0, 1, 2, 3, 4, 7, 8, 11, 12}; // remap the buttonID
                    byte[] buttonPressed = {buttonMap[buttonClicked]};
                    // move at one interval for  button pressed
                    mQueue.add(mGoBLE.getPayload(127, 127, buttonPressed));
                    // stop at next interval for button released
                    mQueue.add(mGoBLE.getPayload(127, 127, null));
                    if (D)
                        Log.e(LOG_TAG, "onClick() : goble : button clicked " + buttonMap[buttonClicked]);
                    break;
                case Protocol.VORPAL:
                    final byte[][] locomotions = {null, mVorpal.goForward(), mVorpal.goRight(),
                            mVorpal.goBackward(), mVorpal.goLeft(),
                            mVorpal.stomp()};
                    if (buttonClicked >=0 && buttonClicked<locomotions.length && locomotions[buttonClicked] != null) {
                        mQueue.add(locomotions[buttonClicked]);
                    }
                    if (D)
                        Log.e(LOG_TAG, "onClick() : vorpal : button clicked " + buttonClicked);
                    break;
                case Protocol.DrawBotScaraGcode:
                    int feedrate = 800;
                    int interval = 10;
                    if (buttonClicked < 5) {
                        mQueue.add(mDrawBot.setRelativePositioning());
                    }
                    switch (buttonClicked) {
                        case 1:
                            mQueue.add(mDrawBot.moveY(interval, feedrate));
                            break;
                        case 2:
                            mQueue.add(mDrawBot.moveX(interval, feedrate));
                            break;
                        case 3:
                            mQueue.add(mDrawBot.moveY(-interval, feedrate));
                            break;
                        case 4:
                            mQueue.add(mDrawBot.moveX(-interval, feedrate));
                            break;
                        case 5:
                            byte[][] gcodes = mDrawBot.goHomePosition();
                            for (int i = 0; i < gcodes.length; i++) {
                                mQueue.add(gcodes[i]);
                            }
                            break;
                    }
                    if (buttonClicked < 5) {
                        mQueue.add(mDrawBot.setAbsolutePositioning());
                    }
                    if (D)
                        Log.e(LOG_TAG, "onClick() : drawbot : button clicked " + buttonClicked);
                    break;
                default:
                    if (D)
                        Log.e(LOG_TAG, "onClick() : protocol not implemented!");
            }
        }

    }

    //AdapterView.OnItemSelectedListener of spinner view
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        synchronized (mQueue) {
            switch (position) {
                case Protocol.PLAIN_TEXT:
                    mQueue.clear();
                    mProtocolId = Protocol.PLAIN_TEXT;
                    break;
                case Protocol.GOBLE:
                    mQueue.clear();
                    mProtocolId = Protocol.GOBLE;
                    break;
                case Protocol.VORPAL:
                    mQueue.clear();
                    mProtocolId = Protocol.VORPAL;
                    break;
                case Protocol.DrawBotScaraGcode:
                    mQueue.clear();
                    mProtocolId = Protocol.DrawBotScaraGcode;
                    break;
                default:
                    mProtocolId = -1;
            }
        }
        if (D)
            Log.e(LOG_TAG, "onItemSelected()");
    }

    //AdapterView.OnItemSelectedListener of spinner view
    @Override
    public void onNothingSelected(AdapterView<?> parent) {

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


