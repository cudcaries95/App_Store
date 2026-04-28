package com.example.app_store.Adapter;

import android.content.Context;
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

        // Cắt bớt ID cho đỡ dài (Ví dụ: ID là 8Xj2n... thì mình lấy 8 ký tự đầu thôi cho đẹp)
        String shortId = order.getOrderId().length() > 8 ? order.getOrderId().substring(0, 8) : order.getOrderId();

        holder.tvOrderId.setText("Mã đơn: #" + shortId.toUpperCase());
        holder.tvOrderDate.setText("Ngày đặt: " + order.getOrderDate()); // Gọi biến ngày thực tế

        holder.tvTotal.setText("Tổng: " + order.getTotalAmount());
        holder.tvStatus.setText("Trạng thái: " + order.getStatus());

        // 2. PHÂN LOẠI HIỂN THỊ
        if (isAdmin) {
            // NẾU LÀ ADMIN: Hiện toàn bộ Tên, SĐT, Địa chỉ và Nút Cập nhật
            holder.tvName.setVisibility(View.VISIBLE);
            holder.tvPhone.setVisibility(View.VISIBLE);
            holder.tvAddress.setVisibility(View.VISIBLE);
            holder.btnUpdate.setVisibility(View.VISIBLE);
            holder.btnCancelOrder.setVisibility(View.GONE);

            holder.tvName.setText("Khách hàng: " + order.getCustomerName());
            holder.tvPhone.setText("SĐT: " + order.getCustomerPhone());
            holder.tvAddress.setText("Địa chỉ: " + order.getDeliveryAddress());

            holder.btnUpdate.setOnClickListener(v -> {
                if (listener != null) listener.onUpdateClick(order);
            });
        } else {
            // NẾU LÀ KHÁCH HÀNG: Giấu tịt Tên, SĐT, Địa chỉ và Nút Cập nhật đi cho gọn
            holder.tvName.setVisibility(View.GONE);
            holder.tvPhone.setVisibility(View.GONE);
            holder.tvAddress.setVisibility(View.GONE);
            holder.btnUpdate.setVisibility(View.GONE);
            try {

                Date orderDate = order.getOrderDate(); // Lấy trực tiếp Date, không cần parse!
                Date currentDate = new Date();
                if (orderDate != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    String formattedDate = sdf.format(orderDate);
                    holder.tvOrderDate.setText("Ngày đặt: " + formattedDate);
                    long diffInMillies = Math.abs(currentDate.getTime() - orderDate.getTime());
                    long diffInHours = diffInMillies / (60 * 60 * 1000); // Đổi sang giờ

                    // Chỉ HIỆN khi: (Chưa quá 24h) VÀ (Trạng thái khác "Đã hủy") VÀ (Trạng thái khác "Giao thành công")
                    if (diffInHours < 24
                            && !"Đã hủy".equalsIgnoreCase(order.getStatus())
                            && !"Giao thành công".equalsIgnoreCase(order.getStatus())) {

                        holder.btnCancelOrder.setVisibility(View.VISIBLE);
                    } else {
                        // Tất cả các trường hợp còn lại (Quá 24h, Đã hủy, Giao thành công) -> ẨN LUÔN
                        holder.tvOrderDate.setText("Ngày đặt: Đang cập nhật");
                        holder.btnCancelOrder.setVisibility(View.GONE);
                    }
                }
            } catch (Exception e) {
                holder.btnCancelOrder.setVisibility(View.GONE);
            }

            // 3. Xử lý khi bấm nút Hủy
            holder.btnCancelOrder.setOnClickListener(v -> {
                confirmCancelOrder(order.getOrderId(), position);
            });
        }
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvAddress, tvTotal, tvStatus, tvOrderId, tvOrderDate;
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
            btnUpdate = itemView.findViewById(R.id.btnUpdateStatus);
            btnCancelOrder = itemView.findViewById(R.id.btnCancelOrder);
        }
    }

    private void confirmCancelOrder(String orderId, int position) {
        new android.app.AlertDialog.Builder(context)
                .setTitle("Xác nhận hủy")
                .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này?")
                .setPositiveButton("Hủy đơn", (dialog, which) -> {

                    FirebaseFirestore.getInstance().collection("Orders").document(orderId)
                            .update("status", "Đã hủy")
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "Đã hủy đơn hàng!", Toast.LENGTH_SHORT).show();

                                // BƯỚC QUAN TRỌNG: Cập nhật lại status trong list dữ liệu tại máy
                                orderList.get(position).setStatus("Đã hủy");

                                // Lệnh này sẽ bắt hàm onBindViewHolder chạy lại cho dòng này
                                // Lúc này diffInHours < 24 vẫn đúng nhưng status đã là "Đã hủy" nên nút sẽ bị GONE
                                notifyItemChanged(position);
                            });
                })
                .setNegativeButton("Đóng", null)
                .show();
    }
}
