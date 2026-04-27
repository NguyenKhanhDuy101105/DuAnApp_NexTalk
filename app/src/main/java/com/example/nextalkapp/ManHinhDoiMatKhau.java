package com.example.nextalkapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ManHinhDoiMatKhau extends AppCompatActivity {

    private TextInputEditText edtPassword, edtConfirmPassword;
    private MaterialButton btnResetPassword;
    private DatabaseReference dbRef;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_man_hinh_doi_mat_khau);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Nhận email từ màn hình OTP
        userEmail = getIntent().getStringExtra("user_email");

        mapping();
        dbRef = FirebaseDatabase.getInstance().getReference();

        btnResetPassword.setOnClickListener(v -> handleResetPassword());
    }

    private void mapping() {
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);
    }

    // 🔐 Hash password (sử dụng lại logic từ ManHinhDangKy)
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

    private void handleResetPassword() {
        String password = edtPassword.getText().toString().trim();
        String confirm = edtConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(password) || TextUtils.isEmpty(confirm)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.matches("^(?=.*[!@#$%^&*]).{8,}$")) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 8 ký tự và 1 ký tự đặc biệt", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirm)) {
            Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cập nhật mật khẩu lên Firebase
        resetPasswordOnFirebase(password);
    }

    private void resetPasswordOnFirebase(String newPassword) {
        btnResetPassword.setEnabled(false);
        btnResetPassword.setText("Đang cập nhật...");

        String hashedNewPassword = hashPassword(newPassword);

        // Tìm user có email tương ứng để cập nhật
        dbRef.child("users").orderByChild("email").equalTo(userEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                userSnapshot.getRef().child("password").setValue(hashedNewPassword)
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(ManHinhDoiMatKhau.this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(ManHinhDoiMatKhau.this, ManHinhDangNhap.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            btnResetPassword.setEnabled(true);
                                            btnResetPassword.setText("Cập nhật mật khẩu");
                                            Toast.makeText(ManHinhDoiMatKhau.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            }
                        } else {
                            btnResetPassword.setEnabled(true);
                            btnResetPassword.setText("Cập nhật mật khẩu");
                            Toast.makeText(ManHinhDoiMatKhau.this, "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        btnResetPassword.setEnabled(true);
                        btnResetPassword.setText("Cập nhật mật khẩu");
                    }
                });
    }
}