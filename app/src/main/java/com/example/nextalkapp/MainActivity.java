package com.example.nextalkapp;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;

import com.onesignal.OneSignal;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private DatabaseReference statusRef;
    private BottomNavigationView bottomNav;
    private ViewPager2 viewPager;
    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mapping();

        // Xử lý Insets cho Layout cha
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // Xử lý Insets cho thanh Bottom Navigation
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(2);

        // 1. Xử lý khi Người dùng CLICK vào Bottom Navigation
        bottomNav.setOnItemSelectedListener(item -> {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }

            setSwipeEnabled(true);

            if (item.getItemId() == R.id.nav_chats) {
                viewPager.setCurrentItem(0, true);
            } else if (item.getItemId() == R.id.nav_friends) {
                viewPager.setCurrentItem(1, true);
            } else if (item.getItemId() == R.id.nav_profile) {
                viewPager.setCurrentItem(2, true);
            }
            return true;
        });

        // 2. Xử lý khi Người dùng VUỐT màn hình sang trái/phải
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                switch (position) {
                    case 0:
                        bottomNav.setSelectedItemId(R.id.nav_chats);
                        break;
                    case 1:
                        bottomNav.setSelectedItemId(R.id.nav_friends);
                        break;
                    case 2:
                        bottomNav.setSelectedItemId(R.id.nav_profile);
                        break;
                }
            }
        });

        // Đăng ký Receiver kết nối mạng
        networkChangeReceiver = new NetworkChangeReceiver();
        registerReceiver(networkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        if (NetworkUtil.isConnected(this)) {
            NetworkChangeReceiver.syncMessages(this);
        }

        // XỬ LÝ KHI NHẤN VÀO THÔNG BÁO
        OneSignal.setNotificationOpenedHandler(result -> {
            JSONObject data = result.getNotification().getAdditionalData();
            if (data != null && data.has("senderUid")) {
                String senderUid = data.optString("senderUid");
                String senderName = data.optString("senderName");

                Intent intent = new Intent(MainActivity.this, MessageActivity.class);
                intent.putExtra("receiverUid", senderUid);
                intent.putExtra("receiverName", senderName);
                // Đảm bảo Activity được đưa lên phía trước và không khởi tạo lại nếu đang chạy
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        SharedPreferences prefs = getSharedPreferences("USER", MODE_PRIVATE);
        String uid = prefs.getString("uid", null);

        if (uid != null) {
            statusRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("status");
            statusRef.setValue("online");
            statusRef.onDisconnect().setValue("offline");

            // Rất quan quan trọng: Định danh thiết bị để nhận thông báo
            OneSignal.setExternalUserId(uid);

            com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) return;
                        String token = task.getResult();
                        FirebaseDatabase.getInstance().getReference("users").child(uid)
                                .child("fcmToken").setValue(token);
                    });
        }

        askNotificationPermission();
    }

    public void setSwipeEnabled(boolean enabled) {
        if (viewPager != null) {
            viewPager.setUserInputEnabled(enabled);
        }

        View mainHolder = findViewById(R.id.main_holder);
        if (mainHolder != null) {
            if (enabled) {
                mainHolder.setVisibility(View.GONE);
            } else {
                mainHolder.setVisibility(View.VISIBLE);
            }
        }
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
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
        viewPager = findViewById(R.id.view_pager);
    }
}