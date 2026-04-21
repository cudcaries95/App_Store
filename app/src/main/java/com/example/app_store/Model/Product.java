package com.example.app_store.Model;

public class Product {
    private String id;
    private String name;
    private String price; // Tạm dùng String cho dễ hiển thị
    private String imageUrl;

    public Product(String id, String name, String price, String imageUrl) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
    }

    // Nhấn Alt + Insert để tự động tạo các hàm Getter và Setter
    public String getId() { return id; }
    public String getName() { return name; }
    public String getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
}
