package com.example.app_store.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_store.Model.Order;
import com.example.app_store.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder>{
    private Context context;
    private List<Order> orderList;
    private OnOrderUpdateListener listener;
    private boolean isAdmin;

    public interface OnOrderUpdateListener {
        void onUpdateClick(Order order);
    }

    public OrderAdapter(Context context, List<Order> orderList, boolean isAdmin, OnOrderUpdateListener listener) {
        this.context = context;
        this.orderList = orderList;
        this.isAdmin = isAdmin;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_admin, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);

        // 1. FORMAT MÃ ĐƠN
        String shortId = order.getOrderId() != null && order.getOrderId().length() > 8
                ? order.getOrderId().substring(0, 8)
                : order.getOrderId();
        holder.tvOrderId.setText("Mã đơn: #" + shortId.toUpperCase());

        // 2. HIỂN THỊ NGÀY ĐẶT
        Date date = order.getOrderDate(); // Lấy biến Date đã khớp với Firestore

        if (date != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            holder.tvOrderDate.setText("Ngày đặt: " + sdf.format(date));
        } else {
            holder.tvOrderDate.setText("Ngày đặt: Không xác định");
        }

        // 3. THÔNG TIN CƠ BẢN CỦA ĐƠN HÀNG (Tổng tiền, Trạng thái, Phương thức thanh toán)
        holder.tvTotal.setText("Tổng: " + order.getTotalAmount() + " đ");

        // Hiển thị phương thức thanh toán (Bạn nhớ kiểm tra xem trong file XML đã có tvPaymentMethod chưa nhé)
        String paymentMethod = order.getPaymentMethod() != null ? order.getPaymentMethod() : "COD";
        if (holder.tvPaymentMethod != null) {
            holder.tvPaymentMethod.setText("Thanh toán: " + paymentMethod);
        }

        // Đổi màu text theo trạng thái để dễ nhìn hơn
        String status = order.getStatus();
        holder.tvStatus.setText("Trạng thái: " + status);
        if (order.getStatus().equals("PAID")) {
            holder.tvStatus.setTextColor(Color.GREEN);
            holder.tvStatus.setText("Đã thanh toán (MoMo)");
        } else if (order.getStatus().equals("PLACED")) {
            holder.tvStatus.setTextColor(Color.BLUE);
            holder.tvStatus.setText("Chờ giao hàng (COD)");
        } else if (order.getStatus().equals("PENDING_MOMO")) {
            holder.tvStatus.setTextColor(Color.parseColor("#FFA500")); // Orange
            holder.tvStatus.setText("Chờ thanh toán MoMo");
        } else if ("Đã hủy".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#F44336")); // Màu đỏ
        } else if ("Giao thành công".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // Màu xanh lá
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#2196F3")); // Xanh dương (Đang giao, v.v.)
        }

        // 4. PHÂN LOẠI HIỂN THỊ THEO QUYỀN (ADMIN vs KHÁCH HÀNG)
        if (isAdmin) {
            // ADMIN: Hiện chi tiết giao hàng & Nút Cập nhật
            holder.tvName.setVisibility(View.VISIBLE);
            holder.tvPhone.setVisibility(View.VISIBLE);
            holder.tvAddress.setVisibility(View.VISIBLE);
            holder.btnCancelOrder.setVisibility(View.GONE);

            String currentStatus = order.getStatus();
            if ("Đã hủy".equalsIgnoreCase(currentStatus) || "Giao thành công".equalsIgnoreCase(currentStatus)) {
                holder.btnUpdate.setVisibility(View.GONE); // Ẩn nút cập nhật
            } else {
                holder.btnUpdate.setVisibility(View.VISIBLE); // Hiện nút cập nhật cho các trạng thái khác (Chờ duyệt, đang giao...)
            }

            // Đảm bảo gọi đúng hàm Getter theo Model Order của bạn
            holder.tvName.setText("Khách hàng: " + order.getCustomerName());
            holder.tvPhone.setText("SĐT: " + order.getPhone()); // Đổi thành getCustomerPhone() nếu class của bạn dùng tên đó
            holder.tvAddress.setText("Địa chỉ: " + order.getAddress()); // Đổi thành getDeliveryAddress() nếu cần

            holder.btnUpdate.setOnClickListener(v -> {
                if (listener != null) listener.onUpdateClick(order);
            });

        } else {
            // KHÁCH HÀNG: Giấu chi tiết giao hàng & Nút Cập nhật
            holder.tvName.setVisibility(View.GONE);
            holder.tvPhone.setVisibility(View.GONE);
            holder.tvAddress.setVisibility(View.GONE);
            holder.btnUpdate.setVisibility(View.GONE);

            // Logic tính thời gian 24h để hiện nút Hủy
            if (!isAdmin && date != null) {
                long diffInMillies = Math.abs(System.currentTimeMillis() - date.getTime());
                long diffInHours = diffInMillies / (60 * 60 * 1000);

                if (diffInHours < 24
                        && !"Đã hủy".equalsIgnoreCase(order.getStatus())
                        && !"Giao thành công".equalsIgnoreCase(order.getStatus())) {
                    holder.btnCancelOrder.setVisibility(View.VISIBLE);
                } else {
                    holder.btnCancelOrder.setVisibility(View.GONE);
                }
            }

            holder.btnCancelOrder.setOnClickListener(v -> confirmCancelOrder(order.getOrderId(), position));
        }
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvAddress, tvTotal, tvStatus, tvOrderId, tvOrderDate, tvPaymentMethod;
        Button btnUpdate, btnCancelOrder;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvName = itemView.findViewById(R.id.tvAdminOrderName);
            tvPhone = itemView.findViewById(R.id.tvAdminOrderPhone);
            tvAddress = itemView.findViewById(R.id.tvAdminOrderAddress);
            tvTotal = itemView.findViewById(R.id.tvAdminOrderTotal);
            tvStatus = itemView.findViewById(R.id.tvAdminOrderStatus);

            // TODO: Bạn cần thêm 1 TextView vào file item_order_admin.xml và ánh xạ ID vào đây
            tvPaymentMethod = itemView.findViewById(R.id.tvPaymentMethod);

            btnUpdate = itemView.findViewById(R.id.btnUpdateStatus);
            btnCancelOrder = itemView.findViewById(R.id.btnCancelOrder);
        }
    }

    private void confirmCancelOrder(String orderId,final int position) {
        // Tạo một EditText để người dùng nhập lý do
        final android.widget.EditText edtReason = new android.widget.EditText(context);
        edtReason.setHint("Nhập lý do hủy...");

        // Tạo khoảng cách (margin) cho EditText để nhìn đẹp hơn
        android.widget.FrameLayout container = new android.widget.FrameLayout(context);
        android.widget.FrameLayout.LayoutParams params = new  android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50; // Khoảng cách lề trái
        params.rightMargin = 50; // Khoảng cách lề phải
        edtReason.setLayoutParams(params);
        container.addView(edtReason);

        new android.app.AlertDialog.Builder(context)
                .setTitle("Xác nhận hủy")
                .setView(container)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    final String reason = edtReason.getText().toString().trim(); // Thêm final ở đây

                    String finalReason = reason.isEmpty() ? "Khách hàng không lý do" : reason;

                    java.util.Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("status", "Đã hủy");
                    updates.put("cancelReason", finalReason);

                    FirebaseFirestore.getInstance().collection("Orders").document(orderId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                // Bây giờ dùng position ở đây sẽ không báo đỏ nữa
                                orderList.get(position).setStatus("Đã hủy");
                                orderList.get(position).setCancelReason(finalReason);
                                notifyItemChanged(position);
                            });
                })
                .show();
    }
}