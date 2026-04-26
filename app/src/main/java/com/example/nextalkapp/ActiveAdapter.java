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

// File: ActiveAdapter.java
public class ActiveAdapter extends RecyclerView.Adapter<ActiveAdapter.ActiveViewHolder> {
    private List<User> list;
    private ChatAdapter.OnItemClickListener listener;

    public ActiveAdapter(List<User> list, ChatAdapter.OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ActiveViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng file layout item hình tròn mà mình đã gợi ý ở câu trước
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_active_user, parent, false);
        return new ActiveViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActiveViewHolder holder, int position) {
        User user = list.get(position);
        holder.txtName.setText(user.name);

        Glide.with(holder.itemView.getContext())
                .load(user.avatar)
                .circleCrop()
                .placeholder(R.drawable.logo2)
                .into(holder.imgAvatar);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(user));
    }

    @Override
    public int getItemCount() { return list.size(); }

    public static class ActiveViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView txtName;
        public ActiveViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgActiveAvatar);
            txtName = itemView.findViewById(R.id.txtActiveName);
        }
    }
}