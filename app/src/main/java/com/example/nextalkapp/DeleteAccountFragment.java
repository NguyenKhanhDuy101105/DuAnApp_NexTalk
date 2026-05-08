package com.example.nextalkapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class DeleteAccountFragment extends Fragment {

    private ImageButton btnBack;
    private TextInputEditText edtCurrentPassword;
    private MaterialButton btnDeleteAccount;
    private DatabaseReference dbRef;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delete_account, container, false);

        if (getContext() != null) {
            SharedPreferences pref = getContext().getSharedPreferences("USER", Context.MODE_PRIVATE);
            currentUserId = pref.getString("uid", null);
        }

        mapping(view);
        dbRef = FirebaseDatabase.getInstance().getReference();

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        btnDeleteAccount.setOnClickListener(v -> {
            String password = edtCurrentPassword.getText().toString().trim();
            if (password.isEmpty()) {
                edtCurrentPassword.setError("Vui lòng nhập mật khẩu");
                return;
            }
            verifyPasswordAndDelete(password);
        });

        return view;
    }

    private void mapping(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        edtCurrentPassword = view.findViewById(R.id.edtCurrentPassword);
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);
    }

    private void verifyPasswordAndDelete(String password) {
        if (currentUserId == null) return;

        String hashedInput = hashPassword(password);

        dbRef.child("users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String dbPassword = snapshot.child("password").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);

                    if (dbPassword != null && dbPassword.equals(hashedInput)) {
                        showConfirmDeleteDialog(phone);
                    } else {
                        edtCurrentPassword.setError("Mật khẩu không chính xác");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showConfirmDeleteDialog(String phone) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Xác nhận xóa");
        builder.setMessage("Bạn có chắc chắn muốn xóa tài khoản này không? Mọi dữ liệu sẽ bị xóa vĩnh viễn.");
        builder.setPositiveButton("Đồng ý", (dialog, which) -> deleteAccount(phone));
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteAccount(String phone) {
        if (currentUserId == null) return;

        // Xóa từ node users
        dbRef.child("users").child(currentUserId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Xóa từ node phones
                if (phone != null) {
                    dbRef.child("phones").child(phone).removeValue();
                }

                // Xóa dữ liệu local
                if (getContext() != null) {
                    SharedPreferences prefs = getContext().getSharedPreferences("USER", Context.MODE_PRIVATE);
                    prefs.edit().clear().apply();
                }

                showMotionToast("Thành công", "Tài khoản của bạn đã được xóa", MotionToastStyle.SUCCESS);

                // Chuyển về màn hình đăng nhập
                btnDeleteAccount.postDelayed(() -> {
                    Intent intent = new Intent(getActivity(), ManHinhDangNhap.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }, 1500);
            } else {
                showMotionToast("Lỗi", "Không thể xóa tài khoản lúc này", MotionToastStyle.ERROR);
            }
        });
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return password;
        }
    }

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
