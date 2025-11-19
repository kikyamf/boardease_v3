package com.example.mock;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.VolleyError;
import com.android.volley.Response;

public class BookingDetailsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ImageView imgProfile;
    private TextView tvBoarderName, tvEmail, tvPhone, tvRoomName, tvBoardingHouseName, 
                     tvBoardingHouseAddress, tvStartDate, tvEndDate, tvAmount, tvTotalBookingAmount, tvRentType, 
                     tvStatus, tvPaymentStatus, tvBookingDate;
    private MaterialButton btnApprove, btnDecline, btnContact, btnViewRoom;
    
    private BookingData bookingData;
    private ProgressDialog progressDialog;
    private static final int PERMISSION_REQUEST_CALL_PHONE = 100;
    private int ownerId = 0;

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
        Intent intent = getIntent();
        
        // Get owner_id from intent, or fallback to SharedPreferences
        ownerId = intent.getIntExtra("owner_id", 0);
        if (ownerId == 0) {
            // Try to get from SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
            String userIdString = sharedPreferences.getString("user_id", "0");
            try {
                ownerId = Integer.parseInt(userIdString);
                Log.d("BookingDetails", "Got owner_id from SharedPreferences: " + ownerId);
            } catch (NumberFormatException e) {
                Log.e("BookingDetails", "Failed to parse user_id from SharedPreferences: " + userIdString);
                ownerId = 0;
            }
        } else {
            Log.d("BookingDetails", "Got owner_id from intent: " + ownerId);
        }
        
        if (intent != null && intent.hasExtra("booking_id")) {
            // Create booking data from intent extras
            bookingData = new BookingData(
                intent.getIntExtra("booking_id", 0),
                intent.getStringExtra("boarder_name"),
                intent.getStringExtra("boarder_email"),
                intent.getStringExtra("boarder_phone"),
                intent.getStringExtra("room_name"),
                intent.getStringExtra("start_date"),
                intent.getStringExtra("end_date"),
                intent.getStringExtra("amount"),
                intent.getStringExtra("rent_type"),
                intent.getStringExtra("status"),
                intent.getStringExtra("boarding_house_name"),
                intent.getStringExtra("boarding_house_address"),
                intent.getStringExtra("booking_date"),
                intent.getStringExtra("payment_status"),
                intent.getStringExtra("notes"),
                intent.getStringExtra("profile_image"),
                intent.getIntExtra("boarder_id", 0),
                intent.getIntExtra("room_id", 0),
                intent.getIntExtra("boarding_house_id", 0)
            );
            
            // Set payment progress fields from intent extras if available
            if (intent.hasExtra("total_periods")) {
                bookingData.setTotalPeriods(intent.getIntExtra("total_periods", 0));
                bookingData.setPaidPeriods(intent.getIntExtra("paid_periods", 0));
                bookingData.setTotalAmountForBooking(intent.getStringExtra("total_amount_for_booking"));
                bookingData.setPaidAmountForBooking(intent.getStringExtra("paid_amount_for_booking"));
                bookingData.setFullyPaid(intent.getBooleanExtra("is_fully_paid", false));
            }
            
            // Log payment status from Intent for debugging
            String intentPaymentStatus = intent.getStringExtra("payment_status");
            Log.d("BookingDetails", "Payment Status from Intent: " + intentPaymentStatus + 
                  ", Total Periods: " + bookingData.getTotalPeriods() + 
                  ", Paid Periods: " + bookingData.getPaidPeriods() +
                  ", Is Fully Paid (flag): " + bookingData.isFullyPaid());
        } else {
            // Fallback to sample data if intent data is missing
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
        tvTotalBookingAmount = findViewById(R.id.tvTotalBookingAmount);
        tvRentType = findViewById(R.id.tvRentType);
        tvStatus = findViewById(R.id.tvStatus);
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus);
        tvBookingDate = findViewById(R.id.tvBookingDate);
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
            tvBoarderName.setText(bookingData.getBoarderName() != null ? bookingData.getBoarderName() : "");
            tvEmail.setText(bookingData.getEmail() != null ? bookingData.getEmail() : "");
            tvPhone.setText(bookingData.getPhoneNumber() != null ? bookingData.getPhoneNumber() : "");
            tvRoomName.setText(bookingData.getRoomName() != null ? bookingData.getRoomName() : "");
            tvBoardingHouseName.setText(bookingData.getBoardingHouseName() != null ? bookingData.getBoardingHouseName() : "");
            tvBoardingHouseAddress.setText(bookingData.getBoardingHouseAddress() != null ? bookingData.getBoardingHouseAddress() : "");
            // Format start and end dates with time
            String startDate = bookingData.getStartDate();
            String endDate = bookingData.getEndDate();
            tvStartDate.setText(startDate != null && !startDate.isEmpty() ? formatDateTime(startDate) : "");
            tvEndDate.setText(endDate != null && !endDate.isEmpty() ? formatDateTime(endDate) : "");
            
            // Set room amount (monthly price) - this should ALWAYS be the room's monthly price
            String roomAmount = bookingData.getAmount();
            if (roomAmount != null && !roomAmount.isEmpty()) {
                // Format if not already formatted
                if (!roomAmount.startsWith("₱")) {
                    tvAmount.setText("₱" + formatAmountForBooking(roomAmount));
                } else {
                    tvAmount.setText(roomAmount);
                }
            } else {
                tvAmount.setText("₱0.00");
            }
            
            // Set total booking amount (from payment_breakdowns) - this is the total amount for the entire booking
            String totalBookingAmount = bookingData.getTotalAmountForBooking();
            boolean hasValidTotal = false;
            
            if (totalBookingAmount != null && !totalBookingAmount.isEmpty() && !totalBookingAmount.equals("0.00")) {
                try {
                    double totalValue = Double.parseDouble(totalBookingAmount);
                    if (totalValue > 0) {
                        tvTotalBookingAmount.setText("₱" + formatAmountForBooking(totalBookingAmount));
                        hasValidTotal = true;
                    }
                } catch (NumberFormatException e) {
                    // Will use fallback
                }
            }
            
            // Fallback to room amount if no valid total amount (but this should be rare)
            if (!hasValidTotal) {
                // If no breakdown exists, total amount = room amount (for single month bookings)
                tvTotalBookingAmount.setText(tvAmount.getText());
            }
            
            // Log for debugging
            Log.d("BookingDetails", "Room Amount (monthly price): " + roomAmount + 
                  ", Total Booking Amount (from breakdowns): " + totalBookingAmount + 
                  ", Displayed Room Amount: " + tvAmount.getText() +
                  ", Displayed Total Amount: " + tvTotalBookingAmount.getText());
            
            tvRentType.setText(bookingData.getRentType() != null ? bookingData.getRentType() : "");
            
            // Set status with styling
            String status = bookingData.getStatus() != null ? bookingData.getStatus() : "";
            tvStatus.setText(status);
            applyStatusStyle(tvStatus, status);
            
            // Set payment status with styling - prioritize database status from payments table
            // The payment status should directly reflect what's in the payments table
            // Logic: payment_status in payments table is the source of truth
            // It's calculated based on payment_breakdowns: 
            // - "Fully Paid" if all periods paid (is_paid = 1)
            // - "Partially Paid" if some periods paid
            // - "Pending" if no periods paid
            String paymentStatus;
            int totalPeriods = bookingData.getTotalPeriods();
            int paidPeriods = bookingData.getPaidPeriods();
            String dbPaymentStatus = bookingData.getPaymentStatus();
            
            // Log what we received for debugging
            Log.d("BookingDetails", "Raw payment_status from BookingData: '" + dbPaymentStatus + "'");
            
            // Always use the database status from payments table as the source of truth
            // The backend already calculates this correctly based on payment_breakdowns
            // CRITICAL: Do NOT calculate from periods - always trust the database status
            // The database status is the single source of truth - it's calculated from payment_breakdowns
            if (dbPaymentStatus != null && !dbPaymentStatus.trim().isEmpty() && 
                !dbPaymentStatus.trim().equalsIgnoreCase("null") && 
                !dbPaymentStatus.trim().equalsIgnoreCase("")) {
                
                String statusLower = dbPaymentStatus.toLowerCase().trim();
                Log.d("BookingDetails", "Normalizing payment status: '" + dbPaymentStatus + "' -> lowercase: '" + statusLower + "'");
                
                // Normalize the status format to match expected display format
                if (statusLower.equals("fully paid") || statusLower.equals("fully_paid")) {
                    paymentStatus = "Fully Paid";
                    Log.d("BookingDetails", "Status normalized to: Fully Paid");
                } else if (statusLower.equals("partially paid") || 
                          statusLower.equals("partially_paid") ||
                          (statusLower.contains("partially") && statusLower.contains("paid")) ||
                          // Legacy support for old "Completed/Partially" status
                          statusLower.equals("completed/partially") || 
                          statusLower.equals("completed_partially") ||
                          (statusLower.contains("completed") && statusLower.contains("partially"))) {
                    paymentStatus = "Partially Paid";
                    Log.d("BookingDetails", "Status normalized to: Partially Paid");
                } else if (statusLower.equals("completed") && paidPeriods > 0 && paidPeriods < totalPeriods) {
                    // If database says "Completed" but periods show partial payment, use "Partially Paid"
                    paymentStatus = "Partially Paid";
                    Log.d("BookingDetails", "Status 'Completed' with partial periods -> normalized to: Partially Paid");
                } else {
                    // Use database status as-is (capitalize first letter properly)
                    if (dbPaymentStatus.length() > 1) {
                        // Capitalize first letter, rest lowercase
                        paymentStatus = dbPaymentStatus.substring(0, 1).toUpperCase() + 
                                       dbPaymentStatus.substring(1).toLowerCase();
                    } else if (dbPaymentStatus.length() == 1) {
                        paymentStatus = dbPaymentStatus.toUpperCase();
                    } else {
                        paymentStatus = dbPaymentStatus;
                    }
                    Log.d("BookingDetails", "Status used as-is (capitalized): " + paymentStatus);
                }
            } else {
                // Database status is missing - this should not happen, but calculate as fallback
                // WARNING: This calculation might not match database if periods are incorrectly counted
                // The database status is the source of truth, so this is only a fallback
                // CRITICAL: Do NOT use isFullyPaid flag - only use period counts for calculation
                Log.w("BookingDetails", "WARNING: Database payment_status is missing or empty! " +
                      "dbPaymentStatus='" + dbPaymentStatus + "', " +
                      "Falling back to calculation. This should not happen. " +
                      "Total Periods: " + totalPeriods + ", Paid Periods: " + paidPeriods);
                if (totalPeriods > 0) {
                    // Calculate based on periods ONLY - ignore isFullyPaid flag
                    // This ensures we calculate correctly even if flag is wrong
                    if (paidPeriods >= totalPeriods) {
                        // All periods paid
                        paymentStatus = "Fully Paid";
                        Log.d("BookingDetails", "Calculated status: Fully Paid (paidPeriods >= totalPeriods: " + paidPeriods + " >= " + totalPeriods + ")");
                    } else if (paidPeriods > 0) {
                        // Some periods paid but not all
                        paymentStatus = "Partially Paid";
                        Log.d("BookingDetails", "Calculated status: Partially Paid (paidPeriods > 0 && < totalPeriods: " + paidPeriods + " < " + totalPeriods + ")");
                    } else {
                        // No periods paid
                paymentStatus = "Pending";
                        Log.d("BookingDetails", "Calculated status: Pending (paidPeriods = 0)");
                    }
                } else {
                    // No breakdown data - default to pending
                    paymentStatus = "Pending";
                    Log.d("BookingDetails", "Calculated status: Pending (no breakdown data, totalPeriods = 0)");
                }
            }
            
            // Log for debugging
            boolean isFullyPaidCalculated = totalPeriods > 0 && paidPeriods >= totalPeriods;
            Log.d("BookingDetails", "Payment Status - Database: " + dbPaymentStatus + 
                  ", Displayed: " + paymentStatus +
                  ", Total Periods: " + totalPeriods + 
                  ", Paid Periods: " + paidPeriods +
                  ", Is Fully Paid (calculated from periods): " + isFullyPaidCalculated);
            
            // Set payment status text and styling
            if (tvPaymentStatus != null) {
                // Ensure payment status has a value
                if (paymentStatus == null || paymentStatus.isEmpty()) {
                    paymentStatus = "Pending";
                }
                
                // Set text first
                tvPaymentStatus.setText(paymentStatus);
                
                // Clear any default styling from XML first
                tvPaymentStatus.setBackground(null);
                
                // Apply correct styling based on status (this will set both background and text color)
                applyPaymentStatusStyle(tvPaymentStatus, paymentStatus);
                
                // Ensure text is visible
                tvPaymentStatus.setVisibility(View.VISIBLE);
                
                // Log for debugging
                Log.d("BookingDetails", "Payment Status Displayed: " + paymentStatus + 
                      ", Total Periods: " + totalPeriods + 
                      ", Paid Periods: " + paidPeriods + 
                      ", Payment Status Text: " + tvPaymentStatus.getText());
            }
            
            // Format booking date with time in "02:00 PM" format
            String bookingDate = bookingData.getBookingDate();
            if (bookingDate != null && !bookingDate.isEmpty()) {
                tvBookingDate.setText(formatDateTime(bookingDate));
            } else {
                tvBookingDate.setText("");
            }
            
            // NOTE: Do NOT overwrite tvAmount (Room Amount) here - it should always show the room's monthly price
            // tvAmount is already set correctly above from bookingData.getAmount() (room price)
            // tvTotalBookingAmount is already set correctly above from bookingData.getTotalAmountForBooking()
            
            // Log payment info for debugging
            String totalAmountForBooking = bookingData.getTotalAmountForBooking();
            String paidAmountForBooking = bookingData.getPaidAmountForBooking();
            Log.d("BookingDetails", "Payment Info - Total Periods: " + totalPeriods + 
                  ", Paid Periods: " + paidPeriods + 
                  ", Total Amount: " + totalAmountForBooking + 
                  ", Paid Amount: " + paidAmountForBooking + 
                  ", Room Amount: " + roomAmount);
            
            // Load profile picture
            if (imgProfile != null) {
                String profilePicture = bookingData.getProfileImage();
                if (profilePicture != null && !profilePicture.isEmpty()) {
                    String fullImageUrl = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/" + profilePicture;
                    com.bumptech.glide.Glide.with(this)
                            .load(fullImageUrl)
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .circleCrop()
                            .into(imgProfile);
                } else {
                    imgProfile.setImageResource(R.drawable.ic_profile);
                }
            }
            
            // Show/hide action buttons based on status
            updateActionButtons();
        } else {
            Toast.makeText(this, "Error: Booking data not available", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void applyStatusStyle(TextView textView, String status) {
        if (textView == null || status == null) return;
        
        int textColor;
        int backgroundRes;
        
        switch (status) {
            case "Confirmed":
            case "Approved":
                // Confirmed - blue
                textColor = getResources().getColor(android.R.color.white);
                backgroundRes = R.drawable.bg_status_completed;
                break;
            case "Cancelled":
            case "Declined":
                textColor = getResources().getColor(android.R.color.white);
                backgroundRes = R.drawable.bg_rounded_red;
                break;
            case "Completed":
                // Completed - green
                textColor = getResources().getColor(android.R.color.white);
                backgroundRes = R.drawable.bg_status_approved;
                break;
            case "Pending":
            default:
                // Pending - orange
                textColor = getResources().getColor(android.R.color.white);
                backgroundRes = R.drawable.bg_status_pending;
                break;
        }
        
        int padding = (int) (12 * getResources().getDisplayMetrics().density);
        textView.setTextColor(textColor);
        textView.setBackgroundResource(backgroundRes);
        textView.setPadding(padding, padding / 2, padding, padding / 2);
    }
    
    private void applyPaymentStatusStyle(TextView textView, String paymentStatus) {
        if (textView == null || paymentStatus == null || paymentStatus.isEmpty()) {
            Log.w("BookingDetails", "Cannot apply payment status style - textView or paymentStatus is null/empty");
            return;
        }
        
        int textColor;
        int backgroundRes;
        
        String statusLower = paymentStatus.toLowerCase().trim();
        Log.d("BookingDetails", "Applying payment status style for: " + statusLower);
        
        switch (statusLower) {
            case "fully paid":
                textColor = getResources().getColor(android.R.color.white);
                backgroundRes = R.drawable.bg_status_approved;
                break;
            case "paid":
            case "confirmed":
                textColor = getResources().getColor(android.R.color.white);
                backgroundRes = R.drawable.bg_status_approved;
                break;
            case "completed":
            case "partially paid":
            case "partially_paid":
            case "completed/partially":
            case "completed_partially":
                // Partially Paid means paid but may have remaining balance - use blue color
                textColor = getResources().getColor(android.R.color.white);
                backgroundRes = R.drawable.bg_status_completed;
                break;
            case "overdue":
            case "cancelled":
                textColor = getResources().getColor(android.R.color.white);
                backgroundRes = R.drawable.bg_rounded_red;
                break;
            case "pending":
            default:
                textColor = getResources().getColor(android.R.color.white);
                backgroundRes = R.drawable.bg_status_pending;
                break;
        }
        
        int padding = (int) (12 * getResources().getDisplayMetrics().density);
        
        // Ensure text is set before styling
        if (textView.getText() == null || textView.getText().toString().isEmpty()) {
            textView.setText(paymentStatus);
        }
        
        // Apply styling
        textView.setTextColor(textColor);
        textView.setBackgroundResource(backgroundRes);
        textView.setPadding(padding, padding / 2, padding, padding / 2);
        
        // Ensure visibility
        textView.setVisibility(View.VISIBLE);
        
        Log.d("BookingDetails", "Payment status styled - Text: " + textView.getText() + 
              ", Background: " + backgroundRes + 
              ", Text Color: " + textColor);
    }
    
    private String formatAmountForBooking(String amount) {
        try {
            double amountValue = Double.parseDouble(amount);
            java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,##0.00");
            return formatter.format(amountValue);
        } catch (NumberFormatException e) {
            return amount;
        }
    }
    
    private String formatDateTime(String dateTime) {
        // Format from "YYYY-MM-DD HH:MM:SS" to "MMM DD, YYYY hh:mm a" (e.g., "Jan 15, 2025 02:00 PM")
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault());
            java.util.Date date = inputFormat.parse(dateTime);
            return outputFormat.format(date);
        } catch (Exception e) {
            // If parsing fails, try date-only format
            try {
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
                java.util.Date date = inputFormat.parse(dateTime);
                return outputFormat.format(date);
            } catch (Exception e2) {
                return dateTime; // Return original if parsing fails
            }
        }
    }
    
    private void updateActionButtons() {
        if (bookingData == null || bookingData.getStatus() == null) {
            return;
        }
        
        String status = bookingData.getStatus();
        
        if (btnApprove != null && btnDecline != null) {
        switch (status) {
            case "Pending":
                btnApprove.setVisibility(View.VISIBLE);
                btnDecline.setVisibility(View.VISIBLE);
                btnApprove.setText("Approve Booking");
                btnDecline.setText("Decline Booking");
                break;
                case "Confirmed":
                btnApprove.setVisibility(View.GONE);
                btnDecline.setVisibility(View.GONE);
                break;
                case "Cancelled":
            case "Declined":
                btnApprove.setVisibility(View.GONE);
                btnDecline.setVisibility(View.GONE);
                break;
            case "Completed":
                btnApprove.setVisibility(View.GONE);
                btnDecline.setVisibility(View.GONE);
                break;
                default:
                btnApprove.setVisibility(View.GONE);
                btnDecline.setVisibility(View.GONE);
                break;
            }
        }
        
        // Update status styling when status changes
        if (tvStatus != null) {
            applyStatusStyle(tvStatus, status);
        }
    }
    
    private void showApprovalDialog() {
        // Use comprehensive approval dialog with payment information (same as PendingBookingsFragment)
        showApproveConfirmationDialog();
    }
    
    private void showApproveConfirmationDialog() {
        // Use light theme for dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_approve_booking_confirmation, null);
        builder.setView(dialogView);
        
        // Get views
        TextView tvBoarderName = dialogView.findViewById(R.id.tvBoarderName);
        TextView tvRoomName = dialogView.findViewById(R.id.tvRoomName);
        TextView tvPaymentStatus = dialogView.findViewById(R.id.tvPaymentStatus);
        TextView tvAmountPaid = dialogView.findViewById(R.id.tvAmountPaid);
        TextView tvTotalAmount = dialogView.findViewById(R.id.tvTotalAmount);
        TextView tvPaymentProgress = dialogView.findViewById(R.id.tvPaymentProgress);
        TextView tvProgressPercent = dialogView.findViewById(R.id.tvProgressPercent);
        ProgressBar progressBarPayment = dialogView.findViewById(R.id.progressBarPayment);
        TextView tvWarning = dialogView.findViewById(R.id.tvWarning);
        android.widget.ImageView imgPaymentProof = dialogView.findViewById(R.id.imgPaymentProof);
        TextView tvNoProof = dialogView.findViewById(R.id.tvNoProof);
        LinearLayout layoutPaymentProof = dialogView.findViewById(R.id.layoutPaymentProof);
        android.widget.Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        
        // Set booking information
        tvBoarderName.setText(bookingData.getBoarderName() != null ? bookingData.getBoarderName() : "Unknown");
        tvRoomName.setText(bookingData.getRoomName() != null ? bookingData.getRoomName() : "");
        
        // Set payment information
        int totalPeriods = bookingData.getTotalPeriods();
        int paidPeriods = bookingData.getPaidPeriods();
        String paidAmount = bookingData.getPaidAmountForBooking();
        String totalAmount = bookingData.getTotalAmountForBooking();
        boolean isFullyPaid = bookingData.isFullyPaid();
        double paymentProgressPercent = bookingData.getPaymentProgressPercent();
        
        // Determine payment status
        String paymentStatusText;
        int statusColor;
        int statusBg;
        
        if (isFullyPaid || (totalPeriods > 0 && paidPeriods >= totalPeriods)) {
            paymentStatusText = "Fully Paid";
            statusColor = getResources().getColor(android.R.color.white);
            statusBg = R.drawable.bg_status_approved;
            tvWarning.setVisibility(View.GONE);
        } else if (paidPeriods > 0 && paidPeriods < totalPeriods) {
            paymentStatusText = "Partially Paid";
            statusColor = getResources().getColor(android.R.color.white);
            statusBg = R.drawable.bg_status_completed;
            tvWarning.setVisibility(View.VISIBLE);
            tvWarning.setText("⚠ Some periods are paid but not all. Please verify payment screenshot and check your GCash account before approving.");
            tvWarning.setTextColor(getResources().getColor(android.R.color.white));
            tvWarning.setBackgroundResource(R.drawable.bg_rounded_red);
        } else {
            paymentStatusText = "Pending - For Confirmation";
            statusColor = getResources().getColor(android.R.color.white);
            statusBg = R.drawable.bg_status_pending;
            tvWarning.setVisibility(View.VISIBLE);
            tvWarning.setText("⚠ Payment may have been made but not yet confirmed. Please check payment screenshot above and verify in your GCash account. After approval, payment will be automatically marked as paid.");
            tvWarning.setTextColor(getResources().getColor(android.R.color.white));
            tvWarning.setBackgroundResource(R.drawable.bg_rounded_red);
        }
        
        tvPaymentStatus.setText(paymentStatusText);
        tvPaymentStatus.setTextColor(statusColor);
        tvPaymentStatus.setBackgroundResource(statusBg);
        
        // Set amounts
        if (paidAmount != null && !paidAmount.isEmpty()) {
            try {
                double paidValue = Double.parseDouble(paidAmount);
                if (paidValue > 0) {
                    tvAmountPaid.setText("₱" + formatAmount(paidAmount));
                } else {
                    tvAmountPaid.setText("₱0.00");
                }
            } catch (NumberFormatException e) {
                tvAmountPaid.setText("₱0.00");
            }
        } else {
            tvAmountPaid.setText("₱0.00");
        }
        
        if (totalAmount != null && !totalAmount.isEmpty()) {
            try {
                double totalValue = Double.parseDouble(totalAmount);
                if (totalValue > 0) {
                    tvTotalAmount.setText("₱" + formatAmount(totalAmount));
                } else {
                    tvTotalAmount.setText(bookingData.getAmount() != null ? bookingData.getAmount() : "₱0.00");
                }
            } catch (NumberFormatException e) {
                tvTotalAmount.setText(bookingData.getAmount() != null ? bookingData.getAmount() : "₱0.00");
            }
        } else {
            tvTotalAmount.setText(bookingData.getAmount() != null ? bookingData.getAmount() : "₱0.00");
        }
        
        // Set payment progress
        if (totalPeriods > 0) {
            tvPaymentProgress.setVisibility(View.VISIBLE);
            progressBarPayment.setVisibility(View.VISIBLE);
            tvProgressPercent.setVisibility(View.VISIBLE);
            
            int totalMonths = bookingData.getTotalMonthsForBooking();
            int paidMonths = bookingData.getPaidMonthsForBooking();
            
            if (totalMonths > 0 && totalPeriods == totalMonths) {
                if (totalMonths == 1) {
                    tvPaymentProgress.setText(String.format("%d/%d month paid", paidMonths, totalMonths));
                } else {
                    tvPaymentProgress.setText(String.format("%d/%d months paid", paidMonths, totalMonths));
                }
            } else {
                if (totalPeriods == 1) {
                    tvPaymentProgress.setText(String.format("%d/%d period paid", paidPeriods, totalPeriods));
                } else {
                    tvPaymentProgress.setText(String.format("%d/%d periods paid", paidPeriods, totalPeriods));
                }
            }
            
            progressBarPayment.setProgress((int) paymentProgressPercent);
            tvProgressPercent.setText(String.format("%.0f%%", paymentProgressPercent));
            
            // Set progress bar color
            if (isFullyPaid) {
                progressBarPayment.setProgressTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50")));
                tvPaymentProgress.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
            } else if (paymentProgressPercent >= 50) {
                progressBarPayment.setProgressTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF9800")));
                tvPaymentProgress.setTextColor(android.graphics.Color.parseColor("#FF9800"));
            } else {
                progressBarPayment.setProgressTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F44336")));
                tvPaymentProgress.setTextColor(android.graphics.Color.parseColor("#F44336"));
            }
        } else {
            tvPaymentProgress.setVisibility(View.GONE);
            progressBarPayment.setVisibility(View.GONE);
            tvProgressPercent.setVisibility(View.GONE);
        }
        
        // Load payment proof
        loadPaymentProofForDialog(imgPaymentProof, tvNoProof, layoutPaymentProof);
        
        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            // Ensure light background
            dialog.getWindow().getDecorView().setBackgroundColor(getResources().getColor(android.R.color.white));
        }
        
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            approveBooking();
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private void loadPaymentProofForDialog(android.widget.ImageView imgPaymentProof, 
                                          TextView tvNoProof, LinearLayout layoutPaymentProof) {
        // Fetch payment proof from the booking's payment record
        String url = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/get_payment_proof_by_booking.php?booking_id=" + bookingData.getBookingId();
        
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        com.android.volley.toolbox.StringRequest stringRequest = new com.android.volley.toolbox.StringRequest(
            Request.Method.GET, url,
            response -> {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.getBoolean("success")) {
                        String paymentProofUrl = jsonResponse.optString("payment_proof_url", "");
                        String receiptUrl = jsonResponse.optString("receipt_url", "");
                        
                        String proofUrl = null;
                        if (receiptUrl != null && !receiptUrl.isEmpty() && !receiptUrl.equals("null")) {
                            proofUrl = receiptUrl;
                        } else if (paymentProofUrl != null && !paymentProofUrl.isEmpty() && !paymentProofUrl.equals("null")) {
                            proofUrl = paymentProofUrl;
                        }
                        
                        if (proofUrl != null && !proofUrl.isEmpty() && !proofUrl.equals("null")) {
                            // Show image view and hide "no proof" text
                            imgPaymentProof.setVisibility(View.VISIBLE);
                            tvNoProof.setVisibility(View.GONE);
                            layoutPaymentProof.setVisibility(View.VISIBLE);
                            
                            // Build full URL
                            String baseUrl = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/";
                            String urlToProcess = proofUrl.trim();
                            String finalFullUrl;
                            
                            if (urlToProcess.startsWith("http://") || urlToProcess.startsWith("https://")) {
                                finalFullUrl = urlToProcess;
                            } else {
                                if (urlToProcess.startsWith("/")) {
                                    urlToProcess = urlToProcess.substring(1);
                                }
                                if (urlToProcess.startsWith("BoardEase2/")) {
                                    urlToProcess = urlToProcess.substring(11);
                                }
                                finalFullUrl = baseUrl + "get_payment_proof.php?path=" + Uri.encode(urlToProcess, "UTF-8");
                            }
                            
                            // Load image using Glide
                            try {
                                com.bumptech.glide.Glide.with(this)
                                    .load(finalFullUrl)
                                    .placeholder(android.R.drawable.ic_menu_report_image)
                                    .error(android.R.drawable.ic_dialog_alert)
                                    .into(imgPaymentProof);
                                
                                // Make image clickable to view full size
                                final String imageUrl = finalFullUrl;
                                imgPaymentProof.setOnClickListener(v -> {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setDataAndType(Uri.parse(imageUrl), "image/*");
                                    try {
                                        startActivity(intent);
                                    } catch (Exception e) {
                                        Toast.makeText(this, "Cannot open image viewer", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } catch (Exception e) {
                                Log.e("BookingDetails", "Error loading payment proof", e);
                                imgPaymentProof.setVisibility(View.GONE);
                                tvNoProof.setVisibility(View.VISIBLE);
                            }
                        } else {
                            // No payment proof available
                            imgPaymentProof.setVisibility(View.GONE);
                            tvNoProof.setVisibility(View.VISIBLE);
                            layoutPaymentProof.setVisibility(View.VISIBLE);
                        }
                    } else {
                        // No payment proof found
                        imgPaymentProof.setVisibility(View.GONE);
                        tvNoProof.setVisibility(View.VISIBLE);
                        layoutPaymentProof.setVisibility(View.VISIBLE);
                    }
                } catch (JSONException e) {
                    Log.e("BookingDetails", "Error parsing payment proof response", e);
                    imgPaymentProof.setVisibility(View.GONE);
                    tvNoProof.setVisibility(View.VISIBLE);
                    layoutPaymentProof.setVisibility(View.VISIBLE);
                }
            },
            error -> {
                Log.e("BookingDetails", "Error fetching payment proof: " + error.getMessage());
                imgPaymentProof.setVisibility(View.GONE);
                tvNoProof.setVisibility(View.VISIBLE);
                layoutPaymentProof.setVisibility(View.VISIBLE);
            }
        );
        
        requestQueue.add(stringRequest);
    }
    
    private String formatAmount(String amount) {
        try {
            double amountValue = Double.parseDouble(amount);
            java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,##0.00");
            return formatter.format(amountValue);
        } catch (NumberFormatException e) {
            return amount;
        }
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
        
        int bookingId = bookingData.getBookingId();
        
        Log.d("BookingDetails", "Approve booking - ownerId: " + ownerId + ", bookingId: " + bookingId);
        
        if (ownerId == 0) {
            hideProgressDialog();
            Toast.makeText(this, "Error: Owner ID not found. Please log in again.", Toast.LENGTH_LONG).show();
            Log.e("BookingDetails", "Owner ID is 0 - cannot approve booking");
            return;
        }
        
        if (bookingId == 0) {
            hideProgressDialog();
            Toast.makeText(this, "Error: Booking ID not found", Toast.LENGTH_SHORT).show();
            Log.e("BookingDetails", "Booking ID is 0 - cannot approve booking");
            return;
        }
        
        // Make API call to approve booking
        String url = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/approve_booking.php";
        
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("booking_id", bookingId);
            requestBody.put("owner_id", ownerId);
            
            Log.d("BookingDetails", "Request URL: " + url);
            Log.d("BookingDetails", "Request body: " + requestBody.toString());
            
            RequestQueue requestQueue = Volley.newRequestQueue(this);
            
            // Set timeout
            requestQueue.getCache().clear();
            
            JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, url, requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
            hideProgressDialog();
                        Log.d("BookingDetails", "Approve response: " + response.toString());
                        try {
                            if (response.getBoolean("success")) {
                                bookingData.setStatus("Confirmed");
                                loadBookingData(); // Reload to update status display
                                updateActionButtons(); // Update buttons after status change
                                Toast.makeText(BookingDetailsActivity.this, "Booking approved successfully!", Toast.LENGTH_SHORT).show();
                                
                                // Set result and finish to navigate back to pending tab
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("booking_updated", true);
                                resultIntent.putExtra("booking_id", bookingId);
                                resultIntent.putExtra("should_navigate_to_pending", true);
                                setResult(RESULT_OK, resultIntent);
                                finish(); // Navigate back to bookings activity
                            } else {
                                String errorMsg = response.optString("error", "Unknown error occurred");
                                Log.e("BookingDetails", "Approve error from server: " + errorMsg);
                                Toast.makeText(BookingDetailsActivity.this, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Log.e("BookingDetails", "Error parsing response", e);
                            hideProgressDialog();
                            Toast.makeText(BookingDetailsActivity.this, "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        hideProgressDialog();
                        Log.e("BookingDetails", "Volley error: " + error.toString());
                        Log.e("BookingDetails", "Error message: " + (error.getMessage() != null ? error.getMessage() : "null"));
                        Log.e("BookingDetails", "Error class: " + error.getClass().getName());
                        
                        String errorMessage = "Connection failed";
                        
                        if (error.networkResponse != null) {
                            int statusCode = error.networkResponse.statusCode;
                            Log.e("BookingDetails", "Network response status: " + statusCode);
                            
                            if (error.networkResponse.data != null) {
                                try {
                                    String responseBody = new String(error.networkResponse.data, "utf-8");
                                    Log.e("BookingDetails", "Error response body (first 500 chars): " + 
                                        (responseBody.length() > 500 ? responseBody.substring(0, 500) : responseBody));
                                    
                                    // Check for specific error types
                                    if (statusCode == 404) {
                                        errorMessage = "Server endpoint not found (404). Please verify approve_booking.php is on the server.";
                                    } else if (statusCode == 503) {
                                        if (responseBody.contains("ERR_NGROK_3004")) {
                                            errorMessage = "Server connection error (503). The server may be unreachable or ngrok is blocking the request.";
                                        } else {
                                            errorMessage = "Server temporarily unavailable (503). Please try again in a moment.";
                                        }
                                    } else if (responseBody.contains("<html") || responseBody.contains("ngrok") || 
                                        responseBody.contains("ERR_NGROK") || responseBody.contains("gateway error")) {
                                        if (responseBody.contains("ERR_NGROK_3004")) {
                                            errorMessage = "Server connection error. Please check if the server is running.";
                                        } else {
                                            errorMessage = "Server connection blocked by ngrok. Please try again.";
                                        }
                                    } else {
                                        // Try to parse as JSON
                                        try {
                                            JSONObject errorJson = new JSONObject(responseBody);
                                            if (errorJson.has("error")) {
                                                errorMessage = errorJson.getString("error");
                                            } else {
                                                errorMessage = "Server error: " + statusCode;
                                            }
                                        } catch (JSONException e) {
                                            errorMessage = "Server error: " + statusCode + " - " + 
                                                (responseBody.length() > 100 ? responseBody.substring(0, 100) : responseBody);
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e("BookingDetails", "Error parsing error response", e);
                                    if (statusCode == 404) {
                                        errorMessage = "Server endpoint not found (404)";
                                    } else if (statusCode == 503) {
                                        errorMessage = "Server temporarily unavailable (503)";
                                    } else {
                                        errorMessage = "Server error: " + statusCode;
                                    }
                                }
                            } else {
                                if (statusCode == 404) {
                                    errorMessage = "Server endpoint not found (404)";
                                } else if (statusCode == 503) {
                                    errorMessage = "Server temporarily unavailable (503)";
                                } else {
                                    errorMessage = "Server error: " + statusCode;
                                }
                            }
                        } else {
                            // Check error type
                            String errorClass = error.getClass().getSimpleName();
                            Log.e("BookingDetails", "Error class name: " + errorClass);
                            
                            if (error.getMessage() != null && !error.getMessage().isEmpty()) {
                                errorMessage = error.getMessage();
                            } else if (error.getCause() != null) {
                                String causeMessage = error.getCause().getMessage();
                                if (causeMessage != null && !causeMessage.isEmpty()) {
                                    errorMessage = causeMessage;
                                } else {
                                    errorMessage = "Network connection failed. Please check your internet connection.";
                                }
                            } else {
                                // Check error type by class name
                                if (errorClass.contains("Timeout")) {
                                    errorMessage = "Request timed out. Please try again.";
                                } else if (errorClass.contains("NoConnection")) {
                                    errorMessage = "No internet connection. Please check your network.";
                                } else if (errorClass.contains("Network")) {
                                    errorMessage = "Network error. Please check your connection.";
                                } else {
                                    errorMessage = "Connection failed. Please check your internet connection and try again.";
                                }
                            }
                        }
                        
                        Log.e("BookingDetails", "Final error message: " + errorMessage);
                        Toast.makeText(BookingDetailsActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            ) {
                @Override
                public java.util.Map<String, String> getHeaders() {
                    java.util.Map<String, String> headers = new java.util.HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("ngrok-skip-browser-warning", "true");
                    headers.put("Accept", "application/json");
                    headers.put("User-Agent", "BoardEase-Android-App");
                    return headers;
                }
                
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };
            
            // Set retry policy
            request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                    10000, // 10 seconds timeout
                    1, // 1 retry
                    com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));
            
            requestQueue.add(request);
        } catch (JSONException e) {
            Log.e("BookingDetails", "Error creating request", e);
            hideProgressDialog();
            Toast.makeText(this, "Error creating request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void declineBooking() {
        showProgressDialog("Declining booking...");
        
        int bookingId = bookingData.getBookingId();
        
        Log.d("BookingDetails", "Decline booking - ownerId: " + ownerId + ", bookingId: " + bookingId);
        
        if (ownerId == 0) {
            hideProgressDialog();
            Toast.makeText(this, "Error: Owner ID not found. Please log in again.", Toast.LENGTH_LONG).show();
            Log.e("BookingDetails", "Owner ID is 0 - cannot decline booking");
            return;
        }
        
        if (bookingId == 0) {
            hideProgressDialog();
            Toast.makeText(this, "Error: Booking ID not found", Toast.LENGTH_SHORT).show();
            Log.e("BookingDetails", "Booking ID is 0 - cannot decline booking");
            return;
        }
        
        // Make API call to decline booking
        String url = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/decline_booking.php";
        
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("booking_id", bookingId);
            requestBody.put("owner_id", ownerId);
            requestBody.put("reason", "Declined by owner");
            
            Log.d("BookingDetails", "Decline request URL: " + url);
            Log.d("BookingDetails", "Decline request body: " + requestBody.toString());
            
            RequestQueue requestQueue = Volley.newRequestQueue(this);
            
            // Set timeout
            requestQueue.getCache().clear();
            
            JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, url, requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
            hideProgressDialog();
                        Log.d("BookingDetails", "Decline response: " + response.toString());
                        try {
                            if (response.getBoolean("success")) {
                                bookingData.setStatus("Cancelled");
                                loadBookingData(); // Reload to update status display
                                updateActionButtons(); // Update buttons after status change
                                Toast.makeText(BookingDetailsActivity.this, "Booking declined.", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                // Don't finish immediately - let user see updated status, they can go back manually
                            } else {
                                String errorMsg = response.optString("error", "Unknown error occurred");
                                Log.e("BookingDetails", "Decline error from server: " + errorMsg);
                                Toast.makeText(BookingDetailsActivity.this, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Log.e("BookingDetails", "Error parsing decline response", e);
                            hideProgressDialog();
                            Toast.makeText(BookingDetailsActivity.this, "Error parsing response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        hideProgressDialog();
                        Log.e("BookingDetails", "Volley error (Decline): " + error.toString());
                        Log.e("BookingDetails", "Error message: " + (error.getMessage() != null ? error.getMessage() : "null"));
                        Log.e("BookingDetails", "Error class: " + error.getClass().getName());
                        
                        String errorMessage = "Connection failed";
                        
                        if (error.networkResponse != null) {
                            int statusCode = error.networkResponse.statusCode;
                            Log.e("BookingDetails", "Network response status: " + statusCode);
                            
                            if (error.networkResponse.data != null) {
                                try {
                                    String responseBody = new String(error.networkResponse.data, "utf-8");
                                    Log.e("BookingDetails", "Error response body (first 500 chars): " + 
                                        (responseBody.length() > 500 ? responseBody.substring(0, 500) : responseBody));
                                    
                                    // Check for specific error types
                                    if (statusCode == 404) {
                                        errorMessage = "Server endpoint not found (404). Please verify approve_booking.php is on the server.";
                                    } else if (statusCode == 503) {
                                        if (responseBody.contains("ERR_NGROK_3004")) {
                                            errorMessage = "Server connection error (503). The server may be unreachable or ngrok is blocking the request.";
                                        } else {
                                            errorMessage = "Server temporarily unavailable (503). Please try again in a moment.";
                                        }
                                    } else if (responseBody.contains("<html") || responseBody.contains("ngrok") || 
                                        responseBody.contains("ERR_NGROK") || responseBody.contains("gateway error")) {
                                        if (responseBody.contains("ERR_NGROK_3004")) {
                                            errorMessage = "Server connection error. Please check if the server is running.";
                                        } else {
                                            errorMessage = "Server connection blocked by ngrok. Please try again.";
                                        }
                                    } else {
                                        // Try to parse as JSON
                                        try {
                                            JSONObject errorJson = new JSONObject(responseBody);
                                            if (errorJson.has("error")) {
                                                errorMessage = errorJson.getString("error");
                                            } else {
                                                errorMessage = "Server error: " + statusCode;
                                            }
                                        } catch (JSONException e) {
                                            errorMessage = "Server error: " + statusCode + " - " + 
                                                (responseBody.length() > 100 ? responseBody.substring(0, 100) : responseBody);
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e("BookingDetails", "Error parsing error response", e);
                                    if (statusCode == 404) {
                                        errorMessage = "Server endpoint not found (404)";
                                    } else if (statusCode == 503) {
                                        errorMessage = "Server temporarily unavailable (503)";
                                    } else {
                                        errorMessage = "Server error: " + statusCode;
                                    }
                                }
                            } else {
                                if (statusCode == 404) {
                                    errorMessage = "Server endpoint not found (404)";
                                } else if (statusCode == 503) {
                                    errorMessage = "Server temporarily unavailable (503)";
                                } else {
                                    errorMessage = "Server error: " + statusCode;
                                }
                            }
                        } else {
                            // Check error type
                            String errorClass = error.getClass().getSimpleName();
                            Log.e("BookingDetails", "Error class name: " + errorClass);
                            
                            if (error.getMessage() != null && !error.getMessage().isEmpty()) {
                                errorMessage = error.getMessage();
                            } else if (error.getCause() != null) {
                                String causeMessage = error.getCause().getMessage();
                                if (causeMessage != null && !causeMessage.isEmpty()) {
                                    errorMessage = causeMessage;
                                } else {
                                    errorMessage = "Network connection failed. Please check your internet connection.";
                                }
                            } else {
                                // Check error type by class name
                                if (errorClass.contains("Timeout")) {
                                    errorMessage = "Request timed out. Please try again.";
                                } else if (errorClass.contains("NoConnection")) {
                                    errorMessage = "No internet connection. Please check your network.";
                                } else if (errorClass.contains("Network")) {
                                    errorMessage = "Network error. Please check your connection.";
                                } else {
                                    errorMessage = "Connection failed. Please check your internet connection and try again.";
                                }
                            }
                        }
                        
                        Log.e("BookingDetails", "Final error message: " + errorMessage);
                        Toast.makeText(BookingDetailsActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            ) {
                @Override
                public java.util.Map<String, String> getHeaders() {
                    java.util.Map<String, String> headers = new java.util.HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("ngrok-skip-browser-warning", "true");
                    headers.put("Accept", "application/json");
                    headers.put("User-Agent", "BoardEase-Android-App");
                    return headers;
                }
                
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };
            
            // Set retry policy
            request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                    10000, // 10 seconds timeout
                    1, // 1 retry
                    com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));
            
            requestQueue.add(request);
        } catch (JSONException e) {
            Log.e("BookingDetails", "Error creating decline request", e);
            hideProgressDialog();
            Toast.makeText(this, "Error creating request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void contactBoarder() {
        // Show contact options dialog
        String phoneNumber = bookingData.getPhoneNumber();
        String email = bookingData.getEmail();
        String boarderName = bookingData.getBoarderName();
        
        if ((phoneNumber == null || phoneNumber.isEmpty()) && (email == null || email.isEmpty())) {
            Toast.makeText(this, "No contact information available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] options;
        if (phoneNumber != null && !phoneNumber.isEmpty() && email != null && !email.isEmpty()) {
            options = new String[]{"Call", "Send SMS", "Send Email"};
        } else if (phoneNumber != null && !phoneNumber.isEmpty()) {
            options = new String[]{"Call", "Send SMS"};
        } else {
            options = new String[]{"Send Email"};
        }
        
        new AlertDialog.Builder(this)
                .setTitle("Contact " + boarderName)
                .setItems(options, (dialog, which) -> {
                    String selectedOption = options[which];
                    if (selectedOption.equals("Call")) {
                        makePhoneCall(phoneNumber);
                    } else if (selectedOption.equals("Send SMS")) {
                        sendSMS(phoneNumber);
                    } else if (selectedOption.equals("Send Email")) {
                        sendEmail(email, boarderName);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void makePhoneCall(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{android.Manifest.permission.CALL_PHONE}, 
                    PERMISSION_REQUEST_CALL_PHONE);
            return;
        }
        
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + phoneNumber));
        try {
            startActivity(callIntent);
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission denied to make phone call", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void sendSMS(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
        smsIntent.setData(Uri.parse("smsto:" + phoneNumber));
        smsIntent.putExtra("sms_body", "Hello, regarding your booking at " + bookingData.getBoardingHouseName());
        try {
            startActivity(smsIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open SMS app", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void sendEmail(String email, String boarderName) {
        if (email == null || email.isEmpty()) {
            Toast.makeText(this, "Email address not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:" + email));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Regarding Your Booking at " + bookingData.getBoardingHouseName());
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Hello " + boarderName + ",\n\nRegarding your booking:");
        try {
            startActivity(Intent.createChooser(emailIntent, "Send email using"));
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open email app", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void viewRoomDetails() {
        // Navigate to RoomViewActivity to show all rooms for this boarding house
        int boardingHouseId = bookingData.getBoardingHouseId();
        String boardingHouseName = bookingData.getBoardingHouseName();
        
        if (boardingHouseId == 0) {
            Toast.makeText(this, "Boarding house information not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(this, RoomViewActivity.class);
        intent.putExtra("bh_id", boardingHouseId);
        intent.putExtra("bh_name", boardingHouseName != null ? boardingHouseName : "Rooms");
        startActivity(intent);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CALL_PHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, make the call
                String phoneNumber = bookingData.getPhoneNumber();
                if (phoneNumber != null && !phoneNumber.isEmpty()) {
                    makePhoneCall(phoneNumber);
                }
            } else {
                Toast.makeText(this, "Permission denied to make phone call", Toast.LENGTH_SHORT).show();
            }
        }
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

















