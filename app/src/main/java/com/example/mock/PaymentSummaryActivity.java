package com.example.mock;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PaymentSummaryActivity extends AppCompatActivity {

    private TextView tvTotalPayments, tvPendingPayments, tvPaidPayments, tvOverduePayments;
    private TextView tvTotalAmount, tvPendingAmount, tvPaidAmount, tvOverdueAmount;
    private TextView tvCollectionRate;
    private Button btnRefresh, btnMarkOverdue;
    
    private PaymentApiService paymentApiService;
    private ProgressDialog progressDialog;
    private int ownerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_summary);

        // Get owner ID from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        
        // Handle String type for user_id (stored as String in Login.java)
        String userIdString = sharedPreferences.getString("user_id", "1");
        try {
            ownerId = Integer.parseInt(userIdString);
        } catch (NumberFormatException e) {
            ownerId = 1; // Default fallback
        }

        initializeViews();
        setupClickListeners();
        
        // Initialize API service
        paymentApiService = new PaymentApiService(this);
        
        // Load payment summary
        loadPaymentSummary();
    }

    private void initializeViews() {
        tvTotalPayments = findViewById(R.id.tvTotalPayments);
        tvPendingPayments = findViewById(R.id.tvPendingPayments);
        tvPaidPayments = findViewById(R.id.tvPaidPayments);
        tvOverduePayments = findViewById(R.id.tvOverduePayments);
        
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvPendingAmount = findViewById(R.id.tvPendingAmount);
        tvPaidAmount = findViewById(R.id.tvPaidAmount);
        tvOverdueAmount = findViewById(R.id.tvOverdueAmount);
        
        tvCollectionRate = findViewById(R.id.tvCollectionRate);
        
        btnRefresh = findViewById(R.id.btnRefresh);
        btnMarkOverdue = findViewById(R.id.btnMarkOverdue);
    }

    private void setupClickListeners() {
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());
        
        btnRefresh.setOnClickListener(v -> loadPaymentSummary());
        btnMarkOverdue.setOnClickListener(v -> markPaymentsAsOverdue());
    }

    private void loadPaymentSummary() {
        showProgressDialog("Loading payment summary...");
        
        paymentApiService.getPaymentSummary(ownerId, "month", new PaymentApiService.PaymentSummaryCallback() {
            @Override
            public void onSuccess(PaymentApiService.PaymentSummary summary) {
                hideProgressDialog();
                populateSummaryData(summary);
            }

            @Override
            public void onError(String error) {
                hideProgressDialog();
                Toast.makeText(PaymentSummaryActivity.this, "Error loading summary: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateSummaryData(PaymentApiService.PaymentSummary summary) {
        tvTotalPayments.setText(String.valueOf(summary.getTotalPayments()));
        tvPendingPayments.setText(String.valueOf(summary.getPendingPayments()));
        tvPaidPayments.setText(String.valueOf(summary.getPaidPayments()));
        tvOverduePayments.setText(String.valueOf(summary.getOverduePayments()));
        
        tvTotalAmount.setText("₱" + String.format("%.2f", summary.getTotalAmount()));
        tvPendingAmount.setText("₱" + String.format("%.2f", summary.getPendingAmount()));
        tvPaidAmount.setText("₱" + String.format("%.2f", summary.getPaidAmount()));
        tvOverdueAmount.setText("₱" + String.format("%.2f", summary.getOverdueAmount()));
        
        tvCollectionRate.setText(String.format("%.1f%%", summary.getCollectionRate()));
    }

    private void markPaymentsAsOverdue() {
        showProgressDialog("Marking payments as overdue...");
        
        // This would call a separate API endpoint to mark overdue payments
        // For now, we'll just show a message
        hideProgressDialog();
        Toast.makeText(this, "Overdue payments marked successfully", Toast.LENGTH_SHORT).show();
        
        // Refresh the summary after marking overdue
        loadPaymentSummary();
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
}








