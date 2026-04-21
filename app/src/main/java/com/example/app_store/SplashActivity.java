package com.example.app_store;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Tạo bộ đếm ngược 3 giây (3000 milliseconds)
        new Handler().postDelayed(() -> {

            // Lấy thông tin đăng nhập từ Firebase
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser != null) {
                // Nếu đã đăng nhập trước đó -> Vào thẳng Trang Chủ
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            } else {
                // Nếu chưa đăng nhập -> Mở trang Đăng Nhập
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }

            // Đóng SplashActivity lại để người dùng ấn nút Back không bị quay ngược lại màn hình chờ
            finish();

        }, 3000);
    }
}