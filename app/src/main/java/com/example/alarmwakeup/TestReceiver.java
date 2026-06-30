package com.example.alarmwakeup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TestReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String sender = intent.getStringExtra("sender");
        String message = intent.getStringExtra("message");
        String packageName = intent.getStringExtra("package");

        if (sender == null) sender = "测试用户";
        if (message == null) message = "测试消息";
        if (packageName == null) packageName = "com.xingin.xhs";

        Intent alarmIntent = new Intent(context, AlarmActivity.class);
        alarmIntent.setAction("com.example.alarmwakeup.ALARM_ACTION");
        alarmIntent.putExtra("sender", sender);
        alarmIntent.putExtra("message", message);
        alarmIntent.putExtra("package", packageName);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(alarmIntent);
    }
}