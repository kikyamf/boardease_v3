package com.example.mock;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OverduePaymentsFragment extends Fragment {
    
    private RecyclerView recyclerView;
    private TextView emptyState;
    private TextView tvCount;
    private PaymentAdapter adapter;
    private List<PaymentData> overduePayments;
    private PaymentApiService paymentApiService;
    private ProgressDialog progressDialog;
    private SwipeRefreshLayout swipeRefreshLayout;
    private int ownerId;
    private boolean isInitialLoad = true;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_payments, container, false);
        
        // Set header title and styling (red for Overdue)
        TextView tvHeaderTitle = view.findViewById(R.id.tvHeaderTitle);
        LinearLayout headerLayout = view.findViewById(R.id.headerLayout);
        ImageView ivHeaderIcon = view.findViewById(R.id.ivHeaderIcon);
        
        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText("Overdue Payment Records");
        }
        if (headerLayout != null) {
            headerLayout.setBackgroundColor(android.graphics.Color.parseColor("#FFEBEE")); // Light red
        }
        if (ivHeaderIcon != null) {
            ivHeaderIcon.setImageResource(R.drawable.ic_info); // Info icon for overdue
            ivHeaderIcon.setColorFilter(android.graphics.Color.parseColor("#F44336")); // Red
        }
        
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyState = view.findViewById(R.id.emptyState);
        tvCount = view.findViewById(R.id.tvCount);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Set count badge background to red (matching header)
        if (tvCount != null) {
            tvCount.setBackgroundResource(R.drawable.bg_rounded_red);
        }
        
        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadOverduePayments(true);
        });
        
        // Get owner ID from SharedPreferences
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("UserSession", getContext().MODE_PRIVATE);
        
        // Handle String type for user_id (stored as String in Login.java)
        String userIdString = sharedPreferences.getString("user_id", "1");
        try {
            ownerId = Integer.parseInt(userIdString);
        } catch (NumberFormatException e) {
            ownerId = 1; // Default fallback
        }

        // Initialize API service
        paymentApiService = new PaymentApiService(getContext());
        overduePayments = new ArrayList<>();
        
        // Don't load automatically - wait for loadIfNeeded() to be called
        
        return view;
    }
    
    @Override
    public void onPause() {
        super.onPause();
        hideProgressDialog();
    }
    
    public void loadIfNeeded() {
        // Public method to trigger load from parent activity
        // Called when tab is clicked/selected for the first time
        if (isInitialLoad) {
            loadOverduePayments(false);
        }
    }
    
    private void loadOverduePayments(boolean isRefresh) {
        if (isRefresh) {
            // Show swipe refresh indicator
            swipeRefreshLayout.setRefreshing(true);
        } else if (isInitialLoad) {
            // Show ProgressDialog on initial load
            showProgressDialog("Loading overdue payments...");
            isInitialLoad = false;
        }
        
        paymentApiService.getOverduePayments(ownerId, new PaymentApiService.PaymentListCallback() {
            @Override
            public void onSuccess(List<PaymentData> payments) {
                hideProgressDialog();
                swipeRefreshLayout.setRefreshing(false);
                overduePayments.clear();
                overduePayments.addAll(payments);
                updateUI();
            }

            @Override
            public void onError(String error) {
                hideProgressDialog();
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), "Error loading overdue payments: " + error, Toast.LENGTH_SHORT).show();
                updateUI();
            }
        });
    }
    
    private void updateUI() {
        // Update count
        if (tvCount != null) {
            tvCount.setText(String.valueOf(overduePayments.size()));
        }
        
        if (overduePayments.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            emptyState.setText("No overdue payments found");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            
            // Create or update adapter with listener
            PaymentAdapter.PaymentActionListener listener = new PaymentAdapter.PaymentActionListener() {
                @Override
                public void onMarkAsPaid(PaymentData payment) {
                    // Handle mark as paid action
                }

                @Override
                public void onMarkAsOverdue(PaymentData payment) {
                    // Handle mark as overdue action (already overdue, so do nothing)
                }

                @Override
                public void onViewDetails(PaymentData payment) {
                    // Not used for overdue - use onSendReminder instead
                }

                @Override
                public void onSendReminder(PaymentData payment) {
                    // Show payment reminder modal
                    showPaymentReminderDialog(payment);
                }
            };
            
            if (adapter == null) {
                adapter = new PaymentAdapter(overduePayments, listener, PaymentAdapter.VIEW_TYPE_OVERDUE);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.setActionListener(listener);
                adapter.setViewType(PaymentAdapter.VIEW_TYPE_OVERDUE);
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void showProgressDialog(String message) {
        if (getContext() == null || getActivity() == null || getActivity().isFinishing() || getActivity().isDestroyed()) {
            return;
        }
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setCancelable(false);
        }
        if (!progressDialog.isShowing()) {
            progressDialog.setMessage(message);
            progressDialog.show();
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    // Method to refresh data (can be called from parent activity)
    public void refreshData() {
        if (isAdded()) {
            loadOverduePayments(true);
        } else {
            isInitialLoad = true;
        }
    }
    
    private void removePaymentLocally(int paymentId) {
        if (overduePayments == null) return;
        
        for (int i = 0; i < overduePayments.size(); i++) {
            if (overduePayments.get(i).getPaymentId() == paymentId) {
                overduePayments.remove(i);
                if (adapter != null) {
                    adapter.notifyItemRemoved(i);
                }
                updateUI(); // Update count
                break;
            }
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == android.app.Activity.RESULT_OK && data != null) {
            boolean paymentUpdated = data.getBooleanExtra("payment_updated", false);
            if (paymentUpdated) {
                // Optimistic UI: remove locally if needed
                int paymentId = data.getIntExtra("payment_id", -1);
                if (paymentId != -1) {
                    removePaymentLocally(paymentId);
                }
                
                // Refresh the payments list - if payment is paid, it will disappear from overdue tab
                refreshData();
                
                // Notify parent activity for tab navigation
                if (getActivity() instanceof ActivityDetailsActivity) {
                    String newPaymentStatus = data.getStringExtra("new_payment_status");
                    ActivityDetailsActivity.PaymentStatusUpdateCallback callback = 
                        ((ActivityDetailsActivity) getActivity()).getPaymentStatusCallback();
                    if (callback != null && newPaymentStatus != null) {
                        callback.onPaymentStatusUpdated(newPaymentStatus);
                    }
                }
            }
        }
    }

    private void showPaymentReminderDialog(PaymentData payment) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Send Payment Reminder");
        
        // Get due amount (only the amount for this specific overdue payment, not total remaining)
        String dueAmount = null;
        
        // First try: use totalAmount (this is the amount for this specific payment period)
        String totalAmount = payment.getTotalAmount();
        if (totalAmount != null && !totalAmount.isEmpty()) {
            // Remove currency symbol and format
            String cleanAmount = totalAmount.replace("₱", "").replace(",", "").trim();
            if (!cleanAmount.isEmpty() && !cleanAmount.equals("0") && !cleanAmount.equals("0.00")) {
                try {
                    double amount = Double.parseDouble(cleanAmount);
                    if (amount > 0) {
                        dueAmount = cleanAmount;
                    }
                } catch (NumberFormatException e) {
                    // Invalid number
                }
            }
        }
        
        // Fallback: if totalAmount is not available, use amountPaid (which might be the due amount for overdue)
        if (dueAmount == null || dueAmount.isEmpty() || dueAmount.equals("0") || dueAmount.equals("0.00")) {
            String amountPaid = payment.getAmountPaid();
            if (amountPaid != null && !amountPaid.isEmpty()) {
                String cleanAmount = amountPaid.replace("₱", "").replace(",", "").trim();
                if (!cleanAmount.isEmpty() && !cleanAmount.equals("0") && !cleanAmount.equals("0.00")) {
                    try {
                        double amount = Double.parseDouble(cleanAmount);
                        if (amount > 0) {
                            dueAmount = cleanAmount;
                        }
                    } catch (NumberFormatException e) {
                        // Invalid number
                    }
                }
            }
        }
        
        // Final fallback
        if (dueAmount == null || dueAmount.isEmpty() || dueAmount.equals("0") || dueAmount.equals("0.00")) {
            dueAmount = "0.00";
        }
        
        // Format due amount
        try {
            double amount = Double.parseDouble(dueAmount);
            dueAmount = String.format(Locale.getDefault(), "%.2f", amount);
        } catch (NumberFormatException e) {
            dueAmount = "0.00";
        }
        
        // Get due date and calculate days overdue
        String dueDateStr = payment.getDueDate();
        if (dueDateStr == null || dueDateStr.isEmpty()) {
            dueDateStr = payment.getPaymentDate();
        }
        
        String dueDateDisplay = "N/A";
        int daysOverdue = 0;
        
        if (dueDateStr != null && !dueDateStr.isEmpty()) {
            try {
                // Parse due date
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String datePart = dueDateStr.split(" ")[0]; // Take only date part
                java.util.Date dueDate = inputFormat.parse(datePart);
                
                // Calculate days overdue
                java.util.Date today = new java.util.Date();
                long diffInMillis = today.getTime() - dueDate.getTime();
                daysOverdue = (int) (diffInMillis / (1000 * 60 * 60 * 24));
                
                // Format due date for display
                java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String formattedDueDate = outputFormat.format(dueDate);
                
                if (daysOverdue > 0) {
                    dueDateDisplay = formattedDueDate + " (" + daysOverdue + " day" + (daysOverdue > 1 ? "s" : "") + " overdue)";
                } else {
                    dueDateDisplay = formattedDueDate;
                }
            } catch (Exception e) {
                // If parsing fails, just show the date as is
                dueDateDisplay = dueDateStr;
            }
        }
        
        String message = "Send payment reminder to " + payment.getBoarderName() + "?\n\n" +
                        "Due Amount: ₱" + dueAmount + "\n" +
                        "Due Date: " + dueDateDisplay;
        
        builder.setMessage(message);
        builder.setPositiveButton("Send Reminder", (dialog, which) -> {
            sendPaymentReminder(payment);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });
        
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void sendPaymentReminder(PaymentData payment) {
        showProgressDialog("Sending payment reminder...");
        
        // For overdue payments, use payment_id (or booking_id if payment_id is 0)
        int paymentId = payment.getPaymentId();
        if (paymentId == 0) {
            // If payment_id is 0, use booking_id as fallback
            paymentId = payment.getBookingId();
        }
        
        // Call API to send reminder
        paymentApiService.sendPaymentReminder(paymentId, payment.getUserId(), new PaymentApiService.PaymentReminderCallback() {
            @Override
            public void onSuccess(String message) {
                hideProgressDialog();
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                hideProgressDialog();
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

