package com.example.nextalkapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.nextalkapp.Model.OfflineMessage;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;

public class NetworkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkChangeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (NetworkUtil.isConnected(context)) {
            Log.d(TAG, "Mạng đã kết nối! Đang tiến hành đồng bộ tin nhắn...");
            syncMessages(context);
        }
    }

    // Chuyển thành public static để MainActivity có thể gọi
    public static void syncMessages(Context context) {
        OfflineDbHelper dbHelper = new OfflineDbHelper(context);
        List<OfflineMessage> pending = dbHelper.getAllPendingMessages();

        if (pending == null || pending.isEmpty()) {
            Log.d(TAG, "Không có tin nhắn nào đang chờ gửi.");
            return;
        }

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

        for (OfflineMessage msg : pending) {
            DatabaseReference messageRef = dbRef.child("messages").child(msg.getChatRoomId()).child(msg.getMessageId());

            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("messageId", msg.getMessageId());
            hashMap.put("sender", msg.getSender());
            hashMap.put("receiver", msg.getReceiver());
            hashMap.put("message", msg.getMessage());
            hashMap.put("type", msg.getType());
            hashMap.put("timestamp", msg.getTimestamp());
            hashMap.put("isseen", false);

            messageRef.setValue(hashMap).addOnSuccessListener(unused -> {
                Log.d(TAG, "Đã gửi thành công tin nhắn: " + msg.getMessageId());
                // Xóa khỏi SQLite sau khi đẩy lên Firebase thành công
                dbHelper.deleteMessage(msg.getMessageId());

                // Cập nhật Last Message để hiển thị ở danh sách chat
                HashMap<String, Object> lastMsgMap = new HashMap<>();
                lastMsgMap.put("lastMessage", msg.getType().equals("image") ? "[Hình ảnh]" : msg.getMessage());
                lastMsgMap.put("lastTime", msg.getTimestamp());

                dbRef.child("users").child(msg.getSender()).updateChildren(lastMsgMap);
                dbRef.child("users").child(msg.getReceiver()).updateChildren(lastMsgMap);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Lỗi khi gửi tin nhắn " + msg.getMessageId() + ": " + e.getMessage());
            });
        }
    }
}
