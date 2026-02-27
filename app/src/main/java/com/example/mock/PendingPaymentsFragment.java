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

public class PendingPaymentsFragment extends Fragment {
    
    private RecyclerView recyclerView;
    private TextView emptyState;
    private TextView tvCount;
    private PaymentAdapter adapter;
    private List<PaymentData> pendingPayments;
    private PaymentApiService paymentApiService;
    private ProgressDialog progressDialog;
    private SwipeRefreshLayout swipeRefreshLayout;
    private int ownerId;
    private boolean isInitialLoad = true;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_payments, container, false);
        
        // Set header title and styling (orange for Pending)
        TextView tvHeaderTitle = view.findViewById(R.id.tvHeaderTitle);
        LinearLayout headerLayout = view.findViewById(R.id.headerLayout);
        ImageView ivHeaderIcon = view.findViewById(R.id.ivHeaderIcon);
        
        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText("Pending Payment Records");
        }
        if (headerLayout != null) {
            headerLayout.setBackgroundColor(android.graphics.Color.parseColor("#FFF3E0")); // Light orange
        }
        if (ivHeaderIcon != null) {
            ivHeaderIcon.setImageResource(R.drawable.ic_clock); // Clock icon for pending
            ivHeaderIcon.setColorFilter(android.graphics.Color.parseColor("#FF9800")); // Orange
        }
        
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyState = view.findViewById(R.id.emptyState);
        tvCount = view.findViewById(R.id.tvCount);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Set count badge background to orange (matching header)
        if (tvCount != null) {
            tvCount.setBackgroundResource(R.drawable.bg_rounded_orange);
        }
        
        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadPendingPayments(true);
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
        pendingPayments = new ArrayList<>();
        
        // Don't load automatically - wait for loadIfNeeded() to be called
        
        return view;
    }
    
    private android.os.Handler pollingHandler = new android.os.Handler();
    private Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            loadPendingPayments(false);
            pollingHandler.postDelayed(this, 5000); // Poll every 5 seconds
        }
    };

    private void startPolling() {
        pollingHandler.removeCallbacks(pollingRunnable);
        pollingHandler.postDelayed(pollingRunnable, 5000);
    }

    private void stopPolling() {
        pollingHandler.removeCallbacks(pollingRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        startPolling();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPolling();
        hideProgressDialog();
    }
    
    public void loadIfNeeded() {
        // Public method to trigger load from parent activity
        // Called when tab is clicked/selected for the first time
        if (isInitialLoad) {
            loadPendingPayments(false);
        }
    }
    
    private void loadPendingPayments(boolean isRefresh) {
        if (isRefresh) {
            // Show swipe refresh indicator
            swipeRefreshLayout.setRefreshing(true);
        } else if (isInitialLoad) {
            // Show ProgressDialog on initial load
            showProgressDialog("Loading pending payments...");
            isInitialLoad = false;
        } else {
             // Polling update - silent
        }
        
        paymentApiService.getPendingPayments(ownerId, new PaymentApiService.PaymentListCallback() {
            @Override
            public void onSuccess(List<PaymentData> payments) {
                if (getContext() == null) return;
                hideProgressDialog();
                swipeRefreshLayout.setRefreshing(false);
                pendingPayments.clear();
                pendingPayments.addAll(payments);
                updateUI();
            }

            @Override
            public void onError(String error) {
                if (getContext() == null) return;
                hideProgressDialog();
                swipeRefreshLayout.setRefreshing(false);
                // Only show toast on manual refresh or initial load
                if (isRefresh || isInitialLoad) {
                    Toast.makeText(getContext(), "Error loading pending payments: " + error, Toast.LENGTH_SHORT).show();
                }
                updateUI();
            }
        });
    }
    
    private void updateUI() {
        // Update count
        if (tvCount != null) {
            tvCount.setText(String.valueOf(pendingPayments.size()));
        }
        
        if (pendingPayments.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            emptyState.setText("No pending payments found");
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
                    // Open payment details activity with remaining context (pending usually has remaining balance)
                    android.content.Intent intent = new android.content.Intent(getContext(), PaymentDetailsActivity.class);
                    intent.putExtra("payment", payment);
                    intent.putExtra("view_type", PaymentAdapter.VIEW_TYPE_PENDING);
                    startActivityForResult(intent, 1001);
                }

                @Override
                public void onSendReminder(PaymentData payment) {
                    // Not used for pending payments
                }
            };
            
            if (adapter == null) {
                adapter = new PaymentAdapter(pendingPayments, listener, PaymentAdapter.VIEW_TYPE_PENDING);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.setActionListener(listener);
                adapter.setViewType(PaymentAdapter.VIEW_TYPE_PENDING);
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
            loadPendingPayments(true);
        } else {
            // If not added, mark as needing initial load so it refreshes when shown
            isInitialLoad = true;
        }
    }
    
    private void removePaymentLocally(int paymentId) {
        if (pendingPayments == null) return;
        
        for (int i = 0; i < pendingPayments.size(); i++) {
            if (pendingPayments.get(i).getPaymentId() == paymentId) {
                pendingPayments.remove(i);
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
                // Optimistic UI: remove the payment locally if we have the ID
                int paymentId = data.getIntExtra("payment_id", -1);
                if (paymentId != -1) {
                    removePaymentLocally(paymentId);
                }
                
                // Refresh the payments list to stay in sync with server
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

















