package com.example.android.libcam.server;

import android.util.Log;

import com.example.android.libcam.CameraView;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer extends Thread {
    private ServerSocket mServer;
//    private DataListener mDataListener;
//    private BufferManager mBufferManager;

    private CameraView mCameraPreview;
    private static final String TAG = "server socket";
    private int mPort;

    public SocketServer(CameraView preview) {
        mCameraPreview = preview;
        mPort = 8888;
        start();
    }

    public SocketServer(CameraView preview, int port) {
        mCameraPreview = preview;
        mPort = port;
        start();
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        super.run();

        System.out.println("server is waiting");
        BufferedInputStream inputStream = null;
        BufferedOutputStream outputStream = null;
        Socket mSocket = null;
        ByteArrayOutputStream byteArray = null;
        try {
            mServer = new ServerSocket(mPort);
            while (!Thread.currentThread().isInterrupted()) {
                if (byteArray != null)
                    byteArray.reset();
                else
                    byteArray = new ByteArrayOutputStream();

                mSocket = mServer.accept();
                System.out.println("new socket");

                outputStream = new BufferedOutputStream(mSocket.getOutputStream());
                inputStream = new BufferedInputStream(mSocket.getInputStream());

                JsonObject jsonObj = new JsonObject();
                jsonObj.addProperty("type", "data");
                jsonObj.addProperty("length", mCameraPreview.getPreviewLength());
                jsonObj.addProperty("width", mCameraPreview.getPreviewWidth());
                jsonObj.addProperty("height", mCameraPreview.getPreviewHeight());

                byte[] buff = new byte[256];
                int len = 0;
                String msg = null;
                outputStream.write(jsonObj.toString().getBytes());
                outputStream.flush();

                while ((len = inputStream.read(buff)) != -1) {
                    msg = new String(buff, 0, len);

                    // JSON analysis
                    JsonParser parser = new JsonParser();
                    boolean isJSON = true;
                    JsonElement element = null;
                    try {
                        element =  parser.parse(msg);
                    }
                    catch (JsonParseException e) {
                        Log.e(TAG, "exception: " + e);
                        isJSON = false;
                    }
                    if (isJSON && element != null) {
                        JsonObject obj = element.getAsJsonObject();
                        element = obj.get("state");
                        if (element != null && element.getAsString().equals("ok")) {
                            // send data
                            while (true) {
                                outputStream.write(mCameraPreview.getImageBuffer());
                                outputStream.flush();

                                if (Thread.currentThread().isInterrupted())
                                    break;
                            }

                            break;
                        }
                    }
                    else {
                        break;
                    }
                }

                outputStream.close();
                inputStream.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                    outputStream = null;
                }

                if (inputStream != null) {
                    inputStream.close();
                    inputStream = null;
                }

                if (mSocket != null) {
                    mSocket.close();
                    mSocket = null;
                }

                if (byteArray != null) {
                    byteArray.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    public void close() {
        if (mServer != null) {
            try {
                mServer.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();
        try {
            mServer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//    public void setOnDataListener(DataListener listener) {
//        mDataListener = listener;
//    }
}