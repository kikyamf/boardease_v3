package com.example.mock;

import android.app.ProgressDialog;
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

import java.util.ArrayList;
import java.util.List;

public class PendingPaymentsFragment extends Fragment {
    
    private RecyclerView recyclerView;
    private TextView emptyState;
    private PaymentAdapter adapter;
    private List<PaymentData> pendingPayments;
    private PaymentApiService paymentApiService;
    private ProgressDialog progressDialog;
    private int ownerId = 1; // TODO: Get from shared preferences or intent
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_payments, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyState = view.findViewById(R.id.emptyState);
        
        // Initialize API service
        paymentApiService = new PaymentApiService(getContext());
        pendingPayments = new ArrayList<>();
        setupRecyclerView();
        loadPendingPayments();
        
        return view;
    }
    
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PaymentAdapter(pendingPayments);
        recyclerView.setAdapter(adapter);
    }
    
    private void loadPendingPayments() {
        showProgressDialog("Loading pending payments...");
        
        paymentApiService.getPendingPayments(ownerId, new PaymentApiService.PaymentListCallback() {
            @Override
            public void onSuccess(List<PaymentData> payments) {
                hideProgressDialog();
                pendingPayments.clear();
                pendingPayments.addAll(payments);
                updateUI();
            }

            @Override
            public void onError(String error) {
                hideProgressDialog();
                Toast.makeText(getContext(), "Error loading pending payments: " + error, Toast.LENGTH_SHORT).show();
                updateUI();
            }
        });
    }
    
    private void updateUI() {
        if (pendingPayments.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            emptyState.setText("No pending payments found");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            
            if (adapter == null) {
                adapter = new PaymentAdapter(pendingPayments, new PaymentAdapter.PaymentActionListener() {
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
                        // Open payment details activity
                        android.content.Intent intent = new android.content.Intent(getContext(), PaymentDetailsActivity.class);
                        intent.putExtra("payment", payment);
                        startActivity(intent);
                    }
                });
                recyclerView.setAdapter(adapter);
            } else {
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getContext());
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

    // Method to refresh data (can be called from parent activity)
    public void refreshData() {
        loadPendingPayments();
    }
}

















