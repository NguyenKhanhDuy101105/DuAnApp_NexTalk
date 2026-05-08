package com.example.nextalkapp;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.nextalkapp.Model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class NicknameActivity extends AppCompatActivity {

    private ImageView imgMyAvatar, imgReceiverAvatar;
    private TextView tvMyRealName, tvMyNickname, tvReceiverRealName, tvReceiverNickname;
    private View itemMyNickname, itemReceiverNickname;
    private String senderUid, receiverUid, chatRoomId;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nickname);

        receiverUid = getIntent().getStringExtra("receiverUid");
        SharedPreferences prefs = getSharedPreferences("USER", MODE_PRIVATE);
        senderUid = prefs.getString("uid", null);

        if (senderUid == null || receiverUid == null) {
            finish();
            return;
        }

        chatRoomId = getChatRoomId(senderUid, receiverUid);
        dbRef = FirebaseDatabase.getInstance().getReference();

        mapping();
        loadInfo();
        listenForNicknames();

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        itemMyNickname.setOnClickListener(v -> showEditNicknameDialog(senderUid, tvMyNickname.getText().toString(), tvMyRealName.getText().toString()));
        itemReceiverNickname.setOnClickListener(v -> showEditNicknameDialog(receiverUid, tvReceiverNickname.getText().toString(), tvReceiverRealName.getText().toString()));
    }

    private void mapping() {
        imgMyAvatar = findViewById(R.id.imgMyAvatar);
        imgReceiverAvatar = findViewById(R.id.imgReceiverAvatar);
        tvMyRealName = findViewById(R.id.tvMyRealName);
        tvMyNickname = findViewById(R.id.tvMyNickname);
        tvReceiverRealName = findViewById(R.id.tvReceiverRealName);
        tvReceiverNickname = findViewById(R.id.tvReceiverNickname);
        itemMyNickname = findViewById(R.id.itemMyNickname);
        itemReceiverNickname = findViewById(R.id.itemReceiverNickname);
    }

    private void loadInfo() {
        dbRef.child("users").child(senderUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    tvMyRealName.setText(user.getName());
                    if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                        Glide.with(NicknameActivity.this).load(user.getAvatar()).placeholder(R.drawable.logo2).into(imgMyAvatar);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        dbRef.child("users").child(receiverUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    tvReceiverRealName.setText(user.getName());
                    if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                        Glide.with(NicknameActivity.this).load(user.getAvatar()).placeholder(R.drawable.logo2).into(imgReceiverAvatar);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void listenForNicknames() {
        dbRef.child("nicknames").child(chatRoomId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String myNick = snapshot.child(senderUid).getValue(String.class);
                String receiverNick = snapshot.child(receiverUid).getValue(String.class);

                tvMyNickname.setText(myNick != null ? myNick : "Đặt biệt danh");
                tvReceiverNickname.setText(receiverNick != null ? receiverNick : "Đặt biệt danh");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showEditNicknameDialog(String targetUid, String currentNick, String realName) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.layout_dialog_edit_nickname);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        EditText edtNickname = dialog.findViewById(R.id.edtNickname);
        TextView btnCancel = dialog.findViewById(R.id.btnCancel);
        TextView btnSave = dialog.findViewById(R.id.btnSave);

        tvTitle.setText("Biệt danh cho " + realName);
        edtNickname.setText(currentNick.equals("Đặt biệt danh") ? "" : currentNick);
        edtNickname.setSelection(edtNickname.getText().length());

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newNick = edtNickname.getText().toString().trim();
            saveNickname(targetUid, newNick);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void saveNickname(String targetUid, String newNick) {
        // 1. Lưu biệt danh
        dbRef.child("nicknames").child(chatRoomId).child(targetUid).setValue(newNick.isEmpty() ? null : newNick);

        // 2. Tạo nội dung thông báo
        String myName = tvMyRealName.getText().toString();
        String targetRealName = targetUid.equals(senderUid) ? "chính mình" : tvReceiverRealName.getText().toString();
        String systemMsg;
        
        if (newNick.isEmpty()) {
            systemMsg = myName + " đã gỡ biệt danh của " + targetRealName + ".";
        } else {
            systemMsg = myName + " đã đặt biệt danh cho " + targetRealName + " là " + newNick + ".";
        }

        // 3. Gửi tin nhắn hệ thống vào messages
        DatabaseReference messageRef = dbRef.child("messages").child(chatRoomId).push();
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("messageId", messageRef.getKey());
        hashMap.put("sender", senderUid);
        hashMap.put("receiver", receiverUid);
        hashMap.put("message", systemMsg);
        hashMap.put("type", "system");
        hashMap.put("timestamp", System.currentTimeMillis());
        hashMap.put("isseen", true);
        messageRef.setValue(hashMap);

        // 4. Cập nhật tin nhắn cuối cùng trong chats
        HashMap<String, Object> lastMsgMap = new HashMap<>();
        lastMsgMap.put("lastMessage", systemMsg);
        lastMsgMap.put("lastTime", System.currentTimeMillis());
        
        dbRef.child("chats").child(senderUid).child(receiverUid).updateChildren(lastMsgMap);
        dbRef.child("chats").child(receiverUid).child(senderUid).updateChildren(lastMsgMap);
    }

    private String getChatRoomId(String uid1, String uid2) {
        return (uid1.compareTo(uid2) < 0) ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }
}
