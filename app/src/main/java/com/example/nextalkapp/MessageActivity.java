package com.example.nextalkapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.nextalkapp.Model.ChatModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MessageActivity extends AppCompatActivity {

    // View Components
    private ImageButton btnBack, btnSend;
    private ImageView imgReceiverAvatar;
    private TextView tvReceiverName, tvStatusText;
    private View viewStatus;
    private EditText edtMessage;
    private RecyclerView rcvMessages;

    // Firebase & Data
    private String receiverUid, receiverName, receiverAvatar;
    private String senderUid;
    private DatabaseReference dbRef;

    private MessageAdapter messageAdapter;
    private List<ChatModel> mChat;

    // 🟢 Thêm biến Listener để quản lý trạng thái đã xem
    ValueEventListener seenListener;
    String chatRoomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        receiverUid = getIntent().getStringExtra("receiverUid");

        if (receiverUid == null || receiverUid.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy ID người dùng!", Toast.LENGTH_SHORT).show();
            finish(); // Thoát Activity ngay để không crash các hàm phía dưới
            return;
        }

        receiverName = getIntent().getStringExtra("receiverName");
        receiverAvatar = getIntent().getStringExtra("receiverAvatar");

        SharedPreferences prefs = getSharedPreferences("USER", MODE_PRIVATE);
        senderUid = prefs.getString("uid", null);

        if (senderUid == null || receiverUid == null) {
            Toast.makeText(this, "Lỗi kết nối dữ liệu!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbRef = FirebaseDatabase.getInstance().getReference();
        chatRoomId = getChatRoomId(senderUid, receiverUid);

        mapping();
        displayReceiverInfo();

        readMessages(senderUid, receiverUid);

        // 🟢 Kích hoạt logic đánh dấu đã xem
        seenMessage(receiverUid);

        btnBack.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            String msg = edtMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                sendMessage(senderUid, receiverUid, msg);
                edtMessage.setText("");
            } else {
                Toast.makeText(this, "Vui lòng nhập tin nhắn!", Toast.LENGTH_SHORT).show();
            }
        });

        checkReceiverStatus();
    }

    private void mapping() {
        btnBack = findViewById(R.id.btnBackChat);
        btnSend = findViewById(R.id.btnSend);
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

    private void displayReceiverInfo() {
        // Thêm kiểm tra null để tránh crash app
        if (receiverName != null) {
            tvReceiverName.setText(receiverName);
        } else {
            tvReceiverName.setText("Người dùng");
        }

        if (receiverAvatar != null && !receiverAvatar.isEmpty()) {
            Glide.with(this).load(receiverAvatar).placeholder(R.drawable.logo2).into(imgReceiverAvatar);
        } else {
            imgReceiverAvatar.setImageResource(R.drawable.logo2);
        }
    }

    private String getChatRoomId(String uid1, String uid2) {
        if (uid1.compareTo(uid2) < 0) {
            return uid1 + "_" + uid2;
        } else {
            return uid2 + "_" + uid1;
        }
    }

    // 🟢 Logic cập nhật trạng thái "Đã xem" khi người dùng ở trong màn hình chat
    private void seenMessage(String userId) {
        // Sử dụng tham chiếu trực tiếp đến phòng chat
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("messages").child(chatRoomId);
        seenListener = reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    ChatModel chat = data.getValue(ChatModel.class);
                    if (chat != null && chat.getReceiver().equals(senderUid) && chat.getSender().equals(userId)) {
                        // Dùng updateChildren để không làm mất các dữ liệu khác của tin nhắn
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("isseen", true);
                        data.getRef().updateChildren(hashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void readMessages(String myId, String receiverId) {
        dbRef.child("messages").child(chatRoomId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mChat = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    ChatModel chat = data.getValue(ChatModel.class);
                    if (chat != null) {
                        mChat.add(chat);
                    }
                }
                messageAdapter = new MessageAdapter(MessageActivity.this, mChat);
                rcvMessages.setAdapter(messageAdapter);

                if (mChat.size() > 0) {
                    rcvMessages.scrollToPosition(mChat.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void sendMessage(String sender, String receiver, String message) {
        long timestamp = System.currentTimeMillis();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        hashMap.put("message", message);
        hashMap.put("timestamp", timestamp);
        hashMap.put("isseen", false); // 🟢 Mặc định tin mới gửi là chưa xem

        dbRef.child("messages").child(chatRoomId).push().setValue(hashMap);

        HashMap<String, Object> lastMsgMap = new HashMap<>();
        lastMsgMap.put("lastMessage", message);
        lastMsgMap.put("lastTime", timestamp);

        dbRef.child("users").child(sender).updateChildren(lastMsgMap);
        dbRef.child("users").child(receiver).updateChildren(lastMsgMap);
    }

    private void checkReceiverStatus() {
        dbRef.child("users").child(receiverUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.hasChild("status")) {
                    String status = snapshot.child("status").getValue(String.class);
                    if ("online".equals(status)) {
                        tvStatusText.setText("Đang hoạt động");
                        viewStatus.setBackgroundResource(R.drawable.bg_status_online);
                    } else {
                        tvStatusText.setText("Ngoại tuyến");
                        viewStatus.setBackgroundResource(R.drawable.bg_status_offline);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // 🟢 Hủy lắng nghe khi thoát Activity để tiết kiệm tài nguyên
    @Override
    protected void onPause() {
        super.onPause();
        if (seenListener != null) {
            dbRef.child("messages").child(chatRoomId).removeEventListener(seenListener);
        }
    }
}