package com.example.app_store;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_store.Adapter.ProductAdapter;
import com.example.app_store.Model.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FavoriteActivity extends AppCompatActivity {

    private RecyclerView recyclerViewFavorites;
    private ProductAdapter productAdapter;
    private List<Product> favoriteList;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_favorite);

        btnBack = findViewById(R.id.btnBack);
        recyclerViewFavorites = findViewById(R.id.recyclerViewFavorites);

        // Chia 2 cột giống hệt trang chủ
        recyclerViewFavorites.setLayoutManager(new GridLayoutManager(this, 2));

        favoriteList = new ArrayList<>();
        productAdapter = new ProductAdapter(this, favoriteList); // Tái sử dụng ProductAdapter
        recyclerViewFavorites.setAdapter(productAdapter);

        // Nút quay lại
        btnBack.setOnClickListener(v -> finish());
        loadFavorites();
    }

    private void loadFavorites() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        // Thêm SnapshotListener vào đúng thư mục Yêu thích của người dùng này
        FirebaseFirestore.getInstance().collection("Favorites").document(userId).collection("Items")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return; // Bỏ qua nếu có lỗi
                    }

                    if (value != null) {
                        favoriteList.clear();

                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String id = doc.getString("productId");
                            String name = doc.getString("productName");
                            String price = doc.getString("price");
                            String imageUrl = doc.getString("imageUrl");

                            favoriteList.add(new Product(id, name, price, imageUrl));
                        }

                        productAdapter.notifyDataSetChanged();
                    }
                });
    }
}