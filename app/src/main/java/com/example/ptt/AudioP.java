package com.example.ptt;

import android.media.AudioManager;
import android.media.AudioTrack;

import java.nio.ByteBuffer;

public class AudioP implements IPlayCallback{
    private AudioTrack mAudioTrack = null;
    private boolean isPlaying = false;

    /**
     * 开始播放
     */
    public synchronized void startPlay(){
        if (null == mAudioTrack){
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    Utils.mSampleRate,
                    Utils.mChannels,
                    Utils.mLsbDepth,
                    Utils.mBufferSize,
                    AudioTrack.MODE_STREAM);
        }
        // 设置数据回调
        Opus.getInstance().setCallback(this);
        // 开始播放
        mAudioTrack.play();
        isPlaying = true;
    }

    /**
     * 接收到解码后的PCM数据
     * @param buffer    解码后的pcm数据
     * @param size      实际数据长度
     */
    @Override
    public void onPlay(ByteBuffer buffer, int size) {
        if (null == mAudioTrack || size == 0 || !isPlaying){
            return ;
        }
        // 播放解码后的pcm
        mAudioTrack.write(buffer.array(), buffer.arrayOffset(), size);
    }
    /**
     * 停止播放
     */
    public synchronized void stopPlay(){
        if (null != mAudioTrack){
            mAudioTrack.stop();
        }
        isPlaying = false;
        mAudioTrack = null;
    }

    // 单例相关
    private static class SingletonHolder{
        private static final AudioP INSTANCE = new AudioP();
    }
    private AudioP(){}
    public static AudioP getInstance(){
        return AudioP.SingletonHolder.INSTANCE;
    }
}
