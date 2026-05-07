package com.example.app_store;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_store.Adapter.OrderAdapter;
import com.example.app_store.Model.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class OrderHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerViewOrders;
    private OrderAdapter orderAdapter;
    private List<Order> orderList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_order_history);

        ImageView btnBackHistory = findViewById(R.id.btnBackHistory);
        btnBackHistory.setOnClickListener(v -> finish());

        recyclerViewOrders = findViewById(R.id.recyclerViewOrders);
        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(this));

        orderList = new ArrayList<>();

        // Khởi tạo Adapter: Khách hàng (isAdmin = false), không cần nút Cập nhật (listener = null)
        orderAdapter = new OrderAdapter(this, orderList, false, null);
        recyclerViewOrders.setAdapter(orderAdapter);

        loadOrderHistory();
    }

    private void loadOrderHistory() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUserId = auth.getCurrentUser().getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // CHÚ Ý: Lọc theo userId và Sắp xếp theo biến lưu thời gian (kiểu long)
        // Nếu trong Firestore bạn lưu tên trường là "orderDate" (kiểu số long) thì giữ nguyên "orderDate"
        // Nếu bạn lưu là "timestamp" thì đổi lại thành "timestamp" cho khớp nhé.
        db.collection("Orders")
                .whereEqualTo("userId", currentUserId)
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    // 1. Kiểm tra lỗi mất kết nối hoặc thiếu quyền (Rules)
                    if (error != null) {
                        Toast.makeText(this, "Không thể tải lịch sử: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 2. Cập nhật dữ liệu Realtime
                    if (value != null) {
                        orderList.clear(); // Xóa list cũ trước khi đổ list mới vào

                        for (DocumentSnapshot doc : value.getDocuments()) {
                            // 1. Tự động map các trường bên trong Document
                            Order order = doc.toObject(Order.class);

                            if (order != null) {
                                // 2. SỬA LỖI CRASH Ở ĐÂY: Lấy ID của Document (CaJ6f...) gán thủ công vào orderId
                                order.setOrderId(doc.getId());

                                // 3. Thêm vào danh sách
                                orderList.add(order);
                            }
                        }

                        // Cập nhật lại giao diện ngay lập tức
                        orderAdapter.notifyDataSetChanged();

                        if (orderList.isEmpty()) {
                            Toast.makeText(this, "Bạn chưa có đơn hàng nào.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}