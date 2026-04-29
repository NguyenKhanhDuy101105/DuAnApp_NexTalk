package com.example.nextalkapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class ManHinhDangNhap extends AppCompatActivity {

    private TextView tvLoginSignUp, tvLoginForgotPassword;
    private CheckBox cbLoginRemember;
    private MaterialButton btnLogin;
    private TextInputLayout tilLoginEmailOrPhone, tilLoginPassword;
    private TextInputEditText edtLoginEmailOrPhone, edtLoginPassword;
    private DatabaseReference dbRef;

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

        // Kiểm tra Auto login
        SharedPreferences pref = getSharedPreferences("USER", MODE_PRIVATE);
        if (pref.getString("uid", null) != null && pref.getBoolean("isRemembered", false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        setupListeners();
    }

    private void mapping() {
        tvLoginSignUp = findViewById(R.id.tvLoginSignUp);
        tvLoginForgotPassword = findViewById(R.id.tvLoginForgotPassword);

        // Mapping TextInputLayout để hiện lỗi
        tilLoginEmailOrPhone = findViewById(R.id.tilLoginEmailOrPhone);
        tilLoginPassword = findViewById(R.id.tilLoginPassword);

        edtLoginEmailOrPhone = findViewById(R.id.edtLoginEmailOrPhone);
        edtLoginPassword = findViewById(R.id.edtLoginPassword);
        cbLoginRemember = findViewById(R.id.cbLoginRemember);
        btnLogin = findViewById(R.id.btnLogin);
    }

    private void setupListeners() {
        tvLoginSignUp.setOnClickListener(v -> startActivity(new Intent(this, ManHinhDangKy.class)));

        tvLoginForgotPassword.setOnClickListener(v -> {
            String input = edtLoginEmailOrPhone.getText().toString().trim();
            Intent intent = new Intent(this, ManHinhGuiOTP.class);
            if (!TextUtils.isEmpty(input)) intent.putExtra("user_input", input);
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> handleLogin());

        // Thêm TextWatcher để tự xóa lỗi khi Duy gõ phím
        addTextWatcher(edtLoginEmailOrPhone, tilLoginEmailOrPhone);
        addTextWatcher(edtLoginPassword, tilLoginPassword);
    }

    private void handleLogin() {
        // Reset lỗi
        tilLoginEmailOrPhone.setError(null);
        tilLoginPassword.setError(null);

        String input = edtLoginEmailOrPhone.getText().toString().trim();
        String password = edtLoginPassword.getText().toString().trim();

        // 1. Validation rỗng
        if (TextUtils.isEmpty(input)) {
            tilLoginEmailOrPhone.setError("Vui lòng nhập Email hoặc SĐT");
            edtLoginEmailOrPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            tilLoginPassword.setError("Vui lòng nhập mật khẩu");
            edtLoginPassword.requestFocus();
            return;
        }

        String hashedPassword = hashPassword(password);
        SharedPreferences prefs = getSharedPreferences("USER", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // TRƯỜNG HỢP 1: LOGIN EMAIL
        if (Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
            dbRef.child("users").orderByChild("email").equalTo(input).get()
                    .addOnSuccessListener(snapshot -> {
                        if (!snapshot.exists()) {
                            tilLoginEmailOrPhone.setError("Email này chưa được đăng ký");
                            edtLoginEmailOrPhone.requestFocus();
                        } else {
                            snapshot.getChildren().forEach(userSnap -> {
                                String dbPass = userSnap.child("password").getValue(String.class);
                                if (dbPass != null && dbPass.equals(hashedPassword)) {
                                    processSuccessfulLogin(userSnap.getKey(), editor);
                                } else {
                                    tilLoginPassword.setError("Mật khẩu không chính xác");
                                    edtLoginPassword.requestFocus();
                                }
                            });
                        }
                    });
        }
        // TRƯỜNG HỢP 2: LOGIN SĐT
        else if (input.matches("^0\\d{9}$")) {
            dbRef.child("phones").child(input).get().addOnSuccessListener(snapshot -> {
                if (!snapshot.exists()) {
                    tilLoginEmailOrPhone.setError("Số điện thoại chưa được đăng ký");
                    edtLoginEmailOrPhone.requestFocus();
                } else {
                    String uid = snapshot.getValue(String.class);
                    dbRef.child("users").child(uid).get().addOnSuccessListener(userSnap -> {
                        String dbPass = userSnap.child("password").getValue(String.class);
                        if (dbPass != null && dbPass.equals(hashedPassword)) {
                            processSuccessfulLogin(uid, editor);
                        } else {
                            tilLoginPassword.setError("Mật khẩu không chính xác");
                            edtLoginPassword.requestFocus();
                        }
                    });
                }
            });
        }
        else {
            tilLoginEmailOrPhone.setError("Định dạng Email hoặc SĐT không hợp lệ");
            edtLoginEmailOrPhone.requestFocus();
        }
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

    private void processSuccessfulLogin(String uid, SharedPreferences.Editor editor) {
        dbRef.child("users").child(uid).child("status").setValue("online");
        editor.putString("uid", uid);
        editor.putBoolean("isRemembered", cbLoginRemember.isChecked());
        editor.apply();

        showMotionToast("Thành công", "Chào bạn đến với NexTalk!", MotionToastStyle.SUCCESS);
        btnLogin.postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }, 1200);
    }

    private void showMotionToast(String title, String message, MotionToastStyle style) {
        MotionToast.Companion.createColorToast(this, title, message, style,
                MotionToast.GRAVITY_BOTTOM, MotionToast.LONG_DURATION, Typeface.SANS_SERIF);
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
}