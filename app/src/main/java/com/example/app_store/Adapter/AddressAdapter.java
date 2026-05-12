package com.example.app_store.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_store.Model.ShippingAddress;
import com.example.app_store.R;

import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.AddressViewHolder>{

    private List<ShippingAddress> addressList;
    private OnAddressClickListener listener;

    // Interface để truyền sự kiện click ra ngoài Activity
    public interface OnAddressClickListener {
        void onAddressClick(ShippingAddress address);
    }

    public AddressAdapter(List<ShippingAddress> addressList, OnAddressClickListener listener) {
        this.addressList = addressList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_address, parent, false);
        return new AddressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        ShippingAddress address = addressList.get(position);

        holder.tvName.setText(address.getReceiverName());
        holder.tvPhone.setText(address.getPhone());
        holder.tvAddress.setText(address.getDetailedAddress());

        // Hiện chữ [Mặc định] nếu isDefault = true
        if (address.isDefault()) {
            holder.tvDefaultBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvDefaultBadge.setVisibility(View.GONE);
        }

        // Bắt sự kiện click vào toàn bộ item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddressClick(address);
            }
        });
    }

    @Override
    public int getItemCount() {
        return addressList != null ? addressList.size() : 0;
    }

    public static class AddressViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvAddress, tvDefaultBadge;

        public AddressViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvPhone = itemView.findViewById(R.id.tvItemPhone);
            tvAddress = itemView.findViewById(R.id.tvItemAddress);
            tvDefaultBadge = itemView.findViewById(R.id.tvItemDefaultBadge);
        }
    }

}
