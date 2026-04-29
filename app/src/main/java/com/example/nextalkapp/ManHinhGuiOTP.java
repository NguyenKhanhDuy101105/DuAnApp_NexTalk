package com.example.nextalkapp;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageButton; // 🔥 Thêm import
import android.widget.Toast;

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

import java.util.Random;

public class ManHinhGuiOTP extends AppCompatActivity {

    private TextInputLayout tilEmail;
    private TextInputEditText edtEmail;
    private MaterialButton btnSendOTP;
    private ImageButton btnBackChat; // 🔥 Khai báo nút Back
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_man_hinh_gui_otp);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Mapping
        tilEmail = findViewById(R.id.tilEmail);
        edtEmail = findViewById(R.id.edtEmail);
        btnSendOTP = findViewById(R.id.btnSendOTP);
        btnBackChat = findViewById(R.id.btnBackChat); // 🔥 Mapping nút Back
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // 🔥 Sự kiện nút Back quay về màn hình Đăng nhập
        btnBackChat.setOnClickListener(v -> finish());

        String receivedData = getIntent().getStringExtra("user_input");
        if (receivedData != null && !receivedData.isEmpty() && receivedData.contains("@")) {
            edtEmail.setText(receivedData);
            updateButtonState(true);
        } else {
            updateButtonState(false);
        }

        edtEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateButtonState(!s.toString().trim().isEmpty());
                tilEmail.setError(null);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSendOTP.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            if (email.isEmpty()) return;
            if (!email.toLowerCase().endsWith("@gmail.com") || email.length() <= 10) {
                tilEmail.setError("Email không hợp lệ (phải có @gmail.com)");
                return;
            }
            checkUserOnFirebase(email);
        });
    }

    private void checkUserOnFirebase(String email) {
        btnSendOTP.setEnabled(false);
        btnSendOTP.setText("Đang kiểm tra...");

        mDatabase.child("users").orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        btnSendOTP.setEnabled(true);
                        btnSendOTP.setText("Gửi mã xác nhận");

                        if (dataSnapshot.exists()) {
                            String randomOtp = String.valueOf(new Random().nextInt(9000) + 1000);

                            String subject = "NexTalk - Mã xác thực của bạn";
                            String messageContent = "Chào bạn, mã OTP để đặt lại mật khẩu của bạn là: " + randomOtp;

                            JavaMailAPI javaMailAPI = new JavaMailAPI(email, subject, messageContent);
                            javaMailAPI.execute();

                            Toast.makeText(ManHinhGuiOTP.this, "Mã OTP đã được gửi về email của bạn", Toast.LENGTH_LONG).show();

                            Intent intent = new Intent(ManHinhGuiOTP.this, ManHinhNhapOTP.class);
                            intent.putExtra("otp_destination", email);
                            intent.putExtra("otp_code", randomOtp);
                            startActivity(intent);
                        } else {
                            tilEmail.setError("Email này chưa được đăng ký tài khoản");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        btnSendOTP.setEnabled(true);
                        btnSendOTP.setText("Gửi mã xác nhận");
                        Toast.makeText(ManHinhGuiOTP.this, "Lỗi: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateButtonState(boolean isEnabled) {
        btnSendOTP.setEnabled(isEnabled);
        btnSendOTP.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(isEnabled ? "#5C8EE6" : "#CCCCCC")));
    }
}