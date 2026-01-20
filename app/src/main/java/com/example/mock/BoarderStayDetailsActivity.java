package com.example.mock;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BoarderStayDetailsActivity extends AppCompatActivity {

    private ImageView ivBack, ivProfilePicture;
    private TextView tvBoarderName, tvEmail, tvPhone, tvBoardingHouseName, 
                     tvRoomNumber, tvRentType, tvStartDate, tvEndDate, tvStatus, tvDaysRemaining;

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
        tvDaysRemaining = findViewById(R.id.tvDaysRemaining);
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
            // Calculate and display days remaining
            String daysRemainingText = calculateDaysRemaining(endDate);
            tvDaysRemaining.setText(daysRemainingText);
        } else {
            tvEndDate.setText("Not specified");
            tvDaysRemaining.setText("Not specified");
        }
        if (status != null) {
            tvStatus.setText(status);
            // Set status background
            // Completed - blue
            if ("Active".equals(status)) {
                tvStatus.setBackgroundResource(R.drawable.bg_status_available);
            } else if ("Completed".equals(status)) {
                tvStatus.setBackgroundResource(R.drawable.bg_status_completed); // Blue for Completed
            } else {
                tvStatus.setBackgroundResource(R.drawable.bg_rounded_orange);
            }
        }

        // Load profile picture
        if (profilePicture != null && !profilePicture.isEmpty()) {
            String fullImageUrl = "https://boardease.calapebohol.com/" + profilePicture;
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

    private String calculateDaysRemaining(String endDateString) {
        if (endDateString == null || endDateString.isEmpty()) {
            return "Not specified";
        }
        
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date endDate = dateFormat.parse(endDateString);
            
            if (endDate == null) {
                return "Not specified";
            }
            
            // Get current date (start of day)
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            
            // Get checkout date (start of day)
            Calendar checkout = Calendar.getInstance();
            checkout.setTime(endDate);
            checkout.set(Calendar.HOUR_OF_DAY, 0);
            checkout.set(Calendar.MINUTE, 0);
            checkout.set(Calendar.SECOND, 0);
            checkout.set(Calendar.MILLISECOND, 0);
            
            // Calculate difference in days
            long diffInMillis = checkout.getTimeInMillis() - today.getTimeInMillis();
            long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);
            
            // If checkout date has passed
            if (diffInDays < 0) {
                return "Stay completed";
            }
            
            // If checkout is today
            if (diffInDays == 0) {
                return "Last day today";
            }
            
            // Format in English
            if (diffInDays >= 28) {
                // Calculate actual months and remaining days (accounting for different month lengths)
                int months = 0;
                Calendar tempDate = (Calendar) today.clone();
                
                // Add months until we reach or pass the checkout date
                while (true) {
                    Calendar nextMonth = (Calendar) tempDate.clone();
                    nextMonth.add(Calendar.MONTH, 1);
                    
                    if (nextMonth.after(checkout) || nextMonth.equals(checkout)) {
                        break;
                    }
                    
                    tempDate = nextMonth;
                    months++;
                }
                
                // Calculate remaining days after full months
                long remainingDays = 0;
                if (months > 0) {
                    Calendar afterMonths = (Calendar) today.clone();
                    afterMonths.add(Calendar.MONTH, months);
                    long remainingMillis = checkout.getTimeInMillis() - afterMonths.getTimeInMillis();
                    remainingDays = remainingMillis / (24 * 60 * 60 * 1000);
                } else {
                    remainingDays = diffInDays;
                }
                
                // Format output
                if (months > 0) {
                    if (remainingDays == 0) {
                        if (months == 1) {
                            return months + " month remaining";
                        } else {
                            return months + " months remaining";
                        }
                    } else {
                        String monthText = months == 1 ? "month" : "months";
                        String dayText = remainingDays == 1 ? "day" : "days";
                        return months + " " + monthText + " and " + remainingDays + " " + dayText + " remaining";
                    }
                } else {
                    // Less than a month but >= 28 days, show days
                    if (diffInDays == 1) {
                        return diffInDays + " day remaining";
                    } else {
                        return diffInDays + " days remaining";
                    }
                }
            } else {
                // Show days (less than 28 days)
                if (diffInDays == 1) {
                    return diffInDays + " day remaining";
                } else {
                    return diffInDays + " days remaining";
                }
            }
            
        } catch (ParseException e) {
            return "Not specified";
        }
    }
}

