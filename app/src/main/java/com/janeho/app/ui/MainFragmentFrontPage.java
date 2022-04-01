package com.janeho.app.ui;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class MainFragmentFrontPage extends Fragment {

    private static final String TAG = MainFragmentFrontPage.class.getSimpleName();

    public MainFragmentFrontPage() {
        // Required empty public constructor
    }

    public static MainFragmentFrontPage newInstance(String param1, String param2) {
        MainFragmentFrontPage fragment = new MainFragmentFrontPage();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_main_front_page, container, false);
        rootView.findViewById(R.id.button_have).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: have sentry");
                MainActivity activity = (MainActivity) getActivity();
                activity.startBluetoothScan(MainFragmentFrontPage.class.getSimpleName());
            }
        });
        rootView.findViewById(R.id.button_donthave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: no sentry");
                MainActivity activity = (MainActivity) getActivity();
                activity.startWithoutBLE(MainFragmentFrontPage.class.getSimpleName());
            }
        });
        rootView.findViewById(R.id.button_monitor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: monitor mode");
                MainActivity activity = (MainActivity) getActivity();
                activity.startMonitorActivity();
            }
        });

        return rootView;
    }

    public static MainFragmentFrontPage newInstance() {
        return new MainFragmentFrontPage();
    }
}