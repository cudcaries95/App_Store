package com.example.app_store;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_store.Adapter.CartAdapter;
import com.example.app_store.Model.CartItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartActivity extends AppCompatActivity {

    private RecyclerView recyclerViewCart;
    private CartAdapter cartAdapter;
    private List<CartItem> cartItemList = new ArrayList<>();
    private TextView tvTotalPrice;
    private Button btnCheckout;
    private ImageView btnBackCart;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cart);

        // Ánh xạ View
        recyclerViewCart = findViewById(R.id.recyclerViewCart);
        recyclerViewCart.setLayoutManager(new LinearLayoutManager(this));
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        btnCheckout = findViewById(R.id.btnCheckout);
        btnBackCart = findViewById(R.id.btnBackCart);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Truyền hàm lắng nghe sự kiện Xóa vào Adapter
        cartAdapter = new CartAdapter(this, cartItemList, new CartAdapter.OnCartItemDeleteListener() {
            @Override
            public void onDeleteClick(CartItem item, int position) {
                deleteCartItem(item, position); // Gọi hàm xử lý xóa
            }
        });
        recyclerViewCart.setAdapter(cartAdapter);

        // Nút quay lại
        btnBackCart.setOnClickListener(v -> finish());

        btnCheckout.setOnClickListener(v -> {
            // 1. Kiểm tra giỏ hàng có trống không
            if (cartItemList.isEmpty()) {
                Toast.makeText(CartActivity.this, "Giỏ hàng đang trống!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Chuyển sang CheckoutActivity và mang theo dữ liệu
            Intent intent = new Intent(CartActivity.this, CheckoutActivity.class);
            intent.putExtra("USER_ID", auth.getCurrentUser().getUid()); // Chỉ cần gửi UID

            // Truyền tổng tiền (String: "1.500.000 đ")
            intent.putExtra("TOTAL_PRICE", tvTotalPrice.getText().toString());

            // Truyền danh sách sản phẩm (Ép kiểu về ArrayList để Serializable gửi đi được)
            intent.putExtra("CART_LIST", new ArrayList<>(cartItemList));

            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Chuyển việc tải dữ liệu vào onStart để giỏ hàng LUÔN LÀM MỚI mỗi khi màn hình hiển thị lại
        loadCartData();
    }

    // Hàm xóa sản phẩm khỏi Firebase
    private void deleteCartItem(CartItem item, int position) {
        String currentUserId = auth.getCurrentUser().getUid();
        db.collection("Cart").document(currentUserId).collection("Items")
                .document(item.getDocumentId()) // Tìm đúng ID của món hàng trên Firebase
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CartActivity.this, "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(CartActivity.this, "Lỗi khi xóa!", Toast.LENGTH_SHORT).show());
    }

    // Hàm tách riêng để tải dữ liệu (Rút gọn phần tính tiền)
    private void loadCartData() {
        if (auth.getCurrentUser() == null) return;
        String currentUserId = auth.getCurrentUser().getUid();

        // Sử dụng addSnapshotListener thay vì get() để cập nhật thời gian thực
        db.collection("Cart").document(currentUserId).collection("Items")
                .addSnapshotListener(CartActivity.this, (value, error) -> {

                    if (error != null) {
                        if (auth.getCurrentUser() == null) return;
                        Toast.makeText(this, "Lỗi tải dữ liệu!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        cartItemList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String docId = doc.getId();
                            String productId = doc.getString("productId");
                            String name = doc.getString("productName");
                            String priceStr = doc.getString("productPrice");
                            String imageUrl = doc.getString("productImageUrl");
                            Long quantityLong = doc.getLong("quantity");
                            int quantity = (quantityLong != null) ? quantityLong.intValue() : 1;

                            cartItemList.add(new CartItem(docId, productId, name, priceStr, imageUrl, quantity));
                        }

                        // Cập nhật Adapter và tính lại tiền
                        cartAdapter.notifyDataSetChanged();
                        calculateTotalPrice();

                        // Nếu giỏ hàng trống sau khi thanh toán, có thể thông báo hoặc đóng màn hình
                        if (cartItemList.isEmpty()) {
                            // tvTotalPrice.setText("0 đ"); // Đảm bảo tiền về 0
                            // Bạn có thể hiện một thông báo nhỏ ở đây nếu muốn
                        }
                    }
                });
    }

    // Hàm Quét toàn bộ danh sách hiện tại và tính lại Tổng tiền
    private void calculateTotalPrice() {
        long totalAmount = 0;
        for (CartItem item : cartItemList) {
            String priceStr = item.getProductPrice();
            if (priceStr != null) {
                String cleanPrice = priceStr.replaceAll("[^0-9]", "");
                if (!cleanPrice.isEmpty()) {
                    totalAmount += Long.parseLong(cleanPrice) * item.getQuantity();
                }
            }
        }
        tvTotalPrice.setText(String.format("%,d đ", totalAmount).replace(',', '.'));
    }
}