package com.example.nextalkapp;

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

        // Hiển thị tin nhắn cuối cùng (nếu trống thì để mặc định)
        if (user.lastMessage != null && !user.lastMessage.isEmpty()) {
            holder.txtLastMessage.setText(user.lastMessage);
        } else {
            holder.txtLastMessage.setText("Bắt đầu cuộc trò chuyện");
        }

        // --- LOGIC HIỂN THỊ THỜI GIAN THÔNG MINH ---
        long currentTime = System.currentTimeMillis();
        long lastTime = user.lastTime;
        String formattedTime;

        // Kiểm tra nếu tin nhắn cũ hơn 24 giờ (24 * 60 * 60 * 1000 miliseconds)
        if (currentTime - lastTime > 86400000) {
            // Định dạng: Ngày/Tháng Giờ:Phút (Ví dụ: 25/04 15:30)
            SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
            formattedTime = sdfDate.format(new Date(lastTime));
        } else {
            // Định dạng: Giờ:Phút 24h (Ví dụ: 15:30)
            SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
            formattedTime = sdfTime.format(new Date(lastTime));
        }

        holder.txtTime.setText(formattedTime);
        // ------------------------------------------

        // Hiển thị ảnh đại diện
        if (user.avatar != null && !user.avatar.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.avatar)
                    .circleCrop()
                    .placeholder(R.drawable.logo2)
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.logo2);
        }

        // Hiển thị chấm trạng thái Online/Offline
        if ("online".equals(user.status)) {
            holder.viewStatusChat.setVisibility(View.VISIBLE);
        } else {
            holder.viewStatusChat.setVisibility(View.GONE);
        }

        // Sự kiện click vào item
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