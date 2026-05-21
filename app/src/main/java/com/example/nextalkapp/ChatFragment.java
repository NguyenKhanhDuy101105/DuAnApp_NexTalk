package com.example.nextalkapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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

import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class ChatFragment extends Fragment {

    private RecyclerView rcvChats, rcvActiveNow;
    private EditText searchBar;
    private ChatAdapter adapter;
    private ActiveAdapter activeAdapter;
    private List<User> list, listFull, listActive;
    private DatabaseReference dbRef;
    private String currentUid;

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

        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences("USER", android.content.Context.MODE_PRIVATE);
            currentUid = prefs.getString("uid", null);
        }

        // Khởi tạo adapter với cả sự kiện Click và Long Click (xóa)
        adapter = new ChatAdapter(list, user -> startChatMessage(user), user -> showDeleteChatDialog(user));
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

    private void showDeleteChatDialog(User user) {
        if (getContext() == null) return;

        // 1. Khởi tạo Builder và nạp Layout custom vào
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_delete_chat, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        // 2. Làm trong suốt background mặc định của hệ thống để hiển thị được góc bo tròn
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // 3. Ánh xạ các View trong layout custom
        TextView tvDialogMessage = view.findViewById(R.id.tvDialogMessage);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnDelete = view.findViewById(R.id.btnDelete);

        // Điền tên người dùng động vào nội dung
        tvDialogMessage.setText("Bạn có chắc chắn muốn xóa cuộc trò chuyện với " + user.name + " không? Hành động này không thể hoàn tác.");

        // 4. Bắt sự kiện cho các nút bấm
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            deleteChat(user); // Gọi hàm xử lý xóa logic của bạn
            dialog.dismiss();
        });

        dialog.show();
    }

    private void deleteChat(User user) {
        if (currentUid == null || user == null || user.uid == null) return;

        // 1. Tạo chatRoomId chuẩn đồng bộ (u1.compareTo(u2) < 0) giống như bên MessageActivity
        String chatRoomId = (currentUid.compareTo(user.uid) < 0) ? currentUid + "_" + user.uid : user.uid + "_" + currentUid;

        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("messages").child(chatRoomId);

        // 2. Duyệt qua tất cả các tin nhắn trong phòng chat này để cập nhật trạng thái xóa ẩn
        messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        String sender = data.child("sender").getValue(String.class);

                        if (sender != null) {
                            // Nếu mình là người gửi (sender) -> chuyển deletedBySender thành true
                            if (sender.equals(currentUid)) {
                                data.getRef().child("deletedBySender").setValue(true);
                            }
                            // Nếu mình không phải người gửi (tức là người nhận) -> chuyển deletedByReceiver thành true
                            else {
                                data.getRef().child("deletedByReceiver").setValue(true);
                            }
                        }
                    }
                }

                // 3. Sau khi đã cập nhật xong trạng thái của các tin nhắn,
                // Tiến hành xóa cuộc trò chuyện ở danh sách bên ngoài (nhánh chats) của riêng mình
                dbRef.child("chats").child(currentUid).child(user.uid).removeValue()
                        .addOnSuccessListener(aVoid -> {
                            showMotionToast("Thành công", "Đã xóa cuộc trò chuyện", MotionToastStyle.SUCCESS);
                        })
                        .addOnFailureListener(e -> {
                            showMotionToast("Lỗi", "Không thể xóa cuộc trò chuyện lúc này", MotionToastStyle.ERROR);
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showMotionToast("Lỗi", "Không thể kết nối đến cơ sở dữ liệu", MotionToastStyle.ERROR);
            }
        });
    }

    private void startChatMessage(User user) {
        Intent intent = new Intent(getActivity(), MessageActivity.class);
        intent.putExtra("receiverUid", user.uid);
        intent.putExtra("receiverName", user.name);
        intent.putExtra("receiverAvatar", user.avatar);
        startActivity(intent);
    }

    private void loadUsers() {
        if (currentUid == null) return;

        dbRef.child("chats").child(currentUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot chatSnapshot) {
                List<User> tempChatList = new ArrayList<>();
                if (!chatSnapshot.exists()) {
                    list.clear();
                    listFull.clear();
                    listActive.clear();
                    adapter.notifyDataSetChanged();
                    activeAdapter.notifyDataSetChanged();
                    return;
                }

                for (DataSnapshot data : chatSnapshot.getChildren()) {
                    String otherUid = data.getKey();
                    String lastMsg = data.child("lastMessage").getValue(String.class);
                    Long lastTime = data.child("lastTime").getValue(Long.class);
                    String chatRoomId = getChatRoomId(currentUid, otherUid);

                    dbRef.child("users").child(otherUid).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            if (userSnapshot.exists()) {
                                String name = userSnapshot.child("name").getValue(String.class);
                                String avatar = userSnapshot.child("avatar").getValue(String.class);
                                String status = userSnapshot.child("status").getValue(String.class);

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

        if (activeAdapter != null) {
            activeAdapter.notifyDataSetChanged();
        }
    }

    private void showMotionToast(String title, String message, MotionToastStyle style) {
        if (getActivity() != null) {
            MotionToast.Companion.createColorToast(getActivity(),
                    title, message, style, MotionToast.GRAVITY_BOTTOM,
                    MotionToast.LONG_DURATION, null);
        }
    }
}