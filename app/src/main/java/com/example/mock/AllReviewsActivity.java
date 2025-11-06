package com.example.mock;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
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
        // TODO: Load all reviews from API for this boarding house
        // For now, add sample reviews (same as in details page but all of them)
        allReviews.clear();
        
        // Sample Review 1
        Review review1 = new Review();
        review1.setBoarderName("Maria Santos");
        review1.setBoardingHouseName(boardingHouseName != null ? boardingHouseName : "Boarding House");
        review1.setRoomNumber("101");
        review1.setRating(5);
        review1.setComment("Excellent boarding house! The facilities are very clean and well-maintained. The owner is very responsive and helpful. Highly recommended!");
        review1.setReviewDate("Nov 15, 2024");
        review1.setProfilePicture("");
        allReviews.add(review1);
        
        // Sample Review 2
        Review review2 = new Review();
        review2.setBoarderName("John Dela Cruz");
        review2.setBoardingHouseName(boardingHouseName != null ? boardingHouseName : "Boarding House");
        review2.setRoomNumber("205");
        review2.setRating(4);
        review2.setComment("Good value for money. The location is convenient and the room is spacious. The only minor issue is the WiFi can be slow during peak hours.");
        review2.setReviewDate("Nov 10, 2024");
        review2.setProfilePicture("");
        allReviews.add(review2);
        
        // Sample Review 3
        Review review3 = new Review();
        review3.setBoarderName("Sarah Garcia");
        review3.setBoardingHouseName(boardingHouseName != null ? boardingHouseName : "Boarding House");
        review3.setRoomNumber("302");
        review3.setRating(5);
        review3.setComment("Amazing experience! The place is peaceful and safe. The owner is very accommodating and the other boarders are friendly. Will definitely stay here again!");
        review3.setReviewDate("Nov 5, 2024");
        review3.setProfilePicture("");
        allReviews.add(review3);
        
        // Sample Review 4
        Review review4 = new Review();
        review4.setBoarderName("Michael Torres");
        review4.setBoardingHouseName(boardingHouseName != null ? boardingHouseName : "Boarding House");
        review4.setRoomNumber("103");
        review4.setRating(4);
        review4.setComment("Nice place to stay. Clean rooms and good facilities. The owner is friendly and always available when needed.");
        review4.setReviewDate("Nov 1, 2024");
        review4.setProfilePicture("");
        allReviews.add(review4);
        
        // Sample Review 5
        Review review5 = new Review();
        review5.setBoarderName("Anna Reyes");
        review5.setBoardingHouseName(boardingHouseName != null ? boardingHouseName : "Boarding House");
        review5.setRoomNumber("201");
        review5.setRating(5);
        review5.setComment("Perfect for students! The location is near the university and the price is very reasonable. Highly satisfied with my stay here.");
        review5.setReviewDate("Oct 28, 2024");
        review5.setProfilePicture("");
        allReviews.add(review5);
        
        // Update adapter
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
    }
}

