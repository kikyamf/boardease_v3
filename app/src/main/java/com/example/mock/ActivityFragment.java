package com.example.mock;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ActivityFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private LinearLayout layoutBookings, layoutPaymentStatus, layoutBoardersRented, layoutMaintenanceRequests, layoutReviews;
    private int userId;

    public static ActivityFragment newInstance(int userId) {
        ActivityFragment fragment = new ActivityFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);

        // Get userId from arguments
        if (getArguments() != null) {
            userId = getArguments().getInt(ARG_USER_ID);
        }

        // Bind Views
        layoutBookings = view.findViewById(R.id.layoutBookings);
        layoutPaymentStatus = view.findViewById(R.id.layoutPaymentStatus);
        layoutBoardersRented = view.findViewById(R.id.layoutBoardersRented);
        layoutMaintenanceRequests = view.findViewById(R.id.layoutMaintenanceRequests);
        layoutReviews = view.findViewById(R.id.layoutReviews);

        // Click Events
        layoutBookings.setOnClickListener(v -> openBookingsActivity());
        layoutPaymentStatus.setOnClickListener(v -> openActivityDetails("payment_status"));
        layoutBoardersRented.setOnClickListener(v -> openActivityDetails("boarders_rented"));
        layoutMaintenanceRequests.setOnClickListener(v -> openMaintenanceRequestsActivity());
        layoutReviews.setOnClickListener(v -> openReviewsActivity());

        return view;
    }

    private void openBookingsActivity() {
        Intent intent = new Intent(getContext(), BookingsActivity.class);
        intent.putExtra("user_id", userId);
        startActivity(intent);
    }

    private void openActivityDetails(String activityType) {
        Intent intent = new Intent(getContext(), ActivityDetailsActivity.class);
        intent.putExtra("activity_type", activityType);
        startActivity(intent);
    }

    private void openMaintenanceRequestsActivity() {
        Intent intent = new Intent(getContext(), MaintenanceRequestsActivity.class);
        intent.putExtra("user_id", userId);
        startActivity(intent);
    }

    private void openReviewsActivity() {
        Intent intent = new Intent(getContext(), ReviewsActivity.class);
        intent.putExtra("user_id", userId);
        startActivity(intent);
    }
}







