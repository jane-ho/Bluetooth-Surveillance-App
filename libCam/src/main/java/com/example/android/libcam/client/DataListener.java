package com.example.android.libcam.client;


import android.graphics.Bitmap;

public interface DataListener {
    public void onDirty(Bitmap bufferedImage);
    public void onDisconnect();
    public void onConnect();
}
