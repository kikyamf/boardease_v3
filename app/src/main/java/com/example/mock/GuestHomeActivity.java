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
        listingsAdapter = new BoardingHouseAdapter(this, filteredListings, (BoardingHouseAdapter.OnBoardingHouseClickListener) this);
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
                if (listing.getBhName().toLowerCase().contains(lowerQuery) ||
                    (listing.getBhAddress() != null && listing.getBhAddress().toLowerCase().contains(lowerQuery)) ||
                    (listing.getBhDescription() != null && listing.getBhDescription().toLowerCase().contains(lowerQuery))) {
                    filteredListings.add(listing);
                }
            }
        }
        
        listingsAdapter.notifyDataSetChanged();
    }

    private void loadMockData() {
        // Create mock listings data using the existing Listing class
        allListings.clear();
        
        allListings.add(new Listing(1, "Sunshine Boarding House", "sample_listing"));
        allListings.add(new Listing(2, "Green Valley Dormitory", "sample_listing"));
        allListings.add(new Listing(3, "Metro Student Housing", "sample_listing"));
        allListings.add(new Listing(4, "Campus View Boarding", "sample_listing"));
        allListings.add(new Listing(5, "Urban Living Spaces", "sample_listing"));

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

}
