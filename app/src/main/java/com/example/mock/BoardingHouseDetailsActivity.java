package com.example.mock;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

public class BoardingHouseDetailsActivity extends AppCompatActivity {
    
    private ViewPager2 viewPagerImages;
    private LinearLayout layoutIndicators;
    private ImageButton btnBack, btnShare, btnFavorite, btnCall;
    private MaterialButton btnChooseAccommodation;
    
    private TextView tvBoardingHouseName, tvLocation, tvPrice, tvDescription;
    private LinearLayout layoutAccommodations;
    
    private ImageCarouselAdapter imageAdapter;
    private List<String> imageUrls;
    
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
        btnCall = findViewById(R.id.btnCall);
        btnChooseAccommodation = findViewById(R.id.btnChooseAccommodation);
        
        tvBoardingHouseName = findViewById(R.id.tvBoardingHouseName);
        tvLocation = findViewById(R.id.tvLocation);
        tvPrice = findViewById(R.id.tvPrice);
        tvDescription = findViewById(R.id.tvDescription);
        layoutAccommodations = findViewById(R.id.layoutAccommodations);
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
            // This could involve:
            // 1. Creating a share intent with boarding house details
            // 2. Generating a shareable link
            // 3. Opening share dialog
        });
        
        btnFavorite.setOnClickListener(v -> {
            // TODO: Implement favorite functionality
            // This could involve:
            // 1. Toggling favorite state
            // 2. Updating UI to show favorite state
            // 3. Saving to local database or sending to server
        });
        
        btnCall.setOnClickListener(v -> {
            // TODO: Implement call functionality
            // This could involve:
            // 1. Opening phone dialer with owner's number
            // 2. Making a direct call
        });
        
        btnChooseAccommodation.setOnClickListener(v -> {
            // Navigate to Pre-Booking Phase 1
            Intent intent = new Intent(this, PreBookingPhase1Activity.class);
            intent.putExtra("boarding_house_id", boardingHouseId);
            intent.putExtra("boarding_house_name", boardingHouseName);
            intent.putExtra("boarding_house_image", boardingHouseImage);
            startActivity(intent);
        });
    }
    
    private void loadBoardingHouseDetails() {
        // Set basic information
        if (boardingHouseName != null) {
            tvBoardingHouseName.setText(boardingHouseName);
        }
        
        // Set sample data (replace with actual API call)
        tvLocation.setText("Quezon City, Metro Manila");
        tvPrice.setText("₱3,500");
        tvDescription.setText("A modern and comfortable boarding house located in the heart of Quezon City. Perfect for students and working professionals who value convenience and affordability. Our facility offers clean, well-maintained rooms with essential amenities.");
        
        // TODO: Load actual boarding house details from API
        // This could involve:
        // 1. Making API call with boardingHouseId
        // 2. Parsing response and updating UI
        // 3. Loading images for carousel
        // 4. Loading accommodation types and availability
        // 5. Loading contact information
    }
}
