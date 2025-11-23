package com.example.mock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ActivityFragment extends Fragment {

    private static final String TAG = "ActivityFragment";
    private static final String ARG_USER_ID = "user_id";
    private LinearLayout layoutBookings, layoutPaymentStatus, layoutBoardersRented, layoutMaintenanceRequests, layoutAnalytics, layoutReviews;
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
            userId = getArguments().getInt(ARG_USER_ID, 0);
        }
        
        // Fallback to SharedPreferences if userId is 0 or not set
        if (userId <= 0) {
            SharedPreferences sharedPreferences = getContext().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
            String userIdString = sharedPreferences.getString("user_id", null);
            if (userIdString != null) {
                try {
                    userId = Integer.parseInt(userIdString);
                    Log.d(TAG, "Got user_id from SharedPreferences: " + userId);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing user_id from SharedPreferences", e);
                    userId = 0;
                }
            }
        }
        
        Log.d(TAG, "Final user_id: " + userId);

        // Bind Views
        layoutBookings = view.findViewById(R.id.layoutBookings);
        layoutPaymentStatus = view.findViewById(R.id.layoutPaymentStatus);
        layoutBoardersRented = view.findViewById(R.id.layoutBoardersRented);
        layoutMaintenanceRequests = view.findViewById(R.id.layoutMaintenanceRequests);
        layoutAnalytics = view.findViewById(R.id.layoutAnalytics);
        layoutReviews = view.findViewById(R.id.layoutReviews);

        // Click Events
        layoutBookings.setOnClickListener(v -> openBookingsActivity());
        layoutPaymentStatus.setOnClickListener(v -> openActivityDetails("payment_status"));
        layoutBoardersRented.setOnClickListener(v -> openActivityDetails("boarders_rented"));
        layoutMaintenanceRequests.setOnClickListener(v -> openMaintenanceRequestsActivity());
        layoutAnalytics.setOnClickListener(v -> openAnalyticsActivity());
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
        intent.putExtra("user_id", userId);
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

    private void openAnalyticsActivity() {
        Intent intent = new Intent(getContext(), AnalyticsActivity.class);
        intent.putExtra("user_id", userId);
        startActivity(intent);
    }
}







