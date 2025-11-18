package com.example.mock;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class AllReviewsActivity extends AppCompatActivity {
    
    private static final String TAG = "AllReviewsActivity";
    
    private ImageButton btnBack;
    private TextView tvBoardingHouseName, tvReviewCount, tvNoReviews;
    private RecyclerView rvAllReviews;
    private ProgressBar progressBar;
    private AllReviewsAdapter reviewsAdapter;
    private List<Review> allReviews;
    private int boardingHouseId;
    private String boardingHouseName;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_reviews);
        
        // Get data from intent
        getIntentData();
        
        // Initialize views
        initializeViews();
        
        // Setup click listeners
        setupClickListeners();
        
        // Load all reviews
        loadAllReviews();
    }
    
    private void getIntentData() {
        Intent intent = getIntent();
        boardingHouseId = intent.getIntExtra("bh_id", 0);
        boardingHouseName = intent.getStringExtra("bh_name");
        
        if (boardingHouseId == 0) {
            Log.e(TAG, "Invalid boarding house ID");
            finish();
        }
    }
    
    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        tvBoardingHouseName = findViewById(R.id.tvBoardingHouseName);
        tvReviewCount = findViewById(R.id.tvReviewCount);
        tvNoReviews = findViewById(R.id.tvNoReviews);
        rvAllReviews = findViewById(R.id.rvAllReviews);
        progressBar = findViewById(R.id.progressBar);
        
        // Set boarding house name
        if (boardingHouseName != null) {
            tvBoardingHouseName.setText(boardingHouseName);
        }
        
        // Initialize lists
        allReviews = new ArrayList<>();
        
        // Setup recycler view
        reviewsAdapter = new AllReviewsAdapter((ArrayList<Review>) allReviews, this);
        rvAllReviews.setLayoutManager(new LinearLayoutManager(this));
        rvAllReviews.setAdapter(reviewsAdapter);
    }
    
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }
    
    private void loadAllReviews() {
        // Load all reviews from API for this boarding house
        allReviews.clear();
        reviewsAdapter.notifyDataSetChanged();
        
        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        rvAllReviews.setVisibility(View.GONE);
        tvNoReviews.setVisibility(View.GONE);
        
        ReviewApiService apiService = new ReviewApiService(this);
        // Get all reviews for this specific boarding house
        apiService.getReviews(boardingHouseId, 0, 0, "all", "published", "newest", new ReviewApiService.ReviewApiCallback() {
            @Override
            public void onSuccess(List<Review> reviewList) {
                // Hide loading
                progressBar.setVisibility(View.GONE);
                
                allReviews.clear();
                allReviews.addAll(reviewList);
                
                reviewsAdapter.notifyDataSetChanged();
                
                // Show reviews if available
                if (allReviews.isEmpty()) {
                    rvAllReviews.setVisibility(View.GONE);
                    tvNoReviews.setVisibility(View.VISIBLE);
                    tvReviewCount.setText("0 Reviews");
                } else {
                    rvAllReviews.setVisibility(View.VISIBLE);
                    tvNoReviews.setVisibility(View.GONE);
                    tvReviewCount.setText(allReviews.size() + " Reviews");
                }
                
                Log.d(TAG, "Loaded " + allReviews.size() + " reviews for boarding house ID: " + boardingHouseId);
            }

            @Override
            public void onError(String error) {
                // Hide loading
                progressBar.setVisibility(View.GONE);
                
                Log.e(TAG, "Error loading reviews: " + error);
                // Show empty state on error
                rvAllReviews.setVisibility(View.GONE);
                tvNoReviews.setVisibility(View.VISIBLE);
                tvReviewCount.setText("0 Reviews");
            }
        });
    }
}

