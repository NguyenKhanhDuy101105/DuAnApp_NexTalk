package com.example.nextalkapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.nextalkapp.Model.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<User> list;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(User user);
    }

    public ChatAdapter(List<User> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        User user = list.get(position);

        holder.txtName.setText(user.name);

        // --- XỬ LÝ HIỂN THỊ "BẠN" VÀ GIẢI MÃ TIN NHẮN CUỐI ---
        SharedPreferences prefs = holder.itemView.getContext().getSharedPreferences("USER", Context.MODE_PRIVATE);
        String myName = prefs.getString("name", "");
        String lastMsg = user.lastMessage;

        if (lastMsg != null && !lastMsg.isEmpty()) {
            // 1. GIẢI MÃ TRƯỚC (Chỉ giải mã nếu không phải là hình ảnh)
            if (!lastMsg.equals("[Hình ảnh]")) {
                try {
                    lastMsg = AESUtils.decrypt(lastMsg);
                } catch (Exception e) {
                    // Nếu lỗi (có thể là tin nhắn cũ chưa mã hóa), giữ nguyên để không bị mất text
                    android.util.Log.e("AES_ChatAdapter", "Lỗi giải mã: " + e.getMessage());
                }
            }

            // 2. SAU ĐÓ MỚI XỬ LÝ CHỮ "BẠN"
            if (!myName.isEmpty() && lastMsg.startsWith(myName)) {
                lastMsg = lastMsg.replaceFirst(myName, "Bạn");
            }
            holder.txtLastMessage.setText(lastMsg);
        } else {
            holder.txtLastMessage.setText("Bắt đầu cuộc trò chuyện");
        }

        // --- LOGIC HIỂN THỊ THỜI GIAN THÔNG MINH ---
        long currentTime = System.currentTimeMillis();
        long lastTime = user.lastTime;
        String formattedTime;

        if (currentTime - lastTime > 86400000) {
            SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
            formattedTime = sdfDate.format(new Date(lastTime));
        } else {
            SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
            formattedTime = sdfTime.format(new Date(lastTime));
        }
        holder.txtTime.setText(formattedTime);

        // --- AVATAR & STATUS ---
        if (user.avatar != null && !user.avatar.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.avatar)
                    .circleCrop()
                    .placeholder(R.drawable.logo2)
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.logo2);
        }

        if ("online".equals(user.status)) {
            holder.viewStatusChat.setVisibility(View.VISIBLE);
        } else {
            holder.viewStatusChat.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView txtName, txtLastMessage, txtTime;
        View viewStatusChat;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            txtName = itemView.findViewById(R.id.txtName);
            txtLastMessage = itemView.findViewById(R.id.txtLastMessage);
            txtTime = itemView.findViewById(R.id.txtTime);
            viewStatusChat = itemView.findViewById(R.id.viewStatusChat);
        }
    }
}