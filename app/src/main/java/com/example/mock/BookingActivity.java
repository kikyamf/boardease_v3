package com.example.mock;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BookingActivity extends AppCompatActivity {
    
    private static final String TAG = "BookingActivity";
    private static final String BASE_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/";
    private static final String BOARD_EASE2_URL = BASE_URL + "BoardEase2/";
    private static final String GET_ROOM_UNITS_URL = BOARD_EASE2_URL + "get_room_units1.php";
    private static final String BOOKING_API_URL = BOARD_EASE2_URL + "create_booking.php";
    
    // Views
    private ImageButton btnBack;
    private TextView tvRoomName, tvRoomDescription, tvRoomPrice, tvRoomCapacity;
    private TextInputEditText etStartDate, etEndDate, etFirstName, etLastName, etEmail, etPhone;
    private MaterialButton btnProceed;
    private ProgressBar progressBar;
    private RadioGroup rgRoomUnits;
    private ProgressBar progressBarRoomUnits;
    private TextView tvNoRoomUnits;
    
    private int roomId; // This is bhr_id
    private int selectedRoomUnitId; // This is room_units.room_id
    private int userId;
    private JSONObject roomData;
    private List<RoomUnitData> roomUnitsList;
    private String roomCategory; // "Private Room" or "Bed Spacer"
    private int roomCapacity; // Total capacity for Bed Spacer rooms
    
    // Room Unit data class
    private static class RoomUnitData {
        int roomId;
        String roomNumber;
        String status;
        int availableCapacity; // Available beds for Bed Spacer
        int totalCapacity; // Total capacity for Bed Spacer
        int occupiedCapacity; // Occupied beds for Bed Spacer
    }
    private Calendar startDateCalendar;
    private Calendar endDateCalendar;
    private SimpleDateFormat dateFormat;
    private RequestQueue requestQueue;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);
        
        // Initialize
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        startDateCalendar = Calendar.getInstance();
        endDateCalendar = Calendar.getInstance();
        requestQueue = Volley.newRequestQueue(this);
        
        // Get data from intent
        getIntentData();
        
        // Get user ID
        getUserId();
        
        // Initialize views
        initializeViews();
        
        // Setup click listeners
        setupClickListeners();
        
        // Load room details
        displayRoomDetails();
        
        // Load available room units
        loadRoomUnits();
        
        // Autofill user information
        autofillUserInfo();
    }
    
    private void getIntentData() {
        try {
            Intent intent = getIntent();
            if (intent == null) {
                Log.e(TAG, "Intent is null");
                Toast.makeText(this, "Invalid room data", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            roomId = intent.getIntExtra("bhr_id", 0);
            String roomDataString = intent.getStringExtra("room_data");
            
            Log.d(TAG, "Received roomId: " + roomId);
            Log.d(TAG, "Received roomDataString: " + (roomDataString != null ? roomDataString.substring(0, Math.min(100, roomDataString.length())) : "null"));
            
            if (roomId == 0 || roomDataString == null || roomDataString.isEmpty()) {
                Log.e(TAG, "Invalid room data - roomId: " + roomId + ", roomDataString: " + (roomDataString != null ? "not null" : "null"));
                Toast.makeText(this, "Invalid room data", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            try {
                roomData = new JSONObject(roomDataString);
                Log.d(TAG, "Successfully parsed room data");
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing room data: " + e.getMessage());
                Log.e(TAG, "Room data string: " + roomDataString);
                e.printStackTrace();
                Toast.makeText(this, "Error loading room data", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in getIntentData: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error loading booking data", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void getUserId() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        String userIdString = sharedPreferences.getString("user_id", null);
        
        if (userIdString != null) {
            try {
                userId = Integer.parseInt(userIdString);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing user ID: " + e.getMessage());
                userId = 0;
            }
        } else {
            userId = 0;
        }
        
        if (userId == 0) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void initializeViews() {
        try {
            btnBack = findViewById(R.id.btnBack);
            tvRoomName = findViewById(R.id.tvRoomName);
            tvRoomDescription = findViewById(R.id.tvRoomDescription);
            tvRoomPrice = findViewById(R.id.tvRoomPrice);
            tvRoomCapacity = findViewById(R.id.tvRoomCapacity);
            etStartDate = findViewById(R.id.etStartDate);
            etEndDate = findViewById(R.id.etEndDate);
            etFirstName = findViewById(R.id.etFirstName);
            etLastName = findViewById(R.id.etLastName);
            etEmail = findViewById(R.id.etEmail);
            etPhone = findViewById(R.id.etPhone);
            btnProceed = findViewById(R.id.btnProceed);
            progressBar = findViewById(R.id.progressBar);
            rgRoomUnits = findViewById(R.id.rgRoomUnits);
            progressBarRoomUnits = findViewById(R.id.progressBarRoomUnits);
            tvNoRoomUnits = findViewById(R.id.tvNoRoomUnits);
            
            // Initialize room units list
            roomUnitsList = new ArrayList<>();
            
            // Check if any view is null
            if (btnBack == null || tvRoomName == null || tvRoomDescription == null || 
                tvRoomPrice == null || tvRoomCapacity == null || etStartDate == null || 
                etEndDate == null || etFirstName == null || etLastName == null || 
                etEmail == null || etPhone == null || 
                btnProceed == null || progressBar == null) {
                Log.e(TAG, "One or more views are null");
                Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        // Start Date Picker
        etStartDate.setOnClickListener(v -> showStartDatePicker());
        
        // End Date Picker
        etEndDate.setOnClickListener(v -> showEndDatePicker());
        
        // Proceed Button
        btnProceed.setOnClickListener(v -> {
            if (validateForm()) {
                navigateToFinalBooking();
            }
        });
    }
    
    private void displayRoomDetails() {
        try {
            if (roomData != null) {
                tvRoomName.setText(roomData.optString("room_name", "Room Name"));
                tvRoomDescription.setText(roomData.optString("room_description", "No description available"));
                
                double price = roomData.optDouble("price", 0);
                tvRoomPrice.setText("₱" + String.format("%,.0f", price) + "/month");
                
                int capacity = roomData.optInt("capacity", 0);
                tvRoomCapacity.setText("Capacity: " + capacity + " person(s)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error displaying room details: " + e.getMessage());
        }
    }
    
    private void loadRoomUnits() {
        loadRoomUnits(false);
    }
    
    private void loadRoomUnits(boolean isRefresh) {
        if (roomId == 0) {
            Log.e(TAG, "Room ID is 0, cannot load room units");
            if (isRefresh && swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }
        
        if (!isRefresh) {
            progressBarRoomUnits.setVisibility(View.VISIBLE);
        }
        tvNoRoomUnits.setVisibility(View.GONE);
        rgRoomUnits.setVisibility(View.GONE);
        
        StringRequest stringRequest = new StringRequest(Request.Method.POST, GET_ROOM_UNITS_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (isRefresh && swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        } else {
                            progressBarRoomUnits.setVisibility(View.GONE);
                        }
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            if (jsonResponse.getBoolean("success")) {
                                // Get room category and capacity from response
                                roomCategory = jsonResponse.optString("room_category", "Private Room");
                                roomCapacity = jsonResponse.optInt("capacity", 1);
                                
                                JSONArray unitsArray = jsonResponse.getJSONArray("units");
                                roomUnitsList.clear();
                                
                                for (int i = 0; i < unitsArray.length(); i++) {
                                    JSONObject unitObj = unitsArray.getJSONObject(i);
                                    RoomUnitData unit = new RoomUnitData();
                                    unit.roomId = unitObj.getInt("room_id");
                                    unit.roomNumber = unitObj.getString("room_number");
                                    unit.status = unitObj.getString("status");
                                    
                                    // Parse capacity information for Bed Spacer rooms
                                    if ("Bed Spacer".equals(roomCategory)) {
                                        unit.availableCapacity = unitObj.optInt("available_capacity", 0);
                                        unit.totalCapacity = unitObj.optInt("total_capacity", roomCapacity);
                                        unit.occupiedCapacity = unitObj.optInt("occupied_capacity", 0);
                                    } else {
                                        // Private Room - no capacity info needed
                                        unit.availableCapacity = 0;
                                        unit.totalCapacity = 0;
                                        unit.occupiedCapacity = 0;
                                    }
                                    
                                    // All units returned from API are already filtered to be available
                                    // (excludes units with Pending or Confirmed bookings)
                                    roomUnitsList.add(unit);
                                }
                                
                                if (roomUnitsList.isEmpty()) {
                                    tvNoRoomUnits.setVisibility(View.VISIBLE);
                                    rgRoomUnits.setVisibility(View.GONE);
                                } else {
                                    populateRoomUnitsRadioGroup();
                                    rgRoomUnits.setVisibility(View.VISIBLE);
                                    tvNoRoomUnits.setVisibility(View.GONE);
                                }
                            } else {
                                String error = jsonResponse.optString("error", "Failed to load room units");
                                Log.e(TAG, "Error loading room units: " + error);
                                tvNoRoomUnits.setVisibility(View.VISIBLE);
                                rgRoomUnits.setVisibility(View.GONE);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing room units: " + e.getMessage());
                            tvNoRoomUnits.setVisibility(View.VISIBLE);
                            rgRoomUnits.setVisibility(View.GONE);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (isRefresh && swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        } else {
                            progressBarRoomUnits.setVisibility(View.GONE);
                        }
                        Log.e(TAG, "Error loading room units: " + error.getMessage());
                        tvNoRoomUnits.setVisibility(View.VISIBLE);
                        rgRoomUnits.setVisibility(View.GONE);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("bhr_id", String.valueOf(roomId));
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
        
        requestQueue.add(stringRequest);
    }
    
    private void refreshData() {
        // Reload room units when user pulls to refresh
        loadRoomUnits(true);
    }
    
    private void populateRoomUnitsRadioGroup() {
        rgRoomUnits.removeAllViews();
        
        for (int i = 0; i < roomUnitsList.size(); i++) {
            RoomUnitData unit = roomUnitsList.get(i);
            
            // Create RadioButton and add directly to RadioGroup (required for proper grouping)
            RadioButton radioButton = new RadioButton(this);
            radioButton.setId(View.generateViewId());
            radioButton.setText(unit.roomNumber);
            radioButton.setTextSize(16);
            radioButton.setPadding(16, 8, 16, 8);
            radioButton.setButtonTintList(getResources().getColorStateList(R.color.brown));
            radioButton.setTextColor(getResources().getColor(R.color.black));
            radioButton.setTag(unit.roomId); // Store room_id in tag
            
            // Create TextView for available bed count (for Bed Spacer only)
            TextView tvAvailability = null;
            if ("Bed Spacer".equals(roomCategory) && unit.availableCapacity > 0) {
                tvAvailability = new TextView(this);
                tvAvailability.setText(String.format(Locale.getDefault(), "%d bed(s) available", unit.availableCapacity));
                tvAvailability.setTextSize(12);
                tvAvailability.setTextColor(getResources().getColor(R.color.dark_gray));
                tvAvailability.setPadding(40, 0, 16, 8); // Indent to align with radio button text
                tvAvailability.setVisibility(View.GONE); // Initially hidden, shown when selected
            }
            
            // Select first unit by default
            if (i == 0) {
                radioButton.setChecked(true);
                selectedRoomUnitId = unit.roomId;
                // Show availability for first selected item if Bed Spacer
                if (tvAvailability != null) {
                    tvAvailability.setVisibility(View.VISIBLE);
                }
            }
            
            // Store reference to availability TextView in radio button tag
            if (tvAvailability != null) {
                // Store both roomId and availability TextView reference
                Object[] tagData = new Object[]{unit.roomId, tvAvailability};
                radioButton.setTag(tagData);
            } else {
                // Just store roomId for Private Room
                radioButton.setTag(unit.roomId);
            }
            
            radioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Object tag = buttonView.getTag();
                int roomIdValue;
                TextView availabilityText = null;
                
                // Extract data from tag
                if (tag instanceof Object[]) {
                    Object[] tagData = (Object[]) tag;
                    roomIdValue = (Integer) tagData[0];
                    availabilityText = (TextView) tagData[1];
                } else {
                    roomIdValue = (Integer) tag;
                }
                
                if (isChecked) {
                    selectedRoomUnitId = roomIdValue;
                    Log.d(TAG, "Selected room unit ID: " + selectedRoomUnitId);
                    
                    // Show availability text for selected Bed Spacer room
                    if ("Bed Spacer".equals(roomCategory)) {
                        // Hide all availability texts first
                        for (int j = 0; j < rgRoomUnits.getChildCount(); j++) {
                            View child = rgRoomUnits.getChildAt(j);
                            // Hide TextViews that are not RadioButtons (these are availability texts)
                            if (child instanceof TextView && !(child instanceof RadioButton)) {
                                child.setVisibility(View.GONE);
                            }
                        }
                        
                        // Show availability for selected radio button
                        if (availabilityText != null) {
                            availabilityText.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    // Hide availability text when unchecked
                    if ("Bed Spacer".equals(roomCategory) && availabilityText != null) {
                        availabilityText.setVisibility(View.GONE);
                    }
                }
            });
            
            // Add radio button directly to RadioGroup (required for proper grouping)
            rgRoomUnits.addView(radioButton);
            
            // Add availability text as a separate direct child of RadioGroup (after the radio button)
            if (tvAvailability != null) {
                rgRoomUnits.addView(tvAvailability);
            }
        }
    }
    
    private void autofillUserInfo() {
        // Get user info from SharedPreferences
        String firstName = Login.getCurrentUserFirstName(this);
        String lastName = Login.getCurrentUserLastName(this);
        String email = Login.getCurrentUserEmail(this);
        String phone = Login.getCurrentUserPhone(this);
        
        // If first/last name not stored separately, try to get from full name
        if ((firstName == null || firstName.isEmpty()) && (lastName == null || lastName.isEmpty())) {
            String fullName = Login.getCurrentUserName(this);
            if (fullName != null && !fullName.isEmpty()) {
                String[] nameParts = fullName.split("\\s+", 2);
                if (nameParts.length > 0) {
                    firstName = nameParts[0];
                }
                if (nameParts.length > 1) {
                    lastName = nameParts[1];
                }
            }
        }
        
        etFirstName.setText(firstName != null ? firstName : "");
        etLastName.setText(lastName != null ? lastName : "");
        etEmail.setText(email != null ? email : "");
        etPhone.setText(phone != null ? phone : "");
    }
    
    private void showStartDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    startDateCalendar.set(year, month, dayOfMonth);
                    etStartDate.setText(dateFormat.format(startDateCalendar.getTime()));
                    
                    // Set minimum date for end date picker
                    if (endDateCalendar.before(startDateCalendar)) {
                        endDateCalendar.setTimeInMillis(startDateCalendar.getTimeInMillis());
                        etEndDate.setText("");
                    }
                },
                startDateCalendar.get(Calendar.YEAR),
                startDateCalendar.get(Calendar.MONTH),
                startDateCalendar.get(Calendar.DAY_OF_MONTH)
        );
        
        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }
    
    private void showEndDatePicker() {
        if (etStartDate.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please select start date first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    endDateCalendar.set(year, month, dayOfMonth);
                    etEndDate.setText(dateFormat.format(endDateCalendar.getTime()));
                },
                endDateCalendar.get(Calendar.YEAR),
                endDateCalendar.get(Calendar.MONTH),
                endDateCalendar.get(Calendar.DAY_OF_MONTH)
        );
        
        // Set minimum date to start date
        datePickerDialog.getDatePicker().setMinDate(startDateCalendar.getTimeInMillis());
        datePickerDialog.show();
    }
    
    private boolean validateForm() {
        boolean isValid = true;
        
        // Validate room unit selection
        if (selectedRoomUnitId == 0) {
            Toast.makeText(this, "Please select a room unit", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        
        // Validate start date
        if (etStartDate.getText().toString().isEmpty()) {
            etStartDate.setError("Please select start date");
            isValid = false;
        }
        
        // Validate end date
        if (etEndDate.getText().toString().isEmpty()) {
            etEndDate.setError("Please select end date");
            isValid = false;
        }
        
        // Validate end date is after start date
        if (!etStartDate.getText().toString().isEmpty() && !etEndDate.getText().toString().isEmpty()) {
            try {
                Calendar start = Calendar.getInstance();
                start.setTime(dateFormat.parse(etStartDate.getText().toString()));
                Calendar end = Calendar.getInstance();
                end.setTime(dateFormat.parse(etEndDate.getText().toString()));
                
                if (!end.after(start)) {
                    etEndDate.setError("End date must be after start date");
                    isValid = false;
                }
            } catch (Exception e) {
                etEndDate.setError("Invalid date format");
                isValid = false;
            }
        }
        
        // Validate first name
        if (etFirstName.getText().toString().trim().isEmpty()) {
            etFirstName.setError("First name is required");
            isValid = false;
        }
        
        // Validate last name
        if (etLastName.getText().toString().trim().isEmpty()) {
            etLastName.setError("Last name is required");
            isValid = false;
        }
        
        // Validate email
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Invalid email format");
            isValid = false;
        }
        
        // Validate phone
        if (etPhone.getText().toString().trim().isEmpty()) {
            etPhone.setError("Phone number is required");
            isValid = false;
        }
        
        return isValid;
    }
    
    private void navigateToFinalBooking() {
        try {
            Intent intent = new Intent(BookingActivity.this, FinalBookingActivity.class);
            intent.putExtra("room_id", selectedRoomUnitId); // Pass room_units.room_id, not bhr_id
            intent.putExtra("bhr_id", roomId); // Also pass bhr_id for reference
            intent.putExtra("user_id", userId);
            intent.putExtra("start_date", etStartDate.getText().toString());
            intent.putExtra("end_date", etEndDate.getText().toString());
            intent.putExtra("room_data", roomData.toString());
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to FinalBookingActivity: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void createBooking() {
        progressBar.setVisibility(View.VISIBLE);
        btnProceed.setEnabled(false);
        
        StringRequest stringRequest = new StringRequest(Request.Method.POST, BOOKING_API_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressBar.setVisibility(View.GONE);
                        btnProceed.setEnabled(true);
                        
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success");
                            String message = jsonResponse.optString("message", "");
                            
                            if (success) {
                                Toast.makeText(BookingActivity.this, "Booking created successfully!", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                Toast.makeText(BookingActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing response: " + e.getMessage());
                            Toast.makeText(BookingActivity.this, "Error processing response", Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressBar.setVisibility(View.GONE);
                        btnProceed.setEnabled(true);
                        Log.e(TAG, "Error creating booking: " + error.getMessage());
                        Toast.makeText(BookingActivity.this, "Network error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("room_id", String.valueOf(roomId));
                params.put("user_id", String.valueOf(userId));
                params.put("start_date", etStartDate.getText().toString());
                params.put("end_date", etEndDate.getText().toString());
                // Note: first_name, last_name, email, phone are validated but not stored in bookings table
                // They are already in the registrations table via user_id
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
        
        requestQueue.add(stringRequest);
    }
}

