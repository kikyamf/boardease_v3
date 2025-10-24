package com.example.mock;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PaymentsFragment extends Fragment {

    private RecyclerView recyclerView;
    private PaymentsAdapter adapter;
    private List<PaymentLog> paymentLogs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payments, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Initialize sample data
        initializeSampleData();
        
        adapter = new PaymentsAdapter(paymentLogs, this::onPaymentClick);
        recyclerView.setAdapter(adapter);
        
        return view;
    }

    private void initializeSampleData() {
        paymentLogs = new ArrayList<>();
        
        // Sample payment logs
        paymentLogs.add(new PaymentLog(
            "John Doe",
            "Batavia Apartments",
            "₱15,000",
            "2024-01-15 10:30",
            "Paid",
            "Monthly rent payment confirmed"
        ));
        
        paymentLogs.add(new PaymentLog(
            "Jane Smith",
            "Takatea Homestay",
            "₱12,500",
            "2024-01-14 14:20",
            "Pending",
            "Payment processing - waiting for confirmation"
        ));
        
        paymentLogs.add(new PaymentLog(
            "Mike Johnson",
            "Tropis Homestay",
            "₱18,000",
            "2024-01-13 09:15",
            "Failed",
            "Payment failed - insufficient funds"
        ));
        
        paymentLogs.add(new PaymentLog(
            "Sarah Wilson",
            "Batavia Apartments",
            "₱15,000",
            "2024-01-12 16:45",
            "Paid",
            "Security deposit payment confirmed"
        ));
    }

    private void onPaymentClick(PaymentLog payment) {
        Toast.makeText(getContext(), "Clicked: " + payment.getPropertyName(), Toast.LENGTH_SHORT).show();
        // Handle payment click - could open details dialog or activity
    }

    // Data class for payment logs
    public static class PaymentLog {
        private String tenantName;
        private String propertyName;
        private String amount;
        private String timestamp;
        private String status;
        private String description;

        public PaymentLog(String tenantName, String propertyName, String amount, String timestamp, String status, String description) {
            this.tenantName = tenantName;
            this.propertyName = propertyName;
            this.amount = amount;
            this.timestamp = timestamp;
            this.status = status;
            this.description = description;
        }

        // Getters
        public String getTenantName() { return tenantName; }
        public String getPropertyName() { return propertyName; }
        public String getAmount() { return amount; }
        public String getTimestamp() { return timestamp; }
        public String getStatus() { return status; }
        public String getDescription() { return description; }
    }
}


































