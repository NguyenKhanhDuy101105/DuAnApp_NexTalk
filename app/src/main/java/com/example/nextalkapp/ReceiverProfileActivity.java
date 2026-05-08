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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class ReceiverProfileActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ImageView imgProfileAvatar, imgMute;
    private TextView tvProfileName, tvMute, tvCreateGroup;
    private LinearLayout btnMute, btnPersonalPage, btnAddFriend;
    
    private View itemTheme, itemQuickReaction, itemNickname, itemWordEffects;
    private View itemCreateGroup, itemMedia, itemAutoSave, itemPinned;
    
    private String receiverUid, receiverName, receiverAvatar, senderUid, chatRoomId;
    private boolean isMuted = false;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver_profile);

        receiverUid = getIntent().getStringExtra("receiverUid");
        receiverName = getIntent().getStringExtra("receiverName");
        receiverAvatar = getIntent().getStringExtra("receiverAvatar");

        SharedPreferences prefs = getSharedPreferences("USER", MODE_PRIVATE);
        senderUid = prefs.getString("uid", null);
        chatRoomId = getChatRoomId(senderUid, receiverUid);
        dbRef = FirebaseDatabase.getInstance().getReference();

        mapping();
        displayInfo();
        checkMuteStatus();
        setupEvents();
        listenForNickname();
    }

    private void mapping() {
        btnBack = findViewById(R.id.btnBack);
        imgProfileAvatar = findViewById(R.id.imgProfileAvatar);
        tvProfileName = findViewById(R.id.tvProfileName);
        
        btnAddFriend = findViewById(R.id.btn_add_friend);
        btnPersonalPage = findViewById(R.id.btn_personal_page);
        btnMute = findViewById(R.id.btn_mute_notifications);
        imgMute = findViewById(R.id.imgMute);
        tvMute = findViewById(R.id.tvMute);
        
        itemTheme = findViewById(R.id.itemTheme);
        itemQuickReaction = findViewById(R.id.itemQuickReaction);
        itemNickname = findViewById(R.id.itemNickname);
        itemWordEffects = findViewById(R.id.itemWordEffects);
        
        itemCreateGroup = findViewById(R.id.itemCreateGroup);
        itemMedia = findViewById(R.id.itemMedia);
        itemAutoSave = findViewById(R.id.itemAutoSave);
        itemPinned = findViewById(R.id.itemPinned);
        
        tvCreateGroup = findViewById(R.id.tvCreateGroup);
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

        btnMute.setOnClickListener(v -> {
            isMuted = !isMuted;
            saveMuteStatus(isMuted);
            updateMuteUI();
            
            String msg = isMuted ? "Đã tắt thông báo từ " + tvProfileName.getText() : "Đã bật thông báo từ " + tvProfileName.getText();
            showMotionToast("Thông báo", msg, isMuted ? MotionToastStyle.INFO : MotionToastStyle.SUCCESS);
        });

        btnAddFriend.setOnClickListener(v -> 
            showMotionToast("Bạn bè", "Đã gửi lời mời kết bạn đến " + tvProfileName.getText(), MotionToastStyle.SUCCESS));

        btnPersonalPage.setOnClickListener(v -> {
            Intent intent = new Intent(ReceiverProfileActivity.this, ViewProfileActivity.class);
            intent.putExtra("receiverUid", receiverUid);
            startActivity(intent);
        });

        itemNickname.setOnClickListener(v -> {
            Intent intent = new Intent(ReceiverProfileActivity.this, NicknameActivity.class);
            intent.putExtra("receiverUid", receiverUid);
            intent.putExtra("receiverName", receiverName);
            intent.putExtra("receiverAvatar", receiverAvatar);
            startActivity(intent);
        });

        itemTheme.setOnClickListener(v -> showThemePickerDialog());
        
        itemQuickReaction.setOnClickListener(v -> showReactionPickerDialog());

        View.OnClickListener comingSoonListener = v -> 
            showMotionToast("Thông báo", "Tính năng này đang được phát triển!", MotionToastStyle.INFO);

        itemWordEffects.setOnClickListener(comingSoonListener);
        itemCreateGroup.setOnClickListener(comingSoonListener);
        itemMedia.setOnClickListener(comingSoonListener);
        itemAutoSave.setOnClickListener(comingSoonListener);
        itemPinned.setOnClickListener(comingSoonListener);
    }

    private void showThemePickerDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.layout_dialog_theme_picker);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

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
        String systemMsg = myName + " đã thay đổi chủ đề cuộc trò chuyện thành màu " + colorName + ".";

        sendSystemMessage(systemMsg);

        dialog.dismiss();
        showMotionToast("Chủ đề", "Đã cập nhật màu " + colorName, MotionToastStyle.SUCCESS);
    }
    
    private void showReactionPickerDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.layout_dialog_reaction_picker);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        int[] ids = {R.id.reactThumb, R.id.reactHeart, R.id.reactHaha, R.id.reactWow, 
                     R.id.reactSad, R.id.reactAngry, R.id.reactFire, R.id.reactStar, 
                     R.id.reactCheck, R.id.reactGift};
        
        for (int id : ids) {
            TextView tv = dialog.findViewById(id);
            tv.setOnClickListener(v -> updateReaction(tv.getText().toString(), dialog));
        }

        dialog.show();
    }

    private void updateReaction(String reaction, Dialog dialog) {
        dbRef.child("themes").child(chatRoomId).child("reaction").setValue(reaction);

        SharedPreferences prefs = getSharedPreferences("USER", MODE_PRIVATE);
        String myName = prefs.getString("name", "Ai đó");
        String systemMsg = myName + " đã thay đổi biểu tượng cảm xúc nhanh thành " + reaction + ".";

        sendSystemMessage(systemMsg);

        dialog.dismiss();
        showMotionToast("Cảm xúc", "Đã cập nhật biểu tượng " + reaction, MotionToastStyle.SUCCESS);
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

        HashMap<String, Object> lastMsgMap = new HashMap<>();
        lastMsgMap.put("lastMessage", msg);
        lastMsgMap.put("lastTime", System.currentTimeMillis());
        dbRef.child("chats").child(senderUid).child(receiverUid).updateChildren(lastMsgMap);
        dbRef.child("chats").child(receiverUid).child(senderUid).updateChildren(lastMsgMap);
    }

    private void displayInfo() {
        tvProfileName.setText(receiverName != null ? receiverName : "Người dùng");
        if (tvCreateGroup != null) {
            tvCreateGroup.setText("Tạo nhóm chat với " + (receiverName != null ? receiverName : "người này"));
        }
        if (receiverAvatar != null && !receiverAvatar.isEmpty()) {
            Glide.with(this).load(receiverAvatar).placeholder(R.drawable.logo2).into(imgProfileAvatar);
        }
    }

    private void checkMuteStatus() {
        SharedPreferences prefs = getSharedPreferences("Notifications", MODE_PRIVATE);
        isMuted = prefs.getBoolean("mute_" + receiverUid, false);
        updateMuteUI();
    }

    private void saveMuteStatus(boolean mute) {
        SharedPreferences.Editor editor = getSharedPreferences("Notifications", MODE_PRIVATE).edit();
        editor.putBoolean("mute_" + receiverUid, mute);
        editor.apply();
    }

    private void updateMuteUI() {
        if (isMuted) {
            tvMute.setText("Bật thông báo");
            imgMute.setImageResource(android.R.drawable.ic_lock_silent_mode);
            imgMute.setAlpha(0.6f); 
        } else {
            tvMute.setText("Tắt thông báo");
            imgMute.setImageResource(R.drawable.notification);
            imgMute.setAlpha(1.0f);
        }
    }

    private String getChatRoomId(String uid1, String uid2) {
        if (uid1 == null || uid2 == null) return "";
        return (uid1.compareTo(uid2) < 0) ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    private void showMotionToast(String title, String msg, MotionToastStyle style) {
        MotionToast.Companion.createColorToast(this, title, msg,
                style, MotionToast.GRAVITY_BOTTOM, MotionToast.LONG_DURATION,
                ResourcesCompat.getFont(this, www.sanju.motiontoast.R.font.helvetica_regular));
    }
}
