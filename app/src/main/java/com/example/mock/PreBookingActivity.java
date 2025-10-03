package com.example.mock;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import java.util.Calendar;

public class PreBookingActivity extends AppCompatActivity {
    
    private ImageButton btnBack;
    private ImageView imgBoardingHouse;
    private TextView tvBoardingHouseName, tvBoardingHouseDescription;
    private MaterialButton btnSeeMoreDetails;
    
    private TextView tvAccommodationType, tvAccommodationPrice, tvAccommodationDescription;
    private MaterialButton btnToggleAccommodationDetails;
    private LinearLayout layoutAccommodationDetails;
    private boolean isAccommodationExpanded = false;
    
    private TextView tvFullName;
    private MaterialButton btnCheckInDate, btnCheckOutDate, btnFinalStep;
    
    // Data from previous activity
    private int boardingHouseId;
    private String boardingHouseName;
    private String boardingHouseImage;
    private String accommodationType;
    private String accommodationPrice;
    private String accommodationDescription;
    private String fullAccommodationDetails;
    
    // Date variables
    private Calendar checkInDate;
    private Calendar checkOutDate;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prebooking);
        
        // Get data from intent
        getIntentData();
        
        // Initialize views
        initializeViews();
        
        // Setup click listeners
        setupClickListeners();
        
        // Load data
        loadData();
    }
    
    private void getIntentData() {
        Intent intent = getIntent();
        boardingHouseId = intent.getIntExtra("boarding_house_id", 0);
        boardingHouseName = intent.getStringExtra("boarding_house_name");
        boardingHouseImage = intent.getStringExtra("boarding_house_image");
        accommodationType = intent.getStringExtra("accommodation_type");
        accommodationPrice = intent.getStringExtra("accommodation_price");
        accommodationDescription = intent.getStringExtra("accommodation_description");
        fullAccommodationDetails = intent.getStringExtra("full_accommodation_details");
    }
    
    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        imgBoardingHouse = findViewById(R.id.imgBoardingHouse);
        tvBoardingHouseName = findViewById(R.id.tvBoardingHouseName);
        tvBoardingHouseDescription = findViewById(R.id.tvBoardingHouseDescription);
        btnSeeMoreDetails = findViewById(R.id.btnSeeMoreDetails);
        
        tvAccommodationType = findViewById(R.id.tvAccommodationType);
        tvAccommodationPrice = findViewById(R.id.tvAccommodationPrice);
        tvAccommodationDescription = findViewById(R.id.tvAccommodationDescription);
        btnToggleAccommodationDetails = findViewById(R.id.btnToggleAccommodationDetails);
        layoutAccommodationDetails = findViewById(R.id.layoutAccommodationDetails);
        
        tvFullName = findViewById(R.id.tvFullName);
        btnCheckInDate = findViewById(R.id.btnCheckInDate);
        btnCheckOutDate = findViewById(R.id.btnCheckOutDate);
        btnFinalStep = findViewById(R.id.btnFinalStep);
    }
    
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnSeeMoreDetails.setOnClickListener(v -> {
            // Navigate back to Boarding House Details page
            Intent intent = new Intent(this, BoardingHouseDetailsActivity.class);
            intent.putExtra("boarding_house_id", boardingHouseId);
            intent.putExtra("boarding_house_name", boardingHouseName);
            intent.putExtra("boarding_house_image", boardingHouseImage);
            startActivity(intent);
        });
        
        btnToggleAccommodationDetails.setOnClickListener(v -> toggleAccommodationDetails());
        
        btnCheckInDate.setOnClickListener(v -> showDatePicker(true));
        btnCheckOutDate.setOnClickListener(v -> showDatePicker(false));
        
        btnFinalStep.setOnClickListener(v -> {
            if (validateDates()) {
                showBookingDetailsModal();
            }
        });
    }
    
    private void toggleAccommodationDetails() {
        if (isAccommodationExpanded) {
            // Collapse
            layoutAccommodationDetails.setVisibility(View.GONE);
            btnToggleAccommodationDetails.setText("See More");
            isAccommodationExpanded = false;
        } else {
            // Expand
            layoutAccommodationDetails.setVisibility(View.VISIBLE);
            btnToggleAccommodationDetails.setText("See Less");
            isAccommodationExpanded = true;
        }
    }
    
    private void showDatePicker(boolean isCheckIn) {
        Calendar currentDate = Calendar.getInstance();
        if (isCheckIn && checkInDate != null) {
            currentDate = checkInDate;
        } else if (!isCheckIn && checkOutDate != null) {
            currentDate = checkOutDate;
        }
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.set(year, month, dayOfMonth);
                
                if (isCheckIn) {
                    checkInDate = selectedDate;
                    btnCheckInDate.setText(String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year));
                } else {
                    checkOutDate = selectedDate;
                    btnCheckOutDate.setText(String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year));
                }
            },
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH),
            currentDate.get(Calendar.DAY_OF_MONTH)
        );
        
        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        
        datePickerDialog.show();
    }
    
    private boolean validateDates() {
        if (checkInDate == null) {
            // Show error message
            return false;
        }
        
        if (checkOutDate == null) {
            // Show error message
            return false;
        }
        
        if (checkOutDate.before(checkInDate) || checkOutDate.equals(checkInDate)) {
            // Show error message - check-out must be after check-in
            return false;
        }
        
        return true;
    }
    
    private void showBookingDetailsModal() {
        // TODO: Show booking details modal
        // For now, navigate directly to payment page
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("boarding_house_id", boardingHouseId);
        intent.putExtra("boarding_house_name", boardingHouseName);
        intent.putExtra("accommodation_type", accommodationType);
        intent.putExtra("accommodation_price", accommodationPrice);
        intent.putExtra("check_in_date", checkInDate.getTimeInMillis());
        intent.putExtra("check_out_date", checkOutDate.getTimeInMillis());
        startActivity(intent);
    }
    
    private void loadData() {
        // Set boarding house info
        if (boardingHouseName != null) {
            tvBoardingHouseName.setText(boardingHouseName);
        }
        
        // Set accommodation info
        if (accommodationType != null) {
            tvAccommodationType.setText(accommodationType);
        }
        
        if (accommodationPrice != null) {
            tvAccommodationPrice.setText(accommodationPrice);
        }
        
        if (accommodationDescription != null) {
            tvAccommodationDescription.setText(accommodationDescription);
        }
        
        // TODO: Load user's full name from profile
        tvFullName.setText("John Doe"); // Placeholder
        
        // TODO: Load boarding house image
        imgBoardingHouse.setImageResource(R.drawable.sample_listing);
    }
}

