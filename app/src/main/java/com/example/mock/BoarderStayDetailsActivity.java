package com.example.mock;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BoarderStayDetailsActivity extends AppCompatActivity {

    private ImageView ivBack, ivProfilePicture;
    private TextView tvBoarderName, tvEmail, tvPhone, tvBoardingHouseName, 
                     tvRoomNumber, tvRentType, tvStartDate, tvEndDate, tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boarder_stay_details);

        initializeViews();
        setupClickListeners();
        loadData();
    }

    private void initializeViews() {
        ivBack = findViewById(R.id.ivBack);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        tvBoarderName = findViewById(R.id.tvBoarderName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvBoardingHouseName = findViewById(R.id.tvBoardingHouseName);
        tvRoomNumber = findViewById(R.id.tvRoomNumber);
        tvRentType = findViewById(R.id.tvRentType);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);
        tvStatus = findViewById(R.id.tvStatus);
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
    }

    private void loadData() {
        // Get data from intent
        String boarderName = getIntent().getStringExtra("boarder_name");
        String email = getIntent().getStringExtra("boarder_email");
        String phone = getIntent().getStringExtra("boarder_phone");
        String boardingHouseName = getIntent().getStringExtra("boarding_house_name");
        String roomNumber = getIntent().getStringExtra("room_number");
        if (roomNumber == null) {
            roomNumber = getIntent().getStringExtra("room_name");
        }
        String rentType = getIntent().getStringExtra("rent_type");
        String startDate = getIntent().getStringExtra("start_date");
        String endDate = getIntent().getStringExtra("end_date");
        String status = getIntent().getStringExtra("status");
        String profilePicture = getIntent().getStringExtra("profile_picture");

        // Set data to views
        if (boarderName != null) {
            tvBoarderName.setText(boarderName);
        }
        if (email != null && !email.isEmpty()) {
            tvEmail.setText(email);
        } else {
            tvEmail.setText("Not provided");
        }
        if (phone != null && !phone.isEmpty()) {
            tvPhone.setText(phone);
        } else {
            tvPhone.setText("Not provided");
        }
        if (boardingHouseName != null) {
            tvBoardingHouseName.setText(boardingHouseName);
        }
        if (roomNumber != null) {
            tvRoomNumber.setText(roomNumber);
        }
        if (rentType != null) {
            tvRentType.setText(rentType);
        }
        if (startDate != null && !startDate.isEmpty()) {
            tvStartDate.setText(formatDate(startDate));
        } else {
            tvStartDate.setText("Not specified");
        }
        if (endDate != null && !endDate.isEmpty()) {
            tvEndDate.setText(formatDate(endDate));
        } else {
            tvEndDate.setText("Not specified");
        }
        if (status != null) {
            tvStatus.setText(status);
            // Set status background
            if ("Active".equals(status)) {
                tvStatus.setBackgroundResource(R.drawable.bg_status_available);
            } else if ("Completed".equals(status)) {
                tvStatus.setBackgroundResource(R.drawable.bg_status_occupied);
            } else {
                tvStatus.setBackgroundResource(R.drawable.bg_rounded_orange);
            }
        }

        // Load profile picture
        if (profilePicture != null && !profilePicture.isEmpty()) {
            String fullImageUrl = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/" + profilePicture;
            Glide.with(this)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.btn_profile)
                    .error(R.drawable.btn_profile)
                    .circleCrop()
                    .into(ivProfilePicture);
        } else {
            ivProfilePicture.setImageResource(R.drawable.btn_profile);
        }
    }

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "";
        }
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (ParseException e) {
            // If parsing fails, return original string
        }
        return dateString;
    }
}

