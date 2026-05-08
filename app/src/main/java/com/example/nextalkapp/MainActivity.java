package com.example.nextalkapp;

import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.content.SharedPreferences;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    // Trong lớp MainActivity
    private DatabaseReference statusRef;
    BottomNavigationView bottomNav;
    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        mapping();
        
        // Đăng ký Receiver động để bắt sự kiện mạng (Cần thiết cho Android 7.0+)
        networkChangeReceiver = new NetworkChangeReceiver();
        registerReceiver(networkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        // Kiểm tra và gửi tin nhắn chờ ngay khi vào app
        if (NetworkUtil.isConnected(this)) {
            NetworkChangeReceiver.syncMessages(this);
        }

        // Hiển thị màn hình Chat mặc định
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ChatFragment()).commit();

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_chats) {
                selectedFragment = new ChatFragment();
            } else if (item.getItemId() == R.id.nav_friends) {
                selectedFragment = new FriendFragment();
            } else if (item.getItemId() == R.id.nav_profile) {
                selectedFragment = new SettingFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        SharedPreferences prefs = getSharedPreferences("USER", MODE_PRIVATE);
        String uid = prefs.getString("uid", null);

        if (uid != null) {
            statusRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("status");

            // 1. Khi app đang mở, set online
            statusRef.setValue("online");

            // 2. Thiết lập: Khi mất kết nối (tắt app), Firebase Server tự động set offline
            statusRef.onDisconnect().setValue("offline");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkChangeReceiver != null) {
            unregisterReceiver(networkChangeReceiver);
        }
    }

    private void mapping() {
        bottomNav = findViewById(R.id.bottom_navigation);
    }
}