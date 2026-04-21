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
import com.example.app_store.Model.Product;
import com.example.app_store.R;

import java.util.List;

public class AdminProductAdapter extends RecyclerView.Adapter<AdminProductAdapter.AdminViewHolder> {

    private Context context;
    private List<Product> productList;
    private OnAdminProductListener listener;

    public interface OnAdminProductListener {
        void onDeleteClick(Product product, int position);
        void onEditClick(Product product);
    }

    public AdminProductAdapter(Context context, List<Product> productList, OnAdminProductListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_admin, parent, false);
        return new AdminViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.tvName.setText(product.getName());
        holder.tvPrice.setText(product.getPrice());

        Glide.with(context).load(product.getImageUrl()).into(holder.imgProduct);

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(product, position);
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(product);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class AdminViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct, btnEdit, btnDelete;
        TextView tvName, tvPrice;

        public AdminViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgAdminProduct);
            btnEdit = itemView.findViewById(R.id.btnEditProduct);
            btnDelete = itemView.findViewById(R.id.btnDeleteProduct);
            tvName = itemView.findViewById(R.id.tvAdminProductName);
            tvPrice = itemView.findViewById(R.id.tvAdminProductPrice);
        }
    }
}
