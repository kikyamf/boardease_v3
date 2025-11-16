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

public class RemainingPaymentsFragment extends Fragment {
    
    private RecyclerView recyclerView;
    private TextView emptyState;
    private TextView tvCount;
    private PaymentAdapter adapter;
    private List<PaymentData> remainingPayments;
    private PaymentApiService paymentApiService;
    private ProgressDialog progressDialog;
    private SwipeRefreshLayout swipeRefreshLayout;
    private int ownerId;
    private boolean isInitialLoad = true;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_payments, container, false);
        
        // Set header title and styling (blue for Remaining / Partially Paid)
        TextView tvHeaderTitle = view.findViewById(R.id.tvHeaderTitle);
        LinearLayout headerLayout = view.findViewById(R.id.headerLayout);
        ImageView ivHeaderIcon = view.findViewById(R.id.ivHeaderIcon);
        
        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText("Remaining / Partially Paid Payment Records");
        }
        if (headerLayout != null) {
            headerLayout.setBackgroundColor(android.graphics.Color.parseColor("#E3F2FD")); // Light blue
        }
        if (ivHeaderIcon != null) {
            ivHeaderIcon.setImageResource(R.drawable.ic_payment2); // Payment icon for remaining/partially paid
            ivHeaderIcon.setColorFilter(android.graphics.Color.parseColor("#2196F3")); // Blue
        }
        
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyState = view.findViewById(R.id.emptyState);
        tvCount = view.findViewById(R.id.tvCount);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Set count badge background to blue (matching header)
        if (tvCount != null) {
            tvCount.setBackgroundResource(R.drawable.bg_rounded_blue);
        }
        
        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadRemainingPayments(true);
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
        remainingPayments = new ArrayList<>();
        
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
            loadRemainingPayments(false);
        }
    }
    
    private void loadRemainingPayments(boolean isRefresh) {
        if (isRefresh) {
            // Show swipe refresh indicator
            swipeRefreshLayout.setRefreshing(true);
        } else if (isInitialLoad) {
            // Show ProgressDialog on initial load
            showProgressDialog("Loading bookings with remaining balance...");
            isInitialLoad = false;
        }
        
        paymentApiService.getRemainingPayments(ownerId, new PaymentApiService.PaymentListCallback() {
            @Override
            public void onSuccess(List<PaymentData> payments) {
                hideProgressDialog();
                swipeRefreshLayout.setRefreshing(false);
                remainingPayments.clear();
                remainingPayments.addAll(payments);
                updateUI();
            }

            @Override
            public void onError(String error) {
                hideProgressDialog();
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), "Error loading remaining payments: " + error, Toast.LENGTH_SHORT).show();
                updateUI();
            }
        });
    }
    
    private void updateUI() {
        // Update count
        if (tvCount != null) {
            tvCount.setText(String.valueOf(remainingPayments.size()));
        }
        
        if (remainingPayments.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            emptyState.setText("No bookings with remaining balance");
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
                    // Handle mark as overdue action
                }

                @Override
                public void onViewDetails(PaymentData payment) {
                    // Open payment details activity with remaining context
                    android.content.Intent intent = new android.content.Intent(getContext(), PaymentDetailsActivity.class);
                    intent.putExtra("payment", payment);
                    intent.putExtra("view_type", PaymentAdapter.VIEW_TYPE_REMAINING);
                    startActivityForResult(intent, 1001);
                }
            };
            
            if (adapter == null) {
                adapter = new PaymentAdapter(remainingPayments, listener, PaymentAdapter.VIEW_TYPE_REMAINING);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.setActionListener(listener);
                adapter.setViewType(PaymentAdapter.VIEW_TYPE_REMAINING);
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
        loadRemainingPayments(true);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == android.app.Activity.RESULT_OK && data != null) {
            boolean paymentUpdated = data.getBooleanExtra("payment_updated", false);
            if (paymentUpdated) {
                // Refresh the payments list
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
}

