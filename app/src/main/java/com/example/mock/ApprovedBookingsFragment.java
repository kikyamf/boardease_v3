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

public class ApprovedBookingsFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private RecyclerView recyclerView;
    private ApprovedBookingsAdapter adapter;
    private List<BookingData> approvedBookings;
    private int userId;

    public static ApprovedBookingsFragment newInstance(int userId) {
        ApprovedBookingsFragment fragment = new ApprovedBookingsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_approved_bookings, container, false);

        // Get userId from arguments
        if (getArguments() != null) {
            userId = getArguments().getInt(ARG_USER_ID);
        }

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize with sample data
        approvedBookings = new ArrayList<>();
        loadApprovedBookings();

        return view;
    }

    private void loadApprovedBookings() {
        // Sample data - replace with actual API call
        approvedBookings.clear();
        approvedBookings.add(new BookingData(
            "Mike Johnson",
            "mike.johnson@email.com",
            "09123456787",
            "Room 3 - Single",
            "2025-01-10",
            "2025-04-10",
            "P3,000.00",
            "Long-term",
            "Approved"
        ));
        approvedBookings.add(new BookingData(
            "Sarah Wilson",
            "sarah.wilson@email.com",
            "09123456786",
            "Room 4 - Double",
            "2025-01-12",
            "2025-02-12",
            "P2,500.00",
            "Short-term",
            "Approved"
        ));

        adapter = new ApprovedBookingsAdapter(approvedBookings, new ApprovedBookingsAdapter.OnBookingActionListener() {
            @Override
            public void onViewDetails(BookingData booking) {
                // Handle view details
                Toast.makeText(getContext(), "View details for " + booking.getBoarderName(), Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView.setAdapter(adapter);
    }
}
