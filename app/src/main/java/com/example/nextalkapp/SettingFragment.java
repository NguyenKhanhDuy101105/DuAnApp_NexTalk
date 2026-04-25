package com.example.nextalkapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SettingFragment extends Fragment {

    // Khai báo đầy đủ các thành phần sẽ dùng
    private MaterialButton btnLogout;
    private LinearLayout itemAccount, itemPrivacy;
    private androidx.appcompat.widget.SwitchCompat switchDarkMode;
    private DatabaseReference dbRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        // 1. Gọi hàm mapping
        mapping(view);

        // 2. Cấu hình Firebase
        dbRef = FirebaseDatabase.getInstance().getReference();

        // 3. Thiết lập các sự kiện Click
        setupEvents();

        return view;
    }

    private void mapping(View view) {
        btnLogout = view.findViewById(R.id.btnLogout);
        itemAccount = view.findViewById(R.id.itemAccount);
        itemPrivacy = view.findViewById(R.id.itemPrivacy);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
    }

    private void setupEvents() {
        // Sự kiện chuyển sang màn hình Quản lý hồ sơ
        // Trong SettingFragment.java, tại sự kiện click itemAccount
        itemAccount.setOnClickListener(v -> {
            EditProfileFragment editProfileFragment = new EditProfileFragment();

            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            android.R.anim.fade_in,  // Hiệu ứng hiện ra
                            android.R.anim.fade_out, // Hiệu ứng biến mất
                            android.R.anim.fade_in,  // Hiệu ứng khi bấm Back hiện lại
                            android.R.anim.fade_out  // Hiệu ứng khi bấm Back biến mất
                    )
                    .replace(R.id.fragment_container, editProfileFragment)
                    .addToBackStack(null) // Để khi ấn Back nó quay lại màn hình Setting
                    .commit();
        });

        // Sự kiện Đổi mật khẩu (nếu bạn có màn hình này)
        itemPrivacy.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
        });

        // Sự kiện Đăng xuất
        btnLogout.setOnClickListener(v -> showLogoutDialog());

        // Sự kiện gạt Chế độ tối
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(getContext(), "Bật Chế độ tối", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Tắt Chế độ tối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLogoutDialog() {
        View view = getLayoutInflater().inflate(R.layout.layout_custom_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext()).setView(view).create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Ánh xạ nút trong Custom Dialog
        view.findViewById(R.id.btnConfirmLogout).setOnClickListener(v -> {
            dialog.dismiss();
            handleLogout();
        });

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void handleLogout() {
        if (getContext() == null) return;

        SharedPreferences prefs = getContext().getSharedPreferences("USER", getContext().MODE_PRIVATE);
        String uid = prefs.getString("uid", null);

        if (uid != null) {
            dbRef.child("users").child(uid).child("status").setValue("offline");
        }

        prefs.edit().clear().apply();
        Toast.makeText(getContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(getActivity(), ManHinhDangNhap.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}