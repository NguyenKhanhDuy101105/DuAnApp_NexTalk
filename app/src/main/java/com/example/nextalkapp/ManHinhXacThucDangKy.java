package com.example.nextalkapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class ManHinhXacThucDangKy extends AppCompatActivity {

    private TextView tvDescription, tvTimer, tvResendOtp;
    private EditText edtOtp1, edtOtp2, edtOtp3, edtOtp4;
    private MaterialButton btnVerify;

    private String name, email, phone, password, correctOtp;
    private DatabaseReference dbRef;
    private CountDownTimer countDownTimer;
    private boolean isOtpExpired = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_man_hinh_xac_thuc_dang_ky);

        // Nhận data từ ManHinhDangKy
        Intent intent = getIntent();
        name = intent.getStringExtra("reg_name");
        email = intent.getStringExtra("reg_email");
        phone = intent.getStringExtra("reg_phone");
        password = intent.getStringExtra("reg_password");
        correctOtp = intent.getStringExtra("reg_otp");

        mapping();
        dbRef = FirebaseDatabase.getInstance().getReference();

        tvDescription.setText("Mã xác thực đã được gửi đến email: " + email);

        setupOtpInputs();
        startTimer();

        tvResendOtp.setOnClickListener(v -> handleResendOtp());
        btnVerify.setOnClickListener(v -> handleVerifyOtp());
    }

    private void mapping() {
        tvDescription = findViewById(R.id.tvDescription);
        tvTimer = findViewById(R.id.tvTimer);
        tvResendOtp = findViewById(R.id.tvResendOtp);
        edtOtp1 = findViewById(R.id.edtOtp1);
        edtOtp2 = findViewById(R.id.edtOtp2);
        edtOtp3 = findViewById(R.id.edtOtp3);
        edtOtp4 = findViewById(R.id.edtOtp4);
        btnVerify = findViewById(R.id.btnVerify);
    }

    private void handleVerifyOtp() {
        String inputOtp = edtOtp1.getText().toString() + edtOtp2.getText().toString() +
                edtOtp3.getText().toString() + edtOtp4.getText().toString();

        if (isOtpExpired) {
            showMotionToast("Hết hạn", "Mã OTP đã hết hiệu lực, vui lòng gửi lại", MotionToastStyle.WARNING);
            return;
        }

        if (inputOtp.equals(correctOtp)) {
            createUserOnFirebase();
        } else {
            showMotionToast("Lỗi", "Mã xác thực không chính xác", MotionToastStyle.ERROR);
        }
    }

    private void createUserOnFirebase() {
        btnVerify.setEnabled(false);
        btnVerify.setText("Đang khởi tạo tài khoản...");

        String uid = dbRef.child("users").push().getKey();
        if (uid == null) return;

        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("uid", uid);
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("phone", phone);
        userMap.put("password", password); // Password này đã được hash từ màn hình trước
        userMap.put("bio", "Chào mừng bạn đến với NexTalk!");
        userMap.put("avatar", "");
        userMap.put("status", "online"); // Đăng ký xong cho online luôn
        userMap.put("createdAt", System.currentTimeMillis());

        // 1. Lưu thông tin User
        dbRef.child("users").child(uid).setValue(userMap).addOnSuccessListener(unused -> {

            // 2. Lưu vào bảng phones để quản lý trùng lặp
            dbRef.child("phones").child(phone).setValue(uid);

            // 3. QUAN TRỌNG: Lưu trạng thái đăng nhập vào SharedPreferences
            SharedPreferences pref = getSharedPreferences("USER", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("uid", uid);
            editor.putString("name", name);
            editor.putString("email", email);
            editor.apply();

            showMotionToast("Thành công", "Đăng ký và đăng nhập hoàn tất!", MotionToastStyle.SUCCESS);

            // 4. Chuyển thẳng đến MainActivity
            Intent mainIntent = new Intent(ManHinhXacThucDangKy.this, MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Xóa lịch sử các màn hình trước
            startActivity(mainIntent);
            finish();
        });
    }

    private void handleResendOtp() {
        correctOtp = String.valueOf(new Random().nextInt(9000) + 1000);
        new JavaMailAPI(email, "NexTalk - Gửi lại mã xác thực", "Mã mới của bạn là: " + correctOtp).execute();
        showMotionToast("Đã gửi", "Vui lòng kiểm tra lại email", MotionToastStyle.SUCCESS);
        startTimer();
    }

    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        countDownTimer = new CountDownTimer(180000, 1000) {
            @Override
            public void onTick(long millis) {
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", (millis/1000)/60, (millis/1000)%60));
            }
            @Override
            public void onFinish() {
                isOtpExpired = true;
                tvTimer.setText("00:00");
            }
        }.start();
    }

    private void setupOtpInputs() {
        edtOtp1.addTextChangedListener(new OtpTextWatcher(edtOtp1, edtOtp2));
        edtOtp2.addTextChangedListener(new OtpTextWatcher(edtOtp2, edtOtp3));
        edtOtp3.addTextChangedListener(new OtpTextWatcher(edtOtp3, edtOtp4));
        edtOtp4.addTextChangedListener(new OtpTextWatcher(edtOtp4, null));
    }

    private void showMotionToast(String title, String message, MotionToastStyle style) {
        MotionToast.Companion.createColorToast(this, title, message, style,
                MotionToast.GRAVITY_BOTTOM, MotionToast.LONG_DURATION, Typeface.SANS_SERIF);
    }

    private class OtpTextWatcher implements TextWatcher {
        private EditText current, next;
        public OtpTextWatcher(EditText c, EditText n) { this.current = c; this.next = n; }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() == 1 && next != null) next.requestFocus();
        }
    }
}