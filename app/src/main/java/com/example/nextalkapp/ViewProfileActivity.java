package com.example.nextalkapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.nextalkapp.Model.User;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ViewProfileActivity extends AppCompatActivity {

    private ImageView imgViewAvatar;
    private TextView tvViewName, tvViewPhone, tvViewBio;
    private MaterialButton btnSendMessage;
    private String receiverUid;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);

        receiverUid = getIntent().getStringExtra("receiverUid");

        mapping();
        loadUserData();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        btnSendMessage.setOnClickListener(v -> {
            // Quay lại màn hình chat nếu đã có receiverUid
            finish();
        });
    }

    private void mapping() {
        imgViewAvatar = findViewById(R.id.imgViewAvatar);
        tvViewName = findViewById(R.id.tvViewName);
        tvViewPhone = findViewById(R.id.tvViewPhone);
        tvViewBio = findViewById(R.id.tvViewBio);
        btnSendMessage = findViewById(R.id.btnSendMessage);
    }

    private void loadUserData() {
        if (receiverUid == null) return;

        dbRef = FirebaseDatabase.getInstance().getReference("users").child(receiverUid);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    tvViewName.setText(user.getName() != null ? user.getName() : "Người dùng NexTalk");
                    tvViewPhone.setText(user.getPhone() != null ? user.getPhone() : "Chưa có số điện thoại");
                    tvViewBio.setText(user.getBio() != null && !user.getBio().isEmpty() ? user.getBio() : "Chưa có lời giới thiệu nào.");

                    if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                        Glide.with(ViewProfileActivity.this)
                                .load(user.getAvatar())
                                .placeholder(R.drawable.logo2)
                                .into(imgViewAvatar);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
}
