package com.example.nextalkapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.nextalkapp.Model.ChatModel;
import com.example.nextalkapp.Model.OfflineMessage;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.OneSignal;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class MessageActivity extends AppCompatActivity {

    private ImageButton btnBack, btnSend, btnImage;
    private TextView btnReaction;
    private ImageView imgReceiverAvatar;
    private TextView tvReceiverName, tvStatusText;
    private View viewStatus, layoutReceiverInfo;
    private CardView cvAvatar;
    private EditText edtMessage;
    private RecyclerView rcvMessages;

    private String receiverUid, receiverName, receiverAvatar, senderUid, chatRoomId, senderName;
    private String themeColor = "#5C8EE6";
    private String quickReaction = "👍";
    private DatabaseReference dbRef;
    private MessageAdapter messageAdapter;
    private List<ChatModel> mChat;
    private ValueEventListener seenListener;
    private ValueEventListener messagesListener;
    private DataSnapshot lastFirebaseSnapshot;
    private OfflineDbHelper offlineDbHelper;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            uploadImage(result.getData().getData());
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        initCloudinary();

        receiverUid = getIntent().getStringExtra("receiverUid");
        receiverName = getIntent().getStringExtra("receiverName");
        receiverAvatar = getIntent().getStringExtra("receiverAvatar");

        SharedPreferences prefs = getSharedPreferences("USER", MODE_PRIVATE);
        senderUid = prefs.getString("uid", null);
        senderName = prefs.getString("name", "NexTalk User");

        dbRef = FirebaseDatabase.getInstance().getReference();
        chatRoomId = getChatRoomId(senderUid, receiverUid);
        offlineDbHelper = new OfflineDbHelper(this);

        mapping();
        displayReceiverInfo();
        readMessages();
        seenMessage(receiverUid);
        listenForNickname();
        listenForTheme();
        listenForReaction();

        btnBack.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            String msg = edtMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                handleSendMessage(msg, "text");
                edtMessage.setText("");
            }
        });

        btnReaction.setOnClickListener(v -> handleSendMessage(quickReaction, "text"));

        edtMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() > 0) {
                    btnSend.setVisibility(View.VISIBLE);
                    btnReaction.setVisibility(View.GONE);
                } else {
                    btnSend.setVisibility(View.GONE);
                    btnReaction.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        View.OnClickListener openProfileListener = v -> {
            Intent intent = new Intent(MessageActivity.this, ReceiverProfileActivity.class);
            intent.putExtra("receiverUid", receiverUid);
            intent.putExtra("receiverName", receiverName);
            intent.putExtra("receiverAvatar", receiverAvatar);
            startActivity(intent);
        };

        imgReceiverAvatar.setOnClickListener(openProfileListener);
        tvReceiverName.setOnClickListener(openProfileListener);
        if (cvAvatar != null) cvAvatar.setOnClickListener(openProfileListener);
        if (layoutReceiverInfo != null) layoutReceiverInfo.setOnClickListener(openProfileListener);

        checkReceiverStatus();
    }

    private void sendMessageToFirebase(String messageId, String sender, String receiver, String message, String type, long timestamp) {
        DatabaseReference messageRef = dbRef.child("messages").child(chatRoomId).child(messageId);

        String finalMessage = message;
        if (type.equals("text")) {
            try {
                finalMessage = AESUtils.encrypt(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("messageId", messageId);
        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        hashMap.put("message", finalMessage);
        hashMap.put("type", type);
        hashMap.put("timestamp", timestamp);
        hashMap.put("isseen", false);

        messageRef.setValue(hashMap).addOnSuccessListener(aVoid -> {
            String content = type.equals("image") ? "[Hình ảnh]" : message;
            // Gửi thông báo thực tế
            sendActualNotification("", senderName, content);
        });

        HashMap<String, Object> lastMsgMap = new HashMap<>();
        lastMsgMap.put("lastMessage", type.equals("image") ? "[Hình ảnh]" : finalMessage);
        lastMsgMap.put("lastTime", timestamp);

        dbRef.child("chats").child(sender).child(receiver).updateChildren(lastMsgMap);
        dbRef.child("chats").child(receiver).child(sender).updateChildren(lastMsgMap);
    }

    private void sendActualNotification(String unusedToken, String title, String message) {
        try {
            JSONObject jsonPayload = new JSONObject();

            // 0. QUAN TRỌNG: Phải có app_id của ứng dụng NexTalk ở đây
            jsonPayload.put("app_id", "3f1507b8-f3c0-417a-a700-8e70612a17bd");

            // 1. Nội dung thông báo
            JSONObject contents = new JSONObject();
            contents.put("en", message);
            jsonPayload.put("contents", contents);

            // 2. Tiêu đề (Tên người gửi)
            JSONObject headings = new JSONObject();
            headings.put("en", title);
            jsonPayload.put("headings", headings);

            // 3. Người nhận (Dựa trên external_id)
            JSONArray externalIds = new JSONArray();
            externalIds.put(receiverUid);
            jsonPayload.put("include_external_user_ids", externalIds);

            // 4. Data Payload để xử lý logic mở phòng chat
            JSONObject data = new JSONObject();
            data.put("senderUid", senderUid);
            data.put("senderName", senderName);
            jsonPayload.put("data", data);

            jsonPayload.put("priority", 10);
            jsonPayload.put("android_visibility", 1);

            String url = "https://api.onesignal.com/notifications/";
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonPayload,
                    response -> Log.d("OneSignal_NexTalk", "Gửi thông báo thành công: " + response.toString()),
                    error -> {
                        if (error.networkResponse != null) {
                            String errorData = new String(error.networkResponse.data);
                            Log.e("OneSignal_NexTalk", "Gửi thất bại. HTTP Code: " + error.networkResponse.statusCode + " | Chi tiết: " + errorData);
                        } else {
                            Log.e("OneSignal_NexTalk", "Gửi thất bại: " + error.toString());
                        }
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json; charset=UTF-8");

                    // THAY ĐỔI: Dán chính xác REST API Key của bạn vào sau chữ "Basic "
                    headers.put("Authorization", "os_v2_app_h4kqpohtybaxvjyarzygckqxxupdgslrwjjelv4qm5pz6x3ty4vez3hyo23gj7meumee4phyuez4zppzole4nrmw5522tl3xyy4co5i");
                    return headers;
                }
            };

            Volley.newRequestQueue(this).add(request);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void handleSendMessage(String message, String type) {
        String messageId = dbRef.child("messages").child(chatRoomId).push().getKey();
        long timestamp = System.currentTimeMillis();

        if (NetworkUtil.isConnected(this)) {
            sendMessageToFirebase(messageId, senderUid, receiverUid, message, type, timestamp);
        } else {
            String messageToSave = message;
            if (type.equals("text")) {
                try {
                    messageToSave = AESUtils.encrypt(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            OfflineMessage offlineMsg = new OfflineMessage(messageId, senderUid, receiverUid, messageToSave, type, timestamp, chatRoomId);
            offlineDbHelper.addMessage(offlineMsg);
            showMotionToast("Ngoại tuyến", "Tin nhắn sẽ được gửi khi có mạng", MotionToastStyle.INFO);
            updateChatUI(lastFirebaseSnapshot);
        }
    }

    private void readMessages() {
        messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                lastFirebaseSnapshot = snapshot;
                updateChatUI(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        dbRef.child("messages").child(chatRoomId).addValueEventListener(messagesListener);
    }

    private void updateChatUI(DataSnapshot snapshot) {
        mChat = new ArrayList<>();
        Set<String> firebaseIds = new HashSet<>();
        if (snapshot != null) {
            for (DataSnapshot data : snapshot.getChildren()) {
                ChatModel chat = data.getValue(ChatModel.class);
                if (chat != null) {
                    chat.setPending(false);
                    mChat.add(chat);
                    firebaseIds.add(chat.getMessageId());
                }
            }
        }
        List<OfflineMessage> pendingMsgs = offlineDbHelper.getAllPendingMessages();
        for (OfflineMessage offline : pendingMsgs) {
            if (offline.getChatRoomId().equals(chatRoomId) && !firebaseIds.contains(offline.getMessageId())) {
                ChatModel chat = new ChatModel(offline.getMessageId(), offline.getSender(),
                        offline.getReceiver(), offline.getMessage(), offline.getType(),
                        offline.getTimestamp(), false);
                chat.setPending(true);
                mChat.add(chat);
            }
        }
        mChat.sort((o1, o2) -> Long.compare(o1.getTimestamp(), o2.getTimestamp()));
        messageAdapter = new MessageAdapter(MessageActivity.this, mChat, chatRoomId);
        messageAdapter.setThemeColor(themeColor);
        rcvMessages.setAdapter(messageAdapter);
        if (mChat.size() > 0) rcvMessages.scrollToPosition(mChat.size() - 1);
    }

    private void initCloudinary() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", "dak681rft");
            config.put("api_key", "251122464815674");
            config.put("api_secret", "xbzo_uLnnX-p5eTuwqoDaDhkA3I");
            MediaManager.init(this, config);
        } catch (Exception e) {
            Log.d("Cloudinary", "Already initialized");
        }
    }

    private void mapping() {
        btnBack = findViewById(R.id.btnBackChat);
        btnSend = findViewById(R.id.btnSend);
        btnImage = findViewById(R.id.btnImage);
        btnReaction = findViewById(R.id.btnReaction);
        imgReceiverAvatar = findViewById(R.id.imgReceiverAvatar);
        tvReceiverName = findViewById(R.id.tvReceiverName);
        tvStatusText = findViewById(R.id.tvStatusText);
        viewStatus = findViewById(R.id.viewStatus);
        layoutReceiverInfo = findViewById(R.id.layoutReceiverInfo);
        cvAvatar = findViewById(R.id.cvAvatar);
        edtMessage = findViewById(R.id.edtMessage);
        rcvMessages = findViewById(R.id.rcvMessages);
        rcvMessages.setHasFixedSize(true);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rcvMessages.setLayoutManager(lm);
    }

    private String getChatRoomId(String u1, String u2) {
        return (u1.compareTo(u2) < 0) ? u1 + "_" + u2 : u2 + "_" + u1;
    }

    private void showMotionToast(String t, String m, MotionToastStyle s) {
        MotionToast.Companion.createColorToast(this, t, m, s, MotionToast.GRAVITY_BOTTOM, MotionToast.LONG_DURATION, ResourcesCompat.getFont(this, www.sanju.motiontoast.R.font.helvetica_regular));
    }

    private void displayReceiverInfo() {
        tvReceiverName.setText(receiverName != null ? receiverName : "Người dùng");
        if (receiverAvatar != null && !receiverAvatar.isEmpty())
            Glide.with(this).load(receiverAvatar).placeholder(R.drawable.logo2).into(imgReceiverAvatar);
    }

    private void checkReceiverStatus() {
        dbRef.child("users").child(receiverUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.child("status").getValue(String.class);
                    tvStatusText.setText("online".equals(status) ? "Đang hoạt động" : "Ngoại tuyến");
                    viewStatus.setBackgroundResource("online".equals(status) ? R.drawable.bg_status_online : R.drawable.bg_status_offline);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void listenForNickname() {
        dbRef.child("nicknames").child(chatRoomId).child(receiverUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String nickname = snapshot.getValue(String.class);
                if (nickname != null) tvReceiverName.setText(nickname);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void listenForTheme() {
        dbRef.child("themes").child(chatRoomId).child("color").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String color = snapshot.getValue(String.class);
                if (color != null) {
                    themeColor = color;
                    applyTheme(color);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void listenForReaction() {
        dbRef.child("themes").child(chatRoomId).child("reaction").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String reaction = snapshot.getValue(String.class);
                if (reaction != null) {
                    quickReaction = reaction;
                    btnReaction.setText(reaction);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void applyTheme(String colorCode) {
        int color = Color.parseColor(colorCode);
        btnSend.setColorFilter(color);
        btnImage.setColorFilter(color);
        if (messageAdapter != null) {
            messageAdapter.setThemeColor(colorCode);
            messageAdapter.notifyDataSetChanged();
        }
    }

    private void seenMessage(String userId) {
        seenListener = dbRef.child("messages").child(chatRoomId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    ChatModel chat = data.getValue(ChatModel.class);
                    if (chat != null && chat.getReceiver().equals(senderUid) && chat.getSender().equals(userId)) {
                        data.getRef().child("isseen").setValue(true);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (seenListener != null)
            dbRef.child("messages").child(chatRoomId).removeEventListener(seenListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null)
            dbRef.child("messages").child(chatRoomId).removeEventListener(messagesListener);
    }

    private void uploadImage(Uri uri) {
        if (!NetworkUtil.isConnected(this)) {
            showMotionToast("Lỗi", "Cần có mạng!", MotionToastStyle.ERROR);
            return;
        }
        showMotionToast("Đang tải", "Đang gửi ảnh...", MotionToastStyle.INFO);
        MediaManager.get().upload(uri).unsigned("Images").option("folder", "chat_images").callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {
            }

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {
            }

            @Override
            public void onSuccess(String requestId, Map resultData) {
                handleSendMessage((String) resultData.get("secure_url"), "image");
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                showMotionToast("Lỗi", "Tải ảnh lỗi!", MotionToastStyle.ERROR);
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {
            }
        }).dispatch();
    }
}