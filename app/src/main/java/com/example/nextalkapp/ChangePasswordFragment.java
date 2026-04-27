package com.example.nextalkapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChangePasswordFragment extends Fragment {

    private ImageButton btnBack;
    private TextInputEditText edtCurrentPassword, edtNewPassword, edtConfirmNewPassword;
    private MaterialButton btnConfirmChange, btnCancel;
    private DatabaseReference dbRef;
    private String currentUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_password, container, false);

        mapping(view);
        dbRef = FirebaseDatabase.getInstance().getReference();

        SharedPreferences prefs = getContext().getSharedPreferences("USER", Context.MODE_PRIVATE);
        currentUid = prefs.getString("uid", null);

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnCancel.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        btnConfirmChange.setOnClickListener(v -> handleChangePassword());

        return view;
    }

    private void mapping(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        edtCurrentPassword = view.findViewById(R.id.edtCurrentPassword);
        edtNewPassword = view.findViewById(R.id.edtNewPassword);
        edtConfirmNewPassword = view.findViewById(R.id.edtConfirmNewPassword);
        btnConfirmChange = view.findViewById(R.id.btnConfirmChange);
        btnCancel = view.findViewById(R.id.btnCancel);
    }

    private void handleChangePassword() {
        String currentPass = edtCurrentPassword.getText().toString().trim();
        String newPass = edtNewPassword.getText().toString().trim();
        String confirmPass = edtConfirmNewPassword.getText().toString().trim();

        // 1. Kiểm tra rỗng
        if (TextUtils.isEmpty(currentPass) || TextUtils.isEmpty(newPass) || TextUtils.isEmpty(confirmPass)) {
            Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Kiểm tra mật khẩu mới hợp lệ (Độ dài >= 8 và có ký tự đặc biệt)
        if (!newPass.matches("^(?=.*[!@#$%^&*]).{8,}$")) {
            Toast.makeText(getContext(), "Mật khẩu mới phải từ 8 ký tự và có ký tự đặc biệt", Toast.LENGTH_LONG).show();
            return;
        }

        // 3. Kiểm tra mật khẩu mới và xác nhận trùng khớp
        if (!newPass.equals(confirmPass)) {
            Toast.makeText(getContext(), "Mật khẩu mới không trùng khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUid == null) return;

        // 4. Kiểm tra mật khẩu hiện tại trên Firebase
        String hashedCurrent = hashPassword(currentPass);
        dbRef.child("users").child(currentUid).child("password").get().addOnSuccessListener(snapshot -> {
            String dbPassword = snapshot.getValue(String.class);
            if (dbPassword != null && dbPassword.equals(hashedCurrent)) {
                // 5. Lưu mật khẩu mới (đã mã hóa)
                String hashedNew = hashPassword(newPass);
                dbRef.child("users").child(currentUid).child("password").setValue(hashedNew)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(getContext(), "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                            getParentFragmentManager().popBackStack();
                        })
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi kết nối CSDL", Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(getContext(), "Mật khẩu hiện tại không đúng", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi hệ thống", Toast.LENGTH_SHORT).show());
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
}
