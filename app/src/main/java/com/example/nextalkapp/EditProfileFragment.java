package com.example.nextalkapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class EditProfileFragment extends Fragment {

    private ImageButton btnBack;
    private MaterialButton btnUpdateProfile;
    private TextInputEditText edtFullName, edtPhone, edtBio;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Sử dụng file XML bạn đã có sẵn
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        mapping(view);

        // Nút quay lại: Sử dụng popBackStack để quay về Fragment trước đó (SettingFragment)
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        // Nút cập nhật
        btnUpdateProfile.setOnClickListener(v -> {
            String name = edtFullName.getText().toString();
            if (name.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập tên", Toast.LENGTH_SHORT).show();
            } else {
                // Xử lý Firebase tại đây
                Toast.makeText(getContext(), "Đã cập nhật hồ sơ!", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void mapping(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        btnUpdateProfile = view.findViewById(R.id.btnUpdateProfile);
        edtFullName = view.findViewById(R.id.edtFullName);
        edtPhone = view.findViewById(R.id.edtPhone);
        edtBio = view.findViewById(R.id.edtBio);
    }
}