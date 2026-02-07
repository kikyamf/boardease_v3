package com.example.mock;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
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
    
    private android.os.Handler pollingHandler = new android.os.Handler();
    private Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            loadPendingBookings(false);
            pollingHandler.postDelayed(this, 5000); // Poll every 5 seconds
        }
    };

    private void startPolling() {
        pollingHandler.removeCallbacks(pollingRunnable);
        pollingHandler.postDelayed(pollingRunnable, 5000);
    }

    private void stopPolling() {
        pollingHandler.removeCallbacks(pollingRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        startPolling();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPolling();
        hideProgressDialog();
    }


    public void loadIfNeeded() {
        if (userId > 0) {
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
            // Polling update - silent
        }
        
        String url = "https://boardease.calapebohol.com/get_pending_bookings.php?user_id=" + userId + "&user_type=owner";
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (getContext() == null) return;
                        
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            JSONArray bookingsArray = data.getJSONArray("pending_bookings");
                            
                            // Only update if data changed or first load, but for simplicity in polling we just update
                            // Use a new list to avoid clearing the old one immediately if we want to diff, 
                            // but standard replacement is fine here.
                            
                            List<BookingData> newBookings = new ArrayList<>();
                            
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
                                
                                // Set payment progress fields
                                booking.setTotalPeriods(bookingObj.optInt("total_periods", 0));
                                booking.setPaidPeriods(bookingObj.optInt("paid_periods", 0));
                                booking.setUnpaidPeriods(bookingObj.optInt("unpaid_periods", 0));
                                booking.setTotalMonthsForBooking(bookingObj.optInt("total_months_for_booking", 0));
                                booking.setPaidMonthsForBooking(bookingObj.optInt("paid_months_for_booking", 0));
                                if (!bookingObj.isNull("remaining_months_to_pay")) {
                                    booking.setRemainingMonthsToPay(bookingObj.optInt("remaining_months_to_pay", 0));
                                }
                                // Parse total_amount_for_booking - use null if not present or empty, not "0.00"
                                String totalAmountForBooking = bookingObj.optString("total_amount_for_booking", null);
                                if (totalAmountForBooking != null && !totalAmountForBooking.isEmpty() && !totalAmountForBooking.equals("null")) {
                                    booking.setTotalAmountForBooking(totalAmountForBooking);
                                } else {
                                    booking.setTotalAmountForBooking(null);
                                }
                                booking.setPaidAmountForBooking(bookingObj.optString("paid_amount_for_booking", "0.00"));
                                booking.setRemainingAmountToPay(bookingObj.optString("remaining_amount_to_pay", "0.00"));
                                booking.setFullyPaid(bookingObj.optBoolean("is_fully_paid", false));
                                booking.setPaymentProgressPercent(bookingObj.optDouble("payment_progress_percent", 0.0));
                                
                                newBookings.add(booking);
                            }
                            
                            pendingBookings.clear();
                            pendingBookings.addAll(newBookings);
                            
                            if (adapter == null) {
                                adapter = new PendingBookingsAdapter(pendingBookings, new PendingBookingsAdapter.OnBookingActionListener() {
                                    @Override
                                    public void onApprove(BookingData booking) {
                                        approveBooking(booking);
                                    }

                                    @Override
                                    public void onDecline(BookingData booking) {
                                        declineBooking(booking);
                                    }

                                    @Override
                                    public void onViewDetails(BookingData booking) {
                                        Intent intent = new Intent(getContext(), BookingDetailsActivity.class);
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
                                        intent.putExtra("total_periods", booking.getTotalPeriods());
                                        intent.putExtra("paid_periods", booking.getPaidPeriods());
                                        intent.putExtra("total_amount_for_booking", booking.getTotalAmountForBooking());
                                        intent.putExtra("paid_amount_for_booking", booking.getPaidAmountForBooking());
                                        intent.putExtra("is_fully_paid", booking.isFullyPaid());
                                        
                                        if (getActivity() != null) {
                                            getActivity().startActivityForResult(intent, 1001);
                                        } else {
                                            startActivity(intent);
                                        }
                                    }
                                });
                                recyclerView.setAdapter(adapter);
                            } else {
                                adapter.notifyDataSetChanged();
                            }
                            
                            // Update count and UI
                            updateCount(pendingBookings.size());
                            updateUI();
                        } else {
                            // Only show toast if manual refresh or initial load, to avoid spamming toast
                            if (isRefresh || isInitialLoad) {
                                Toast.makeText(getContext(), "Error loading bookings: " + response.getString("error"), Toast.LENGTH_SHORT).show();
                            }
                            updateCount(0);
                            updateUI();
                        }
                        hideProgressDialog();
                        hideLoadingIndicator();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        if (isRefresh || isInitialLoad) {
                            Toast.makeText(getContext(), "Error parsing booking data", Toast.LENGTH_SHORT).show();
                        }
                        updateCount(0);
                        updateUI();
                        hideProgressDialog();
                        hideLoadingIndicator();
                    }
                },
                error -> {
                    if (isRefresh || isInitialLoad) {
                        Toast.makeText(getContext(), "Error loading bookings: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    updateCount(0);
                    updateUI();
                    hideProgressDialog();
                    hideLoadingIndicator();
                });

        requestQueue.add(request);
    }
    
    private void approveBooking(BookingData booking) {
        // Show confirmation dialog with payment information
        showApproveConfirmationDialog(booking);
    }
    
    private void showApproveConfirmationDialog(BookingData booking) {
        // Use light theme for dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext(), android.R.style.Theme_Material_Light_Dialog);
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_approve_booking_confirmation, null);
        builder.setView(dialogView);
        
        // Get views
        TextView tvBoarderName = dialogView.findViewById(R.id.tvBoarderName);
        TextView tvRoomName = dialogView.findViewById(R.id.tvRoomName);
        TextView tvPaymentStatus = dialogView.findViewById(R.id.tvPaymentStatus);
        TextView tvAmountPaid = dialogView.findViewById(R.id.tvAmountPaid);
        TextView tvTotalAmount = dialogView.findViewById(R.id.tvTotalAmount);
        TextView tvPaymentProgress = dialogView.findViewById(R.id.tvPaymentProgress);
        TextView tvProgressPercent = dialogView.findViewById(R.id.tvProgressPercent);
        ProgressBar progressBarPayment = dialogView.findViewById(R.id.progressBarPayment);
        TextView tvWarning = dialogView.findViewById(R.id.tvWarning);
        android.widget.ImageView imgPaymentProof = dialogView.findViewById(R.id.imgPaymentProof);
        TextView tvNoProof = dialogView.findViewById(R.id.tvNoProof);
        LinearLayout layoutPaymentProof = dialogView.findViewById(R.id.layoutPaymentProof);
        android.widget.Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        
        // Set booking information
        tvBoarderName.setText(booking.getBoarderName() != null ? booking.getBoarderName() : "Unknown");
        tvRoomName.setText(booking.getRoomName() != null ? booking.getRoomName() : "");
        
        // Set payment information
        int totalPeriods = booking.getTotalPeriods();
        int paidPeriods = booking.getPaidPeriods();
        String paidAmount = booking.getPaidAmountForBooking();
        String totalAmount = booking.getTotalAmountForBooking();
        boolean isFullyPaid = booking.isFullyPaid();
        double progressPercent = booking.getPaymentProgressPercent();
        
        // Determine payment status
        String paymentStatusText;
        int statusColor;
        int statusBg;
        
        if (isFullyPaid || (totalPeriods > 0 && paidPeriods >= totalPeriods)) {
            paymentStatusText = "Fully Paid";
            statusColor = getResources().getColor(android.R.color.white);
            statusBg = R.drawable.bg_status_approved;
            tvWarning.setVisibility(View.VISIBLE);
            if ("Approved".equals(booking.getStatus())) {
                tvWarning.setText("Payment is fully paid. Confirming will officially enroll the boarder and mark the room as occupied.");
                tvWarning.setBackgroundResource(R.drawable.bg_status_approved);
            } else {
                tvWarning.setVisibility(View.GONE);
            }
        } else if (paidPeriods > 0 && paidPeriods < totalPeriods) {
            paymentStatusText = "Partially Paid";
            statusColor = getResources().getColor(android.R.color.white);
            statusBg = R.drawable.bg_status_completed;
            tvWarning.setVisibility(View.VISIBLE);
            if ("Approved".equals(booking.getStatus())) {
                tvWarning.setText("⚠ Partial payment received. Please verify screenshot before confirming.");
                tvWarning.setTextColor(getResources().getColor(android.R.color.white));
                tvWarning.setBackgroundResource(R.drawable.bg_status_completed);
            } else {
                tvWarning.setText("⚠ Some periods are paid but not all. Please verify payment screenshot.");
                tvWarning.setTextColor(getResources().getColor(android.R.color.white));
                tvWarning.setBackgroundResource(R.drawable.bg_rounded_red);
            }
        } else {
            // This is likely Stage 2 (Initial Approval)
            paymentStatusText = "No Payment Yet";
            statusColor = getResources().getColor(android.R.color.white);
            statusBg = R.drawable.bg_status_pending;
            tvWarning.setVisibility(View.VISIBLE);
            if ("Approved".equals(booking.getStatus())) {
                tvWarning.setText("Boarder has not submitted payment proof yet. Only confirm if you've received payment through other means.");
                tvWarning.setBackgroundResource(R.drawable.bg_rounded_red);
            } else {
                tvWarning.setText("This is an initial application. Approving will reserve the room for the boarder. Payment will be required after your approval.");
                tvWarning.setTextColor(getResources().getColor(android.R.color.white));
                tvWarning.setBackgroundResource(R.drawable.bg_status_completed); // Use blue/green instead of red warning
            }
        }
        
        tvPaymentStatus.setText(paymentStatusText);
        tvPaymentStatus.setTextColor(statusColor);
        tvPaymentStatus.setBackgroundResource(statusBg);
        
        // Update confirm button text
        if ("Approved".equals(booking.getStatus())) {
            btnConfirm.setText("Confirm Payment");
        } else {
            btnConfirm.setText("Approve Application");
        }
        
        // Set amounts
        if (paidAmount != null && !paidAmount.isEmpty()) {
            try {
                double paidValue = Double.parseDouble(paidAmount);
                if (paidValue > 0) {
                    tvAmountPaid.setText("₱" + formatAmount(paidAmount));
                } else {
                    tvAmountPaid.setText("₱0.00");
                }
            } catch (NumberFormatException e) {
                tvAmountPaid.setText("₱0.00");
            }
        } else {
            tvAmountPaid.setText("₱0.00");
        }
        
        if (totalAmount != null && !totalAmount.isEmpty()) {
            try {
                double totalValue = Double.parseDouble(totalAmount);
                if (totalValue > 0) {
                    tvTotalAmount.setText("₱" + formatAmount(totalAmount));
                } else {
                    tvTotalAmount.setText(booking.getAmount() != null ? booking.getAmount() : "₱0.00");
                }
            } catch (NumberFormatException e) {
                tvTotalAmount.setText(booking.getAmount() != null ? booking.getAmount() : "₱0.00");
            }
        } else {
            tvTotalAmount.setText(booking.getAmount() != null ? booking.getAmount() : "₱0.00");
        }
        
        // Set payment progress
        if (totalPeriods > 0) {
            tvPaymentProgress.setVisibility(View.VISIBLE);
            progressBarPayment.setVisibility(View.VISIBLE);
            tvProgressPercent.setVisibility(View.VISIBLE);
            
            int totalMonths = booking.getTotalMonthsForBooking();
            int paidMonths = booking.getPaidMonthsForBooking();
            
            if (totalMonths > 0 && totalPeriods == totalMonths) {
                if (totalMonths == 1) {
                    tvPaymentProgress.setText(String.format("%d/%d month paid", paidMonths, totalMonths));
                } else {
                    tvPaymentProgress.setText(String.format("%d/%d months paid", paidMonths, totalMonths));
                }
            } else {
                if (totalPeriods == 1) {
                    tvPaymentProgress.setText(String.format("%d/%d period paid", paidPeriods, totalPeriods));
                } else {
                    tvPaymentProgress.setText(String.format("%d/%d periods paid", paidPeriods, totalPeriods));
                }
            }
            
            progressBarPayment.setProgress((int) progressPercent);
            tvProgressPercent.setText(String.format("%.0f%%", progressPercent));
            
            // Set progress bar color
            if (isFullyPaid) {
                progressBarPayment.setProgressTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50")));
                tvPaymentProgress.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
            } else if (progressPercent >= 50) {
                progressBarPayment.setProgressTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF9800")));
                tvPaymentProgress.setTextColor(android.graphics.Color.parseColor("#FF9800"));
            } else {
                progressBarPayment.setProgressTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F44336")));
                tvPaymentProgress.setTextColor(android.graphics.Color.parseColor("#F44336"));
            }
        } else {
            tvPaymentProgress.setVisibility(View.GONE);
            progressBarPayment.setVisibility(View.GONE);
            tvProgressPercent.setVisibility(View.GONE);
        }
        
        // Load payment proof
        loadPaymentProofForDialog(booking, imgPaymentProof, tvNoProof, layoutPaymentProof);
        
        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            // Ensure light background
            dialog.getWindow().getDecorView().setBackgroundColor(getResources().getColor(android.R.color.white));
        }
        
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            confirmApproveBooking(booking);
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private void loadPaymentProofForDialog(BookingData booking, android.widget.ImageView imgPaymentProof, 
                                          TextView tvNoProof, LinearLayout layoutPaymentProof) {
        // Fetch payment proof from the booking's payment record
        String url = "https://boardease.calapebohol.com/get_payment_proof_by_booking.php?booking_id=" + booking.getBookingId();
        
        com.android.volley.RequestQueue requestQueue = com.android.volley.toolbox.Volley.newRequestQueue(getContext());
        com.android.volley.toolbox.StringRequest request = new com.android.volley.toolbox.StringRequest(
            com.android.volley.Request.Method.GET, url,
            response -> {
                try {
                    org.json.JSONObject jsonResponse = new org.json.JSONObject(response);
                    if (jsonResponse.getBoolean("success")) {
                        String paymentProofUrl = jsonResponse.optString("payment_proof_url", "");
                        String receiptUrl = jsonResponse.optString("receipt_url", "");
                        
                        String proofUrl = null;
                        if (receiptUrl != null && !receiptUrl.isEmpty() && !receiptUrl.equals("null")) {
                            proofUrl = receiptUrl;
                        } else if (paymentProofUrl != null && !paymentProofUrl.isEmpty() && !paymentProofUrl.equals("null")) {
                            proofUrl = paymentProofUrl;
                        }
                        
                        if (proofUrl != null && !proofUrl.isEmpty() && !proofUrl.equals("null")) {
                            // Show image view and hide "no proof" text
                            imgPaymentProof.setVisibility(View.VISIBLE);
                            tvNoProof.setVisibility(View.GONE);
                            layoutPaymentProof.setVisibility(View.VISIBLE);
                            
                            // Build full URL
                            String baseUrl = "https://boardease.calapebohol.com/";
                            String urlToProcess = proofUrl.trim();
                            String finalFullUrl;
                            
                            if (urlToProcess.startsWith("http://") || urlToProcess.startsWith("https://")) {
                                finalFullUrl = urlToProcess;
                            } else {
                                if (urlToProcess.startsWith("/")) {
                                    urlToProcess = urlToProcess.substring(1);
                                }
                                if (urlToProcess.startsWith("BoardEase2/")) {
                                    urlToProcess = urlToProcess.substring(11);
                                }
                                finalFullUrl = baseUrl + "get_payment_proof.php?path=" + android.net.Uri.encode(urlToProcess, "UTF-8");
                            }
                            
                            // Load image using Glide
                            try {
                                com.bumptech.glide.Glide.with(getContext())
                                    .load(finalFullUrl)
                                    .placeholder(android.R.drawable.ic_menu_report_image)
                                    .error(android.R.drawable.ic_dialog_alert)
                                    .into(imgPaymentProof);
                                
                                // Make image clickable to view full size
                                final String imageUrl = finalFullUrl;
                                imgPaymentProof.setOnClickListener(v -> {
                                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                                    intent.setDataAndType(android.net.Uri.parse(imageUrl), "image/*");
                                    try {
                                        startActivity(intent);
                                    } catch (Exception e) {
                                        android.widget.Toast.makeText(getContext(), "Cannot open image viewer", android.widget.Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } catch (Exception e) {
                                android.util.Log.e("PendingBookingsFragment", "Error loading payment proof", e);
                                imgPaymentProof.setVisibility(View.GONE);
                                tvNoProof.setVisibility(View.VISIBLE);
                            }
                        } else {
                            // No payment proof available
                            imgPaymentProof.setVisibility(View.GONE);
                            tvNoProof.setVisibility(View.VISIBLE);
                            layoutPaymentProof.setVisibility(View.VISIBLE);
                        }
                    } else {
                        // No payment proof found
                        imgPaymentProof.setVisibility(View.GONE);
                        tvNoProof.setVisibility(View.VISIBLE);
                        layoutPaymentProof.setVisibility(View.VISIBLE);
                    }
                } catch (org.json.JSONException e) {
                    android.util.Log.e("PendingBookingsFragment", "Error parsing payment proof response", e);
                    imgPaymentProof.setVisibility(View.GONE);
                    tvNoProof.setVisibility(View.VISIBLE);
                    layoutPaymentProof.setVisibility(View.VISIBLE);
                }
            },
            error -> {
                android.util.Log.e("PendingBookingsFragment", "Error fetching payment proof: " + error.getMessage());
                imgPaymentProof.setVisibility(View.GONE);
                tvNoProof.setVisibility(View.VISIBLE);
                layoutPaymentProof.setVisibility(View.VISIBLE);
            }
        );
        
        requestQueue.add(request);
    }
    
    private void confirmApproveBooking(BookingData booking) {
        showProgressDialog("Approving booking...");
        
        String url = "https://boardease.calapebohol.com/approve_booking.php";
        
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
                            String message = response.optString("message", "Booking updated successfully!");
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
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
        
        String url = "https://boardease.calapebohol.com/decline_booking.php";
        
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
    
    private String formatAmount(String amount) {
        try {
            double amountValue = Double.parseDouble(amount);
            java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,##0.00");
            return formatter.format(amountValue);
        } catch (NumberFormatException e) {
            return amount;
        }
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

















