package com.example.app_store;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.app_store.Adapter.ProductAdapter;
import com.example.app_store.Model.Product;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvHotProducts, rvSaleProducts, recyclerViewProducts, rvSearchResults;
    private ProductAdapter hotAdapter, saleAdapter, allAdapter, searchAdapter;
    private List<Product> hotList, saleList, allList, searchList;
    private FirebaseFirestore db; // Khai báo Firestore
    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private ImageView btnMenu, btnCartIcon, imgNavAvatar;
    private LinearLayout layoutDefaultHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
        btnMenu = findViewById(R.id.btnMenu);
        btnCartIcon = findViewById(R.id.btnCartIcon);
        rvHotProducts = findViewById(R.id.rvHotProducts);
        rvSaleProducts = findViewById(R.id.rvSaleProducts);
        recyclerViewProducts = findViewById(R.id.recyclerViewProducts);
        layoutDefaultHome = findViewById(R.id.layoutDefaultHome);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        SearchView searchView = findViewById(R.id.searchView);

        // 1. Khởi tạo Firestore và Auth
        db = FirebaseFirestore.getInstance();

        // Setup cuộn ngang (Horizontal) cho Hot và Sale
        rvHotProducts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSaleProducts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));


        // Setup chia 2 cột (Grid) cho Tất cả sản phẩm (tránh bị lỗi cuộn thì tắt cuộn độc lập)
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        rvSearchResults.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerViewProducts.setLayoutManager(gridLayoutManager);
        recyclerViewProducts.setNestedScrollingEnabled(false);

        // Khởi tạo 3 danh sách và 3 Adapter (tái sử dụng ProductAdapter cực kỳ tiện lợi)
        hotList = new ArrayList<>();
        saleList = new ArrayList<>();
        allList = new ArrayList<>();
        searchList = new ArrayList<>();

        hotAdapter = new ProductAdapter(this, hotList);
        saleAdapter = new ProductAdapter(this, saleList);
        allAdapter = new ProductAdapter(this, allList);
        searchAdapter = new ProductAdapter(this, searchList);

        rvHotProducts.setAdapter(hotAdapter);
        rvSaleProducts.setAdapter(saleAdapter);
        recyclerViewProducts.setAdapter(allAdapter);
        rvSearchResults.setAdapter(searchAdapter);

        // --- ĐOẠN CODE KIỂM TRA QUYỀN ADMIN ---
        Menu navMenu = navView.getMenu(); // Lấy danh sách các nút trong Menu

        // 1. Vừa vào là giấu tịt đi trước cho an toàn
        navMenu.findItem(R.id.nav_admin_orders).setVisible(false);
        navMenu.findItem(R.id.nav_admin_statistics).setVisible(false);
        navMenu.findItem(R.id.nav_admin_statistics).setVisible(false);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();

            db.collection("Users").document(currentUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String role = documentSnapshot.getString("role");
                            // THÊM DÒNG NÀY ĐỂ KIỂM TRA TRÊN LOGCAT
                            android.util.Log.d("CHECK_ROLE", "Role hiện tại là: " + role);

                            if (role != null && role.equalsIgnoreCase("admin")) {
                                // 1. NẾU LÀ ADMIN: Hiện các nút Quản lý
                                navMenu.findItem(R.id.nav_admin_statistics).setVisible(true);
                                navMenu.findItem(R.id.nav_admin_products).setVisible(true);
                                navMenu.findItem(R.id.nav_admin_orders).setVisible(true);

                                // Đồng thời ẨN các nút của khách hàng bình thường
                                navMenu.findItem(R.id.nav_profile).setVisible(false);
                                navMenu.findItem(R.id.nav_cart).setVisible(false);
                                navMenu.findItem(R.id.nav_favorite).setVisible(false);
                                navMenu.findItem(R.id.nav_history).setVisible(false);

                                // Ẩn luôn Icon Giỏ hàng trên thanh Top Bar ở góc phải
                                btnCartIcon.setVisibility(View.GONE);

                            } else {
                                // 2. NẾU LÀ KHÁCH HÀNG: Đảm bảo ẩn các nút Admin (đề phòng lỗi hiển thị)
                                navMenu.findItem(R.id.nav_admin_statistics).setVisible(false);
                                navMenu.findItem(R.id.nav_admin_products).setVisible(false);
                                navMenu.findItem(R.id.nav_admin_orders).setVisible(false);

                                // Đảm bảo các nút khách hàng và icon giỏ hàng vẫn hiện
                                navMenu.findItem(R.id.nav_profile).setVisible(true);
                                navMenu.findItem(R.id.nav_cart).setVisible(true);
                                navMenu.findItem(R.id.nav_favorite).setVisible(true);
                                navMenu.findItem(R.id.nav_history).setVisible(true);

                                btnCartIcon.setVisibility(View.VISIBLE);
                                android.util.Log.d("CHECK_ROLE", "Không phải admin hoặc role null");
                            }
                        } else {
                            android.util.Log.d("CHECK_ROLE", "Document User không tồn tại!");
                        }
                    }).addOnFailureListener(e -> {
                        android.util.Log.e("CHECK_ROLE", "Lỗi truy vấn Firestore: " + e.getMessage());
                    });
        }

        // 2. Xử lý sự kiện: Bấm icon Giỏ hàng góc phải -> Mở màn hình Giỏ hàng
        btnCartIcon.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CartActivity.class));
        });

        // 3. Xử lý sự kiện: Bấm icon Menu góc trái -> Mở thanh ngăn kéo trượt ra
        btnMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.START);
        });

        // 4. Xử lý sự kiện: Bấm vào các mục trong Menu trượt
        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    // Đang ở trang chủ rồi nên chỉ cần đóng menu lại
                    drawerLayout.closeDrawer(GravityCompat.START);

                } else if (id == R.id.nav_profile) {
                    Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    drawerLayout.closeDrawer(GravityCompat.START);

                } else if (id == R.id.nav_cart) {
                    // Mở giỏ hàng và đóng menu
                    Intent intent = new Intent(MainActivity.this, CartActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    drawerLayout.closeDrawer(GravityCompat.START);

                } else if (id == R.id.nav_history) {
                    // Mở Lịch sử đơn hàng
                    Intent intent = new Intent(MainActivity.this, OrderHistoryActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    drawerLayout.closeDrawer(GravityCompat.START);

                } else if (id == R.id.nav_favorite) {
                    Intent intent = new Intent(MainActivity.this, FavoriteActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    drawerLayout.closeDrawer(GravityCompat.START);

                } else if (id == R.id.nav_admin_statistics) {
                    // Mở trang Quản lý sản phẩm
                    Toast.makeText(MainActivity.this, "Mở Thống kê", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, AdminStatisticsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    drawerLayout.closeDrawer(GravityCompat.START);

                } else if (id == R.id.nav_admin_products) {
                    // Mở trang Quản lý sản phẩm
                    Toast.makeText(MainActivity.this, "Mở Quản lý Sản phẩm", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, ManageProductsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    drawerLayout.closeDrawer(GravityCompat.START);

                } else if (id == R.id.nav_admin_orders) {
                    // Mở trang Quản lý đơn hàng
                    Toast.makeText(MainActivity.this, "Mở Quản lý Đơn hàng", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, ManageOrdersActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    drawerLayout.closeDrawer(GravityCompat.START);

                } else if (id == R.id.nav_logout) {
                    // Đăng xuất Firebase và quay về màn hình Login
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    finish();
                }
                return true;
            }
        });

        loadNavHeaderData();

        // Gọi hàm tải dữ liệu
        loadProductsFromFirestore();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    // 1. NẾU XÓA TRẮNG THANH TÌM KIẾM -> Hiện lại trang chủ, Ẩn list tìm kiếm
                    layoutDefaultHome.setVisibility(View.VISIBLE);
                    rvSearchResults.setVisibility(View.GONE);
                } else {
                    // 2. NẾU ĐANG GÕ CHỮ -> Giấu trang chủ đi, Hiện list tìm kiếm lên
                    layoutDefaultHome.setVisibility(View.GONE);
                    rvSearchResults.setVisibility(View.VISIBLE);

                    // Chạy thuật toán lọc (Filter) tại máy
                    filterProducts(newText);
                }
                return false;
            }
        });
    }

    private void loadProductsFromFirestore() {
        db.collection("Products")
                .addSnapshotListener(this, (value, error) -> {
                    if (error != null) return;

                    if (value != null) {
                        // Xóa sạch cả 3 danh sách cũ
                        hotList.clear();
                        saleList.clear();
                        allList.clear();

                        for (DocumentSnapshot document : value) {
                            String id = document.getId();
                            String name = document.getString("name") != null ? document.getString("name") : "Không tên";
                            String price = document.getString("price");
                            String imageUrl = document.getString("imageUrl");

                            // LẤY THÊM TRƯỜNG CATEGORY ĐỂ LỌC
                            String category = document.getString("category");

                            Product product = new Product(id, name, price, imageUrl);

                            // Món nào cũng cho vào mục Tất cả
                            allList.add(product);

                            // Lọc riêng vào mục Hot và Sale
                            if ("hot".equals(category)) {
                                hotList.add(product);
                            } else if ("sale".equals(category)) {
                                saleList.add(product);
                            }
                        }

                        // Cập nhật lại giao diện cho cả 3
                        hotAdapter.notifyDataSetChanged();
                        saleAdapter.notifyDataSetChanged();
                        allAdapter.notifyDataSetChanged();
                    }
                });
    }

    // HÀM TÍNH TOÁN VÀ ĐỔ DỮ LIỆU LÊN HEADER MENU
    private void loadNavHeaderData() {
        // BẮT BUỘC: Lấy phần đầu của Menu ra trước
        View headerView = navView.getHeaderView(0);

        // Ánh xạ các TextView nằm BÊN TRONG cái headerView đó
        imgNavAvatar = headerView.findViewById(R.id.imgNavAvatar);
        TextView tvNavName = headerView.findViewById(R.id.tvNavName);
        TextView tvNavRank = headerView.findViewById(R.id.tvNavRank);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. KÉO TÊN HIỂN THỊ
        db.collection("Users").document(userId).addSnapshotListener(this, (documentSnapshot, error) -> {
            if (error != null) {
                android.util.Log.e("NAV_ERROR", "Lỗi tải dữ liệu real-time", error);
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                String name = documentSnapshot.getString("name");
                String base64Image = documentSnapshot.getString("avatarBase64");
                String role = documentSnapshot.getString("role");

                if (role != null && role.equalsIgnoreCase("admin")) {
                    // NẾU LÀ ADMIN: Gắn mác Quản Trị Viên (Hoặc dùng lệnh tvNavRank.setVisibility(View.GONE); để giấu tịt luôn)
                    tvNavRank.setText("👑 QUẢN TRỊ VIÊN");
                    tvNavRank.setTextColor(android.graphics.Color.parseColor("#FF5252")); // Màu Đỏ quyền lực
                    tvNavRank.setVisibility(android.view.View.VISIBLE);
                    tvNavName.setText(name);

                    // Tuyệt đối không gọi lệnh đếm đơn hàng ở đây để tiết kiệm Database
                } else {
                    // NẾU LÀ KHÁCH HÀNG: Kích hoạt hàm đếm tiền và xét hạng VIP
                    tvNavRank.setVisibility(android.view.View.VISIBLE);
                    tvNavName.setText(name);
                    calculateRankForCustomer(userId, tvNavRank);
                }

                if (base64Image != null && !base64Image.isEmpty()) {
                    // Giải mã Base64
                    byte[] decodedString = Base64.decode(base64Image, Base64.NO_WRAP);

                    // Đổ vào ImageView bằng Glide
                    Glide.with(this)
                            .asBitmap()
                            .load(decodedString)
                            .placeholder(R.drawable.ic_person) // Ảnh hiện trong lúc chờ
                            .error(R.drawable.ic_error)            // Ảnh hiện nếu lỗi
                            .circleCrop()
                            .into(imgNavAvatar);
                }
            }
        });
    }

    // 2. KÉO ĐƠN HÀNG VÀ TÍNH HẠNG
    private void calculateRankForCustomer(String userId, TextView tvNavRank) {
        FirebaseFirestore.getInstance().collection("Orders")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "Giao thành công")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    long totalSpent = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String priceString = doc.getString("totalAmount");
                        if (priceString != null) {
                            String rawNumber = priceString.replaceAll("[^0-9]", "");
                            if (!rawNumber.isEmpty()) totalSpent += Long.parseLong(rawNumber);
                        }
                    }

                    // HIỂN THỊ HẠNG VIP CHO KHÁCH
                    if (totalSpent >= 10000000) {
                        tvNavRank.setText("💎 KIM CƯƠNG");
                        tvNavRank.setTextColor(android.graphics.Color.parseColor("#64B5F6"));
                    } else if (totalSpent >= 5000000) {
                        tvNavRank.setText("⭐ VÀNG VIP");
                        tvNavRank.setTextColor(android.graphics.Color.parseColor("#FFD54F"));
                    } else if (totalSpent >= 2000000) {
                        tvNavRank.setText("🥈 BẠC TÍN");
                        tvNavRank.setTextColor(android.graphics.Color.parseColor("#E0E0E0"));
                    } else {
                        tvNavRank.setText("🥉 THÀNH VIÊN");
                        tvNavRank.setTextColor(android.graphics.Color.parseColor("#BCAAA4"));
                    }
                });
    }

    // --- HÀM LỌC SẢN PHẨM ---
    private void filterProducts(String keyword) {
        searchList.clear(); // Xóa kết quả cũ

        // Chuyển từ khóa về chữ thường để không phân biệt hoa/thường
        String lowerCaseKeyword = keyword.toLowerCase().trim();

        // Quét toàn bộ sản phẩm trong allList (allList đã được tải ở hàm loadAllProducts)
        for (Product p : allList) {
            if (p.getName() != null && p.getName().toLowerCase().contains(lowerCaseKeyword)) {
                searchList.add(p); // Nếu tên có chứa từ khóa thì nhét vào danh sách tìm kiếm
            }
        }

        // Báo cho Adapter biết để vẽ lại màn hình
        searchAdapter.notifyDataSetChanged();
    }
}