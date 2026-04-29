package com.example.nextalkapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class SettingFragment extends Fragment {

    private MaterialButton btnLogout;
    private LinearLayout itemAccount, itemPrivacy;
    private androidx.appcompat.widget.SwitchCompat switchDarkMode;
    private DatabaseReference dbRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        mapping(view);
        dbRef = FirebaseDatabase.getInstance().getReference();
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
        // Chuyển sang màn hình Quản lý hồ sơ
        itemAccount.setOnClickListener(v -> {
            EditProfileFragment editProfileFragment = new EditProfileFragment();
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                            android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.fragment_container, editProfileFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Chuyển sang màn hình Đổi mật khẩu
        itemPrivacy.setOnClickListener(v -> {
            ChangePasswordFragment changePasswordFragment = new ChangePasswordFragment();
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                            android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.fragment_container, changePasswordFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Sự kiện Đăng xuất
        btnLogout.setOnClickListener(v -> showLogoutDialog());

        // Sự kiện gạt Chế độ tối
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String msg = isChecked ? "Đã bật chế độ tối" : "Đã tắt chế độ tối";
            showMotionToast("Giao diện", msg, MotionToastStyle.INFO);
        });
    }

    private void showLogoutDialog() {
        if (getContext() == null) return;

        View view = getLayoutInflater().inflate(R.layout.layout_custom_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext()).setView(view).create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

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

        // Cập nhật trạng thái offline trước khi xóa dữ liệu local
        if (uid != null) {
            dbRef.child("users").child(uid).child("status").setValue("offline");
        }

        prefs.edit().clear().apply();

        showMotionToast("Đăng xuất", "Hẹn gặp lại bạn tại NexTalk!", MotionToastStyle.SUCCESS);

        // Delay một chút để hiệu ứng Toast hiện lên trước khi chuyển màn hình
        btnLogout.postDelayed(() -> {
            Intent intent = new Intent(getActivity(), ManHinhDangNhap.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }, 1000);
    }

    // Hàm gọi MotionToast dùng chung trong Fragment
    private void showMotionToast(String title, String message, MotionToastStyle style) {
        if (getActivity() != null) {
            MotionToast.Companion.createColorToast(getActivity(),
                    title,
                    message,
                    style,
                    MotionToast.GRAVITY_BOTTOM,
                    MotionToast.LONG_DURATION,
                    Typeface.SANS_SERIF);
        }
    }
}