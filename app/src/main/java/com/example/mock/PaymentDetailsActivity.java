package com.example.mock;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;

public class PaymentDetailsActivity extends AppCompatActivity implements PaymentAdapter.PaymentActionListener {

    private ImageButton btnBack;
    private ImageView imgProfile, imgPaymentProof;
    private LinearLayout layoutPaymentProof;
    private TextView tvBoarderName, tvEmail, tvPhone, tvRoom, tvRentType, tvAmountPaid, tvTotalAmount;
    private TextView tvPaymentStatus, tvRentalStatus, tvPaymentDate, tvDueDate;
    private TextView tvPaymentMethod, tvNotes, tvCreatedAt, tvUpdatedAt, tvButtonInfo, tvNoProof;
    private MaterialButton btnMarkAsPaid, btnMarkAsOverdue;
    
    private PaymentData payment;
    private PaymentApiService paymentApiService;
    private ProgressDialog progressDialog;

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

        initializeViews();
        setupClickListeners();
        populateData();
        
        // Initialize API service
        paymentApiService = new PaymentApiService(this);
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
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        tvNotes = findViewById(R.id.tvNotes);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvUpdatedAt = findViewById(R.id.tvUpdatedAt);
        tvButtonInfo = findViewById(R.id.tvButtonInfo);
        imgPaymentProof = findViewById(R.id.imgPaymentProof);
        layoutPaymentProof = findViewById(R.id.layoutPaymentProof);
        tvNoProof = findViewById(R.id.tvNoProof);
        
        btnMarkAsPaid = findViewById(R.id.btnMarkAsPaid);
        btnMarkAsOverdue = findViewById(R.id.btnMarkAsOverdue);
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
        android.util.Log.d("PaymentDetails", "Receipt URL: " + payment.getReceiptUrl());
        android.util.Log.d("PaymentDetails", "Payment Proof: " + payment.getPaymentProof());
        android.util.Log.d("PaymentDetails", "Payment Status: " + payment.getPaymentStatus());
        
        // Set boarder name
        tvBoarderName.setText(payment.getBoarderName() != null ? payment.getBoarderName() : "Unknown");
        
        // Set profile image (placeholder for now)
        if (imgProfile != null) {
            imgProfile.setImageResource(R.drawable.ic_profile);
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
        
        // Set payment amounts
        tvAmountPaid.setText(payment.getAmountPaid() != null ? payment.getAmountPaid() : "₱0.00");
        tvTotalAmount.setText(payment.getTotalAmount() != null ? payment.getTotalAmount() : "₱0.00");
        
        // Set payment method
        tvPaymentMethod.setText(payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "N/A");
        
        // Set dates
        tvPaymentDate.setText(payment.getPaymentDate() != null ? payment.getPaymentDate() : "N/A");
        tvDueDate.setText(payment.getDueDate() != null && !payment.getDueDate().isEmpty() ? payment.getDueDate() : "N/A");
        
        // Set status with styling
        String paymentStatus = payment.getPaymentStatus() != null ? payment.getPaymentStatus() : "";
        tvPaymentStatus.setText(paymentStatus);
        applyPaymentStatusStyle(tvPaymentStatus, paymentStatus);
        
        // Set rental status
        String rentalStatus = payment.getRentalStatus() != null ? payment.getRentalStatus() : "";
        tvRentalStatus.setText(rentalStatus);
        
        // Set notes
        String notes = payment.getNotes() != null && !payment.getNotes().isEmpty() ? payment.getNotes() : "No notes available";
        tvNotes.setText(notes);
        
        // Set timestamps
        tvCreatedAt.setText(payment.getCreatedAt() != null ? payment.getCreatedAt() : "N/A");
        tvUpdatedAt.setText(payment.getUpdatedAt() != null ? payment.getUpdatedAt() : "N/A");
        
        // Load payment proof image
        loadPaymentProof();
        
        // Update button visibility based on current status
        updateButtonVisibility();
    }
    
    private void loadPaymentProof() {
        // Get payment proof URL (prefer receipt_url, fallback to payment_proof)
        String receiptUrl = payment.getReceiptUrl();
        String paymentProof = payment.getPaymentProof();
        
        android.util.Log.d("PaymentDetails", "Receipt URL: " + receiptUrl);
        android.util.Log.d("PaymentDetails", "Payment Proof: " + paymentProof);
        
        String paymentProofUrl = null;
        if (receiptUrl != null && !receiptUrl.isEmpty() && !receiptUrl.equals("null")) {
            paymentProofUrl = receiptUrl;
        } else if (paymentProof != null && !paymentProof.isEmpty() && !paymentProof.equals("null")) {
            paymentProofUrl = paymentProof;
        }
        
        android.util.Log.d("PaymentDetails", "Final Payment Proof URL: " + paymentProofUrl);
        
        if (paymentProofUrl != null && !paymentProofUrl.isEmpty() && !paymentProofUrl.equals("null")) {
            // Show image view and hide "no proof" text
            imgPaymentProof.setVisibility(View.VISIBLE);
            tvNoProof.setVisibility(View.GONE);
            layoutPaymentProof.setVisibility(View.VISIBLE);
            
            // Build full URL if it's a relative path
            String baseUrl = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/";
            String urlToProcess = paymentProofUrl.trim();
            String finalFullUrl;
            
            if (urlToProcess.startsWith("http://") || urlToProcess.startsWith("https://")) {
                // Already a full URL
                finalFullUrl = urlToProcess;
            } else {
                // Relative path - try direct file access first, then fallback to endpoint
                // Remove leading slash if present
                if (urlToProcess.startsWith("/")) {
                    urlToProcess = urlToProcess.substring(1);
                }
                // Remove "BoardEase2/" if it's already in the path
                if (urlToProcess.startsWith("BoardEase2/")) {
                    urlToProcess = urlToProcess.substring(11); // Remove "BoardEase2/"
                }
                
                // First, try direct file access (faster)
                String directUrl = baseUrl + urlToProcess;
                // Also prepare endpoint URL as fallback
                String endpointUrl = baseUrl + "get_payment_proof.php?path=" + android.net.Uri.encode(urlToProcess, "UTF-8");
                
                // Try endpoint first (more reliable for serving files through PHP)
                finalFullUrl = endpointUrl;
                
                android.util.Log.d("PaymentDetails", "Direct URL: " + directUrl);
                android.util.Log.d("PaymentDetails", "Endpoint URL: " + endpointUrl);
            }
            
            android.util.Log.d("PaymentDetails", "Final Full URL: " + finalFullUrl);
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
                            android.util.Log.e("PaymentDetails", "Original path: " + originalPath);
                            android.util.Log.e("PaymentDetails", "Model: " + (model != null ? model.toString() : "null"));
                            if (e != null) {
                                android.util.Log.e("PaymentDetails", "Exception: " + e.getMessage());
                                if (e.getRootCauses() != null && !e.getRootCauses().isEmpty()) {
                                    android.util.Log.e("PaymentDetails", "Root causes:");
                                    for (Throwable cause : e.getRootCauses()) {
                                        android.util.Log.e("PaymentDetails", "  - " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
                                    }
                                }
                            }
                            
                            // If endpoint failed, we could try direct URL, but for now just show error
                            runOnUiThread(() -> {
                                tvNoProof.setText("Payment proof image not found.\nPath: " + originalPath);
                                tvNoProof.setVisibility(View.VISIBLE);
                                imgPaymentProof.setVisibility(View.GONE);
                            });
                            return false; // Let Glide handle the error (show error placeholder)
                        }
                        
                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            android.util.Log.d("PaymentDetails", "=== GLIDE LOAD SUCCESS ===");
                            android.util.Log.d("PaymentDetails", "URL: " + debugUrl);
                            android.util.Log.d("PaymentDetails", "DataSource: " + dataSource.name());
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
                    android.util.Log.e("PaymentDetails", "Error opening image viewer", e);
                }
            });
        } else {
            // Hide image view and show "no proof" text
            android.util.Log.d("PaymentDetails", "No payment proof URL available");
            imgPaymentProof.setVisibility(View.GONE);
            tvNoProof.setVisibility(View.VISIBLE);
            layoutPaymentProof.setVisibility(View.VISIBLE);
        }
    }
    
    private void applyPaymentStatusStyle(TextView textView, String paymentStatus) {
        if (textView == null || paymentStatus == null || paymentStatus.isEmpty()) return;
        
        int textColor;
        int backgroundRes;
        
        switch (paymentStatus.toLowerCase()) {
            case "paid":
            case "completed":
                textColor = getResources().getColor(android.R.color.white);
                backgroundRes = R.drawable.bg_status_approved;
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
                    public void onSuccess(String message) {
                        hideProgressDialog();
                        Toast.makeText(PaymentDetailsActivity.this, message, Toast.LENGTH_SHORT).show();
                        // Update local payment data
                        payment.setPaymentStatus("Paid");
                        // Refresh the UI
                        populateData();
                        // Set result to notify parent activity to refresh
                        setResult(RESULT_OK);
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
                    public void onSuccess(String message) {
                        hideProgressDialog();
                        Toast.makeText(PaymentDetailsActivity.this, message, Toast.LENGTH_SHORT).show();
                        // Update local payment data
                        payment.setPaymentStatus("Overdue");
                        // Refresh the UI
                        populateData();
                        // Set result to notify parent activity to refresh
                        setResult(RESULT_OK);
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
}
















