package com.example.nextalkapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        String title = null;
        String body = null;
        String senderUid = null;
        String senderName = null;

        if (message.getData().size() > 0) {
            Map<String, String> data = message.getData();
            Log.d("NexTalk_FCM", "Dữ liệu nhận được: " + data.toString());

            if (data.containsKey("custom")) {
                try {
                    JSONObject customJson = new JSONObject(data.get("custom"));
                    if (customJson.has("a")) {
                        JSONObject additionalData = customJson.getJSONObject("a");
                        senderUid = additionalData.optString("senderUid");
                        senderName = additionalData.optString("senderName");
                    }
                } catch (Exception e) {
                    Log.e("NexTalk_FCM", "Lỗi phân giải JSON OneSignal: " + e.getMessage());
                }
            }

            title = data.get("title");
            body = data.get("alert");

            if (senderUid == null) senderUid = data.get("senderUid");
            if (senderName == null) senderName = data.get("senderName");
        }

        if (message.getNotification() != null) {
            if (title == null) title = message.getNotification().getTitle();
            if (body == null) body = message.getNotification().getBody();
        }

        if (body != null || title != null) {
            sendNotification(title != null ? title : "NexTalk", body != null ? body : "", senderUid, senderName);
        }
    }

    private void sendNotification(String title, String body, String senderUid, String senderName) {
        Intent intent;

        if (senderUid != null && !senderUid.isEmpty()) {
            intent = new Intent(this, MessageActivity.class);
            intent.putExtra("receiverUid", senderUid);
            intent.putExtra("receiverName", senderName != null ? senderName : title);
        } else {
            intent = new Intent(this, MainActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int notificationId = (senderUid != null) ? senderUid.hashCode() : (int) System.currentTimeMillis();

        // ĐÃ SỬA: Gom logic khởi tạo kiểm tra SDK Android của PendingIntent lên đầu gọn gàng
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, notificationId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, notificationId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }

        String channelId = "nextalk_chat";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.logo2)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent); // Gán biến đã check SDK vào đây

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Tin nhắn NexTalk", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        manager.notify(notificationId, builder.build());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        String uid = getSharedPreferences("USER", MODE_PRIVATE).getString("uid", null);
        if (uid != null) {
            FirebaseDatabase.getInstance().getReference("users").child(uid).child("fcmToken").setValue(token);
        }
    }
}