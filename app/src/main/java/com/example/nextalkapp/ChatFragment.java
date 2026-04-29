package com.example.nextalkapp;

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

public class ChatFragment extends Fragment {

    private RecyclerView rcvChats, rcvActiveNow;
    private EditText searchBar;
    private ChatAdapter adapter;
    private ActiveAdapter activeAdapter;
    private List<User> list, listFull, listActive;
    private DatabaseReference dbRef;

    public ChatFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        rcvChats = view.findViewById(R.id.rcvChats);
        rcvActiveNow = view.findViewById(R.id.rcvActiveNow);
        searchBar = view.findViewById(R.id.search_bar);

        list = new ArrayList<>();
        listFull = new ArrayList<>();
        listActive = new ArrayList<>();

        adapter = new ChatAdapter(list, user -> startChatMessage(user));
        rcvChats.setLayoutManager(new LinearLayoutManager(getContext()));
        rcvChats.setAdapter(adapter);

        activeAdapter = new ActiveAdapter(listActive, user -> startChatMessage(user));
        rcvActiveNow.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rcvActiveNow.setAdapter(activeAdapter);

        dbRef = FirebaseDatabase.getInstance().getReference();
        loadUsers();
        setupSearch();
        return view;
    }

    private void startChatMessage(User user) {
        Intent intent = new Intent(getActivity(), MessageActivity.class);
        intent.putExtra("receiverUid", user.uid);
        intent.putExtra("receiverName", user.name);
        intent.putExtra("receiverAvatar", user.avatar);
        startActivity(intent);
    }

    private void loadUsers() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences("USER", android.content.Context.MODE_PRIVATE);
        String currentUid = prefs.getString("uid", null);
        if (currentUid == null) return;

        dbRef.child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Sử dụng list tạm để tránh việc RecyclerView update liên tục gây lag
                List<User> tempChatList = new ArrayList<>();
                List<User> tempActiveList = new ArrayList<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    String uid = data.getKey();
                    if (uid == null || uid.equals(currentUid)) continue;

                    String lastMsg = data.child("lastMessage").getValue(String.class);
                    // Nếu rỗng nghĩa là chưa có chat hoặc đã xóa hết sạch tin nhắn
                    if (lastMsg == null || lastMsg.isEmpty()) continue;

                    String name = data.child("name").getValue(String.class);
                    String avatar = data.child("avatar").getValue(String.class);
                    String status = data.child("status").getValue(String.class);
                    Long lastTime = data.child("lastTime").getValue(Long.class);

                    User userObj = new User(uid, name, avatar, lastMsg, lastTime != null ? lastTime : 0, status);

                    if ("online".equals(status)) tempActiveList.add(userObj);
                    tempChatList.add(userObj);
                }

                tempChatList.sort((o1, o2) -> Long.compare(o2.lastTime, o1.lastTime));

                // Cập nhật dữ liệu vào list chính
                list.clear();
                list.addAll(tempChatList);
                listFull.clear();
                listFull.addAll(tempChatList);
                listActive.clear();
                listActive.addAll(tempActiveList);

                adapter.notifyDataSetChanged();
                activeAdapter.notifyDataSetChanged();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filter(String text) {
        list.clear();
        for (User item : listFull) {
            if (item.name.toLowerCase().contains(text.toLowerCase())) list.add(item);
        }
        adapter.notifyDataSetChanged();
    }
}