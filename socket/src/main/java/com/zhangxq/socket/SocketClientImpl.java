package com.zhangxq.socket;

import android.graphics.Bitmap;
import android.os.MessageQueue;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SocketClientImpl implements SocketClient {

    private static final String TAG = "SocketClientImpl";

    private Socket mSocket;
    private InputStream mSocketInputStream;
    private OutputStream mSocketOutputStream;
    private SocketResultCallback mResultCallback;
    private ThreadPoolExecutor mSocketThreadPool = new ThreadPoolExecutor(0, 1, 1, TimeUnit.HOURS, new LinkedBlockingDeque<Runnable>());

    @Override
    public void connect(final String host, final int port) {
        mSocketThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                initsocket(host, port);
            }
        });
    }

    private void initsocket(String host, int port) {
        try {
            mSocket = new Socket();
            mSocket.connect(new InetSocketAddress(host, port));
            mSocketInputStream = mSocket.getInputStream();
            mSocketOutputStream = mSocket.getOutputStream();
            readSocket();
            initWriteSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void send(String send) {
        mWriteBlockingQueue.add(new SocketMessage(0,send.getBytes()));
    }

    @Override
    public void sendFile(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] result = baos.toByteArray();
        mWriteBlockingQueue.add(new SocketMessage(1,result));
    }

    private byte[] file2byte(File tradeFile) {
        byte[] buffer = null;
        try {
            FileInputStream fis = new FileInputStream(tradeFile);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }

    @Override
    public boolean isconnect() {
        return mSocket != null && mSocket.isConnected();
    }

    @Override
    public void setResultCallback(SocketResultCallback callback) {
        this.mResultCallback = callback;
    }


    private LinkedBlockingDeque<SocketMessage> mWriteBlockingQueue = new LinkedBlockingDeque<>();

    private void initWriteSocket() {
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        SocketMessage queueMessage = mWriteBlockingQueue.take();
                        if (queueMessage != null) {
                            Log.e(TAG, "write ---> " + queueMessage);
                            byte[] result = queueMessage.getResult();
                            mSocketOutputStream.write(result);
                            mSocketOutputStream.flush();
                        }
                        Thread.sleep(200);
                    } catch (InterruptedException e) {  //queue.take(),thread.sleep
                        e.printStackTrace();
                    } catch (IOException e) {  //string.getBytes("UTF-8")
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private void readSocket() {
        new Thread() {
            @Override
            public void run() {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(mSocketInputStream));
                try {
                    String readlineStr = bufferedReader.readLine();
                    Log.e(TAG, "read ---> " + readlineStr);
                    if (mResultCallback != null) {
                        mResultCallback.onReceived(readlineStr);
                    }
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void readByteaArraySocket() {
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    byte[] bytes = new byte[1024];
                    try {
                        int length = mSocketInputStream.read(bytes);
                        StringBuffer stringBuffer = new StringBuffer();
                        while (length != -1) {
                            stringBuffer.append(bytes);
                        }
                        if (mResultCallback != null) {
                            mResultCallback.onReceived(stringBuffer.toString());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }
}
