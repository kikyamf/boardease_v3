package com.example.mock;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import com.google.android.material.button.MaterialButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FinalBookingActivity extends AppCompatActivity {
    
    private static final String TAG = "FinalBookingActivity";
    // Local development URL - Update this to match your local IP
    private static final String BASE_URL = "http://192.168.1.9/boardease_v3/";
    private static final String BOARD_EASE2_URL = BASE_URL + "BoardEase2/";
    private static final String GET_BH_DETAILS_URL = BASE_URL + "get_boarding_house_details.php";
    private static final String GET_GCASH_INFO_URL = BOARD_EASE2_URL + "get_gcash_info.php";
    private static final String CREATE_BOOKING_URL = BOARD_EASE2_URL + "create_booking.php";
    private static final int PICK_IMAGE_REQUEST = 100;
    
    // Views
    private ImageButton btnBack;
    private ImageView ivBhImage, ivCashProof, ivGcashProof, ivOwnerQrCode;
    private TextView tvBhName, tvRoomType, tvDuration, tvPrice;
    private RadioGroup rgPaymentMethod;
    private RadioButton rbCash, rbGcash;
    private LinearLayout layoutCashPayment, layoutGcashPayment;
    private MaterialButton btnUploadCash, btnUploadGcash, btnRemoveCash, btnRemoveGcash, btnBook;
    private ProgressBar progressBar;
    
    // Data
    private int roomId;
    private int userId;
    private int bhId;
    private String startDate;
    private String endDate;
    private JSONObject roomData;
    private String paymentMethod = "Cash";
    private Uri cashProofUri;
    private Uri gcashProofUri;
    private String ownerGcashQrPath;
    private RequestQueue requestQueue;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final_booking);
        
        requestQueue = Volley.newRequestQueue(this);
        
        // Get data from intent
        getIntentData();
        
        // Initialize views
        initializeViews();
        
        // Setup click listeners
        setupClickListeners();
        
        // Load booking summary
        loadBookingSummary();
    }
    
    private void getIntentData() {
        Intent intent = getIntent();
        roomId = intent.getIntExtra("room_id", 0);
        userId = intent.getIntExtra("user_id", 0);
        startDate = intent.getStringExtra("start_date");
        endDate = intent.getStringExtra("end_date");
        String roomDataString = intent.getStringExtra("room_data");
        
        // If user_id from intent is 0, try to get it from SharedPreferences
        if (userId == 0) {
            SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
            String userIdString = sharedPreferences.getString("user_id", null);
            if (userIdString != null) {
                try {
                    userId = Integer.parseInt(userIdString);
                    Log.d(TAG, "Retrieved user_id from SharedPreferences: " + userId);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing user_id from SharedPreferences: " + e.getMessage());
                }
            }
        }
        
        if (roomId == 0 || userId == 0 || startDate == null || endDate == null || roomDataString == null) {
            Toast.makeText(this, "Invalid booking data. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        Log.d(TAG, "Booking data - user_id: " + userId + ", room_id: " + roomId);
        
        try {
            roomData = new JSONObject(roomDataString);
            bhId = roomData.optInt("bh_id", 0);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing room data: " + e.getMessage());
            Toast.makeText(this, "Error loading booking data", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        ivBhImage = findViewById(R.id.ivBhImage);
        tvBhName = findViewById(R.id.tvBhName);
        tvRoomType = findViewById(R.id.tvRoomType);
        tvDuration = findViewById(R.id.tvDuration);
        tvPrice = findViewById(R.id.tvPrice);
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        rbCash = findViewById(R.id.rbCash);
        rbGcash = findViewById(R.id.rbGcash);
        layoutCashPayment = findViewById(R.id.layoutCashPayment);
        layoutGcashPayment = findViewById(R.id.layoutGcashPayment);
        btnUploadCash = findViewById(R.id.btnUploadCash);
        btnUploadGcash = findViewById(R.id.btnUploadGcash);
        btnRemoveCash = findViewById(R.id.btnRemoveCash);
        btnRemoveGcash = findViewById(R.id.btnRemoveGcash);
        ivCashProof = findViewById(R.id.ivCashProof);
        ivGcashProof = findViewById(R.id.ivGcashProof);
        ivOwnerQrCode = findViewById(R.id.ivOwnerQrCode);
        btnBook = findViewById(R.id.btnBook);
        progressBar = findViewById(R.id.progressBar);
    }
    
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        // Payment method selection
        rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbCash) {
                paymentMethod = "Cash";
                layoutCashPayment.setVisibility(View.VISIBLE);
                layoutGcashPayment.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbGcash) {
                paymentMethod = "GCash";
                layoutCashPayment.setVisibility(View.GONE);
                layoutGcashPayment.setVisibility(View.VISIBLE);
                // Load owner's GCash QR code
                loadOwnerGcashQr();
            }
        });
        
        // Upload buttons
        btnUploadCash.setOnClickListener(v -> selectImage(PICK_IMAGE_REQUEST));
        btnUploadGcash.setOnClickListener(v -> selectImage(PICK_IMAGE_REQUEST + 1));
        
        // Remove buttons
        btnRemoveCash.setOnClickListener(v -> {
            cashProofUri = null;
            ivCashProof.setVisibility(View.GONE);
            btnRemoveCash.setVisibility(View.GONE);
        });
        
        btnRemoveGcash.setOnClickListener(v -> {
            gcashProofUri = null;
            ivGcashProof.setVisibility(View.GONE);
            btnRemoveGcash.setVisibility(View.GONE);
        });
        
        // Book button
        btnBook.setOnClickListener(v -> {
            if (validateForm()) {
                createBooking();
            }
        });
    }
    
    private void loadBookingSummary() {
        try {
            // Display room type
            String roomCategory = roomData.optString("room_category", "Private Room");
            tvRoomType.setText(roomCategory);
            
            // Display price
            double price = roomData.optDouble("price", 0);
            tvPrice.setText("₱" + String.format("%,.0f", price) + "/month");
            
            // Calculate and display duration
            String duration = calculateDuration(startDate, endDate);
            tvDuration.setText("Duration: " + duration);
            
            // Load boarding house details
            loadBoardingHouseDetails();
        } catch (Exception e) {
            Log.e(TAG, "Error loading booking summary: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String calculateDuration(String start, String end) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date startDate = sdf.parse(start);
            Date endDate = sdf.parse(end);
            
            long diffInMillis = endDate.getTime() - startDate.getTime();
            long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);
            
            if (diffInDays == 1) {
                return "1 day";
            } else {
                return diffInDays + " days";
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error calculating duration: " + e.getMessage());
            return "N/A";
        }
    }
    
    private void loadBoardingHouseDetails() {
        String url = GET_BH_DETAILS_URL + "?bh_id=" + bhId;
        
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            if (jsonResponse.getBoolean("success")) {
                                JSONObject data = jsonResponse.getJSONObject("data");
                                JSONObject bh = data.getJSONObject("boarding_house");
                                
                                // Display BH name
                                tvBhName.setText(bh.getString("bh_name"));
                                
                                // Load BH image
                                if (bh.has("images") && !bh.isNull("images")) {
                                    org.json.JSONArray images = bh.getJSONArray("images");
                                    if (images.length() > 0) {
                                        String imageUrl = images.getString(0);
                                        Glide.with(FinalBookingActivity.this)
                                                .load(imageUrl)
                                                .placeholder(R.drawable.sample_listing)
                                                .error(R.drawable.sample_listing)
                                                .into(ivBhImage);
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing BH details: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error loading BH details: " + error.getMessage());
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", "BoardEase-Android-App");
                headers.put("Accept", "application/json");
                headers.put("ngrok-skip-browser-warning", "true");
                return headers;
            }
        };
        
        requestQueue.add(stringRequest);
    }
    
    private void loadOwnerGcashQr() {
        // Get owner's user_id from boarding house
        String url = GET_BH_DETAILS_URL + "?bh_id=" + bhId;
        
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            if (jsonResponse.getBoolean("success")) {
                                JSONObject data = jsonResponse.getJSONObject("data");
                                JSONObject bh = data.getJSONObject("boarding_house");
                                
                                // Get owner user_id from boarding house
                                int ownerUserId = 0;
                                if (bh.has("user_id")) {
                                    ownerUserId = bh.getInt("user_id");
                                }
                                
                                if (ownerUserId > 0) {
                                    loadGcashQrCode(ownerUserId);
                                } else {
                                    Log.e(TAG, "Could not find owner user_id");
                                    Toast.makeText(FinalBookingActivity.this, "Owner GCash QR not available", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing BH details for owner: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error loading owner info: " + error.getMessage());
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", "BoardEase-Android-App");
                headers.put("Accept", "application/json");
                headers.put("ngrok-skip-browser-warning", "true");
                return headers;
            }
        };
        
        requestQueue.add(stringRequest);
    }
    
    private void loadGcashQrCode(int ownerUserId) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST, GET_GCASH_INFO_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            if (jsonResponse.getBoolean("success")) {
                                ownerGcashQrPath = jsonResponse.optString("gcash_qr", "");
                                if (!ownerGcashQrPath.isEmpty()) {
                                    // Construct full image URL - if path doesn't start with http, prepend BASE_URL
                                    String fullImageUrl = ownerGcashQrPath.startsWith("http") 
                                        ? ownerGcashQrPath 
                                        : BASE_URL + ownerGcashQrPath;
                                    Glide.with(FinalBookingActivity.this)
                                            .load(fullImageUrl)
                                            .placeholder(R.drawable.placeholder)
                                            .error(R.drawable.placeholder)
                                            .into(ivOwnerQrCode);
                                } else {
                                    ivOwnerQrCode.setImageResource(R.drawable.placeholder);
                                }
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing GCash info: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error loading GCash QR: " + error.getMessage());
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(ownerUserId));
                return params;
            }
        };
        
        requestQueue.add(stringRequest);
    }
    
    private void selectImage(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, requestCode);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                if (requestCode == PICK_IMAGE_REQUEST) {
                    // Cash proof
                    cashProofUri = imageUri;
                    displayImage(imageUri, ivCashProof);
                    btnRemoveCash.setVisibility(View.VISIBLE);
                } else if (requestCode == PICK_IMAGE_REQUEST + 1) {
                    // GCash proof
                    gcashProofUri = imageUri;
                    displayImage(imageUri, ivGcashProof);
                    btnRemoveGcash.setVisibility(View.VISIBLE);
                }
            }
        }
    }
    
    private void displayImage(Uri imageUri, ImageView imageView) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            imageView.setImageBitmap(bitmap);
            imageView.setVisibility(View.VISIBLE);
        } catch (IOException e) {
            Log.e(TAG, "Error displaying image: " + e.getMessage());
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
        }
    }
    
    private boolean validateForm() {
        if (paymentMethod.equals("Cash")) {
            if (cashProofUri == null) {
                Toast.makeText(this, "Please upload cash transaction photo", Toast.LENGTH_SHORT).show();
                return false;
            }
        } else if (paymentMethod.equals("GCash")) {
            if (gcashProofUri == null) {
                Toast.makeText(this, "Please upload GCash payment screenshot", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }
    
    private void createBooking() {
        progressBar.setVisibility(View.VISIBLE);
        btnBook.setEnabled(false);
        
        // Convert image to base64
        final String paymentProofBase64;
        try {
            if (paymentMethod.equals("Cash") && cashProofUri != null) {
                paymentProofBase64 = imageToBase64(cashProofUri);
            } else if (paymentMethod.equals("GCash") && gcashProofUri != null) {
                paymentProofBase64 = imageToBase64(gcashProofUri);
            } else {
                paymentProofBase64 = "";
            }
        } catch (IOException e) {
            Log.e(TAG, "Error converting image to base64: " + e.getMessage());
            progressBar.setVisibility(View.GONE);
            btnBook.setEnabled(true);
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
            return;
        }
        
        StringRequest stringRequest = new StringRequest(Request.Method.POST, CREATE_BOOKING_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressBar.setVisibility(View.GONE);
                        btnBook.setEnabled(true);
                        
                        try {
                            Log.d(TAG, "Booking response: " + response);
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success");
                            String message = jsonResponse.optString("message", "");
                            
                            if (success) {
                                showSuccessDialog();
                            } else {
                                // Check if it's a user not found error
                                if (message.contains("User not found")) {
                                    // Clear session and ask user to log in again
                                    SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
                                    sharedPreferences.edit().clear().apply();
                                    
                                    AlertDialog.Builder builder = new AlertDialog.Builder(FinalBookingActivity.this);
                                    builder.setTitle("Session Expired");
                                    builder.setMessage("Your session is invalid. Please log in again.");
                                    builder.setPositiveButton("OK", (dialog, which) -> {
                                        dialog.dismiss();
                                        // Navigate to login
                                        Intent loginIntent = new Intent(FinalBookingActivity.this, Login.class);
                                        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(loginIntent);
                                        finish();
                                    });
                                    builder.setCancelable(false);
                                    builder.show();
                                } else {
                                    showErrorDialog("Unsuccessful: " + message);
                                }
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing response: " + e.getMessage());
                            Log.e(TAG, "Response: " + response);
                            showErrorDialog("Error parsing response: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressBar.setVisibility(View.GONE);
                        btnBook.setEnabled(true);
                        String errorMessage = "Network error";
                        if (error.networkResponse != null) {
                            errorMessage = "Error " + error.networkResponse.statusCode;
                            if (error.networkResponse.data != null) {
                                errorMessage += ": " + new String(error.networkResponse.data);
                            }
                        } else if (error.getMessage() != null) {
                            errorMessage = error.getMessage();
                        }
                        Log.e(TAG, "Volley error: " + errorMessage);
                        showErrorDialog("Unsuccessful: " + errorMessage);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("room_id", String.valueOf(roomId));
                params.put("user_id", String.valueOf(userId));
                params.put("start_date", startDate);
                params.put("end_date", endDate);
                params.put("payment_method", paymentMethod);
                params.put("payment_proof", paymentProofBase64);
                
                // Debug logging
                Log.d(TAG, "Sending booking request with user_id: " + userId + ", room_id: " + roomId);
                
                return params;
            }
            
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", "BoardEase-Android-App");
                headers.put("Accept", "application/json");
                headers.put("ngrok-skip-browser-warning", "true");
                return headers;
            }
        };
        
        requestQueue.add(stringRequest);
    }
    
    private String imageToBase64(Uri imageUri) throws IOException {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
        
        // Resize if too large
        int maxWidth = 800;
        int maxHeight = 800;
        if (bitmap.getWidth() > maxWidth || bitmap.getHeight() > maxHeight) {
            float scale = Math.min((float) maxWidth / bitmap.getWidth(), (float) maxHeight / bitmap.getHeight());
            int newWidth = Math.round(bitmap.getWidth() * scale);
            int newHeight = Math.round(bitmap.getHeight() * scale);
            bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageBytes = baos.toByteArray();
        return android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP);
    }
    
    private void showSuccessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Booking Successful");
        builder.setMessage("Your booking is received by the Owner and is subject for approval. you will receive a notification once the booking is successful. If you haven't received an update within 6 hours, you may message the BH Owner in the Messages section. Track your booking the Booking Page. Thank you!");
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
            finish();
        });
        builder.setCancelable(false);
        builder.show();
    }
    
    private void showErrorDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Booking Unsuccessful");
        builder.setMessage("Unsuccessful\n\nError Details: " + message);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}

