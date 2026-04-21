package com.example.app_store;

import android.app.AlertDialog;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ManageOrdersActivity extends AppCompatActivity {

    private RecyclerView rvAdminOrders;
    private OrderAdapter orderAdapter;
    private List<Order> orderList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_orders);

        // Nút quay lại
        ImageView btnBack = findViewById(R.id.btnBackAdminOrders);
        btnBack.setOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();
        rvAdminOrders = findViewById(R.id.rvAdminOrders);
        rvAdminOrders.setLayoutManager(new LinearLayoutManager(this));

        orderList = new ArrayList<>();

        // Khởi tạo Adapter và gài hàm Update Trạng thái vào
        orderAdapter = new OrderAdapter(this, orderList, true, new OrderAdapter.OnOrderUpdateListener() {
            @Override
            public void onUpdateClick(Order order) {
                // Gọi cái hàm hiện bảng chọn trạng thái mà chúng ta vừa dán ở trên
                showUpdateStatusDialog(order);
            }
        });
        rvAdminOrders.setAdapter(orderAdapter);

        loadAllOrders();
    }

    private void loadAllOrders() {
        // Kéo toàn bộ đơn hàng về, đơn mới nhất xếp lên đầu
        db.collection("Orders")
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        orderList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            // 1. LẤY MÃ ĐƠN HÀNG CHUẨN (Chính là ID của dòng dữ liệu)
                            String orderId = doc.getId();

                            // 2. KÉO VÀ DỊCH NGÀY THÁNG
                            String formattedDate = "Đang cập nhật";
                            Timestamp timestamp = doc.getTimestamp("orderDate");
                            if (timestamp != null) {
                                Date date = timestamp.toDate(); // Đổi Timestamp thành Date
                                // Định dạng ngày giờ kiểu Việt Nam (Ngày/Tháng/Năm Giờ:Phút)
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                                formattedDate = sdf.format(date);
                            }

                            // Kéo các trường khác bình thường...
                            String name = doc.getString("customerName");
                            String phone = doc.getString("customerPhone");
                            String address = doc.getString("deliveryAddress");
                            String total = doc.getString("totalAmount");
                            String status = doc.getString("status");

                            // 3. NHÉT CÁI orderId VÀ formattedDate VÀO CONSTRUCTOR
                            orderList.add(new Order(orderId, formattedDate, name, phone, address, total, status));
                        }
                        orderAdapter.notifyDataSetChanged();
                    }
                });
    }

    // Hàm hiển thị bảng chọn và cập nhật lên Firebase
    private void showUpdateStatusDialog(Order order) {
        // 1. Tạo danh sách các trạng thái để Admin chọn
        String[] statuses = {"Đang xử lý", "Đang lấy hàng", "Đang giao hàng", "Giao thành công", "Đã hủy"};

        // 2. Tạo một cái Bảng thông báo (Dialog)
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Cập nhật trạng thái đơn hàng");

        // 3. Gắn danh sách trạng thái vào bảng và bắt sự kiện khi Admin bấm chọn 1 dòng
        builder.setItems(statuses, (dialog, which) -> {
            String selectedStatus = statuses[which]; // Lấy trạng thái Admin vừa chọn

            // 4. BẮN DỮ LIỆU LÊN FIREBASE
            // Tìm đúng cái đơn hàng có orderId đó trong bảng "Orders" và sửa trường "status"
            db.collection("Orders").document(order.getOrderId())
                    .update("status", selectedStatus)
                    .addOnSuccessListener(aVoid -> {
                        // Báo thành công
                        android.widget.Toast.makeText(ManageOrdersActivity.this, "Đã chuyển sang: " + selectedStatus, android.widget.Toast.LENGTH_SHORT).show();

                        // Cập nhật lại danh sách hiển thị trên màn hình Admin luôn
                        loadAllOrders();
                    })
                    .addOnFailureListener(e -> {
                        // Báo lỗi nếu mạng lag
                        android.widget.Toast.makeText(ManageOrdersActivity.this, "Lỗi cập nhật: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                    });
        });

        // Hiện cái bảng lên màn hình
        builder.show();
    }
}