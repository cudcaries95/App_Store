package com.example.app_store.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_store.Model.Order;
import com.example.app_store.R;

import java.util.List;

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
        }
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvAddress, tvTotal, tvStatus, tvOrderId, tvOrderDate;
        Button btnUpdate;

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
        }
    }
}
