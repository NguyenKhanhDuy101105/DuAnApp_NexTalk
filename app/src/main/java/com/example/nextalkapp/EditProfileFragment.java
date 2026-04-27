package com.example.nextalkapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.nextalkapp.Model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

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

    private final String BUCKET_URL = "gs://nexttalk-ca7be.firebasestorage.app";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

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
            else saveUserData();
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
        dbRef.addValueEventListener(new ValueEventListener() {
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

        if (getContext() != null && currentUser.getAvatar() != null && !currentUser.getAvatar().isEmpty()) {
            if (currentUser.getAvatar().startsWith("data:image")) {
                try {
                    byte[] decodedString = Base64.decode(currentUser.getAvatar().split(",")[1], Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    imgAvatarProfile.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    Glide.with(this).load(currentUser.getAvatar()).placeholder(R.drawable.logo2).into(imgAvatarProfile);
                }
            } else {
                Glide.with(this).load(currentUser.getAvatar()).placeholder(R.drawable.logo2).into(imgAvatarProfile);
            }
        }
    }

    private void cancelEditing() {
        setFieldsEnabled(false);
        displayUserData();
        selectedImageUri = null;
    }

    private void saveUserData() {
        if (currentUserId == null) return;
        String name = edtFullName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String bio = edtBio.getText().toString().trim();

        if (name.isEmpty()) { Toast.makeText(getContext(), "Nhập họ tên", Toast.LENGTH_SHORT).show(); return; }

        progressDialog.show();

        if (selectedImageUri != null) {
            StorageReference ref = FirebaseStorage.getInstance(BUCKET_URL).getReference()
                    .child("avatars").child(currentUserId + ".jpg");

            ref.putFile(selectedImageUri).addOnSuccessListener(taskSnapshot -> {
                ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    updateDatabase(name, phone, bio, uri.toString());
                });
            }).addOnFailureListener(e -> {
                Log.e("Firebase", "Storage lỗi, dùng Base64 thay thế: " + e.getMessage());
                String base64Image = convertUriToBase64(selectedImageUri);
                updateDatabase(name, phone, bio, base64Image);
            });
        } else {
            updateDatabase(name, phone, bio, currentUser != null ? currentUser.getAvatar() : "");
        }
    }

    private String convertUriToBase64(Uri uri) {
        try {
            InputStream is = getContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Nén ảnh để không vượt quá giới hạn 10MB của Realtime Database node
            bitmap.compress(Bitmap.CompressFormat.JPEG, 30, baos); 
            byte[] b = baos.toByteArray();
            return "data:image/jpeg;base64," + Base64.encodeToString(b, Base64.DEFAULT);
        } catch (Exception e) { 
            Log.e("Base64", "Lỗi chuyển đổi: " + e.getMessage());
            return ""; 
        }
    }

    private void updateDatabase(String name, String phone, String bio, String avatarUrl) {
        dbRef.child("name").setValue(name);
        dbRef.child("phone").setValue(phone);
        dbRef.child("bio").setValue(bio);
        dbRef.child("avatar").setValue(avatarUrl).addOnCompleteListener(task -> {
            progressDialog.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                setFieldsEnabled(false);
                selectedImageUri = null;
            }
        });
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

    private boolean isValidImage(Uri uri) { return true; }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }
}
