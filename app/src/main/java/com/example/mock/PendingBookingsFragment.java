package com.example.mock;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PendingBookingsFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private RecyclerView recyclerView;
    private PendingBookingsAdapter adapter;
    private List<BookingData> pendingBookings;
    private RequestQueue requestQueue;
    private int userId;
    private TextView tvCount;
    private TextView emptyState;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private ProgressDialog progressDialog;
    private boolean isInitialLoad = true;

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
        
        // Initialize count TextView
        tvCount = view.findViewById(R.id.tvCount);
        
        // Initialize empty state
        emptyState = view.findViewById(R.id.emptyState);
        
        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadPendingBookings(true);
        });
        
        // Initialize progress bar
        progressBar = view.findViewById(R.id.progressBar);

        // Initialize request queue
        requestQueue = Volley.newRequestQueue(getContext());

        // Initialize with empty list
        pendingBookings = new ArrayList<>();
        
        // Set initial count to 0
        if (tvCount != null) {
            tvCount.setText("0");
        }

        return view;
    }
    
    @Override
    public void onPause() {
        super.onPause();
        hideProgressDialog();
    }
    
    public void loadIfNeeded() {
        // Public method to trigger load from parent activity
        // Called when tab is clicked/selected for the first time
        if (isInitialLoad) {
            loadPendingBookings(false);
        }
    }

    private void loadPendingBookings(boolean isRefresh) {
        if (isRefresh) {
            // Show swipe refresh indicator
            swipeRefreshLayout.setRefreshing(true);
        } else if (isInitialLoad) {
            // Show ProgressDialog on initial load - PENDING BOOKINGS
            showProgressDialog("Loading pending bookings...");
            isInitialLoad = false;
        } else {
            // Not initial load and not refresh - should not happen, but just in case
            // Don't show any loading indicator
        }
        
        String url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_pending_bookings.php?user_id=" + userId + "&user_type=owner";
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            JSONArray bookingsArray = data.getJSONArray("pending_bookings");
                            
                            pendingBookings.clear();
                            
                            for (int i = 0; i < bookingsArray.length(); i++) {
                                JSONObject bookingObj = bookingsArray.getJSONObject(i);
                                
                                BookingData booking = new BookingData(
                                    bookingObj.optInt("booking_id", 0),
                                    bookingObj.optString("boarder_name", ""),
                                    bookingObj.optString("boarder_email", ""),
                                    bookingObj.optString("boarder_phone", ""),
                                    bookingObj.optString("room_name", ""),
                                    bookingObj.optString("start_date", ""),
                                    bookingObj.optString("end_date", ""),
                                    bookingObj.optString("amount", ""),
                                    bookingObj.optString("rent_type", ""),
                                    bookingObj.optString("status", ""),
                                    bookingObj.optString("boarding_house_name", ""),
                                    bookingObj.optString("boarding_house_address", ""),
                                    bookingObj.optString("booking_date", ""),
                                    bookingObj.optString("payment_status", "Pending"),
                                    bookingObj.optString("notes", ""),
                                    bookingObj.optString("profile_image", ""),
                                    bookingObj.optInt("boarder_id", 0),
                                    bookingObj.optInt("room_id", 0),
                                    bookingObj.optInt("boarding_house_id", 0)
                                );
                                
                                pendingBookings.add(booking);
                            }
                            
                            adapter = new PendingBookingsAdapter(pendingBookings, new PendingBookingsAdapter.OnBookingActionListener() {
                                @Override
                                public void onApprove(BookingData booking) {
                                    // Handle approve booking
                                    approveBooking(booking);
                                }

                                @Override
                                public void onDecline(BookingData booking) {
                                    // Handle decline booking
                                    declineBooking(booking);
                                }

                                @Override
                                public void onViewDetails(BookingData booking) {
                                    // Navigate to booking details
                                    Intent intent = new Intent(getContext(), BookingDetailsActivity.class);
                                    // Pass booking data through intent
                                    intent.putExtra("booking_id", booking.getBookingId());
                                    intent.putExtra("boarder_name", booking.getBoarderName());
                                    intent.putExtra("boarder_email", booking.getEmail());
                                    intent.putExtra("boarder_phone", booking.getPhoneNumber());
                                    intent.putExtra("room_name", booking.getRoomName());
                                    intent.putExtra("start_date", booking.getStartDate());
                                    intent.putExtra("end_date", booking.getEndDate());
                                    intent.putExtra("amount", booking.getAmount());
                                    intent.putExtra("rent_type", booking.getRentType());
                                    intent.putExtra("status", booking.getStatus());
                                    intent.putExtra("boarding_house_name", booking.getBoardingHouseName());
                                    intent.putExtra("boarding_house_address", booking.getBoardingHouseAddress());
                                    intent.putExtra("booking_date", booking.getBookingDate());
                                    intent.putExtra("payment_status", booking.getPaymentStatus());
                                    intent.putExtra("notes", booking.getNotes());
                                    intent.putExtra("profile_image", booking.getProfileImage());
                                    intent.putExtra("boarder_id", booking.getBoarderId());
                                    intent.putExtra("room_id", booking.getRoomId());
                                    intent.putExtra("boarding_house_id", booking.getBoardingHouseId());
                                    intent.putExtra("owner_id", userId);
                                    startActivity(intent);
                                }
                            });
                            
                            recyclerView.setAdapter(adapter);
                            
                            // Update count and UI
                            updateCount(pendingBookings.size());
                            updateUI();
                        } else {
                            Toast.makeText(getContext(), "Error loading bookings: " + response.getString("error"), Toast.LENGTH_SHORT).show();
                            updateCount(0);
                            updateUI();
                        }
                        hideProgressDialog();
                        hideLoadingIndicator();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error parsing booking data", Toast.LENGTH_SHORT).show();
                        updateCount(0);
                        updateUI();
                        hideProgressDialog();
                        hideLoadingIndicator();
                    }
                },
                error -> {
                    Toast.makeText(getContext(), "Error loading bookings: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    updateCount(0);
                    updateUI();
                    hideProgressDialog();
                    hideLoadingIndicator();
                });

        requestQueue.add(request);
    }
    
    private void approveBooking(BookingData booking) {
        showProgressDialog("Approving booking...");
        
        String url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/approve_booking.php";
        
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("booking_id", booking.getBookingId());
            requestBody.put("owner_id", userId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            hideProgressDialog();
                            Toast.makeText(getContext(), "Booking approved successfully!", Toast.LENGTH_SHORT).show();
                            // Reload the list to reflect changes (this will update UI including empty state)
                            loadPendingBookings(false);
                        } else {
                            hideProgressDialog();
                            Toast.makeText(getContext(), "Error: " + response.getString("error"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        hideProgressDialog();
                        Toast.makeText(getContext(), "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    hideProgressDialog();
                    Toast.makeText(getContext(), "Error approving booking: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(request);
    }
    
    private void declineBooking(BookingData booking) {
        showProgressDialog("Declining booking...");
        
        String url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/decline_booking.php";
        
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("booking_id", booking.getBookingId());
            requestBody.put("owner_id", userId);
            requestBody.put("reason", "Declined by owner");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            hideProgressDialog();
                            Toast.makeText(getContext(), "Booking declined.", Toast.LENGTH_SHORT).show();
                            // Reload the list to reflect changes (this will update UI including empty state)
                            loadPendingBookings(false);
                        } else {
                            hideProgressDialog();
                            Toast.makeText(getContext(), "Error: " + response.getString("error"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        hideProgressDialog();
                        Toast.makeText(getContext(), "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    hideProgressDialog();
                    Toast.makeText(getContext(), "Error declining booking: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(request);
    }
    
    private void showProgressDialog(String message) {
        try {
            if (getContext() == null || getActivity() == null) {
                return; // Context not available yet
            }
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage(message);
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            if (!getActivity().isFinishing() && !getActivity().isDestroyed()) {
                progressDialog.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideProgressDialog() {
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            progressDialog = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void showProgressBar() {
        if (progressBar != null) {
            // Show immediately first
            progressBar.setIndeterminate(true);
            progressBar.setVisibility(View.VISIBLE);
            
            // Then animate if view is ready
            if (getView() != null) {
                getView().post(() -> {
                    if (progressBar != null && progressBar.getVisibility() == View.VISIBLE) {
                        // Slide in animation from bottom
                        TranslateAnimation slideIn = new TranslateAnimation(
                            TranslateAnimation.RELATIVE_TO_SELF, 0.0f,
                            TranslateAnimation.RELATIVE_TO_SELF, 0.0f,
                            TranslateAnimation.RELATIVE_TO_SELF, 1.0f,
                            TranslateAnimation.RELATIVE_TO_SELF, 0.0f
                        );
                        slideIn.setDuration(300);
                        progressBar.startAnimation(slideIn);
                    }
                });
            }
        }
    }

    private void hideLoadingIndicator() {
        // Hide swipe refresh if active
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
        
        // Hide progress bar
        if (progressBar != null && progressBar.getVisibility() == View.VISIBLE) {
            // Slide out animation to bottom
            TranslateAnimation slideOut = new TranslateAnimation(
                TranslateAnimation.RELATIVE_TO_SELF, 0.0f,
                TranslateAnimation.RELATIVE_TO_SELF, 0.0f,
                TranslateAnimation.RELATIVE_TO_SELF, 0.0f,
                TranslateAnimation.RELATIVE_TO_SELF, 1.0f
            );
            slideOut.setDuration(300);
            slideOut.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
                @Override
                public void onAnimationStart(android.view.animation.Animation animation) {}

                @Override
                public void onAnimationEnd(android.view.animation.Animation animation) {
                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(android.view.animation.Animation animation) {}
            });
            progressBar.startAnimation(slideOut);
        }
    }

    public void refreshBookings() {
        if (getContext() != null) {
            loadPendingBookings(true);
        }
    }
    
    private void updateCount(int count) {
        if (tvCount != null) {
            tvCount.setText(String.valueOf(count));
            tvCount.setVisibility(View.VISIBLE); // Always visible, even if 0
        }
    }
    
    private void updateUI() {
        if (pendingBookings == null || pendingBookings.isEmpty()) {
            // Show empty state
            if (recyclerView != null) {
                recyclerView.setVisibility(View.GONE);
            }
            if (emptyState != null) {
                emptyState.setVisibility(View.VISIBLE);
                emptyState.setText("No pending bookings yet");
            }
        } else {
            // Show recycler view
            if (recyclerView != null) {
                recyclerView.setVisibility(View.VISIBLE);
            }
            if (emptyState != null) {
                emptyState.setVisibility(View.GONE);
            }
        }
    }
}

















