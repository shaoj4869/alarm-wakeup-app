package com.example.alarmwakeup;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class NotificationMonitor extends NotificationListenerService {

    private static final String TAG = "NotificationMonitor";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);

        String packageName = sbn.getPackageName();
        
        if (!isWatchedApp(packageName)) {
            return;
        }

        Log.d(TAG, "检测到通知: " + packageName);

        String senderName = extractSenderName(sbn);
        String messageContent = extractMessageContent(sbn);

        Log.d(TAG, "发送者: " + senderName);
        Log.d(TAG, "消息内容: " + messageContent);

        if (isInEarlyMorning()) {
            if (isWatchedContact(senderName, messageContent)) {
                Log.d(TAG, "触发闹钟唤醒");
                triggerAlarmWakeup(senderName, messageContent, packageName);
            }
        }
    }

    private boolean isWatchedApp(String packageName) {
        SharedPreferences prefs = getSharedPreferences("AlarmWakeup", Context.MODE_PRIVATE);
        Set<String> watchedApps = prefs.getStringSet("watchedApps", new HashSet<>());
        
        if (watchedApps.isEmpty()) {
            return true;
        }
        
        return watchedApps.contains(packageName);
    }

    private String extractSenderName(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;

        String title = extras.getString(Notification.EXTRA_TITLE);
        if (title != null && !title.isEmpty()) {
            return title;
        }

        return "未知";
    }

    private String extractMessageContent(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;

        String text = extras.getString(Notification.EXTRA_TEXT);
        if (text != null) {
            return text;
        }

        CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (lines != null && lines.length > 0) {
            return lines[0].toString();
        }

        return "";
    }

    private boolean isInEarlyMorning() {
        SharedPreferences prefs = getSharedPreferences("AlarmWakeup", Context.MODE_PRIVATE);
        int startHour = prefs.getInt("startHour", 23);
        int endHour = prefs.getInt("endHour", 6);

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY);

        if (startHour >= endHour) {
            return currentHour >= startHour || currentHour < endHour;
        } else {
            return currentHour >= startHour && currentHour < endHour;
        }
    }

    private boolean isWatchedContact(String senderName, String messageContent) {
        SharedPreferences prefs = getSharedPreferences("AlarmWakeup", Context.MODE_PRIVATE);

        boolean watchAll = prefs.getBoolean("watchAll", false);
        if (watchAll) {
            return true;
        }

        String watchedNames = prefs.getString("watchedNames", "");
        if (watchedNames.isEmpty()) {
            return true;
        }

        String[] names = watchedNames.split(",");
        for (String name : names) {
            name = name.trim();
            if (senderName.contains(name) || name.contains(senderName)) {
                return true;
            }
        }

        String watchedKeywords = prefs.getString("watchedKeywords", "");
        if (!watchedKeywords.isEmpty() && messageContent != null) {
            String[] keywords = watchedKeywords.split(",");
            for (String keyword : keywords) {
                keyword = keyword.trim();
                if (messageContent.contains(keyword)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void triggerAlarmWakeup(String senderName, String messageContent, String packageName) {
        Intent intent = new Intent(this, AlarmActivity.class);
        intent.setAction("com.example.alarmwakeup.ALARM_ACTION");
        intent.putExtra("sender", senderName);
        intent.putExtra("message", messageContent);
        intent.putExtra("package", packageName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.d(TAG, "通知监听服务已连接");
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.d(TAG, "通知监听服务已断开");
    }
}