package com.example.app_store;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private EditText edtProfileName, edtProfilePhone, edtProfileAddress;
    private Button btnSaveProfile;
    private ImageView btnBackProfile, imgAvatar;
    private Uri imageUri;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        // Ánh xạ
        edtProfileName = findViewById(R.id.edtProfileName);
        edtProfilePhone = findViewById(R.id.edtProfilePhone);
        edtProfileAddress = findViewById(R.id.edtProfileAddress);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnBackProfile = findViewById(R.id.btnBackProfile);
        imgAvatar = findViewById(R.id.imgAvatar);

        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
            loadUserProfile(); // Tải dữ liệu cũ khi vừa mở trang
        } else {
            Toast.makeText(this, "Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 1. Tải avatar cũ (nếu có) khi vừa mở trang
        loadUserAvatar();

        // 2. Bắt sự kiện khi khách hàng bấm vào Avatar
        imgAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*"); // Chỉ cho phép chọn ảnh
            pickImageLauncher.launch(intent);
        });

        btnBackProfile.setOnClickListener(v -> finish());

        // Bắt sự kiện Lưu thông tin
        btnSaveProfile.setOnClickListener(v -> saveUserProfile());
    }

    // Bộ công cụ hiện đại để mở Thư viện ảnh của điện thoại
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData(); // Lấy được ảnh khách vừa chọn
                    imgAvatar.setImageURI(imageUri); // In tạm lên màn hình cho khách xem
                    uploadImageToFirebase(); // Bắt đầu ném lên mạng
                }
            }
    );

    private void loadUserProfile() {
        // Tìm vào bảng Users -> đúng ID của người này
        db.collection("Users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Nếu đã từng lưu thông tin, lấy ra và gán vào giao diện
                        String name = documentSnapshot.getString("name");
                        String phone = documentSnapshot.getString("phone");
                        String address = documentSnapshot.getString("address");

                        if (name != null) edtProfileName.setText(name);
                        if (phone != null) edtProfilePhone.setText(phone);
                        if (address != null) edtProfileAddress.setText(address);
                    }
                });
    }

    private void saveUserProfile() {
        String name = edtProfileName.getText().toString().trim();
        String phone = edtProfilePhone.getText().toString().trim();
        String address = edtProfileAddress.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo gói dữ liệu
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("phone", phone);
        userMap.put("address", address);

        // Đẩy lên Firestore (dùng set() để tạo mới hoặc ghi đè nếu đã có)
        db.collection("Users").document(currentUserId)
                .set(userMap, SetOptions.merge()) // SỬA Ở ĐÂY
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Lỗi cập nhật!", Toast.LENGTH_SHORT).show());
    }

    // Hàm lưu ảnh lên kho Firebase Storage
    private void uploadImageToFirebase() {
        if (imageUri == null) {
            Toast.makeText(this, "Vui lòng chọn một bức ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        Toast.makeText(this, "Đang xử lý ảnh...", Toast.LENGTH_SHORT).show();

        try {
            // 1. Đọc ảnh từ điện thoại
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

            // Thu nhỏ kích thước ảnh xuống tối đa 400x400 pixel
            int maxWidth = 400;
            int maxHeight = 400;
            float scale = Math.min(((float)maxWidth / selectedImage.getWidth()), ((float)maxHeight / selectedImage.getHeight()));
            int finalWidth = (int)(selectedImage.getWidth() * scale);
            int finalHeight = (int)(selectedImage.getHeight() * scale);
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(selectedImage, finalWidth, finalHeight, true);

            // 2. Nén ảnh lại
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Dùng resizedBitmap thay vì selectedImage
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();

            // 3. Mã hóa ảnh thành chuỗi văn bản Base64
            String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

            // 4. Lưu chuỗi văn bản này vào Firestore
            Map<String, Object> avatarMap = new HashMap<>();
            avatarMap.put("avatarBase64", base64Image); // Đổi tên key để phân biệt với URL cũ

            FirebaseFirestore.getInstance().collection("Users").document(currentUserId)
                    .set(avatarMap, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Toast.makeText(ProfileActivity.this, "Cập nhật ảnh thành công!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Lỗi lưu ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Hàm lấy ảnh từ Database để hiện lên màn hình
    private void loadUserAvatar() {
        if (currentUserId == null) return;

        FirebaseFirestore.getInstance().collection("Users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Lấy chuỗi mã hóa từ Database
                        String base64Image = documentSnapshot.getString("avatarBase64");

                        if (base64Image != null && !base64Image.isEmpty()) {
                            // Dùng NO_WRAP cho đồng bộ
                            byte[] decodedString = Base64.decode(base64Image, Base64.NO_WRAP);

                            Glide.with(this)
                                    .asBitmap()
                                    .load(decodedString)
                                    .skipMemoryCache(true) // Bỏ qua bộ nhớ đệm
                                    .circleCrop()
                                    .into(imgAvatar);
                        }
                    }
                });
    }
}