package com.example.nextalkapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.nextalkapp.Model.ChatModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class MessageActivity extends AppCompatActivity {

    private ImageButton btnBack, btnSend, btnImage;
    private ImageView imgReceiverAvatar;
    private TextView tvReceiverName, tvStatusText;
    private View viewStatus;
    private EditText edtMessage;
    private RecyclerView rcvMessages;

    private String receiverUid, receiverName, receiverAvatar, senderUid, chatRoomId;
    private DatabaseReference dbRef;
    private MessageAdapter messageAdapter;
    private List<ChatModel> mChat;
    ValueEventListener seenListener;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    uploadImage(result.getData().getData());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        receiverUid = getIntent().getStringExtra("receiverUid");
        receiverName = getIntent().getStringExtra("receiverName");
        receiverAvatar = getIntent().getStringExtra("receiverAvatar");

        SharedPreferences prefs = getSharedPreferences("USER", MODE_PRIVATE);
        senderUid = prefs.getString("uid", null);

        dbRef = FirebaseDatabase.getInstance().getReference();
        chatRoomId = getChatRoomId(senderUid, receiverUid);

        mapping();
        displayReceiverInfo();
        readMessages();
        seenMessage(receiverUid);

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> {
            String msg = edtMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                sendMessage(senderUid, receiverUid, msg, "text");
                edtMessage.setText("");
            } else {
                showMotionToast("Cảnh báo", "Vui lòng nhập tin nhắn!", MotionToastStyle.WARNING);
            }
        });

        btnImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        checkReceiverStatus();
    }

    private void mapping() {
        btnBack = findViewById(R.id.btnBackChat);
        btnSend = findViewById(R.id.btnSend);
        btnImage = findViewById(R.id.btnImage);
        imgReceiverAvatar = findViewById(R.id.imgReceiverAvatar);
        tvReceiverName = findViewById(R.id.tvReceiverName);
        tvStatusText = findViewById(R.id.tvStatusText);
        viewStatus = findViewById(R.id.viewStatus);
        edtMessage = findViewById(R.id.edtMessage);
        rcvMessages = findViewById(R.id.rcvMessages);

        rcvMessages.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        rcvMessages.setLayoutManager(linearLayoutManager);
    }

    private void uploadImage(Uri uri) {
        if (uri == null) return;
        showMotionToast("Đang tải", "Hình ảnh đang được gửi...", MotionToastStyle.INFO);

        String fileName = UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = FirebaseStorage.getInstance().getReference().child("chat_images/" + fileName);

        ref.putFile(uri).addOnSuccessListener(taskSnapshot ->
                ref.getDownloadUrl().addOnSuccessListener(downloadUri ->
                        sendMessage(senderUid, receiverUid, downloadUri.toString(), "image"))
        ).addOnFailureListener(e -> showMotionToast("Lỗi", "Không thể tải ảnh!", MotionToastStyle.ERROR));
    }

    private void sendMessage(String sender, String receiver, String message, String type) {
        DatabaseReference messageRef = dbRef.child("messages").child(chatRoomId).push();
        String messageId = messageRef.getKey();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("messageId", messageId);
        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        hashMap.put("message", message);
        hashMap.put("type", type);
        hashMap.put("timestamp", System.currentTimeMillis());
        hashMap.put("isseen", false);

        messageRef.setValue(hashMap);

        HashMap<String, Object> lastMsgMap = new HashMap<>();
        lastMsgMap.put("lastMessage", type.equals("image") ? "[Hình ảnh]" : message);
        lastMsgMap.put("lastTime", System.currentTimeMillis());

        dbRef.child("users").child(sender).updateChildren(lastMsgMap);
        dbRef.child("users").child(receiver).updateChildren(lastMsgMap);
    }

    private void showMotionToast(String title, String msg, MotionToastStyle style) {
        MotionToast.Companion.createColorToast(this, title, msg,
                style, MotionToast.GRAVITY_BOTTOM, MotionToast.LONG_DURATION,
                ResourcesCompat.getFont(this, www.sanju.motiontoast.R.font.helvetica_regular));
    }

    // Các hàm (readMessages, seenMessage, getChatRoomId, checkReceiverStatus, onPause, displayReceiverInfo) giữ nguyên logic cũ Duy nhé.
    private void readMessages() {
        dbRef.child("messages").child(chatRoomId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mChat = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    ChatModel chat = data.getValue(ChatModel.class);
                    if (chat != null) mChat.add(chat);
                }
                messageAdapter = new MessageAdapter(MessageActivity.this, mChat, chatRoomId);
                rcvMessages.setAdapter(messageAdapter);
                if (mChat.size() > 0) rcvMessages.scrollToPosition(mChat.size() - 1);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String getChatRoomId(String uid1, String uid2) {
        if (uid1 == null || uid2 == null) return "";
        return (uid1.compareTo(uid2) < 0) ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    private void seenMessage(String userId) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("messages").child(chatRoomId);
        seenListener = reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    ChatModel chat = data.getValue(ChatModel.class);
                    if (chat != null && chat.getReceiver().equals(senderUid) && chat.getSender().equals(userId)) {
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("isseen", true);
                        data.getRef().updateChildren(hashMap);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void displayReceiverInfo() {
        tvReceiverName.setText(receiverName != null ? receiverName : "Người dùng");
        if (receiverAvatar != null && !receiverAvatar.isEmpty()) {
            Glide.with(this).load(receiverAvatar).placeholder(R.drawable.logo2).into(imgReceiverAvatar);
        }
    }

    private void checkReceiverStatus() {
        dbRef.child("users").child(receiverUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChild("status")) {
                    String status = snapshot.child("status").getValue(String.class);
                    tvStatusText.setText("online".equals(status) ? "Đang hoạt động" : "Ngoại tuyến");
                    viewStatus.setBackgroundResource("online".equals(status) ? R.drawable.bg_status_online : R.drawable.bg_status_offline);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (seenListener != null) dbRef.child("messages").child(chatRoomId).removeEventListener(seenListener);
    }
}