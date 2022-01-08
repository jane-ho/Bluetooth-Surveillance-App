package com.example.android.libcam.client;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.libcam.R;
import com.example.android.libcam.server.SocketServer;

import java.util.LinkedList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class MonitorFragment extends Fragment implements DataListener {
    private final static String TAG = MonitorFragment.class.getSimpleName();
    private LinkedList<Bitmap> mQueue = new LinkedList<Bitmap>();
    private static final int MAX_BUFFER = 15;
    ImageView iv_monitor;

    Bitmap mImage, mLastFrame;

    private boolean mIsOn = true;
    private SocketClient mThread;
    private Button mButton;

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

        EditText et_ip = inflatedView.findViewById(R.id.editText_ip);
        et_ip.setText("192.168.1.38");

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
            if (mQueue.size() > 0) {    //
                mLastFrame = mQueue.poll();    //
            }       //
            mQueue.add(bufferedImage);
        }

//        synchronized (mQueue) {
//            if (mQueue.size() > 0) {
//                mLastFrame = mQueue.poll();
//            }
//        }
        if (mLastFrame != null) {
//            iv_monitor.setImageBitmap(mLastFrame);
            iv_monitor.post(new Runnable() {
                @Override
                public void run() {
                    iv_monitor.setImageBitmap(mLastFrame);
                }
            });
        }
        else if (mImage != null) {
//            iv_monitor.setImageBitmap(mImage);
            iv_monitor.post(new Runnable() {
                @Override
                public void run() {
                    iv_monitor.setImageBitmap(mImage);
                }
            });
        }
    }

//    @Override
//    public Dimension getPreferredSize() {
//        if (mImage == null) {
//            return new Dimension(960,720); // init window size
//        } else {
//            return new Dimension(mImage.getWidth(null), mImage.getHeight(null));
//        }
//    }

    @Override
    public void onDirty(Bitmap bufferedImage) {
        updateUI(bufferedImage);
    }

    @Override
    public void onDisconnect() {
//        getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(getContext(), "Connection Failed", Toast.LENGTH_SHORT).show();
//            }
//        });
        closeSocketClient();
        reset();
    }

    private void reset() {
        mButton.post(new Runnable() {
            @Override
            public void run() {
                mButton.setText("Connect");
            }
        });
        mIsOn = true;
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
    }

    @Override
    public void onStop() {
        super.onStop();
        closeSocketClient();
        reset();
    }
}