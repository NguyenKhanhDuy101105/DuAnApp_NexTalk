package com.example.nextalkapp;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class ManHinhDangKy extends AppCompatActivity {

    private TextView tvLogin;
    private TextInputLayout tilFullName, tilEmail, tilPhone, tilPassword, tilConfirmPassword;
    private TextInputEditText edtFullName, edtEmail, edtPhone, edtPassword, edtConfirmPassword;
    private MaterialButton btnSignUp;
    private DatabaseReference dbRef;

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

        tvLogin.setOnClickListener(v -> startActivity(new Intent(this, ManHinhDangNhap.class)));
        btnSignUp.setOnClickListener(v -> handleSignUp());

        setupTextWatchers();
    }

    private void mapping() {
        tvLogin = findViewById(R.id.tvLogin);
        btnSignUp = findViewById(R.id.btnSignUp);

        // Mapping Layout để hiện lỗi chuẩn Duy thích
        tilFullName = findViewById(R.id.tilFullName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPhone = findViewById(R.id.tilPhone);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        edtFullName = findViewById(R.id.edtFullName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
    }

    private void setupTextWatchers() {
        addTextWatcher(edtFullName, tilFullName);
        addTextWatcher(edtEmail, tilEmail);
        addTextWatcher(edtPhone, tilPhone);
        addTextWatcher(edtPassword, tilPassword);
        addTextWatcher(edtConfirmPassword, tilConfirmPassword);
    }

    private void addTextWatcher(TextInputEditText editText, TextInputLayout inputLayout) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Khi người dùng gõ, xóa lỗi và tắt luôn vùng hiển thị lỗi để thu hẹp khoảng cách
                if (inputLayout.isErrorEnabled()) {
                    inputLayout.setError(null);
                    inputLayout.setErrorEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void handleSignUp() {
        // Reset toàn bộ lỗi
        tilFullName.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        String name = edtFullName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirm = edtConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            tilFullName.setError("Vui lòng nhập họ tên");
            edtFullName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Vui lòng nhập Email");
            edtEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Định dạng Email không hợp lệ");
            edtEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            tilPhone.setError("Vui lòng nhập số điện thoại");
            edtPhone.requestFocus();
            return;
        }
        if (!phone.matches("^0\\d{9}$")) {
            tilPhone.setError("SĐT phải gồm 10 chữ số và bắt đầu bằng số 0");
            edtPhone.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            edtPassword.requestFocus();
            return;
        }
        if (!password.matches("^(?=.*[!@#$%^&*]).{8,}$")) {
            tilPassword.setError("Mật khẩu cần ít nhất 8 ký tự và 1 ký tự đặc biệt");
            edtPassword.requestFocus();
            return;
        }
        if (!password.equals(confirm)) {
            tilConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            edtConfirmPassword.requestFocus();
            return;
        }

        checkAvailability(name, email, phone, password);
    }

    private void checkAvailability(String name, String email, String phone, String password) {
        dbRef.child("phones").child(phone).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                tilPhone.setError("Số điện thoại này đã được đăng ký");
                edtPhone.requestFocus();
                return;
            }

            dbRef.child("users").orderByChild("email").equalTo(email).get().addOnSuccessListener(emailSnap -> {
                if (emailSnap.exists()) {
                    tilEmail.setError("Email này đã được sử dụng");
                    edtEmail.requestFocus();
                } else {
                    createUser(name, email, phone, password);
                }
            });
        });
    }

    private void createUser(String name, String email, String phone, String password) {
        String uid = dbRef.child("users").push().getKey();
        if (uid == null) return;

        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("uid", uid);
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("phone", phone);
        userMap.put("password", hashPassword(password));
        userMap.put("bio", "Chào mừng bạn đến với NexTalk!");
        userMap.put("avatar", "");
        userMap.put("status", "offline");
        userMap.put("createdAt", System.currentTimeMillis());

        dbRef.child("users").child(uid).setValue(userMap).addOnSuccessListener(unused -> {
            dbRef.child("phones").child(phone).setValue(uid);
            showMotionToast("Thành công", "Chào mừng bạn tham gia NexTalk!", MotionToastStyle.SUCCESS);
            btnSignUp.postDelayed(() -> {
                startActivity(new Intent(ManHinhDangKy.this, ManHinhDangNhap.class));
                finish();
            }, 1500);
        });
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) { return password; }
    }

    private void showMotionToast(String title, String message, MotionToastStyle style) {
        MotionToast.Companion.createColorToast(this, title, message, style,
                MotionToast.GRAVITY_BOTTOM, MotionToast.LONG_DURATION, Typeface.SANS_SERIF);
    }
}