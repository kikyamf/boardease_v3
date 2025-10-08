package com.example.mock;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class BoardingHouseDetailsActivitySimple extends AppCompatActivity {
    
    private ImageButton btnBack, btnShare;
    private MaterialButton btnChooseAccommodation;
    private TextView tvBoardingHouseName, tvLocation, tvPrice, tvDescription;
    
    private int boardingHouseId;
    private String boardingHouseName;
    private String boardingHouseImage;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boarding_house_details);
        
        // Get data from intent
        getIntentData();
        
        // Initialize views
        initializeViews();
        
        // Setup click listeners
        setupClickListeners();
        
        // Load boarding house details
        loadBoardingHouseDetails();
    }
    
    private void getIntentData() {
        Intent intent = getIntent();
        boardingHouseId = intent.getIntExtra("boarding_house_id", 0);
        boardingHouseName = intent.getStringExtra("boarding_house_name");
        boardingHouseImage = intent.getStringExtra("boarding_house_image");
    }
    
    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        btnShare = findViewById(R.id.btnShare);
        btnChooseAccommodation = findViewById(R.id.btnChooseAccommodation);
        
        tvBoardingHouseName = findViewById(R.id.tvBoardingHouseName);
        tvLocation = findViewById(R.id.tvLocation);
        tvPrice = findViewById(R.id.tvPrice);
        tvDescription = findViewById(R.id.tvDescription);
    }
    
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnShare.setOnClickListener(v -> {
            // TODO: Implement share functionality
        });
        
        btnChooseAccommodation.setOnClickListener(v -> {
            // TODO: Implement accommodation selection
        });
    }
    
    private void loadBoardingHouseDetails() {
        // Set basic information
        if (boardingHouseName != null) {
            tvBoardingHouseName.setText(boardingHouseName);
        }
        
        // Set sample data (replace with actual API call)
        tvLocation.setText("Quezon City, Metro Manila");
        tvPrice.setText("₱3,500/month");
        tvDescription.setText("A modern and comfortable boarding house located in the heart of Quezon City. Perfect for students and working professionals who value convenience and affordability.");
    }
}

