package com.example.app_store;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.app_store.Model.CartItem;
import com.example.app_store.Model.Order;
import com.example.app_store.Model.ShippingAddress;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CheckoutActivity extends AppCompatActivity {

    // Thay đổi bộ Config cũ bằng bộ này
    private final String MOMO_ENDPOINT = "https://test-payment.momo.vn/v2/gateway/api/create";
    private final String PARTNER_CODE = "MOMOCU7R20200302"; // Partner Code Sandbox chuẩn
    private final String ACCESS_KEY = "E86QkiR9z6p0D3Y0";   // Access Key Sandbox chuẩn
    private final String SECRET_KEY = "8S3D15D7S5D15D7S5D15D7S5D15D7S5"; // Secret Key Sandbox chuẩn
    private final String REDIRECT_URL = "appstore://payment_result";
    private final String IPN_URL = "https://webhook.site/test";

    // --- Views ---
    private EditText edtCustomerName, edtPhone;
    private TextView tvTotalAmount, tvChangeAddress, tvReceiverInfo, tvCheckoutAddress;
    private Button btnPlaceOrder;
    private RadioGroup rgPaymentMethod;
    private RadioButton rbMomo, rbCOD;

    // --- Data ---
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<CartItem> cartItemList = new ArrayList<>();
    private String totalOrderAmount = "0 đ";
    private String amountRaw = "0";
    private ShippingAddress selectedAddress = null;
    private ActivityResultLauncher<Intent> addressBookLauncher;
    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_checkout);

        initViews();
        initFirebase();
        getDataFromIntent();
        setupAddressLauncher();

        // Lấy userId từ Intent
        String userId = getIntent().getStringExtra("USER_ID");

        // NẾU CÓ USER ID THÌ GỌI HÀM TẢI DỮ LIỆU
        if (userId != null) {
            loadCustomerData(userId);
        }

        btnPlaceOrder.setOnClickListener(v -> validateAndProcess());
    }

    private void initViews() {
        edtCustomerName = findViewById(R.id.edtCustomerName);
        edtPhone = findViewById(R.id.edtPhone);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);
        tvChangeAddress = findViewById(R.id.tvChangeAddress);
        tvReceiverInfo = findViewById(R.id.tvReceiverInfo);
        tvCheckoutAddress = findViewById(R.id.tvCheckoutAddress);
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        rbMomo = findViewById(R.id.rbMomo);
        rbCOD = findViewById(R.id.rbCOD);
    }

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void getDataFromIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            totalOrderAmount = intent.getStringExtra("TOTAL_PRICE");
            tvTotalAmount.setText("Tổng tiền: " + totalOrderAmount);

            // Lấy số nguyên để thanh toán
            if (totalOrderAmount != null) {
                amountRaw = totalOrderAmount.replaceAll("[^0-9]", "");
            }

            List<CartItem> receivedList = (List<CartItem>) intent.getSerializableExtra("CART_LIST");
            if (receivedList != null) {
                cartItemList.addAll(receivedList);
            }
        }
    }

    // Tại CheckoutActivity
    private void setupAddressLauncher() {
        addressBookLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // Nhận object địa chỉ mà người dùng vừa chọn từ danh sách
                        selectedAddress = (ShippingAddress) result.getData().getSerializableExtra("selected_address");
                        updateAddressUI(); // Hàm cập nhật TextView lên màn hình
                    }
                });

        // Sự kiện nhấn vào phần địa chỉ để thay đổi
        tvChangeAddress.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddAddressActivity.class);
            addressBookLauncher.launch(intent);
        });
    }

    private void validateAndProcess() {
        String name = edtCustomerName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String address = tvCheckoutAddress.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty() || address.equals("Vui lòng chọn địa chỉ")) {
            Toast.makeText(this, "Vui lòng hoàn thiện thông tin giao hàng!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Xác định phương thức thanh toán
        String paymentMethod = rbMomo.isChecked() ? "MOMO" : "COD";
        String orderId = "ORDER_" + System.currentTimeMillis();

        // Lưu đơn hàng vào Firestore
        saveOrderToFirestore(orderId, name, phone, address, paymentMethod);
    }

    private void saveOrderToFirestore(String orderId, String name, String phone, String address, String paymentMethod) {
        if (mAuth.getCurrentUser() == null) return;

        btnPlaceOrder.setEnabled(false);
        btnPlaceOrder.setText("Đang xử lý...");

        // Nếu COD thì trạng thái là PENDING_COD, nếu MOMO thì PENDING_MOMO
        String initialStatus = paymentMethod.equals("COD") ? "PLACED" : "PENDING_MOMO";

        Order newOrder = new Order(
                orderId,
                mAuth.getCurrentUser().getUid(),
                name,
                phone,
                address,
                totalOrderAmount,
                cartItemList,
                paymentMethod,
                initialStatus,
                new Date(),
                null
        );

        db.collection("Orders").document(orderId)
                .set(newOrder)
                .addOnSuccessListener(aVoid -> {
                    if (paymentMethod.equals("MOMO")) {
                        // Nếu chọn MoMo -> Gọi API
                        requestMoMoPaymentAPI(orderId);
                    } else {
                        // Nếu chọn COD -> Thành công luôn
                        clearUserCart();
                        Toast.makeText(this, "Đặt hàng thành công (COD)!", Toast.LENGTH_LONG).show();

                        Intent intent = new Intent(CheckoutActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    btnPlaceOrder.setEnabled(true);
                    btnPlaceOrder.setText("Đặt hàng");
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void requestMoMoPaymentAPI(String orderId) {
        String requestId = UUID.randomUUID().toString();
        String orderInfo = "Thanh toán đơn hàng " + orderId;
        String extraData = ""; // Có thể để trống nhưng phải có trong signature
        String requestType = "captureWallet";

        // 1. Tạo chuỗi ký theo đúng thứ tự Alphabet (BẮT BUỘC)
        String rawSignature = "accessKey=" + ACCESS_KEY +
                "&amount=" + amountRaw +
                "&extraData=" + extraData +
                "&ipnUrl=" + IPN_URL +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + PARTNER_CODE +
                "&redirectUrl=" + REDIRECT_URL +
                "&requestId=" + requestId +
                "&requestType=" + requestType;

        String signature = hmacSHA256(rawSignature, SECRET_KEY);

        // 2. Tạo JSON Body
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("partnerCode", PARTNER_CODE);
            jsonBody.put("partnerName", "App Store");
            jsonBody.put("storeId", "Main_Store");
            jsonBody.put("requestId", requestId);
            jsonBody.put("amount", Long.parseLong(amountRaw)); // MoMo nhận kiểu Number
            jsonBody.put("orderId", orderId);
            jsonBody.put("orderInfo", orderInfo);
            jsonBody.put("redirectUrl", REDIRECT_URL);
            jsonBody.put("ipnUrl", IPN_URL);
            jsonBody.put("lang", "vi");
            jsonBody.put("extraData", extraData);
            jsonBody.put("requestType", requestType);
            jsonBody.put("signature", signature);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3. Gửi Request
        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(MOMO_ENDPOINT)
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handleApiError("Lỗi kết nối server MoMo: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                try {
                    JSONObject jsonRes = new JSONObject(responseData);
                    // MoMo trả về kết quả qua payUrl
                    if (jsonRes.has("payUrl")) {
                        String payUrl = jsonRes.getString("payUrl");
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(payUrl));
                        startActivity(intent);
                    } else {
                        // Nếu lỗi cấu hình, MoMo sẽ trả về message giải thích
                        String errorMessage = jsonRes.optString("message", "Lỗi cấu hình thanh toán");
                        handleApiError(errorMessage);
                    }
                } catch (Exception e) {
                    handleApiError("Lỗi xử lý phản hồi từ MoMo");
                }
            }
        });
    }

    private void handleApiError(String message) {
        runOnUiThread(() -> {
            btnPlaceOrder.setEnabled(true);
            btnPlaceOrder.setText("Đặt hàng");
            Toast.makeText(CheckoutActivity.this, message, Toast.LENGTH_LONG).show();
        });
    }

    // --- Helper Functions ---

    private String hmacSHA256(String data, String key) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] rawHmac = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : rawHmac) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void loadCustomerData(String uid) {
        db.collection("Users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Lấy thông tin từ Profile User
                        String name = documentSnapshot.getString("name");
                        String phone = documentSnapshot.getString("phone");
                        String address = documentSnapshot.getString("address");

                        if (name != null) edtCustomerName.setText(name);
                        if (phone != null) edtPhone.setText(phone);

                        // Hiển thị địa chỉ mặc định từ profile nếu có
                        if (address != null && !address.isEmpty()) {
                            tvCheckoutAddress.setText(address);
                            tvReceiverInfo.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void updateAddressUI() {
        if (selectedAddress != null) {
            // Chỉ cập nhật trường địa chỉ vì bạn đã lược bỏ tên/SĐT ở phần thêm địa chỉ
            tvCheckoutAddress.setText(selectedAddress.getDetailedAddress());

            // Ẩn thông báo "Chưa có thông tin" nếu nó đang hiện
            tvReceiverInfo.setVisibility(View.GONE);

            // Tùy chọn: Nếu bạn muốn đổi màu chữ địa chỉ cho nổi bật sau khi chọn
            tvCheckoutAddress.setTextColor(getResources().getColor(android.R.color.black));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleMoMoResponse(intent);
    }

    private void handleMoMoResponse(Intent intent) {
        Uri data = intent.getData();
        if (data != null && data.getScheme().equals("appstore")) {
            // MoMo trả về kết quả qua URL query params
            String resultCode = data.getQueryParameter("resultCode");
            String orderId = data.getQueryParameter("orderId");

            if ("0".equals(resultCode)) {
                // Thanh toán thành công
                updateOrderStatus(orderId, "PAID");
            } else {
                // Thanh toán thất bại hoặc người dùng hủy
                updateOrderStatus(orderId, "FAILED");
                Toast.makeText(this, "Thanh toán không thành công", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateOrderStatus(String orderId, String status) {
        db.collection("Orders").document(orderId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    if (status.equals("PAID")) {
                        clearUserCart();
                        Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_LONG).show();
                        // Chuyển về màn hình chính
                        Intent intent = new Intent(CheckoutActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    private void clearUserCart() {
        String uid = mAuth.getCurrentUser().getUid();
        // Truy cập vào sub-collection Items của user và xóa sạch
        db.collection("Cart").document(uid).collection("Items")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete();
                    }
                });
    }
}