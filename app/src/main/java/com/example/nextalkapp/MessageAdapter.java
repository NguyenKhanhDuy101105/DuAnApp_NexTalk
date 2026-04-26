package com.example.nextalkapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nextalkapp.Model.ChatModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    public static final int MSG_TYPE_LEFT = 0;
    public static final int MSG_TYPE_RIGHT = 1;

    private Context mContext;
    private List<ChatModel> mChat;
    private String fuser;

    public MessageAdapter(Context mContext, List<ChatModel> mChat) {
        this.mChat = mChat;
        this.mContext = mContext;

        SharedPreferences prefs = mContext.getSharedPreferences("USER", Context.MODE_PRIVATE);
        fuser = prefs.getString("uid", "");
    }

    @NonNull
    @Override
    public MessageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MSG_TYPE_RIGHT) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_chat_right, parent, false);
            return new ViewHolder(view);
        } else {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_chat_left, parent, false);
            return new ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.ViewHolder holder, int position) {
        ChatModel chat = mChat.get(position);
        holder.show_message.setText(chat.getMessage());

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String time = sdf.format(new Date(chat.getTimestamp()));

        // Nếu là tin nhắn của mình gửi
        if (chat.getSender().equals(fuser)) {
            if (chat.isIsseen()) {
                holder.txt_status.setText("Đã xem - " + time);
            } else {
                holder.txt_status.setText("Đã gửi - " + time);
            }
        } else {
            // Tin nhắn của người kia gửi cho mình
            holder.txt_status.setText(time);
        }

        // Luôn ẩn lúc đầu, click mới hiện (hoặc tùy bạn muốn)
        holder.itemView.setOnClickListener(v -> {
            holder.txt_status.setVisibility(
                    holder.txt_status.getVisibility() == View.GONE ? View.VISIBLE : View.GONE
            );
        });
    }

    @Override
    public int getItemCount() {
        return mChat.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView show_message;
        public TextView txt_status; // Thêm biến cho TextView trạng thái

        public ViewHolder(View itemView) {
            super(itemView);
            show_message = itemView.findViewById(R.id.show_message);
            txt_status = itemView.findViewById(R.id.txt_status); // Ánh xạ từ XML
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mChat.get(position).getSender().equals(fuser)) {
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }
}