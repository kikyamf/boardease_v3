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

public class PendingBookingsFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private RecyclerView recyclerView;
    private PendingBookingsAdapter adapter;
    private List<BookingData> pendingBookings;
    private int userId;

    public static PendingBookingsFragment newInstance(int userId) {
        PendingBookingsFragment fragment = new PendingBookingsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pending_bookings, container, false);

        // Get userId from arguments
        if (getArguments() != null) {
            userId = getArguments().getInt(ARG_USER_ID);
        }

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize with sample data
        pendingBookings = new ArrayList<>();
        loadPendingBookings();

        return view;
    }

    private void loadPendingBookings() {
        // Sample data - replace with actual API call
        pendingBookings.clear();
        pendingBookings.add(new BookingData(
            "John Doe",
            "john.doe@email.com",
            "09123456789",
            "Room 1 - Single",
            "2025-01-15",
            "2025-04-15",
            "P3,000.00",
            "Long-term",
            "Pending"
        ));
        pendingBookings.add(new BookingData(
            "Jane Smith",
            "jane.smith@email.com",
            "09123456788",
            "Room 2 - Double",
            "2025-01-20",
            "2025-02-20",
            "P2,500.00",
            "Short-term",
            "Pending"
        ));

        adapter = new PendingBookingsAdapter(pendingBookings, new PendingBookingsAdapter.OnBookingActionListener() {
            @Override
            public void onApprove(BookingData booking) {
                // Handle approve booking
                Toast.makeText(getContext(), "Approved booking for " + booking.getBoarderName(), Toast.LENGTH_SHORT).show();
                // Remove from pending list
                pendingBookings.remove(booking);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onDecline(BookingData booking) {
                // Handle decline booking
                Toast.makeText(getContext(), "Declined booking for " + booking.getBoarderName(), Toast.LENGTH_SHORT).show();
                // Remove from pending list
                pendingBookings.remove(booking);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onViewDetails(BookingData booking) {
                // Handle view details
                Toast.makeText(getContext(), "View details for " + booking.getBoarderName(), Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView.setAdapter(adapter);
    }
}
