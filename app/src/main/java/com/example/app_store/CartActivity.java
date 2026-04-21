package com.example.app_store;

import android.app.AlertDialog;
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

            // Gọi hàm hiển thị Bảng nhập địa chỉ
            showAddressDialog();
        });

        loadCartData();
    }

    private void showAddressDialog() {
        String currentUserId = auth.getCurrentUser().getUid();

        // 1. Tạo và thiết lập giao diện cho Dialog
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_checkout_address, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(true) // Cho phép bấm ra ngoài để đóng
                .create();

        // Bo góc cho nền của Dialog nếu muốn đẹp hơn (tùy chọn)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Ánh xạ các View trong Dialog
        EditText edtName = view.findViewById(R.id.edtDialogName);
        EditText edtPhone = view.findViewById(R.id.edtDialogPhone);
        EditText edtAddress = view.findViewById(R.id.edtDialogAddress);
        Button btnConfirmOrder = view.findViewById(R.id.btnConfirmOrderDialog);

        // 2. Lấy thông tin từ Hồ sơ (nếu có) để điền sẵn vào ô
        db.collection("Users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String savedName = documentSnapshot.getString("name");
                        String savedPhone = documentSnapshot.getString("phone");
                        String savedAddress = documentSnapshot.getString("address");

                        if (savedName != null) edtName.setText(savedName);
                        if (savedPhone != null) edtPhone.setText(savedPhone);
                        if (savedAddress != null) edtAddress.setText(savedAddress);
                    }
                    dialog.show(); // Lấy xong thì hiện bảng lên
                });

        // 3. Xử lý khi bấm nút "XÁC NHẬN ĐẶT HÀNG" trong Dialog
        btnConfirmOrder.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String phone = edtPhone.getText().toString().trim();
            String address = edtAddress.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
                Toast.makeText(CartActivity.this, "Vui lòng nhập đủ thông tin giao hàng!", Toast.LENGTH_SHORT).show();
                return;
            }

            // --- BẮT ĐẦU QUÁ TRÌNH LƯU ĐƠN HÀNG (Giống hệt code cũ nhưng có thêm 3 trường địa chỉ) ---
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("userId", currentUserId);
            orderMap.put("totalAmount", tvTotalPrice.getText().toString());
            orderMap.put("orderDate", FieldValue.serverTimestamp());
            orderMap.put("status", "Đang xử lý");

            // LƯU THÊM THÔNG TIN GIAO HÀNG VÀO ĐƠN NÀY
            orderMap.put("customerName", name);
            orderMap.put("customerPhone", phone);
            orderMap.put("deliveryAddress", address);

            List<Map<String, Object>> orderItems = new ArrayList<>();
            for (CartItem item : cartItemList) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("productId", item.getProductId());
                itemMap.put("productName", item.getProductName());
                itemMap.put("price", item.getProductPrice());
                itemMap.put("quantity", item.getQuantity());
                orderItems.add(itemMap);
            }
            orderMap.put("items", orderItems);

            db.collection("Orders")
                    .add(orderMap)
                    .addOnSuccessListener(documentReference -> {
                        dialog.dismiss(); // Đóng Dialog
                        clearCartAfterCheckout(currentUserId); // Dọn giỏ hàng
                    })
                    .addOnFailureListener(e -> Toast.makeText(CartActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    // Hàm xóa sản phẩm khỏi Firebase
    private void deleteCartItem(CartItem item, int position) {
        String currentUserId = auth.getCurrentUser().getUid();

        db.collection("Cart").document(currentUserId).collection("Items")
                .document(item.getDocumentId()) // Tìm đúng ID của món hàng trên Firebase
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Xóa thành công trên mạng -> Xóa luôn khỏi danh sách hiển thị
                    cartItemList.remove(position);
                    cartAdapter.notifyItemRemoved(position);
                    cartAdapter.notifyItemRangeChanged(position, cartItemList.size());

                    // Tính lại tổng tiền
                    calculateTotalPrice();

                    Toast.makeText(CartActivity.this, "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(CartActivity.this, "Lỗi khi xóa!", Toast.LENGTH_SHORT).show());
    }

    // Hàm tách riêng để tải dữ liệu (Rút gọn phần tính tiền)
    private void loadCartData() {
        if (auth.getCurrentUser() == null) return;
        String currentUserId = auth.getCurrentUser().getUid();

        db.collection("Cart").document(currentUserId).collection("Items")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        cartItemList.clear();

                        for (DocumentSnapshot doc : task.getResult()) {
                            String docId = doc.getId();
                            String productId = doc.getString("productId");
                            String name = doc.getString("productName");
                            String priceStr = doc.getString("productPrice");
                            String imageUrl = doc.getString("productImageUrl");
                            Long quantityLong = doc.getLong("quantity");
                            int quantity = (quantityLong != null) ? quantityLong.intValue() : 1;

                            cartItemList.add(new CartItem(docId, productId, name, priceStr, imageUrl, quantity));
                        }

                        cartAdapter.notifyDataSetChanged();
                        calculateTotalPrice(); // Gọi hàm tính tiền lần đầu tiên
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

    // Hàm xóa sạch giỏ hàng sau khi đặt thành công
    private void clearCartAfterCheckout(String currentUserId) {
        // Duyệt qua từng sản phẩm đang có trên màn hình
        for (CartItem item : cartItemList) {
            db.collection("Cart").document(currentUserId).collection("Items")
                    .document(item.getDocumentId()) // Lấy ID của món hàng
                    .delete(); // Xóa nó khỏi Firebase
        }
        // Làm sạch danh sách trên màn hình điện thoại
        cartItemList.clear();
        cartAdapter.notifyDataSetChanged();
        tvTotalPrice.setText("0 đ");

        // Thông báo cho người dùng và quay về Trang chủ
        Toast.makeText(CartActivity.this, "Đặt hàng thành công! Cảm ơn bạn.", Toast.LENGTH_LONG).show();
        finish(); // Đóng màn hình Giỏ hàng lại
    }
}