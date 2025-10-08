package com.example.mock;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

/**
 * GuestHomeActivity - Simple guest view of the main page
 * Displays basic listings with guest restrictions
 */
public class GuestHomeActivity extends AppCompatActivity {

    // Views
    private EditText etSearch;
    private LinearLayout layoutListings;
    private TextView tvWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_guest_home);

        initializeViews();
        loadSimpleListings();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeViews() {
        etSearch = findViewById(R.id.etSearch);
        layoutListings = findViewById(R.id.layoutListings);
        tvWelcome = findViewById(R.id.tvWelcome);
    }

    private void loadSimpleListings() {
        // Create simple listing cards
        String[] listingNames = {
            "Sunshine Boarding House",
            "Green Valley Dormitory", 
            "Metro Student Housing",
            "Campus View Boarding",
            "Urban Living Spaces"
        };

        String[] locations = {
            "Quezon City, Metro Manila",
            "Makati City, Metro Manila",
            "Taguig City, Metro Manila", 
            "Manila, Metro Manila",
            "Pasig City, Metro Manila"
        };

        String[] prices = {
            "₱3,500/month",
            "₱2,800/month",
            "₱4,500/month",
            "₱2,200/month", 
            "₱5,000/month"
        };

        for (int i = 0; i < listingNames.length; i++) {
            createListingCard(listingNames[i], locations[i], prices[i]);
        }
    }

    private void createListingCard(String name, String location, String price) {
        // Create a simple card layout
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.HORIZONTAL);
        cardLayout.setPadding(16, 16, 16, 16);
        cardLayout.setBackgroundResource(R.drawable.card_bg);
        
        // Add margins
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(16, 8, 16, 8);
        cardLayout.setLayoutParams(params);

        // Image
        ImageView imgBoardingHouse = new ImageView(this);
        imgBoardingHouse.setImageResource(R.drawable.sample_listing);
        imgBoardingHouse.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(120, 120);
        imgParams.setMargins(0, 0, 16, 0);
        imgBoardingHouse.setLayoutParams(imgParams);
        cardLayout.addView(imgBoardingHouse);

        // Details container
        LinearLayout detailsLayout = new LinearLayout(this);
        detailsLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        detailsLayout.setLayoutParams(detailsParams);

        // Name
        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(16);
        tvName.setTextColor(getResources().getColor(android.R.color.black));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tvName.setTypeface(getResources().getFont(R.font.poppins_bold));
        }
        detailsLayout.addView(tvName);

        // Location
        TextView tvLocation = new TextView(this);
        tvLocation.setText(location);
        tvLocation.setTextSize(12);
        tvLocation.setTextColor(getResources().getColor(android.R.color.darker_gray));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tvLocation.setTypeface(getResources().getFont(R.font.poppins_regular));
        }
        tvLocation.setPadding(0, 4, 0, 8);
        detailsLayout.addView(tvLocation);

        // Description
        TextView tvDescription = new TextView(this);
        tvDescription.setText("Cozy and affordable boarding house with modern amenities. Perfect for students and working professionals.");
        tvDescription.setTextSize(12);
        tvDescription.setTextColor(getResources().getColor(android.R.color.darker_gray));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tvDescription.setTypeface(getResources().getFont(R.font.poppins_regular));
        }
        tvDescription.setMaxLines(2);
        detailsLayout.addView(tvDescription);

        // Price
        TextView tvPrice = new TextView(this);
        tvPrice.setText(price);
        tvPrice.setTextSize(16);
        tvPrice.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tvPrice.setTypeface(getResources().getFont(R.font.poppins_bold));
        }
        tvPrice.setPadding(0, 8, 0, 0);
        detailsLayout.addView(tvPrice);

        cardLayout.addView(detailsLayout);

        // Make card clickable
        cardLayout.setOnClickListener(v -> showGuestRestrictionDialog());

        layoutListings.addView(cardLayout);
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
