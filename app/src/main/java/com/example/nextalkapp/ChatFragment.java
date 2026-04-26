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

    private RecyclerView rcvChats, rcvActiveNow;
    private EditText searchBar;

    private ChatAdapter adapter;
    private ActiveAdapter activeAdapter; // Adapter cho danh sách ngang

    private List<User> list;
    private List<User> listFull; // dùng cho search
    private List<User> listActive; // Danh sách người dùng đang online

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
        rcvActiveNow = view.findViewById(R.id.rcvActiveNow);
        searchBar = view.findViewById(R.id.search_bar);

        // 🔧 Setup Dữ liệu
        list = new ArrayList<>();
        listFull = new ArrayList<>();
        listActive = new ArrayList<>();

        // 🔧 Setup RecyclerView Chat chính (Dọc)
        adapter = new ChatAdapter(list, user -> startChatMessage(user));
        rcvChats.setLayoutManager(new LinearLayoutManager(getContext()));
        rcvChats.setAdapter(adapter);

        // 🔧 Setup RecyclerView Active Now (Ngang)
        activeAdapter = new ActiveAdapter(listActive, user -> startChatMessage(user));
        LinearLayoutManager horizontalLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        rcvActiveNow.setLayoutManager(horizontalLayoutManager);
        rcvActiveNow.setAdapter(activeAdapter);

        // 🔥 Firebase
        dbRef = FirebaseDatabase.getInstance().getReference();

        loadUsers();
        setupSearch();

        return view;
    }

    // Hàm bổ trợ để chuyển màn hình Chat
    private void startChatMessage(User user) {
        Intent intent = new Intent(getActivity(), MessageActivity.class);
        intent.putExtra("receiverUid", user.uid);
        intent.putExtra("receiverName", user.name);
        intent.putExtra("receiverAvatar", user.avatar);
        startActivity(intent);
    }

    // 📥 Load danh sách user
    private void loadUsers() {
        if (getContext() == null) return;

        SharedPreferences prefs = getSharedPreferencesSafe();
        String currentUid = (prefs != null) ? prefs.getString("uid", null) : null;

        if (currentUid == null) return;

        // Vẫn trỏ vào "users" vì bạn đang lưu lastMessage trực tiếp trong User object
        dbRef.child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                list.clear();
                listFull.clear();
                listActive.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    String uid = data.getKey();

                    // 1. Loại bỏ chính mình
                    if (uid == null || uid.equals(currentUid)) continue;

                    // 2. Kiểm tra xem đã từng nhắn tin chưa
                    // Nếu lastMessage rỗng (null), nghĩa là chưa bao giờ nhắn tin -> Bỏ qua
                    String lastMsg = data.child("lastMessage").getValue(String.class);
                    if (lastMsg == null || lastMsg.isEmpty()) continue;

                    String name = data.child("name").getValue(String.class);
                    String avatar = data.child("avatar").getValue(String.class);
                    String status = data.child("status").getValue(String.class);
                    Long lastTime = data.child("lastTime").getValue(Long.class);

                    // Xử lý giá trị mặc định cho an toàn
                    if (lastTime == null) lastTime = System.currentTimeMillis();
                    if (status == null) status = "offline";

                    User userObj = new User(
                            uid,
                            name != null ? name : "Unknown",
                            avatar != null ? avatar : "",
                            lastMsg,
                            lastTime,
                            status
                    );

                    // 3. Phân loại vào danh sách Active Now (Nếu đang Online)
                    if ("online".equals(status)) {
                        listActive.add(userObj);
                    }

                    // 4. Thêm vào danh sách Chat chính
                    list.add(userObj);
                    listFull.add(userObj);
                }

                // 5. Sắp xếp danh sách chat theo thời gian mới nhất lên đầu
                list.sort((o1, o2) -> Long.compare(o2.lastTime, o1.lastTime));

                adapter.notifyDataSetChanged();
                activeAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if(getContext() != null)
                    Toast.makeText(getContext(), "Lỗi load dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private SharedPreferences getSharedPreferencesSafe() {
        if (getContext() != null) {
            return getContext().getSharedPreferences("USER", android.content.Context.MODE_PRIVATE);
        }
        return null;
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