package com.example.mock;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PaymentDetailsActivity extends AppCompatActivity implements PaymentAdapter.PaymentActionListener {

    private TextView tvBoarderName, tvRoom, tvRentType, tvAmountPaid, tvTotalAmount;
    private TextView tvPaymentStatus, tvRentalStatus, tvPaymentDate, tvDueDate;
    private TextView tvPaymentMethod, tvNotes, tvCreatedAt, tvUpdatedAt;
    private Button btnMarkAsPaid, btnMarkAsOverdue;
    
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
        tvBoarderName = findViewById(R.id.tvBoarderName);
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
        
        btnMarkAsPaid = findViewById(R.id.btnMarkAsPaid);
        btnMarkAsOverdue = findViewById(R.id.btnMarkAsOverdue);
    }

    private void setupClickListeners() {
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());
        
        btnMarkAsPaid.setOnClickListener(v -> showMarkAsPaidDialog());
        btnMarkAsOverdue.setOnClickListener(v -> showMarkAsOverdueDialog());
    }

    private void populateData() {
        tvBoarderName.setText(payment.getBoarderName());
        tvRoom.setText(payment.getRoom());
        tvRentType.setText(payment.getRentType());
        tvAmountPaid.setText(payment.getAmountPaid());
        tvTotalAmount.setText(payment.getTotalAmount());
        tvPaymentStatus.setText(payment.getPaymentStatus());
        tvRentalStatus.setText(payment.getRentalStatus());
        tvPaymentDate.setText(payment.getPaymentDate());
        tvDueDate.setText(payment.getDueDate());
        tvPaymentMethod.setText(payment.getPaymentMethod());
        tvNotes.setText(payment.getNotes());
        tvCreatedAt.setText(payment.getCreatedAt());
        tvUpdatedAt.setText(payment.getUpdatedAt());
        
        // Update button visibility based on current status
        updateButtonVisibility();
    }

    private void updateButtonVisibility() {
        String status = payment.getPaymentStatus().toLowerCase();
        
        switch (status) {
            case "pending":
                btnMarkAsPaid.setVisibility(View.VISIBLE);
                btnMarkAsOverdue.setVisibility(View.VISIBLE);
                break;
            case "paid":
            case "completed":
                btnMarkAsPaid.setVisibility(View.GONE);
                btnMarkAsOverdue.setVisibility(View.VISIBLE);
                break;
            case "overdue":
                btnMarkAsPaid.setVisibility(View.VISIBLE);
                btnMarkAsOverdue.setVisibility(View.GONE);
                break;
            default:
                btnMarkAsPaid.setVisibility(View.VISIBLE);
                btnMarkAsOverdue.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void showMarkAsPaidDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Mark as Paid")
                .setMessage("Are you sure you want to mark this payment as paid?")
                .setPositiveButton("Yes", (dialog, which) -> markPaymentAsPaid())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMarkAsOverdueDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Mark as Overdue")
                .setMessage("Are you sure you want to mark this payment as overdue?")
                .setPositiveButton("Yes", (dialog, which) -> markPaymentAsOverdue())
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
                        payment.setPaymentStatus("Paid");
                        populateData();
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
                        payment.setPaymentStatus("Overdue");
                        populateData();
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
















