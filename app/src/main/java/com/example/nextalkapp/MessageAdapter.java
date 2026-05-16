package com.example.nextalkapp;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.nextalkapp.Model.ChatModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
    public static final int MSG_TYPE_SYSTEM = 2;

    private Context mContext;
    private List<ChatModel> mChat;
    private String fuser;
    private String chatRoomId;
    private String themeColor = "#5C8EE6";

    public MessageAdapter(Context mContext, List<ChatModel> mChat, String chatRoomId) {
        this.mChat = mChat;
        this.mContext = mContext;
        this.chatRoomId = chatRoomId;
        SharedPreferences prefs = mContext.getSharedPreferences("USER", Context.MODE_PRIVATE);
        fuser = prefs.getString("uid", "");
    }

    public void setThemeColor(String themeColor) {
        this.themeColor = themeColor;
    }

    @NonNull
    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MSG_TYPE_SYSTEM) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_chat_system, parent, false);
            return new ViewHolder(view);
        }
        View view = LayoutInflater.from(mContext).inflate(
                viewType == MSG_TYPE_RIGHT ? R.layout.item_chat_right : R.layout.item_chat_left,
                parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.ViewHolder holder, int position) {
        ChatModel chat = mChat.get(position);

        // --- 1. XỬ LÝ TIN NHẮN HỆ THỐNG ---
        if (getItemViewType(position) == MSG_TYPE_SYSTEM) {
            String msg = chat.getMessage();
            SharedPreferences prefs = mContext.getSharedPreferences("USER", Context.MODE_PRIVATE);
            String myName = prefs.getString("name", "");

            if (chat.getSender().equals(fuser)) {
                if (!myName.isEmpty() && msg.startsWith(myName)) {
                    msg = msg.replaceFirst(myName, "Bạn");
                }
            } else {
                if (!myName.isEmpty() && msg.contains(myName)) {
                    msg = msg.replace(myName, "bạn");
                }
            }

            if (holder.tvSystemMessage != null) {
                holder.tvSystemMessage.setText(msg);
            }
            return;
        }

        // --- 2. XỬ LÝ TIN NHẮN THÔNG THƯỜNG (ẢNH/CHỮ) ---
        if (holder.show_message != null && holder.img_chat != null) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.txt_status.getLayoutParams();

            if ("image".equals(chat.getType())) {
                // Tin nhắn ảnh: KHÔNG giải mã (vì message là link URL)
                holder.show_message.setVisibility(View.GONE);
                holder.img_chat.setVisibility(View.VISIBLE);

                Glide.with(mContext)
                        .load(chat.getMessage())
                        .placeholder(R.drawable.logo2)
                        .into(holder.img_chat);

                params.addRule(RelativeLayout.BELOW, R.id.img_chat);

            } else {
                // Tin nhắn chữ: CẦN giải mã
                holder.show_message.setVisibility(View.VISIBLE);
                holder.img_chat.setVisibility(View.GONE);

                // MÃ NGUỒN GIẢI MÃ Ở ĐÂY
                String decryptedMsg = chat.getMessage();
                try {
                    // Chỉ giải mã nếu tin nhắn không trống
                    if (decryptedMsg != null && !decryptedMsg.isEmpty()) {
                        decryptedMsg = AESUtils.decrypt(decryptedMsg);
                    }
                } catch (Exception e) {
                    // Nếu lỗi (tin nhắn cũ chưa mã hóa), vẫn giữ nguyên text gốc để không bị trống tin nhắn
                    Log.e("AES_Decrypt", "Lỗi giải mã: " + e.getMessage());
                    decryptedMsg = chat.getMessage();
                }

                holder.show_message.setText(decryptedMsg);

                // Cập nhật vị trí txt_status xuống dưới text
                params.addRule(RelativeLayout.BELOW, R.id.show_message);

                // Áp dụng màu chủ đề cho tin nhắn bên phải
                if (getItemViewType(position) == MSG_TYPE_RIGHT) {
                    Drawable background = holder.show_message.getBackground();
                    if (background != null) {
                        Drawable wrappedDrawable = DrawableCompat.wrap(background.mutate());
                        DrawableCompat.setTint(wrappedDrawable, Color.parseColor(themeColor));
                        holder.show_message.setBackground(wrappedDrawable);
                    }
                }
            }
            holder.txt_status.setLayoutParams(params);
        }

        // --- 3. XỬ LÝ THỜI GIAN VÀ TRẠNG THÁI ---
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String time = sdf.format(new Date(chat.getTimestamp()));

        if (holder.txt_status != null) {
            if (chat.getSender().equals(fuser)) {
                if (chat.isPending()) {
                    holder.txt_status.setText("Đang chờ - " + time);
                    if (holder.img_pending != null) holder.img_pending.setVisibility(View.VISIBLE);
                } else {
                    holder.txt_status.setText(chat.isIsseen() ? "Đã xem - " + time : "Đã gửi - " + time);
                    if (holder.img_pending != null) holder.img_pending.setVisibility(View.GONE);
                }
            } else {
                holder.txt_status.setText(time);
            }

            holder.itemView.setOnClickListener(v ->
                    holder.txt_status.setVisibility(holder.txt_status.getVisibility() == View.GONE ? View.VISIBLE : View.GONE));
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (!chat.isPending()) {
                showDeleteDialog(chat);
            }
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

    private void deleteMessage(ChatModel chat) {
        String msgId = chat.getMessageId();
        if (msgId == null) return;

        DatabaseReference messageRef = FirebaseDatabase.getInstance().getReference("messages").child(chatRoomId);
        messageRef.child(msgId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    showMotionToast("Thành công", "Đã xóa tin nhắn", MotionToastStyle.SUCCESS);
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
                                    updateFirebaseLastMessage(chat.getSender(), chat.getReceiver(), newLastMsg, newLastTime);
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                            });
                });

        // Lưu ý: Không xóa ảnh từ link Cloudinary bằng SDK Firebase được
    }

    private void updateFirebaseLastMessage(String sender, String receiver, String msg, long time) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("lastMessage", msg);
        map.put("lastTime", time);
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("chats");
        chatRef.child(sender).child(receiver).updateChildren(map);
        chatRef.child(receiver).child(sender).updateChildren(map);
    }

    private void showMotionToast(String title, String msg, MotionToastStyle style) {
        MotionToast.Companion.createColorToast((Activity) mContext, title, msg,
                style, MotionToast.GRAVITY_BOTTOM, MotionToast.LONG_DURATION,
                ResourcesCompat.getFont(mContext, www.sanju.motiontoast.R.font.helvetica_regular));
    }

    @Override
    public int getItemCount() { return mChat.size(); }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView show_message, txt_status, tvSystemMessage;
        public ImageView img_chat, img_pending;
        public ViewHolder(View itemView) {
            super(itemView);
            show_message = itemView.findViewById(R.id.show_message);
            txt_status = itemView.findViewById(R.id.txt_status);
            img_chat = itemView.findViewById(R.id.img_chat);
            tvSystemMessage = itemView.findViewById(R.id.tvSystemMessage);
            img_pending = itemView.findViewById(R.id.img_pending);
        }
    }

    @Override
    public int getItemViewType(int position) {
        ChatModel chat = mChat.get(position);
        if ("system".equals(chat.getType())) return MSG_TYPE_SYSTEM;
        return chat.getSender().equals(fuser) ? MSG_TYPE_RIGHT : MSG_TYPE_LEFT;
    }
}