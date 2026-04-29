package com.example.nextalkapp;

import android.content.Intent;
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

import java.util.Locale;
import java.util.Random;

import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class ManHinhNhapOTP extends AppCompatActivity {

    private TextView tvDescription, tvTimer, tvResendOtp;
    private EditText edtOtp1, edtOtp2, edtOtp3, edtOtp4;
    private MaterialButton btnVerify;
    private CountDownTimer countDownTimer;
    private CountDownTimer resendTimer;
    private String correctOtp;
    private String userEmail;
    private boolean isOtpExpired = false;
    private static final long START_TIME_IN_MILLIS = 180000; // 3 phút
    private static final long RESEND_TIME_IN_MILLIS = 30000; // 30 giây

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_man_hinh_nhap_otp);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mapping();

        // 1. Nhận dữ liệu
        userEmail = getIntent().getStringExtra("otp_destination");
        correctOtp = getIntent().getStringExtra("otp_code");

        if (userEmail != null) {
            tvDescription.setText("Quý khách vui lòng nhập mã OTP được gửi về email " + userEmail);
        }

        setupOtpInputs();
        startTimer();
        startResendTimer();

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

    private void handleResendOtp() {
        // Tạo mã mới
        correctOtp = String.valueOf(new Random().nextInt(9000) + 1000);
        isOtpExpired = false;

        // Gửi mail thật
        String subject = "NexTalk - Mã xác thực mới";
        String messageContent = "Chào bạn, mã OTP mới của bạn là: " + correctOtp;
        JavaMailAPI javaMailAPI = new JavaMailAPI(userEmail, subject, messageContent);
        javaMailAPI.execute();

        showMotionToast("Đã gửi lại", "Mã OTP mới đã được gửi đến Email của bạn", MotionToastStyle.SUCCESS);

        startTimer();
        startResendTimer();
    }

    private void handleVerifyOtp() {
        if (isOtpExpired) {
            showMotionToast("Mã hết hạn", "Vui lòng gửi lại mã mới để tiếp tục", MotionToastStyle.WARNING);
            return;
        }

        String inputOtp = edtOtp1.getText().toString() + edtOtp2.getText().toString() +
                edtOtp3.getText().toString() + edtOtp4.getText().toString();

        if (inputOtp.length() < 4) {
            showMotionToast("Thiếu thông tin", "Vui lòng nhập đủ 4 chữ số OTP", MotionToastStyle.INFO);
        } else if (inputOtp.equals(correctOtp)) {
            showMotionToast("Thành công", "Xác thực mã OTP chính xác", MotionToastStyle.SUCCESS);

            // Delay một chút để người dùng kịp thấy Toast thành công
            btnVerify.postDelayed(() -> {
                Intent intent = new Intent(ManHinhNhapOTP.this, ManHinhDoiMatKhau.class);
                intent.putExtra("user_email", userEmail);
                startActivity(intent);
                finish();
            }, 1000);
        } else {
            showMotionToast("Sai mã", "Mã OTP không chính xác, vui lòng kiểm tra lại", MotionToastStyle.ERROR);
        }
    }

    private void setupOtpInputs() {
        edtOtp1.addTextChangedListener(new OtpTextWatcher(edtOtp1, edtOtp2));
        edtOtp2.addTextChangedListener(new OtpTextWatcher(edtOtp2, edtOtp3));
        edtOtp3.addTextChangedListener(new OtpTextWatcher(edtOtp3, edtOtp4));
        edtOtp4.addTextChangedListener(new OtpTextWatcher(edtOtp4, null));
    }

    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        isOtpExpired = false;
        countDownTimer = new CountDownTimer(START_TIME_IN_MILLIS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                tvTimer.setText(String.format(Locale.getDefault(), "OTP hết hạn sau %02d:%02d", minutes, seconds));
            }
            @Override
            public void onFinish() {
                tvTimer.setText("OTP đã hết hạn");
                isOtpExpired = true;
            }
        }.start();
    }

    private void startResendTimer() {
        tvResendOtp.setEnabled(false);
        tvResendOtp.setTextColor(Color.GRAY);

        if (resendTimer != null) resendTimer.cancel();
        resendTimer = new CountDownTimer(RESEND_TIME_IN_MILLIS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                tvResendOtp.setText("Gửi lại OTP (" + seconds + "s)");
            }

            @Override
            public void onFinish() {
                tvResendOtp.setEnabled(true);
                tvResendOtp.setText("Gửi lại OTP");
                tvResendOtp.setTextColor(Color.parseColor("#5C8EE6"));
            }
        }.start();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
        if (resendTimer != null) resendTimer.cancel();
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