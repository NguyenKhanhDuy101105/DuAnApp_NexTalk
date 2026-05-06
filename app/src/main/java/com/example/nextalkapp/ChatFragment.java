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

import com.example.nextalkapp.Model.OfflineMessage;
import com.example.nextalkapp.Model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatFragment extends Fragment {

    private RecyclerView rcvChats, rcvActiveNow;
    private EditText searchBar;
    private ChatAdapter adapter;
    private ActiveAdapter activeAdapter;
    private List<User> list, listFull, listActive;
    private DatabaseReference dbRef;
    private OfflineDbHelper offlineDbHelper;

    public ChatFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        rcvChats = view.findViewById(R.id.rcvChats);
        rcvActiveNow = view.findViewById(R.id.rcvActiveNow);
        searchBar = view.findViewById(R.id.search_bar);

        offlineDbHelper = new OfflineDbHelper(getContext());

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
                List<User> tempChatList = new ArrayList<>();
                List<User> tempActiveList = new ArrayList<>();

                // 1. Lấy danh sách tin nhắn pending từ SQLite để so sánh
                List<OfflineMessage> pendingList = offlineDbHelper.getAllPendingMessages();
                Map<String, OfflineMessage> latestPendingMap = new HashMap<>();
                for (OfflineMessage om : pendingList) {
                    // Chỉ lấy tin nhắn mới nhất cho mỗi người nhận
                    if (!latestPendingMap.containsKey(om.getReceiver()) || 
                        om.getTimestamp() > latestPendingMap.get(om.getReceiver()).getTimestamp()) {
                        latestPendingMap.put(om.getReceiver(), om);
                    }
                }

                for (DataSnapshot data : snapshot.getChildren()) {
                    String uid = data.getKey();
                    if (uid == null || uid.equals(currentUid)) continue;

                    String name = data.child("name").getValue(String.class);
                    String avatar = data.child("avatar").getValue(String.class);
                    String status = data.child("status").getValue(String.class);
                    
                    String lastMsg = data.child("lastMessage").getValue(String.class);
                    Long lastTime = data.child("lastTime").getValue(Long.class);
                    if (lastTime == null) lastTime = 0L;

                    boolean isPending = false;

                    // 2. Kiểm tra xem có tin nhắn offline nào mới hơn không
                    if (latestPendingMap.containsKey(uid)) {
                        OfflineMessage om = latestPendingMap.get(uid);
                        if (om.getTimestamp() > lastTime) {
                            lastMsg = om.getMessage();
                            lastTime = om.getTimestamp();
                            isPending = true;
                        }
                    }

                    // Nếu hoàn toàn không có tin nhắn (cả Firebase lẫn Offline) thì không hiện ở Chat list
                    if (lastMsg == null || lastMsg.isEmpty()) continue;

                    User userObj = new User(uid, name, avatar, lastMsg, lastTime, status);
                    userObj.setLastMsgPending(isPending);

                    if ("online".equals(status)) tempActiveList.add(userObj);
                    tempChatList.add(userObj);
                }

                // Sắp xếp theo thời gian mới nhất
                tempChatList.sort((o1, o2) -> Long.compare(o2.lastTime, o1.lastTime));

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
