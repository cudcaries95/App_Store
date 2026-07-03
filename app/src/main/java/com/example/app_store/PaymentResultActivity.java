package com.example.app_store;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class PaymentResultActivity extends AppCompatActivity {

    private TextView tvPaymentStatus;
    private Button btnBackToHome;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_result);

        tvPaymentStatus = findViewById(R.id.tvPaymentStatus);
        btnBackToHome = findViewById(R.id.btnBackToHome);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Xử lý Intent được gọi tới từ DeepLink MoMo
        handleIntent(getIntent());

        btnBackToHome.setOnClickListener(v -> {
            Intent intent = new Intent(PaymentResultActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void handleIntent(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null) {
            // Lấy các tham số do MoMo trả về trên URL
            String resultCodeStr = uri.getQueryParameter("resultCode");
            String orderId = uri.getQueryParameter("orderId");
            String message = uri.getQueryParameter("message");

            if (resultCodeStr != null && resultCodeStr.equals("0")) {
                // resultCode = 0 là Thanh toán thành công
                tvPaymentStatus.setText("Thanh toán thành công!");
                tvPaymentStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

                if (orderId != null) {
                    updateOrderStatus(orderId, "PAID");
                }
            } else {
                // Thanh toán thất bại hoặc người dùng hủy
                tvPaymentStatus.setText("Thanh toán thất bại hoặc đã hủy!\nLý do: " + message);
                tvPaymentStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));

                if (orderId != null) {
                    // Bạn có thể update thành CANCELED hoặc để nguyên PENDING tùy logic dự án
                    updateOrderStatus(orderId, "CANCELED");
                }
                btnBackToHome.setVisibility(View.VISIBLE);
            }
        } else {
            tvPaymentStatus.setText("Không nhận được dữ liệu thanh toán.");
            btnBackToHome.setVisibility(View.VISIBLE);
        }
    }

    private void updateOrderStatus(String orderId, String status) {
        // Cập nhật trạng thái đơn hàng trên Firestore
        db.collection("Orders").document(orderId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    if (status.equals("PAID")) {
                        clearUserCart(); // Xóa giỏ hàng nếu thanh toán thành công
                    } else {
                        btnBackToHome.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi cập nhật đơn hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnBackToHome.setVisibility(View.VISIBLE);
                });
    }

    private void clearUserCart() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();

        // Vì ta không có list cartItem ở đây, ta sẽ truy vấn và xóa toàn bộ collection Items trong Cart của User
        db.collection("Cart").document(userId).collection("Items")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot document : task.getResult()) {
                            document.getReference().delete();
                        }
                    }
                    // Hiển thị nút quay về sau khi đã xử lý xong mọi thứ
                    btnBackToHome.setVisibility(View.VISIBLE);
                });
    }
}