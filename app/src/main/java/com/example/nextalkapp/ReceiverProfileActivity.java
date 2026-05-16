package com.example.nextalkapp;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class ReceiverProfileActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ImageView imgProfileAvatar;
    private TextView tvProfileName, tvViewPhone, tvViewBio;
    private View itemTheme, itemQuickReaction, itemNickname;

    private String receiverUid, receiverName, receiverAvatar, senderUid, chatRoomId;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver_profile);

        // Nhận dữ liệu cơ bản từ Intent
        receiverUid = getIntent().getStringExtra("receiverUid");
        receiverName = getIntent().getStringExtra("receiverName");
        receiverAvatar = getIntent().getStringExtra("receiverAvatar");

        SharedPreferences prefs = getSharedPreferences("USER", MODE_PRIVATE);
        senderUid = prefs.getString("uid", null);
        chatRoomId = getChatRoomId(senderUid, receiverUid);
        dbRef = FirebaseDatabase.getInstance().getReference();

        mapping();
        displayBasicInfo();
        loadFullUserData(); // Tải dữ liệu chi tiết từ Firebase
        setupEvents();
        listenForNickname();
    }

    private void mapping() {
        btnBack = findViewById(R.id.btnBack);
        imgProfileAvatar = findViewById(R.id.imgProfileAvatar);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvViewPhone = findViewById(R.id.tvViewPhone);
        tvViewBio = findViewById(R.id.tvViewBio);

        itemTheme = findViewById(R.id.itemTheme);
        itemQuickReaction = findViewById(R.id.itemQuickReaction);
        itemNickname = findViewById(R.id.itemNickname);
    }

    private void displayBasicInfo() {
        tvProfileName.setText(receiverName != null ? receiverName : "Người dùng");
        if (receiverAvatar != null && !receiverAvatar.isEmpty()) {
            Glide.with(this).load(receiverAvatar).placeholder(R.drawable.logo2).into(imgProfileAvatar);
        }
    }

    private void loadFullUserData() {
        dbRef.child("users").child(receiverUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String phone = snapshot.child("phone").getValue(String.class);
                    String bio = snapshot.child("bio").getValue(String.class);

                    if (phone != null && !phone.isEmpty()) tvViewPhone.setText(phone);
                    else tvViewPhone.setText("Chưa cập nhật");
                    
                    if (bio != null && !bio.isEmpty()) tvViewBio.setText(bio);
                    else tvViewBio.setText("Không có lời giới thiệu.");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void listenForNickname() {
        dbRef.child("nicknames").child(chatRoomId).child(receiverUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String nickname = snapshot.getValue(String.class);
                if (nickname != null && !nickname.isEmpty()) {
                    tvProfileName.setText(nickname);
                } else {
                    tvProfileName.setText(receiverName != null ? receiverName : "Người dùng");
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupEvents() {
        btnBack.setOnClickListener(v -> finish());

        itemNickname.setOnClickListener(v -> {
            Intent intent = new Intent(ReceiverProfileActivity.this, NicknameActivity.class);
            intent.putExtra("receiverUid", receiverUid);
            intent.putExtra("receiverName", receiverName);
            intent.putExtra("receiverAvatar", receiverAvatar);
            startActivity(intent);
        });

        itemTheme.setOnClickListener(v -> showThemePickerDialog());
        itemQuickReaction.setOnClickListener(v -> showReactionPickerDialog());
    }

    private void showThemePickerDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.layout_dialog_theme_picker);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        dialog.findViewById(R.id.colorBlue).setOnClickListener(v -> updateTheme("#5C8EE6", "Xanh dương", dialog));
        dialog.findViewById(R.id.colorPink).setOnClickListener(v -> updateTheme("#FF4081", "Hồng", dialog));
        dialog.findViewById(R.id.colorPurple).setOnClickListener(v -> updateTheme("#7B1FA2", "Tím", dialog));
        dialog.findViewById(R.id.colorGreen).setOnClickListener(v -> updateTheme("#4CAF50", "Xanh lá", dialog));
        dialog.findViewById(R.id.colorOrange).setOnClickListener(v -> updateTheme("#FF9800", "Cam", dialog));
        dialog.show();
    }

    private void updateTheme(String colorCode, String colorName, Dialog dialog) {
        dbRef.child("themes").child(chatRoomId).child("color").setValue(colorCode);
        SharedPreferences prefs = getSharedPreferences("USER", MODE_PRIVATE);
        String myName = prefs.getString("name", "Ai đó");
        sendSystemMessage(myName + " đã thay đổi chủ đề thành màu " + colorName);
        dialog.dismiss();
    }

    private void showReactionPickerDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.layout_dialog_reaction_picker);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        int[] ids = {R.id.reactThumb, R.id.reactHeart, R.id.reactHaha, R.id.reactWow, 
                     R.id.reactSad, R.id.reactAngry, R.id.reactFire, R.id.reactStar, 
                     R.id.reactCheck, R.id.reactGift};
        for (int id : ids) {
            TextView tv = dialog.findViewById(id);
            if (tv != null) {
                tv.setOnClickListener(v -> {
                    dbRef.child("themes").child(chatRoomId).child("reaction").setValue(tv.getText().toString());
                    SharedPreferences prefs = getSharedPreferences("USER", MODE_PRIVATE);
                    String myName = prefs.getString("name", "Ai đó");
                    sendSystemMessage(myName + " đã thay đổi cảm xúc nhanh thành " + tv.getText().toString());
                    dialog.dismiss();
                });
            }
        }
        dialog.show();
    }

    private void sendSystemMessage(String msg) {
        DatabaseReference messageRef = dbRef.child("messages").child(chatRoomId).push();
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("messageId", messageRef.getKey());
        hashMap.put("sender", senderUid);
        hashMap.put("receiver", receiverUid);
        hashMap.put("message", msg);
        hashMap.put("type", "system");
        hashMap.put("timestamp", System.currentTimeMillis());
        hashMap.put("isseen", true);
        messageRef.setValue(hashMap);
    }

    private String getChatRoomId(String uid1, String uid2) {
        if (uid1 == null || uid2 == null) return "";
        return (uid1.compareTo(uid2) < 0) ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }
}
