package com.example.nextalkapp;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class ManHinhDoiMatKhau extends AppCompatActivity {

    private TextInputLayout tilPassword, tilConfirmPassword;
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

        userEmail = getIntent().getStringExtra("user_email");

        mapping();
        dbRef = FirebaseDatabase.getInstance().getReference();

        btnResetPassword.setOnClickListener(v -> handleResetPassword());

        setupTextWatchers();
    }

    private void mapping() {
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);
    }

    private void setupTextWatchers() {
        addTextWatcher(edtPassword, tilPassword);
        addTextWatcher(edtConfirmPassword, tilConfirmPassword);
    }

    private void addTextWatcher(TextInputEditText editText, TextInputLayout inputLayout) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (inputLayout.isErrorEnabled()) {
                    inputLayout.setError(null);
                    inputLayout.setErrorEnabled(false);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void handleResetPassword() {
        String password = edtPassword.getText().toString().trim();
        String confirm = edtConfirmPassword.getText().toString().trim();

        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Vui lòng nhập mật khẩu mới");
            edtPassword.requestFocus();
            return;
        }

        if (!password.matches("^(?=.*[!@#$%^&*]).{8,}$")) {
            tilPassword.setError("Mật khẩu tối thiểu 8 ký tự và 1 ký tự đặc biệt");
            edtPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirm)) {
            tilConfirmPassword.setError("Vui lòng xác nhận lại mật khẩu");
            edtConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirm)) {
            tilConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            edtConfirmPassword.requestFocus();
            return;
        }

        resetPasswordOnFirebase(password);
    }

    private void resetPasswordOnFirebase(String newPassword) {
        btnResetPassword.setEnabled(false);
        btnResetPassword.setText("Đang cập nhật...");

        String hashedNewPassword = hashPassword(newPassword);

        dbRef.child("users").orderByChild("email").equalTo(userEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                userSnapshot.getRef().child("password").setValue(hashedNewPassword)
                                        .addOnSuccessListener(unused -> {
                                            showMotionToast("Thành công", "Mật khẩu đã được thay đổi!", MotionToastStyle.SUCCESS);
                                            btnResetPassword.postDelayed(() -> {
                                                Intent intent = new Intent(ManHinhDoiMatKhau.this, ManHinhDangNhap.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                                finish();
                                            }, 1500);
                                        })
                                        .addOnFailureListener(e -> {
                                            btnResetPassword.setEnabled(true);
                                            btnResetPassword.setText("Cập nhật mật khẩu");
                                            showMotionToast("Lỗi", e.getMessage(), MotionToastStyle.ERROR);
                                        });
                            }
                        } else {
                            btnResetPassword.setEnabled(true);
                            btnResetPassword.setText("Cập nhật mật khẩu");
                            showMotionToast("Lỗi", "Không tìm thấy người dùng", MotionToastStyle.ERROR);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        btnResetPassword.setEnabled(true);
                        btnResetPassword.setText("Cập nhật mật khẩu");
                    }
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