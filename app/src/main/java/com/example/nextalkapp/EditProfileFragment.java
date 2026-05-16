package com.example.nextalkapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.app.ProgressDialog;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.nextalkapp.Model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import www.sanju.motiontoast.MotionToast;
import www.sanju.motiontoast.MotionToastStyle;

public class EditProfileFragment extends Fragment {

    private ImageButton btnBack;
    private ImageView imgAvatarProfile;
    private FloatingActionButton btnChangeAvatar;
    private MaterialButton btnUpdateProfile, btnCancel;
    private TextInputEditText edtFullName, edtPhone, edtBio;

    private DatabaseReference dbRef;
    private String currentUserId;
    private User currentUser;
    private boolean isEditing = false;
    private Uri selectedImageUri;
    private ProgressDialog progressDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        initCloudinaryOnce();

        if (getContext() != null) {
            SharedPreferences pref = getContext().getSharedPreferences("USER", Context.MODE_PRIVATE);
            currentUserId = pref.getString("uid", null);
        }

        mapping(view);

        if (currentUserId != null) {
            dbRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId);
            loadUserData();
        }

        setupEvents();
        return view;
    }

    private void initCloudinaryOnce() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", "dak681rft");
            config.put("api_key", "251122464815674");
            config.put("api_secret", "xbzo_uLnnX-p5eTuwqoDaDhkA3I");
            MediaManager.init(requireContext(), config);
        } catch (Exception e) {
            Log.d("Cloudinary", "Đã khởi tạo trước đó");
        }
    }

    private void mapping(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        imgAvatarProfile = view.findViewById(R.id.imgAvatarProfile);
        btnChangeAvatar = view.findViewById(R.id.btnChangeAvatar);
        btnUpdateProfile = view.findViewById(R.id.btnUpdateProfile);
        btnCancel = view.findViewById(R.id.btnCancel);
        edtFullName = view.findViewById(R.id.edtFullName);
        edtPhone = view.findViewById(R.id.edtPhone);
        edtBio = view.findViewById(R.id.edtBio);

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Đang lưu...");
        progressDialog.setCancelable(false);

        setFieldsEnabled(false);
    }

    private void setupEvents() {
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnUpdateProfile.setOnClickListener(v -> {
            if (!isEditing) setFieldsEnabled(true);
            else validateAndSave();
        });
        if (btnCancel != null) btnCancel.setOnClickListener(v -> cancelEditing());
        btnChangeAvatar.setOnClickListener(v -> openGallery());
    }

    private void setFieldsEnabled(boolean enabled) {
        isEditing = enabled;
        edtFullName.setEnabled(enabled);
        edtPhone.setEnabled(enabled);
        edtBio.setEnabled(enabled);
        btnChangeAvatar.setVisibility(enabled ? View.VISIBLE : View.GONE);
        if (btnCancel != null) btnCancel.setVisibility(enabled ? View.VISIBLE : View.GONE);
        btnUpdateProfile.setText(enabled ? "Lưu" : "Chỉnh sửa thông tin");
    }

    private void loadUserData() {
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUser = snapshot.getValue(User.class);
                if (currentUser != null) displayUserData();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void displayUserData() {
        if (currentUser == null || !isAdded()) return;
        edtFullName.setText(currentUser.getName() != null ? currentUser.getName() : "");
        edtPhone.setText(currentUser.getPhone() != null ? currentUser.getPhone() : "");
        edtBio.setText(currentUser.getBio() != null ? currentUser.getBio() : "");

        String avatarUrl = currentUser.getAvatar();
        if (getContext() != null && avatarUrl != null && !avatarUrl.isEmpty()) {
            String optimizedUrl = avatarUrl;
            if (avatarUrl.contains("cloudinary.com")) {
                // Tối ưu: rộng 300px, cao 300px, tự động nhận diện khuôn mặt
                optimizedUrl = avatarUrl.replace("/upload/", "/upload/w_300,h_300,c_fill,g_face/");
            }

            Glide.with(this)
                    .load(optimizedUrl)
                    .placeholder(R.drawable.logo2)
                    .circleCrop()
                    .into(imgAvatarProfile);
        }
    }

    private void validateAndSave() {
        String name = edtFullName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String bio = edtBio.getText().toString().trim();

        if (name.isEmpty()) {
            showMotionToast("Lỗi", "Họ tên không được để trống", MotionToastStyle.ERROR);
            return;
        }

        if (!phone.matches("^0[0-9]{9}$")) {
            showMotionToast("Lỗi", "Số điện thoại không hợp lệ", MotionToastStyle.ERROR);
            return;
        }

        progressDialog.show();
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        Query query = usersRef.orderByChild("phone").equalTo(phone);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isTaken = false;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (!ds.getKey().equals(currentUserId)) {
                        isTaken = true;
                        break;
                    }
                }

                if (isTaken) {
                    progressDialog.dismiss();
                    showMotionToast("Trùng lặp", "Số điện thoại này đã được sử dụng!", MotionToastStyle.WARNING);
                } else {
                    saveUserData(name, phone, bio);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { progressDialog.dismiss(); }
        });
    }

    private void saveUserData(String name, String phone, String bio) {
        if (selectedImageUri != null) {
            progressDialog.setMessage("Đang tải ảnh lên...");

            MediaManager.get().upload(selectedImageUri)
                    .option("folder", "avatars/")
                    .option("public_id", currentUserId)
                    .callback(new UploadCallback() {
                        @Override public void onStart(String requestId) {}
                        @Override public void onProgress(String requestId, long bytes, long totalBytes) {}

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String avatarUrl = (String) resultData.get("secure_url");
                            updateDatabase(name, phone, bio, avatarUrl);
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            progressDialog.dismiss();
                            showMotionToast("Lỗi tải ảnh", error.getDescription(), MotionToastStyle.ERROR);
                        }

                        @Override public void onReschedule(String requestId, ErrorInfo error) {}
                    }).dispatch();
        } else {
            updateDatabase(name, phone, bio, currentUser != null ? currentUser.getAvatar() : "");
        }
    }

    private void updateDatabase(String name, String phone, String bio, String avatarUrl) {
        String oldPhone = currentUser != null ? currentUser.getPhone() : null;
        Map<String, Object> childUpdates = new HashMap<>();

        childUpdates.put("/users/" + currentUserId + "/name", name);
        childUpdates.put("/users/" + currentUserId + "/phone", phone);
        childUpdates.put("/users/" + currentUserId + "/bio", bio);
        childUpdates.put("/users/" + currentUserId + "/avatar", avatarUrl);

        if (oldPhone != null && !oldPhone.equals(phone)) {
            childUpdates.put("/phones/" + oldPhone, null);
            childUpdates.put("/phones/" + phone, currentUserId);
        } else if (oldPhone == null) {
            childUpdates.put("/phones/" + phone, currentUserId);
        }

        FirebaseDatabase.getInstance().getReference().updateChildren(childUpdates)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        if (getContext() != null) {
                            SharedPreferences.Editor editor = getContext().getSharedPreferences("USER", Context.MODE_PRIVATE).edit();
                            editor.putString("name", name);
                            editor.apply();
                        }
                        showMotionToast("Thành công", "Cập nhật hồ sơ hoàn tất", MotionToastStyle.SUCCESS);
                        setFieldsEnabled(false);
                        loadUserData();
                    } else {
                        showMotionToast("Thất bại", "Lỗi đồng bộ dữ liệu", MotionToastStyle.ERROR);
                    }
                });
    }

    private void showMotionToast(String title, String message, MotionToastStyle style) {
        if (getActivity() == null) return;
        MotionToast.Companion.createColorToast(getActivity(),
                title, message, style, MotionToast.GRAVITY_BOTTOM,
                MotionToast.LONG_DURATION,
                ResourcesCompat.getFont(requireContext(), www.sanju.motiontoast.R.font.helvetica_regular));
    }

    private void cancelEditing() {
        setFieldsEnabled(false);
        displayUserData();
        selectedImageUri = null;
    }

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    imgAvatarProfile.setImageURI(selectedImageUri);
                }
            }
    );

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Khi màn hình này hiển thị lên: KHÓA VUỐT ViewPager2
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setSwipeEnabled(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Khi thoát khỏi màn hình này (Back hoặc ấn sang tab khác): MỞ LẠI VUỐT
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setSwipeEnabled(true);
        }
    }
}