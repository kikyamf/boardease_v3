package com.example.mock;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.example.mock.adapters.ImageCarouselAdapter;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class PreBookingPhase1Activity extends AppCompatActivity {
    
    private ViewPager2 viewPagerImages;
    private LinearLayout layoutIndicators;
    private ImageButton btnBack, btnShare, btnFavorite;
    
    private TextView tvBoardingHouseName;
    
    // Private Room Components
    private MaterialButton btnPrivateRoomToggle, btnBookPrivateRoom;
    private LinearLayout layoutPrivateRoomDetails;
    private boolean isPrivateRoomExpanded = false;
    
    // Bed Spacer Components
    private MaterialButton btnBedSpacerToggle, btnBookBedSpacer;
    private LinearLayout layoutBedSpacerDetails;
    private boolean isBedSpacerExpanded = false;
    
    private ImageCarouselAdapter imageAdapter;
    private List<String> imageUrls;
    
    private int boardingHouseId;
    private String boardingHouseName;
    private String boardingHouseImage;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prebooking_phase1);
        
        // Get data from intent
        getIntentData();
        
        // Initialize views
        initializeViews();
        
        // Setup image carousel
        setupImageCarousel();
        
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
        viewPagerImages = findViewById(R.id.viewPagerImages);
        layoutIndicators = findViewById(R.id.layoutIndicators);
        btnBack = findViewById(R.id.btnBack);
        btnShare = findViewById(R.id.btnShare);
        btnFavorite = findViewById(R.id.btnFavorite);
        
        tvBoardingHouseName = findViewById(R.id.tvBoardingHouseName);
        
        // Private Room views
        btnPrivateRoomToggle = findViewById(R.id.btnPrivateRoomToggle);
        btnBookPrivateRoom = findViewById(R.id.btnBookPrivateRoom);
        layoutPrivateRoomDetails = findViewById(R.id.layoutPrivateRoomDetails);
        
        // Bed Spacer views
        btnBedSpacerToggle = findViewById(R.id.btnBedSpacerToggle);
        btnBookBedSpacer = findViewById(R.id.btnBookBedSpacer);
        layoutBedSpacerDetails = findViewById(R.id.layoutBedSpacerDetails);
    }
    
    private void setupImageCarousel() {
        try {
            // Create sample image URLs (replace with actual data)
            imageUrls = new ArrayList<>();
            imageUrls.add("sample_listing");
            imageUrls.add("carousel1");
            imageUrls.add("carousel2");
            imageUrls.add("carousel3");
            
            imageAdapter = new ImageCarouselAdapter(imageUrls);
            viewPagerImages.setAdapter(imageAdapter);
            
            // Setup page change listener for indicators
            viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    updateIndicators(position);
                }
            });
            
            // Create indicators
            createIndicators();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void createIndicators() {
        try {
            layoutIndicators.removeAllViews();
            
            for (int i = 0; i < imageUrls.size(); i++) {
                ImageView indicator = new ImageView(this);
                indicator.setImageResource(R.drawable.dot_inactive);
                indicator.setPadding(8, 0, 8, 0);
                layoutIndicators.addView(indicator);
            }
            
            // Set first indicator as active
            if (imageUrls.size() > 0) {
                updateIndicators(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void updateIndicators(int position) {
        try {
            for (int i = 0; i < layoutIndicators.getChildCount(); i++) {
                ImageView indicator = (ImageView) layoutIndicators.getChildAt(i);
                if (i == position) {
                    indicator.setImageResource(R.drawable.dot_active);
                } else {
                    indicator.setImageResource(R.drawable.dot_inactive);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnShare.setOnClickListener(v -> {
            // TODO: Implement share functionality
        });
        
        btnFavorite.setOnClickListener(v -> {
            // TODO: Implement favorite functionality
        });
        
        // Private Room Toggle
        btnPrivateRoomToggle.setOnClickListener(v -> togglePrivateRoomDetails());
        
        // Bed Spacer Toggle
        btnBedSpacerToggle.setOnClickListener(v -> toggleBedSpacerDetails());
        
        // Book Buttons
        btnBookPrivateRoom.setOnClickListener(v -> {
            Intent intent = new Intent(this, PreBookingActivity.class);
            intent.putExtra("boarding_house_id", boardingHouseId);
            intent.putExtra("boarding_house_name", boardingHouseName);
            intent.putExtra("boarding_house_image", boardingHouseImage);
            intent.putExtra("accommodation_type", "Private Room");
            intent.putExtra("accommodation_price", "₱5,000/month");
            intent.putExtra("accommodation_description", "Single occupancy with private bathroom and essential amenities.");
            intent.putExtra("full_accommodation_details", "• Private bedroom with lockable door\n• Private bathroom with hot and cold water\n• Study desk and chair\n• Wardrobe and storage space\n• Air conditioning unit\n• Free WiFi access\n• Access to common kitchen area\n• Laundry facilities available\n• 24/7 security and CCTV monitoring\n• Near public transportation");
            startActivity(intent);
        });
        
        btnBookBedSpacer.setOnClickListener(v -> {
            Intent intent = new Intent(this, PreBookingActivity.class);
            intent.putExtra("boarding_house_id", boardingHouseId);
            intent.putExtra("boarding_house_name", boardingHouseName);
            intent.putExtra("boarding_house_image", boardingHouseImage);
            intent.putExtra("accommodation_type", "Bed Spacer");
            intent.putExtra("accommodation_price", "₱3,500/month");
            intent.putExtra("accommodation_description", "Shared room with common bathroom and shared facilities.");
            intent.putExtra("full_accommodation_details", "• Shared bedroom with 2-4 occupants\n• Shared bathroom with hot and cold water\n• Personal storage locker\n• Shared study area\n• Ceiling fan for ventilation\n• Free WiFi access\n• Access to common kitchen area\n• Laundry facilities available\n• 24/7 security and CCTV monitoring\n• Near public transportation\n• Perfect for budget-conscious students");
            startActivity(intent);
        });
    }
    
    private void togglePrivateRoomDetails() {
        if (isPrivateRoomExpanded) {
            // Collapse
            layoutPrivateRoomDetails.setVisibility(View.GONE);
            btnPrivateRoomToggle.setText("See More");
            isPrivateRoomExpanded = false;
        } else {
            // Expand
            layoutPrivateRoomDetails.setVisibility(View.VISIBLE);
            btnPrivateRoomToggle.setText("See Less");
            isPrivateRoomExpanded = true;
        }
    }
    
    private void toggleBedSpacerDetails() {
        if (isBedSpacerExpanded) {
            // Collapse
            layoutBedSpacerDetails.setVisibility(View.GONE);
            btnBedSpacerToggle.setText("See More");
            isBedSpacerExpanded = false;
        } else {
            // Expand
            layoutBedSpacerDetails.setVisibility(View.VISIBLE);
            btnBedSpacerToggle.setText("See Less");
            isBedSpacerExpanded = true;
        }
    }
    
    private void loadBoardingHouseDetails() {
        // Set basic information
        if (boardingHouseName != null) {
            tvBoardingHouseName.setText(boardingHouseName);
        }
        
        // TODO: Load actual boarding house details from API
        // This could involve:
        // 1. Making API call with boardingHouseId
        // 2. Parsing response and updating UI
        // 3. Loading images for carousel
        // 4. Loading accommodation types and availability
    }
}
