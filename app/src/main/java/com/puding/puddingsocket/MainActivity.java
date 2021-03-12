package com.puding.puddingsocket;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SocketTestActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "SocketTestActivity";
    private List<String> mServers = new ArrayList<String>();
    private List<String> mClients1 = new ArrayList<>();
    private List<String> mClients2 = new ArrayList<>();
    private EditText et_client1, et_client2;
    private SocketClient socketClient1, socketClient2;
    private RecyclerView client_recyclerview1, client_recyclerview2;
    private SimpleStringdapter mServerAdapter, mClientAdapter1, mClientAdapter2;
    private ServerSocket serverSocket;
    private ImageView test_server;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn_start_server = findViewById(R.id.btn_start_server);
        test_server = findViewById(R.id.test_server);

        RecyclerView server_recyclerview = findViewById(R.id.server_recyclerview);
        mServerAdapter = new SimpleStringdapter(this, mServers);
        server_recyclerview.setLayoutManager(new LinearLayoutManager(SocketTestActivity.this, LinearLayoutManager.VERTICAL, false));
        server_recyclerview.setAdapter(mServerAdapter);
        btn_start_server.setOnClickListener(this);
        //
        Button btn_start_client1 = findViewById(R.id.btn_start_client1);
        findViewById(R.id.btn_send_file).setOnClickListener(this);
        et_client1 = findViewById(R.id.et_client1);
        Button btn_send1 = findViewById(R.id.btn_send1);
        client_recyclerview1 = findViewById(R.id.client_recyclerview1);
        Button btn_start_client2 = findViewById(R.id.btn_start_client2);
        et_client2 = findViewById(R.id.et_client2);
        Button btn_send2 = findViewById(R.id.btn_send2);
        client_recyclerview2 = findViewById(R.id.client_recyclerview2);
        //
        btn_start_client1.setOnClickListener(this);
        btn_send1.setOnClickListener(this);
        mClientAdapter1 = new SimpleStringdapter(this, mClients1);
        client_recyclerview1.setLayoutManager(new LinearLayoutManager(SocketTestActivity.this, LinearLayoutManager.VERTICAL, false));
        client_recyclerview1.setAdapter(mClientAdapter1);
        btn_start_client2.setOnClickListener(this);
        //
        btn_send2.setOnClickListener(this);
        mClientAdapter2 = new SimpleStringdapter(this, mClients2);
        client_recyclerview2.setLayoutManager(new LinearLayoutManager(SocketTestActivity.this, LinearLayoutManager.VERTICAL, false));
        client_recyclerview2.setAdapter(new SimpleStringdapter(this, mClients2));
        //
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        mServerAdapter.notifyDataSetChanged();
                        Log.e(TAG, "server  start33333 ");
                        break;
                }
            }
        };
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start_server:
                startByteArrayServer();
                break;
            case R.id.btn_start_client1:
                socketClient1 = new SocketClientImpl();
                socketClient1.connect("127.0.0.1", 8080);
                socketClient1.setResultCallback(new SocketResultCallback() {
                    @Override
                    public void onReceived(String result) {
                        Log.e(TAG, "client1  received  ->  " + result);
                        mClients1.add(result);
                        mClientAdapter1.notifyDataSetChanged();
                    }
                });
                break;
            case R.id.btn_send1:
                String result = et_client1.getText().toString().trim();
                if (TextUtils.isEmpty(result)) {
                    Toast.makeText(SocketTestActivity.this, "请输入内容", Toast.LENGTH_LONG).show();
                } else {
                    if (socketClient1 == null) {
                        Toast.makeText(SocketTestActivity.this, "请连接socket", Toast.LENGTH_LONG).show();
                    } else {
                        socketClient1.send(result);
                        Log.e(TAG, "client1  send  -> " + result);
                        et_client1.setText("");
                    }
                }
                break;
            case R.id.btn_send_file:
                Bitmap bitmap=getBytearrayWithRes();
                socketClient1.sendFile(bitmap);
                break;
            case R.id.btn_start_client2:
                socketClient2 = new SocketClientImpl();
                socketClient2.connect("127.0.0.1", 8080);
                socketClient2.setResultCallback(new SocketResultCallback() {
                    @Override
                    public void onReceived(String result) {
                        Log.e(TAG, "client2  received  ->  " + result);
                        mClients2.add(result);
                        mClientAdapter2.notifyDataSetChanged();
                    }
                });
                break;
            case R.id.btn_send2:
                String result2 = et_client2.getText().toString().trim();
                if (TextUtils.isEmpty(result2)) {
                    Toast.makeText(SocketTestActivity.this, "请输入内容", Toast.LENGTH_LONG).show();
                } else {
                    if (socketClient2 == null) {
                        Toast.makeText(SocketTestActivity.this, "请连接socket", Toast.LENGTH_LONG).show();
                    } else {
                        socketClient2.send(result2);
                        Log.e(TAG, "client2  send  -> " + result2);
                        et_client2.setText("");
                    }
                }
                break;
        }
    }

    private Bitmap getBytearrayWithRes(){
        Drawable drawable= getResources().getDrawable(R.drawable.ic_launcher_background);
        Bitmap bitmap = Bitmap.createBitmap( drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    //调用相册
    private void openAlbum() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 100);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            if (data != null) {
                // 照片的原始资源地址
                Uri uri = data.getData();
                File file = getFileByUri(uri);
//                socketClient2.sendFile(file);
            }
        }
    }

    private File getFileByUri(Uri uri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, proj, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(proj[0]);
        String img_path = cursor.getString(columnIndex);
        cursor.close();
        File file = new File(img_path);
        return file;
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 4];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }

    private void startServer() {
        if (serverSocket != null) return;
        new Thread() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(8080);
                    final Socket socket = serverSocket.accept();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    Log.e(TAG, "server  start ");
                    while (true) {
                        Log.e(TAG, "server  start11111 ");
                        String result = bufferedReader.readLine();
                        Log.e(TAG, "server  start22222 ");
                        mServers.add(result);
                        Log.e(TAG, "server  received -> " + result);
                        mHandler.sendEmptyMessage(1);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "server  start44444 ");
                    e.printStackTrace();
                } catch (Exception e) {
                    Log.e(TAG, "server  start6666 ");
                    e.printStackTrace();
                } finally {
                    Log.e(TAG, "server  start55555 ");
                }
            }
        }.start();
    }

    private void startByteArrayServer() {
        if (serverSocket != null) return;
        new Thread() {
            private byte[] resultBytes;

            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(8080);
                    final Socket socket = serverSocket.accept();
                    Log.e(TAG, "server  start ");
                    InputStream inputStream = socket.getInputStream();
                    while (true) {
                        Log.e(TAG, "server  start11111 ");
                        byte[] resultType = getBytesWithLength(inputStream, 1);
                        byte[] resultLength = getBytesWithLength(inputStream, 4);
                        if (resultLength != null) {
                            int length = big_bytesToInt(resultLength);
                            int type = big_bytesToInt(resultType);
                            resultBytes = getBytesWithLength(inputStream, length);
                            if (resultBytes != null) {
                                if (type == 0) {
                                    String res = new String(resultBytes);
                                    Log.e(TAG, "server  start22222 :" + res);
                                    mServers.add(res);
                                } else if (type == 1) {
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(resultBytes, 0, resultBytes.length);
                                    test_server.setImageBitmap(bitmap);
                                }
                                mHandler.sendEmptyMessage(1);
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "server  start44444 ");
                    e.printStackTrace();
                } catch (
                        Exception e) {
                    Log.e(TAG, "server  start6666 ");
                    e.printStackTrace();
                } finally {
                    Log.e(TAG, "server  start55555 ");
                }
            }
        }.start();
    }

    private File byte2File(byte[] buf, String filePath, String fileName) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        File file = null;
        try {
            File dir = new File(filePath);
            if (!dir.exists() && dir.isDirectory()) {
                dir.mkdirs();
            }
            file = new File(filePath + File.separator + fileName);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(buf);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return file;
    }

    //根据4个包头，获取包体长度
    private byte[] getBytesWithLength(InputStream inputStream, int length) {
        byte[] bytes = new byte[length];
        try {
            inputStream.read(bytes, 0, length);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return bytes;
        }
    }

    private int big_bytesToInt(byte[] bytes) {
        int addr = 0;
        if (bytes.length == 1) {
            addr = bytes[0] & 0xFF;
        } else if (bytes.length == 2) {
            addr = bytes[0] & 0xFF;
            addr = (addr << 8) | (bytes[1] & 0xff);
        } else {
            addr = bytes[0] & 0xFF;
            addr = (addr << 8) | (bytes[1] & 0xff);
            addr = (addr << 8) | (bytes[2] & 0xff);
            addr = (addr << 8) | (bytes[3] & 0xff);
        }
        return addr;
    }

    private Handler mHandler;
}
