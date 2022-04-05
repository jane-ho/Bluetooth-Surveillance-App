package com.janeho.app.server;

import android.util.Log;

import androidx.fragment.app.Fragment;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.janeho.bt.BluetoothConnect;
import com.janeho.app.ui.MainActivity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServer extends Thread {
    private ServerSocket mServer;

    private CameraView mCameraPreview;
    private static final String TAG = "server socket";
    private int mPort;
    private Fragment fragment;

    private boolean withControl;
    private boolean isFront;

    BufferedInputStream inputStream = null;
    BufferedOutputStream outputStream = null;
    Socket mSocket = null;
    ByteArrayOutputStream byteArray = null;

    private ExecutorService executorService = Executors.newFixedThreadPool(20);
    // OpenCV
    private ArrayList<Socket> mSocketList = new ArrayList<Socket>();

    public SocketServer(CameraView preview, int port, Fragment fragment, boolean withControl, boolean isFront) {
        mCameraPreview = preview;
        mPort = port;
        this.fragment = fragment;
        this.withControl = withControl;
        this.isFront = isFront;
        start();
    }

    @Override
    public void run() {
        super.run();

        System.out.println("server is waiting");
        try {
            mServer = new ServerSocket(mPort);
            while (!Thread.currentThread().isInterrupted()) {
                if (byteArray != null)
                    byteArray.reset();
                else
                    byteArray = new ByteArrayOutputStream();

                mSocket = mServer.accept();
                mSocketList.add(mSocket);      // OpenCV
                System.out.println("new socket");
                executorService.submit(new ClientHandler(mSocket, mCameraPreview, isFront));
                if (withControl)
                    executorService.submit(new Reader(mSocket, fragment));
//                outputStream = new BufferedOutputStream(mSocket.getOutputStream());
//                inputStream = new BufferedInputStream(mSocket.getInputStream());
//
//                JsonObject jsonObj = new JsonObject();
//                jsonObj.addProperty("type", "data");
//                jsonObj.addProperty("length", mCameraPreview.getPreviewLength());
//                jsonObj.addProperty("width", mCameraPreview.getPreviewWidth());
//                jsonObj.addProperty("height", mCameraPreview.getPreviewHeight());
//
//                byte[] buff = new byte[256];
//                int len = 0;
//                String msg = null;
//                outputStream.write(jsonObj.toString().getBytes());
//                outputStream.flush();
//
//                while ((len = inputStream.read(buff)) != -1) {
//                    msg = new String(buff, 0, len);
//
//                    // JSON analysis
//                    JsonParser parser = new JsonParser();
//                    boolean isJSON = true;
//                    JsonElement element = null;
//                    try {
//                        element =  parser.parse(msg);
//                    }
//                    catch (JsonParseException e) {
//                        Log.e(TAG, "exception: " + e);
//                        isJSON = false;
//                    }
//                    if (isJSON && element != null) {
//                        JsonObject obj = element.getAsJsonObject();
//                        element = obj.get("state");
//                        if (element != null && element.getAsString().equals("ok")) {
//                            // send data
//                            while (true) {
//                                outputStream.write(mCameraPreview.getImageBuffer());
//                                outputStream.flush();
//
//                                if (Thread.currentThread().isInterrupted())
//                                    break;
//                            }
//
//                            break;
//                        }
//                    }
//                    else {
//                        break;
//                    }
//                }
//
//                outputStream.close();
//                inputStream.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdownNow();
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

                if (mSocketList.size() > 0)     // OpenCV
                    mSocketList.clear();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    @Override
    public void interrupt() {
        super.interrupt();
        executorService.shutdownNow();
        try {
            mServer.close();
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
            if (mSocketList.size() > 0)     // OpenCV
                mSocketList.clear();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler implements Runnable {
        Socket mSocket;
        private CameraView mCameraPreview;
        private boolean isFront;

        public ClientHandler(Socket socket, CameraView mCameraPreview, boolean isFront) {
            this.mSocket = socket;
            this.mCameraPreview = mCameraPreview;
            this.isFront = isFront;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    if (byteArray != null)
                        byteArray.reset();
                    else
                        byteArray = new ByteArrayOutputStream();

                    outputStream = new BufferedOutputStream(mSocket.getOutputStream());
                    inputStream = new BufferedInputStream(mSocket.getInputStream());

                    JsonObject jsonObj = new JsonObject();
                    jsonObj.addProperty("type", "data");
                    jsonObj.addProperty("length", mCameraPreview.getPreviewLength());
                    jsonObj.addProperty("width", mCameraPreview.getPreviewWidth());
                    jsonObj.addProperty("height", mCameraPreview.getPreviewHeight());
                    int orientation = mCameraPreview.getPreviewOrientation();
                    orientation = isFront? 360-orientation : orientation;
                    jsonObj.addProperty("orientation", orientation);

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
//                                while (mCameraPreview.getPreviewOrientation() == orientation) {

//                                // HOG OpenCV
//                                HumanDetection hd = new HumanDetection();
                                while (true) {
                                    // HOG OpenCV
                                    byte[] imgbuff = mCameraPreview.getImageBuffer();
//                                    hd.detect(imgbuff,mCameraPreview.getPreviewHeight(),mCameraPreview.getPreviewWidth());

                                    outputStream.write(imgbuff);
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
    }

    private class Reader implements Runnable {
        Socket mSocket;
        Fragment fragment;

        public Reader(Socket socket, Fragment fragment) {
            this.mSocket = socket;
            this.fragment = fragment;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    inputStream = new BufferedInputStream(mSocket.getInputStream());

                    byte[] buff = new byte[256];
                    int len = 0;
                    String msg = null;

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
                            element = obj.get("command");
                            if (element != null) {
                                // TODO
                                String cmd = element.getAsString();
                                Log.d(TAG, "read command: "+ cmd);
                                MainActivity activity = (MainActivity) fragment.getActivity();
                                BluetoothConnect mBtConnect = activity.getBluetoothConnect();
                                synchronized (mBtConnect){
                                    mBtConnect.send((cmd+"/r/n").getBytes("iso8859-1"));
                                }
                            }
                        }
                        else {
                            break;
                        }
                    }

                    inputStream.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                        inputStream = null;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    // OpenCV
    private class BroadcastSender extends Thread {
        String msg;

        public BroadcastSender (String msg){
            this.msg = msg;
        }

        @Override
        public void run() {
            super.run();
            if (mSocketList.size() > 0 ){
                for (int i=0; i<mSocketList.size();i++){
                    Log.d(TAG, "send broadcast: warning"+i);
                    Socket mSocket = mSocketList.get(i);
                    try {
                        OutputStream outputStream = new BufferedOutputStream(mSocket.getOutputStream());
//                        JsonObject jsonObj = new JsonObject();
//                        jsonObj.addProperty("warning", msg);
//                        outputStream.write(jsonObj.toString().getBytes());
//                        byte[] dst = new byte[256];
//                        String foo = "!warning!";
//                        byte[] src = foo.getBytes();
////                        dst[0] = src.length;
//                        System.arraycopy(src, 0, dst, 0, src.length);
//                        outputStream.flush();
                        outputStream.write("!warning!".getBytes());
                        outputStream.flush();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (Thread.currentThread().isInterrupted())
                        break;
                }
            } else {
                return;
            }
        }
    }
    public void sendBroadcast(String msg) {
        Thread mThread = new BroadcastSender(msg);
        mThread.run();
    }
}