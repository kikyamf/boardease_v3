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

public class ReservationsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ReservationsAdapter adapter;
    private List<ReservationLog> reservationLogs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reservations, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Initialize sample data
        initializeSampleData();
        
        adapter = new ReservationsAdapter(reservationLogs, this::onReservationClick);
        recyclerView.setAdapter(adapter);
        
        return view;
    }

    private void initializeSampleData() {
        reservationLogs = new ArrayList<>();
        
        // Sample reservation logs
        reservationLogs.add(new ReservationLog(
            "John Doe",
            "Batavia Apartments",
            "2024-01-15 10:30",
            "Pending",
            "New reservation application submitted"
        ));
        
        reservationLogs.add(new ReservationLog(
            "Jane Smith",
            "Takatea Homestay",
            "2024-01-14 14:20",
            "Approved",
            "Reservation approved by landlord"
        ));
        
        reservationLogs.add(new ReservationLog(
            "Mike Johnson",
            "Tropis Homestay",
            "2024-01-13 09:15",
            "Declined",
            "Reservation declined - no available rooms"
        ));
        
        reservationLogs.add(new ReservationLog(
            "Sarah Wilson",
            "Batavia Apartments",
            "2024-01-12 16:45",
            "Approved",
            "Reservation approved and payment confirmed"
        ));
    }

    private void onReservationClick(ReservationLog reservation) {
        Toast.makeText(getContext(), "Clicked: " + reservation.getPropertyName(), Toast.LENGTH_SHORT).show();
        // Handle reservation click - could open details dialog or activity
    }

    // Data class for reservation logs
    public static class ReservationLog {
        private String tenantName;
        private String propertyName;
        private String timestamp;
        private String status;
        private String description;

        public ReservationLog(String tenantName, String propertyName, String timestamp, String status, String description) {
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


































