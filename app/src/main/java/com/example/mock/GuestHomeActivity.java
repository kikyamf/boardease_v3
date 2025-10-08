package com.example.mock;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mock.adapters.BoardingHouseAdapter;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * GuestHomeActivity - Guest view of the main page
 * Displays listings with search functionality and guest restrictions
 */
public class GuestHomeActivity extends AppCompatActivity implements BoardingHouseAdapter.OnBoardingHouseClickListener, BoardingHouseAdapter.OnFavoriteClickListener {

    // Views
    private EditText etSearch;
    private RecyclerView rvListings;
    private TextView tvWelcome;

    // Adapter
    private BoardingHouseAdapter listingsAdapter;

    // Data
    private List<Listing> allListings;
    private List<Listing> filteredListings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_guest_home);

        initializeViews();
        setupRecyclerView();
        setupSearchFunctionality();
        loadMockData();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeViews() {
        etSearch = findViewById(R.id.etSearch);
        rvListings = findViewById(R.id.rvListings);
        tvWelcome = findViewById(R.id.tvWelcome);
    }

    private void setupRecyclerView() {
        // Initialize data lists
        allListings = new ArrayList<>();
        filteredListings = new ArrayList<>();

        // Setup RecyclerView
        listingsAdapter = new BoardingHouseAdapter(this, filteredListings, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        rvListings.setLayoutManager(layoutManager);
        rvListings.setAdapter(listingsAdapter);
    }

    private void setupSearchFunctionality() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterListings(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterListings(String query) {
        filteredListings.clear();
        
        if (query.isEmpty()) {
            filteredListings.addAll(allListings);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Listing listing : allListings) {
                if (listing.getName().toLowerCase().contains(lowerQuery) ||
                    listing.getLocation().toLowerCase().contains(lowerQuery) ||
                    listing.getDescription().toLowerCase().contains(lowerQuery)) {
                    filteredListings.add(listing);
                }
            }
        }
        
        listingsAdapter.notifyDataSetChanged();
    }

    private void loadMockData() {
        // Create mock listings data
        allListings.clear();
        
        allListings.add(new Listing(1, "Sunshine Boarding House", "sample_listing", 
            "Quezon City, Metro Manila", "Cozy and affordable boarding house with modern amenities. Perfect for students and working professionals.",
            "Private Rooms • Bed Spacer", "₱3,500/month", 4.5f));
            
        allListings.add(new Listing(2, "Green Valley Dormitory", "sample_listing", 
            "Makati City, Metro Manila", "Modern dormitory with excellent facilities and 24/7 security. Great for students near universities.",
            "Bed Spacer • Shared Rooms", "₱2,800/month", 4.2f));
            
        allListings.add(new Listing(3, "Metro Student Housing", "sample_listing", 
            "Taguig City, Metro Manila", "Premium student housing with study areas, gym, and laundry facilities. Perfect for serious students.",
            "Private Rooms • Studio", "₱4,500/month", 4.7f));
            
        allListings.add(new Listing(4, "Campus View Boarding", "sample_listing", 
            "Manila, Metro Manila", "Affordable boarding house near major universities. Clean rooms with basic amenities included.",
            "Bed Spacer • Shared Rooms", "₱2,200/month", 3.8f));
            
        allListings.add(new Listing(5, "Urban Living Spaces", "sample_listing", 
            "Pasig City, Metro Manila", "Contemporary living spaces designed for young professionals. Modern amenities and great location.",
            "Private Rooms • Studio", "₱5,000/month", 4.3f));

        // Initialize filtered list with all listings
        filteredListings.addAll(allListings);
        listingsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBoardingHouseClick(Listing listing) {
        // Show guest restriction dialog
        showGuestRestrictionDialog();
    }

    @Override
    public void onFavoriteClick(Listing listing, boolean isFavorite) {
        // Show guest restriction dialog for favorites too
        showGuestRestrictionDialog();
    }

    private void showGuestRestrictionDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_guest_restriction, null);
            builder.setView(dialogView);

            // Initialize views
            MaterialButton btnLogin = dialogView.findViewById(R.id.btnLogin);
            MaterialButton btnSignup = dialogView.findViewById(R.id.btnSignup);

            AlertDialog dialog = builder.create();
            dialog.show();

            // Login button click listener
            btnLogin.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(GuestHomeActivity.this, Login.class);
                startActivity(intent);
            });

            // Signup button click listener
            btnSignup.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(GuestHomeActivity.this, RegistrationActivity.class);
                startActivity(intent);
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error showing dialog", Toast.LENGTH_SHORT).show();
        }
    }

    // Listing data class
    public static class Listing {
        private int id;
        private String name;
        private String imagePath;
        private String location;
        private String description;
        private String accommodationTypes;
        private String price;
        private float rating;

        public Listing(int id, String name, String imagePath, String location, String description,
                      String accommodationTypes, String price, float rating) {
            this.id = id;
            this.name = name;
            this.imagePath = imagePath;
            this.location = location;
            this.description = description;
            this.accommodationTypes = accommodationTypes;
            this.price = price;
            this.rating = rating;
        }

        // Getters
        public int getId() { return id; }
        public String getName() { return name; }
        public String getImagePath() { return imagePath; }
        public String getLocation() { return location; }
        public String getDescription() { return description; }
        public String getAccommodationTypes() { return accommodationTypes; }
        public String getPrice() { return price; }
        public float getRating() { return rating; }
    }
}
