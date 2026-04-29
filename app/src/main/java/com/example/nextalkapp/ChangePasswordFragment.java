package com.example.nextalkapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class ChangePasswordFragment extends Fragment {

    private ImageButton btnBack;
    private TextInputLayout tilCurrent, tilNew, tilConfirm;
    private TextInputEditText edtCurrentPassword, edtNewPassword, edtConfirmNewPassword;
    private MaterialButton btnConfirmChange, btnCancel;
    private DatabaseReference dbRef;
    private String currentUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_password, container, false);

        mapping(view);
        dbRef = FirebaseDatabase.getInstance().getReference();

        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences("USER", Context.MODE_PRIVATE);
            currentUid = prefs.getString("uid", null);
        }

        setupListeners();
        return view;
    }

    private void mapping(View view) {
        btnBack = view.findViewById(R.id.btnBack);

        // Đã đồng bộ ID chuẩn xác với file XML ở trên
        tilCurrent = view.findViewById(R.id.tilCurrent);
        tilNew = view.findViewById(R.id.tilNewPassword);
        tilConfirm = view.findViewById(R.id.tilConfirmNewPassword);

        edtCurrentPassword = view.findViewById(R.id.edtCurrentPassword);
        edtNewPassword = view.findViewById(R.id.edtNewPassword);
        edtConfirmNewPassword = view.findViewById(R.id.edtConfirmNewPassword);

        btnConfirmChange = view.findViewById(R.id.btnConfirmChange);
        btnCancel = view.findViewById(R.id.btnCancel);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnCancel.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnConfirmChange.setOnClickListener(v -> handleChangePassword());

        // Tự động xóa thông báo lỗi khi người dùng gõ phím
        addTextWatcher(edtCurrentPassword, tilCurrent);
        addTextWatcher(edtNewPassword, tilNew);
        addTextWatcher(edtConfirmNewPassword, tilConfirm);
    }

    private void handleChangePassword() {
        tilCurrent.setError(null);
        tilNew.setError(null);
        tilConfirm.setError(null);

        String currentPass = edtCurrentPassword.getText().toString().trim();
        String newPass = edtNewPassword.getText().toString().trim();
        String confirmPass = edtConfirmNewPassword.getText().toString().trim();

        // 1. Validation cơ bản
        if (TextUtils.isEmpty(currentPass)) {
            tilCurrent.setError("Vui lòng nhập mật khẩu hiện tại");
            edtCurrentPassword.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(newPass)) {
            tilNew.setError("Vui lòng nhập mật khẩu mới");
            edtNewPassword.requestFocus();
            return;
        }
        if (!newPass.matches("^(?=.*[!@#$%^&*]).{8,}$")) {
            tilNew.setError("Mật khẩu phải >= 8 ký tự và có ký tự đặc biệt");
            edtNewPassword.requestFocus();
            showMotionToast("Bảo mật yếu", "Mật khẩu chưa đủ mạnh", MotionToastStyle.ERROR);
            return;
        }
        if (!newPass.equals(confirmPass)) {
            tilConfirm.setError("Mật khẩu xác nhận không khớp");
            edtConfirmNewPassword.requestFocus();
            return;
        }

        if (currentUid == null) return;

        // 2. Xử lý Database
        String hashedCurrent = hashPassword(currentPass);
        dbRef.child("users").child(currentUid).child("password").get().addOnSuccessListener(snapshot -> {
            String dbPassword = snapshot.getValue(String.class);
            if (dbPassword != null && dbPassword.equals(hashedCurrent)) {

                String hashedNew = hashPassword(newPass);
                dbRef.child("users").child(currentUid).child("password").setValue(hashedNew)
                        .addOnSuccessListener(unused -> {
                            showMotionToast("Thành công", "Đã cập nhật mật khẩu mới", MotionToastStyle.SUCCESS);
                            getParentFragmentManager().popBackStack();
                        })
                        .addOnFailureListener(e -> showMotionToast("Lỗi", "Không thể kết nối CSDL", MotionToastStyle.ERROR));
            } else {
                tilCurrent.setError("Mật khẩu hiện tại không đúng");
                edtCurrentPassword.requestFocus();
                showMotionToast("Thất bại", "Mật khẩu cũ không chính xác", MotionToastStyle.ERROR);
            }
        }).addOnFailureListener(e -> showMotionToast("Lỗi hệ thống", "Vui lòng thử lại sau", MotionToastStyle.ERROR));
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

    private void showMotionToast(String title, String message, MotionToastStyle style) {
        if (getActivity() != null) {
            MotionToast.Companion.createColorToast(getActivity(), title, message, style,
                    MotionToast.GRAVITY_BOTTOM, MotionToast.LONG_DURATION,
                    ResourcesCompat.getFont(getContext(), www.sanju.motiontoast.R.font.helvetica_regular));
        }
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
}