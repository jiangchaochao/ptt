package com.example.ptt;

import android.content.Context;
import android.media.AudioFormat;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 共用参数定义
 */
public class Utils {
    public static final int mSampleRate = 8000;      // 采样率
    public static final int mLsbDepth = AudioFormat.ENCODING_PCM_16BIT;          // 量化位数
    public static final int mChannels = AudioFormat.CHANNEL_IN_STEREO;           // 通道数
    public static final int mFrameSize = 20;    // 帧size以ms为单位
    // 以每帧20ms的数据包打包
    public static final int mFrameLength = mSampleRate * mLsbDepth * 2 * mFrameSize /1000 ;  // 帧长
    public static int mBufferSize;           // 最小缓冲区大小
    public static int mDefaultPort = 5555;   // 默认端口
    // 判断IP地址的合法性
    private static final String IPADDRESS_PATTERN =
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$";

    /**
     * 获取WiFi IP地址
     * @param context 上下文
     * @return WiFi的IP地址
     */
    public static String getWiFiIPAddress(Context context) {
        WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiMgr != null && wifiMgr.getConnectionInfo() != null) {
            int ipAddress = wifiMgr.getConnectionInfo().getIpAddress();
            return Formatter.formatIpAddress(ipAddress);
        }
        return null;
    }

    /**
     * 判断IP地址的有效性
     * @param ipAddress  ip地址
     * @return true: 有效，false: 无效
     */
    public static boolean ipValidate(final String ipAddress) {
        if (ipAddress == null) {
            return false;
        }
        Pattern  pattern = Pattern.compile(IPADDRESS_PATTERN);
        Matcher matcher = pattern.matcher(ipAddress);
        return matcher.matches();
    }

}
