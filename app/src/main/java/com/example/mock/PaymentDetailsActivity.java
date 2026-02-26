package com.example.mock;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PaymentDetailsActivity extends AppCompatActivity implements PaymentAdapter.PaymentActionListener {

    private ImageButton btnBack;
    private ImageView imgProfile, imgPaymentProof;
    private LinearLayout layoutPaymentProof, layoutHighlightSection;
    private TextView tvBoarderName, tvEmail, tvPhone, tvRoom, tvRentType, tvAmountPaid, tvTotalAmount;
    private TextView tvPaymentStatus, tvRentalStatus, tvPaymentDate, tvDueDate;
    private TextView tvDetailPaymentMethod, tvCreatedAt, tvUpdatedAt, tvButtonInfo, tvNoProof;
    private TextView tvNoBreakdown;
    private TextView tvBoardingHouseName, tvBoardingHouseAddress;
    private MaterialButton btnMarkAsPaid, btnMarkAsOverdue;
    private RecyclerView recyclerViewBreakdown;
    private ScrollView scrollView;
    private PaymentBreakdownAdapter breakdownAdapter;
    private List<PaymentBreakdownItem> breakdownItems;
    
    private PaymentData payment;
    private PaymentApiService paymentApiService;
    private ProgressDialog progressDialog;
    private int viewType = PaymentAdapter.VIEW_TYPE_ALL; // Default to all payments view

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_details);

        // Get payment data from intent
        payment = (PaymentData) getIntent().getSerializableExtra("payment");
        if (payment == null) {
            Toast.makeText(this, "Payment data not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get view type from intent (which tab it came from)
        viewType = getIntent().getIntExtra("view_type", PaymentAdapter.VIEW_TYPE_ALL);

        initializeViews();
        setupClickListeners();
        populateData();
        adjustUIForViewType();
        
        // Initialize API service
        paymentApiService = new PaymentApiService(this);
        
        // Load payment breakdown
        loadPaymentBreakdown();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        imgProfile = findViewById(R.id.imgProfile);
        tvBoarderName = findViewById(R.id.tvBoarderName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvRoom = findViewById(R.id.tvRoom);
        tvRentType = findViewById(R.id.tvRentType);
        tvAmountPaid = findViewById(R.id.tvAmountPaid);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus);
        tvRentalStatus = findViewById(R.id.tvRentalStatus);
        tvPaymentDate = findViewById(R.id.tvPaymentDate);
        tvDueDate = findViewById(R.id.tvDueDate);

        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvUpdatedAt = findViewById(R.id.tvUpdatedAt);
        tvButtonInfo = findViewById(R.id.tvButtonInfo);
        imgPaymentProof = findViewById(R.id.imgPaymentProof);
        layoutPaymentProof = findViewById(R.id.layoutPaymentProof);
        layoutHighlightSection = findViewById(R.id.layoutHighlightSection);
        tvNoProof = findViewById(R.id.tvNoProof);
        tvDetailPaymentMethod = findViewById(R.id.tvDetailPaymentMethod);
        
        tvBoardingHouseName = findViewById(R.id.tvBoardingHouseName);
        tvBoardingHouseAddress = findViewById(R.id.tvBoardingHouseAddress);
        
        btnMarkAsPaid = findViewById(R.id.btnMarkAsPaid);
        btnMarkAsOverdue = findViewById(R.id.btnMarkAsOverdue);
        
        // Breakdown views
        recyclerViewBreakdown = findViewById(R.id.recyclerViewBreakdown);
        scrollView = findViewById(R.id.scrollView);
        tvNoBreakdown = findViewById(R.id.tvNoBreakdown);
        breakdownItems = new ArrayList<>();
        breakdownAdapter = new PaymentBreakdownAdapter(breakdownItems);
        recyclerViewBreakdown.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewBreakdown.setAdapter(breakdownAdapter);
        
        breakdownAdapter.setOnPaymentItemClickListener(item -> {
            String proofUrl = item.getPaymentProof();
            String paymentDate = item.getPaymentDate();
            String dueDate = item.getDueDate();
            boolean isPaid = item.isPaid();
            
            // If item is not paid, check if there is a payment date (Pending status)
            if (!isPaid) {
                if (paymentDate != null && !paymentDate.isEmpty() && !paymentDate.equals("null")) {
                    // It has a date but not yet marked as paid (Pending)
                    // Keep the paymentDate as is for display
                } else {
                    paymentDate = "No Payment Submitted Yet";
                }
            } else if (paymentDate == null || paymentDate.isEmpty()) {
                paymentDate = "N/A";
            }
            
            updatePaymentProofDisplay(proofUrl, paymentDate, dueDate, isPaid, item.getPaymentMethod());
            
            // Visual feedback: Highlight the section
            if (layoutHighlightSection != null) {
                layoutHighlightSection.setBackgroundResource(R.drawable.bg_highlight_section);
            }
            
            // Auto-scroll to the highlight section
            if (scrollView != null && layoutHighlightSection != null) {
                // Scroll to the Y position of the highlight section
                scrollView.post(() -> {
                   scrollView.smoothScrollTo(0, layoutHighlightSection.getTop());
                });
            }
            
            Toast.makeText(this, "Showing details for " + item.getPeriodLabel(), Toast.LENGTH_SHORT).show();
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnMarkAsPaid.setOnClickListener(v -> showMarkAsPaidDialog());
        btnMarkAsOverdue.setOnClickListener(v -> showMarkAsOverdueDialog());
    }

    private void populateData() {
        if (payment == null) {
            Toast.makeText(this, "Payment data not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Log payment data for debugging
        android.util.Log.d("PaymentDetails", "=== Payment Data ===");
        android.util.Log.d("PaymentDetails", "Payment ID: " + payment.getPaymentId());
        android.util.Log.d("PaymentDetails", "Payment Proof: " + payment.getPaymentProof());
        android.util.Log.d("PaymentDetails", "Payment Status: " + payment.getPaymentStatus());
        
        // Set boarder name
        tvBoarderName.setText(payment.getBoarderName() != null ? payment.getBoarderName() : "Unknown");
        
        // Load profile picture
        if (imgProfile != null) {
            String profilePicture = payment.getProfilePicture();
            if (profilePicture != null && !profilePicture.isEmpty()) {
                String fullImageUrl = "https://boardease.calapebohol.com/" + profilePicture;
                Glide.with(this)
                        .load(fullImageUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(imgProfile);
            } else {
                imgProfile.setImageResource(R.drawable.ic_profile);
            }
        }
        
        // Hide email and phone (not available in PaymentData)
        if (tvEmail != null) {
            tvEmail.setVisibility(View.GONE);
        }
        if (tvPhone != null) {
            tvPhone.setVisibility(View.GONE);
        }
        
        // Set room information
        tvRoom.setText(payment.getRoom() != null ? payment.getRoom() : "Room N/A");
        tvRentType.setText(payment.getRentType() != null ? payment.getRentType() : "N/A");
        
        // Set boarding house information
        if (tvBoardingHouseName != null) {
            tvBoardingHouseName.setText(payment.getBoardingHouseName() != null ? payment.getBoardingHouseName() : "Boarding House Name");
        }
        if (tvBoardingHouseAddress != null) {
            tvBoardingHouseAddress.setText(payment.getBoardingHouseAddress() != null ? payment.getBoardingHouseAddress() : "Boarding House Address");
        }
        
        // Always show overall totals for Amount Paid and Total Amount
        tvAmountPaid.setText(payment.getAmountPaid());
        tvTotalAmount.setText(payment.getTotalAmount());
        
        // If in "Remaining" view, also highlight the remaining balance
        if (viewType == PaymentAdapter.VIEW_TYPE_REMAINING) {
            String remaining = payment.getRemainingAmountToPay();
            if (remaining != null && !remaining.isEmpty()) {
                try {
                    double remValue = Double.parseDouble(remaining);
                    if (remValue > 0) {
                        // Show remaining balance in the info text area as well
                        if (tvButtonInfo != null) {
                            tvButtonInfo.setText("Remaining Balance: ₱" + formatAmountForDetails(remaining));
                            tvButtonInfo.setVisibility(View.VISIBLE);
                            tvButtonInfo.setTextColor(Color.parseColor("#F44336")); // Red for attention
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }
        
        
        // Set dates with formatting
        String paymentDate = payment.getPaymentDate();
        if (paymentDate != null && !paymentDate.isEmpty() && !paymentDate.equals("N/A")) {
            tvPaymentDate.setText(formatDateTime(paymentDate));
        } else {
            tvPaymentDate.setText("N/A");
        }
        
        String dueDate = payment.getDueDate();
        if (dueDate != null && !dueDate.isEmpty() && !dueDate.equals("N/A")) {
            tvDueDate.setText(formatDateTime(dueDate));
        } else {
            tvDueDate.setText("N/A");
        }
        
        // Set status with styling - prioritize database status from payments table
        // The payment status should directly reflect what's in the payments table
        // Logic: payment_status in payments table is the source of truth
        // It's calculated based on payment_breakdowns: 
        // - "Fully Paid" if all periods paid (is_paid = 1)
        // - "Partially Paid" if some periods paid
        // - "Pending" if no periods paid
        String paymentStatus;
        int totalPeriods = payment.getTotalPeriods();
        int paidPeriods = payment.getPaidPeriods();
        String dbPaymentStatus = payment.getPaymentStatus();
        
        // Always use the database status from payments table as the source of truth
        // The backend already calculates this correctly based on payment_breakdowns
        // CRITICAL: Do NOT calculate from periods - always trust the database status
        if (dbPaymentStatus != null && !dbPaymentStatus.trim().isEmpty() && 
            !dbPaymentStatus.trim().equalsIgnoreCase("null") && 
            !dbPaymentStatus.trim().equalsIgnoreCase("")) {
            
            String statusLower = dbPaymentStatus.toLowerCase().trim();
            
            // Normalize the status format to match expected display format
            if (statusLower.equals("fully paid") || statusLower.equals("fully_paid")) {
                paymentStatus = "Fully Paid";
            } else if (statusLower.equals("partially paid") || 
                      statusLower.equals("partially_paid") ||
                      (statusLower.contains("partially") && statusLower.contains("paid")) ||
                      // Legacy support for old "Completed/Partially" status
                      statusLower.equals("completed/partially") || 
                      statusLower.equals("completed_partially") ||
                      (statusLower.contains("completed") && statusLower.contains("partially")) ||
                      // "Completed" status means partially paid when periods show partial payment
                      (statusLower.equals("completed") && paidPeriods > 0 && paidPeriods < totalPeriods)) {
                paymentStatus = "Partially Paid";
            } else if (statusLower.equals("completed") && paidPeriods >= totalPeriods && totalPeriods > 0) {
                // If database says "Completed" but all periods are paid, use "Fully Paid"
                paymentStatus = "Fully Paid";
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
            }
        } else {
            // Only if database status is completely missing, calculate from period data
            // This should rarely happen since backend always sets payment_status
            if (totalPeriods > 0) {
                if (paidPeriods >= totalPeriods) {
                    // All periods paid
                    paymentStatus = "Fully Paid";
                } else if (paidPeriods > 0) {
                    // Some periods paid but not all
                    paymentStatus = "Partially Paid";
                } else {
                    // No periods paid
                    paymentStatus = "Pending";
                }
            } else {
                // No breakdown data - default to pending
                paymentStatus = "Pending";
            }
        }
        
        tvPaymentStatus.setText(paymentStatus);
        applyPaymentStatusStyle(tvPaymentStatus, paymentStatus);
        
        // Set rental status
        String rentalStatus = payment.getRentalStatus() != null ? payment.getRentalStatus() : "";
        tvRentalStatus.setText(rentalStatus);
        
        // Set notes

        
        // Set timestamps with formatting
        String createdAt = payment.getCreatedAt();
        if (createdAt != null && !createdAt.isEmpty() && !createdAt.equals("N/A")) {
            tvCreatedAt.setText(formatDateTime(createdAt));
        } else {
            tvCreatedAt.setText("N/A");
        }
        
        String updatedAt = payment.getUpdatedAt();
        if (updatedAt != null && !updatedAt.isEmpty() && !updatedAt.equals("N/A")) {
            tvUpdatedAt.setText(formatDateTime(updatedAt));
        } else {
            tvUpdatedAt.setText("N/A");
        }
        
        // Load payment proof image
        loadPaymentProof();
        
        // Update button visibility based on current status
        updateButtonVisibility();
    }
    
    private void loadPaymentProof() {
        // Get payment proof URL
        String paymentProof = payment.getPaymentProof();
        String dueDate = payment.getDueDate();
        
        android.util.Log.d("PaymentDetails", "Payment Proof: " + paymentProof);
        
        String paymentProofUrl = null;
        if (paymentProof != null && !paymentProof.isEmpty() && !paymentProof.equals("null")) {
            paymentProofUrl = paymentProof;
        }
        
        // Update display with the resolved URL
        // Pass original due date and check if main payment is paid
        boolean isPaid = payment.getPaymentStatus() != null && 
                         (payment.getPaymentStatus().equalsIgnoreCase("Paid") || 
                          payment.getPaymentStatus().equalsIgnoreCase("Fully Paid") ||
                          payment.getPaymentStatus().equalsIgnoreCase("Completed"));
        String paymentMethod = payment.getPaymentMethod();
        updatePaymentProofDisplay(paymentProofUrl, null, dueDate, isPaid, paymentMethod);
    }
    
    private void updatePaymentProofDisplay(String paymentProofUrl, String paymentDate, String dueDate, boolean isPaid, String paymentMethod) {
        // Update payment method if provided
        if (tvDetailPaymentMethod != null && paymentMethod != null && !paymentMethod.isEmpty()) {
            tvDetailPaymentMethod.setText(paymentMethod);
        }

        // Update payment date if provided
        if (paymentDate != null && !paymentDate.isEmpty() && !paymentDate.equals("N/A")) {
            if (paymentDate.equals("No Payment Submitted Yet")) {
                tvPaymentDate.setText(paymentDate);
            } else {
                tvPaymentDate.setText(formatDateTime(paymentDate));
            }
        } else if (paymentDate != null) {
              tvPaymentDate.setText("N/A");
        }
        
        // Update due date if provided
        if (dueDate != null && !dueDate.isEmpty() && !dueDate.equals("N/A")) {
            tvDueDate.setText(formatDateTime(dueDate));
        } else if (dueDate != null) {
            tvDueDate.setText("N/A");
        }
        
        android.util.Log.d("PaymentDetails", "Updating proof display. URL: " + paymentProofUrl + ", IsPaid: " + isPaid);
        
        // Attempt to load image if a proof URL exists, regardless of paid status
        // This allows owners to see "Pending" payments that need verification
        if (paymentProofUrl != null && !paymentProofUrl.isEmpty() && !paymentProofUrl.equals("null")) {
            // Show image view and hide "no proof" text
            imgPaymentProof.setVisibility(View.VISIBLE);
            tvNoProof.setVisibility(View.GONE);
            layoutPaymentProof.setVisibility(View.VISIBLE);
            
            // Build full URL if it's a relative path
            String baseUrl = "https://boardease.calapebohol.com/";
            String urlToProcess = paymentProofUrl.trim();
            
            // Clean up path: Remove different versions of prefix
            if (urlToProcess.startsWith("/")) {
                urlToProcess = urlToProcess.substring(1);
            }
            if (urlToProcess.startsWith("BoardEase2/")) {
                urlToProcess = urlToProcess.substring(11);
            }
            if (urlToProcess.startsWith("/BoardEase2/")) {
                urlToProcess = urlToProcess.substring(12);
            }
            
            String finalFullUrl;
            if (urlToProcess.startsWith("http://") || urlToProcess.startsWith("https://")) {
                finalFullUrl = urlToProcess;
            } else {
                // Use get_payment_proof.php for reliable path resolution
                finalFullUrl = baseUrl + "get_payment_proof.php?path=" + Uri.encode(urlToProcess, "UTF-8");
            }
            
            android.util.Log.d("PaymentDetails", "Attempting to load image from: " + finalFullUrl);
            
            // Clear previous image first
            Glide.with(this).clear(imgPaymentProof);
            
            // Load image using Glide with better error handling
            RequestOptions requestOptions = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Enable cache
                    .skipMemoryCache(false) // Enable memory cache
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .error(android.R.drawable.ic_dialog_alert);
            
            // Create a final variable for the listener
            final String debugUrl = finalFullUrl;
            final String originalPath = urlToProcess;
            
            Glide.with(this)
                    .load(finalFullUrl)
                    .apply(requestOptions)
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                            android.util.Log.e("PaymentDetails", "=== GLIDE LOAD FAILED ===");
                            android.util.Log.e("PaymentDetails", "URL: " + debugUrl);
                            if (e != null) {
                                android.util.Log.e("PaymentDetails", "Exception: " + e.getMessage());
                            }
                            
                            runOnUiThread(() -> {
                                tvNoProof.setText("Payment proof image not found.\nPath: " + originalPath);
                                tvNoProof.setVisibility(View.VISIBLE);
                                imgPaymentProof.setVisibility(View.GONE);
                            });
                            return false;
                        }
                        
                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                             runOnUiThread(() -> {
                                tvNoProof.setVisibility(View.GONE);
                            });
                            return false;
                        }
                    })
                    .into(imgPaymentProof);
            
            // Make image clickable to view full size (use final variable for lambda)
            final String imageUrl = finalFullUrl;
            imgPaymentProof.setOnClickListener(v -> {
                // Open image in full screen or external viewer
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(imageUrl), "image/*");
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Cannot open image viewer", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Hide image view and show "no proof" text
            imgPaymentProof.setVisibility(View.GONE);
            tvNoProof.setVisibility(View.VISIBLE);
            
            // Handle Cash payment specific message
            if (paymentMethod != null && paymentMethod.equalsIgnoreCase("Cash")) {
                if (!isPaid) {
                    tvNoProof.setText("Cash Payment - no proof required. Please confirm or mark as paid if you have received the payment.");
                    tvNoProof.setTextColor(getResources().getColor(R.color.brown));
                } else {
                    tvNoProof.setText("No proof uploaded for cash payments");
                    tvNoProof.setTextColor(getResources().getColor(R.color.gray));
                }
            } else if (!isPaid) {
                tvNoProof.setText("No payment submitted yet");
                tvNoProof.setTextColor(getResources().getColor(R.color.gray));
            } else {
                tvNoProof.setText("No payment proof for this item");
                tvNoProof.setTextColor(getResources().getColor(R.color.gray));
            }
            
            layoutPaymentProof.setVisibility(View.VISIBLE);
        }
    }
    
    private void applyPaymentStatusStyle(TextView textView, String paymentStatus) {
        if (textView == null || paymentStatus == null || paymentStatus.isEmpty()) return;
        
        int textColor;
        int backgroundRes;
        
        String statusLower = paymentStatus.toLowerCase().trim();
        
        switch (statusLower) {
            case "fully paid":
            case "fully_paid":
                textColor = getResources().getColor(android.R.color.white);
                backgroundRes = R.drawable.bg_status_approved;
                break;
            case "paid":
                textColor = getResources().getColor(android.R.color.white);
                backgroundRes = R.drawable.bg_status_approved;
                break;
            case "partially paid":
            case "partially_paid":
            case "completed/partially":
            case "completed_partially":
                // Partially paid - use blue color
                textColor = getResources().getColor(android.R.color.white);
                backgroundRes = R.drawable.bg_status_completed;
                break;
            case "completed":
                // Completed means paid but may have remaining balance - use blue color
                textColor = getResources().getColor(android.R.color.white);
                backgroundRes = R.drawable.bg_status_completed;
                break;
            case "overdue":
            case "failed":
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
        textView.setTextColor(textColor);
        textView.setBackgroundResource(backgroundRes);
        textView.setPadding(padding, padding / 2, padding, padding / 2);
    }

    private void adjustUIForViewType() {
        // Adjust UI based on which tab the payment came from
        switch (viewType) {
            case PaymentAdapter.VIEW_TYPE_FULLY_PAID:
                // Fully Paid: Hide action buttons, show completion message
                // Payment proof should still be visible if it exists
                if (btnMarkAsPaid != null) btnMarkAsPaid.setVisibility(View.GONE);
                if (btnMarkAsOverdue != null) btnMarkAsOverdue.setVisibility(View.GONE);
                if (tvButtonInfo != null) {
                    tvButtonInfo.setText("This booking is fully paid. All payment periods have been completed.");
                    tvButtonInfo.setVisibility(View.VISIBLE);
                }
                // Ensure payment proof section is visible (it's loaded in populateData)
                break;
            case PaymentAdapter.VIEW_TYPE_REMAINING:
                // Remaining: Show action buttons for updating payment status
                // Payment proof should be visible - important for verifying payments
                updateButtonVisibility();
                break;
            case PaymentAdapter.VIEW_TYPE_ALL:
            default:
                // All Payments: Check if fully paid first, then show action buttons based on payment status
                // If fully paid, hide buttons (same as Fully Paid tab)
                boolean isFullyPaid = payment.isFullyPaid();
                if (isFullyPaid) {
                    // Hide buttons for fully paid payments
                    if (btnMarkAsPaid != null) btnMarkAsPaid.setVisibility(View.GONE);
                    if (btnMarkAsOverdue != null) btnMarkAsOverdue.setVisibility(View.GONE);
                    if (tvButtonInfo != null) {
                        tvButtonInfo.setText("This booking is fully paid. All payment periods have been completed.");
                        tvButtonInfo.setVisibility(View.VISIBLE);
                    }
                } else {
                    // Not fully paid, show buttons based on payment status
                    updateButtonVisibility();
                }
                // Payment proof should be visible
                break;
        }
        
        // Payment proof section should always be visible if payment has proof
        // It's already handled in loadPaymentProof() method
    }
    
    private void updateButtonVisibility() {
        String status = payment.getPaymentStatus() != null ? payment.getPaymentStatus().toLowerCase() : "";
        String infoText = "";
        
        switch (status) {
            case "pending":
                btnMarkAsPaid.setVisibility(View.VISIBLE);
                btnMarkAsOverdue.setVisibility(View.VISIBLE);
                infoText = "Click 'Mark as Paid' when boarder has paid. Click 'Mark as Overdue' if payment is late.";
                break;
            case "paid":
            case "completed":
                btnMarkAsPaid.setVisibility(View.GONE);
                btnMarkAsOverdue.setVisibility(View.VISIBLE);
                infoText = "Payment is already completed. Click 'Mark as Overdue' if needed to change status.";
                break;
            case "overdue":
                btnMarkAsPaid.setVisibility(View.VISIBLE);
                btnMarkAsOverdue.setVisibility(View.GONE);
                infoText = "Payment is overdue. Click 'Mark as Paid' once payment is received.";
                break;
            default:
                btnMarkAsPaid.setVisibility(View.VISIBLE);
                btnMarkAsOverdue.setVisibility(View.VISIBLE);
                infoText = "Update payment status based on actual payment received from boarder.";
                break;
        }
        
        // Update info text
        if (tvButtonInfo != null) {
            if (!infoText.isEmpty()) {
                tvButtonInfo.setText(infoText);
                tvButtonInfo.setVisibility(View.VISIBLE);
            } else {
                tvButtonInfo.setVisibility(View.GONE);
            }
        }
    }

    private void showMarkAsPaidDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Mark as Paid")
                .setMessage("Have you verified that the boarder has paid this amount?\n\n" +
                           "This will update the payment status to 'Paid' and record the payment as completed.")
                .setPositiveButton("Yes, Mark as Paid", (dialog, which) -> markPaymentAsPaid())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMarkAsOverdueDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Mark as Overdue")
                .setMessage("Is this payment past the due date and unpaid?\n\n" +
                           "This will update the payment status to 'Overdue' to track late payments.")
                .setPositiveButton("Yes, Mark as Overdue", (dialog, which) -> markPaymentAsOverdue())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void markPaymentAsPaid() {
        showProgressDialog("Updating payment status...");
        
        paymentApiService.updatePaymentStatus(payment.getPaymentId(), "paid", "Marked as paid by owner", 
                new PaymentApiService.PaymentUpdateCallback() {
                    @Override
                    public void onSuccess(String message, String newStatus) {
                        hideProgressDialog();
                        Toast.makeText(PaymentDetailsActivity.this, message, Toast.LENGTH_SHORT).show();
                        // Update local payment data
                        if (newStatus != null && !newStatus.isEmpty()) {
                            payment.setPaymentStatus(newStatus);
                        } else {
                            payment.setPaymentStatus("Paid");
                        }
                        // Refresh the UI
                        populateData();
                        // Set result to notify parent activity to refresh and navigate back to original tab
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("payment_updated", true);
                        resultIntent.putExtra("new_payment_status", newStatus != null ? newStatus : "Paid");
                        resultIntent.putExtra("view_type", viewType); // Pass back the original tab
                        resultIntent.putExtra("payment_id", payment.getPaymentId());
                        setResult(RESULT_OK, resultIntent);
                        finish(); // Navigate back to the original tab
                    }

                    @Override
                    public void onError(String error) {
                        hideProgressDialog();
                        Toast.makeText(PaymentDetailsActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void markPaymentAsOverdue() {
        showProgressDialog("Updating payment status...");
        
        paymentApiService.updatePaymentStatus(payment.getPaymentId(), "overdue", "Marked as overdue by owner", 
                new PaymentApiService.PaymentUpdateCallback() {
                    @Override
                    public void onSuccess(String message, String newStatus) {
                        hideProgressDialog();
                        Toast.makeText(PaymentDetailsActivity.this, message, Toast.LENGTH_SHORT).show();
                        // Update local payment data
                        if (newStatus != null && !newStatus.isEmpty()) {
                            payment.setPaymentStatus(newStatus);
                        } else {
                            payment.setPaymentStatus("Overdue");
                        }
                        // Refresh the UI
                        populateData();
                        // Set result to notify parent activity to refresh and navigate back to original tab
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("payment_updated", true);
                        resultIntent.putExtra("new_payment_status", newStatus != null ? newStatus : "Overdue");
                        resultIntent.putExtra("view_type", viewType); // Pass back the original tab
                        resultIntent.putExtra("payment_id", payment.getPaymentId());
                        setResult(RESULT_OK, resultIntent);
                        finish(); // Navigate back to the original tab
                    }

                    @Override
                    public void onError(String error) {
                        hideProgressDialog();
                        Toast.makeText(PaymentDetailsActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
    
    private String formatAmountForDetails(String amount) {
        // Format amount with commas (e.g., "1000.00" -> "1,000.00")
        try {
            double amountValue = Double.parseDouble(amount);
            java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,##0.00");
            return formatter.format(amountValue);
        } catch (NumberFormatException e) {
            return amount;
        }
    }
    
    private String formatDateTime(String dateTime) {
        // Format from "YYYY-MM-DD HH:MM:SS" (UTC) to "MMM DD, YYYY hh:mm a" (Local Time)
        // e.g., "2025-01-15 18:00:00" -> "Jan 16, 2025 02:00 AM" (if PHP+8)
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            inputFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC")); // Server sends UTC
            
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault());
            outputFormat.setTimeZone(java.util.TimeZone.getDefault()); // Display in Local Time
            
            java.util.Date date = inputFormat.parse(dateTime);
            return outputFormat.format(date);
        } catch (Exception e) {
            // If parsing fails, try date-only format
            try {
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                // Date only usually doesn't need timezone conversion as it represents a day, 
                // but if it's strictly UTC date, we might want to keep it as is or convert. 
                // For due dates/payment dates without time, usually best to keep as is to avoid shifting day.
                
                java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
                java.util.Date date = inputFormat.parse(dateTime);
                return outputFormat.format(date);
            } catch (Exception e2) {
                return dateTime; // Return original if parsing fails
            }
        }
    }

    private void loadPaymentBreakdown() {
        if (payment == null) {
            android.util.Log.e("PaymentDetails", "Payment is null, cannot load breakdown");
            // Hide breakdown section if no payment
            if (recyclerViewBreakdown != null) {
                recyclerViewBreakdown.setVisibility(View.GONE);
            }
            if (tvNoBreakdown != null) {
                tvNoBreakdown.setVisibility(View.VISIBLE);
                tvNoBreakdown.setText("Payment data not available");
            }
            return;
        }
        
        int bookingId = payment.getBookingId();
        android.util.Log.d("PaymentDetails", "Loading payment breakdown for booking_id: " + bookingId);
        android.util.Log.d("PaymentDetails", "Payment ID: " + payment.getPaymentId());
        android.util.Log.d("PaymentDetails", "Payment data - paid_amount_for_booking: " + payment.getPaidAmountForBooking());
        android.util.Log.d("PaymentDetails", "Payment data - total_amount_for_booking: " + payment.getTotalAmountForBooking());
        android.util.Log.d("PaymentDetails", "Payment data - amount_paid: " + payment.getAmountPaid());
        android.util.Log.d("PaymentDetails", "Payment data - total_amount: " + payment.getTotalAmount());
        
        if (bookingId == 0) {
            android.util.Log.w("PaymentDetails", "Booking ID is 0, cannot load breakdown");
            // Hide breakdown section if no booking ID
            if (recyclerViewBreakdown != null) {
                recyclerViewBreakdown.setVisibility(View.GONE);
            }
            if (tvNoBreakdown != null) {
                tvNoBreakdown.setVisibility(View.VISIBLE);
                tvNoBreakdown.setText("Booking information not available");
            }
            return;
        }

        paymentApiService.getPaymentBreakdown(bookingId, new PaymentApiService.PaymentBreakdownCallback() {
            @Override
            public void onSuccess(List<PaymentBreakdownItem> breakdowns) {
                breakdownItems.clear();
                breakdownItems.addAll(breakdowns);
                breakdownAdapter.notifyDataSetChanged();
                
                // Show/hide views based on data
                if (breakdownItems.isEmpty()) {
                    recyclerViewBreakdown.setVisibility(View.GONE);
                    tvNoBreakdown.setVisibility(View.VISIBLE);
                } else {
                    recyclerViewBreakdown.setVisibility(View.VISIBLE);
                    tvNoBreakdown.setVisibility(View.GONE);
                    
                    // Identify and display the latest relevant item (Paid, has Proof, or has Submission)
                    PaymentBreakdownItem latestRelevant = null;
                    // Scan backwards to find the most recent item that is either paid or has proof/info submitted
                    for (int i = breakdownItems.size() - 1; i >= 0; i--) {
                        PaymentBreakdownItem item = breakdownItems.get(i);
                        String proof = item.getPaymentProof();
                        boolean hasProof = proof != null && !proof.isEmpty() && !proof.equals("null");
                        
                        // Check if a payment has been submitted (even if unpaid and no proof, e.g. Cash)
                        boolean hasSubmission = item.getPaymentId() != null && item.getPaymentId() != 0;
                        
                        if (item.isPaid() || hasProof || hasSubmission) {
                            latestRelevant = item;
                            break;
                        }
                    }
                    
                    if (latestRelevant != null) {
                        android.util.Log.d("PaymentDetails", "Defaulting to latest relevant item: " + latestRelevant.getPeriodLabel());
                        
                        String pDate = latestRelevant.getPaymentDate();
                        if (!latestRelevant.isPaid()) {
                            if (pDate == null || pDate.isEmpty() || pDate.equals("null")) {
                                pDate = "No Payment Submitted Yet";
                            }
                        }
                        
                        updatePaymentProofDisplay(latestRelevant.getPaymentProof(), pDate, latestRelevant.getDueDate(), latestRelevant.isPaid(), latestRelevant.getPaymentMethod());
                    }
                }
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("PaymentDetails", "Error loading payment breakdown: " + error);
                recyclerViewBreakdown.setVisibility(View.GONE);
                tvNoBreakdown.setVisibility(View.VISIBLE);
                tvNoBreakdown.setText("Unable to load payment breakdown");
            }
        });
    }

    // PaymentActionListener implementation (for compatibility)
    @Override
    public void onMarkAsPaid(PaymentData payment) {
        this.payment = payment;
        populateData();
        markPaymentAsPaid();
    }

    @Override
    public void onMarkAsOverdue(PaymentData payment) {
        this.payment = payment;
        populateData();
        markPaymentAsOverdue();
    }

    @Override
    public void onViewDetails(PaymentData payment) {
        // Already viewing details
    }

    @Override
    public void onSendReminder(PaymentData payment) {
        // Not used in payment details
    }
}
















