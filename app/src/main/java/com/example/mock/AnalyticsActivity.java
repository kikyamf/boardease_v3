package com.example.mock;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.utils.MPPointF;
import android.app.ProgressDialog;
import android.widget.LinearLayout;
import android.widget.ImageButton;
import android.content.Context;

public class AnalyticsActivity extends AppCompatActivity {

    private int userId;
    
    // Revenue TextViews
    private TextView tvTotalRevenue, tvMonthlyRevenue, tvWeeklyRevenue;
    private TextView tvPaidAmount, tvPaidCount, tvUnpaidAmount, tvUnpaidCount;
    
    // Chart views
    private LineChart chartMonthlyRevenue;
    private HorizontalBarChart chartTopRooms;
    private LineChart chartPaymentActivity;
    
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressDialog progressDialog;
    private LinearLayout layoutBoardingHouses;
    private LinearLayout layoutBhOccupancy;
    private com.google.android.material.card.MaterialCardView cardRevenueByBh;
    private com.google.android.material.card.MaterialCardView cardOverallOccupancy;
    private android.widget.ImageView ivExpandRevenue;
    private android.widget.ImageView ivExpandOccupancy;
    private boolean isRevenueExpanded = false;
    private boolean isOccupancyExpanded = false;
    
    // Bookings TextViews
    private TextView tvTotalBookings, tvMonthlyBookings;
    
    // Occupancy TextViews
    private TextView tvOverallOccupancy, tvOccupiedRooms, tvAvailableRooms;
    
    // Payment Status TextViews
    private TextView tvPaidPaymentsCount, tvPaidPaymentsAmount;
    private TextView tvPendingPaymentsCount, tvPendingPaymentsAmount;
    private TextView tvOverduePaymentsCount, tvOverduePaymentsAmount;
    
    // Room Availability TextViews
    private TextView tvOccupiedRoomsCount, tvAvailableRoomsCount;
    
    // Applications TextViews
    private TextView tvPendingApplications, tvApprovedApplications, tvRejectedApplications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);
        
        // Get userId from intent
        userId = getIntent().getIntExtra("user_id", 0);
        if (userId <= 0) {
            Toast.makeText(this, "Invalid user ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Hide default action bar since we have custom header
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        // Initialize views
        initializeViews();
        initializeCharts();
        
        // Setup swipe refresh
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchOwnerDashboardData();
        });
        
        // Show progress dialog and fetch data
        showProgressDialog("Loading analytics...");
        fetchOwnerDashboardData();
    }
    
    private void initializeViews() {
        // Setup back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvMonthlyRevenue = findViewById(R.id.tvMonthlyRevenue);
        tvWeeklyRevenue = findViewById(R.id.tvWeeklyRevenue);
        tvPaidAmount = findViewById(R.id.tvPaidAmount);
        tvPaidCount = findViewById(R.id.tvPaidCount);
        tvUnpaidAmount = findViewById(R.id.tvUnpaidAmount);
        tvUnpaidCount = findViewById(R.id.tvUnpaidCount);
        
        chartMonthlyRevenue = findViewById(R.id.chartMonthlyRevenue);
        chartTopRooms = findViewById(R.id.chartTopRooms);
        chartPaymentActivity = findViewById(R.id.chartPaymentActivity);
        
        layoutBoardingHouses = findViewById(R.id.layoutBoardingHouses);
        layoutBhOccupancy = findViewById(R.id.layoutBhOccupancy);
        
        // Expandable sections
        cardRevenueByBh = findViewById(R.id.cardRevenueByBh);
        cardOverallOccupancy = findViewById(R.id.cardOverallOccupancy);
        ivExpandRevenue = findViewById(R.id.ivExpandRevenue);
        ivExpandOccupancy = findViewById(R.id.ivExpandOccupancy);
        
        // Set click listeners for expandable sections
        if (cardRevenueByBh != null && ivExpandRevenue != null) {
            cardRevenueByBh.setOnClickListener(v -> toggleRevenueSection());
        }
        if (cardOverallOccupancy != null && ivExpandOccupancy != null) {
            cardOverallOccupancy.setOnClickListener(v -> toggleOccupancySection());
        }
        
        // Bookings
        tvTotalBookings = findViewById(R.id.tvTotalBookings);
        tvMonthlyBookings = findViewById(R.id.tvMonthlyBookings);
        
        // Occupancy
        tvOverallOccupancy = findViewById(R.id.tvOverallOccupancy);
        tvOccupiedRooms = findViewById(R.id.tvOccupiedRooms);
        tvAvailableRooms = findViewById(R.id.tvAvailableRooms);
        
        // Payment Status
        tvPaidPaymentsCount = findViewById(R.id.tvPaidPaymentsCount);
        tvPaidPaymentsAmount = findViewById(R.id.tvPaidPaymentsAmount);
        tvPendingPaymentsCount = findViewById(R.id.tvPendingPaymentsCount);
        tvPendingPaymentsAmount = findViewById(R.id.tvPendingPaymentsAmount);
        tvOverduePaymentsCount = findViewById(R.id.tvOverduePaymentsCount);
        tvOverduePaymentsAmount = findViewById(R.id.tvOverduePaymentsAmount);
        
        // Room Availability
        tvOccupiedRoomsCount = findViewById(R.id.tvOccupiedRoomsCount);
        tvAvailableRoomsCount = findViewById(R.id.tvAvailableRoomsCount);
        
        // Applications
        tvPendingApplications = findViewById(R.id.tvPendingApplications);
        tvApprovedApplications = findViewById(R.id.tvApprovedApplications);
        tvRejectedApplications = findViewById(R.id.tvRejectedApplications);
    }
    
    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }
    
    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
    
    private void fetchOwnerDashboardData() {
        String url = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/get_owner_dashboard.php";
        
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        Log.d("AnalyticsActivity", "=== SERVER RESPONSE RECEIVED ===");
                        Log.d("AnalyticsActivity", "Response length: " + response.length() + " characters");
                        Log.d("AnalyticsActivity", "Full Server Response: " + response);
                        
                        JSONObject obj = new JSONObject(response);
                        
                        // Revenue Analytics
                        JSONObject revenue = obj.optJSONObject("revenue");
                        if (revenue != null && tvTotalRevenue != null) {
                            // Total Revenue
                            double totalRevenue = revenue.optDouble("total_revenue", 0.0);
                            if (Double.isNaN(totalRevenue) || Double.isInfinite(totalRevenue)) {
                                totalRevenue = 0.0;
                            }
                            tvTotalRevenue.setText(formatCurrency(totalRevenue));
                            
                            // Monthly Revenue
                            if (tvMonthlyRevenue != null) {
                                double monthlyRevenue = revenue.optDouble("monthly_revenue", 0.0);
                                if (Double.isNaN(monthlyRevenue) || Double.isInfinite(monthlyRevenue)) {
                                    monthlyRevenue = 0.0;
                                }
                                tvMonthlyRevenue.setText(formatCurrency(monthlyRevenue));
                            }
                            
                            // Weekly Revenue
                            if (tvWeeklyRevenue != null) {
                                double weeklyRevenue = revenue.optDouble("weekly_revenue", 0.0);
                                if (Double.isNaN(weeklyRevenue) || Double.isInfinite(weeklyRevenue)) {
                                    weeklyRevenue = 0.0;
                                }
                                tvWeeklyRevenue.setText(formatCurrency(weeklyRevenue));
                            }
                            
                            // Paid Amount & Count
                            if (tvPaidAmount != null && tvPaidCount != null) {
                                double paidAmount = revenue.optDouble("paid_amount", 0.0);
                                if (Double.isNaN(paidAmount) || Double.isInfinite(paidAmount)) {
                                    paidAmount = 0.0;
                                }
                                int paidCount = revenue.optInt("paid_count", 0);
                                tvPaidAmount.setText(formatCurrency(paidAmount));
                                tvPaidCount.setText(paidCount + (paidCount == 1 ? " payment" : " payments"));
                            }
                            
                            // Unpaid Amount & Count
                            if (tvUnpaidAmount != null && tvUnpaidCount != null) {
                                double unpaidAmount = revenue.optDouble("unpaid_amount", 0.0);
                                if (Double.isNaN(unpaidAmount) || Double.isInfinite(unpaidAmount)) {
                                    unpaidAmount = 0.0;
                                }
                                int unpaidCount = revenue.optInt("unpaid_count", 0);
                                tvUnpaidAmount.setText(formatCurrency(unpaidAmount));
                                tvUnpaidCount.setText(unpaidCount + (unpaidCount == 1 ? " payment" : " payments"));
                            }
                            
                            // Populate Boarding House Revenue Breakdown
                            JSONArray boardingHouses = revenue.optJSONArray("boarding_houses");
                            if (boardingHouses != null && boardingHouses.length() > 0) {
                                populateBoardingHousesRevenue(boardingHouses);
                            } else {
                                if (layoutBoardingHouses != null) {
                                    layoutBoardingHouses.removeAllViews();
                                }
                            }
                        } else {
                            // Default values if revenue data is missing
                            if (tvTotalRevenue != null) tvTotalRevenue.setText("₱0.00");
                            if (tvMonthlyRevenue != null) tvMonthlyRevenue.setText("₱0.00");
                            if (tvWeeklyRevenue != null) tvWeeklyRevenue.setText("₱0.00");
                            if (tvPaidAmount != null) tvPaidAmount.setText("₱0.00");
                            if (tvPaidCount != null) tvPaidCount.setText("0 payments");
                            if (tvUnpaidAmount != null) tvUnpaidAmount.setText("₱0.00");
                            if (tvUnpaidCount != null) tvUnpaidCount.setText("0 payments");
                            if (layoutBoardingHouses != null) {
                                layoutBoardingHouses.removeAllViews();
                            }
                        }
                        
                        // Populate Bookings
                        JSONObject bookings = obj.optJSONObject("bookings");
                        if (bookings != null) {
                            if (tvTotalBookings != null) {
                                int totalBookings = bookings.optInt("total_bookings", 0);
                                tvTotalBookings.setText(String.valueOf(totalBookings));
                            }
                            if (tvMonthlyBookings != null) {
                                int monthlyBookings = bookings.optInt("monthly_bookings", 0);
                                tvMonthlyBookings.setText(String.valueOf(monthlyBookings));
                            }
                        }
                        
                        // Populate Occupancy
                        JSONObject occupancy = obj.optJSONObject("occupancy");
                        if (occupancy != null) {
                            if (tvOverallOccupancy != null) {
                                double overallRate = occupancy.optDouble("overall_rate", 0.0);
                                if (Double.isNaN(overallRate) || Double.isInfinite(overallRate)) {
                                    overallRate = 0.0;
                                }
                                tvOverallOccupancy.setText(String.format("%.1f%%", overallRate));
                            }
                            if (tvOccupiedRooms != null) {
                                int occupied = occupancy.optInt("occupied_rooms", 0);
                                tvOccupiedRooms.setText(occupied + " occupied");
                            }
                            if (tvAvailableRooms != null) {
                                int available = occupancy.optInt("available_rooms", 0);
                                tvAvailableRooms.setText(available + " available");
                            }
                            
                            // Per-BH Occupancy
                            JSONArray bhOccupancy = occupancy.optJSONArray("by_boarding_house");
                            if (bhOccupancy != null && bhOccupancy.length() > 0) {
                                populateBoardingHousesOccupancy(bhOccupancy);
                            } else {
                                if (layoutBhOccupancy != null) {
                                    layoutBhOccupancy.removeAllViews();
                                }
                            }
                        }
                        
                        // Populate Payment Status
                        JSONObject paymentStatus = obj.optJSONObject("payment_status");
                        if (paymentStatus != null) {
                            if (tvPaidPaymentsCount != null) {
                                int paidCount = paymentStatus.optInt("paid_count", 0);
                                tvPaidPaymentsCount.setText(String.valueOf(paidCount));
                            }
                            if (tvPaidPaymentsAmount != null) {
                                double paidAmount = paymentStatus.optDouble("paid_amount", 0.0);
                                if (Double.isNaN(paidAmount) || Double.isInfinite(paidAmount)) {
                                    paidAmount = 0.0;
                                }
                                tvPaidPaymentsAmount.setText(formatCurrency(paidAmount));
                            }
                            if (tvPendingPaymentsCount != null) {
                                int pendingCount = paymentStatus.optInt("pending_count", 0);
                                tvPendingPaymentsCount.setText(String.valueOf(pendingCount));
                            }
                            if (tvPendingPaymentsAmount != null) {
                                double pendingAmount = paymentStatus.optDouble("pending_amount", 0.0);
                                if (Double.isNaN(pendingAmount) || Double.isInfinite(pendingAmount)) {
                                    pendingAmount = 0.0;
                                }
                                tvPendingPaymentsAmount.setText(formatCurrency(pendingAmount));
                            }
                            if (tvOverduePaymentsCount != null) {
                                int overdueCount = paymentStatus.optInt("overdue_count", 0);
                                tvOverduePaymentsCount.setText(String.valueOf(overdueCount));
                            }
                            if (tvOverduePaymentsAmount != null) {
                                double overdueAmount = paymentStatus.optDouble("overdue_amount", 0.0);
                                if (Double.isNaN(overdueAmount) || Double.isInfinite(overdueAmount)) {
                                    overdueAmount = 0.0;
                                }
                                tvOverduePaymentsAmount.setText(formatCurrency(overdueAmount));
                            }
                        }
                        
                        // Populate Room Availability
                        JSONObject roomAvailability = obj.optJSONObject("room_availability");
                        if (roomAvailability != null) {
                            if (tvOccupiedRoomsCount != null) {
                                int occupied = roomAvailability.optInt("occupied_rooms", 0);
                                tvOccupiedRoomsCount.setText(String.valueOf(occupied));
                            }
                            if (tvAvailableRoomsCount != null) {
                                int available = roomAvailability.optInt("available_rooms", 0);
                                tvAvailableRoomsCount.setText(String.valueOf(available));
                            }
                        }
                        
                        // Populate Applications
                        JSONObject applications = obj.optJSONObject("applications");
                        if (applications != null) {
                            if (tvPendingApplications != null) {
                                int pending = applications.optInt("pending_count", 0);
                                tvPendingApplications.setText(String.valueOf(pending));
                            }
                            if (tvApprovedApplications != null) {
                                int approved = applications.optInt("approved_count", 0);
                                tvApprovedApplications.setText(String.valueOf(approved));
                            }
                            if (tvRejectedApplications != null) {
                                int rejected = applications.optInt("rejected_count", 0);
                                tvRejectedApplications.setText(String.valueOf(rejected));
                            }
                        }
                        
                        // Populate Charts
                        JSONObject charts = obj.optJSONObject("charts");
                        if (charts != null) {
                            // Monthly Revenue Chart
                            JSONArray monthlyRevenue = charts.optJSONArray("monthly_revenue");
                            if (monthlyRevenue != null && monthlyRevenue.length() > 0 && chartMonthlyRevenue != null) {
                                populateMonthlyRevenueChart(monthlyRevenue);
                                chartMonthlyRevenue.post(() -> chartMonthlyRevenue.invalidate());
                            } else if (chartMonthlyRevenue != null) {
                                showEmptyChartMessage(chartMonthlyRevenue, "No monthly revenue data available");
                            }
                            
                            // Top-Paying Rooms Chart
                            JSONArray topRooms = charts.optJSONArray("top_paying_rooms");
                            if (topRooms != null && topRooms.length() > 0 && chartTopRooms != null) {
                                populateTopRoomsChart(topRooms);
                                chartTopRooms.post(() -> chartTopRooms.invalidate());
                            } else if (chartTopRooms != null) {
                                showEmptyChartMessage(chartTopRooms, "No room revenue data available");
                            }
                            
                            // Payment Activity Chart
                            JSONArray paymentActivity = charts.optJSONArray("payment_activity");
                            if (paymentActivity != null && paymentActivity.length() > 0 && chartPaymentActivity != null) {
                                populatePaymentActivityChart(paymentActivity);
                                chartPaymentActivity.post(() -> chartPaymentActivity.invalidate());
                            } else if (chartPaymentActivity != null) {
                                showEmptyChartMessage(chartPaymentActivity, "No payment activity data available");
                            }
                        } else {
                            if (chartMonthlyRevenue != null) showEmptyChartMessage(chartMonthlyRevenue, "No chart data available");
                            if (chartTopRooms != null) showEmptyChartMessage(chartTopRooms, "No chart data available");
                            if (chartPaymentActivity != null) showEmptyChartMessage(chartPaymentActivity, "No chart data available");
                        }
                        
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "JSON Parsing error", Toast.LENGTH_SHORT).show();
                        Log.e("AnalyticsActivity", "JSON Parse Error: " + e.getMessage());
                    } finally {
                        hideProgressDialog();
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }
                },
                error -> {
                    Log.e("AnalyticsActivity", "Error fetching dashboard: " + error.getMessage());
                    Toast.makeText(this, "Error fetching analytics: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    hideProgressDialog();
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                return params;
            }
        };
        
        Volley.newRequestQueue(this).add(request);
    }
    
    private String formatCurrency(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount == 0) {
            return "₱0.00";
        }
        java.text.DecimalFormat formatter = new java.text.DecimalFormat("₱#,##0.00");
        return formatter.format(amount);
    }
    
    private void populateBoardingHousesRevenue(JSONArray boardingHouses) {
        if (layoutBoardingHouses == null) {
            return;
        }
        
        layoutBoardingHouses.removeAllViews();
        
        try {
            for (int i = 0; i < boardingHouses.length(); i++) {
                JSONObject bh = boardingHouses.getJSONObject(i);
                
                // Create card layout - Revenue cards use blue background (matches revenue value color)
                com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                cardParams.setMargins(
                    (int) (4 * getResources().getDisplayMetrics().density),
                    (int) ((i > 0 ? 8 : 0) * getResources().getDisplayMetrics().density),
                    (int) (4 * getResources().getDisplayMetrics().density),
                    (int) (8 * getResources().getDisplayMetrics().density)
                );
                card.setLayoutParams(cardParams);
                card.setCardBackgroundColor(0xFFBBDEFB); // Slightly darker blue for better contrast
                card.setCardElevation(4f);
                card.setRadius(8f);
                card.setStrokeWidth(0); // No border
                
                // Create inner layout
                LinearLayout cardContent = new LinearLayout(this);
                cardContent.setOrientation(LinearLayout.VERTICAL);
                cardContent.setPadding(
                    (int) (16 * getResources().getDisplayMetrics().density),
                    (int) (16 * getResources().getDisplayMetrics().density),
                    (int) (16 * getResources().getDisplayMetrics().density),
                    (int) (16 * getResources().getDisplayMetrics().density)
                );
                
                // BH Name
                TextView tvBhName = new TextView(this);
                tvBhName.setText(bh.optString("bh_name", "Unnamed"));
                tvBhName.setTextSize(15f);
                tvBhName.setTextColor(0xFF000000);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    tvBhName.setTypeface(getResources().getFont(R.font.poppins_semibold));
                } else {
                    try {
                        android.graphics.Typeface semiboldTypeface = android.graphics.Typeface.createFromAsset(getAssets(), "fonts/poppins_semibold.ttf");
                        tvBhName.setTypeface(semiboldTypeface);
                    } catch (Exception e) {
                        tvBhName.setTypeface(null, android.graphics.Typeface.BOLD);
                    }
                }
                cardContent.addView(tvBhName);
                
                // Address
                String address = bh.optString("address", "");
                if (!address.isEmpty()) {
                    TextView tvAddress = new TextView(this);
                    tvAddress.setText(address);
                    tvAddress.setTextSize(12f);
                    tvAddress.setTextColor(0xFF666666);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        tvAddress.setTypeface(getResources().getFont(R.font.poppins_regular));
                    } else {
                        try {
                            android.graphics.Typeface regularTypeface = android.graphics.Typeface.createFromAsset(getAssets(), "fonts/poppins_regular.ttf");
                            tvAddress.setTypeface(regularTypeface);
                        } catch (Exception e) {
                            tvAddress.setTypeface(null, android.graphics.Typeface.NORMAL);
                        }
                    }
                    LinearLayout.LayoutParams addressParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    addressParams.topMargin = (int) (4 * getResources().getDisplayMetrics().density);
                    tvAddress.setLayoutParams(addressParams);
                    cardContent.addView(tvAddress);
                }
                
                // Revenue Row
                LinearLayout revenueRow = new LinearLayout(this);
                revenueRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams revenueParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                revenueParams.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
                revenueRow.setLayoutParams(revenueParams);
                
                // Total Revenue
                LinearLayout revenueLayout = new LinearLayout(this);
                revenueLayout.setOrientation(LinearLayout.VERTICAL);
                revenueLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ));
                
                TextView tvRevenueLabel = new TextView(this);
                tvRevenueLabel.setText("Total Revenue");
                tvRevenueLabel.setTextSize(12f);
                tvRevenueLabel.setTextColor(0xFF666666);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    tvRevenueLabel.setTypeface(getResources().getFont(R.font.poppins_medium));
                } else {
                    try {
                        android.graphics.Typeface mediumTypeface = android.graphics.Typeface.createFromAsset(getAssets(), "fonts/poppins_medium.ttf");
                        tvRevenueLabel.setTypeface(mediumTypeface);
                    } catch (Exception e) {
                        tvRevenueLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
                    }
                }
                revenueLayout.addView(tvRevenueLabel);
                
                TextView tvRevenueValue = new TextView(this);
                double totalRevenue = bh.optDouble("total_revenue", 0.0);
                if (Double.isNaN(totalRevenue) || Double.isInfinite(totalRevenue)) {
                    totalRevenue = 0.0;
                }
                tvRevenueValue.setText(formatCurrency(totalRevenue));
                tvRevenueValue.setTextSize(18f);
                tvRevenueValue.setTextColor(0xFF2196F3);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    tvRevenueValue.setTypeface(getResources().getFont(R.font.poppins_bold));
                } else {
                    try {
                        android.graphics.Typeface boldTypeface = android.graphics.Typeface.createFromAsset(getAssets(), "fonts/poppins_bold.ttf");
                        tvRevenueValue.setTypeface(boldTypeface);
                    } catch (Exception e) {
                        tvRevenueValue.setTypeface(null, android.graphics.Typeface.BOLD);
                    }
                }
                LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                valueParams.topMargin = (int) (2 * getResources().getDisplayMetrics().density);
                tvRevenueValue.setLayoutParams(valueParams);
                revenueLayout.addView(tvRevenueValue);
                
                TextView tvPaymentCount = new TextView(this);
                int paymentCount = bh.optInt("payment_count", 0);
                tvPaymentCount.setText(paymentCount + (paymentCount == 1 ? " payment" : " payments"));
                tvPaymentCount.setTextSize(11f);
                tvPaymentCount.setTextColor(0xFF666666);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    tvPaymentCount.setTypeface(getResources().getFont(R.font.poppins_regular));
                } else {
                    try {
                        android.graphics.Typeface regularTypeface = android.graphics.Typeface.createFromAsset(getAssets(), "fonts/poppins_regular.ttf");
                        tvPaymentCount.setTypeface(regularTypeface);
                    } catch (Exception e) {
                        tvPaymentCount.setTypeface(null, android.graphics.Typeface.NORMAL);
                    }
                }
                LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                countParams.topMargin = (int) (2 * getResources().getDisplayMetrics().density);
                tvPaymentCount.setLayoutParams(countParams);
                revenueLayout.addView(tvPaymentCount);
                
                revenueRow.addView(revenueLayout);
                
                // Unpaid Revenue
                LinearLayout unpaidLayout = new LinearLayout(this);
                unpaidLayout.setOrientation(LinearLayout.VERTICAL);
                unpaidLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ));
                
                TextView tvUnpaidLabel = new TextView(this);
                tvUnpaidLabel.setText("Unpaid");
                tvUnpaidLabel.setTextSize(12f);
                tvUnpaidLabel.setTextColor(0xFF666666);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    tvUnpaidLabel.setTypeface(getResources().getFont(R.font.poppins_medium));
                } else {
                    try {
                        android.graphics.Typeface mediumTypeface = android.graphics.Typeface.createFromAsset(getAssets(), "fonts/poppins_medium.ttf");
                        tvUnpaidLabel.setTypeface(mediumTypeface);
                    } catch (Exception e) {
                        tvUnpaidLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
                    }
                }
                unpaidLayout.addView(tvUnpaidLabel);
                
                TextView tvUnpaidValue = new TextView(this);
                double unpaidAmount = bh.optDouble("unpaid_amount", 0.0);
                if (Double.isNaN(unpaidAmount) || Double.isInfinite(unpaidAmount)) {
                    unpaidAmount = 0.0;
                }
                tvUnpaidValue.setText(formatCurrency(unpaidAmount));
                tvUnpaidValue.setTextSize(18f);
                tvUnpaidValue.setTextColor(0xFFF44336);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    tvUnpaidValue.setTypeface(getResources().getFont(R.font.poppins_bold));
                } else {
                    try {
                        android.graphics.Typeface boldTypeface = android.graphics.Typeface.createFromAsset(getAssets(), "fonts/poppins_bold.ttf");
                        tvUnpaidValue.setTypeface(boldTypeface);
                    } catch (Exception e) {
                        tvUnpaidValue.setTypeface(null, android.graphics.Typeface.BOLD);
                    }
                }
                tvUnpaidValue.setLayoutParams(valueParams);
                unpaidLayout.addView(tvUnpaidValue);
                
                TextView tvUnpaidCount = new TextView(this);
                int unpaidCount = bh.optInt("unpaid_count", 0);
                tvUnpaidCount.setText(unpaidCount + (unpaidCount == 1 ? " payment" : " payments"));
                tvUnpaidCount.setTextSize(11f);
                tvUnpaidCount.setTextColor(0xFF666666);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    tvUnpaidCount.setTypeface(getResources().getFont(R.font.poppins_regular));
                } else {
                    try {
                        android.graphics.Typeface regularTypeface = android.graphics.Typeface.createFromAsset(getAssets(), "fonts/poppins_regular.ttf");
                        tvUnpaidCount.setTypeface(regularTypeface);
                    } catch (Exception e) {
                        tvUnpaidCount.setTypeface(null, android.graphics.Typeface.NORMAL);
                    }
                }
                tvUnpaidCount.setLayoutParams(countParams);
                unpaidLayout.addView(tvUnpaidCount);
                
                revenueRow.addView(unpaidLayout);
                
                cardContent.addView(revenueRow);
                card.addView(cardContent);
                layoutBoardingHouses.addView(card);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("AnalyticsActivity", "Error parsing boarding houses: " + e.getMessage());
        }
    }
    
    private void populateBoardingHousesOccupancy(JSONArray bhOccupancy) {
        if (layoutBhOccupancy == null) {
            return;
        }
        
        layoutBhOccupancy.removeAllViews();
        
        try {
            for (int i = 0; i < bhOccupancy.length(); i++) {
                JSONObject bh = bhOccupancy.getJSONObject(i);
                
                // Create card layout - Occupancy cards use orange background (matches occupancy value color)
                com.google.android.material.card.MaterialCardView card = new com.google.android.material.card.MaterialCardView(this);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                cardParams.setMargins(
                    (int) (4 * getResources().getDisplayMetrics().density),
                    (int) ((i > 0 ? 8 : 0) * getResources().getDisplayMetrics().density),
                    (int) (4 * getResources().getDisplayMetrics().density),
                    (int) (8 * getResources().getDisplayMetrics().density)
                );
                card.setLayoutParams(cardParams);
                card.setCardBackgroundColor(0xFFFFE0B2); // Light orange background (matches orange occupancy value)
                card.setCardElevation(4f);
                card.setRadius(8f);
                card.setStrokeWidth(0); // No border
                
                // Create inner layout
                LinearLayout cardContent = new LinearLayout(this);
                cardContent.setOrientation(LinearLayout.VERTICAL);
                cardContent.setPadding(
                    (int) (16 * getResources().getDisplayMetrics().density),
                    (int) (16 * getResources().getDisplayMetrics().density),
                    (int) (16 * getResources().getDisplayMetrics().density),
                    (int) (16 * getResources().getDisplayMetrics().density)
                );
                
                // BH Name
                TextView tvBhName = new TextView(this);
                tvBhName.setText(bh.optString("bh_name", "Unnamed"));
                tvBhName.setTextSize(15f);
                tvBhName.setTextColor(0xFF000000);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    tvBhName.setTypeface(getResources().getFont(R.font.poppins_semibold));
                } else {
                    try {
                        android.graphics.Typeface semiboldTypeface = android.graphics.Typeface.createFromAsset(getAssets(), "fonts/poppins_semibold.ttf");
                        tvBhName.setTypeface(semiboldTypeface);
                    } catch (Exception e) {
                        tvBhName.setTypeface(null, android.graphics.Typeface.BOLD);
                    }
                }
                cardContent.addView(tvBhName);
                
                // Occupancy Row
                LinearLayout occupancyRow = new LinearLayout(this);
                occupancyRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams occupancyParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                occupancyParams.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
                occupancyRow.setLayoutParams(occupancyParams);
                
                // Occupancy Rate
                LinearLayout rateLayout = new LinearLayout(this);
                rateLayout.setOrientation(LinearLayout.VERTICAL);
                rateLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ));
                
                TextView tvRateLabel = new TextView(this);
                tvRateLabel.setText("Occupancy Rate");
                tvRateLabel.setTextSize(12f);
                tvRateLabel.setTextColor(0xFF666666);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    tvRateLabel.setTypeface(getResources().getFont(R.font.poppins_medium));
                } else {
                    try {
                        android.graphics.Typeface mediumTypeface = android.graphics.Typeface.createFromAsset(getAssets(), "fonts/poppins_medium.ttf");
                        tvRateLabel.setTypeface(mediumTypeface);
                    } catch (Exception e) {
                        tvRateLabel.setTypeface(null, android.graphics.Typeface.NORMAL);
                    }
                }
                rateLayout.addView(tvRateLabel);
                
                TextView tvRateValue = new TextView(this);
                double occupancyRate = bh.optDouble("occupancy_rate", 0.0);
                if (Double.isNaN(occupancyRate) || Double.isInfinite(occupancyRate)) {
                    occupancyRate = 0.0;
                }
                tvRateValue.setText(String.format("%.1f%%", occupancyRate));
                tvRateValue.setTextSize(20f);
                tvRateValue.setTextColor(0xFFFF9800);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    tvRateValue.setTypeface(getResources().getFont(R.font.poppins_bold));
                } else {
                    try {
                        android.graphics.Typeface boldTypeface = android.graphics.Typeface.createFromAsset(getAssets(), "fonts/poppins_bold.ttf");
                        tvRateValue.setTypeface(boldTypeface);
                    } catch (Exception e) {
                        tvRateValue.setTypeface(null, android.graphics.Typeface.BOLD);
                    }
                }
                LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                valueParams.topMargin = (int) (2 * getResources().getDisplayMetrics().density);
                tvRateValue.setLayoutParams(valueParams);
                rateLayout.addView(tvRateValue);
                
                occupancyRow.addView(rateLayout);
                
                // Room counts
                LinearLayout roomsLayout = new LinearLayout(this);
                roomsLayout.setOrientation(LinearLayout.VERTICAL);
                roomsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ));
                roomsLayout.setGravity(android.view.Gravity.END);
                
                TextView tvOccupied = new TextView(this);
                int occupiedRooms = bh.optInt("occupied_rooms", 0);
                tvOccupied.setText(occupiedRooms + " occupied");
                tvOccupied.setTextSize(11f);
                tvOccupied.setTextColor(0xFF666666);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    tvOccupied.setTypeface(getResources().getFont(R.font.poppins_regular));
                } else {
                    try {
                        android.graphics.Typeface regularTypeface = android.graphics.Typeface.createFromAsset(getAssets(), "fonts/poppins_regular.ttf");
                        tvOccupied.setTypeface(regularTypeface);
                    } catch (Exception e) {
                        tvOccupied.setTypeface(null, android.graphics.Typeface.NORMAL);
                    }
                }
                roomsLayout.addView(tvOccupied);
                
                TextView tvAvailable = new TextView(this);
                int availableRooms = bh.optInt("available_rooms", 0);
                tvAvailable.setText(availableRooms + " available");
                tvAvailable.setTextSize(11f);
                tvAvailable.setTextColor(0xFF666666);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    tvAvailable.setTypeface(getResources().getFont(R.font.poppins_regular));
                } else {
                    try {
                        android.graphics.Typeface regularTypeface = android.graphics.Typeface.createFromAsset(getAssets(), "fonts/poppins_regular.ttf");
                        tvAvailable.setTypeface(regularTypeface);
                    } catch (Exception e) {
                        tvAvailable.setTypeface(null, android.graphics.Typeface.NORMAL);
                    }
                }
                LinearLayout.LayoutParams availableParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                availableParams.topMargin = (int) (2 * getResources().getDisplayMetrics().density);
                tvAvailable.setLayoutParams(availableParams);
                roomsLayout.addView(tvAvailable);
                
                occupancyRow.addView(roomsLayout);
                
                cardContent.addView(occupancyRow);
                card.addView(cardContent);
                layoutBhOccupancy.addView(card);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("AnalyticsActivity", "Error parsing boarding houses occupancy: " + e.getMessage());
        }
    }
    
    private void showEmptyChartMessage(com.github.mikephil.charting.charts.Chart chartView, String message) {
        if (chartView != null) {
            chartView.clear();
            chartView.setNoDataText(message);
            chartView.setNoDataTextColor(0xFF999999);
            chartView.setNoDataTextTypeface(android.graphics.Typeface.DEFAULT);
            chartView.invalidate();
        }
    }
    
    private void initializeCharts() {
        // Initialize Monthly Revenue Chart
        if (chartMonthlyRevenue != null) {
            chartMonthlyRevenue.getDescription().setEnabled(false);
            chartMonthlyRevenue.setTouchEnabled(true);
            chartMonthlyRevenue.setDragEnabled(true);
            chartMonthlyRevenue.setScaleEnabled(true);
            chartMonthlyRevenue.setPinchZoom(true);
            chartMonthlyRevenue.getLegend().setEnabled(true);
            chartMonthlyRevenue.getLegend().setTextSize(11f);
            chartMonthlyRevenue.getLegend().setTextColor(0xFF333333);
            chartMonthlyRevenue.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
            chartMonthlyRevenue.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
            
            XAxis xAxis = chartMonthlyRevenue.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setLabelRotationAngle(-45f);
            xAxis.setTextSize(11f);
            xAxis.setTextColor(0xFF333333);
            xAxis.setDrawGridLines(true);
            xAxis.setGridColor(0xFFE0E0E0);
            
            YAxis leftAxis = chartMonthlyRevenue.getAxisLeft();
            leftAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    // Show exact values on axis
                    if (value >= 1000) {
                        return String.format("%.0fK", value / 1000);
                    }
                    return String.format("%.0f", value);
                }
            });
            leftAxis.setTextSize(11f);
            leftAxis.setTextColor(0xFF333333);
            leftAxis.setDrawGridLines(true);
            leftAxis.setGridColor(0xFFE0E0E0);
            
            chartMonthlyRevenue.getAxisRight().setEnabled(false);
            
            // Enable highlights for click interactions
            chartMonthlyRevenue.setHighlightPerTapEnabled(true);
            chartMonthlyRevenue.setHighlightPerDragEnabled(false);
        }
        
        // Initialize Top Rooms Chart
        if (chartTopRooms != null) {
            chartTopRooms.getDescription().setEnabled(false);
            chartTopRooms.setTouchEnabled(true);
            chartTopRooms.setDragEnabled(false);
            chartTopRooms.setScaleEnabled(false);
            chartTopRooms.getLegend().setEnabled(false);
            
            XAxis xAxis = chartTopRooms.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawAxisLine(true);
            xAxis.setDrawGridLines(false);
            xAxis.setGranularity(1f);
            xAxis.setTextSize(10f);
            xAxis.setLabelRotationAngle(-45f);
            xAxis.setLabelCount(10, false);
            xAxis.setTextSize(11f);
            xAxis.setTextColor(0xFF333333);
            xAxis.setLabelRotationAngle(0f);
            
            YAxis leftAxis = chartTopRooms.getAxisLeft();
            leftAxis.setDrawAxisLine(true);
            leftAxis.setDrawGridLines(true);
            leftAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    // Show exact values on axis
                    if (value >= 1000) {
                        return String.format("%.0fK", value / 1000);
                    }
                    return String.format("%.0f", value);
                }
            });
            leftAxis.setTextSize(11f);
            leftAxis.setTextColor(0xFF333333);
            leftAxis.setGridColor(0xFFE0E0E0);
            
            chartTopRooms.getAxisRight().setEnabled(false);
            
            // Enable highlights for click interactions
            chartTopRooms.setHighlightPerTapEnabled(true);
        }
        
        // Initialize Payment Activity Chart
        if (chartPaymentActivity != null) {
            chartPaymentActivity.getDescription().setEnabled(false);
            chartPaymentActivity.setTouchEnabled(true);
            chartPaymentActivity.setDragEnabled(true);
            chartPaymentActivity.setScaleEnabled(true);
            chartPaymentActivity.setPinchZoom(true);
            chartPaymentActivity.getLegend().setEnabled(true);
            chartPaymentActivity.getLegend().setTextSize(11f);
            chartPaymentActivity.getLegend().setTextColor(0xFF333333);
            chartPaymentActivity.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
            chartPaymentActivity.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
            
            XAxis xAxis = chartPaymentActivity.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);
            xAxis.setLabelRotationAngle(-45f);
            xAxis.setTextSize(10f);
            xAxis.setTextColor(0xFF333333);
            xAxis.setDrawGridLines(true);
            xAxis.setGridColor(0xFFE0E0E0);
            
            YAxis leftAxis = chartPaymentActivity.getAxisLeft();
            leftAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    if (value >= 1000000) {
                        return "₱" + String.format("%.1fM", value / 1000000);
                    } else if (value >= 1000) {
                        return "₱" + String.format("%.0fK", value / 1000);
                    }
                    return "₱" + String.format("%.0f", value);
                }
            });
            leftAxis.setTextSize(11f);
            leftAxis.setTextColor(0xFF333333);
            leftAxis.setDrawGridLines(true);
            leftAxis.setGridColor(0xFFE0E0E0);
            
            YAxis rightAxis = chartPaymentActivity.getAxisRight();
            rightAxis.setEnabled(true);
            rightAxis.setTextSize(11f);
            rightAxis.setTextColor(0xFFFF9800);
            rightAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.format("%.0f", value / 100);
                }
            });
            rightAxis.setDrawGridLines(false);
            
            // Enable highlights for click interactions
            chartPaymentActivity.setHighlightPerTapEnabled(true);
            chartPaymentActivity.setHighlightPerDragEnabled(false);
        }
    }
    
    private void populateMonthlyRevenueChart(JSONArray monthlyData) {
        if (monthlyData == null || monthlyData.length() == 0) {
            return;
        }
        
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        try {
            for (int i = 0; i < monthlyData.length(); i++) {
                JSONObject item = monthlyData.getJSONObject(i);
                float revenue = (float) item.getDouble("revenue");
                String monthLabel = item.getString("month_label");
                
                entries.add(new Entry(i, revenue));
                labels.add(monthLabel);
            }
            
            LineDataSet dataSet = new LineDataSet(entries, "Revenue");
            dataSet.setColor(0xFF2196F3);
            dataSet.setValueTextColor(0xFF333333);
            dataSet.setValueTextSize(10f);
            dataSet.setLineWidth(2.5f);
            dataSet.setCircleColor(0xFF2196F3);
            dataSet.setCircleRadius(5f);
            dataSet.setCircleHoleColor(0xFFFFFFFF);
            dataSet.setCircleHoleRadius(2.5f);
            dataSet.setDrawCircleHole(true);
            dataSet.setFillColor(0xFF2196F3);
            dataSet.setFillAlpha(60);
            dataSet.setDrawFilled(true);
            dataSet.setDrawValues(false);
            // Format values to show exact currency amount when clicked
            dataSet.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return formatCurrency(value);
                }
            });
            
            // Create custom marker for exact values on click
            ChartMarker marker = new ChartMarker(this, labels, true); // true = is currency
            chartMonthlyRevenue.setMarker(marker);
            
            LineData lineData = new LineData(dataSet);
            chartMonthlyRevenue.setData(lineData);
            chartMonthlyRevenue.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            chartMonthlyRevenue.invalidate();
            chartMonthlyRevenue.post(() -> {
                chartMonthlyRevenue.notifyDataSetChanged();
                chartMonthlyRevenue.invalidate();
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    private void populateTopRoomsChart(JSONArray topRoomsData) {
        if (topRoomsData == null || topRoomsData.length() == 0) {
            return;
        }
        
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        try {
            for (int i = topRoomsData.length() - 1; i >= 0; i--) {
                JSONObject item = topRoomsData.getJSONObject(i);
                float revenue = (float) item.getDouble("total_revenue");
                String roomLabel = item.optString("room_label", "");
                String bhName = item.optString("bh_name", "");
                String roomNumber = item.optString("room_number", "");
                
                // Create shorter label: "BH Name - R#"
                if (!bhName.isEmpty() && !roomNumber.isEmpty()) {
                    // Limit BH name to 12 chars max
                    String shortBhName = bhName.length() > 12 ? bhName.substring(0, 12) + ".." : bhName;
                    roomLabel = shortBhName + " - R" + roomNumber;
                } else if (roomLabel.length() > 20) {
                    // Fallback: just truncate if we can't parse
                    roomLabel = roomLabel.substring(0, 17) + "...";
                }
                
                entries.add(new BarEntry(topRoomsData.length() - 1 - i, revenue));
                labels.add(roomLabel);
            }
            
            BarDataSet dataSet = new BarDataSet(entries, "");
            dataSet.setColor(0xFF4CAF50);
            dataSet.setValueTextColor(0xFF333333);
            dataSet.setValueTextSize(10f);
            dataSet.setDrawValues(false);
            // Format values to show exact currency amount when clicked
            dataSet.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return formatCurrency(value);
                }
            });
            
            // Create custom marker for exact values on click
            ChartMarker marker = new ChartMarker(this, labels, true); // true = is currency
            chartTopRooms.setMarker(marker);
            
            BarData barData = new BarData(dataSet);
            barData.setBarWidth(0.5f);
            chartTopRooms.setData(barData);
            chartTopRooms.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            chartTopRooms.invalidate();
            chartTopRooms.post(() -> {
                chartTopRooms.notifyDataSetChanged();
                chartTopRooms.invalidate();
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    private void populatePaymentActivityChart(JSONArray activityData) {
        if (activityData == null || activityData.length() == 0) {
            return;
        }
        
        List<Entry> revenueEntries = new ArrayList<>();
        List<Entry> countEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        try {
            for (int i = 0; i < activityData.length(); i++) {
                JSONObject item = activityData.getJSONObject(i);
                float revenue = (float) item.getDouble("revenue");
                int count = item.getInt("payment_count");
                String dayLabel = item.getString("day_label");
                
                revenueEntries.add(new Entry(i, revenue));
                countEntries.add(new Entry(i, count * 100));
                labels.add(dayLabel);
            }
            
            // Store original values for marker display
            final float[] originalRevenues = new float[revenueEntries.size()];
            final int[] originalCounts = new int[countEntries.size()];
            for (int i = 0; i < revenueEntries.size(); i++) {
                originalRevenues[i] = revenueEntries.get(i).getY();
                originalCounts[i] = (int) (countEntries.get(i).getY() / 100);
            }
            
            LineDataSet revenueDataSet = new LineDataSet(revenueEntries, "Revenue");
            revenueDataSet.setColor(0xFF2196F3);
            revenueDataSet.setValueTextColor(0xFF333333);
            revenueDataSet.setValueTextSize(10f);
            revenueDataSet.setLineWidth(2.5f);
            revenueDataSet.setCircleColor(0xFF2196F3);
            revenueDataSet.setCircleRadius(4f);
            revenueDataSet.setCircleHoleColor(0xFFFFFFFF);
            revenueDataSet.setCircleHoleRadius(2f);
            revenueDataSet.setDrawCircleHole(true);
            revenueDataSet.setFillColor(0xFF2196F3);
            revenueDataSet.setFillAlpha(60);
            revenueDataSet.setDrawFilled(true);
            revenueDataSet.setDrawValues(false);
            revenueDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
            // Format values to show exact currency amount when clicked
            revenueDataSet.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return formatCurrency(value);
                }
            });
            
            LineDataSet countDataSet = new LineDataSet(countEntries, "Count");
            countDataSet.setColor(0xFFFF9800);
            countDataSet.setValueTextColor(0xFF333333);
            countDataSet.setValueTextSize(10f);
            countDataSet.setLineWidth(2.5f);
            countDataSet.setCircleColor(0xFFFF9800);
            countDataSet.setCircleRadius(4f);
            countDataSet.setCircleHoleColor(0xFFFFFFFF);
            countDataSet.setCircleHoleRadius(2f);
            countDataSet.setDrawCircleHole(true);
            countDataSet.setDrawValues(false);
            countDataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
            // Format count values to show exact number (not currency)
            countDataSet.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    // value is multiplied by 100, so divide to get original count
                    int count = (int) (value / 100);
                    return String.valueOf(count);
                }
            });
            
            // Create custom marker for exact values on click (supports both revenue and count)
            PaymentActivityMarker marker = new PaymentActivityMarker(this, labels, originalRevenues, originalCounts);
            chartPaymentActivity.setMarker(marker);
            
            List<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(revenueDataSet);
            dataSets.add(countDataSet);
            
            LineData lineData = new LineData(dataSets);
            chartPaymentActivity.setData(lineData);
            chartPaymentActivity.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            chartPaymentActivity.invalidate();
            chartPaymentActivity.post(() -> {
                chartPaymentActivity.notifyDataSetChanged();
                chartPaymentActivity.invalidate();
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    private void toggleRevenueSection() {
        isRevenueExpanded = !isRevenueExpanded;
        if (layoutBoardingHouses != null && ivExpandRevenue != null) {
            if (isRevenueExpanded) {
                layoutBoardingHouses.setVisibility(android.view.View.VISIBLE);
                ivExpandRevenue.animate().rotation(180f).setDuration(200).start();
            } else {
                layoutBoardingHouses.setVisibility(android.view.View.GONE);
                ivExpandRevenue.animate().rotation(0f).setDuration(200).start();
            }
        }
    }
    
    private void toggleOccupancySection() {
        isOccupancyExpanded = !isOccupancyExpanded;
        if (layoutBhOccupancy != null && ivExpandOccupancy != null) {
            if (isOccupancyExpanded) {
                layoutBhOccupancy.setVisibility(android.view.View.VISIBLE);
                ivExpandOccupancy.animate().rotation(180f).setDuration(200).start();
            } else {
                layoutBhOccupancy.setVisibility(android.view.View.GONE);
                ivExpandOccupancy.animate().rotation(0f).setDuration(200).start();
            }
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideProgressDialog();
    }
    
    // Custom Marker for charts that show currency values
    private static class ChartMarker extends MarkerView {
        private TextView tvContent;
        private List<String> labels;
        private boolean isCurrency;
        
        public ChartMarker(Context context, List<String> labels, boolean isCurrency) {
            super(context, R.layout.chart_marker);
            this.labels = labels;
            this.isCurrency = isCurrency;
        }
        
        @Override
        public void refreshContent(Entry e, Highlight highlight) {
            if (tvContent == null) {
                tvContent = findViewById(R.id.tvContent);
            }
            
            float value = e.getY();
            
            String formattedValue;
            
            if (isCurrency) {
                // Format as exact currency (no abbreviations like 4.33K)
                java.text.DecimalFormat formatter = new java.text.DecimalFormat("₱#,##0.00");
                formattedValue = formatter.format(value);
            } else {
                // Format as exact number
                formattedValue = String.format("%.0f", value);
            }
            
            if (tvContent != null) {
                tvContent.setText(formattedValue);
            }
            super.refreshContent(e, highlight);
        }
        
        @Override
        public MPPointF getOffset() {
            return new MPPointF(-(getWidth() / 2), -getHeight());
        }
        
        @Override
        public MPPointF getOffsetForDrawingAtPoint(float posX, float posY) {
            MPPointF offset = getOffset();
            MPPointF offset2 = new MPPointF();
            
            offset2.x = offset.x;
            offset2.y = offset.y;
            
            com.github.mikephil.charting.charts.Chart chart = getChartView();
            if (chart == null) {
                return offset2;
            }
            
            float width = getWidth();
            float height = getHeight();
            
            // Adjust horizontal position to stay within chart bounds
            if (posX + offset2.x < 0) {
                // Too far left - align to left edge
                offset2.x = -posX;
            } else if (posX + width + offset2.x > chart.getWidth()) {
                // Too far right - align to right edge
                offset2.x = chart.getWidth() - posX - width;
            }
            
            // Adjust vertical position to stay within chart bounds
            if (posY + offset2.y < 0) {
                // Too far up - show below instead
                offset2.y = 10;
            } else if (posY + height + offset2.y > chart.getHeight()) {
                // Too far down - show above instead
                offset2.y = -height - 10;
            }
            
            return offset2;
        }
    }
    
    // Custom Marker for Payment Activity chart (shows both revenue and count)
    private static class PaymentActivityMarker extends MarkerView {
        private TextView tvContent;
        private List<String> labels;
        private float[] originalRevenues;
        private int[] originalCounts;
        
        public PaymentActivityMarker(Context context, List<String> labels, float[] originalRevenues, int[] originalCounts) {
            super(context, R.layout.chart_marker);
            this.labels = labels;
            this.originalRevenues = originalRevenues;
            this.originalCounts = originalCounts;
        }
        
        @Override
        public void refreshContent(Entry e, Highlight highlight) {
            if (tvContent == null) {
                tvContent = findViewById(R.id.tvContent);
            }
            
            int index = (int) e.getX();
            int dataSetIndex = highlight.getDataSetIndex();
            
            String formattedValue;
            
            // Check which dataset was clicked (0 = revenue, 1 = count)
            if (dataSetIndex == 0 && index < originalRevenues.length) {
                // Revenue dataset - show as exact currency (no abbreviations)
                float revenue = originalRevenues[index];
                java.text.DecimalFormat formatter = new java.text.DecimalFormat("₱#,##0.00");
                formattedValue = formatter.format(revenue);
            } else if (dataSetIndex == 1 && index < originalCounts.length) {
                // Count dataset - show as exact number (not currency)
                int count = originalCounts[index];
                formattedValue = String.valueOf(count);
            } else {
                formattedValue = "";
            }
            
            if (tvContent != null) {
                tvContent.setText(formattedValue);
            }
            super.refreshContent(e, highlight);
        }
        
        @Override
        public MPPointF getOffset() {
            return new MPPointF(-(getWidth() / 2), -getHeight());
        }
        
        @Override
        public MPPointF getOffsetForDrawingAtPoint(float posX, float posY) {
            MPPointF offset = getOffset();
            MPPointF offset2 = new MPPointF();
            
            offset2.x = offset.x;
            offset2.y = offset.y;
            
            com.github.mikephil.charting.charts.Chart chart = getChartView();
            if (chart == null) {
                return offset2;
            }
            
            float width = getWidth();
            float height = getHeight();
            
            // Adjust horizontal position to stay within chart bounds
            if (posX + offset2.x < 0) {
                // Too far left - align to left edge
                offset2.x = -posX;
            } else if (posX + width + offset2.x > chart.getWidth()) {
                // Too far right - align to right edge
                offset2.x = chart.getWidth() - posX - width;
            }
            
            // Adjust vertical position to stay within chart bounds
            if (posY + offset2.y < 0) {
                // Too far up - show below instead
                offset2.y = 10;
            } else if (posY + height + offset2.y > chart.getHeight()) {
                // Too far down - show above instead
                offset2.y = -height - 10;
            }
            
            return offset2;
        }
    }
}

