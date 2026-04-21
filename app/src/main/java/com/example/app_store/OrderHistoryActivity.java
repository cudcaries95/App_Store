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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

        // Truy vấn lấy danh sách đơn hàng CỦA TÀI KHOẢN NÀY
        db.collection("Orders")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        orderList.clear();
                        for (DocumentSnapshot doc : task.getResult()) {
                            String orderId = doc.getId();
                            String totalAmount = doc.getString("totalAmount");
                            String status = doc.getString("status");

                            // Xử lý chuyển đổi Timestamp thành ngày giờ dễ đọc
                            String dateStr = "";
                            com.google.firebase.Timestamp timestamp = doc.getTimestamp("orderDate");
                            if (timestamp != null) {
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                                dateStr = sdf.format(timestamp.toDate());
                            }

                            orderList.add(new Order(orderId, totalAmount, status, dateStr));
                        }
                        orderAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Không thể tải lịch sử", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}