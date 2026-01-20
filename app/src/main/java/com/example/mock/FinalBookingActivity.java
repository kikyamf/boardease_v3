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
import android.widget.CheckBox;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FinalBookingActivity extends AppCompatActivity {
    
    private static final String TAG = "FinalBookingActivity";
    // Local development URL - Using local IP address (192.168.1.4)
    private static final String BASE_URL = "https://boardease.calapebohol.com/";
    private static final String BOARD_EASE2_URL = BASE_URL + "";
    private static final String GET_BH_DETAILS_URL = BASE_URL + "get_boarding_house_details1.php";
    private static final String GET_GCASH_INFO_URL = BOARD_EASE2_URL + "get_gcash_info.php";
    private static final String CREATE_BOOKING_URL = BOARD_EASE2_URL + "create_booking.php";
    private static final int PICK_IMAGE_REQUEST = 100;
    
    // Views // balik update
    private ImageButton btnBack;
    private ImageView ivBhImage, ivCashProof, ivGcashProof, ivOwnerQrCode;
    private TextView tvBhName, tvRoomType, tvDuration, tvPrice, tvGcashNumber, tvTotalPayment, tvPaymentBreakdown;
    private RadioGroup rgPaymentMethod;
    private RadioButton rbCash, rbGcash;
    private LinearLayout layoutCashPayment, layoutGcashPayment, layoutPaymentSelection, layoutPaymentCheckboxes;
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
    private String ownerGcashNumber;
    private RequestQueue requestQueue;
    
    // Payment calculation
    private double monthlyPrice;
    private int numberOfDays;
    private double totalPaymentAmount;
    
    // Payment breakdown for multi-month stays
    private List<PaymentPeriod> paymentPeriods;
    private List<CheckBox> paymentCheckboxes;
    
    // Inner class to represent a payment period
    private static class PaymentPeriod {
        String label;
        String periodType; // "month" or "days"
        int periodNumber; // 1, 2, 3 for months, 0 for days
        double amount;
        String startDate;
        String endDate;
        boolean isSelected;
        
        PaymentPeriod(String label, String periodType, int periodNumber, double amount, String startDate, String endDate) {
            this.label = label;
            this.periodType = periodType;
            this.periodNumber = periodNumber;
            this.amount = amount;
            this.startDate = startDate;
            this.endDate = endDate;
            this.isSelected = false;
        }
    }
    
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
            Log.d(TAG, "Room data JSON: " + roomData.toString());
            
            // Try multiple possible keys for bh_id
            if (roomData.has("bh_id")) {
                bhId = roomData.optInt("bh_id", 0);
            } else if (roomData.has("boarding_house_id")) {
                bhId = roomData.optInt("boarding_house_id", 0);
            } else {
                // Try to get it from Intent directly
                bhId = intent.getIntExtra("bh_id", 0);
                if (bhId == 0) {
                    bhId = intent.getIntExtra("boarding_house_id", 0);
                }
            }
            
            Log.d(TAG, "Extracted bh_id: " + bhId);
            
            if (bhId == 0) {
                Log.e(TAG, "ERROR: bh_id is 0! Room data keys: " + roomData.keys());
                Toast.makeText(this, "Error: Boarding house ID not found", Toast.LENGTH_SHORT).show();
            }
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
        tvTotalPayment = findViewById(R.id.tvTotalPayment);
        tvPaymentBreakdown = findViewById(R.id.tvPaymentBreakdown);
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
        tvGcashNumber = findViewById(R.id.tvGcashNumber);
        btnBook = findViewById(R.id.btnBook);
        progressBar = findViewById(R.id.progressBar);
        layoutPaymentSelection = findViewById(R.id.layoutPaymentSelection);
        layoutPaymentCheckboxes = findViewById(R.id.layoutPaymentCheckboxes);
        
        // Initialize lists
        paymentPeriods = new ArrayList<>();
        paymentCheckboxes = new ArrayList<>();
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
        
        // Book button - prevent double clicks
        btnBook.setOnClickListener(v -> {
            // Prevent multiple clicks
            if (isBookingInProgress) {
                Log.w(TAG, "Booking already in progress, ignoring click");
                return;
            }
            
            if (validateForm()) {
                // Disable button immediately to prevent double clicks
                btnBook.setEnabled(false);
                createBooking();
            }
        });
    }
    
    private void loadBookingSummary() {
        try {
            // Display room type
            String roomCategory = roomData.optString("room_category", "Private Room");
            tvRoomType.setText(roomCategory);
            
            // Get monthly price
            monthlyPrice = roomData.optDouble("price", 0);
            tvPrice.setText("₱" + String.format("%,.0f", monthlyPrice) + "/month");
            
            // Calculate number of days and create payment breakdown
            numberOfDays = calculateNumberOfDays(startDate, endDate);
            createPaymentBreakdown(monthlyPrice, numberOfDays);
            
            // Display duration
            String duration = formatDuration(numberOfDays);
            tvDuration.setText("Duration: " + duration);
            
            // Display payment UI (checkboxes for multi-month or simple display)
            displayPaymentUI();
            
            // Load boarding house details
            loadBoardingHouseDetails();
        } catch (Exception e) {
            Log.e(TAG, "Error loading booking summary: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private int calculateNumberOfDays(String start, String end) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date startDateObj = sdf.parse(start);
            Date endDateObj = sdf.parse(end);
            
            long diffInMillis = endDateObj.getTime() - startDateObj.getTime();
            long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);
            
            // Start date is not included in the count
            // Count starts from the next day after start date
            // Example: Start = Jan 1, End = Jan 3
            // Days counted: Jan 2, Jan 3 = 2 days (not 3)
            return (int) diffInDays;
        } catch (ParseException e) {
            Log.e(TAG, "Error calculating number of days: " + e.getMessage());
            return 30; // Default to 30 days if calculation fails
        }
    }
    
    private void createPaymentBreakdown(double monthlyPrice, int numberOfDays) {
        paymentPeriods.clear();
        
        if (numberOfDays <= 0) {
            totalPaymentAmount = 0.0;
            return;
        }
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(startDate));
            // Start from next day (start date is not included)
            cal.add(Calendar.DAY_OF_MONTH, 1);
            
            int remainingDays = numberOfDays;
            int monthCount = 0;
            
            // Create monthly periods
            while (remainingDays >= 30) {
                monthCount++;
                Calendar monthStart = (Calendar) cal.clone();
                Calendar monthEnd = (Calendar) cal.clone();
                monthEnd.add(Calendar.DAY_OF_MONTH, 29); // 30 days total (0-29 = 30 days)
                
                String periodStart = sdf.format(monthStart.getTime());
                String periodEnd = sdf.format(monthEnd.getTime());
                String label = getMonthLabel(monthCount);
                
                paymentPeriods.add(new PaymentPeriod(label, "month", monthCount, monthlyPrice, periodStart, periodEnd));
                
                cal.add(Calendar.DAY_OF_MONTH, 30);
                remainingDays -= 30;
            }
            
            // Create days period if there are remaining days
            if (remainingDays > 0) {
                Calendar daysStart = (Calendar) cal.clone();
                Calendar daysEnd = (Calendar) cal.clone();
                daysEnd.add(Calendar.DAY_OF_MONTH, remainingDays - 1);
                
                String periodStart = sdf.format(daysStart.getTime());
                String periodEnd = sdf.format(daysEnd.getTime());
                double dailyRate = monthlyPrice / 30.0;
                double daysAmount = dailyRate * remainingDays;
                String label = remainingDays + (remainingDays == 1 ? " day" : " days");
                
                paymentPeriods.add(new PaymentPeriod(label, "days", 0, daysAmount, periodStart, periodEnd));
            }
            
            // Calculate default total (all selected by default)
            updateTotalPayment();
        } catch (ParseException e) {
            Log.e(TAG, "Error creating payment breakdown: " + e.getMessage());
            // Fallback to simple calculation
            if (numberOfDays < 30) {
                double dailyRate = monthlyPrice / 30.0;
                totalPaymentAmount = dailyRate * numberOfDays;
            } else {
                totalPaymentAmount = monthlyPrice;
            }
        }
    }
    
    private String getMonthLabel(int monthNumber) {
        switch (monthNumber) {
            case 1: return "1st month";
            case 2: return "2nd month";
            case 3: return "3rd month";
            case 4: return "4th month";
            case 5: return "5th month";
            case 6: return "6th month";
            default: return monthNumber + "th month";
        }
    }
    
    private void displayPaymentUI() {
        // Clear existing checkboxes
        layoutPaymentCheckboxes.removeAllViews();
        paymentCheckboxes.clear();
        
        // Show checkboxes only if more than 1 month
        if (paymentPeriods.size() > 1) {
            layoutPaymentSelection.setVisibility(View.VISIBLE);
            
            // Create checkboxes for each period
            for (int i = 0; i < paymentPeriods.size(); i++) {
                PaymentPeriod period = paymentPeriods.get(i);
                CheckBox checkBox = createPaymentCheckbox(period, i);
                layoutPaymentCheckboxes.addView(checkBox);
                paymentCheckboxes.add(checkBox);
                
                // Set first checkbox as checked by default
                if (i == 0) {
                    checkBox.setChecked(true);
                    period.isSelected = true;
                }
            }
        } else {
            layoutPaymentSelection.setVisibility(View.GONE);
            // For single period, select it by default
            if (!paymentPeriods.isEmpty()) {
                paymentPeriods.get(0).isSelected = true;
            }
        }
        
        // Update total payment display
        updateTotalPayment();
    }
    
    private CheckBox createPaymentCheckbox(PaymentPeriod period, int index) {
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(String.format(Locale.getDefault(), "%s - ₱%,.2f", period.label, period.amount));
        checkBox.setTextSize(14);
        checkBox.setTypeface(getResources().getFont(R.font.poppins_medium));
        checkBox.setTextColor(getResources().getColor(R.color.dark_gray));
        checkBox.setButtonTintList(getResources().getColorStateList(R.color.brown));
        checkBox.setPadding(0, 8, 0, 8);
        
        // Sequential logic: can only check if previous is checked (except first)
        if (index > 0) {
            checkBox.setEnabled(false);
        }
        
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            period.isSelected = isChecked;
            updateTotalPayment();
            updateCheckboxStates();
        });
        
        return checkBox;
    }
    
    private void updateCheckboxStates() {
        // Enable/disable checkboxes based on sequential logic
        for (int i = 0; i < paymentCheckboxes.size(); i++) {
            CheckBox checkBox = paymentCheckboxes.get(i);
            if (i == 0) {
                // First checkbox is always enabled
                checkBox.setEnabled(true);
            } else {
                // Can only check if previous is checked
                CheckBox previousCheckBox = paymentCheckboxes.get(i - 1);
                boolean canEnable = previousCheckBox.isChecked();
                checkBox.setEnabled(canEnable);
                
                // If previous is unchecked, uncheck this one too
                if (!canEnable && checkBox.isChecked()) {
                    checkBox.setChecked(false);
                    paymentPeriods.get(i).isSelected = false;
                }
            }
        }
    }
    
    private void updateTotalPayment() {
        totalPaymentAmount = 0.0;
        for (PaymentPeriod period : paymentPeriods) {
            if (period.isSelected) {
                totalPaymentAmount += period.amount;
            }
        }
        
        // Update display
        tvTotalPayment.setText("₱" + String.format("%,.2f", totalPaymentAmount));
        
        // Update breakdown text
        int selectedCount = 0;
        for (PaymentPeriod period : paymentPeriods) {
            if (period.isSelected) selectedCount++;
        }
        
        if (selectedCount > 0) {
            tvPaymentBreakdown.setText(String.format(Locale.getDefault(), 
                "%d of %d period(s) selected", selectedCount, paymentPeriods.size()));
        } else {
            tvPaymentBreakdown.setText("Please select at least one payment period");
        }
    }
    
    private String formatDuration(int days) {
        if (days == 1) {
            return "1 day";
        } else if (days < 30) {
            return days + " days";
        } else {
            int months = days / 30;
            int remainingDays = days % 30;
            if (remainingDays == 0) {
                return months + (months == 1 ? " month" : " months");
            } else {
                return months + (months == 1 ? " month" : " months") + " and " + remainingDays + (remainingDays == 1 ? " day" : " days");
            }
        }
    }
    
    
    
    private void loadBoardingHouseDetails() {
        // Validate bhId before making API call
        if (bhId == 0) {
            Log.e(TAG, "ERROR: Cannot load boarding house details - bh_id is 0");
            Toast.makeText(this, "Error: Boarding house ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String url = GET_BH_DETAILS_URL + "?bh_id=" + bhId;
        Log.d(TAG, "Loading boarding house details from URL: " + url);
        
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Log.d(TAG, "=== API Response received ===");
                            Log.d(TAG, "Response: " + response);
                            
                            JSONObject jsonResponse = new JSONObject(response);
                            if (jsonResponse.getBoolean("success")) {
                                JSONObject data = jsonResponse.getJSONObject("data");
                                JSONObject bh = data.getJSONObject("boarding_house");
                                
                                // Log all keys in response for debugging
                                Log.d(TAG, "=== Boarding House Keys ===");
                                if (bh.length() > 0) {
                                    Iterator<String> keys = bh.keys();
                                    while (keys.hasNext()) {
                                        String key = keys.next();
                                        Log.d(TAG, "Key: " + key);
                                    }
                                }
                                
                                // Display BH name
                                String bhName = bh.optString("bh_name", "Boarding House");
                                if (bhName != null && !bhName.isEmpty() && !bhName.equals("Boarding House Name")) {
                                    tvBhName.setText(bhName);
                                    Log.d(TAG, "BH Name set: " + bhName);
                                } else {
                                    Log.e(TAG, "BH Name is empty or default, keeping placeholder");
                                }
                                
                                // Store GCash QR code from boarding house details (if available)
                                if (bh.has("gcash_qr") && !bh.isNull("gcash_qr")) {
                                    String qrPath = bh.optString("gcash_qr", "");
                                    Log.d(TAG, "GCash QR from response: '" + qrPath + "'");
                                    if (qrPath != null && !qrPath.isEmpty() && !qrPath.equals("null")) {
                                        // If it's already a full URL, use it; otherwise it should already be formatted by PHP
                                        ownerGcashQrPath = qrPath;
                                        Log.d(TAG, "GCash QR stored: " + ownerGcashQrPath);
                                    } else {
                                        Log.d(TAG, "GCash QR is null or empty in response");
                                        ownerGcashQrPath = null;
                                    }
                                } else {
                                    Log.d(TAG, "GCash QR field not found in boarding house response");
                                    ownerGcashQrPath = null;
                                }
                                
                                // Store GCash number from boarding house details (if available)
                                if (bh.has("gcash_number") && !bh.isNull("gcash_number")) {
                                    String gcashNum = bh.optString("gcash_number", "");
                                    Log.d(TAG, "GCash Number from response: '" + gcashNum + "'");
                                    if (gcashNum != null && !gcashNum.isEmpty() && !gcashNum.equals("null")) {
                                        ownerGcashNumber = gcashNum;
                                        Log.d(TAG, "GCash Number stored: " + ownerGcashNumber);
                                    } else {
                                        Log.d(TAG, "GCash Number is null or empty in response");
                                        ownerGcashNumber = null;
                                    }
                                } else {
                                    Log.d(TAG, "GCash Number not in main object, checking owner object");
                                }
                                
                                // Try getting from owner object if not in main object
                                if ((ownerGcashNumber == null || ownerGcashNumber.isEmpty()) && bh.has("owner")) {
                                    JSONObject owner = bh.getJSONObject("owner");
                                    if (owner.has("gcash_number") && !owner.isNull("gcash_number")) {
                                        String gcashNum = owner.optString("gcash_number", "");
                                        if (gcashNum != null && !gcashNum.isEmpty() && !gcashNum.equals("null")) {
                                            ownerGcashNumber = gcashNum;
                                            Log.d(TAG, "GCash Number from owner object: " + ownerGcashNumber);
                                        }
                                    }
                                }
                                
                                // If GCash is selected, load QR immediately
                                if (paymentMethod.equals("GCash") && layoutGcashPayment.getVisibility() == View.VISIBLE) {
                                    loadOwnerGcashQr();
                                }
                                
                                // Load BH image
                                if (bh.has("images") && !bh.isNull("images")) {
                                    org.json.JSONArray images = bh.getJSONArray("images");
                                    Log.d(TAG, "Number of images: " + images.length());
                                    if (images.length() > 0) {
                                        String imageUrl = images.getString(0);
                                        Log.d(TAG, "Loading BH image from URL: " + imageUrl);
                                        
                                        // URL should already be complete from PHP, but verify
                                        String fullImageUrl = imageUrl;
                                        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                                            // If it's a relative path, prepend base URL
                                            if (imageUrl.startsWith("uploads/")) {
                                                fullImageUrl = BOARD_EASE2_URL + imageUrl;
                                            } else {
                                                fullImageUrl = BOARD_EASE2_URL + "uploads/" + imageUrl;
                                            }
                                        }
                                        
                                        Log.d(TAG, "Full image URL: " + fullImageUrl);
                                        Glide.with(FinalBookingActivity.this)
                                                .load(fullImageUrl)
                                                .placeholder(R.drawable.sample_listing)
                                                .error(R.drawable.sample_listing)
                                                .into(ivBhImage);
                                    } else {
                                        Log.d(TAG, "No images found in response");
                                        ivBhImage.setImageResource(R.drawable.sample_listing);
                                    }
                                } else {
                                    Log.d(TAG, "Images field not found or is null");
                                    ivBhImage.setImageResource(R.drawable.sample_listing);
                                }
                            } else {
                                Log.e(TAG, "API returned success=false");
                                String error = jsonResponse.optString("error", "Unknown error");
                                Log.e(TAG, "Error: " + error);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing BH details: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error loading BH details: " + error.getMessage());
                        Log.e(TAG, "Error details: " + (error.networkResponse != null ? 
                            "Status: " + error.networkResponse.statusCode : "No network response"));
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            String errorBody = new String(error.networkResponse.data);
                            Log.e(TAG, "Error body: " + errorBody);
                        }
                        Toast.makeText(FinalBookingActivity.this, "Error loading boarding house details", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", "BoardEase-Android-App");
                headers.put("Accept", "application/json");
                return headers;
            }
        };
        
        requestQueue.add(stringRequest);
    }
    
    private void loadOwnerGcashQr() {
        Log.d(TAG, "=== loadOwnerGcashQr called ===");
        Log.d(TAG, "ownerGcashQrPath: " + ownerGcashQrPath);
        Log.d(TAG, "ownerGcashNumber: " + ownerGcashNumber);
        
        // Display GCash number if available
        if (ownerGcashNumber != null && !ownerGcashNumber.isEmpty() && !ownerGcashNumber.equals("null")) {
            tvGcashNumber.setText("GCash: " + ownerGcashNumber);
            tvGcashNumber.setVisibility(View.VISIBLE);
            Log.d(TAG, "✓ Displaying GCash number: " + ownerGcashNumber);
        } else {
            tvGcashNumber.setVisibility(View.GONE);
            Log.d(TAG, "✗ GCash number not available");
        }
        
        // Use the QR code that was already fetched from boarding house details
        if (ownerGcashQrPath != null && !ownerGcashQrPath.isEmpty() && !ownerGcashQrPath.equals("null")) {
            // URL should already be complete from PHP, but verify
            String fullImageUrl = ownerGcashQrPath;
            if (!ownerGcashQrPath.startsWith("http://") && !ownerGcashQrPath.startsWith("https://")) {
                // If it's a relative path, prepend base URL
                String cleanPath = ownerGcashQrPath.startsWith("/") ? ownerGcashQrPath.substring(1) : ownerGcashQrPath;
                if (cleanPath.startsWith("uploads/")) {
                    fullImageUrl = BOARD_EASE2_URL + cleanPath;
                } else {
                    fullImageUrl = BOARD_EASE2_URL + "uploads/" + cleanPath;
                }
            }
            
            Log.d(TAG, "Loading GCash QR from URL: " + fullImageUrl);
            Glide.with(FinalBookingActivity.this)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .into(ivOwnerQrCode);
            Log.d(TAG, "✓ GCash QR image loading started");
        } else {
            Log.e(TAG, "✗ GCash QR not available - path: '" + ownerGcashQrPath + "'");
            ivOwnerQrCode.setImageResource(R.drawable.placeholder);
            Toast.makeText(FinalBookingActivity.this, "Owner GCash QR not available", Toast.LENGTH_SHORT).show();
        }
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
    
    // Flag to prevent multiple simultaneous booking requests
    private boolean isBookingInProgress = false;
    // Track request count to detect duplicate requests
    private int requestCount = 0;
    
    private void createBooking() {
        // Prevent multiple simultaneous requests
        if (isBookingInProgress) {
            Log.w(TAG, "WARNING: Booking already in progress, ignoring duplicate request");
            Log.w(TAG, "  - This should not happen if button is properly disabled");
            return;
        }
        
        requestCount++;
        Log.d(TAG, "========================================");
        Log.d(TAG, "=== BOOKING PROCESS START (Android) ===");
        Log.d(TAG, "=== Request Count: " + requestCount + " ===");
        Log.d(TAG, "========================================");
        
        isBookingInProgress = true;
        progressBar.setVisibility(View.VISIBLE);
        btnBook.setEnabled(false);
        btnBook.setClickable(false); // Also disable clicks
        
        Log.d(TAG, "Step 1: Preparing booking data...");
        Log.d(TAG, "  - Room ID: " + roomId);
        Log.d(TAG, "  - User ID: " + userId);
        Log.d(TAG, "  - Start Date: " + startDate);
        Log.d(TAG, "  - End Date: " + endDate);
        Log.d(TAG, "  - Payment Method: " + paymentMethod);
        
        // Convert image to base64
        final String paymentProofBase64;
        try {
            if (paymentMethod.equals("Cash") && cashProofUri != null) {
                Log.d(TAG, "Step 2: Converting Cash proof image to base64...");
                paymentProofBase64 = imageToBase64(cashProofUri);
                Log.d(TAG, "  - Cash proof converted. Length: " + (paymentProofBase64 != null ? paymentProofBase64.length() : 0) + " chars");
            } else if (paymentMethod.equals("GCash") && gcashProofUri != null) {
                Log.d(TAG, "Step 2: Converting GCash proof image to base64...");
                paymentProofBase64 = imageToBase64(gcashProofUri);
                Log.d(TAG, "  - GCash proof converted. Length: " + (paymentProofBase64 != null ? paymentProofBase64.length() : 0) + " chars");
            } else {
                paymentProofBase64 = "";
                Log.d(TAG, "Step 2: No payment proof image (empty)");
            }
        } catch (IOException e) {
            Log.e(TAG, "ERROR: Failed to convert image to base64");
            Log.e(TAG, "  - Error: " + e.getMessage());
            isBookingInProgress = false; // Reset flag
            progressBar.setVisibility(View.GONE);
            btnBook.setEnabled(true);
            btnBook.setClickable(true);
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "Step 3: Sending booking request to server...");
        Log.d(TAG, "  - URL: " + CREATE_BOOKING_URL);
        
        StringRequest stringRequest = new StringRequest(Request.Method.POST, CREATE_BOOKING_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "========================================");
                        Log.d(TAG, "=== SERVER RESPONSE RECEIVED ===");
                        Log.d(TAG, "========================================");
                        Log.d(TAG, "Raw response: " + response);
                        
                        // Reset booking flag
                        isBookingInProgress = false;
                        progressBar.setVisibility(View.GONE);
                        btnBook.setEnabled(true);
                        
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success");
                            String message = jsonResponse.optString("message", "");
                            int bookingId = jsonResponse.optInt("booking_id", 0);
                            
                            Log.d(TAG, "Parsed response:");
                            Log.d(TAG, "  - Success: " + success);
                            Log.d(TAG, "  - Message: " + message);
                            if (bookingId > 0) {
                                Log.d(TAG, "  - Booking ID: " + bookingId);
                            }
                            
                            if (success) {
                                Log.d(TAG, "========================================");
                                Log.d(TAG, "=== BOOKING SUCCESSFUL ===");
                                Log.d(TAG, "========================================");
                                Log.d(TAG, "Booking ID: " + bookingId);
                                Log.d(TAG, "Message: " + message);
                                showSuccessDialog();
                            } else {
                                Log.e(TAG, "========================================");
                                Log.e(TAG, "=== BOOKING UNSUCCESSFUL ===");
                                Log.e(TAG, "========================================");
                                Log.e(TAG, "Error Message: " + message);
                                
                                // Check if it's a user not found error
                                if (message.contains("User not found")) {
                                    Log.e(TAG, "User not found - clearing session");
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
                                    Log.e(TAG, "Showing error dialog with message: " + message);
                                    showErrorDialog("Unsuccessful: " + message);
                                }
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "========================================");
                            Log.e(TAG, "=== JSON PARSING ERROR ===");
                            Log.e(TAG, "========================================");
                            Log.e(TAG, "Error: " + e.getMessage());
                            Log.e(TAG, "Response that failed to parse: " + response);
                            e.printStackTrace();
                            showErrorDialog("Error parsing response: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "========================================");
                        Log.e(TAG, "=== NETWORK ERROR ===");
                        Log.e(TAG, "========================================");
                        
                        // Reset booking flag
                        isBookingInProgress = false;
                        progressBar.setVisibility(View.GONE);
                        btnBook.setEnabled(true);
                        btnBook.setClickable(true);
                        String errorMessage = "Network error";
                        if (error.networkResponse != null) {
                            errorMessage = "Error " + error.networkResponse.statusCode;
                            Log.e(TAG, "  - Status Code: " + error.networkResponse.statusCode);
                            if (error.networkResponse.data != null) {
                                String errorBody = new String(error.networkResponse.data);
                                errorMessage += ": " + errorBody;
                                Log.e(TAG, "  - Error Body: " + errorBody);
                            }
                        } else if (error.getMessage() != null) {
                            errorMessage = error.getMessage();
                            Log.e(TAG, "  - Error Message: " + errorMessage);
                        }
                        Log.e(TAG, "  - Error Class: " + error.getClass().getSimpleName());
                        if (error.getCause() != null) {
                            Log.e(TAG, "  - Cause: " + error.getCause().getMessage());
                        }
                        showErrorDialog("Unsuccessful: " + errorMessage);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                // NOTE: getParams() may be called multiple times by Volley internally
                // This is normal - it doesn't mean the request is sent multiple times
                // We log it but don't worry about it unless we see actual duplicate requests
                Map<String, String> params = new HashMap<>();
                params.put("room_id", String.valueOf(roomId));
                params.put("user_id", String.valueOf(userId));
                params.put("start_date", startDate);
                params.put("end_date", endDate);
                params.put("payment_method", paymentMethod);
                params.put("payment_proof", paymentProofBase64);
                params.put("total_amount", String.format(Locale.getDefault(), "%.2f", totalPaymentAmount));
                params.put("number_of_days", String.valueOf(numberOfDays));
                
                // Add payment breakdown JSON
                try {
                    org.json.JSONArray breakdownArray = new org.json.JSONArray();
                    for (PaymentPeriod period : paymentPeriods) {
                        org.json.JSONObject periodObj = new org.json.JSONObject();
                        periodObj.put("label", period.label);
                        periodObj.put("period_type", period.periodType);
                        periodObj.put("period_number", period.periodNumber);
                        periodObj.put("amount", period.amount);
                        periodObj.put("start_date", period.startDate);
                        periodObj.put("end_date", period.endDate);
                        periodObj.put("is_selected", period.isSelected);
                        breakdownArray.put(periodObj);
                    }
                    params.put("payment_breakdown", breakdownArray.toString());
                    Log.d(TAG, "  - payment_breakdown: " + breakdownArray.toString());
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "Error creating payment breakdown JSON: " + e.getMessage());
                }
                
                // Only log once per actual request (use a counter or flag)
                // Note: This may still log multiple times due to Volley internals
                Log.d(TAG, "getParams() called - Preparing request parameters");
                Log.d(TAG, "  - room_id: " + roomId);
                Log.d(TAG, "  - user_id: " + userId);
                Log.d(TAG, "  - start_date: " + startDate);
                Log.d(TAG, "  - end_date: " + endDate);
                Log.d(TAG, "  - payment_method: " + paymentMethod);
                Log.d(TAG, "  - total_amount: " + totalPaymentAmount);
                Log.d(TAG, "  - number_of_days: " + numberOfDays);
                Log.d(TAG, "  - payment_proof length: " + (paymentProofBase64 != null ? paymentProofBase64.length() : 0) + " chars");
                
                return params;
            }
            
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", "BoardEase-Android-App");
                headers.put("Accept", "application/json");
                return headers;
            }
        };
        
        // Set retry policy to prevent automatic retries (Volley retries by default)
        // This prevents duplicate requests when network is slow
        stringRequest.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
            30000, // 30 seconds timeout
            0, // NO retries (0 = don't retry, prevent duplicate requests)
            com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        
        Log.d(TAG, "Adding request to Volley queue (should only happen once per booking)");
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
        // Inflate custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_booking_success, null);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
            navigateToBoardingHouseDetails();
        });
        builder.setCancelable(false);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Style the button
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.brown));
    }
    
    private void navigateToBoardingHouseDetails() {
        try {
            if (bhId > 0) {
                Log.d(TAG, "Navigating to BoardingHouseDetailsActivity with bh_id: " + bhId);
                
                // Navigate to BoardingHouseDetailsActivity with the boarding house ID
                Intent intent = new Intent(FinalBookingActivity.this, BoardingHouseDetailsActivity.class);
                intent.putExtra("bh_id", bhId);
                
                // Clear all activities on top of BoardingHouseDetailsActivity if it exists in the back stack
                // This will remove BookingActivity and FinalBookingActivity from the stack
                // If BoardingHouseDetailsActivity doesn't exist in stack, it will start fresh
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                
                startActivity(intent);
                
                // Finish this activity (FinalBookingActivity)
                // The FLAG_ACTIVITY_CLEAR_TOP will handle clearing activities above BoardingHouseDetailsActivity
                finish();
            } else {
                // If bhId is not available, just finish and go back
                Log.e(TAG, "bhId is 0, cannot navigate to BoardingHouseDetailsActivity");
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to BoardingHouseDetailsActivity: " + e.getMessage());
            e.printStackTrace();
            // Fallback: just finish the activity
            finish();
        }
    }
    
    private void showErrorDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Booking Unsuccessful");
        builder.setMessage("Unsuccessful\n\nError Details: " + message);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}

