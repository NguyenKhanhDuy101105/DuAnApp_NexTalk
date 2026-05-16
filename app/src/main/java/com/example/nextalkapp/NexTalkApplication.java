package com.example.nextalkapp;

import android.app.Application;
import com.onesignal.OneSignal;

public class NexTalkApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Đưa cấu hình OneSignal lên lớp Application cao nhất
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
        OneSignal.initWithContext(this);
        OneSignal.setAppId("3f1507b8-f3c0-417a-a700-8e70612a17bd");
    }
}