package com.example.ptt;

import java.nio.ByteBuffer;

/**
 * 解码后的数据回调
 */
public interface IPlayCallback {
    void onPlay(ByteBuffer buffer, int size);
}
