package com.example.android.libcam.client;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.example.android.libcam.client.BufferManager;
import com.example.android.libcam.client.DataListener;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class SocketClient extends Thread {
    private DataListener mDataListener;
    private BufferManager mBufferManager;
    private static final String TAG = "socket";
    private String mIP = "";
    private int mPort = 8888;

    BufferedInputStream inputStream = null;
    BufferedOutputStream outputStream = null;
    Socket socket = null;
    ByteArrayOutputStream byteArray = null;

    public SocketClient(String ip, int port) {
        mIP = ip;
        mPort = port;
        if (mIP == null) {
            Log.d(TAG, "SocketClient: no ip info is provided.");
        }
    }

    public SocketClient() {
        Log.d(TAG, "SocketClient: no ip is provided");
    }

    @Override
    public void run() {
        super.run();

//        BufferedInputStream inputStream = null;
//        BufferedOutputStream outputStream = null;
//        Socket socket = null;
//        ByteArrayOutputStream byteArray = null;
        try {
            socket = new Socket();
            Log.d(TAG, "run: connecting to server");
            socket.connect(new InetSocketAddress(mIP, mPort),5000);
            inputStream = new BufferedInputStream(socket.getInputStream());
            outputStream = new BufferedOutputStream(socket.getOutputStream());

            byte[] buff = new byte[256];
            byte[] imageBuff = null;
            int len = 0;
            String msg = null;

            // read msg
            Log.d(TAG, "run: reading info");
            while ((len = inputStream.read(buff)) != -1) {
                msg = new String(buff, 0, len);
                // JSON analysis
                JsonParser parser = new JsonParser();
                boolean isJSON = true;
                JsonElement element = null;
                try {
                    element = parser.parse(msg);
                } catch (JsonParseException e) {
                    System.out.println("exception: " + e);
                    isJSON = false;
                }
                if (isJSON && element != null) {
                    JsonObject obj = element.getAsJsonObject();
                    element = obj.get("type");
                    if (element != null && element.getAsString().equals("data")) {
                        element = obj.get("length");
                        int length = element.getAsInt();
                        element = obj.get("width");
                        int width = element.getAsInt();
                        element = obj.get("height");
                        int height = element.getAsInt();

                        imageBuff = new byte[length];
                        mBufferManager = new BufferManager(length, width, height);
                        mBufferManager.setOnDataListener(mDataListener);
                        break;
                    }
                } else {
                    byteArray.write(buff, 0, len);
                    break;
                }
            }

            mDataListener.onConnect();

            if (imageBuff != null) {
                Log.d(TAG, "run: writing ok");
                JsonObject jsonObj = new JsonObject();
                jsonObj.addProperty("state", "ok");
                outputStream.write(jsonObj.toString().getBytes());
                outputStream.flush();

                // read image data
                Log.d(TAG, "run: reading image");
                while ((len = inputStream.read(imageBuff)) != -1) {
                    mBufferManager.fillBuffer(imageBuff, len);
                }
            }

            if (mBufferManager != null) {
                mBufferManager.close();
                System.out.println("Disconnected");
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

                if (socket != null) {
                    socket.close();
                    socket = null;
                }

                if (byteArray != null) {
                    byteArray.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mDataListener.onDisconnect();
            }

        }

    }

    public void setOnDataListener(DataListener listener) {
        mDataListener = listener;
    }

    @Override
    public void interrupt() {
        super.interrupt();
        try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }

            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }

            if (socket != null) {
                socket.close();
                socket = null;
            }

            if (byteArray != null) {
                byteArray.close();
            }
//
//            if (mBufferManager != null){
//                mBufferManager.close();
//            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(byte[] payload) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (outputStream != null){
                    if (payload == null || payload.length == 0) {
                        Log.d(TAG, "send(): invalid payload");
                        return ;
                    }
                    JsonObject jsonObj = new JsonObject();
                    try {
                        String str = new String(payload,"iso8859-1");
                        Log.d(TAG, "send(): writing "+str);
                        jsonObj.addProperty("command", str);
                        try {
                            outputStream.write(jsonObj.toString().getBytes());
                            outputStream.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.run();

    }
}
