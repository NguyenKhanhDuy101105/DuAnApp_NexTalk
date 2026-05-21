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

        // --- ĐÃ THÊM: KHÔNG CHO PHÉP HIỂN THỊ THÔNG BÁO TỰ ĐỘNG KHI APP ĐANG MỞ ---
        // Hoặc cấu hình chế độ hiển thị Notification để tránh xung đột với API Volley
        OneSignal.setNotificationWillShowInForegroundHandler(notificationReceivedEvent -> {
            // Nếu muốn ẩn hoàn toàn thông báo tự động trùng lặp khi đang mở app, chọn null
            // Hoặc kiểm tra dữ liệu để đưa ra quyết định hiển thị
            notificationReceivedEvent.complete(null);
        });
    }
}