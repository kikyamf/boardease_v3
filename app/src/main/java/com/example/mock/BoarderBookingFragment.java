package com.example.mock;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
//for updates

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.mock.adapters.BookingAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * BoarderBookingFragment - Displays current bookings and booking history
 * Allows users to view their booking details and history
 */
public class BoarderBookingFragment extends Fragment {

    // Views
    private RecyclerView rvCurrentBookings;
    private RecyclerView rvPendingBookings;
    private RecyclerView rvBookingHistory;
    private LinearLayout layoutCurrentBookingsEmpty;
    private LinearLayout layoutPendingBookingsEmpty;
    private LinearLayout layoutBookingHistoryEmpty;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Adapters
    private BookingAdapter currentBookingsAdapter;
    private BookingAdapter pendingBookingsAdapter;
    private BookingAdapter bookingHistoryAdapter;

    // Data
    private List<Booking> currentBookings;
    private List<Booking> pendingBookings;
    private List<Booking> bookingHistory;

    // User session
    private SharedPreferences userSessionPrefs;
    private int userId;

    // API
    private static final String TAG = "BoarderBookingFragment";
    private static final String BASE_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/";
    private static final String BOARD_EASE2_URL = BASE_URL + "BoardEase2/";
    private static final String GET_BOOKINGS_URL = BASE_URL + "BoardEase2/get_boarder_bookings.php";
    private static final String GET_UNPAID_BREAKDOWNS_URL = BASE_URL + "BoardEase2/get_unpaid_payment_breakdowns.php";
    private static final String GET_BH_DETAILS_URL = BASE_URL + "BoardEase2/get_boarding_house_details1.php";
    private static final String SUBMIT_PAYMENT_URL = BOARD_EASE2_URL + "submit_payment.php";
    
    // Request queue
    private RequestQueue requestQueue;
    
    // Payment dialog references for image handling
    private View currentPaymentDialogView;
    
    // Activity result launchers for image picking
    private ActivityResultLauncher<String> cashImagePickerLauncher;
    private ActivityResultLauncher<String> gcashImagePickerLauncher;

    public BoarderBookingFragment() {
        // Required empty public constructor
    }

    public static BoarderBookingFragment newInstance() {
        return new BoarderBookingFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getContext() != null) {
            userSessionPrefs = getContext().getSharedPreferences("UserSession", 0);
            
            // Get user ID from UserSession
            String userIdString = userSessionPrefs.getString("user_id", null);
            if (userIdString != null) {
                try {
                    userId = Integer.parseInt(userIdString);
                } catch (NumberFormatException e) {
                    userId = 0;
                }
            }
            
            requestQueue = Volley.newRequestQueue(getContext());
            
            // Register activity result launchers for image picking
            cashImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && currentPaymentDialogView != null) {
                        Object[] tagData = (Object[]) currentPaymentDialogView.getTag();
                        if (tagData != null && tagData.length >= 6) {
                            Uri[] cashProofUri = (Uri[]) tagData[0];
                            ImageView ivCashProof = (ImageView) tagData[2];
                            MaterialButton btnRemoveCash = (MaterialButton) tagData[4];
                            cashProofUri[0] = uri;
                            displayImage(uri, ivCashProof);
                            btnRemoveCash.setVisibility(View.VISIBLE);
                        }
                    }
                }
            );
            
            gcashImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && currentPaymentDialogView != null) {
                        Object[] tagData = (Object[]) currentPaymentDialogView.getTag();
                        if (tagData != null && tagData.length >= 6) {
                            Uri[] gcashProofUri = (Uri[]) tagData[1];
                            ImageView ivGcashProof = (ImageView) tagData[3];
                            MaterialButton btnRemoveGcash = (MaterialButton) tagData[5];
                            gcashProofUri[0] = uri;
                            displayImage(uri, ivGcashProof);
                            btnRemoveGcash.setVisibility(View.VISIBLE);
                        }
                    }
                }
            );
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_boarder_booking, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupRecyclerViews();
        loadBookingData();
    }

    private void initializeViews(View view) {
        try {
            rvCurrentBookings = view.findViewById(R.id.rvCurrentBookings);
            rvPendingBookings = view.findViewById(R.id.rvPendingBookings);
            rvBookingHistory = view.findViewById(R.id.rvBookingHistory);
            layoutCurrentBookingsEmpty = view.findViewById(R.id.layoutCurrentBookingsEmpty);
            layoutPendingBookingsEmpty = view.findViewById(R.id.layoutPendingBookingsEmpty);
            layoutBookingHistoryEmpty = view.findViewById(R.id.layoutBookingHistoryEmpty);
            progressBar = view.findViewById(R.id.progressBar);
            swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
            
            // Set up pull-to-refresh listener
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setOnRefreshListener(() -> {
                    loadBookingData();
                });
                // Set brown color scheme for pull-to-refresh
                swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.brown));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupRecyclerViews() {
        try {
            // Initialize data lists
            currentBookings = new ArrayList<>();
            pendingBookings = new ArrayList<>();
            bookingHistory = new ArrayList<>();

            // Setup Current Bookings RecyclerView with click listener
            currentBookingsAdapter = new BookingAdapter(getContext(), currentBookings, this::showCurrentBookingDetailsDialog);
            LinearLayoutManager currentLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
            rvCurrentBookings.setLayoutManager(currentLayoutManager);
            rvCurrentBookings.setAdapter(currentBookingsAdapter);

            // Setup Pending Bookings RecyclerView with click listener
            pendingBookingsAdapter = new BookingAdapter(getContext(), pendingBookings, this::showPendingBookingDialog);
            LinearLayoutManager pendingLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
            rvPendingBookings.setLayoutManager(pendingLayoutManager);
            rvPendingBookings.setAdapter(pendingBookingsAdapter);

            // Setup Booking History RecyclerView with click listener for review
            bookingHistoryAdapter = new BookingAdapter(getContext(), bookingHistory, this::showReviewDialog);
            LinearLayoutManager historyLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
            rvBookingHistory.setLayoutManager(historyLayoutManager);
            rvBookingHistory.setAdapter(bookingHistoryAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadBookingData() {
        try {
            if (userId == 0) {
                Log.e(TAG, "User ID is 0, cannot load bookings");
                Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                return;
            }

            // Show loading indicator only if not refreshing (to avoid double indicators)
            boolean isRefreshing = swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing();
            if (!isRefreshing && progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }

            // Fetch bookings from database
            fetchBookingsFromAPI();
        } catch (Exception e) {
            Log.e(TAG, "Error loading booking data: " + e.getMessage());
            e.printStackTrace();
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            Toast.makeText(getContext(), "Error loading bookings", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchBookingsFromAPI() {
        try {
            StringRequest stringRequest = new StringRequest(Request.Method.POST, GET_BOOKINGS_URL,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                Log.d(TAG, "Bookings API Response received");
                                
                                if (response == null || response.trim().isEmpty()) {
                                    Log.e(TAG, "Received null or empty response");
                                    showError("Server returned empty response");
                                    return;
                                }
                                
                                // Parse JSON response
                                JSONObject jsonResponse = new JSONObject(response);
                                boolean success = jsonResponse.getBoolean("success");
                                
                                if (success) {
                                    JSONObject data = jsonResponse.getJSONObject("data");
                                    
                                    // Parse current bookings
                                    JSONArray currentArray = data.getJSONArray("current");
                                    parseBookings(currentArray, currentBookings);
                                    
                                    // Parse pending bookings
                                    JSONArray pendingArray = data.getJSONArray("pending");
                                    parseBookings(pendingArray, pendingBookings);
                                    
                                    // Parse booking history
                                    JSONArray historyArray = data.getJSONArray("history");
                                    parseBookings(historyArray, bookingHistory);
                                    
                                    Log.d(TAG, "Loaded bookings - Current: " + currentBookings.size() + 
                                          ", Pending: " + pendingBookings.size() + 
                                          ", History: " + bookingHistory.size());
                                } else {
                                    String error = jsonResponse.optString("error", "Unknown error occurred");
                                    Log.e(TAG, "API Error: " + error);
                                    showError("Failed to load bookings: " + error);
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "JSON parsing error: " + e.getMessage());
                                showError("Error parsing server response: " + e.getMessage());
                            } catch (Exception e) {
                                Log.e(TAG, "Unexpected error: " + e.getMessage());
                                showError("Unexpected error: " + e.getMessage());
                            } finally {
                                if (progressBar != null) {
                                    progressBar.setVisibility(View.GONE);
                                }
                                if (swipeRefreshLayout != null) {
                                    swipeRefreshLayout.setRefreshing(false);
                                }
                                updateUI();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "Volley error: " + error.getMessage());
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                            if (swipeRefreshLayout != null) {
                                swipeRefreshLayout.setRefreshing(false);
                            }
                            showError("Network error: " + error.getMessage());
                        }
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("user_id", String.valueOf(userId));
                    return params;
                }
                
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("User-Agent", "BoardEase-Android-App");
                    headers.put("Accept", "application/json");
                    return headers;
                }
            };
            
            requestQueue.add(stringRequest);
        } catch (Exception e) {
            Log.e(TAG, "Error creating request: " + e.getMessage());
            e.printStackTrace();
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            showError("Error loading bookings");
        }
    }

    private void parseBookings(JSONArray dataArray, List<Booking> bookingsList) throws JSONException {
        bookingsList.clear();
        
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject bookingJson = dataArray.getJSONObject(i);
                
                int bookingId = bookingJson.getInt("booking_id");
                String bhName = bookingJson.getString("bh_name");
                String imagePath = bookingJson.optString("image_path", "");
                String location = bookingJson.optString("bh_address", "");
                
                // Parse dates
                String startDateStr = bookingJson.getString("start_date");
                String endDateStr = bookingJson.getString("end_date");
                String startDate = formatDate(startDateStr, inputFormat, outputFormat);
                String endDate = formatDate(endDateStr, inputFormat, outputFormat);
                
                // Parse price and payments
                double price = bookingJson.getDouble("price");
                double balanceDue = bookingJson.getDouble("balance_due");
                String monthlyDue = "₱" + String.format(Locale.getDefault(), "%.2f", price);
                String balanceDueStr = "₱" + String.format(Locale.getDefault(), "%.2f", balanceDue);
                
                String status = bookingJson.getString("booking_status");
                
                // Get room category and room number
                String roomCategory = bookingJson.optString("room_category", "Private Room");
                String roomNumber = bookingJson.optString("room_number", "");
                int roomId = bookingJson.optInt("room_id", 0);
                int bhId = bookingJson.optInt("bh_id", 0);
                
                Booking booking = new Booking(bookingId, bhName, imagePath, location, 
                    startDate, endDate, monthlyDue, balanceDueStr, status, roomCategory, roomNumber, roomId, bhId);
                
                bookingsList.add(booking);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing booking data: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error parsing booking data: " + e.getMessage());
            throw new JSONException("Error parsing booking data: " + e.getMessage());
        }
    }

    private String formatDate(String dateStr, SimpleDateFormat inputFormat, SimpleDateFormat outputFormat) {
        try {
            if (dateStr != null && !dateStr.isEmpty()) {
                java.util.Date date = inputFormat.parse(dateStr);
                return outputFormat.format(date);
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + dateStr, e);
        }
        return dateStr;
    }

    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        // Clear all lists on error
        currentBookings.clear();
        pendingBookings.clear();
        bookingHistory.clear();
    }

    private void updateUI() {
        try {
            // Update current bookings
            if (currentBookingsAdapter != null) {
                currentBookingsAdapter.notifyDataSetChanged();
            }
            
            // Update pending bookings
            if (pendingBookingsAdapter != null) {
                pendingBookingsAdapter.notifyDataSetChanged();
            }
            
            // Update booking history
            if (bookingHistoryAdapter != null) {
                bookingHistoryAdapter.notifyDataSetChanged();
            }

            // Show/hide empty states for current bookings
            if (currentBookings.isEmpty()) {
                if (rvCurrentBookings != null) {
                    rvCurrentBookings.setVisibility(View.GONE);
                }
                if (layoutCurrentBookingsEmpty != null) {
                    layoutCurrentBookingsEmpty.setVisibility(View.VISIBLE);
                }
            } else {
                if (rvCurrentBookings != null) {
                    rvCurrentBookings.setVisibility(View.VISIBLE);
                }
                if (layoutCurrentBookingsEmpty != null) {
                    layoutCurrentBookingsEmpty.setVisibility(View.GONE);
                }
            }

            // Show/hide empty states for pending bookings
            if (pendingBookings.isEmpty()) {
                if (rvPendingBookings != null) {
                    rvPendingBookings.setVisibility(View.GONE);
                }
                if (layoutPendingBookingsEmpty != null) {
                    layoutPendingBookingsEmpty.setVisibility(View.VISIBLE);
                }
            } else {
                if (rvPendingBookings != null) {
                    rvPendingBookings.setVisibility(View.VISIBLE);
                }
                if (layoutPendingBookingsEmpty != null) {
                    layoutPendingBookingsEmpty.setVisibility(View.GONE);
                }
            }

            // Show/hide empty states for booking history
            if (bookingHistory.isEmpty()) {
                if (rvBookingHistory != null) {
                    rvBookingHistory.setVisibility(View.GONE);
                }
                if (layoutBookingHistoryEmpty != null) {
                    layoutBookingHistoryEmpty.setVisibility(View.VISIBLE);
                }
            } else {
                if (rvBookingHistory != null) {
                    rvBookingHistory.setVisibility(View.VISIBLE);
                }
                if (layoutBookingHistoryEmpty != null) {
                    layoutBookingHistoryEmpty.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showCurrentBookingDetailsDialog(Booking booking) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_current_booking_details, null);
            builder.setView(dialogView);

            // Initialize dialog views
            ImageButton btnClose = dialogView.findViewById(R.id.btnClose);
            ImageView imgBoardingHouse = dialogView.findViewById(R.id.imgBoardingHouse);
            TextView tvBoardingHouseName = dialogView.findViewById(R.id.tvBoardingHouseName);
            TextView tvLocation = dialogView.findViewById(R.id.tvLocation);
            TextView tvRoomDetails = dialogView.findViewById(R.id.tvRoomDetails);
            TextView tvStartDate = dialogView.findViewById(R.id.tvStartDate);
            TextView tvEndDate = dialogView.findViewById(R.id.tvEndDate);
            TextView tvMonthlyDue = dialogView.findViewById(R.id.tvMonthlyDue);
            TextView tvStatus = dialogView.findViewById(R.id.tvStatus);
            com.google.android.material.button.MaterialButton btnMakePayment = dialogView.findViewById(R.id.btnMakePayment);
            com.google.android.material.button.MaterialButton btnReportMaintenance = dialogView.findViewById(R.id.btnReportMaintenance);

            // Set booking data
            if (booking.getImagePath() != null && !booking.getImagePath().isEmpty()) {
                Glide.with(getContext())
                        .load(booking.getImagePath())
                        .placeholder(R.drawable.sample_listing)
                        .error(R.drawable.sample_listing)
                        .into(imgBoardingHouse);
            } else {
                imgBoardingHouse.setImageResource(R.drawable.sample_listing);
            }

            tvBoardingHouseName.setText(booking.getBoardingHouseName());
            tvLocation.setText(booking.getLocation());
            
            // Set room details in format: "Type of Room | Room_number"
            String roomDetails = booking.getRoomCategory();
            if (booking.getRoomNumber() != null && !booking.getRoomNumber().isEmpty()) {
                roomDetails += " | " + booking.getRoomNumber();
            }
            tvRoomDetails.setText(roomDetails);
            
            tvStartDate.setText(booking.getStartDate());
            tvEndDate.setText(booking.getEndDate());
            tvMonthlyDue.setText(booking.getMonthlyDue());
            
            // Display "Active" instead of "Confirmed"
            String status = booking.getStatus();
            String displayStatus = "Confirmed".equals(status) ? "Active" : status;
            tvStatus.setText(displayStatus);

            // Set status background
            if ("Confirmed".equals(status)) {
                tvStatus.setBackgroundResource(R.drawable.bg_status_approved);
            } else {
                tvStatus.setBackgroundResource(R.drawable.bg_status_approved);
            }

            AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();

            // Close button click listener
            btnClose.setOnClickListener(v -> dialog.dismiss());

            // Make Payment button click listener
            btnMakePayment.setOnClickListener(v -> {
                dialog.dismiss();
                fetchUnpaidPaymentBreakdowns(booking.getBookingId());
            });

            // Report for Maintenance button click listener
            btnReportMaintenance.setOnClickListener(v -> {
                dialog.dismiss();
                // Show maintenance report dialog (UI only - functionality to be added later)
                showMaintenanceReportDialog(booking);
            });

        } catch (Exception e) {
            Log.e(TAG, "Error showing booking details dialog: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(getContext(), "Error showing booking details", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPendingBookingDialog(Booking booking) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_pending_booking_approval, null);
            builder.setView(dialogView);

            // Initialize dialog views
            com.google.android.material.button.MaterialButton btnOk = dialogView.findViewById(R.id.btnOkPending);

            AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();

            // OK button click listener
            btnOk.setOnClickListener(v -> dialog.dismiss());
        } catch (Exception e) {
            Log.e(TAG, "Error showing pending booking dialog: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(getContext(), "Error showing dialog", Toast.LENGTH_SHORT).show();
        }
    }

    // Booking data class
    public static class Booking {
        private int bookingId;
        private String boardingHouseName;
        private String imagePath;
        private String location;
        private String startDate;
        private String endDate;
        private String monthlyDue;
        private String balanceDue;
        private String status;
        private String roomCategory;
        private String roomNumber;
        private int roomId;
        private int bhId;

        public Booking(int bookingId, String boardingHouseName, String imagePath, String location,
                      String startDate, String endDate, String monthlyDue, String balanceDue, String status,
                      String roomCategory, String roomNumber, int roomId, int bhId) {
            this.bookingId = bookingId;
            this.boardingHouseName = boardingHouseName;
            this.imagePath = imagePath;
            this.location = location;
            this.startDate = startDate;
            this.endDate = endDate;
            this.monthlyDue = monthlyDue;
            this.balanceDue = balanceDue;
            this.status = status;
            this.roomCategory = roomCategory;
            this.roomNumber = roomNumber;
            this.roomId = roomId;
            this.bhId = bhId;
        }

        // Getters
        public int getBookingId() { return bookingId; }
        public String getBoardingHouseName() { return boardingHouseName; }
        public String getImagePath() { return imagePath; }
        public String getLocation() { return location; }
        public String getStartDate() { return startDate; }
        public String getEndDate() { return endDate; }
        public String getMonthlyDue() { return monthlyDue; }
        public String getBalanceDue() { return balanceDue; }
        public String getStatus() { return status; }
        public String getRoomCategory() { return roomCategory; }
        public String getRoomNumber() { return roomNumber; }
        public int getRoomId() { return roomId; }
        public int getBhId() { return bhId; }
    }
    
    // Payment Breakdown data class
    private static class PaymentBreakdown {
        private int breakdownId;
        private String periodLabel;
        private String periodType;
        private int periodNumber;
        private double amount;
        private String startDate;
        private String endDate;
        private String dueDate;
        private String paymentStatus;
        private boolean isSelected;
        private boolean isPaid;
        
        public PaymentBreakdown(int breakdownId, String periodLabel, String periodType, int periodNumber,
                               double amount, String startDate, String endDate, String dueDate,
                               String paymentStatus, boolean isSelected, boolean isPaid) {
            this.breakdownId = breakdownId;
            this.periodLabel = periodLabel;
            this.periodType = periodType;
            this.periodNumber = periodNumber;
            this.amount = amount;
            this.startDate = startDate;
            this.endDate = endDate;
            this.dueDate = dueDate;
            this.paymentStatus = paymentStatus;
            this.isSelected = isSelected;
            this.isPaid = isPaid;
        }
        
        // Getters
        public int getBreakdownId() { return breakdownId; }
        public String getPeriodLabel() { return periodLabel; }
        public String getPeriodType() { return periodType; }
        public int getPeriodNumber() { return periodNumber; }
        public double getAmount() { return amount; }
        public String getStartDate() { return startDate; }
        public String getEndDate() { return endDate; }
        public String getDueDate() { return dueDate; }
        public String getPaymentStatus() { return paymentStatus; }
        public boolean isSelected() { return isSelected; }
        public boolean isPaid() { return isPaid; }
    }
    
    private void fetchUnpaidPaymentBreakdowns(int bookingId) {
        try {
            if (getContext() == null) {
                return;
            }
            
            // Show loading toast
            Toast.makeText(getContext(), "Loading payment breakdowns...", Toast.LENGTH_SHORT).show();
            
            StringRequest stringRequest = new StringRequest(Request.Method.POST, GET_UNPAID_BREAKDOWNS_URL,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d(TAG, "Unpaid breakdowns response: " + response);
                            try {
                                JSONObject jsonResponse = new JSONObject(response);
                                if (jsonResponse.getBoolean("success")) {
                                    JSONArray breakdownsArray = jsonResponse.getJSONArray("data");
                                    int count = jsonResponse.optInt("count", breakdownsArray.length());
                                    Log.d(TAG, "Received " + count + " unpaid payment breakdowns for booking_id: " + bookingId);
                                    
                                    List<PaymentBreakdown> unpaidBreakdowns = parsePaymentBreakdowns(breakdownsArray);
                                    Log.d(TAG, "Parsed " + unpaidBreakdowns.size() + " payment breakdowns");
                                    
                                    // Log each breakdown for debugging
                                    for (PaymentBreakdown breakdown : unpaidBreakdowns) {
                                        Log.d(TAG, "Breakdown: " + breakdown.getPeriodLabel() + 
                                            " - Status: " + breakdown.getPaymentStatus() + 
                                            " - Due: " + breakdown.getDueDate() + 
                                            " - Selected: " + breakdown.isSelected() + 
                                            " - Paid: " + breakdown.isPaid());
                                    }
                                    
                                    if (unpaidBreakdowns.isEmpty()) {
                                        Toast.makeText(getContext(), "No unpaid payments found. All payments are up to date!", Toast.LENGTH_LONG).show();
                                    } else {
                                        showUnpaidPaymentBreakdownsDialog(unpaidBreakdowns, bookingId);
                                    }
                                } else {
                                    String error = jsonResponse.optString("error", "Unknown error");
                                    Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing unpaid breakdowns: " + e.getMessage());
                                Toast.makeText(getContext(), "Error loading payment breakdowns", Toast.LENGTH_SHORT).show();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "Volley error fetching unpaid breakdowns: " + error.getMessage());
                            Toast.makeText(getContext(), "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("booking_id", String.valueOf(bookingId));
                    return params;
                }
                
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("User-Agent", "BoardEase-Android-App");
                    headers.put("Accept", "application/json");
                    return headers;
                }
            };
            
            requestQueue.add(stringRequest);
        } catch (Exception e) {
            Log.e(TAG, "Error fetching unpaid payment breakdowns: " + e.getMessage());
            Toast.makeText(getContext(), "Error loading payment breakdowns", Toast.LENGTH_SHORT).show();
        }
    }
    
    private List<PaymentBreakdown> parsePaymentBreakdowns(JSONArray breakdownsArray) throws JSONException {
        List<PaymentBreakdown> breakdowns = new ArrayList<>();
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        
        for (int i = 0; i < breakdownsArray.length(); i++) {
            JSONObject breakdownJson = breakdownsArray.getJSONObject(i);
            
            int breakdownId = breakdownJson.getInt("breakdown_id");
            String periodLabel = breakdownJson.getString("period_label");
            String periodType = breakdownJson.getString("period_type");
            int periodNumber = breakdownJson.getInt("period_number");
            double amount = breakdownJson.getDouble("amount");
            String startDate = formatDate(breakdownJson.getString("period_start_date"), inputFormat, outputFormat);
            String endDate = formatDate(breakdownJson.getString("period_end_date"), inputFormat, outputFormat);
            String dueDate = formatDate(breakdownJson.getString("due_date"), inputFormat, outputFormat);
            String paymentStatus = breakdownJson.getString("payment_status");
            boolean isSelected = breakdownJson.getInt("is_selected") == 1;
            boolean isPaid = breakdownJson.getInt("is_paid") == 1;
            
            breakdowns.add(new PaymentBreakdown(breakdownId, periodLabel, periodType, periodNumber,
                    amount, startDate, endDate, dueDate, paymentStatus, isSelected, isPaid));
        }
        
        return breakdowns;
    }
    
    private void showUnpaidPaymentBreakdownsDialog(List<PaymentBreakdown> unpaidBreakdowns, int bookingId) {
        try {
            if (getContext() == null) {
                return;
            }
            
            // Filter only unpaid breakdowns (API already filters, but double-check for safety)
            // Show ALL unpaid periods, not just initially selected ones (allows advance payment)
            List<PaymentBreakdown> filteredBreakdowns = new ArrayList<>();
            for (PaymentBreakdown breakdown : unpaidBreakdowns) {
                // Show all unpaid periods regardless of initial selection status
                if (!breakdown.isPaid()) {
                    filteredBreakdowns.add(breakdown);
                }
            }
            
            if (filteredBreakdowns.isEmpty()) {
                Toast.makeText(getContext(), "No unpaid payments found. All payments are up to date!", Toast.LENGTH_LONG).show();
                return;
            }
            
            Log.d(TAG, "Showing dialog with " + filteredBreakdowns.size() + " unpaid payment periods");
            
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_unpaid_payments, null);
            builder.setView(dialogView);
            
            // Initialize views
            ImageButton btnClose = dialogView.findViewById(R.id.btnClose);
            TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
            LinearLayout layoutBreakdowns = dialogView.findViewById(R.id.layoutBreakdowns);
            TextView tvTotalAmount = dialogView.findViewById(R.id.tvTotalAmount);
            com.google.android.material.button.MaterialButton btnProceedToPayment = dialogView.findViewById(R.id.btnProceedToPayment);
            com.google.android.material.button.MaterialButton btnToggleAllPayments = dialogView.findViewById(R.id.btnToggleAllPayments);
            
            tvTitle.setText("Payment Periods");
            
            // Map to track selected breakdowns
            Map<Integer, PaymentBreakdown> selectedBreakdowns = new HashMap<>();
            List<android.widget.CheckBox> checkboxes = new ArrayList<>();
            List<View> breakdownItemViews = new ArrayList<>(); // Store all breakdown item views
            
            // Track if all payments are visible
            boolean[] showAllPayments = {false};
            
            // Create breakdown items with checkboxes
            for (int i = 0; i < filteredBreakdowns.size(); i++) {
                PaymentBreakdown breakdown = filteredBreakdowns.get(i);
                
                // Create breakdown item view
                View breakdownItem = LayoutInflater.from(getContext()).inflate(R.layout.item_payment_breakdown, null);
                android.widget.CheckBox checkboxPeriod = breakdownItem.findViewById(R.id.checkboxPeriod);
                TextView tvPeriodLabel = breakdownItem.findViewById(R.id.tvPeriodLabel);
                TextView tvPeriodDates = breakdownItem.findViewById(R.id.tvPeriodDates);
                TextView tvAmount = breakdownItem.findViewById(R.id.tvAmount);
                TextView tvDueDate = breakdownItem.findViewById(R.id.tvDueDate);
                TextView tvStatus = breakdownItem.findViewById(R.id.tvStatus);
                
                // Show/hide elements for BoarderBookingFragment usage
                checkboxPeriod.setVisibility(View.VISIBLE);
                tvPeriodDates.setVisibility(View.VISIBLE);
                tvAmount.setVisibility(View.VISIBLE);
                tvDueDate.setVisibility(View.VISIBLE);
                tvStatus.setVisibility(View.VISIBLE);
                
                // Hide elements not used by BoarderBookingFragment
                // Hide the LinearLayout containing start/end dates
                View startDateParent = (View) breakdownItem.findViewById(R.id.tvPeriodStartDate).getParent();
                if (startDateParent != null && startDateParent.getParent() instanceof View) {
                    ((View) startDateParent.getParent()).setVisibility(View.GONE);
                }
                breakdownItem.findViewById(R.id.tvPeriodAmount).setVisibility(View.GONE);
                breakdownItem.findViewById(R.id.tvPeriodStatus).setVisibility(View.GONE);
                
                // Set period data
                tvPeriodLabel.setText(breakdown.getPeriodLabel());
                tvPeriodDates.setText(breakdown.getStartDate() + " - " + breakdown.getEndDate());
                tvAmount.setText("₱" + String.format(Locale.getDefault(), "%,.2f", breakdown.getAmount()));
                tvDueDate.setText("Due: " + breakdown.getDueDate());
                
                // Determine if this is current/overdue period (should be checked by default)
                boolean isCurrentOrOverdue = false;
                if ("Overdue".equals(breakdown.getPaymentStatus())) {
                    isCurrentOrOverdue = true;
                } else if (breakdown.getDueDate() != null && !breakdown.getDueDate().isEmpty()) {
                    isCurrentOrOverdue = isDateCurrentOrPast(breakdown.getDueDate());
                }
                
                // Set checkbox styling - brown border and check when clicked
                checkboxPeriod.setButtonTintList(getResources().getColorStateList(R.color.checkbox_brown));
                
                // Chronological validation: disable checkboxes if previous period is not checked
                // First period is always enabled, others are disabled by default
                if (i == 0) {
                    // First checkbox is always enabled
                    checkboxPeriod.setEnabled(true);
                    checkboxPeriod.setAlpha(1.0f);
                    // Check first period if it's current/overdue
                    if (isCurrentOrOverdue) {
                        checkboxPeriod.setChecked(true);
                        selectedBreakdowns.put(breakdown.getBreakdownId(), breakdown);
                    } else {
                        checkboxPeriod.setChecked(false);
                    }
                } else {
                    // Later periods are disabled by default until previous one is checked
                    checkboxPeriod.setEnabled(false);
                    checkboxPeriod.setAlpha(0.5f); // Visual indication that it's disabled
                    checkboxPeriod.setChecked(false);
                }
                
                // Set status and colors
                if ("Overdue".equals(breakdown.getPaymentStatus())) {
                    tvStatus.setText("Overdue");
                    tvStatus.setBackgroundResource(R.drawable.bg_status_cancelled);
                    if (breakdown.getDueDate() != null && !breakdown.getDueDate().isEmpty()) {
                        tvDueDate.setTextColor(getResources().getColor(R.color.red));
                    }
                } else if (breakdown.getDueDate() != null && !breakdown.getDueDate().isEmpty() && isDateDueSoon(breakdown.getDueDate())) {
                    tvStatus.setText("Due Soon");
                    tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                    tvDueDate.setTextColor(getResources().getColor(R.color.orange));
                } else {
                    tvStatus.setText("Pending");
                    tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                    // Keep default color for future periods
                }
                
                // Set checkbox listener with chronological validation
                int periodIndex = i; // Capture index for lambda
                checkboxPeriod.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        selectedBreakdowns.put(breakdown.getBreakdownId(), breakdown);
                        // Enable the next period if this one is checked
                        updateCheckboxStates(checkboxes, selectedBreakdowns, filteredBreakdowns, 
                                            breakdownItemViews, tvTotalAmount, btnProceedToPayment);
                    } else {
                        selectedBreakdowns.remove(breakdown.getBreakdownId());
                        // Disable and uncheck all later periods if this one is unchecked
                        updateCheckboxStates(checkboxes, selectedBreakdowns, filteredBreakdowns, 
                                            breakdownItemViews, tvTotalAmount, btnProceedToPayment);
                    }
                    updateSelectedTotal(selectedBreakdowns, tvTotalAmount, btnProceedToPayment);
                });
                
                checkboxes.add(checkboxPeriod);
                breakdownItemViews.add(breakdownItem);
                
                // Ensure margins are applied by setting LayoutParams
                // Convert dp to pixels for proper spacing
                float density = getContext().getResources().getDisplayMetrics().density;
                int marginInPx = (int) (10 * density); // 10dp spacing (reduced from 20dp)
                
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                layoutParams.setMargins(0, marginInPx, 0, marginInPx); // left, top, right, bottom
                breakdownItem.setLayoutParams(layoutParams);
                
                layoutBreakdowns.addView(breakdownItem);
                
                // Initially show only the first period (soonest due date)
                // Hide all other periods
                if (i > 0) {
                    breakdownItem.setVisibility(View.GONE);
                }
            }
            
            // Update toggle button text and visibility
            if (filteredBreakdowns.size() > 1) {
                btnToggleAllPayments.setVisibility(View.VISIBLE);
                btnToggleAllPayments.setText("Show All Pending Payments (" + (filteredBreakdowns.size() - 1) + " more)");
            } else {
                btnToggleAllPayments.setVisibility(View.GONE);
            }
            
            // Initialize checkbox states after all checkboxes are created
            // This ensures the second checkbox is enabled if the first one is checked by default
            updateCheckboxStates(checkboxes, selectedBreakdowns, filteredBreakdowns, 
                                breakdownItemViews, tvTotalAmount, btnProceedToPayment);
            
            // Toggle button click listener
            btnToggleAllPayments.setOnClickListener(v -> {
                showAllPayments[0] = !showAllPayments[0];
                
                if (showAllPayments[0]) {
                    // Show all payments
                    for (int i = 1; i < breakdownItemViews.size(); i++) {
                        breakdownItemViews.get(i).setVisibility(View.VISIBLE);
                    }
                    btnToggleAllPayments.setText("Hide All Pending Payments");
                } else {
                    // Hide all except the first one
                    for (int i = 1; i < breakdownItemViews.size(); i++) {
                        breakdownItemViews.get(i).setVisibility(View.GONE);
                    }
                    btnToggleAllPayments.setText("Show All Pending Payments (" + (filteredBreakdowns.size() - 1) + " more)");
                }
            });
            
            // Update total for initially selected items
            updateSelectedTotal(selectedBreakdowns, tvTotalAmount, btnProceedToPayment);
            
            AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();
            
            btnClose.setOnClickListener(v -> dialog.dismiss());
            
            // Proceed to Payment button
            btnProceedToPayment.setOnClickListener(v -> {
                // Validate that at least one payment period is selected
                if (selectedBreakdowns == null || selectedBreakdowns.isEmpty()) {
                    Toast.makeText(getContext(), "Please select at least one payment period to proceed", Toast.LENGTH_LONG).show();
                    return;
                }
                
                dialog.dismiss();
                List<PaymentBreakdown> selectedList = new ArrayList<>(selectedBreakdowns.values());
                proceedToPayment(selectedList, bookingId);
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing unpaid payment breakdowns dialog: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(getContext(), "Error displaying payment breakdowns", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateSelectedTotal(Map<Integer, PaymentBreakdown> selectedBreakdowns, TextView tvTotalAmount, 
                                     com.google.android.material.button.MaterialButton btnProceedToPayment) {
        double totalSelected = 0.0;
        boolean hasSelection = selectedBreakdowns != null && !selectedBreakdowns.isEmpty();
        
        if (hasSelection) {
            for (PaymentBreakdown breakdown : selectedBreakdowns.values()) {
                totalSelected += breakdown.getAmount();
            }
            tvTotalAmount.setText("₱" + String.format(Locale.getDefault(), "%,.2f", totalSelected));
        } else {
            tvTotalAmount.setText("₱0.00");
        }
        
        // Disable button if no periods are selected
        btnProceedToPayment.setEnabled(hasSelection);
        
        // Update button color based on selection
        if (getContext() != null) {
            if (hasSelection) {
                // Original green color when enabled (#2E7D32)
                btnProceedToPayment.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), R.color.green_enabled)));
            } else {
                // Disabled/muted green color when no selection (#81C784 - lighter green)
                btnProceedToPayment.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(getContext(), R.color.green_disabled)));
            }
        }
    }
    
    /**
     * Updates checkbox states based on chronological validation.
     * Enables/disables checkboxes and unchecks later periods if earlier ones are unchecked.
     */
    private void updateCheckboxStates(List<android.widget.CheckBox> checkboxes,
                                      Map<Integer, PaymentBreakdown> selectedBreakdowns,
                                      List<PaymentBreakdown> filteredBreakdowns,
                                      List<View> breakdownItemViews,
                                      TextView tvTotalAmount,
                                      com.google.android.material.button.MaterialButton btnProceedToPayment) {
        // Enable/disable checkboxes based on sequential logic
        for (int i = 0; i < checkboxes.size(); i++) {
            android.widget.CheckBox checkBox = checkboxes.get(i);
            PaymentBreakdown breakdown = filteredBreakdowns.get(i);
            
            if (i == 0) {
                // First checkbox is always enabled
                checkBox.setEnabled(true);
                checkBox.setAlpha(1.0f);
            } else {
                // Can only check if previous is checked
                android.widget.CheckBox previousCheckBox = checkboxes.get(i - 1);
                boolean canEnable = previousCheckBox.isChecked();
                checkBox.setEnabled(canEnable);
                
                // Update alpha based on enabled state
                checkBox.setAlpha(canEnable ? 1.0f : 0.5f);
                
                // If previous is unchecked, uncheck this one too and remove from selected
                if (!canEnable && checkBox.isChecked()) {
                    checkBox.setChecked(false);
                    selectedBreakdowns.remove(breakdown.getBreakdownId());
                    // Recursively uncheck all subsequent periods
                    for (int j = i + 1; j < checkboxes.size(); j++) {
                        android.widget.CheckBox laterCheckBox = checkboxes.get(j);
                        if (laterCheckBox.isChecked()) {
                            laterCheckBox.setChecked(false);
                            selectedBreakdowns.remove(filteredBreakdowns.get(j).getBreakdownId());
                        }
                        laterCheckBox.setEnabled(false);
                        laterCheckBox.setAlpha(0.5f);
                    }
                }
            }
        }
        
        // Update total after state changes
        updateSelectedTotal(selectedBreakdowns, tvTotalAmount, btnProceedToPayment);
    }
    
    private boolean isDateCurrentOrPast(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            Date dueDate = sdf.parse(dateStr);
            Date today = new Date();
            return dueDate != null && (dueDate.before(today) || dueDate.equals(today));
        } catch (ParseException e) {
            return false;
        }
    }
    
    private boolean isDateDueSoon(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            Date dueDate = sdf.parse(dateStr);
            if (dueDate == null) return false;
            
            Calendar cal = Calendar.getInstance();
            Date today = cal.getTime();
            cal.add(Calendar.DAY_OF_MONTH, 7);
            Date weekFromNow = cal.getTime();
            
            return dueDate.after(today) && dueDate.before(weekFromNow);
        } catch (ParseException e) {
            return false;
        }
    }
    
    private void proceedToPayment(List<PaymentBreakdown> selectedBreakdowns, int bookingId) {
        try {
            if (getContext() == null) {
                return;
            }
            
            // Find booking to get bh_id
            int bhId = 0;
            for (Booking booking : currentBookings) {
                if (booking.getBookingId() == bookingId) {
                    bhId = booking.getBhId();
                    break;
                }
            }
            // If not found in current, check pending
            if (bhId == 0) {
                for (Booking booking : pendingBookings) {
                    if (booking.getBookingId() == bookingId) {
                        bhId = booking.getBhId();
                        break;
                    }
                }
            }
            
            if (bhId == 0) {
                Toast.makeText(getContext(), "Error: Could not find booking details", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Calculate total amount
            double total = 0.0;
            for (PaymentBreakdown breakdown : selectedBreakdowns) {
                total += breakdown.getAmount();
            }
            
            // Show payment method dialog
            showPaymentMethodDialog(selectedBreakdowns, bookingId, bhId, total);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in proceedToPayment: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(getContext(), "Error opening payment dialog", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showPaymentMethodDialog(List<PaymentBreakdown> selectedBreakdowns, int bookingId, int bhId, double totalAmount) {
        try {
            if (getContext() == null) {
                return;
            }
            
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_payment_method, null);
            builder.setView(dialogView);
            
            // Initialize views
            ImageButton btnClosePayment = dialogView.findViewById(R.id.btnClosePayment);
            RadioGroup rgPaymentMethod = dialogView.findViewById(R.id.rgPaymentMethod);
            RadioButton rbCash = dialogView.findViewById(R.id.rbCash);
            RadioButton rbGcash = dialogView.findViewById(R.id.rbGcash);
            LinearLayout layoutCashPayment = dialogView.findViewById(R.id.layoutCashPayment);
            LinearLayout layoutGcashPayment = dialogView.findViewById(R.id.layoutGcashPayment);
            MaterialButton btnUploadCash = dialogView.findViewById(R.id.btnUploadCash);
            MaterialButton btnUploadGcash = dialogView.findViewById(R.id.btnUploadGcash);
            MaterialButton btnRemoveCash = dialogView.findViewById(R.id.btnRemoveCash);
            MaterialButton btnRemoveGcash = dialogView.findViewById(R.id.btnRemoveGcash);
            ImageView ivCashProof = dialogView.findViewById(R.id.ivCashProof);
            ImageView ivGcashProof = dialogView.findViewById(R.id.ivGcashProof);
            ImageView ivOwnerQrCode = dialogView.findViewById(R.id.ivOwnerQrCode);
            TextView tvGcashNumber = dialogView.findViewById(R.id.tvGcashNumber);
            MaterialButton btnSubmitPayment = dialogView.findViewById(R.id.btnSubmitPayment);
            
            // Payment method and proof URIs
            String[] paymentMethod = {"Cash"}; // Use array to allow modification in inner classes
            Uri[] cashProofUri = {null};
            Uri[] gcashProofUri = {null};
            String[] ownerGcashQrPath = {null};
            String[] ownerGcashNumber = {null};
            
            // Create and show dialog
            AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();
            
            // Close button
            btnClosePayment.setOnClickListener(v -> {
                currentPaymentDialogView = null;
                dialog.dismiss();
            });
            
            // Payment method selection
            rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == rbCash.getId()) {
                    paymentMethod[0] = "Cash";
                    layoutCashPayment.setVisibility(View.VISIBLE);
                    layoutGcashPayment.setVisibility(View.GONE);
                } else if (checkedId == rbGcash.getId()) {
                    paymentMethod[0] = "GCash";
                    layoutCashPayment.setVisibility(View.GONE);
                    layoutGcashPayment.setVisibility(View.VISIBLE);
                    // Load owner's GCash QR code
                    loadOwnerGcashQr(bhId, ivOwnerQrCode, tvGcashNumber, ownerGcashQrPath, ownerGcashNumber);
                }
            });
            
            // Upload buttons
            btnUploadCash.setOnClickListener(v -> {
                if (cashImagePickerLauncher != null) {
                    cashImagePickerLauncher.launch("image/*");
                }
            });
            
            btnUploadGcash.setOnClickListener(v -> {
                if (gcashImagePickerLauncher != null) {
                    gcashImagePickerLauncher.launch("image/*");
                }
            });
            
            // Remove buttons
            btnRemoveCash.setOnClickListener(v -> {
                cashProofUri[0] = null;
                ivCashProof.setVisibility(View.GONE);
                btnRemoveCash.setVisibility(View.GONE);
            });
            
            btnRemoveGcash.setOnClickListener(v -> {
                gcashProofUri[0] = null;
                ivGcashProof.setVisibility(View.GONE);
                btnRemoveGcash.setVisibility(View.GONE);
            });
            
            // Store references for image handling
            currentPaymentDialogView = dialogView;
            dialogView.setTag(new Object[]{cashProofUri, gcashProofUri, ivCashProof, ivGcashProof, btnRemoveCash, btnRemoveGcash});
            
            // Submit payment button
            btnSubmitPayment.setOnClickListener(v -> {
                // Validate payment proof
                if ("Cash".equals(paymentMethod[0]) && cashProofUri[0] == null) {
                    Toast.makeText(getContext(), "Please upload cash transaction photo", Toast.LENGTH_SHORT).show();
                    return;
                } else if ("GCash".equals(paymentMethod[0]) && gcashProofUri[0] == null) {
                    Toast.makeText(getContext(), "Please upload GCash payment screenshot", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Submit payment
                submitPayment(selectedBreakdowns, bookingId, totalAmount, paymentMethod[0], 
                    cashProofUri[0], gcashProofUri[0], dialog);
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing payment method dialog: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(getContext(), "Error showing payment dialog", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadOwnerGcashQr(int bhId, ImageView ivOwnerQrCode, TextView tvGcashNumber, 
                                   String[] ownerGcashQrPath, String[] ownerGcashNumber) {
        if (bhId == 0) {
            Log.e(TAG, "Cannot load GCash QR - bh_id is 0");
            return;
        }
        
        String url = GET_BH_DETAILS_URL + "?bh_id=" + bhId;
        
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            JSONObject data = jsonResponse.getJSONObject("data");
                            JSONObject bh = data.getJSONObject("boarding_house");
                            
                            // Get GCash QR code
                            if (bh.has("gcash_qr") && !bh.isNull("gcash_qr")) {
                                String qrPath = bh.optString("gcash_qr", "");
                                if (qrPath != null && !qrPath.isEmpty() && !qrPath.equals("null")) {
                                    ownerGcashQrPath[0] = qrPath;
                                    String fullImageUrl = qrPath;
                                    if (!qrPath.startsWith("http://") && !qrPath.startsWith("https://")) {
                                        if (qrPath.startsWith("uploads/")) {
                                            fullImageUrl = BOARD_EASE2_URL + qrPath;
                                        } else {
                                            fullImageUrl = BOARD_EASE2_URL + "uploads/" + qrPath;
                                        }
                                    }
                                    Glide.with(getContext())
                                        .load(fullImageUrl)
                                        .placeholder(R.drawable.placeholder)
                                        .error(R.drawable.placeholder)
                                        .into(ivOwnerQrCode);
                                }
                            }
                            
                            // Get GCash number
                            if (bh.has("gcash_number") && !bh.isNull("gcash_number")) {
                                String gcashNum = bh.optString("gcash_number", "");
                                if (gcashNum != null && !gcashNum.isEmpty() && !gcashNum.equals("null")) {
                                    ownerGcashNumber[0] = gcashNum;
                                    tvGcashNumber.setText("GCash: " + gcashNum);
                                    tvGcashNumber.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing GCash info: " + e.getMessage());
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Error loading GCash info: " + error.getMessage());
                }
            }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", "BoardEase-Android-App");
                headers.put("Accept", "application/json");
                return headers;
            }
        };
        
        requestQueue.add(stringRequest);
    }
    
    private void displayImage(Uri imageUri, ImageView imageView) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), imageUri);
            imageView.setImageBitmap(bitmap);
            imageView.setVisibility(View.VISIBLE);
        } catch (IOException e) {
            Log.e(TAG, "Error displaying image: " + e.getMessage());
            Toast.makeText(getContext(), "Error loading image", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void submitPayment(List<PaymentBreakdown> selectedBreakdowns, int bookingId, double totalAmount,
                              String paymentMethod, Uri cashProofUri, Uri gcashProofUri, AlertDialog dialog) {
        try {
            if (getContext() == null) {
                return;
            }
            
            // Convert image to base64 - use final array to allow modification in inner class
            final String[] paymentProofBase64 = {""};
            try {
                if ("Cash".equals(paymentMethod) && cashProofUri != null) {
                    paymentProofBase64[0] = imageToBase64(cashProofUri);
                } else if ("GCash".equals(paymentMethod) && gcashProofUri != null) {
                    paymentProofBase64[0] = imageToBase64(gcashProofUri);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error converting image to base64: " + e.getMessage());
                Toast.makeText(getContext(), "Error processing image", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Prepare breakdown IDs
            final List<Integer> breakdownIds = new ArrayList<>();
            for (PaymentBreakdown breakdown : selectedBreakdowns) {
                breakdownIds.add(breakdown.getBreakdownId());
            }
            
            // Show loading
            final android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(getContext());
            progressDialog.setMessage("Submitting payment...");
            progressDialog.setCancelable(false);
            progressDialog.show();
            
            // Create request
            StringRequest stringRequest = new StringRequest(Request.Method.POST, SUBMIT_PAYMENT_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressDialog.dismiss();
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            if (jsonResponse.getBoolean("success")) {
                                currentPaymentDialogView = null;
                                dialog.dismiss();
                                Toast.makeText(getContext(), "Payment submitted successfully!", Toast.LENGTH_LONG).show();
                                // Refresh bookings
                                loadBookingData();
                            } else {
                                String error = jsonResponse.optString("error", "Failed to submit payment");
                                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing payment response: " + e.getMessage());
                            Toast.makeText(getContext(), "Error processing response", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.dismiss();
                        Log.e(TAG, "Error submitting payment: " + error.getMessage());
                        Toast.makeText(getContext(), "Network error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("booking_id", String.valueOf(bookingId));
                    params.put("payment_method", paymentMethod);
                    params.put("total_amount", String.format(Locale.getDefault(), "%.2f", totalAmount));
                    params.put("payment_proof", paymentProofBase64[0]);
                    
                    // Add breakdown IDs as JSON array
                    try {
                        JSONArray breakdownIdsArray = new JSONArray();
                        for (Integer id : breakdownIds) {
                            breakdownIdsArray.put(id);
                        }
                        params.put("breakdown_ids", breakdownIdsArray.toString());
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating breakdown IDs array: " + e.getMessage());
                    }
                    
                    return params;
                }
                
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("User-Agent", "BoardEase-Android-App");
                    headers.put("Accept", "application/json");
                    return headers;
                }
            };
            
            requestQueue.add(stringRequest);
            
        } catch (Exception e) {
            Log.e(TAG, "Error submitting payment: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(getContext(), "Error submitting payment", Toast.LENGTH_SHORT).show();
        }
    }
    
    private String imageToBase64(Uri imageUri) throws IOException {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), imageUri);
        
        // Resize if too large
        int maxWidth = 800;
        int maxHeight = 800;
        if (bitmap.getWidth() > maxWidth || bitmap.getHeight() > maxHeight) {
            float scale = Math.min((float) maxWidth / bitmap.getWidth(), (float) maxHeight / bitmap.getHeight());
            int newWidth = Math.round(bitmap.getWidth() * scale);
            int newHeight = Math.round(bitmap.getHeight() * scale);
            bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageBytes = baos.toByteArray();
        return android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP);
    }
    
    /**
     * Show maintenance report dialog and handle form submission
     */
    private void showMaintenanceReportDialog(Booking booking) {
        try {
            if (getContext() == null) {
                return;
            }
            
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_report_maintenance, null);
            builder.setView(dialogView);
            
            // Initialize views
            ImageButton btnClose = dialogView.findViewById(R.id.btnCloseMaintenance);
            com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelMaintenance);
            com.google.android.material.button.MaterialButton btnSubmit = dialogView.findViewById(R.id.btnSubmitMaintenance);
            android.widget.RadioGroup radioGroupArea = dialogView.findViewById(R.id.radioGroupAreaForMaintenance);
            android.widget.RadioButton radioBHRoom = dialogView.findViewById(R.id.radioBHRoom);
            android.widget.RadioButton radioBathroom = dialogView.findViewById(R.id.radioBathroom);
            android.widget.RadioButton radioKitchen = dialogView.findViewById(R.id.radioKitchen);
            android.widget.RadioButton radioOthers = dialogView.findViewById(R.id.radioOthers);
            com.google.android.material.textfield.TextInputEditText etTitle = dialogView.findViewById(R.id.etMaintenanceTitle);
            com.google.android.material.textfield.TextInputEditText etDescription = dialogView.findViewById(R.id.etDescription);
            
            // Set orange cursor color for text fields (API 29+ only)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                try {
                    android.graphics.drawable.Drawable cursorDrawable = getContext().getResources().getDrawable(R.drawable.cursor_orange);
                    if (etTitle != null && cursorDrawable != null) {
                        etTitle.setTextCursorDrawable(cursorDrawable);
                    }
                    if (etDescription != null && cursorDrawable != null) {
                        etDescription.setTextCursorDrawable(cursorDrawable);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Could not set cursor color: " + e.getMessage());
                }
            }
            
            // Create and show dialog
            android.app.AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();
            
            // Close button click listener
            btnClose.setOnClickListener(v -> dialog.dismiss());
            
            // Cancel button click listener
            btnCancel.setOnClickListener(v -> dialog.dismiss());
            
            // Submit button click listener
            btnSubmit.setOnClickListener(v -> {
                // Validate form
                String subject = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
                
                // Get selected area
                String areaForMaintenance = "";
                int selectedRadioId = radioGroupArea.getCheckedRadioButtonId();
                if (selectedRadioId == radioBHRoom.getId()) {
                    areaForMaintenance = "BH Room";
                } else if (selectedRadioId == radioBathroom.getId()) {
                    areaForMaintenance = "Bathroom";
                } else if (selectedRadioId == radioKitchen.getId()) {
                    areaForMaintenance = "Kitchen";
                } else if (selectedRadioId == radioOthers.getId()) {
                    areaForMaintenance = "Others";
                }
                
                // Validate fields
                if (subject.isEmpty()) {
                    etTitle.setError("Subject is required");
                    etTitle.requestFocus();
                    return;
                }
                
                if (areaForMaintenance.isEmpty()) {
                    Toast.makeText(getContext(), "Please select an area for maintenance", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (description.isEmpty()) {
                    etDescription.setError("Description is required");
                    etDescription.requestFocus();
                    return;
                }
                
                // Submit maintenance request
                submitMaintenanceRequest(booking, subject, areaForMaintenance, description, dialog);
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing maintenance report dialog: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(getContext(), "Error showing maintenance report dialog", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Submit maintenance request to server
     */
    private void submitMaintenanceRequest(Booking booking, String subject, String areaForMaintenance, String description, android.app.AlertDialog dialog) {
        try {
            if (getContext() == null) {
                return;
            }
            
            // Get user ID from SharedPreferences
            SharedPreferences sharedPreferences = getContext().getSharedPreferences("UserSession", getContext().MODE_PRIVATE);
            String userIdString = sharedPreferences.getString("user_id", "0");
            int userId = 0;
            try {
                userId = Integer.parseInt(userIdString);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid user_id in SharedPreferences: " + userIdString);
                Toast.makeText(getContext(), "Error: Invalid user session", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (userId == 0) {
                Toast.makeText(getContext(), "Error: User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Show loading indicator
            android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(getContext());
            progressDialog.setMessage("Submitting maintenance request...");
            progressDialog.setCancelable(false);
            progressDialog.show();
            
            // Get room ID from booking
            int roomId = booking.getRoomId();
            
            // API URL - using local IP address
            String url = BASE_URL + "BoardEase2/submit_maintenance_request.php";
            
            // Create JSON request body
            JSONObject requestBody = new JSONObject();
            try {
                requestBody.put("user_id", userId);
                // Only include room_id if it's valid (greater than 0)
                if (roomId > 0) {
                    requestBody.put("room_id", roomId);
                }
                requestBody.put("subject", subject);
                requestBody.put("area_for_maintenance", areaForMaintenance);
                requestBody.put("description", description);
            } catch (JSONException e) {
                Log.e(TAG, "Error creating request body: " + e.getMessage());
                progressDialog.dismiss();
                Toast.makeText(getContext(), "Error preparing request", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Create request
            com.android.volley.toolbox.JsonObjectRequest jsonRequest = new com.android.volley.toolbox.JsonObjectRequest(
                com.android.volley.Request.Method.POST,
                url,
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        progressDialog.dismiss();
                        try {
                            if (response.getBoolean("success")) {
                                // Close the maintenance report dialog
                                dialog.dismiss();
                                // Show success dialog
                                showMaintenanceReportSuccessDialog();
                            } else {
                                String error = response.optString("error", "Failed to submit maintenance request");
                                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing response: " + e.getMessage());
                            Toast.makeText(getContext(), "Error parsing server response", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.dismiss();
                        Log.e(TAG, "Volley error: " + error.getMessage());
                        String errorMessage = "Network error";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                String responseBody = new String(error.networkResponse.data, "utf-8");
                                JSONObject errorJson = new JSONObject(responseBody);
                                errorMessage = errorJson.optString("error", "Network error");
                            } catch (Exception e) {
                                errorMessage = "Network error: " + error.getMessage();
                            }
                        }
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("User-Agent", "BoardEase-Android-App");
                    headers.put("Accept", "application/json");
                    return headers;
                }
            };
            
            // Add request to queue
            RequestQueue requestQueue = Volley.newRequestQueue(getContext());
            requestQueue.add(jsonRequest);
            
        } catch (Exception e) {
            Log.e(TAG, "Error submitting maintenance request: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(getContext(), "Error submitting maintenance request", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Show success dialog after maintenance report submission
     */
    private void showMaintenanceReportSuccessDialog() {
        try {
            if (getContext() == null) {
                return;
            }
            
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_maintenance_report_success, null);
            builder.setView(dialogView);
            
            // Initialize views
            com.google.android.material.button.MaterialButton btnOk = dialogView.findViewById(R.id.btnOkSuccess);
            
            // Create and show dialog
            android.app.AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
            
            // OK button click listener
            btnOk.setOnClickListener(v -> dialog.dismiss());
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing success dialog: " + e.getMessage());
            e.printStackTrace();
            // Fallback to toast if dialog fails
            Toast.makeText(getContext(), "Maintenance request submitted successfully", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show review dialog when clicking on a booking history card
     */
    private void showReviewDialog(Booking booking) {
        try {
            if (getContext() == null) {
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_review, null);
            builder.setView(dialogView);

            // Initialize dialog views
            ImageButton btnClose = dialogView.findViewById(R.id.btnCloseReview);
            ImageView ivStar1 = dialogView.findViewById(R.id.ivStar1);
            ImageView ivStar2 = dialogView.findViewById(R.id.ivStar2);
            ImageView ivStar3 = dialogView.findViewById(R.id.ivStar3);
            ImageView ivStar4 = dialogView.findViewById(R.id.ivStar4);
            ImageView ivStar5 = dialogView.findViewById(R.id.ivStar5);
            TextView tvRatingText = dialogView.findViewById(R.id.tvRatingText);
            com.google.android.material.textfield.TextInputEditText etReviewComments = dialogView.findViewById(R.id.etReviewComments);
            com.google.android.material.button.MaterialButton btnSubmitReview = dialogView.findViewById(R.id.btnSubmitReview);

            // Array of star ImageViews for easier management
            ImageView[] stars = {ivStar1, ivStar2, ivStar3, ivStar4, ivStar5};
            
            // Rating state (0 = no rating, 1-5 = rating)
            final int[] currentRating = {0};

            // Function to validate and update button state
            Runnable updateSubmitButtonState = () -> {
                boolean hasRating = currentRating[0] > 0;
                String comments = etReviewComments.getText() != null ? 
                    etReviewComments.getText().toString().trim() : "";
                boolean hasComments = !comments.isEmpty();
                
                boolean isEnabled = hasRating && hasComments;
                btnSubmitReview.setEnabled(isEnabled);
                
                // Update button color based on enabled state
                if (isEnabled) {
                    btnSubmitReview.setBackgroundTintList(
                        ContextCompat.getColorStateList(getContext(), R.color.blue));
                } else {
                    btnSubmitReview.setBackgroundTintList(
                        ContextCompat.getColorStateList(getContext(), R.color.blue_disabled));
                }
            };

            // Function to update stars based on rating
            Runnable updateStars = () -> {
                for (int i = 0; i < stars.length; i++) {
                    if (i < currentRating[0]) {
                        // Fill the star (gold/yellow color)
                        stars[i].setImageResource(R.drawable.ic_star_filled);
                        // Use ImageViewCompat to properly tint vector drawables
                        ImageViewCompat.setImageTintList(stars[i], 
                            android.content.res.ColorStateList.valueOf(0xFFFFC107)); // Gold color #FFC107
                    } else {
                        // Empty star (gray color)
                        stars[i].setImageResource(R.drawable.ic_star_empty);
                        // Use ImageViewCompat to properly tint vector drawables
                        ImageViewCompat.setImageTintList(stars[i], 
                            android.content.res.ColorStateList.valueOf(0xFFCCCCCC)); // Gray color
                    }
                }
                
                // Update rating text based on rating
                String ratingText;
                switch (currentRating[0]) {
                    case 1:
                        ratingText = "Poor";
                        break;
                    case 2:
                        ratingText = "Fair";
                        break;
                    case 3:
                        ratingText = "Good";
                        break;
                    case 4:
                        ratingText = "Very Good";
                        break;
                    case 5:
                        ratingText = "Excellent";
                        break;
                    default:
                        ratingText = "Tap to rate";
                        break;
                }
                tvRatingText.setText(ratingText);
                
                // Update submit button state after rating change
                updateSubmitButtonState.run();
            };

            // Set click listeners for each star
            for (int i = 0; i < stars.length; i++) {
                final int rating = i + 1;
                stars[i].setOnClickListener(v -> {
                    currentRating[0] = rating;
                    updateStars.run();
                });
            }

            // Initialize stars to empty state
            updateStars.run();

            // Add text watcher to comments field to validate button state
            etReviewComments.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updateSubmitButtonState.run();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

            // Create and show dialog
            AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();

            // Initialize button state (disabled by default)
            updateSubmitButtonState.run();

            // Close button click listener
            btnClose.setOnClickListener(v -> dialog.dismiss());

            // Submit Review button click listener
            btnSubmitReview.setOnClickListener(v -> {
                if (!btnSubmitReview.isEnabled()) {
                    return; // Prevent action if button is disabled
                }

                String comments = etReviewComments.getText() != null ? 
                    etReviewComments.getText().toString().trim() : "";

                // Submit review to database
                submitReview(booking.getBookingId(), currentRating[0], comments, dialog);
            });

        } catch (Exception e) {
            Log.e(TAG, "Error showing review dialog: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(getContext(), "Error showing review dialog", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Submit review to server
     */
    private void submitReview(int bookingId, int rating, String comments, AlertDialog dialog) {
        try {
            if (getContext() == null) {
                return;
            }

            // Get user ID from SharedPreferences
            SharedPreferences sharedPreferences = getContext().getSharedPreferences("UserSession", getContext().MODE_PRIVATE);
            String userIdString = sharedPreferences.getString("user_id", "0");
            int userId = 0;
            try {
                userId = Integer.parseInt(userIdString);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid user_id in SharedPreferences: " + userIdString);
                Toast.makeText(getContext(), "Error: Invalid user session", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userId == 0) {
                Toast.makeText(getContext(), "Error: User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get boarding house ID from the booking
            // We need to find the booking in our lists to get bh_id
            int bhId = 0;
            for (Booking booking : bookingHistory) {
                if (booking.getBookingId() == bookingId) {
                    bhId = booking.getBhId();
                    break;
                }
            }

            if (bhId == 0) {
                Toast.makeText(getContext(), "Error: Could not find boarding house information", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading indicator
            android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(getContext());
            progressDialog.setMessage("Submitting review...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            // API URL - Use localhost for review submission
            // Try multiple possible IPs - user can update this based on their network
            // If your XAMPP document root includes boardease_v3 folder, use: "http://192.168.1.5/boardease_v3/BoardEase2/submit_review.php"
            // If your XAMPP document root is boardease_v3, use: "http://192.168.1.5/BoardEase2/submit_review.php"
            String localhostUrl = "https://hookiest-unprotecting-cher.ngrok-free.dev/";
            String url = localhostUrl + "BoardEase2/submit_review.php";
            
            Log.d(TAG, "=== REVIEW SUBMISSION DEBUG ===");
            Log.d(TAG, "Submitting review to: " + url);
            Log.d(TAG, "Review data - userId: " + userId + ", bhId: " + bhId + ", rating: " + rating);
            Log.d(TAG, "Comment length: " + (comments != null ? comments.length() : 0));
            
            // Check if we can reach the server (basic test)
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
                getContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            Log.d(TAG, "Network connected: " + isConnected);
            if (activeNetwork != null) {
                Log.d(TAG, "Network type: " + activeNetwork.getTypeName());
                Log.d(TAG, "Network state: " + activeNetwork.getDetailedState());
            }

            // Create JSON request body
            JSONObject requestBody = new JSONObject();
            try {
                requestBody.put("user_id", userId);
                requestBody.put("bh_id", bhId);
                requestBody.put("rating", rating);
                requestBody.put("comment", comments);
                Log.d(TAG, "Request body: " + requestBody.toString());
            } catch (JSONException e) {
                Log.e(TAG, "Error creating request body: " + e.getMessage());
                progressDialog.dismiss();
                Toast.makeText(getContext(), "Error preparing request", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create request
            com.android.volley.toolbox.JsonObjectRequest jsonRequest = new com.android.volley.toolbox.JsonObjectRequest(
                com.android.volley.Request.Method.POST,
                url,
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        progressDialog.dismiss();
                        Log.d(TAG, "Review submission response: " + response.toString());
                        try {
                            if (response.getBoolean("success")) {
                                // Close the review dialog
                                dialog.dismiss();
                                // Show success message
                                Toast.makeText(getContext(), "Review submitted successfully!", Toast.LENGTH_SHORT).show();
                            } else {
                                String error = response.optString("error", "Failed to submit review");
                                Log.e(TAG, "Review submission failed: " + error);
                                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing response: " + e.getMessage());
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Error parsing server response", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.dismiss();
                        
                        // Enhanced error logging
                        Log.e(TAG, "Volley error type: " + error.getClass().getSimpleName());
                        Log.e(TAG, "Volley error message: " + (error.getMessage() != null ? error.getMessage() : "null"));
                        
                        if (error.networkResponse != null) {
                            Log.e(TAG, "Network response status code: " + error.networkResponse.statusCode);
                            if (error.networkResponse.data != null) {
                                try {
                                    String responseBody = new String(error.networkResponse.data, "utf-8");
                                    Log.e(TAG, "Error response body: " + responseBody);
                                    JSONObject errorJson = new JSONObject(responseBody);
                                    String errorMsg = errorJson.optString("error", "Network error");
                                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                                    return;
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing error response: " + e.getMessage());
                                }
                            }
                        } else {
                            Log.e(TAG, "No network response - connection failed or timeout");
                        }
                        
                        // More specific error messages
                        String errorMessage = "Network error";
                        if (error instanceof com.android.volley.TimeoutError) {
                            errorMessage = "Connection timeout. Server might be slow or unreachable.\n\nPlease verify:\n• XAMPP Apache is running\n• URL: http://192.168.1.5/boardease_v3/BoardEase2/submit_review.php\n• Device is on same WiFi network";
                        } else if (error instanceof com.android.volley.NoConnectionError) {
                            // For localhost, this might just mean server is not reachable, not necessarily no internet
                            errorMessage = "Cannot connect to server.\n\nTroubleshooting:\n1. Test in browser: http://192.168.1.5/boardease_v3/BoardEase2/submit_review.php\n2. Ensure device & PC are on same WiFi\n3. Check Windows Firewall allows port 80\n4. Verify XAMPP Apache is running\n5. Try accessing from device browser first";
                        } else if (error.getMessage() != null && !error.getMessage().isEmpty()) {
                            errorMessage = "Error: " + error.getMessage();
                        } else {
                            String testUrl = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/submit_review.php";
                            errorMessage = "Failed to connect to server.\n\nTest this URL in your device's browser:\n" + testUrl + "\n\nIf browser can't access it, check:\n• Same WiFi network\n• XAMPP running\n• Firewall settings";
                        }
                        
                        // Show detailed error in Logcat and user-friendly message
                        Log.e(TAG, "=== CONNECTION FAILED ===");
                        Log.e(TAG, "URL attempted: " + url);
                        Log.e(TAG, "Error class: " + error.getClass().getSimpleName());
                        Log.e(TAG, "Full error: " + error.toString());
                        
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("User-Agent", "BoardEase-Android-App");
                    headers.put("Accept", "application/json");
                    headers.put("ngrok-skip-browser-warning", "true");
                    return headers;
                }
            };
            
            // Set retry policy
            jsonRequest.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                10000, // 10 seconds timeout
                com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            // Add request to queue
            RequestQueue requestQueue = Volley.newRequestQueue(getContext());
            requestQueue.add(jsonRequest);

        } catch (Exception e) {
            Log.e(TAG, "Error submitting review: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(getContext(), "Error submitting review", Toast.LENGTH_SHORT).show();
        }
    }
}
