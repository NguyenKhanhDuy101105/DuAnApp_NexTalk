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
import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {
    private List<User> list;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(User user);
    }

    public FriendAdapter(List<User> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Tái sử dụng item_chat
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        User user = list.get(position);
        holder.txtName.setText(user.name);

        // Ẩn tin nhắn cuối, thời gian và chấm online vì đây là danh bạ thuần túy
        holder.txtLastMessage.setVisibility(View.GONE);
        holder.txtTime.setVisibility(View.GONE);
        holder.viewStatusChat.setVisibility(View.GONE);

        if (user.avatar != null && !user.avatar.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.avatar)
                    .circleCrop()
                    .placeholder(R.drawable.logo2)
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.logo2);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    public static class FriendViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView txtName, txtLastMessage, txtTime;
        View viewStatusChat; // Phải có biến này để ánh xạ từ item_chat

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            txtName = itemView.findViewById(R.id.txtName);
            txtLastMessage = itemView.findViewById(R.id.txtLastMessage);
            txtTime = itemView.findViewById(R.id.txtTime);
            viewStatusChat = itemView.findViewById(R.id.viewStatusChat); // Ánh xạ để tránh crash
        }
    }
}