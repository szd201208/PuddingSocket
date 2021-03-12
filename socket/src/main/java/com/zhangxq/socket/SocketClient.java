package com.zhangxq.socket;

import android.graphics.Bitmap;
import android.net.Uri;

import java.io.File;

public interface SocketClient {
    void connect(String host,int port); //socket connet
    void send(String send);//send
    void sendFile(Bitmap bitmap);// send file
    boolean isconnect();
    void setResultCallback(SocketResultCallback callback);
}
