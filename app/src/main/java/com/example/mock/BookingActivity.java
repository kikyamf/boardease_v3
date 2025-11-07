package com.example.mock;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BookingActivity extends AppCompatActivity {
    
    private static final String TAG = "BookingActivity";
    private static final String BASE_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/";
    private static final String BOOKING_API_URL = BASE_URL + "create_booking.php";
    private static final String COUNTRIES_API_URL = "https://restcountries.com/v3.1/all?fields=name";
    
    // Views
    private ImageButton btnBack;
    private TextView tvRoomName, tvRoomDescription, tvRoomPrice, tvRoomCapacity;
    private TextInputEditText etStartDate, etEndDate, etFirstName, etLastName, etEmail, etPhone;
    private AutoCompleteTextView actvCountry;
    private MaterialButton btnProceed;
    private ProgressBar progressBar;
    
    // Data
    private int roomId;
    private int userId;
    private JSONObject roomData;
    private Calendar startDateCalendar;
    private Calendar endDateCalendar;
    private SimpleDateFormat dateFormat;
    private RequestQueue requestQueue;
    private List<String> countryList;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);
        
        // Initialize
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        startDateCalendar = Calendar.getInstance();
        endDateCalendar = Calendar.getInstance();
        requestQueue = Volley.newRequestQueue(this);
        countryList = new ArrayList<>();
        
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
        
        // Autofill user information
        autofillUserInfo();
        
        // Load countries
        loadCountries();
    }
    
    private void getIntentData() {
        Intent intent = getIntent();
        roomId = intent.getIntExtra("bhr_id", 0);
        String roomDataString = intent.getStringExtra("room_data");
        
        if (roomId == 0 || roomDataString == null) {
            Toast.makeText(this, "Invalid room data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        try {
            roomData = new JSONObject(roomDataString);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing room data: " + e.getMessage());
            Toast.makeText(this, "Error loading room data", Toast.LENGTH_SHORT).show();
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
        actvCountry = findViewById(R.id.actvCountry);
        btnProceed = findViewById(R.id.btnProceed);
        progressBar = findViewById(R.id.progressBar);
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
                createBooking();
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
    
    private void loadCountries() {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, COUNTRIES_API_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONArray countriesArray = new JSONArray(response);
                            countryList.clear();
                            
                            for (int i = 0; i < countriesArray.length(); i++) {
                                JSONObject country = countriesArray.getJSONObject(i);
                                JSONObject name = country.getJSONObject("name");
                                String countryName = name.getString("common");
                                countryList.add(countryName);
                            }
                            
                            // Sort countries alphabetically
                            countryList.sort(String::compareToIgnoreCase);
                            
                            // Set up AutoCompleteTextView
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                    BookingActivity.this,
                                    android.R.layout.simple_dropdown_item_1line,
                                    countryList
                            );
                            actvCountry.setAdapter(adapter);
                            actvCountry.setThreshold(1);
                            
                            Log.d(TAG, "Loaded " + countryList.size() + " countries");
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing countries: " + e.getMessage());
                            // Fallback to common countries
                            loadFallbackCountries();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error loading countries: " + error.getMessage());
                        // Fallback to common countries
                        loadFallbackCountries();
                    }
                });
        
        requestQueue.add(stringRequest);
    }
    
    private void loadFallbackCountries() {
        countryList.clear();
        countryList.add("Philippines");
        countryList.add("United States");
        countryList.add("United Kingdom");
        countryList.add("Canada");
        countryList.add("Australia");
        countryList.add("Japan");
        countryList.add("South Korea");
        countryList.add("Singapore");
        countryList.add("Malaysia");
        countryList.add("Thailand");
        countryList.add("Indonesia");
        countryList.add("Vietnam");
        countryList.add("India");
        countryList.add("China");
        countryList.add("Germany");
        countryList.add("France");
        countryList.add("Spain");
        countryList.add("Italy");
        countryList.add("New Zealand");
        countryList.add("Other");
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                countryList
        );
        actvCountry.setAdapter(adapter);
        actvCountry.setThreshold(1);
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
        
        // Validate country
        if (actvCountry.getText().toString().trim().isEmpty()) {
            actvCountry.setError("Country is required");
            isValid = false;
        }
        
        return isValid;
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
                // Note: first_name, last_name, email, phone, country are validated but not stored in bookings table
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

