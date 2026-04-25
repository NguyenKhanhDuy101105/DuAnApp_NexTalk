package com.example.nextalkapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.CheckBox;
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

public class ManHinhDangNhap extends AppCompatActivity {

    TextView tvLoginSignUp, tvLoginForgotPassword;
    CheckBox cbLoginRemember;
    MaterialButton btnLogin;
    TextInputEditText edtLoginEmailOrPhone, edtLoginPassword;

    DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_man_hinh_dang_nhap);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mapping();

        dbRef = FirebaseDatabase.getInstance().getReference();

        // 🔥 Auto login nếu đã lưu
        SharedPreferences pref = getSharedPreferences("USER", MODE_PRIVATE);
        String savedUid = pref.getString("uid", null);
        if (savedUid != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        tvLoginSignUp.setOnClickListener(v -> {
            startActivity(new Intent(this, ManHinhDangKy.class));
        });

        tvLoginForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ManHinhGuiOTP.class));
        });

        btnLogin.setOnClickListener(v -> handleLogin());
    }

    private void mapping() {
        tvLoginSignUp = findViewById(R.id.tvLoginSignUp);
        tvLoginForgotPassword = findViewById(R.id.tvLoginForgotPassword);
        edtLoginEmailOrPhone = findViewById(R.id.edtLoginEmailOrPhone);
        edtLoginPassword = findViewById(R.id.edtLoginPassword);
        cbLoginRemember = findViewById(R.id.cbLoginRemember);
        btnLogin = findViewById(R.id.btnLogin);
    }

    // 🔐 Hash password giống đăng ký
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

    // 🚀 LOGIN
    private void handleLogin() {

        String input = edtLoginEmailOrPhone.getText().toString().trim();
        String password = edtLoginPassword.getText().toString().trim();

        // 1️⃣ Validate rỗng
        if (TextUtils.isEmpty(input) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2️⃣ Validate password
        if (password.length() < 8) {
            Toast.makeText(this, "Mật khẩu không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        String hashedPassword = hashPassword(password);

        // 🔍 LOGIN BẰNG EMAIL
        if (Patterns.EMAIL_ADDRESS.matcher(input).matches()) {

            dbRef.child("users")
                    .orderByChild("email")
                    .equalTo(input)
                    .get()
                    .addOnSuccessListener(snapshot -> {

                        if (!snapshot.exists()) {
                            Toast.makeText(this, "Email không tồn tại", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        snapshot.getChildren().forEach(userSnap -> {

                            String dbPassword = userSnap.child("password").getValue(String.class);

                            if (dbPassword != null && dbPassword.equals(hashedPassword)) {

                                String uid = userSnap.getKey();

                                // 🟢 update trạng thái
                                dbRef.child("users").child(uid).child("status").setValue("online");

                                // 💾 remember login
                                if (cbLoginRemember.isChecked()) {
                                    getSharedPreferences("USER", MODE_PRIVATE)
                                            .edit()
                                            .putString("uid", uid)
                                            .apply();
                                }
                                SharedPreferences prefs = getSharedPreferences("USER", MODE_PRIVATE);
                                prefs.edit().putString("uid", uid).apply();
                                Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();

                                startActivity(new Intent(this, MainActivity.class));
                                finish();

                            } else {
                                Toast.makeText(this, "Sai mật khẩu", Toast.LENGTH_SHORT).show();
                            }

                        });

                    })
                    .addOnFailureListener(e -> {
                        Log.e("LOGIN_ERROR", "Email login error", e);
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        }

        // 🔍 LOGIN BẰNG SĐT
        else if (input.matches("^0\\d{9}$")) {

            dbRef.child("phones").child(input).get()
                    .addOnSuccessListener(snapshot -> {

                        if (!snapshot.exists()) {
                            Toast.makeText(this, "SĐT không tồn tại", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String uid = snapshot.getValue(String.class);

                        dbRef.child("users").child(uid).get()
                                .addOnSuccessListener(userSnap -> {

                                    String dbPassword = userSnap.child("password").getValue(String.class);

                                    if (dbPassword != null && dbPassword.equals(hashedPassword)) {

                                        dbRef.child("users").child(uid).child("status").setValue("online");

                                        if (cbLoginRemember.isChecked()) {
                                            getSharedPreferences("USER", MODE_PRIVATE)
                                                    .edit()
                                                    .putString("uid", uid)
                                                    .apply();
                                        }

                                        Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();

                                        startActivity(new Intent(this, MainActivity.class));
                                        finish();

                                    } else {
                                        Toast.makeText(this, "Sai mật khẩu", Toast.LENGTH_SHORT).show();
                                    }

                                });

                    })
                    .addOnFailureListener(e -> {
                        Log.e("LOGIN_ERROR", "Phone login error", e);
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        }

        // ❌ INVALID INPUT
        else {
            Toast.makeText(this, "Nhập Email hoặc SĐT hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }
}