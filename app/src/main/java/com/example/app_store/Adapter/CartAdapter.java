package com.example.app_store.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.app_store.Model.CartItem;
import com.example.app_store.R;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder>{

    private Context context;
    private List<CartItem> cartItemList;
    private OnCartItemDeleteListener deleteListener;

    // 1. Khai báo Interface (Bộ đàm)
    public interface OnCartItemDeleteListener {
        void onDeleteClick(CartItem item, int position);
    }

    public CartAdapter(Context context, List<CartItem> cartItemList, OnCartItemDeleteListener deleteListener) {
        this.context = context;
        this.cartItemList = cartItemList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem cartItem = cartItemList.get(position);

        holder.tvCartName.setText(cartItem.getProductName());
        holder.tvCartPrice.setText(cartItem.getProductPrice());
        holder.tvCartQuantity.setText("Số lượng: " + cartItem.getQuantity());

        // Load ảnh bằng Glide
        Glide.with(context)
                .load(cartItem.getProductImageUrl())
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imgCartProduct);

        // 3. Bắt sự kiện khi bấm nút Thùng rác
        holder.btnDeleteCart.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(cartItem, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItemList.size();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCartProduct, btnDeleteCart;
        TextView tvCartName, tvCartPrice, tvCartQuantity;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCartProduct = itemView.findViewById(R.id.imgCartProduct);
            tvCartName = itemView.findViewById(R.id.tvCartName);
            tvCartPrice = itemView.findViewById(R.id.tvCartPrice);
            tvCartQuantity = itemView.findViewById(R.id.tvCartQuantity);
            btnDeleteCart = itemView.findViewById(R.id.btnDeleteCart);
        }
    }
}
