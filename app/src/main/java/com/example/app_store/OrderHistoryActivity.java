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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        orderAdapter = new OrderAdapter(this, orderList, false, null);
        recyclerViewOrders.setAdapter(orderAdapter);

        loadOrderHistory();
    }

    private void loadOrderHistory() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        String currentUserId = auth.getCurrentUser().getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Truy vấn lấy danh sách đơn hàng CỦA TÀI KHOẢN NÀY, sắp xếp mới nhất lên đầu
        db.collection("Orders")
                .whereEqualTo("userId", currentUserId)
                .orderBy("orderDate", Query.Direction.DESCENDING) // Sắp xếp theo thời gian giảm dần
                .addSnapshotListener((value, error) -> {
                    // 1. Kiểm tra lỗi trước
                    if (error != null) {
                        android.widget.Toast.makeText(this, "Không thể tải lịch sử: " + error.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                        return; // Thoát khỏi hàm nếu có lỗi
                    }

                    // 2. Kiểm tra xem dữ liệu có tồn tại không
                    if (value != null) {
                        orderList.clear(); // Xóa danh sách cũ để cập nhật danh sách mới

                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String orderId = doc.getId();
                            String totalAmount = doc.getString("totalAmount");
                            String status = doc.getString("status");

                            // LẤY TRỰC TIẾP ĐỐI TƯỢNG DATE TỪ TIMESTAMP
                            Timestamp timestamp = doc.getTimestamp("orderDate");
                            Date dateObject = (timestamp != null) ? timestamp.toDate() : new Date();

                            // NHÉT TRỰC TIẾP DATE VÀO CONSTRUCTOR
                            orderList.add(new Order(orderId, totalAmount, status, dateObject));
                        }

                        // Cập nhật lại giao diện
                        orderAdapter.notifyDataSetChanged();
                    }
                });
    }
}