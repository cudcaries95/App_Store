package com.example.app_store;

import android.icu.text.DecimalFormat;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AdminStatisticsActivity extends AppCompatActivity {

    private TextView tvTotalRevenue, tvSuccessOrders, tvCancelledOrders, tvTotalOrders;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_statistics);

        // Ánh xạ UI
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvSuccessOrders = findViewById(R.id.tvSuccessOrders);
        tvCancelledOrders = findViewById(R.id.tvCancelledOrders);
        tvTotalOrders = findViewById(R.id.tvTotalOrders);
        db = FirebaseFirestore.getInstance();

        // Nút quay lại
        ImageView btnBack = findViewById(R.id.btnBackStats);
        btnBack.setOnClickListener(v -> finish());

        // Gọi hàm tính toán
        calculateStatistics();
    }

    private void calculateStatistics() {
        // Kéo TOÀN BỘ đơn hàng về để tính
        db.collection("Orders").get().addOnSuccessListener(queryDocumentSnapshots -> {
            long totalRevenue = 0;
            int countTotal = 0;
            int countSuccess = 0;
            int countCancelled = 0;

            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                countTotal++; // Cứ lặp qua 1 đơn là cộng 1 vào tổng số đơn

                String status = doc.getString("status");
                String priceString = doc.getString("totalAmount");

                if (status != null) {
                    if (status.equals("Giao thành công")) {
                        countSuccess++; // Đếm đơn thành công

                        // Cộng tiền vào tổng doanh thu
                        if (priceString != null) {
                            String rawNumber = priceString.replaceAll("[^0-9]", "");
                            if (!rawNumber.isEmpty()) {
                                totalRevenue += Long.parseLong(rawNumber);
                            }
                        }
                    } else if (status.equals("Đã hủy")) {
                        countCancelled++; // Đếm đơn hủy
                    }
                }
            }

            // In kết quả ra màn hình
            DecimalFormat formatter = new DecimalFormat("#,###");
            tvTotalRevenue.setText(formatter.format(totalRevenue) + " đ");
            tvTotalOrders.setText(String.valueOf(countTotal));
            tvSuccessOrders.setText(String.valueOf(countSuccess));
            tvCancelledOrders.setText(String.valueOf(countCancelled));

        }).addOnFailureListener(e -> {
            Toast.makeText(AdminStatisticsActivity.this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}