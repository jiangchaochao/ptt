package com.example.ptt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ptt.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private String mLocalIp;

    private boolean isInitSuccess = false;

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mLocalIp = Utils.getWiFiIPAddress(this);
        binding.tvLocalIp.setText("本机IP:" + mLocalIp);
        if (ContextCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.RECORD_AUDIO", "android.permission.ACCESS_WIFI_STATE."}, 0);
        }
        // 初始化参数
        binding.btnInit.setOnClickListener(v -> {
            if (!isInitSuccess) {
                init();
            }
        });
        binding.btnPtt.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    binding.tvTips.setText("正在讲话");
                    AudioR.getInstance().resumeRecording();
                    break;
                case MotionEvent.ACTION_UP:
                    binding.tvTips.setText("按下讲话");
                    AudioR.getInstance().pauseRecording();
                    break;
            }
            return false;
        });
    }


    private void init() {
        if (!Utils.ipValidate(binding.tvRemoteIp.getText().toString())) {
            Toast.makeText(this, "ip地址不合法", Toast.LENGTH_SHORT).show();
            return;
        }
        // 1. 初始化网络参数
        Opus.getInstance().init_network(mLocalIp, Utils.mDefaultPort, binding.tvRemoteIp.getText().toString(), Utils.mDefaultPort);
        // 2. 初始化编解码参数
        Opus.getInstance().init(Utils.mSampleRate, 2, 16);
        isInitSuccess = true;
        binding.btnInit.setClickable(false);
        // 初始化录音
        AudioR.getInstance().startRecording();
        Toast.makeText(MainActivity.this, "初始化成功", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "onRequestPermissionsResult: 录音权限被允许");
            } else {
                Log.e(TAG, "onRequestPermissionsResult: 没有录音权限");
            }
        }
    }
}