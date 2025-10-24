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

public class MaintenanceFragment extends Fragment {

    private RecyclerView recyclerView;
    private MaintenanceAdapter adapter;
    private List<MaintenanceLog> maintenanceLogs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maintenance, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Initialize sample data
        initializeSampleData();
        
        adapter = new MaintenanceAdapter(maintenanceLogs, this::onMaintenanceClick);
        recyclerView.setAdapter(adapter);
        
        return view;
    }

    private void initializeSampleData() {
        maintenanceLogs = new ArrayList<>();
        
        // Sample maintenance logs
        maintenanceLogs.add(new MaintenanceLog(
            "Batavia Apartments",
            "Plumbing Issue",
            "2024-01-15 10:30",
            "New",
            "Leaky faucet in unit 3A reported by tenant"
        ));
        
        maintenanceLogs.add(new MaintenanceLog(
            "Takatea Homestay",
            "Electrical Repair",
            "2024-01-14 14:20",
            "In Progress",
            "Power outlet repair in progress"
        ));
        
        maintenanceLogs.add(new MaintenanceLog(
            "Tropis Homestay",
            "AC Maintenance",
            "2024-01-13 09:15",
            "Completed",
            "AC unit cleaning and maintenance completed"
        ));
        
        maintenanceLogs.add(new MaintenanceLog(
            "Batavia Apartments",
            "Door Lock",
            "2024-01-12 16:45",
            "New",
            "Broken door lock needs replacement"
        ));
    }

    private void onMaintenanceClick(MaintenanceLog maintenance) {
        Toast.makeText(getContext(), "Clicked: " + maintenance.getPropertyName(), Toast.LENGTH_SHORT).show();
        // Handle maintenance click - could open details dialog or activity
    }

    // Data class for maintenance logs
    public static class MaintenanceLog {
        private String propertyName;
        private String issueType;
        private String timestamp;
        private String status;
        private String description;

        public MaintenanceLog(String propertyName, String issueType, String timestamp, String status, String description) {
            this.propertyName = propertyName;
            this.issueType = issueType;
            this.timestamp = timestamp;
            this.status = status;
            this.description = description;
        }

        // Getters
        public String getPropertyName() { return propertyName; }
        public String getIssueType() { return issueType; }
        public String getTimestamp() { return timestamp; }
        public String getStatus() { return status; }
        public String getDescription() { return description; }
    }
}


































