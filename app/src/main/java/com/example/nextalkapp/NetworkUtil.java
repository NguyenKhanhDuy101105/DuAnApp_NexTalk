package com.example.nextalkapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkUtil {
    /**
     * Kiểm tra trạng thái kết nối mạng của thiết bị.
     * Sử dụng NetworkInfo để phản hồi nhanh hơn với các thay đổi vật lý (tắt/mở Wi-Fi).
     */
    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }
}
