package com.yjz.recordbutton;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * demo
 *
 * @author lizheng
 *         created at 2018/9/27 上午9:14
 */

public class MainActivity extends AppCompatActivity implements RecorderButton.OnFinishedRecordListener, CheckRecordPermissionListener {

    private RecorderButton recorderButton;
    private RelativeLayout rlyVoice;
    private ImageView iconVoice;
    private TextView tvVoiceLong;

    protected String finishAudioPath;
    protected String finishTimeLong;
    private static final int REQUEST_PERMISSION = 0x007;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recorderButton = findViewById(R.id.recorder_button);
        rlyVoice = findViewById(R.id.rly_voice);
        iconVoice = findViewById(R.id.icon_voice);
        tvVoiceLong = findViewById(R.id.tv_voice_long);

        recorderButton.setOnFinishedRecordListener(this);
        recorderButton.setCheckRecordPermissionListener(this);
        rlyVoice.setVisibility(View.GONE);
    }

    @Override
    public void onFinishedRecord(String audioPath, String voiceLong) {
        finishAudioPath = audioPath;
        finishTimeLong = voiceLong;
        rlyVoice.setVisibility(View.VISIBLE);
        tvVoiceLong.setText(voiceLong + "''");

        rlyVoice.setOnClickListener(new VoiceRecordPlayClickListener(MainActivity.this, finishAudioPath, iconVoice, finishAudioPath));

        Toast.makeText(this, "录音完成,路径:" + audioPath + "==录音时长:" + voiceLong, Toast.LENGTH_SHORT).show();
    }

    /**
     * 请求权限
     */
    public void checkPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this, "权限被禁止,无法录音", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void checkRecordPermission() {
        checkPermission();
    }
}
