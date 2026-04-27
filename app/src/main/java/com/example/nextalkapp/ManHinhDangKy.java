package com.example.nextalkapp;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class ManHinhDangKy extends AppCompatActivity {

    TextView tvLogin;
    TextInputEditText edtFullName, edtEmail, edtPhone, edtPassword, edtConfirmPassword;
    MaterialButton btnSignUp;
    DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_man_hinh_dang_ky);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mapping();

        dbRef = FirebaseDatabase.getInstance().getReference();

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, ManHinhDangNhap.class));
        });

        btnSignUp.setOnClickListener(v -> handleSignUp());
    }

    private void mapping() {
        tvLogin = findViewById(R.id.tvLogin);
        edtFullName = findViewById(R.id.edtFullName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
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
        MotionToast.Companion.createColorToast(this,
                title,
                message,
                style,
                MotionToast.GRAVITY_BOTTOM,
                MotionToast.LONG_DURATION,
                Typeface.SANS_SERIF);
    }

    private void handleSignUp() {
        String name = edtFullName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirm = edtConfirmPassword.getText().toString().trim();

        // --- VALIDATION CHI TIẾT ---

        if (TextUtils.isEmpty(name)) {
            edtFullName.setError("Vui lòng nhập họ tên");
            edtFullName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Vui lòng nhập Email");
            edtEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Định dạng Email không hợp lệ");
            edtEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            edtPhone.setError("Vui lòng nhập số điện thoại");
            edtPhone.requestFocus();
            return;
        }

        if (!phone.matches("^0\\d{9}$")) {
            edtPhone.setError("SĐT phải bắt đầu bằng số 0 và có 10 chữ số");
            edtPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Vui lòng nhập mật khẩu");
            edtPassword.requestFocus();
            return;
        }

        // Kiểm tra mật khẩu mạnh (8 ký tự, ít nhất 1 ký tự đặc biệt)
        if (!password.matches("^(?=.*[!@#$%^&*]).{8,}$")) {
            edtPassword.setError("Mật khẩu tối thiểu 8 ký tự và 1 ký tự đặc biệt (!@#$%^&*)");
            edtPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirm)) {
            edtConfirmPassword.setError("Vui lòng xác nhận lại mật khẩu");
            edtConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirm)) {
            edtConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            edtConfirmPassword.requestFocus();
            return;
        }

        // 🔍 Kiểm tra trùng lặp trên Firebase
        checkAvailability(name, email, phone, password);
    }

    private void checkAvailability(String name, String email, String phone, String password) {
        // Check Phone trước
        dbRef.child("phones").child(phone).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                edtPhone.setError("Số điện thoại này đã được đăng ký");
                edtPhone.requestFocus();
                return;
            }

            // Check Email
            dbRef.child("users").orderByChild("email").equalTo(email).get().addOnSuccessListener(emailSnap -> {
                if (emailSnap.exists()) {
                    edtEmail.setError("Email này đã được sử dụng");
                    edtEmail.requestFocus();
                } else {
                    createUser(name, email, phone, password);
                }
            }).addOnFailureListener(e -> showMotionToast("Lỗi", "Không thể kiểm tra Email", MotionToastStyle.ERROR));

        }).addOnFailureListener(e -> showMotionToast("Lỗi", "Không thể kiểm tra SĐT", MotionToastStyle.ERROR));
    }

    private void createUser(String name, String email, String phone, String password) {
        String uid = dbRef.child("users").push().getKey();
        if (uid == null) return;

        String hashedPassword = hashPassword(password);
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("uid", uid); // Nên lưu cả UID vào trong Object cho dễ truy vấn
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("phone", phone);
        userMap.put("password", hashedPassword);
        userMap.put("bio", "Chào mừng bạn đến với NexTalk!");
        userMap.put("avatar", "");
        userMap.put("status", "offline");
        userMap.put("lastSeen", System.currentTimeMillis());
        userMap.put("createdAt", System.currentTimeMillis());

        dbRef.child("users").child(uid).setValue(userMap)
                .addOnSuccessListener(unused -> {
                    dbRef.child("phones").child(phone).setValue(uid);
                    showMotionToast("Thành công", "Tài khoản của Duy đã sẵn sàng!", MotionToastStyle.SUCCESS);

                    // Chuyển về màn hình đăng nhập sau 1.5s để người dùng kịp thấy Toast thành công
                    btnSignUp.postDelayed(() -> {
                        startActivity(new Intent(ManHinhDangKy.this, ManHinhDangNhap.class));
                        finish();
                    }, 1500);
                })
                .addOnFailureListener(e -> showMotionToast("Lỗi tạo user", e.getMessage(), MotionToastStyle.ERROR));
    }
}