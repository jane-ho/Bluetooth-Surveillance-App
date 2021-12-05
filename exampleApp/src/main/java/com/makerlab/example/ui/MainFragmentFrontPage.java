package com.makerlab.example.ui;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class MainFragmentFrontPage extends Fragment {

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
        return inflater.inflate(R.layout.fragment_main_front_page, container, false);

    }
    public static MainFragmentFrontPage newInstance() {
        return new MainFragmentFrontPage();
    }
}