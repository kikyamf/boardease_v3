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

public class BookingHistoryFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private RecyclerView recyclerView;
    private BookingHistoryAdapter adapter;
    private List<BookingData> bookingHistory;
    private int userId;

    public static BookingHistoryFragment newInstance(int userId) {
        BookingHistoryFragment fragment = new BookingHistoryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_booking_history, container, false);

        // Get userId from arguments
        if (getArguments() != null) {
            userId = getArguments().getInt(ARG_USER_ID);
        }

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize with sample data
        bookingHistory = new ArrayList<>();
        loadBookingHistory();

        return view;
    }

    private void loadBookingHistory() {
        // Sample data - replace with actual API call
        bookingHistory.clear();
        bookingHistory.add(new BookingData(
            "Alex Brown",
            "alex.brown@email.com",
            "09123456785",
            "Room 1 - Single",
            "2024-10-01",
            "2024-12-31",
            "P3,000.00",
            "Long-term",
            "Completed"
        ));
        bookingHistory.add(new BookingData(
            "Emma Davis",
            "emma.davis@email.com",
            "09123456784",
            "Room 2 - Double",
            "2024-11-15",
            "2024-12-15",
            "P2,500.00",
            "Short-term",
            "Completed"
        ));
        bookingHistory.add(new BookingData(
            "Tom Wilson",
            "tom.wilson@email.com",
            "09123456783",
            "Room 3 - Single",
            "2024-09-01",
            "2024-10-01",
            "P3,000.00",
            "Short-term",
            "Expired"
        ));

        adapter = new BookingHistoryAdapter(bookingHistory, new BookingHistoryAdapter.OnBookingActionListener() {
            @Override
            public void onViewDetails(BookingData booking) {
                // Handle view details
                Toast.makeText(getContext(), "View details for " + booking.getBoarderName(), Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView.setAdapter(adapter);
    }
}
