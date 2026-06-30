package com.example.alarmwakeup;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AlarmActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "AlarmWakeup::WakeLock"
        );
        wakeLock.acquire(10 * 60 * 1000L);

        String sender = getIntent().getStringExtra("sender");
        String message = getIntent().getStringExtra("message");
        String packageName = getIntent().getStringExtra("package");

        TextView senderText = findViewById(R.id.sender_text);
        TextView messageText = findViewById(R.id.message_text);
        Button dismissButton = findViewById(R.id.dismiss_button);

        String appName = getAppName(packageName);
        senderText.setText("来自 [" + appName + "]: " + sender);
        messageText.setText("消息: " + message);

        setupAudio();
        startAlarmSound();
        startVibration();

        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAlarm();
                finish();
            }
        });
    }

    private void setupAudio() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        int targetVolume = (int) (maxVolume * 0.4);
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0);
    }

    private void startAlarmSound() {
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
            try {
                mediaPlayer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void startVibration() {
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 1000, 500, 1000};
            vibrator.vibrate(pattern, 0);
        }
    }

    private void stopAlarm() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
        if (audioManager != null) {
            audioManager.abandonAudioFocus(null);
        }
    }

    private String getAppName(String packageName) {
        if (packageName == null) {
            return "未知应用";
        }
        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(packageName, 0);
            return getPackageManager().getApplicationLabel(info).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    @Override
    protected void onDestroy() {
        stopAlarm();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
    }
}