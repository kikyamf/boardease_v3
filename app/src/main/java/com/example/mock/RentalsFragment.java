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

public class RentalsFragment extends Fragment {

    private RecyclerView recyclerView;
    private RentalsAdapter adapter;
    private List<RentalLog> rentalLogs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rentals, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Initialize sample data
        initializeSampleData();
        
        adapter = new RentalsAdapter(rentalLogs, this::onRentalClick);
        recyclerView.setAdapter(adapter);
        
        return view;
    }

    private void initializeSampleData() {
        rentalLogs = new ArrayList<>();
        
        // Sample rental logs
        rentalLogs.add(new RentalLog(
            "John Doe",
            "Batavia Apartments",
            "2024-01-15 10:30",
            "New",
            "New rental agreement started"
        ));
        
        rentalLogs.add(new RentalLog(
            "Jane Smith",
            "Takatea Homestay",
            "2024-01-14 14:20",
            "Extended",
            "Rental period extended for 6 months"
        ));
        
        rentalLogs.add(new RentalLog(
            "Mike Johnson",
            "Tropis Homestay",
            "2024-01-13 09:15",
            "Terminated",
            "Rental agreement terminated by tenant"
        ));
        
        rentalLogs.add(new RentalLog(
            "Sarah Wilson",
            "Batavia Apartments",
            "2024-01-12 16:45",
            "Extended",
            "Rental period extended for 1 year"
        ));
    }

    private void onRentalClick(RentalLog rental) {
        Toast.makeText(getContext(), "Clicked: " + rental.getPropertyName(), Toast.LENGTH_SHORT).show();
        // Handle rental click - could open details dialog or activity
    }

    // Data class for rental logs
    public static class RentalLog {
        private String tenantName;
        private String propertyName;
        private String timestamp;
        private String status;
        private String description;

        public RentalLog(String tenantName, String propertyName, String timestamp, String status, String description) {
            this.tenantName = tenantName;
            this.propertyName = propertyName;
            this.timestamp = timestamp;
            this.status = status;
            this.description = description;
        }

        // Getters
        public String getTenantName() { return tenantName; }
        public String getPropertyName() { return propertyName; }
        public String getTimestamp() { return timestamp; }
        public String getStatus() { return status; }
        public String getDescription() { return description; }
    }
}






























