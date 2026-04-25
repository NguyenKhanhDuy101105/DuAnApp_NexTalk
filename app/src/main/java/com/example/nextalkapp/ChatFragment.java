package com.example.nextalkapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

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

    private RecyclerView rcvChats;
    private EditText searchBar;

    private ChatAdapter adapter;
    private List<User> list;
    private List<User> listFull; // dùng cho search

    private DatabaseReference dbRef;

    public ChatFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        // 🔗 Mapping
        rcvChats = view.findViewById(R.id.rcvChats);
        searchBar = view.findViewById(R.id.search_bar);

        // 🔧 Setup RecyclerView
        list = new ArrayList<>();
        listFull = new ArrayList<>();
        adapter = new ChatAdapter(list);

        rcvChats.setLayoutManager(new LinearLayoutManager(getContext()));
        rcvChats.setAdapter(adapter);

        // 🔥 Firebase
        dbRef = FirebaseDatabase.getInstance().getReference();

        loadUsers();
        setupSearch();

        return view;
    }

    // 📥 Load danh sách user
    private void loadUsers() {

        if (getContext() == null) return;

        SharedPreferences prefs = getContext().getSharedPreferences("USER", getContext().MODE_PRIVATE);
        String currentUid = prefs.getString("uid", null);

        if (currentUid == null) {
//            Toast.makeText(getContext(), "Chưa có UID!", Toast.LENGTH_SHORT).show();
            return;
        }

        dbRef.child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                list.clear();
                listFull.clear();

                for (DataSnapshot data : snapshot.getChildren()) {

                    String uid = data.getKey();

                    if (uid == null) continue;

                    // 🚫 bỏ chính mình
                    if (uid.equals(currentUid)) continue;

                    String name = data.child("name").getValue(String.class);
                    String avatar = data.child("avatar").getValue(String.class);

                    User chat = new User(
                            uid,
                            name != null ? name : "Unknown",
                            avatar != null ? avatar : "",
                            "Chưa có tin nhắn",
                            System.currentTimeMillis()
                    );

                    list.add(chat);
                    listFull.add(chat);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Lỗi load dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔍 Search user
    private void setupSearch() {
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
        list.clear();

        for (User item : listFull) {
            if (item.name.toLowerCase().contains(text.toLowerCase())) {
                list.add(item);
            }
        }

        adapter.notifyDataSetChanged();
    }
}