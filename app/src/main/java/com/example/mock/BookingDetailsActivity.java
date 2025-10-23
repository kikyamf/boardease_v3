package com.example.mock;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class BookingDetailsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ImageView imgProfile;
    private TextView tvBoarderName, tvEmail, tvPhone, tvRoomName, tvBoardingHouseName, 
                     tvBoardingHouseAddress, tvStartDate, tvEndDate, tvAmount, tvRentType, 
                     tvStatus, tvPaymentStatus, tvBookingDate, tvNotes;
    private MaterialButton btnApprove, btnDecline, btnContact, btnViewRoom;
    
    private BookingData bookingData;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_details);
        
        // Get booking data from intent
        getIntentData();
        
        // Initialize views
        initializeViews();
        
        // Setup click listeners
        setupClickListeners();
        
        // Load booking data
        loadBookingData();
    }
    
    private void getIntentData() {
        // Get booking data from intent
        // For now, we'll create sample data, but this should come from the intent
        bookingData = new BookingData(
            1, // bookingId
            "John Doe", // boarderName
            "john.doe@email.com", // email
            "09123456789", // phoneNumber
            "Room 1 - Single", // roomName
            "2025-01-15", // startDate
            "2025-04-15", // endDate
            "P3,000.00", // amount
            "Long-term", // rentType
            "Pending", // status
            "Sunset Boarding House", // boardingHouseName
            "123 Main Street, City", // boardingHouseAddress
            "2025-01-10", // bookingDate
            "Pending", // paymentStatus
            "Prefer ground floor room", // notes
            "", // profileImage
            1, // boarderId
            1, // roomId
            1  // boardingHouseId
        );
    }
    
    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        imgProfile = findViewById(R.id.imgProfile);
        tvBoarderName = findViewById(R.id.tvBoarderName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvRoomName = findViewById(R.id.tvRoomName);
        tvBoardingHouseName = findViewById(R.id.tvBoardingHouseName);
        tvBoardingHouseAddress = findViewById(R.id.tvBoardingHouseAddress);
        tvStartDate = findViewById(R.id.tvStartDate);
        tvEndDate = findViewById(R.id.tvEndDate);
        tvAmount = findViewById(R.id.tvAmount);
        tvRentType = findViewById(R.id.tvRentType);
        tvStatus = findViewById(R.id.tvStatus);
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus);
        tvBookingDate = findViewById(R.id.tvBookingDate);
        tvNotes = findViewById(R.id.tvNotes);
        btnApprove = findViewById(R.id.btnApprove);
        btnDecline = findViewById(R.id.btnDecline);
        btnContact = findViewById(R.id.btnContact);
        btnViewRoom = findViewById(R.id.btnViewRoom);
    }
    
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnApprove.setOnClickListener(v -> showApprovalDialog());
        btnDecline.setOnClickListener(v -> showDeclineDialog());
        btnContact.setOnClickListener(v -> contactBoarder());
        btnViewRoom.setOnClickListener(v -> viewRoomDetails());
    }
    
    private void loadBookingData() {
        if (bookingData != null) {
            tvBoarderName.setText(bookingData.getBoarderName());
            tvEmail.setText(bookingData.getEmail());
            tvPhone.setText(bookingData.getPhoneNumber());
            tvRoomName.setText(bookingData.getRoomName());
            tvBoardingHouseName.setText(bookingData.getBoardingHouseName());
            tvBoardingHouseAddress.setText(bookingData.getBoardingHouseAddress());
            tvStartDate.setText(bookingData.getStartDate());
            tvEndDate.setText(bookingData.getEndDate());
            tvAmount.setText(bookingData.getAmount());
            tvRentType.setText(bookingData.getRentType());
            tvStatus.setText(bookingData.getStatus());
            tvPaymentStatus.setText(bookingData.getPaymentStatus());
            tvBookingDate.setText(bookingData.getBookingDate());
            tvNotes.setText(bookingData.getNotes());
            
            // Set profile image (placeholder for now)
            imgProfile.setImageResource(R.drawable.ic_profile);
            
            // Show/hide action buttons based on status
            updateActionButtons();
        }
    }
    
    private void updateActionButtons() {
        String status = bookingData.getStatus();
        
        switch (status) {
            case "Pending":
                btnApprove.setVisibility(View.VISIBLE);
                btnDecline.setVisibility(View.VISIBLE);
                btnApprove.setText("Approve Booking");
                btnDecline.setText("Decline Booking");
                break;
            case "Approved":
                btnApprove.setVisibility(View.GONE);
                btnDecline.setVisibility(View.GONE);
                break;
            case "Declined":
                btnApprove.setVisibility(View.GONE);
                btnDecline.setVisibility(View.GONE);
                break;
            case "Completed":
                btnApprove.setVisibility(View.GONE);
                btnDecline.setVisibility(View.GONE);
                break;
            case "Expired":
                btnApprove.setVisibility(View.GONE);
                btnDecline.setVisibility(View.GONE);
                break;
        }
    }
    
    private void showApprovalDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Approve Booking")
                .setMessage("Are you sure you want to approve this booking?")
                .setPositiveButton("Approve", (dialog, which) -> approveBooking())
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showDeclineDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Decline Booking")
                .setMessage("Are you sure you want to decline this booking?")
                .setPositiveButton("Decline", (dialog, which) -> declineBooking())
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void approveBooking() {
        showProgressDialog("Approving booking...");
        
        // TODO: Make API call to approve booking
        // For now, simulate API call
        new android.os.Handler().postDelayed(() -> {
            hideProgressDialog();
            bookingData.setStatus("Approved");
            updateActionButtons();
            Toast.makeText(this, "Booking approved successfully!", Toast.LENGTH_SHORT).show();
        }, 2000);
    }
    
    private void declineBooking() {
        showProgressDialog("Declining booking...");
        
        // TODO: Make API call to decline booking
        // For now, simulate API call
        new android.os.Handler().postDelayed(() -> {
            hideProgressDialog();
            bookingData.setStatus("Declined");
            updateActionButtons();
            Toast.makeText(this, "Booking declined.", Toast.LENGTH_SHORT).show();
        }, 2000);
    }
    
    private void contactBoarder() {
        // TODO: Open messaging or call functionality
        Toast.makeText(this, "Opening contact options for " + bookingData.getBoarderName(), Toast.LENGTH_SHORT).show();
    }
    
    private void viewRoomDetails() {
        // TODO: Navigate to room details
        Toast.makeText(this, "Viewing room details for " + bookingData.getRoomName(), Toast.LENGTH_SHORT).show();
    }
    
    private void showProgressDialog(String message) {
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(message);
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideProgressDialog() {
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            progressDialog = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        hideProgressDialog();
    }
}













