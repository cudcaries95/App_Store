package com.example.app_store;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.app_store.Adapter.ReviewAdapter;
import com.example.app_store.Model.Review;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProductDetailActivity extends AppCompatActivity {

    private ImageView imgDetailProduct, btnBack, btnFavorite, imgArrowDesc;
    private TextView tvDetailName, tvDetailPrice, btnWriteReview, tvDetailDescription;
    private Button btnAddCart;

    private RecyclerView rvReviews;
    private ReviewAdapter reviewAdapter;
    private List<Review> reviewList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_product_detail);

        // Ánh xạ View
        imgDetailProduct = findViewById(R.id.imgDetailProduct);
        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailPrice = findViewById(R.id.tvDetailPrice);
        btnAddCart = findViewById(R.id.btnAddCart);
        btnBack = findViewById(R.id.btnBack);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnWriteReview = findViewById(R.id.btnWriteReview);
        // Cài đặt danh sách Đánh giá
        rvReviews = findViewById(R.id.rvReviews);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));

        // Ánh xạ các thành phần Đóng/Mở
        LinearLayout layoutHeaderDesc = findViewById(R.id.layoutHeaderDesc);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        imgArrowDesc = findViewById(R.id.imgArrowDesc);

        LinearLayout layoutHeaderReview = findViewById(R.id.layoutHeaderReview);
        LinearLayout layoutContentReview = findViewById(R.id.layoutContentReview);
        ImageView imgArrowReview = findViewById(R.id.imgArrowReview);

        reviewList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(this, reviewList);
        rvReviews.setAdapter(reviewAdapter);

        // 1. Khởi tạo Firestore và Auth
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Nhận gói dữ liệu từ Intent gửi qua
        String productId = getIntent().getStringExtra("id");
        String productName = getIntent().getStringExtra("name");
        String productPrice = getIntent().getStringExtra("price");
        String productImageUrl = getIntent().getStringExtra("imageUrl");

        // Đổ dữ liệu lên giao diện
        if (productName != null) {
            tvDetailName.setText(productName);
            tvDetailPrice.setText(productPrice);

            // Dùng Glide để tải ảnh chi tiết
            Glide.with(this)
                    .load(productImageUrl)
                    .into(imgDetailProduct);
        }

        // Bắt sự kiện nút Thêm vào giỏ hàng
        btnAddCart.setOnClickListener(v -> {
            // Kiểm tra xem người dùng đã đăng nhập chưa (đề phòng lỗi văng app)
            if (auth.getCurrentUser() != null) {
                String currentUserId = auth.getCurrentUser().getUid();

                // 1. Trỏ chính xác vào Document mang tên là productId
                DocumentReference cartRef = firestore.collection("Cart")
                        .document(currentUserId)
                        .collection("Items")
                        .document(productId); // Ép tên Document phải là ID sản phẩm

                // 2. Kiểm tra xem món hàng này đã có trong giỏ chưa
                cartRef.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();

                        if (document.exists()) {
                            // TRƯỜNG HỢP 1: SẢN PHẨM ĐÃ CÓ TRONG GIỎ -> CHỈ TĂNG SỐ LƯỢNG

                            // Lấy số lượng hiện tại trên mạng xuống
                            Long currentQuantity = document.getLong("quantity");
                            int newQuantity = (currentQuantity != null ? currentQuantity.intValue() : 0) + 1;

                            // Cập nhật lại số lượng mới
                            cartRef.update("quantity", newQuantity)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(ProductDetailActivity.this, "Đã tăng số lượng " + productName + " lên " + newQuantity, Toast.LENGTH_SHORT).show();
                                        finish();
                                    });

                        } else {
                            // TRƯỜNG HỢP 2: SẢN PHẨM CHƯA CÓ TRONG GIỎ -> TẠO MỚI VỚI SỐ LƯỢNG LÀ 1

                            HashMap<String, Object> cartMap = new HashMap<>();
                            cartMap.put("productId", productId);
                            cartMap.put("productName", productName);
                            cartMap.put("productPrice", productPrice);
                            cartMap.put("productImageUrl", productImageUrl);
                            cartMap.put("quantity", 1); // Lần đầu tiên thêm vào thì số lượng là 1

                            // Dùng set() thay vì add() để ghi vào đúng Document mang tên productId
                            cartRef.set(cartMap)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(ProductDetailActivity.this, "Đã thêm mới " + productName + " vào giỏ hàng", Toast.LENGTH_SHORT).show();
                                        finish();
                                    });
                        }
                    } else {
                        Toast.makeText(ProductDetailActivity.this, "Lỗi kết nối máy chủ!", Toast.LENGTH_SHORT).show();
                    }
                });

            } else {
                Toast.makeText(ProductDetailActivity.this, "Bạn cần đăng nhập để mua hàng!", Toast.LENGTH_SHORT).show();
            }
        });

        // Bắt sự kiện quay lại
        btnBack.setOnClickListener(v -> {
            finish(); // Đóng ProductDetailActivity, tự động quay về MainActivity
        });

        // Kiểm tra trạng thái Yêu thích khi vừa mở màn hình
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            // Truy cập thẳng vào Document có tên là productId
            DocumentReference favRef = db.collection("Favorites").document(userId).collection("Items").document(productId);

            // Kiểm tra xem đã thả tim chưa
            favRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    btnFavorite.setImageResource(R.drawable.ic_heart_filled); // Đã thích -> Tim đặc
                    btnFavorite.setColorFilter(Color.parseColor("#E53935"));
                } else {
                    btnFavorite.setImageResource(R.drawable.ic_heart_outline); // Chưa thích -> Tim rỗng
                    btnFavorite.setColorFilter(Color.parseColor("#758A7E"));
                }
            });

            // Xử lý sự kiện khi bấm vào nút Tim
            btnFavorite.setOnClickListener(v -> {
                favRef.get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Nếu đang thích -> Bấm vào sẽ Hủy thích (Xóa khỏi Firebase)
                        favRef.delete();
                        btnFavorite.setImageResource(R.drawable.ic_heart_outline);
                        btnFavorite.setColorFilter(Color.parseColor("#758A7E")); // Trả về màu xám
                        Toast.makeText(this, "Đã bỏ yêu thích", Toast.LENGTH_SHORT).show();
                    } else {
                        // Nếu chưa thích -> Bấm vào sẽ Thêm yêu thích
                        HashMap<String, Object> favMap = new HashMap<>();
                        favMap.put("productId", productId);
                        favMap.put("productName", productName);
                        favMap.put("price", productPrice);
                        favMap.put("imageUrl", productImageUrl);

                        favRef.set(favMap); // Dùng set() thay vì add() để ép tên Document phải là productId
                        btnFavorite.setImageResource(R.drawable.ic_heart_filled);
                        btnFavorite.setColorFilter(Color.parseColor("#E53935"));
                        Toast.makeText(this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        btnWriteReview.setOnClickListener(v -> {
            if (auth.getCurrentUser() == null) {
                Toast.makeText(this, "Vui lòng đăng nhập để đánh giá!", Toast.LENGTH_SHORT).show();
                return;
            }
            // Truyền 3 biến này sang hàm showReviewDialog
            showReviewDialog(productId, auth, db);
        });

        // Bắt sự kiện click khối Mô tả
        layoutHeaderDesc.setOnClickListener(v -> {
            if (tvDetailDescription.getVisibility() == View.GONE) {
                // Đang ẩn -> Hiện ra và xoay mũi tên lên 180 độ
                tvDetailDescription.setVisibility(View.VISIBLE);
                imgArrowDesc.animate().rotation(180f).setDuration(200).start();
            } else {
                // Đang hiện -> Ẩn đi và xoay mũi tên về vị trí cũ (0 độ)
                tvDetailDescription.setVisibility(View.GONE);
                imgArrowDesc.animate().rotation(0f).setDuration(200).start();
            }
        });

        // Bắt sự kiện click khối Đánh giá
        layoutHeaderReview.setOnClickListener(v -> {
            if (layoutContentReview.getVisibility() == View.GONE) {
                layoutContentReview.setVisibility(View.VISIBLE);
                imgArrowReview.animate().rotation(180f).setDuration(200).start();
            } else {
                layoutContentReview.setVisibility(View.GONE);
                imgArrowReview.animate().rotation(0f).setDuration(200).start();
            }
        });

        // Gọi hàm tải đánh giá
        loadReviews(db, productId);
    }

    private void loadReviews(com.google.firebase.firestore.FirebaseFirestore db, String productId) {
        db.collection("Products").document(productId).collection("Reviews")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null) {
                        reviewList.clear();

                        // 3. Dùng value.getDocuments() để vòng lặp chạy mượt nhất
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            String name = doc.getString("userName");

                            // Lấy số sao (đề phòng khách không vuốt sao bị null thì cho mặc định 5 sao)
                            float rating = doc.getDouble("rating") != null ? doc.getDouble("rating").floatValue() : 5f;
                            String comment = doc.getString("comment");

                            reviewList.add(new Review(name, rating, comment));
                        }

                        reviewAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void showReviewDialog(String productId, FirebaseAuth auth, FirebaseFirestore db) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_review, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        RatingBar ratingBarInput = view.findViewById(R.id.ratingBarInput);
        EditText edtReviewComment = view.findViewById(R.id.edtReviewComment);
        Button btnSubmitReview = view.findViewById(R.id.btnSubmitReview);

        btnSubmitReview.setOnClickListener(v -> {
            float rating = ratingBarInput.getRating();
            String comment = edtReviewComment.getText().toString().trim();

            if (comment.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập bình luận!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Lấy tên người dùng hiện tại từ bảng Users để làm tên người bình luận
            String userId = auth.getCurrentUser().getUid();
            db.collection("Users").document(userId).get().addOnSuccessListener(doc -> {
                String userName = doc.contains("name") ? doc.getString("name") : "Khách hàng ẩn danh";

                // Lưu vào sub-collection "Reviews" nằm bên trong Sản phẩm này
                HashMap<String, Object> reviewMap = new HashMap<>();
                reviewMap.put("userName", userName);
                reviewMap.put("rating", rating);
                reviewMap.put("comment", comment);
                reviewMap.put("timestamp", FieldValue.serverTimestamp());

                db.collection("Products").document(productId).collection("Reviews")
                        .add(reviewMap)
                        .addOnSuccessListener(docRef -> {
                            Toast.makeText(this, "Cảm ơn bạn đã đánh giá!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        });
            });
        });

        // Bắt sự kiện khi khách hàng vuốt chọn sao
        ratingBarInput.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (rating == 5) {
                Toast.makeText(this, "Tuyệt vời quá!", Toast.LENGTH_SHORT).show();
            } else if (rating <= 2) {
                Toast.makeText(this, "Rất tiếc vì trải nghiệm chưa tốt!", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}