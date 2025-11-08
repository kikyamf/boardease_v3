package com.example.mock;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private static final String BASE_URL = "http://192.168.1.9/boardease_v3/";
    private static final String GET_BOOKINGS_URL = BASE_URL + "BoardEase2/get_boarder_bookings.php";
    private static final String GET_UNPAID_BREAKDOWNS_URL = BASE_URL + "BoardEase2/get_unpaid_payment_breakdowns.php";
    
    // Request queue
    private RequestQueue requestQueue;

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

            // Setup Pending Bookings RecyclerView
            pendingBookingsAdapter = new BookingAdapter(getContext(), pendingBookings, null);
            LinearLayoutManager pendingLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
            rvPendingBookings.setLayoutManager(pendingLayoutManager);
            rvPendingBookings.setAdapter(pendingBookingsAdapter);

            // Setup Booking History RecyclerView
            bookingHistoryAdapter = new BookingAdapter(getContext(), bookingHistory, null);
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
                return;
            }

            // Show loading indicator
            if (progressBar != null) {
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
                
                Booking booking = new Booking(bookingId, bhName, imagePath, location, 
                    startDate, endDate, monthlyDue, balanceDueStr, status, roomCategory, roomNumber);
                
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
                // TODO: Implement Report for Maintenance functionality
                Toast.makeText(getContext(), "Report for Maintenance functionality coming soon", Toast.LENGTH_SHORT).show();
            });

        } catch (Exception e) {
            Log.e(TAG, "Error showing booking details dialog: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(getContext(), "Error showing booking details", Toast.LENGTH_SHORT).show();
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

        public Booking(int bookingId, String boardingHouseName, String imagePath, String location,
                      String startDate, String endDate, String monthlyDue, String balanceDue, String status,
                      String roomCategory, String roomNumber) {
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
                                    List<PaymentBreakdown> unpaidBreakdowns = parsePaymentBreakdowns(breakdownsArray);
                                    
                                    if (unpaidBreakdowns.isEmpty()) {
                                        Toast.makeText(getContext(), "No unpaid payments found. All payments are up to date!", Toast.LENGTH_LONG).show();
                                    } else {
                                        showUnpaidPaymentBreakdownsDialog(unpaidBreakdowns);
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
    
    private void showUnpaidPaymentBreakdownsDialog(List<PaymentBreakdown> unpaidBreakdowns) {
        try {
            if (getContext() == null) {
                return;
            }
            
            // Filter only unpaid and selected breakdowns (should already be filtered by API, but double-check)
            List<PaymentBreakdown> filteredBreakdowns = new ArrayList<>();
            for (PaymentBreakdown breakdown : unpaidBreakdowns) {
                if (breakdown.isSelected() && !breakdown.isPaid()) {
                    filteredBreakdowns.add(breakdown);
                }
            }
            
            if (filteredBreakdowns.isEmpty()) {
                Toast.makeText(getContext(), "No unpaid payments found. All payments are up to date!", Toast.LENGTH_LONG).show();
                return;
            }
            
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_unpaid_payments, null);
            builder.setView(dialogView);
            
            // Initialize views
            ImageButton btnClose = dialogView.findViewById(R.id.btnClose);
            TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
            LinearLayout layoutBreakdowns = dialogView.findViewById(R.id.layoutBreakdowns);
            TextView tvTotalAmount = dialogView.findViewById(R.id.tvTotalAmount);
            
            tvTitle.setText("Unpaid Payment Periods");
            
            // Calculate total unpaid amount and create breakdown items
            double totalUnpaid = 0.0;
            for (PaymentBreakdown breakdown : filteredBreakdowns) {
                totalUnpaid += breakdown.getAmount();
                
                // Create breakdown item view
                View breakdownItem = LayoutInflater.from(getContext()).inflate(R.layout.item_payment_breakdown, null);
                TextView tvPeriodLabel = breakdownItem.findViewById(R.id.tvPeriodLabel);
                TextView tvPeriodDates = breakdownItem.findViewById(R.id.tvPeriodDates);
                TextView tvAmount = breakdownItem.findViewById(R.id.tvAmount);
                TextView tvDueDate = breakdownItem.findViewById(R.id.tvDueDate);
                TextView tvStatus = breakdownItem.findViewById(R.id.tvStatus);
                
                tvPeriodLabel.setText(breakdown.getPeriodLabel());
                tvPeriodDates.setText(breakdown.getStartDate() + " - " + breakdown.getEndDate());
                tvAmount.setText("₱" + String.format(Locale.getDefault(), "%,.2f", breakdown.getAmount()));
                tvDueDate.setText("Due: " + breakdown.getDueDate());
                
                // Set status
                if ("Overdue".equals(breakdown.getPaymentStatus())) {
                    tvStatus.setText("Overdue");
                    tvStatus.setBackgroundResource(R.drawable.bg_status_cancelled);
                    tvDueDate.setTextColor(getResources().getColor(R.color.red));
                } else {
                    tvStatus.setText("Pending");
                    tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                }
                
                layoutBreakdowns.addView(breakdownItem);
            }
            
            tvTotalAmount.setText("Total Unpaid: ₱" + String.format(Locale.getDefault(), "%,.2f", totalUnpaid));
            
            AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();
            
            btnClose.setOnClickListener(v -> dialog.dismiss());
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing unpaid payment breakdowns dialog: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(getContext(), "Error displaying payment breakdowns", Toast.LENGTH_SHORT).show();
        }
    }
}
