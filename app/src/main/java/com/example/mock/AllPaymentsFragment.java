package com.example.mock;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class AllPaymentsFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView emptyState;
    private PaymentAdapter adapter;
    private List<PaymentData> allPayments;
    private PaymentApiService paymentApiService;
    private ProgressDialog progressDialog;
    private SwipeRefreshLayout swipeRefreshLayout;
    private int ownerId;
    private boolean isInitialLoad = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_payments, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        emptyState = view.findViewById(R.id.emptyState);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadAllPayments(true);
        });

        // Get owner ID from SharedPreferences
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("UserSession", getContext().MODE_PRIVATE);
        
        // Handle String type for user_id (stored as String in Login.java)
        String userIdString = sharedPreferences.getString("user_id", "1");
        try {
            ownerId = Integer.parseInt(userIdString);
            android.util.Log.d("AllPaymentsFragment", "Loaded ownerId from SharedPreferences: " + ownerId);
        } catch (NumberFormatException e) {
            ownerId = 1; // Default fallback
            android.util.Log.e("AllPaymentsFragment", "Failed to parse user_id: " + userIdString, e);
        }

        // Initialize API service
        paymentApiService = new PaymentApiService(getContext());
        allPayments = new ArrayList<>();

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
            loadAllPayments(false);
        }
    }

    private void loadAllPayments(boolean isRefresh) {
        if (isRefresh) {
            // Show swipe refresh indicator
            swipeRefreshLayout.setRefreshing(true);
        } else if (isInitialLoad) {
            // Show ProgressDialog on initial load
            showProgressDialog("Loading payments...");
            isInitialLoad = false;
        }
        
        android.util.Log.d("AllPaymentsFragment", "Loading payments for ownerId: " + ownerId);
        
        paymentApiService.getAllPayments(ownerId, new PaymentApiService.PaymentListCallback() {
            @Override
            public void onSuccess(List<PaymentData> payments) {
                hideProgressDialog();
                swipeRefreshLayout.setRefreshing(false);
                android.util.Log.d("AllPaymentsFragment", "Successfully loaded " + payments.size() + " payments");
                allPayments.clear();
                allPayments.addAll(payments);
                updateUI();
            }

            @Override
            public void onError(String error) {
                hideProgressDialog();
                swipeRefreshLayout.setRefreshing(false);
                android.util.Log.e("AllPaymentsFragment", "Error loading payments: " + error);
                Toast.makeText(getContext(), "Error loading payments: " + error, Toast.LENGTH_LONG).show();
                updateUI();
            }
        });
    }
    
    public void refreshPayments() {
        loadAllPayments(true);
    }

    private void updateUI() {
        if (allPayments.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            emptyState.setText("No payments found");
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
                    // Open payment details activity with all payments context
                    android.content.Intent intent = new android.content.Intent(getContext(), PaymentDetailsActivity.class);
                    intent.putExtra("payment", payment);
                    intent.putExtra("view_type", PaymentAdapter.VIEW_TYPE_ALL);
                    startActivity(intent);
                }
            };
            
            if (adapter == null) {
                adapter = new PaymentAdapter(allPayments, listener);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.setActionListener(listener);
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
        loadAllPayments(true);
    }
}



































