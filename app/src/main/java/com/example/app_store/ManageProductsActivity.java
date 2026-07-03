package com.example.app_store;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_store.Adapter.AdminProductAdapter;
import com.example.app_store.Model.Product;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ManageProductsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminProductAdapter adapter;
    private List<Product> productList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_products);

        findViewById(R.id.btnBackManage).setOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recyclerViewAdminProducts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        productList = new ArrayList<>();

        adapter = new AdminProductAdapter(this, productList, new AdminProductAdapter.OnAdminProductListener() {
            @Override
            public void onDeleteClick(Product product, int position) {
                new android.app.AlertDialog.Builder(ManageProductsActivity.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc chắn muốn xóa sản phẩm \"" + product.getName() + "\" không?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            // Nếu Admin bấm "Xóa", mới tiến hành gọi hàm xóa dữ liệu trên Firebase
                            deleteProduct(product.getId(), position);
                        })
                        .setNegativeButton("Hủy", null) // Bấm "Hủy" thì đóng Dialog, không làm gì cả
                        .show();
            }

            @Override
            public void onEditClick(Product product) {
                // Tạo Intent để mở màn hình AddProductActivity
                Intent intent = new Intent(ManageProductsActivity.this, AddProductActivity.class);

                // Đóng gói dữ liệu của sản phẩm cần sửa để gửi sang
                intent.putExtra("editProductId", product.getId());
                intent.putExtra("editName", product.getName());
                intent.putExtra("editPrice", product.getPrice());
                intent.putExtra("editImageUrl", product.getImageUrl());

                startActivity(intent);
            }
        });

        recyclerView.setAdapter(adapter);

        // Bắt sự kiện nút Thêm mới
        FloatingActionButton fabAddProduct = findViewById(R.id.fabAddProduct);
        fabAddProduct.setOnClickListener(v -> {
            // Chuyển sang màn hình Thêm Sản Phẩm
            startActivity(new Intent(ManageProductsActivity.this, AddProductActivity.class));
        });

        loadProducts();
    }

    private void loadProducts() {
        // Bật "camera giám sát" thời gian thực cho danh sách sản phẩm của Admin
        db.collection("Products")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return; // Bỏ qua nếu có lỗi mạng
                    }

                    if (value != null) {
                        productList.clear(); // Xóa danh sách cũ đang hiển thị

                        // Cập nhật lại danh sách mới nhất từ mạng
                        for (DocumentSnapshot doc : value) {
                            productList.add(new Product(doc.getId(), doc.getString("name"), doc.getString("price"), doc.getString("imageUrl")));
                        }

                        adapter.notifyDataSetChanged(); // Báo cho giao diện vẽ lại ngay lập tức
                    }
                });
    }

    private void deleteProduct(String productId, int position) {
        // Chỉ cần ra lệnh xóa trên Firebase. Việc cập nhật giao diện sẽ do addSnapshotListener tự lo!
        db.collection("Products").document(productId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi xóa", Toast.LENGTH_SHORT).show();
                });
    }
}