package com.janeho.app.client;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;

public class BufferManager extends Thread {
    private ImageBuffer[] mBufferQueue;
    private int mFillCount = 0;
    private final int mFrameLength;
    private int mRemained = 0;
    private static final int MAX_BUFFER_COUNT = 2;
    private int mWidth, mHeight, mOrientation;
    private LinkedList<byte[]> mYUVQueue = new LinkedList<byte[]>();
    private DataListener mListener;

    public BufferManager(int frameLength, int width, int height, int orientation) {
        mWidth = width;
        mHeight = height;
        mOrientation = orientation;
        mFrameLength = frameLength;
        mBufferQueue = new ImageBuffer[MAX_BUFFER_COUNT];
        for (int i = 0; i < MAX_BUFFER_COUNT; ++i) {
            mBufferQueue[i] = new ImageBuffer(mFrameLength, width, height);
        }
    }

    public void fillBuffer(byte[] data, int len) {
        mFillCount = mFillCount % MAX_BUFFER_COUNT;
        if (mRemained != 0) {
            if (mRemained < len) {
                mBufferQueue[mFillCount].fillBuffer(data, 0, mRemained, mYUVQueue);
                ++mFillCount;
                if (mFillCount == MAX_BUFFER_COUNT)
                    mFillCount = 0;
                mBufferQueue[mFillCount].fillBuffer(data, mRemained, len - mRemained, mYUVQueue);
                mRemained = mFrameLength - len + mRemained;
            } else if (mRemained == len) {
                mBufferQueue[mFillCount].fillBuffer(data, 0, mRemained, mYUVQueue);
                mRemained = 0;
                ++mFillCount;
                if (mFillCount == MAX_BUFFER_COUNT)
                    mFillCount = 0;
            } else {
                mBufferQueue[mFillCount].fillBuffer(data, 0, len, mYUVQueue);
                mRemained = mRemained - len;
            }
        } else {
            mBufferQueue[mFillCount].fillBuffer(data, 0, len, mYUVQueue);

            if (len < mFrameLength) {
                mRemained = mFrameLength - len;
            } else {
                ++mFillCount;
                if (mFillCount == MAX_BUFFER_COUNT)
                    mFillCount = 0;
            }
        }
    }

    public void setOnDataListener(DataListener listener) {
        mListener = listener;
        start();
    }

    public void close() {
        mListener.onDisconnect();
        mYUVQueue.clear();
        interrupt();
        try {
            join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        super.run();

        while (!Thread.currentThread().isInterrupted()) {
            byte[] data = null;
            synchronized (mYUVQueue) {
                data = mYUVQueue.poll();

                if (data != null) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, mWidth, mHeight, null);
                    yuvImage.compressToJpeg(new Rect(0, 0, mWidth, mHeight), 50, out);
                    byte[] imageBytes = out.toByteArray();

                    Matrix matrix = new Matrix();
                    matrix.postRotate(mOrientation);
                    Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    image =  Bitmap.createBitmap(image, 0 , 0, image.getWidth(), image.getHeight(), matrix, true);

                    mListener.onDirty(image);
//                    System.out.println("time cost = " + (System.currentTimeMillis() - t));
                }

            }
        }
    }
}
