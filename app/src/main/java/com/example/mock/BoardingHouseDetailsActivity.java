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
        
        // Setup click listeners
        setupClickListeners();
        
        // Setup image carousel
        setupImageCarousel();
        
        // Load boarding house details
        loadBoardingHouseDetails();
    }
    
    private void getIntentData() {
        Intent intent = getIntent();
        boardingHouseId = intent.getIntExtra("bh_id", 0);
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
            if (layoutIndicators.getChildCount() > 0) {
                ((ImageView) layoutIndicators.getChildAt(0)).setImageResource(R.drawable.dot_active);
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
    
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnShare.setOnClickListener(v -> {
            // TODO: Implement share functionality
        });
        
        btnFavorite.setOnClickListener(v -> {
            // TODO: Implement favorite functionality
        });
        
        btnCall.setOnClickListener(v -> {
            // TODO: Implement call functionality
        });
        
        btnChooseAccommodation.setOnClickListener(v -> {
            // TODO: Navigate to booking/selection screen
            Intent intent = new Intent(this, BoardingHouseDetailsActivity.class);
            intent.putExtra("boarding_house_id", boardingHouseId);
            intent.putExtra("boarding_house_name", boardingHouseName);
            intent.putExtra("boarding_house_image", boardingHouseImage);
            startActivity(intent);
        });
    }
}