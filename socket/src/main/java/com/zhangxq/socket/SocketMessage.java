package com.zhangxq.socket;

public class SocketMessage {
    private byte[] type = new byte[1];//类型0：json   1:file
    private byte[] data; //socket 发送数据
    private byte[] header = new byte[4];//包头
    private byte[] result;//result=type+header+data

    public SocketMessage(int mtype, byte[] data) {
        this.data = data;
        type = big_intToByte(mtype, type.length);
        result = new byte[1+header.length + data.length];
        header = big_intToByte(data.length, header.length);
        System.arraycopy(type, 0, result, 0, type.length);
        System.arraycopy(header, 0, result, type.length, header.length);
        System.arraycopy(data, 0, result, type.length+header.length, data.length);
    }

    public byte[] getResult() {
        return result;
    }

    public void setResult(byte[] result) {
        this.result = result;
    }

    private byte[] big_intToByte(int i, int len) {
        byte[] abyte = new byte[len];
        if (len == 1) {
            abyte[0] = (byte) (0xff & i);
        } else if (len == 2) {
            abyte[0] = (byte) ((i >>> 8) & 0xff);
            abyte[1] = (byte) (i & 0xff);
        } else {
            abyte[0] = (byte) ((i >>> 24) & 0xff);
            abyte[1] = (byte) ((i >>> 16) & 0xff);
            abyte[2] = (byte) ((i >>> 8) & 0xff);
            abyte[3] = (byte) (i & 0xff);
        }
        return abyte;
    }

}
