package com.example.nextalkapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nextalkapp.Model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FriendFragment extends Fragment {

    private RecyclerView rcvFriends;
    private EditText searchBar;
    private FriendAdapter adapter;
    private List<User> listUsers;
    private List<User> listFull;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friend, container, false);

        rcvFriends = view.findViewById(R.id.rcvFriends);
        searchBar = view.findViewById(R.id.search_bar_friend);

        listUsers = new ArrayList<>();
        listFull = new ArrayList<>();

        adapter = new FriendAdapter(listUsers, user -> {
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), MessageActivity.class);
                intent.putExtra("receiverUid", user.uid);
                intent.putExtra("receiverName", user.name);
                intent.putExtra("receiverAvatar", user.avatar);
                startActivity(intent);
            }
        });

        rcvFriends.setLayoutManager(new LinearLayoutManager(getContext()));
        rcvFriends.setAdapter(adapter);

        loadAllUsers();
        setupSearch();

        return view;
    }

    private void loadAllUsers() {
        if (getContext() == null) return;

        SharedPreferences prefs = getContext().getSharedPreferences("USER", Context.MODE_PRIVATE);
        String currentUid = prefs.getString("uid", "");

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;

                listUsers.clear();
                listFull.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    // Lấy UID từ Key của node (đây là cách an toàn nhất)
                    String uid = data.getKey();

                    // 1. Loại bỏ bản thân khỏi danh bạ
                    if (uid == null || uid.equals(currentUid)) continue;

                    // 2. Lấy dữ liệu từng trường một cách an toàn (tránh lỗi ép kiểu POJO)
                    String name = data.child("name").getValue(String.class);
                    String avatar = data.child("avatar").getValue(String.class);
                    String status = data.child("status").getValue(String.class);
                    String phone = data.child("phone").getValue(String.class);
                    String bio = data.child("bio").getValue(String.class);
                    String lastMsg = data.child("lastMessage").getValue(String.class);

                    // Xử lý riêng cho kiểu Long để tránh crash
                    Long lastTimeObj = data.child("lastTime").getValue(Long.class);
                    long lastTime = (lastTimeObj != null) ? lastTimeObj : System.currentTimeMillis();

                    // 3. Khởi tạo đối tượng User thủ công
                    User user = new User(
                            uid,
                            name != null ? name : "Người dùng NexTalk",
                            phone != null ? phone : "",
                            bio != null ? bio : "",
                            avatar != null ? avatar : "",
                            lastMsg != null ? lastMsg : "",
                            lastTime,
                            status != null ? status : "offline"
                    );

                    listUsers.add(user);
                    listFull.add(user);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupSearch() {
        if (searchBar == null) return;
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filter(String text) {
        listUsers.clear();
        for (User item : listFull) {
            if (item.name != null && item.name.toLowerCase().contains(text.toLowerCase())) {
                listUsers.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }
}