package com.example.nextalkapp;

import android.content.Intent;
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

    // 🔐 Hash password
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

    // 🚀 Tạo user
    private void createUser(String name, String email, String phone, String password) {

        String uid = dbRef.child("users").push().getKey();

        if (uid == null) {
            Toast.makeText(this, "Lỗi tạo UID", Toast.LENGTH_SHORT).show();
            return;
        }

        String hashedPassword = hashPassword(password);

        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("phone", phone);
        userMap.put("password", hashedPassword);
        userMap.put("bio", "");
        userMap.put("avatar", "");
        userMap.put("status", "offline");
        userMap.put("lastSeen", System.currentTimeMillis());
        userMap.put("createdAt", System.currentTimeMillis());

        dbRef.child("users").child(uid).setValue(userMap)
                .addOnSuccessListener(unused -> {

                    dbRef.child("phones").child(phone).setValue(uid);

                    Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();

                    startActivity(new Intent(this, ManHinhDangNhap.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_ERROR", "Create user lỗi", e);
                    Toast.makeText(this, "Lỗi tạo user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // 🔍 Validate + check DB
    private void handleSignUp() {

        String name = edtFullName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirm = edtConfirmPassword.getText().toString().trim();

        // 1️⃣ Check rỗng
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(phone) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirm)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2️⃣ Email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3️⃣ Phone VN
        if (!phone.matches("^0\\d{9}$")) {
            Toast.makeText(this, "SĐT không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // 4️⃣ Password mạnh
        if (!password.matches("^(?=.*[!@#$%^&*]).{8,}$")) {
            Toast.makeText(this, "Mật khẩu yếu", Toast.LENGTH_SHORT).show();
            return;
        }

        // 5️⃣ Confirm
        if (!password.equals(confirm)) {
            Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔍 Check PHONE trước (nhanh)
        dbRef.child("phones").child(phone).get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.exists()) {
                        Toast.makeText(this, "SĐT đã tồn tại", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 🔍 Check EMAIL
                    dbRef.child("users")
                            .orderByChild("email")
                            .equalTo(email)
                            .get()
                            .addOnSuccessListener(emailSnap -> {

                                if (emailSnap.exists()) {
                                    Toast.makeText(this, "Email đã tồn tại", Toast.LENGTH_SHORT).show();
                                } else {
                                    createUser(name, email, phone, password);
                                }

                            })
                            .addOnFailureListener(e -> {
                                Log.e("FIREBASE_ERROR", "Check email lỗi", e);
                                Toast.makeText(this, "Lỗi check Email: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });

                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_ERROR", "Check phone lỗi", e);
                    Toast.makeText(this, "Lỗi check SĐT: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}