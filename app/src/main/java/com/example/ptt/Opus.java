package com.example.ptt;

import java.nio.ByteBuffer;

/**
 * 编解码
 */
public class Opus {
    static {
        System.loadLibrary("ptt");
    }
    private ByteBuffer mEncodeDirectBuffer;   // 编码时使
    private ByteBuffer mDecodeDirectBuffer;   // 解码时使用

    private IPlayCallback mCallback;    // 回调，用于通知播放器播放数据

    /**
     * 初始化方法
     * @param sample_rate  采样率
     * @param channels     通道数
     * @param lsb_depth    量化位数
     */
    public void init(int sample_rate, int channels, int lsb_depth){
        mEncodeDirectBuffer = getEncodeDirectBuffer();
        mDecodeDirectBuffer = getDecodeDirectBuffer();
        opus_init(sample_rate, channels, lsb_depth);
    }

    public ByteBuffer getmEncodeDirectBuffer() {
        return mEncodeDirectBuffer;
    }

    public ByteBuffer getmDecodeDirectBuffer() {
        return mDecodeDirectBuffer;
    }

    /**
     * opus引擎初始化
     * @param sample_rate               采样率
     * @param channels                  通道数
     * @param lsb_depth                 量化位数
     */
    private native void opus_init(int sample_rate, int channels, int lsb_depth);

    /**
     * 网络初始化
     * @param local_ip                本机IP
     * @param local_port              本机端口
     * @param remote_ip               对端IP
     * @param remote_port             对端端口
     */
    public native void init_network(String local_ip, int local_port, String remote_ip, int remote_port);

    /**
     * 获取编码用缓冲区引用
     * @return DirectBuffer引用
     */
    private native ByteBuffer getEncodeDirectBuffer();

    /**
     * 获取解码用缓冲区引用
     * @return DirectBuffer引用
     */
    private native ByteBuffer getDecodeDirectBuffer();

    /**
     * 编码一帧数据
     */
    public native void opus_encoder();
    public native void opus_byte_encoder(byte[] bytes, int length);

    /**
     * 给播放数据的类使用
     * @param mCallback   callback
     */
    public void setCallback(IPlayCallback mCallback) {
        this.mCallback = mCallback;
    }

    /**
     * 接收解码后的数据
     */
    public void onPcmCallback(int size){
        if (mCallback != null){
            mCallback.onPlay(mDecodeDirectBuffer, size);
        }
    }

    // 单例相关
    private static class SingletonHolder{
        private static final Opus INSTANCE = new Opus();
    }
    private Opus(){}
    public static Opus getInstance(){
        return Opus.SingletonHolder.INSTANCE;
    }
}
