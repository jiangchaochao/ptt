package com.example.ptt;

import android.annotation.SuppressLint;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.nio.ByteBuffer;

/**
 * 录音
 */
public class AudioR {
    private static final String TAG = "Audio";
    private AudioRecord mRecorder = null;    // 录音
    private boolean isRecording = false;     // 录音标志
    private ByteBuffer pcmBuffer = null;     // pcm缓冲区

    private static final int INIT = 0;
    private static final int START = 1;
    private static final int PAUSE = 2;
    private static final int STOP = 3;
    volatile private int status = INIT;

    @SuppressLint("MissingPermission")
    public synchronized void startRecording() {
        if (null == mRecorder) {
            // 获取缓冲区
            pcmBuffer = Opus.getInstance().getmEncodeDirectBuffer();
            // 计算缓冲区大小
            Utils.mBufferSize = AudioRecord.getMinBufferSize(Utils.mSampleRate, Utils.mChannels, Utils.mLsbDepth);
            // 创建AudioRecord
            mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    Utils.mSampleRate,
                    Utils.mChannels,
                    Utils.mLsbDepth,
                    Utils.mBufferSize
            );
        }
        if (status == INIT){
            // 开始录音
            mRecorder.startRecording();
            isRecording = true;
            // 获取录音数据
            // PCM处理线程
            Thread recordingThread = new Thread(this::pcmDataProcess, "Ptt AudioRecorder Thread");
            recordingThread.start();
        }
    }

    /**
     * 录音数据处理
     */
    private void pcmDataProcess(){
        int length = (Utils.mSampleRate * 20)/1000 * 2 * 2;
        byte[] bytes = new byte[length];
        while (isRecording){
            if (status == START) {
                // 读取PCM数据
                mRecorder.read(pcmBuffer, length);
                // 重置数据起始位
                pcmBuffer.position(0);
                // 通知编码发送
                Opus.getInstance().opus_encoder();
            }
        }
    }

    /**
     * 暂停录音
     */
    public void pauseRecording(){
        if (isRecording) {
            status = PAUSE;
        }
    }

    /**
     * 继续录音
     */
    public void resumeRecording(){
        if (isRecording) {
            status = START;
        }
    }

    /**
     * 停止录音，释放资源
     */
    public void stopRecording(){
        if (isRecording && mRecorder != null) {
            mRecorder.stop();
        }
        isRecording = false;
        status = STOP;
        mRecorder = null;
    }

    // 单例相关

    private static class SingletonHolder{
        private static final AudioR INSTANCE = new AudioR();
    }
    private AudioR(){}
    public static AudioR getInstance(){
        return SingletonHolder.INSTANCE;
    }
}
