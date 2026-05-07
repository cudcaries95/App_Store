package com.example.app_store;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_store.Adapter.OrderAdapter;
import com.example.app_store.Model.Order;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ManageOrdersActivity extends AppCompatActivity {

    private RecyclerView rcvOrders;
    private OrderAdapter orderAdapter;
    private List<Order> orderList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // TODO: Bạn cần thay đổi giá trị này dựa trên quyền của user khi đăng nhập
    // Nếu user đăng nhập là Admin -> true, Khách hàng -> false.
    // Có thể truyền qua Intent từ màn hình trước: getIntent().getBooleanExtra("IS_ADMIN", false);
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_orders);

        rcvOrders = findViewById(R.id.rvAdminOrders);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Lấy quyền Admin từ Intent (nếu có truyền)
        isAdmin = getIntent().getBooleanExtra("IS_ADMIN", false);

        setupRecyclerView();
        loadOrdersFromFirestore();
    }

    private void setupRecyclerView() {
        rcvOrders.setLayoutManager(new LinearLayoutManager(this));
        orderList = new ArrayList<>();

        // Khởi tạo OrderAdapter với tham số isAdmin
        orderAdapter = new OrderAdapter(this, orderList, isAdmin, new OrderAdapter.OnOrderUpdateListener() {
            @Override
            public void onUpdateClick(Order order) {
                // Chỉ Admin mới gọi được hàm này (Do nút Cập nhật chỉ hiện với Admin)
                // TODO: Mở Dialog hoặc Activity để Admin chọn đổi trạng thái (VD: PENDING -> SHIPPING)
                Toast.makeText(ManageOrdersActivity.this, "Đang cập nhật đơn: " + order.getOrderId(), Toast.LENGTH_SHORT).show();
            }
        });

        rcvOrders.setAdapter(orderAdapter);
    }

    private void loadOrdersFromFirestore() {
        // Luôn sắp xếp đơn hàng theo ngày đặt (mới nhất xếp trên cùng)
        // Lưu ý: Trường ngày đặt trong Firestore của bạn phải lưu đúng tên là "orderDate"
        Query query = db.collection("Orders").orderBy("orderDate", Query.Direction.DESCENDING);

        if (!isAdmin) {
            // NẾU LÀ KHÁCH HÀNG: Chỉ lấy những đơn hàng có userId trùng với UID hiện tại
            if (mAuth.getCurrentUser() != null) {
                String currentUserId = mAuth.getCurrentUser().getUid();
                query = db.collection("Orders")
                        .whereEqualTo("userId", currentUserId)
                        .orderBy("orderDate", Query.Direction.DESCENDING);
            } else {
                Toast.makeText(this, "Vui lòng đăng nhập để xem đơn hàng!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Thực thi truy vấn
        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            orderList.clear();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                // Tự động map dữ liệu từ Firestore vào class Order
                Order order = document.toObject(Order.class);
                orderList.add(order);
            }

            // Cập nhật giao diện sau khi có dữ liệu
            orderAdapter.notifyDataSetChanged();

            if (orderList.isEmpty()) {
                Toast.makeText(this, "Chưa có đơn hàng nào.", Toast.LENGTH_SHORT).show();
            }

        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}