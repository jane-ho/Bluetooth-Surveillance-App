package com.janeho.app.client;

import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;


import com.janeho.app.ui.R;

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

//        EditText et_ip = inflatedView.findViewById(R.id.editText_ip);
//        et_ip.setText("192.168.1.38");

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
//        reset();
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
}