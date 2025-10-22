package com.example.mock;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PaymentMethodsActivity extends AppCompatActivity {

    private static final String TAG = "PaymentMethods";
    private static final String GET_PAYMENT_INFO_URL = "http://192.168.1.3/boardease_v3/get_payment_info.php";

    // Views
    private ImageView btnBack;
    private ImageView ivQrCode;
    private TextView tvGcashNumber;
    private ImageButton btnToggleGcash;
    private ProgressDialog progressDialog;
    private RequestQueue requestQueue;

    // Data
    private String gcashNumber;
    private String qrCodePath;
    private boolean isGcashVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_methods);

        // Initialize views
        initializeViews();
        
        // Initialize Volley RequestQueue
        requestQueue = Volley.newRequestQueue(this);
        
        // Set click listeners
        setupClickListeners();
        
        // Load payment information
        loadPaymentInfo();
    }
    
    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        ivQrCode = findViewById(R.id.ivQrCode);
        tvGcashNumber = findViewById(R.id.tvGcashNumber);
        btnToggleGcash = findViewById(R.id.btnToggleGcash);
        
        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading payment information...");
        progressDialog.setCancelable(false);
    }
    
    private void setupClickListeners() {
        // Back button
        btnBack.setOnClickListener(v -> {
            finish();
        });
        
        // GCash number toggle
        btnToggleGcash.setOnClickListener(v -> {
            toggleGcashVisibility();
        });
    }
    
    private void loadPaymentInfo() {
        showProgressDialog();
        
        // Get user ID from Login session
        String userId = Login.getCurrentUserId(this);
        if (userId == null) {
            hideProgressDialog();
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        StringRequest request = new StringRequest(Request.Method.POST, GET_PAYMENT_INFO_URL,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    hideProgressDialog();
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            populatePaymentData(jsonResponse);
                        } else {
                            String error = jsonResponse.optString("message", "Failed to load payment info");
                            Toast.makeText(PaymentMethodsActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing payment info response", e);
                        Toast.makeText(PaymentMethodsActivity.this, "Error loading payment info", Toast.LENGTH_SHORT).show();
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    hideProgressDialog();
                    Log.e(TAG, "Error loading payment info", error);
                    Toast.makeText(PaymentMethodsActivity.this, "Error loading payment info", Toast.LENGTH_SHORT).show();
                }
            }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", userId);
                return params;
            }
        };

        requestQueue.add(request);
    }
    
    private void populatePaymentData(JSONObject paymentData) {
        try {
            // Get GCash number
            gcashNumber = paymentData.optString("gcash_number", "");
            if (!gcashNumber.isEmpty()) {
                tvGcashNumber.setText(gcashNumber);
            } else {
                tvGcashNumber.setText("No GCash number available");
            }
            
            // Get QR code path
            qrCodePath = paymentData.optString("qr_code_path", "");
            if (!qrCodePath.isEmpty()) {
                loadQrCodeImage(qrCodePath);
            } else {
                ivQrCode.setImageResource(R.drawable.placeholder);
                Toast.makeText(this, "No QR code available", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error populating payment data", e);
            Toast.makeText(this, "Error displaying payment information", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadQrCodeImage(String imagePath) {
        try {
            // If it's a base64 encoded image
            if (imagePath.startsWith("data:image") || imagePath.startsWith("/9j/")) {
                byte[] decodedBytes = Base64.decode(imagePath, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                ivQrCode.setImageBitmap(bitmap);
            } else {
                // If it's a URL or file path, use Glide
                Glide.with(this)
                    .load(imagePath)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .into(ivQrCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading QR code image", e);
            ivQrCode.setImageResource(R.drawable.placeholder);
        }
    }
    
    private void toggleGcashVisibility() {
        try {
            if (isGcashVisible) {
                // Hide GCash number
                tvGcashNumber.setTransformationMethod(PasswordTransformationMethod.getInstance());
                btnToggleGcash.setImageResource(R.drawable.ic_eye_closed);
                isGcashVisible = false;
            } else {
                // Show GCash number
                tvGcashNumber.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                btnToggleGcash.setImageResource(R.drawable.ic_eye_opened);
                isGcashVisible = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling GCash visibility", e);
        }
    }
    
    private void showProgressDialog() {
        if (progressDialog != null && !progressDialog.isShowing()) {
            progressDialog.show();
        }
    }
    
    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
