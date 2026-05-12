package com.example.app_store;

import android.os.Bundle;
import android.util.Log;
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
        if (auth.getCurrentUser() == null) return;

        String currentUserId = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Sử dụng SnapshotListener để cập nhật trạng thái đơn hàng (PAID/FAILED) ngay lập tức
        db.collection("Orders")
                .whereEqualTo("userId", currentUserId)
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        // LƯU Ý: Nếu Logcat báo lỗi "The query requires an index",
                        // hãy nhấn vào đường link màu xanh trong Logcat để Firebase tự tạo Index cho bạn.
                        Log.e("FirestoreError", "Lỗi: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        orderList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Order order = doc.toObject(Order.class);
                            if (order != null) {
                                // Gán document ID để dùng cho các thao tác xem chi tiết/hủy đơn sau này
                                order.setOrderId(doc.getId());
                                orderList.add(order);
                            }
                        }
                        orderAdapter.notifyDataSetChanged();
                    }
                });
    }
}