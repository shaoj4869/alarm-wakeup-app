package com.example.alarmwakeup;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS =
        "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    private EditText startHourInput;
    private EditText endHourInput;
    private EditText watchedNamesInput;
    private EditText watchedKeywordsInput;
    private CheckBox watchAllCheckBox;
    private Button saveButton;
    private Button checkPermissionButton;
    private Button startServiceButton;
    private Button selectAppsButton;
    private Button testAlarmButton;

    private Map<String, String> commonApps = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startHourInput = findViewById(R.id.start_hour);
        endHourInput = findViewById(R.id.end_hour);
        watchedNamesInput = findViewById(R.id.watched_names);
        watchedKeywordsInput = findViewById(R.id.watched_keywords);
        watchAllCheckBox = findViewById(R.id.watch_all);
        saveButton = findViewById(R.id.save_button);
        checkPermissionButton = findViewById(R.id.check_permission_button);
        startServiceButton = findViewById(R.id.start_service_button);
        selectAppsButton = findViewById(R.id.select_apps_button);
        testAlarmButton = findViewById(R.id.test_alarm_button);

        initCommonApps();
        loadSettings();

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });

        checkPermissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkNotificationPermission();
            }
        });

        startServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAlarmService();
                requestBatteryOptimization();
            }
        });

        selectAppsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAppSelectionDialog();
            }
        });

        testAlarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testAlarm();
            }
        });

        watchAllCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            watchedNamesInput.setEnabled(!isChecked);
            watchedKeywordsInput.setEnabled(!isChecked);
        });
    }

    private void initCommonApps() {
        commonApps.put("com.tencent.mobileqq", "QQ");
        commonApps.put("com.tencent.mm", "微信");
        commonApps.put("com.xingin.xhs", "小红书");
        commonApps.put("com.ss.android.ugc.aweme", "抖音");
        commonApps.put("com.sina.weibo", "微博");
        commonApps.put("com.taobao.taobao", "淘宝");
        commonApps.put("com.jingdong.app.mall", "京东");
        commonApps.put("com.meituan.android.pt.homepage", "美团");
        commonApps.put("com.bytedance.douyin", "TikTok");
        commonApps.put("com.alibaba.android.rimet", "钉钉");
        commonApps.put("com.microsoft.teams", "Teams");
        commonApps.put("com.google.android.gm", "Gmail");
        commonApps.put("com.google.android.apps.messaging", "短信");
        commonApps.put("com.whatsapp", "WhatsApp");
        commonApps.put("com.line.android", "LINE");
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences("AlarmWakeup", Context.MODE_PRIVATE);
        startHourInput.setText(String.valueOf(prefs.getInt("startHour", 23)));
        endHourInput.setText(String.valueOf(prefs.getInt("endHour", 6)));
        watchedNamesInput.setText(prefs.getString("watchedNames", ""));
        watchedKeywordsInput.setText(prefs.getString("watchedKeywords", ""));
        watchAllCheckBox.setChecked(prefs.getBoolean("watchAll", false));
    }

    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences("AlarmWakeup", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        try {
            editor.putInt("startHour", Integer.parseInt(startHourInput.getText().toString()));
            editor.putInt("endHour", Integer.parseInt(endHourInput.getText().toString()));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的小时数", Toast.LENGTH_SHORT).show();
            return;
        }

        editor.putString("watchedNames", watchedNamesInput.getText().toString());
        editor.putString("watchedKeywords", watchedKeywordsInput.getText().toString());
        editor.putBoolean("watchAll", watchAllCheckBox.isChecked());
        editor.apply();

        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
    }

    private void checkNotificationPermission() {
        ComponentName cn = new ComponentName(this, NotificationMonitor.class);
        String flat = Settings.Secure.getString(getContentResolver(), ENABLED_NOTIFICATION_LISTENERS);
        boolean enabled = flat != null && flat.contains(cn.flattenToString());

        if (!enabled) {
            new AlertDialog.Builder(this)
                .setTitle("需要通知权限")
                .setMessage("请在设置中允许本应用监听通知")
                .setPositiveButton("前往设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    }
                })
                .setNegativeButton("取消", null)
                .show();
        } else {
            Toast.makeText(this, "通知权限已开启", Toast.LENGTH_SHORT).show();
        }
    }

    private void startAlarmService() {
        Intent serviceIntent = new Intent(this, AlarmService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        Toast.makeText(this, "服务已启动", Toast.LENGTH_SHORT).show();
    }

    private void requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(android.net.Uri.parse("package:" + packageName));
            startActivity(intent);
        }
    }

    private void showAppSelectionDialog() {
        SharedPreferences prefs = getSharedPreferences("AlarmWakeup", Context.MODE_PRIVATE);
        Set<String> selectedApps = prefs.getStringSet("watchedApps", new HashSet<>());

        List<String> appNames = new ArrayList<>();
        List<String> appPackages = new ArrayList<>();

        for (Map.Entry<String, String> entry : commonApps.entrySet()) {
            if (isAppInstalled(entry.getKey())) {
                appPackages.add(entry.getKey());
                appNames.add(entry.getValue());
            }
        }

        final boolean[] checkedItems = new boolean[appNames.size()];
        for (int i = 0; i < appNames.size(); i++) {
            checkedItems[i] = selectedApps.contains(appPackages.get(i));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择要监听的应用");
        builder.setMultiChoiceItems(appNames.toArray(new String[0]), checkedItems,
            new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    checkedItems[which] = isChecked;
                }
            });

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Set<String> newSelectedApps = new HashSet<>();
                for (int i = 0; i < checkedItems.length; i++) {
                    if (checkedItems[i]) {
                        newSelectedApps.add(appPackages.get(i));
                    }
                }

                SharedPreferences.Editor editor = prefs.edit();
                editor.putStringSet("watchedApps", newSelectedApps);
                editor.apply();

                String selectedNames = "";
                for (int i = 0; i < checkedItems.length; i++) {
                    if (checkedItems[i]) {
                        if (!selectedNames.isEmpty()) {
                            selectedNames += ", ";
                        }
                        selectedNames += appNames.get(i);
                    }
                }

                if (selectedNames.isEmpty()) {
                    selectedNames = "所有应用";
                }
                selectAppsButton.setText("已选: " + selectedNames);

                Toast.makeText(MainActivity.this, "应用选择已保存", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private boolean isAppInstalled(String packageName) {
        try {
            getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkNotificationPermissionStatus();
        updateSelectedAppsDisplay();
    }

    private void checkNotificationPermissionStatus() {
        ComponentName cn = new ComponentName(this, NotificationMonitor.class);
        String flat = Settings.Secure.getString(getContentResolver(), ENABLED_NOTIFICATION_LISTENERS);
        boolean enabled = flat != null && flat.contains(cn.flattenToString());

        if (enabled) {
            checkPermissionButton.setText("✓ 通知权限已开启");
        } else {
            checkPermissionButton.setText("点击开启通知权限");
        }
    }

    private void updateSelectedAppsDisplay() {
        SharedPreferences prefs = getSharedPreferences("AlarmWakeup", Context.MODE_PRIVATE);
        Set<String> selectedApps = prefs.getStringSet("watchedApps", new HashSet<>());

        if (selectedApps.isEmpty()) {
            selectAppsButton.setText("选择监听应用");
            return;
        }

        String selectedNames = "";
        for (String pkg : selectedApps) {
            String name = commonApps.get(pkg);
            if (name != null) {
                if (!selectedNames.isEmpty()) {
                    selectedNames += ", ";
                }
                selectedNames += name;
            }
        }

        if (selectedNames.isEmpty()) {
            selectedNames = "所有应用";
        }
        selectAppsButton.setText("已选: " + selectedNames);
    }

    private void testAlarm() {
        Intent intent = new Intent(this, AlarmActivity.class);
        intent.setAction("com.example.alarmwakeup.ALARM_ACTION");
        intent.putExtra("sender", "测试用户");
        intent.putExtra("message", "这是一条测试消息，用于验证闹钟唤醒功能");
        intent.putExtra("package", "com.xingin.xhs");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}