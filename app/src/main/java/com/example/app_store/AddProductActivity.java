package com.example.app_store;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddProductActivity extends AppCompatActivity {

    private EditText edtAddProductName, edtAddProductPrice, edtAddProductImageUrl;
    private Button btnSaveNewProduct;
    private ImageView btnBackAddProduct;
    private TextView tvAddProductTitle;
    private String editProductId = null;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_product);

        // Ánh xạ View
        edtAddProductName = findViewById(R.id.edtAddProductName);
        edtAddProductPrice = findViewById(R.id.edtAddProductPrice);
        edtAddProductImageUrl = findViewById(R.id.edtAddProductImageUrl);
        btnSaveNewProduct = findViewById(R.id.btnSaveNewProduct);
        btnBackAddProduct = findViewById(R.id.btnBackAddProduct);
        tvAddProductTitle = findViewById(R.id.tvAddProductTitle);

        db = FirebaseFirestore.getInstance();

        // Nút quay lại
        btnBackAddProduct.setOnClickListener(v -> finish());

        editProductId = getIntent().getStringExtra("editProductId");

        if (editProductId != null) {
            // NẾU LÀ CHẾ ĐỘ SỬA: Đổi giao diện và điền sẵn dữ liệu cũ
            tvAddProductTitle.setText("Chỉnh Sửa Sản Phẩm");
            btnSaveNewProduct.setText("CẬP NHẬT SẢN PHẨM");

            edtAddProductName.setText(getIntent().getStringExtra("editName"));
            edtAddProductPrice.setText(getIntent().getStringExtra("editPrice"));
            edtAddProductImageUrl.setText(getIntent().getStringExtra("editImageUrl"));
        }

        // Bắt sự kiện Lưu sản phẩm
        btnSaveNewProduct.setOnClickListener(v -> saveProductToFirebase());
    }

    private void saveProductToFirebase() {
        String name = edtAddProductName.getText().toString().trim();
        String price = edtAddProductPrice.getText().toString().trim();
        String imageUrl = edtAddProductImageUrl.getText().toString().trim();

        if (name.isEmpty() || price.isEmpty() || imageUrl.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> productMap = new HashMap<>();
        productMap.put("name", name);
        productMap.put("price", price);
        productMap.put("imageUrl", imageUrl);

        if (editProductId != null) {
            // CHẾ ĐỘ SỬA: Cập nhật dữ liệu đè lên ID cũ
            db.collection("Products").document(editProductId)
                    .update(productMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AddProductActivity.this, "Đã cập nhật sản phẩm!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(AddProductActivity.this, "Lỗi cập nhật", Toast.LENGTH_SHORT).show());
        } else {
            // CHẾ ĐỘ THÊM: Tạo sản phẩm mới hoàn toàn
            db.collection("Products")
                    .add(productMap)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(AddProductActivity.this, "Đã thêm sản phẩm thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(AddProductActivity.this, "Lỗi thêm mới", Toast.LENGTH_SHORT).show());
        }
    }
}