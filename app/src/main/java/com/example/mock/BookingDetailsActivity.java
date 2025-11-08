package com.example.mock;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.VolleyError;
import com.android.volley.Response;

public class BookingDetailsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ImageView imgProfile;
    private TextView tvBoarderName, tvEmail, tvPhone, tvRoomName, tvBoardingHouseName, 
                     tvBoardingHouseAddress, tvStartDate, tvEndDate, tvAmount, tvRentType, 
                     tvStatus, tvPaymentStatus, tvBookingDate;
    private MaterialButton btnApprove, btnDecline, btnContact, btnViewRoom;
    
    private BookingData bookingData;
    private ProgressDialog progressDialog;
    private static final int PERMISSION_REQUEST_CALL_PHONE = 100;
    private int ownerId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_details);
        
        // Get booking data from intent
        getIntentData();
        
        // Initialize views
        initializeViews();
        
        // Setup click listeners
        setupClickListeners();
        
        // Load booking data
        loadBookingData();
    }
    
    private void getIntentData() {
        // Get booking data from intent
        Intent intent = getIntent();
        
        // Get owner_id from intent, or fallback to SharedPreferences
        ownerId = intent.getIntExtra("owner_id", 0);
        if (ownerId == 0) {
            // Try to get from SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
            String userIdString = sharedPreferences.getString("user_id", "0");
            try {
                ownerId = Integer.parseInt(userIdString);
                Log.d("BookingDetails", "Got owner_id from SharedPreferences: " + ownerId);
            } catch (NumberFormatException e) {
                Log.e("BookingDetails", "Failed to parse user_id from SharedPreferences: " + userIdString);
                ownerId = 0;
            }
        } else {
            Log.d("BookingDetails", "Got owner_id from intent: " + ownerId);
        }
        
        if (intent != null && intent.hasExtra("booking_id")) {
            // Create booking data from intent extras
            bookingData = new BookingData(
                intent.getIntExtra("booking_id", 0),
                intent.getStringExtra("boarder_name"),
                intent.getStringExtra("boarder_email"),
                intent.getStringExtra("boarder_phone"),
                intent.getStringExtra("room_name"),
                intent.getStringExtra("start_date"),
                intent.getStringExtra("end_date"),
                intent.getStringExtra("amount"),
                intent.getStringExtra("rent_type"),
                intent.getStringExtra("status"),
                intent.getStringExtra("boarding_house_name"),
                intent.getStringExtra("boarding_house_address"),
                intent.getStringExtra("booking_date"),
                intent.getStringExtra("payment_status"),
                intent.getStringExtra("notes"),
                intent.getStringExtra("profile_image"),
                intent.getIntExtra("boarder_id", 0),
                intent.getIntExtra("room_id", 0),
                intent.getIntExtra("boarding_house_id", 0)
            );
        } else {
            // Fallback to sample data if intent data is missing
        bookingData = new BookingData(
            1, // bookingId
            "John Doe", // boarderName
            "john.doe@email.com", // email
            "09123456789", // phoneNumber
            "Room 1 - Single", // roomName
            "2025-01-15", // startDate
            "2025-04-15", // endDate
            "P3,000.00", // amount
            "Long-term", // rentType
            "Pending", // status
            "Sunset Boarding House", // boardingHouseName
            "123 Main Street, City", // boardingHouseAddress
            "2025-01-10", // bookingDate
            "Pending", // paymentStatus
            "Prefer ground floor room", // notes
            "", // profileImage
            1, // boarderId
            1, // roomId
            1  // boardingHouseId
        );
        }
    }
    
    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        imgProfile = findViewById(R.id.imgProfile);
        tvBoarderName = findViewById(R.id.tvBoarderName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvRoomName = findViewById(R.id.tvRoomName);
        tvBoardingHouseName = findViewById(R.id.tvBoardingHouseName);
        tvBoardingHouseAddress = findViewById(R.id.tvBoardingHouseAddress);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);
        tvAmount = findViewById(R.id.tvAmount);
        tvRentType = findViewById(R.id.tvRentType);
        tvStatus = findViewById(R.id.tvStatus);
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus);
        tvBookingDate = findViewById(R.id.tvBookingDate);
        btnApprove = findViewById(R.id.btnApprove);
        btnDecline = findViewById(R.id.btnDecline);
        btnContact = findViewById(R.id.btnContact);
        btnViewRoom = findViewById(R.id.btnViewRoom);
    }
    
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnApprove.setOnClickListener(v -> showApprovalDialog());
        btnDecline.setOnClickListener(v -> showDeclineDialog());
        btnContact.setOnClickListener(v -> contactBoarder());
        btnViewRoom.setOnClickListener(v -> viewRoomDetails());
    }
    
    private void loadBookingData() {
        if (bookingData != null) {
            tvBoarderName.setText(bookingData.getBoarderName() != null ? bookingData.getBoarderName() : "");
            tvEmail.setText(bookingData.getEmail() != null ? bookingData.getEmail() : "");
            tvPhone.setText(bookingData.getPhoneNumber() != null ? bookingData.getPhoneNumber() : "");
            tvRoomName.setText(bookingData.getRoomName() != null ? bookingData.getRoomName() : "");
            tvBoardingHouseName.setText(bookingData.getBoardingHouseName() != null ? bookingData.getBoardingHouseName() : "");
            tvBoardingHouseAddress.setText(bookingData.getBoardingHouseAddress() != null ? bookingData.getBoardingHouseAddress() : "");
            tvStartDate.setText(bookingData.getStartDate() != null ? bookingData.getStartDate() : "");
            tvEndDate.setText(bookingData.getEndDate() != null ? bookingData.getEndDate() : "");
            tvAmount.setText(bookingData.getAmount() != null ? bookingData.getAmount() : "");
            tvRentType.setText(bookingData.getRentType() != null ? bookingData.getRentType() : "");
            
            // Set status with styling
            String status = bookingData.getStatus() != null ? bookingData.getStatus() : "";
            tvStatus.setText(status);
            applyStatusStyle(tvStatus, status);
            
            // Set payment status with styling
            String paymentStatus = bookingData.getPaymentStatus() != null ? bookingData.getPaymentStatus() : "";
            tvPaymentStatus.setText(paymentStatus);
            applyPaymentStatusStyle(tvPaymentStatus, paymentStatus);
            
            tvBookingDate.setText(bookingData.getBookingDate() != null ? bookingData.getBookingDate() : "");
            
            // Set profile image (placeholder for now)
            if (imgProfile != null) {
            imgProfile.setImageResource(R.drawable.ic_profile);
            }
            
            // Show/hide action buttons based on status
            updateActionButtons();
        } else {
            Toast.makeText(this, "Error: Booking data not available", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void applyStatusStyle(TextView textView, String status) {
        if (textView == null || status == null) return;
        
        int textColor;
        int backgroundRes;
        
        switch (status) {
            case "Confirmed":
            case "Approved":
                textColor = getResources().getColor(android.R.color.white);
                backgroundRes = R.drawable.bg_status_approved;
                break;
            case "Cancelled":
            case "Declined":
                textColor = getResources().getColor(android.R.color.white);
                backgroundRes = R.drawable.bg_rounded_red;
                break;
            case "Completed":
                textColor = getResources().getColor(android.R.color.white);
                backgroundRes = R.drawable.bg_status_completed;
                break;
            case "Pending":
            default:
                textColor = getResources().getColor(android.R.color.white);
                backgroundRes = R.drawable.bg_status_pending;
                break;
        }
        
        int padding = (int) (12 * getResources().getDisplayMetrics().density);
        textView.setTextColor(textColor);
        textView.setBackgroundResource(backgroundRes);
        textView.setPadding(padding, padding / 2, padding, padding / 2);
    }
    
    private void applyPaymentStatusStyle(TextView textView, String paymentStatus) {
        if (textView == null || paymentStatus == null) return;
        
        int textColor;
        int backgroundRes;
        
        switch (paymentStatus.toLowerCase()) {
            case "paid":
            case "confirmed":
                textColor = getResources().getColor(android.R.color.white);
                backgroundRes = R.drawable.bg_status_approved;
                break;
            case "overdue":
            case "cancelled":
                textColor = getResources().getColor(android.R.color.white);
                backgroundRes = R.drawable.bg_rounded_red;
                break;
            case "pending":
            default:
                textColor = getResources().getColor(android.R.color.white);
                backgroundRes = R.drawable.bg_status_pending;
                break;
        }
        
        int padding = (int) (12 * getResources().getDisplayMetrics().density);
        textView.setTextColor(textColor);
        textView.setBackgroundResource(backgroundRes);
        textView.setPadding(padding, padding / 2, padding, padding / 2);
    }
    
    private void updateActionButtons() {
        if (bookingData == null || bookingData.getStatus() == null) {
            return;
        }
        
        String status = bookingData.getStatus();
        
        if (btnApprove != null && btnDecline != null) {
        switch (status) {
            case "Pending":
                btnApprove.setVisibility(View.VISIBLE);
                btnDecline.setVisibility(View.VISIBLE);
                btnApprove.setText("Approve Booking");
                btnDecline.setText("Decline Booking");
                break;
                case "Confirmed":
                btnApprove.setVisibility(View.GONE);
                btnDecline.setVisibility(View.GONE);
                break;
                case "Cancelled":
            case "Declined":
                btnApprove.setVisibility(View.GONE);
                btnDecline.setVisibility(View.GONE);
                break;
            case "Completed":
                btnApprove.setVisibility(View.GONE);
                btnDecline.setVisibility(View.GONE);
                break;
                default:
                btnApprove.setVisibility(View.GONE);
                btnDecline.setVisibility(View.GONE);
                break;
            }
        }
        
        // Update status styling when status changes
        if (tvStatus != null) {
            applyStatusStyle(tvStatus, status);
        }
    }
    
    private void showApprovalDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Approve Booking")
                .setMessage("Are you sure you want to approve this booking?")
                .setPositiveButton("Approve", (dialog, which) -> approveBooking())
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showDeclineDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Decline Booking")
                .setMessage("Are you sure you want to decline this booking?")
                .setPositiveButton("Decline", (dialog, which) -> declineBooking())
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void approveBooking() {
        showProgressDialog("Approving booking...");
        
        int bookingId = bookingData.getBookingId();
        
        Log.d("BookingDetails", "Approve booking - ownerId: " + ownerId + ", bookingId: " + bookingId);
        
        if (ownerId == 0) {
            hideProgressDialog();
            Toast.makeText(this, "Error: Owner ID not found. Please log in again.", Toast.LENGTH_LONG).show();
            Log.e("BookingDetails", "Owner ID is 0 - cannot approve booking");
            return;
        }
        
        if (bookingId == 0) {
            hideProgressDialog();
            Toast.makeText(this, "Error: Booking ID not found", Toast.LENGTH_SHORT).show();
            Log.e("BookingDetails", "Booking ID is 0 - cannot approve booking");
            return;
        }
        
        // Make API call to approve booking
        String url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/approve_booking.php";
        
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("booking_id", bookingId);
            requestBody.put("owner_id", ownerId);
            
            Log.d("BookingDetails", "Request URL: " + url);
            Log.d("BookingDetails", "Request body: " + requestBody.toString());
            
            RequestQueue requestQueue = Volley.newRequestQueue(this);
            
            // Set timeout
            requestQueue.getCache().clear();
            
            JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, url, requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
            hideProgressDialog();
                        Log.d("BookingDetails", "Approve response: " + response.toString());
                        try {
                            if (response.getBoolean("success")) {
                                bookingData.setStatus("Confirmed");
                                loadBookingData(); // Reload to update status display
                                updateActionButtons(); // Update buttons after status change
                                Toast.makeText(BookingDetailsActivity.this, "Booking approved successfully!", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                // Don't finish immediately - let user see updated status, they can go back manually
                            } else {
                                String errorMsg = response.optString("error", "Unknown error occurred");
                                Log.e("BookingDetails", "Approve error from server: " + errorMsg);
                                Toast.makeText(BookingDetailsActivity.this, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Log.e("BookingDetails", "Error parsing response", e);
                            hideProgressDialog();
                            Toast.makeText(BookingDetailsActivity.this, "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        hideProgressDialog();
                        Log.e("BookingDetails", "Volley error: " + error.toString());
                        Log.e("BookingDetails", "Error message: " + (error.getMessage() != null ? error.getMessage() : "null"));
                        Log.e("BookingDetails", "Error class: " + error.getClass().getName());
                        
                        String errorMessage = "Connection failed";
                        
                        if (error.networkResponse != null) {
                            int statusCode = error.networkResponse.statusCode;
                            Log.e("BookingDetails", "Network response status: " + statusCode);
                            
                            if (error.networkResponse.data != null) {
                                try {
                                    String responseBody = new String(error.networkResponse.data, "utf-8");
                                    Log.e("BookingDetails", "Error response body (first 500 chars): " + 
                                        (responseBody.length() > 500 ? responseBody.substring(0, 500) : responseBody));
                                    
                                    // Check for specific error types
                                    if (statusCode == 404) {
                                        errorMessage = "Server endpoint not found (404). Please verify approve_booking.php is on the server.";
                                    } else if (statusCode == 503) {
                                        if (responseBody.contains("ERR_NGROK_3004")) {
                                            errorMessage = "Server connection error (503). The server may be unreachable or ngrok is blocking the request.";
                                        } else {
                                            errorMessage = "Server temporarily unavailable (503). Please try again in a moment.";
                                        }
                                    } else if (responseBody.contains("<html") || responseBody.contains("ngrok") || 
                                        responseBody.contains("ERR_NGROK") || responseBody.contains("gateway error")) {
                                        if (responseBody.contains("ERR_NGROK_3004")) {
                                            errorMessage = "Server connection error. Please check if the server is running.";
                                        } else {
                                            errorMessage = "Server connection blocked by ngrok. Please try again.";
                                        }
                                    } else {
                                        // Try to parse as JSON
                                        try {
                                            JSONObject errorJson = new JSONObject(responseBody);
                                            if (errorJson.has("error")) {
                                                errorMessage = errorJson.getString("error");
                                            } else {
                                                errorMessage = "Server error: " + statusCode;
                                            }
                                        } catch (JSONException e) {
                                            errorMessage = "Server error: " + statusCode + " - " + 
                                                (responseBody.length() > 100 ? responseBody.substring(0, 100) : responseBody);
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e("BookingDetails", "Error parsing error response", e);
                                    if (statusCode == 404) {
                                        errorMessage = "Server endpoint not found (404)";
                                    } else if (statusCode == 503) {
                                        errorMessage = "Server temporarily unavailable (503)";
                                    } else {
                                        errorMessage = "Server error: " + statusCode;
                                    }
                                }
                            } else {
                                if (statusCode == 404) {
                                    errorMessage = "Server endpoint not found (404)";
                                } else if (statusCode == 503) {
                                    errorMessage = "Server temporarily unavailable (503)";
                                } else {
                                    errorMessage = "Server error: " + statusCode;
                                }
                            }
                        } else {
                            // Check error type
                            String errorClass = error.getClass().getSimpleName();
                            Log.e("BookingDetails", "Error class name: " + errorClass);
                            
                            if (error.getMessage() != null && !error.getMessage().isEmpty()) {
                                errorMessage = error.getMessage();
                            } else if (error.getCause() != null) {
                                String causeMessage = error.getCause().getMessage();
                                if (causeMessage != null && !causeMessage.isEmpty()) {
                                    errorMessage = causeMessage;
                                } else {
                                    errorMessage = "Network connection failed. Please check your internet connection.";
                                }
                            } else {
                                // Check error type by class name
                                if (errorClass.contains("Timeout")) {
                                    errorMessage = "Request timed out. Please try again.";
                                } else if (errorClass.contains("NoConnection")) {
                                    errorMessage = "No internet connection. Please check your network.";
                                } else if (errorClass.contains("Network")) {
                                    errorMessage = "Network error. Please check your connection.";
                                } else {
                                    errorMessage = "Connection failed. Please check your internet connection and try again.";
                                }
                            }
                        }
                        
                        Log.e("BookingDetails", "Final error message: " + errorMessage);
                        Toast.makeText(BookingDetailsActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            ) {
                @Override
                public java.util.Map<String, String> getHeaders() {
                    java.util.Map<String, String> headers = new java.util.HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("ngrok-skip-browser-warning", "true");
                    headers.put("Accept", "application/json");
                    headers.put("User-Agent", "BoardEase-Android-App");
                    return headers;
                }
                
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };
            
            // Set retry policy
            request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                    10000, // 10 seconds timeout
                    1, // 1 retry
                    com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));
            
            requestQueue.add(request);
        } catch (JSONException e) {
            Log.e("BookingDetails", "Error creating request", e);
            hideProgressDialog();
            Toast.makeText(this, "Error creating request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void declineBooking() {
        showProgressDialog("Declining booking...");
        
        int bookingId = bookingData.getBookingId();
        
        Log.d("BookingDetails", "Decline booking - ownerId: " + ownerId + ", bookingId: " + bookingId);
        
        if (ownerId == 0) {
            hideProgressDialog();
            Toast.makeText(this, "Error: Owner ID not found. Please log in again.", Toast.LENGTH_LONG).show();
            Log.e("BookingDetails", "Owner ID is 0 - cannot decline booking");
            return;
        }
        
        if (bookingId == 0) {
            hideProgressDialog();
            Toast.makeText(this, "Error: Booking ID not found", Toast.LENGTH_SHORT).show();
            Log.e("BookingDetails", "Booking ID is 0 - cannot decline booking");
            return;
        }
        
        // Make API call to decline booking
        String url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/decline_booking.php";
        
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("booking_id", bookingId);
            requestBody.put("owner_id", ownerId);
            requestBody.put("reason", "Declined by owner");
            
            Log.d("BookingDetails", "Decline request URL: " + url);
            Log.d("BookingDetails", "Decline request body: " + requestBody.toString());
            
            RequestQueue requestQueue = Volley.newRequestQueue(this);
            
            // Set timeout
            requestQueue.getCache().clear();
            
            JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, url, requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
            hideProgressDialog();
                        Log.d("BookingDetails", "Decline response: " + response.toString());
                        try {
                            if (response.getBoolean("success")) {
                                bookingData.setStatus("Cancelled");
                                loadBookingData(); // Reload to update status display
                                updateActionButtons(); // Update buttons after status change
                                Toast.makeText(BookingDetailsActivity.this, "Booking declined.", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                // Don't finish immediately - let user see updated status, they can go back manually
                            } else {
                                String errorMsg = response.optString("error", "Unknown error occurred");
                                Log.e("BookingDetails", "Decline error from server: " + errorMsg);
                                Toast.makeText(BookingDetailsActivity.this, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Log.e("BookingDetails", "Error parsing decline response", e);
                            hideProgressDialog();
                            Toast.makeText(BookingDetailsActivity.this, "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        hideProgressDialog();
                        Log.e("BookingDetails", "Volley error (Decline): " + error.toString());
                        Log.e("BookingDetails", "Error message: " + (error.getMessage() != null ? error.getMessage() : "null"));
                        Log.e("BookingDetails", "Error class: " + error.getClass().getName());
                        
                        String errorMessage = "Connection failed";
                        
                        if (error.networkResponse != null) {
                            int statusCode = error.networkResponse.statusCode;
                            Log.e("BookingDetails", "Network response status: " + statusCode);
                            
                            if (error.networkResponse.data != null) {
                                try {
                                    String responseBody = new String(error.networkResponse.data, "utf-8");
                                    Log.e("BookingDetails", "Error response body (first 500 chars): " + 
                                        (responseBody.length() > 500 ? responseBody.substring(0, 500) : responseBody));
                                    
                                    // Check for specific error types
                                    if (statusCode == 404) {
                                        errorMessage = "Server endpoint not found (404). Please verify approve_booking.php is on the server.";
                                    } else if (statusCode == 503) {
                                        if (responseBody.contains("ERR_NGROK_3004")) {
                                            errorMessage = "Server connection error (503). The server may be unreachable or ngrok is blocking the request.";
                                        } else {
                                            errorMessage = "Server temporarily unavailable (503). Please try again in a moment.";
                                        }
                                    } else if (responseBody.contains("<html") || responseBody.contains("ngrok") || 
                                        responseBody.contains("ERR_NGROK") || responseBody.contains("gateway error")) {
                                        if (responseBody.contains("ERR_NGROK_3004")) {
                                            errorMessage = "Server connection error. Please check if the server is running.";
                                        } else {
                                            errorMessage = "Server connection blocked by ngrok. Please try again.";
                                        }
                                    } else {
                                        // Try to parse as JSON
                                        try {
                                            JSONObject errorJson = new JSONObject(responseBody);
                                            if (errorJson.has("error")) {
                                                errorMessage = errorJson.getString("error");
                                            } else {
                                                errorMessage = "Server error: " + statusCode;
                                            }
                                        } catch (JSONException e) {
                                            errorMessage = "Server error: " + statusCode + " - " + 
                                                (responseBody.length() > 100 ? responseBody.substring(0, 100) : responseBody);
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e("BookingDetails", "Error parsing error response", e);
                                    if (statusCode == 404) {
                                        errorMessage = "Server endpoint not found (404)";
                                    } else if (statusCode == 503) {
                                        errorMessage = "Server temporarily unavailable (503)";
                                    } else {
                                        errorMessage = "Server error: " + statusCode;
                                    }
                                }
                            } else {
                                if (statusCode == 404) {
                                    errorMessage = "Server endpoint not found (404)";
                                } else if (statusCode == 503) {
                                    errorMessage = "Server temporarily unavailable (503)";
                                } else {
                                    errorMessage = "Server error: " + statusCode;
                                }
                            }
                        } else {
                            // Check error type
                            String errorClass = error.getClass().getSimpleName();
                            Log.e("BookingDetails", "Error class name: " + errorClass);
                            
                            if (error.getMessage() != null && !error.getMessage().isEmpty()) {
                                errorMessage = error.getMessage();
                            } else if (error.getCause() != null) {
                                String causeMessage = error.getCause().getMessage();
                                if (causeMessage != null && !causeMessage.isEmpty()) {
                                    errorMessage = causeMessage;
                                } else {
                                    errorMessage = "Network connection failed. Please check your internet connection.";
                                }
                            } else {
                                // Check error type by class name
                                if (errorClass.contains("Timeout")) {
                                    errorMessage = "Request timed out. Please try again.";
                                } else if (errorClass.contains("NoConnection")) {
                                    errorMessage = "No internet connection. Please check your network.";
                                } else if (errorClass.contains("Network")) {
                                    errorMessage = "Network error. Please check your connection.";
                                } else {
                                    errorMessage = "Connection failed. Please check your internet connection and try again.";
                                }
                            }
                        }
                        
                        Log.e("BookingDetails", "Final error message: " + errorMessage);
                        Toast.makeText(BookingDetailsActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            ) {
                @Override
                public java.util.Map<String, String> getHeaders() {
                    java.util.Map<String, String> headers = new java.util.HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("ngrok-skip-browser-warning", "true");
                    headers.put("Accept", "application/json");
                    headers.put("User-Agent", "BoardEase-Android-App");
                    return headers;
                }
                
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };
            
            // Set retry policy
            request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                    10000, // 10 seconds timeout
                    1, // 1 retry
                    com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));
            
            requestQueue.add(request);
        } catch (JSONException e) {
            Log.e("BookingDetails", "Error creating decline request", e);
            hideProgressDialog();
            Toast.makeText(this, "Error creating request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void contactBoarder() {
        // Show contact options dialog
        String phoneNumber = bookingData.getPhoneNumber();
        String email = bookingData.getEmail();
        String boarderName = bookingData.getBoarderName();
        
        if ((phoneNumber == null || phoneNumber.isEmpty()) && (email == null || email.isEmpty())) {
            Toast.makeText(this, "No contact information available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] options;
        if (phoneNumber != null && !phoneNumber.isEmpty() && email != null && !email.isEmpty()) {
            options = new String[]{"Call", "Send SMS", "Send Email"};
        } else if (phoneNumber != null && !phoneNumber.isEmpty()) {
            options = new String[]{"Call", "Send SMS"};
        } else {
            options = new String[]{"Send Email"};
        }
        
        new AlertDialog.Builder(this)
                .setTitle("Contact " + boarderName)
                .setItems(options, (dialog, which) -> {
                    String selectedOption = options[which];
                    if (selectedOption.equals("Call")) {
                        makePhoneCall(phoneNumber);
                    } else if (selectedOption.equals("Send SMS")) {
                        sendSMS(phoneNumber);
                    } else if (selectedOption.equals("Send Email")) {
                        sendEmail(email, boarderName);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void makePhoneCall(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{android.Manifest.permission.CALL_PHONE}, 
                    PERMISSION_REQUEST_CALL_PHONE);
            return;
        }
        
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + phoneNumber));
        try {
            startActivity(callIntent);
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission denied to make phone call", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void sendSMS(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
        smsIntent.setData(Uri.parse("smsto:" + phoneNumber));
        smsIntent.putExtra("sms_body", "Hello, regarding your booking at " + bookingData.getBoardingHouseName());
        try {
            startActivity(smsIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open SMS app", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void sendEmail(String email, String boarderName) {
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Email address not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:" + email));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Regarding Your Booking at " + bookingData.getBoardingHouseName());
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Hello " + boarderName + ",\n\nRegarding your booking:");
        try {
            startActivity(Intent.createChooser(emailIntent, "Send email using"));
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open email app", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void viewRoomDetails() {
        // Navigate to RoomViewActivity to show all rooms for this boarding house
        int boardingHouseId = bookingData.getBoardingHouseId();
        String boardingHouseName = bookingData.getBoardingHouseName();
        
        if (boardingHouseId == 0) {
            Toast.makeText(this, "Boarding house information not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(this, RoomViewActivity.class);
        intent.putExtra("bh_id", boardingHouseId);
        intent.putExtra("bh_name", boardingHouseName != null ? boardingHouseName : "Rooms");
        startActivity(intent);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CALL_PHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, make the call
                String phoneNumber = bookingData.getPhoneNumber();
                if (phoneNumber != null && !phoneNumber.isEmpty()) {
                    makePhoneCall(phoneNumber);
                }
            } else {
                Toast.makeText(this, "Permission denied to make phone call", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void showProgressDialog(String message) {
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(message);
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideProgressDialog() {
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            progressDialog = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        hideProgressDialog();
    }
}

















