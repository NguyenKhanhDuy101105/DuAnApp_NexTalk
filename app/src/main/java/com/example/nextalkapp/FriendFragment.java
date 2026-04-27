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
import java.util.Collections;
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
                    String uid = data.getKey();
                    if (uid == null || uid.equals(currentUid)) continue;

                    String name = data.child("name").getValue(String.class);
                    String avatar = data.child("avatar").getValue(String.class);
                    String status = data.child("status").getValue(String.class);

                    User user = new User();
                    user.uid = uid;
                    user.name = (name != null) ? name : "Người dùng NexTalk";
                    user.avatar = (avatar != null) ? avatar : "";
                    user.status = (status != null) ? status : "offline";

                    listUsers.add(user);
                    listFull.add(user);
                }

                // 🔥 BƯỚC QUAN TRỌNG: Sắp xếp ưu tiên Online lên đầu
                Collections.sort(listUsers, (u1, u2) -> {
                    if (u1.status.equals("online") && !u2.status.equals("online")) return -1;
                    if (!u1.status.equals("online") && u2.status.equals("online")) return 1;
                    return 0; // Giữ nguyên thứ tự nếu cùng trạng thái
                });

                // Cập nhật listFull để khi search cũng theo thứ tự này
                listFull.clear();
                listFull.addAll(listUsers);

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