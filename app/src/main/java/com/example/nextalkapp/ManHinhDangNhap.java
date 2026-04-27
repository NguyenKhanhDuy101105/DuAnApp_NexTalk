package com.example.nextalkapp;

import android.graphics.Typeface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.CheckBox;
import android.widget.TextView;

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

import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

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

        SharedPreferences pref = getSharedPreferences("USER", MODE_PRIVATE);
        String savedUid = pref.getString("uid", null);
        if (savedUid != null && pref.getBoolean("isRemembered", false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        tvLoginSignUp.setOnClickListener(v -> startActivity(new Intent(this, ManHinhDangKy.class)));
        tvLoginForgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ManHinhGuiOTP.class)));

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

    private void handleLogin() {
        String input = edtLoginEmailOrPhone.getText().toString().trim();
        String password = edtLoginPassword.getText().toString().trim();

        // --- VALIDATION CHI TIẾT ---

        // 1. Kiểm tra Email/SĐT trống
        if (TextUtils.isEmpty(input)) {
            edtLoginEmailOrPhone.setError("Vui lòng nhập Email hoặc Số điện thoại");
            edtLoginEmailOrPhone.requestFocus();
            return;
        }

        // 2. Kiểm tra mật khẩu trống
        if (TextUtils.isEmpty(password)) {
            edtLoginPassword.setError("Vui lòng nhập mật khẩu");
            edtLoginPassword.requestFocus();
            return;
        }

        // 3. Kiểm tra độ dài mật khẩu
        if (password.length() < 8) {
            edtLoginPassword.setError("Mật khẩu phải có ít nhất 8 ký tự");
            edtLoginPassword.requestFocus();
            return;
        }

        String hashedPassword = hashPassword(password);
        SharedPreferences prefs = getSharedPreferences("USER", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Kiểm tra định dạng Email
        if (Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
            loginWithEmail(input, hashedPassword, editor);
        }
        // Kiểm tra định dạng SĐT Việt Nam (0xxxxxxxxx)
        else if (input.matches("^0\\d{9}$")) {
            loginWithPhone(input, hashedPassword, editor);
        }
        // Sai định dạng cả hai
        else {
            edtLoginEmailOrPhone.setError("Email hoặc Số điện thoại không hợp lệ");
            edtLoginEmailOrPhone.requestFocus();
        }
    }

    private void loginWithEmail(String email, String hashedPassword, SharedPreferences.Editor editor) {
        dbRef.child("users").orderByChild("email").equalTo(email).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        edtLoginEmailOrPhone.setError("Email này chưa được đăng ký");
                        edtLoginEmailOrPhone.requestFocus();
                        return;
                    }

                    snapshot.getChildren().forEach(userSnap -> {
                        String dbPassword = userSnap.child("password").getValue(String.class);
                        if (dbPassword != null && dbPassword.equals(hashedPassword)) {
                            completeLogin(userSnap.getKey(), editor);
                        } else {
                            edtLoginPassword.setError("Mật khẩu không chính xác");
                            edtLoginPassword.requestFocus();
                        }
                    });
                })
                .addOnFailureListener(e -> showMotionToast("Lỗi", e.getMessage(), MotionToastStyle.ERROR));
    }

    private void loginWithPhone(String phone, String hashedPassword, SharedPreferences.Editor editor) {
        dbRef.child("phones").child(phone).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        edtLoginEmailOrPhone.setError("Số điện thoại chưa được đăng ký");
                        edtLoginEmailOrPhone.requestFocus();
                        return;
                    }

                    String uid = snapshot.getValue(String.class);
                    dbRef.child("users").child(uid).get()
                            .addOnSuccessListener(userSnap -> {
                                String dbPassword = userSnap.child("password").getValue(String.class);
                                if (dbPassword != null && dbPassword.equals(hashedPassword)) {
                                    completeLogin(uid, editor);
                                } else {
                                    edtLoginPassword.setError("Mật khẩu không chính xác");
                                    edtLoginPassword.requestFocus();
                                }
                            });
                })
                .addOnFailureListener(e -> showMotionToast("Lỗi", e.getMessage(), MotionToastStyle.ERROR));
    }

    private void completeLogin(String uid, SharedPreferences.Editor editor) {
        dbRef.child("users").child(uid).child("status").setValue("online");
        editor.putString("uid", uid);
        editor.putBoolean("isRemembered", cbLoginRemember.isChecked());
        editor.apply();

        showMotionToast("Thành công", "Chào mừng Duy quay trở lại!", MotionToastStyle.SUCCESS);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}