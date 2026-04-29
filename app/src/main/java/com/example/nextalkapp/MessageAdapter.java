package com.example.nextalkapp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.nextalkapp.Model.ChatModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    public static final int MSG_TYPE_LEFT = 0;
    public static final int MSG_TYPE_RIGHT = 1;

    private Context mContext;
    private List<ChatModel> mChat;
    private String fuser;
    private String chatRoomId;

    public MessageAdapter(Context mContext, List<ChatModel> mChat, String chatRoomId) {
        this.mChat = mChat;
        this.mContext = mContext;
        this.chatRoomId = chatRoomId;
        SharedPreferences prefs = mContext.getSharedPreferences("USER", Context.MODE_PRIVATE);
        fuser = prefs.getString("uid", "");
    }

    @NonNull
    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(
                viewType == MSG_TYPE_RIGHT ? R.layout.item_chat_right : R.layout.item_chat_left,
                parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.ViewHolder holder, int position) {
        ChatModel chat = mChat.get(position);

        if ("image".equals(chat.getType())) {
            holder.show_message.setVisibility(View.GONE);
            holder.img_chat.setVisibility(View.VISIBLE);
            Glide.with(mContext).load(chat.getMessage()).placeholder(R.drawable.logo2).into(holder.img_chat);
        } else {
            holder.show_message.setVisibility(View.VISIBLE);
            holder.img_chat.setVisibility(View.GONE);
            holder.show_message.setText(chat.getMessage());
        }

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String time = sdf.format(new Date(chat.getTimestamp()));
        if (chat.getSender().equals(fuser)) {
            holder.txt_status.setText(chat.isIsseen() ? "Đã xem - " + time : "Đã gửi - " + time);
        } else {
            holder.txt_status.setText(time);
        }

        holder.itemView.setOnClickListener(v -> holder.txt_status.setVisibility(
                holder.txt_status.getVisibility() == View.GONE ? View.VISIBLE : View.GONE));

        holder.itemView.setOnLongClickListener(v -> {
            showDeleteDialog(chat);
            return true;
        });
    }

    private void showDeleteDialog(ChatModel chat) {
        Dialog dialog = new Dialog(mContext);
        dialog.setContentView(R.layout.layout_delete_dialog);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        Button btnCancel = dialog.findViewById(R.id.btnCancel);
        Button btnConfirm = dialog.findViewById(R.id.btnConfirmDelete);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            deleteMessage(chat);
            dialog.dismiss();
        });
        dialog.show();
    }

    // Trong MessageAdapter.java

    private void deleteMessage(ChatModel chat) {
        String msgId = chat.getMessageId();
        if (msgId == null) return;

        DatabaseReference messageRef = FirebaseDatabase.getInstance().getReference("messages").child(chatRoomId);

        messageRef.child(msgId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    showMotionToast("Thành công", "Đã xóa tin nhắn", MotionToastStyle.SUCCESS);

                    // Sau khi xóa, lấy tin nhắn cuối cùng còn lại trong node messages/chatRoomId
                    messageRef.orderByChild("timestamp").limitToLast(1)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    String newLastMsg = "";
                                    long newLastTime = 0;

                                    if (snapshot.exists()) {
                                        for (DataSnapshot child : snapshot.getChildren()) {
                                            ChatModel lastChat = child.getValue(ChatModel.class);
                                            if (lastChat != null) {
                                                newLastMsg = "image".equals(lastChat.getType()) ? "[Hình ảnh]" : lastChat.getMessage();
                                                newLastTime = lastChat.getTimestamp();
                                            }
                                        }
                                    }

                                    // Cập nhật lại cho cả 2 người
                                    updateFirebaseLastMessage(chat.getSender(), chat.getReceiver(), newLastMsg, newLastTime);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });
                });

        if ("image".equals(chat.getType())) {
            try {
                FirebaseStorage.getInstance().getReferenceFromUrl(chat.getMessage()).delete();
            } catch (Exception ignored) {}
        }
    }

    private void updateFirebaseLastMessage(String sender, String receiver, String msg, long time) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("lastMessage", msg);
        map.put("lastTime", time);

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users");
        userRef.child(sender).updateChildren(map);
        userRef.child(receiver).updateChildren(map);
    }

    private void showMotionToast(String title, String msg, MotionToastStyle style) {
        MotionToast.Companion.createColorToast((Activity) mContext, title, msg,
                style, MotionToast.GRAVITY_BOTTOM, MotionToast.LONG_DURATION,
                ResourcesCompat.getFont(mContext, www.sanju.motiontoast.R.font.helvetica_regular));
    }

    @Override
    public int getItemCount() { return mChat.size(); }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView show_message, txt_status;
        public ImageView img_chat;
        public ViewHolder(View itemView) {
            super(itemView);
            show_message = itemView.findViewById(R.id.show_message);
            txt_status = itemView.findViewById(R.id.txt_status);
            img_chat = itemView.findViewById(R.id.img_chat);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return mChat.get(position).getSender().equals(fuser) ? MSG_TYPE_RIGHT : MSG_TYPE_LEFT;
    }
}