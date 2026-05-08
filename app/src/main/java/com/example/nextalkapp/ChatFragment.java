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

        List<User> tempChatList = new ArrayList<>();

        dbRef.child("chats").child(currentUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot chatSnapshot) {
                for (DataSnapshot data : chatSnapshot.getChildren()) {
                    String otherUid = data.getKey();
                    String lastMsg = data.child("lastMessage").getValue(String.class);
                    Long lastTime = data.child("lastTime").getValue(Long.class);
                    String chatRoomId = getChatRoomId(currentUid, otherUid);

                    // Lấy thông tin User và Biệt danh song song
                    dbRef.child("users").child(otherUid).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            if (userSnapshot.exists()) {
                                String name = userSnapshot.child("name").getValue(String.class);
                                String avatar = userSnapshot.child("avatar").getValue(String.class);
                                String status = userSnapshot.child("status").getValue(String.class);

                                // Lấy biệt danh từ node nicknames
                                dbRef.child("nicknames").child(chatRoomId).child(otherUid).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot nickSnapshot) {
                                        String nickname = nickSnapshot.getValue(String.class);
                                        String displayName = (nickname != null && !nickname.isEmpty()) ? nickname : name;

                                        User userObj = new User(otherUid, displayName, avatar, lastMsg,
                                                lastTime != null ? lastTime : 0, status);
                                        
                                        addToTempList(userObj, tempChatList);
                                    }
                                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                                });
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String getChatRoomId(String uid1, String uid2) {
        return (uid1.compareTo(uid2) < 0) ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
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

    private void addToTempList(User user, List<User> tempList) {
        int index = -1;
        for (int i = 0; i < tempList.size(); i++) {
            if (tempList.get(i).uid.equals(user.uid)) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            tempList.set(index, user);
        } else {
            tempList.add(user);
        }

        tempList.sort((o1, o2) -> Long.compare(o2.lastTime, o1.lastTime));

        list.clear();
        list.addAll(tempList);
        listFull.clear();
        listFull.addAll(tempList);

        updateActiveList();
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void updateActiveList() {
        listActive.clear();
        for (User u : listFull) {
            if ("online".equals(u.status)) {
                listActive.add(u);
            }
        }
        if (activeAdapter != null) activeAdapter.notifyDataSetChanged();
    }
}
