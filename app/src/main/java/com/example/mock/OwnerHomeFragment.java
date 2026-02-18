package com.example.mock;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import android.content.Context;


public class OwnerHomeFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private int userId = -1; // default to -1

    // UI elements
    private TextView tvOwnerName, tvListingsCount, tvBoardersCount, tvViewsCount, tvPopularTitle, tvPopularVisits, tvTotalRevenue;
    private ImageView imgPopularListing, ivNotification, ivMessage;
    private View badgeMsg, badgeNotif;
    private TextView badgeCount, badgeNotifCount;
    private LinearLayout numofListings, layoutTotalBoarders;
    private com.github.mikephil.charting.charts.LineChart chartMonthlyRevenue;
    private com.google.android.material.card.MaterialCardView cardVerificationStatus;
    private TextView tvStatusTitle, tvStatusMessage;
    private View viewStatusAccent;
    private ImageView ivStatusIcon;
    
    // Store popular listing data for navigation
    private int popularListingBhId = -1;
    
    // Broadcast receiver for badge updates
    private BroadcastReceiver badgeUpdateReceiver;
    
    // Periodic notification check
    private android.os.Handler notificationCheckHandler;
    private Runnable notificationCheckRunnable;
    
    // Periodic message check
    private android.os.Handler messageCheckHandler;
    private Runnable messageCheckRunnable;
    
    // Flag to track if data has been loaded (to prevent reloading on navigation)
    private boolean dataLoaded = false;
    
    // SwipeRefreshLayout for pull-to-refresh
    private SwipeRefreshLayout swipeRefreshLayout;

    public OwnerHomeFragment() {
        // Required empty public constructor
    }

    public static OwnerHomeFragment newInstance(int userId) {
        OwnerHomeFragment fragment = new OwnerHomeFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getInt(ARG_USER_ID, -1);
        }
        // Restore dataLoaded flag if fragment was recreated
        if (savedInstanceState != null) {
            dataLoaded = savedInstanceState.getBoolean("dataLoaded", false);
        }
    }
    
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save dataLoaded flag to prevent reloading after recreation
        outState.putBoolean("dataLoaded", dataLoaded);
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_owner_home, container, false);

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Refresh dashboard data when pulled
            if (userId != -1) {
                fetchOwnerDashboardData();
                // Also refresh badge counts
                checkUnreadMessages();
                checkUnreadNotifications();
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        // Always bind views (onCreateView provides a fresh view instance)
        // Bind views
            tvOwnerName = view.findViewById(R.id.tvOwnerName);
            
            // Load name from session immediately
            android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
            String firstName = prefs.getString("user_first_name", "Owner");
        if (tvOwnerName != null) tvOwnerName.setText("Hello, " + firstName + "!");
            
            tvListingsCount = view.findViewById(R.id.tvListingsCount);
            tvBoardersCount = view.findViewById(R.id.tvBoardersCount);
            tvViewsCount = view.findViewById(R.id.tvViewsCount);
            tvPopularTitle = view.findViewById(R.id.tvPopularTitle);
            tvPopularVisits = view.findViewById(R.id.tvPopularVisits);
            imgPopularListing = view.findViewById(R.id.imgPopularListing);
            numofListings = view.findViewById(R.id.noofListings);
            layoutTotalBoarders = view.findViewById(R.id.layoutTotalBoarders);
            tvTotalRevenue = view.findViewById(R.id.tvTotalRevenue);
            chartMonthlyRevenue = view.findViewById(R.id.chartMonthlyRevenue);
            ivNotification = view.findViewById(R.id.ivNotification);
            ivMessage = view.findViewById(R.id.ivMessage);
            badgeMsg = view.findViewById(R.id.badgeMsg);
            badgeNotif = view.findViewById(R.id.badgeNotif);
            cardVerificationStatus = view.findViewById(R.id.cardVerificationStatus);
            tvStatusTitle = view.findViewById(R.id.tvStatusTitle);
            tvStatusMessage = view.findViewById(R.id.tvStatusMessage);
            viewStatusAccent = view.findViewById(R.id.viewStatusAccent);
            ivStatusIcon = view.findViewById(R.id.ivStatusIcon);
            
            checkUserStatus();
            
            // Initialize chart
            if (chartMonthlyRevenue != null) {
                initializeMonthlyRevenueChart();
            }
            
            // Revenue analytics views
            
            // Create a TextView for message badge count if it doesn't exist
            if (badgeMsg != null) {
            badgeCount = new TextView(getContext());
            
            // Create FrameLayout.LayoutParams for proper positioning (same as notification badge)
            FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, 
                FrameLayout.LayoutParams.WRAP_CONTENT);
            badgeParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
            badgeParams.topMargin = 6; // Same as notification badge
            badgeParams.rightMargin = 6; // Same as notification badge
            badgeCount.setLayoutParams(badgeParams);
            
            // Enhanced badge styling
            badgeCount.setBackground(getResources().getDrawable(R.drawable.red_dot));
            badgeCount.setTextColor(getResources().getColor(android.R.color.white));
            badgeCount.setTextSize(11);
            badgeCount.setTypeface(null, android.graphics.Typeface.BOLD);
            badgeCount.setPadding(8, 4, 8, 4);
            badgeCount.setMinWidth(24);
            badgeCount.setMinHeight(24);
            badgeCount.setGravity(android.view.Gravity.CENTER);
            badgeCount.setVisibility(View.GONE);
            
            // Add elevation for better visual separation
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                badgeCount.setElevation(4f);
            }
            
            // Add the badge count to the message icon's parent FrameLayout
            if (ivMessage.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) ivMessage.getParent();
                parent.addView(badgeCount);
                android.util.Log.d("MessageBadge", "Added badgeCount to parent ViewGroup");
                android.util.Log.d("MessageBadge", "Parent ViewGroup: " + parent.getClass().getSimpleName());
                android.util.Log.d("MessageBadge", "Parent child count: " + parent.getChildCount());
            }
        
        // Create a TextView for notification badge count if it doesn't exist
        android.util.Log.d("NotificationBadge", "Initializing notification badge, badgeNotif is null: " + (badgeNotif == null));
        if (badgeNotif != null) {
            badgeNotifCount = new TextView(getContext());
            android.util.Log.d("NotificationBadge", "Created badgeNotifCount TextView");
            
            // Create FrameLayout.LayoutParams for proper positioning (same as badgeNotif)
            FrameLayout.LayoutParams notifBadgeParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, 
                FrameLayout.LayoutParams.WRAP_CONTENT
            );
            notifBadgeParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
            notifBadgeParams.topMargin = 6; // Same as badgeNotif
            notifBadgeParams.rightMargin = 6; // Same as badgeNotif
            badgeNotifCount.setLayoutParams(notifBadgeParams);
            
            // Enhanced badge styling
            badgeNotifCount.setBackground(getResources().getDrawable(R.drawable.red_dot));
            badgeNotifCount.setTextColor(getResources().getColor(android.R.color.white));
            badgeNotifCount.setTextSize(11);
            badgeNotifCount.setTypeface(null, android.graphics.Typeface.BOLD);
            badgeNotifCount.setPadding(8, 4, 8, 4);
            badgeNotifCount.setMinWidth(24);
            badgeNotifCount.setMinHeight(24);
            badgeNotifCount.setGravity(android.view.Gravity.CENTER);
            badgeNotifCount.setVisibility(View.GONE);
            
            // Add elevation for better visibility
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                badgeNotifCount.setElevation(4f);
            }
            
            // Add the badge count to the notification icon's parent FrameLayout
            if (ivNotification.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) ivNotification.getParent();
                parent.addView(badgeNotifCount);
                android.util.Log.d("NotificationBadge", "Added badgeNotifCount to parent ViewGroup");
                android.util.Log.d("NotificationBadge", "Parent ViewGroup: " + parent.getClass().getSimpleName());
                android.util.Log.d("NotificationBadge", "Parent child count: " + parent.getChildCount());
            } else {
                android.util.Log.d("NotificationBadge", "ivNotification parent is not a ViewGroup: " + ivNotification.getParent().getClass().getSimpleName());
            }
            }
        }

        // Setup click listeners (always, regardless of whether views were just initialized)
        ivNotification.setOnClickListener(v -> {
            if (getContext() != null) {
                if (Login.checkFeatureAccess(getContext())) {
                    // Hide badge when opening notifications
                    hideNotificationBadge();
                    Intent intent = new Intent(getContext(), Notification.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(getContext(), "Account Verification Pending", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ivMessage.setOnClickListener(v -> {
            if (getContext() != null) {
                if (Login.checkFeatureAccess(getContext())) {
                    // Don't hide badge here - only hide when opening actual conversation
                    Intent intent = new Intent(getContext(), Messages.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(getContext(), "Account Verification Pending", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Check for unread messages and show badge
        checkUnreadMessages();
        
        // Check for unread notifications and show badge
        checkUnreadNotifications();
        
        // Setup broadcast receiver for badge updates
        setupBadgeUpdateReceiver();

        // Transactions & Logs button click listener
        view.findViewById(R.id.cardTransactionsLogs).setOnClickListener(v -> {
            if (getContext() != null) {
                Intent intent = new Intent(getContext(), TransactionsLogsActivity.class);
                intent.putExtra("user_id", userId);
                startActivity(intent);
            }
        });


        // Click listener for LinearLayout to navigate to ManageFragment
        numofListings.setOnClickListener(v -> {
            // Navigate using MainActivity's switchToTab method if available
            if (getActivity() instanceof MainActivity) {
                // Use the bottom navigation to switch (fragment is already cached)
                BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_manage);
                }
            } else {
                // Fallback: use fragment transaction
                ManageFragment manageFragment = ManageFragment.newInstance(userId);
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, manageFragment)
                            .addToBackStack(null)
                            .commit();

                    // Update BottomNavigationView selection
                    BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.nav_manage);
                    }
                }
            }
        });

        // Click listener for Total Boarders to navigate to BoardersListActivity
        layoutTotalBoarders.setOnClickListener(v -> {
            if (getContext() != null) {
                Intent intent = new Intent(getContext(), BoardersListActivity.class);
                intent.putExtra("user_id", userId);
                startActivity(intent);
            }
        });

        // Click listener for Today's Bookings card
        view.findViewById(R.id.cardTodayBookings).setOnClickListener(v -> {
            if (getContext() != null) {
                Intent intent = new Intent(getContext(), TodayBookingsActivity.class);
                intent.putExtra("user_id", userId);
                startActivity(intent);
            }
        });

        // Click listener for Popular Listing card
        view.findViewById(R.id.cardPopularListing).setOnClickListener(v -> {
            if (getContext() != null && popularListingBhId != -1) {
                Intent intent = new Intent(getContext(), PopularListingBookingsActivity.class);
                intent.putExtra("user_id", userId);
                intent.putExtra("bh_id", popularListingBhId);
                startActivity(intent);
            } else if (popularListingBhId == -1) {
                Toast.makeText(getContext(), "No popular listing available", Toast.LENGTH_SHORT).show();
            }
        });

        // Only load data if it hasn't been loaded yet (first time only)
        if (userId != -1 && !dataLoaded) {
            fetchOwnerDashboardData();
        } else if (userId == -1) {
            Toast.makeText(getContext(), "User not logged in!", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private android.os.Handler pollingHandler = new android.os.Handler();
    private Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            // Background update
            fetchOwnerDashboardDataInternal(false);
            // Also refresh badge counts
            checkUnreadMessages();
            checkUnreadNotifications();
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
        
        // Check for unread messages and show badge
        checkUnreadMessages();
        
        // Check for unread notifications and show badge
        checkUnreadNotifications();
        
        startPolling();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        stopPolling();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            // Refresh data when fragment becomes visible (e.g. from tab navigation)
            Log.d("OwnerHomeFragment", "Fragment shown, refreshing data...");
            fetchOwnerDashboardData();
            checkUnreadMessages();
            checkUnreadNotifications();
            checkUserStatus();
        }
    }

    private void fetchOwnerDashboardData() {
        // Manual call (first time or swipe refresh)
        fetchOwnerDashboardDataInternal(true);
    }

    private void fetchOwnerDashboardDataInternal(boolean showLoading) {
        if (userId == -1) {
            if (showLoading) Log.e("OwnerHomeFragment", "Cannot fetch dashboard data: User ID is -1");
            return;
        }

        String url = "https://boardease.calapebohol.com/get_owner_dashboard.php";
        if (showLoading) {
            Log.d("OwnerHomeFragment", "=== FETCHING OWNER DASHBOARD DATA ===");
        }

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    if (showLoading) {
                        Log.d("OwnerHomeFragment", "=== SERVER RESPONSE RECEIVED ===");
                    }
                    
                    if (getContext() == null) return;

                    if (response == null || response.trim().isEmpty()) {
                        if (showLoading) {
                            Toast.makeText(getContext(), "Empty response from server", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    try {
                        JSONObject obj = new JSONObject(response);
                        
                        // Validate JSON content
                        if (!obj.has("owner_name")) {
                             if (showLoading) {
                                Log.e("OwnerHomeFragment", "ERROR: Missing owner_name in response");
                                Toast.makeText(getContext(), "Invalid server response", Toast.LENGTH_SHORT).show();
                             }
                            return;
                        }
                        
                        // Owner name
                        String ownerName = obj.optString("owner_name", "Owner");
                        if (tvOwnerName != null) tvOwnerName.setText(ownerName);

                        // Listings count
                        int listingsCount = obj.optInt("listings_count", 0);
                        if (tvListingsCount != null) tvListingsCount.setText(String.valueOf(listingsCount));

                        // Boarders count
                        int boardersCount = obj.optInt("boarders_count", 0);
                        if (tvBoardersCount != null) tvBoardersCount.setText(String.valueOf(boardersCount));

                        // Views count
                        int viewsCount = obj.optInt("views_count", 0);
                        if (tvViewsCount != null) tvViewsCount.setText(String.valueOf(viewsCount));
                        
                        // Revenue data - Total Revenue
                        JSONObject revenue = obj.optJSONObject("revenue");
                        if (tvTotalRevenue != null) {
                            if (revenue != null) {
                                double totalRevenue = revenue.optDouble("total_revenue", 0.0);
                                if (Double.isNaN(totalRevenue) || Double.isInfinite(totalRevenue)) {
                                    totalRevenue = 0.0;
                                }
                                tvTotalRevenue.setText(formatCurrency(totalRevenue));
                            } else {
                                tvTotalRevenue.setText("₱0.00");
                            }
                        }
                        
                        // Only update heavy chart data if it's a full reload OR if we really want real-time charts
                        // For polling, maybe skip chart updates to save redraws unless necessary?
                        // Let's safe-guard chart updates
                        if (chartMonthlyRevenue != null) {
                            JSONObject charts = obj.optJSONObject("charts");
                            if (charts != null) {
                                JSONArray monthlyData = charts.optJSONArray("monthly_revenue");
                                if (monthlyData != null && monthlyData.length() > 0) {
                                    populateMonthlyRevenueChart(monthlyData);
                                }
                            }
                        }

                        // Popular listing update
                        JSONObject popular = obj.optJSONObject("popular_listing");
                        if (popular != null) {
                            popularListingBhId = popular.optInt("bh_id", -1);
                            if (tvPopularTitle != null) tvPopularTitle.setText(popular.optString("bh_name", "No Listing"));
                            if (tvPopularVisits != null) tvPopularVisits.setText(popular.optInt("visits", 0) + " bookings");

                            String imageUrl = popular.optString("image_path", "");
                            if (imgPopularListing != null) {
                                if (!imageUrl.isEmpty() && imageUrl.trim().length() > 0) {
                                    Glide.with(getContext())
                                            .load(imageUrl)
                                            .placeholder(R.drawable.sample_listing)
                                            .error(R.drawable.sample_listing)
                                            .fallback(R.drawable.sample_listing)
                                            .into(imgPopularListing);
                                } else {
                                    imgPopularListing.setImageResource(R.drawable.sample_listing);
                                }
                            }
                        } else {
                            popularListingBhId = -1;
                            if (tvPopularTitle != null) tvPopularTitle.setText("No Popular Listing");
                            if (tvPopularVisits != null) tvPopularVisits.setText("0 bookings");
                            if (imgPopularListing != null) imgPopularListing.setImageResource(R.drawable.sample_listing);
                        }
                        
                        // Mark data as loaded after successful parsing
                        dataLoaded = true;

                    } catch (JSONException e) {
                        e.printStackTrace();
                        if (showLoading) {
                            Toast.makeText(getContext(), "JSON Parsing error", Toast.LENGTH_SHORT).show();
                        }
                    } finally {
                        // Stop refresh indicator
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }
                },
                error -> {
                    if (showLoading) {
                        Log.e("OwnerHomeFragment", "=== VOLLEY ERROR ===");
                         Toast.makeText(getContext(), "Error fetching dashboard: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    // Stop refresh indicator on error
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                // Add email for robust lookup
                if (getContext() != null) {
                    android.content.SharedPreferences prefs = getContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
                    String email = prefs.getString("user_email", "");
                    if (!email.isEmpty()) {
                        params.put("email", email);
                    }
                }
                return params;
            }
        };

        Volley.newRequestQueue(getContext()).add(request);
    }
    
    private String formatCurrency(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount == 0) {
            return "₱0.00";
        }
        java.text.DecimalFormat formatter = new java.text.DecimalFormat("₱#,##0.00");
        return formatter.format(amount);
    }
    
    private void initializeMonthlyRevenueChart() {
        if (chartMonthlyRevenue == null) return;
        
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
        chartMonthlyRevenue.setHighlightPerTapEnabled(true);
    }
    
    private void populateMonthlyRevenueChart(JSONArray monthlyData) {
        if (chartMonthlyRevenue == null || monthlyData == null || monthlyData.length() == 0) {
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
            ChartMarker marker = new ChartMarker(getContext(), labels, true); // true = is currency
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
            Log.e("OwnerHomeFragment", "Error parsing monthly revenue data: " + e.getMessage());
        }
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
                // Format as exact currency (no abbreviations)
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

    private void checkUnreadMessages() {
        android.util.Log.d("MessageBadge", "Checking unread messages for user: " + userId);
        
        String url = "https://boardease.calapebohol.com/get_unread_count.php?user_id=" + userId;
        
        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.GET, url, null,
            response -> {
                try {
                    android.util.Log.d("MessageBadge", "API Response: " + response.toString());
                    
                    if (response.getBoolean("success")) {
                        JSONObject data = response.getJSONObject("data");
                        int totalUnread = data.getInt("total_unread");
                        android.util.Log.d("MessageBadge", "Unread count: " + totalUnread);
                        showMessageBadge(totalUnread);
                    } else {
                        android.util.Log.e("MessageBadge", "API Error: " + response.getString("message"));
                        hideMessageBadge();
                    }
                } catch (JSONException e) {
                    android.util.Log.e("MessageBadge", "JSON parsing error", e);
                    e.printStackTrace();
                    hideMessageBadge();
                }
            },
            error -> {
                android.util.Log.e("MessageBadge", "API request error", error);
                error.printStackTrace();
                hideMessageBadge();
            }
        );
        
        // Add request to queue
        if (getContext() != null) {
            RequestQueue queue = Volley.newRequestQueue(getContext());
            queue.add(request);
        }
    }

    private void showMessageBadge(int count) {
        android.util.Log.d("MessageBadge", "showMessageBadge called with count: " + count);
        android.util.Log.d("MessageBadge", "badgeMsg is null: " + (badgeMsg == null));
        android.util.Log.d("MessageBadge", "badgeCount is null: " + (badgeCount == null));
        
        if (count > 0) {
            // Hide the simple red dot
            if (badgeMsg != null) {
                badgeMsg.setVisibility(View.GONE);
                android.util.Log.d("MessageBadge", "Hiding simple red dot");
            }
            
            // Show the count badge
            if (badgeCount != null) {
                android.util.Log.d("MessageBadge", "Setting up count badge");
                // Format the count display
                String displayText;
                if (count > 99) {
                    displayText = "99+";
                } else {
                    displayText = String.valueOf(count);
                }
                
                android.util.Log.d("MessageBadge", "Display text: " + displayText);
                badgeCount.setText(displayText);
                
                // Reset any previous animation state
                badgeCount.clearAnimation();
                badgeCount.setAlpha(1f);
                badgeCount.setScaleX(1f);
                badgeCount.setScaleY(1f);
                badgeCount.setVisibility(View.VISIBLE);
                android.util.Log.d("MessageBadge", "Badge is now VISIBLE");
                android.util.Log.d("MessageBadge", "Badge parent: " + (badgeCount.getParent() != null ? badgeCount.getParent().getClass().getSimpleName() : "null"));
                android.util.Log.d("MessageBadge", "Badge visibility: " + badgeCount.getVisibility());
                android.util.Log.d("MessageBadge", "Badge text: " + badgeCount.getText());
                
                // Check layout parameters
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) badgeCount.getLayoutParams();
                if (params != null) {
                    android.util.Log.d("MessageBadge", "Badge layout params - gravity: " + params.gravity + ", topMargin: " + params.topMargin + ", rightMargin: " + params.rightMargin);
                } else {
                    android.util.Log.d("MessageBadge", "Badge layout params is null!");
                }
                
                // Add scale animation
                badgeCount.setScaleX(0f);
                badgeCount.setScaleY(0f);
                badgeCount.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start();
                android.util.Log.d("MessageBadge", "Count badge animation started");
            } else {
                android.util.Log.d("MessageBadge", "badgeCount is null, cannot show count badge");
            }
        } else {
            hideMessageBadge();
        }
    }

    private void hideMessageBadge() {
        android.util.Log.d("MessageBadge", "hideMessageBadge called");
        if (badgeMsg != null) {
            badgeMsg.setVisibility(View.GONE);
        }
        if (badgeCount != null) {
            android.util.Log.d("MessageBadge", "Hiding count badge with animation");
            // Clear any existing animation first
            badgeCount.clearAnimation();
            // Add fade out animation
            badgeCount.animate()
                .alpha(0f)
                .scaleX(0f)
                .scaleY(0f)
                .setDuration(150)
                .withEndAction(() -> {
                    badgeCount.setVisibility(View.GONE);
                    android.util.Log.d("MessageBadge", "Count badge hidden and set to GONE");
                })
                .start();
        }
    }

    private void checkUnreadNotifications() {
        if (userId == -1) {
            android.util.Log.d("NotificationBadge", "User ID is -1, skipping notification check");
            return;
        }
        
        android.util.Log.d("NotificationBadge", "Checking unread notifications for user: " + userId);
        // Use get_notifications.php to get full list and filter duplicates
        String url = "https://boardease.calapebohol.com/get_notifications.php?user_id=" + userId;
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    android.util.Log.d("NotificationBadge", "API Response: " + response.toString());
                    if (response.getBoolean("success")) {
                        JSONObject data = response.getJSONObject("data");
                        JSONArray notifications = data.getJSONArray("notifications");
                        
                        // Calculate unique unread count by filtering duplicates
                        int uniqueUnreadCount = calculateUniqueUnreadCount(notifications);
                        android.util.Log.d("NotificationBadge", "Total notifications: " + notifications.length() + 
                                          ", Unique unread count: " + uniqueUnreadCount);
                        showNotificationBadge(uniqueUnreadCount);
                    } else {
                        android.util.Log.d("NotificationBadge", "API returned success: false");
                        hideNotificationBadge();
                    }
                } catch (JSONException e) {
                    android.util.Log.e("NotificationBadge", "JSON parsing error", e);
                    e.printStackTrace();
                    hideNotificationBadge();
                }
            },
            error -> {
                android.util.Log.e("NotificationBadge", "API request error", error);
                error.printStackTrace();
                hideNotificationBadge();
            }
        );
        
        // Add request to queue
        if (getContext() != null) {
            RequestQueue queue = Volley.newRequestQueue(getContext());
            queue.add(request);
        }
    }
    
    /**
     * Calculate unique unread notification count by filtering duplicates
     */
    private int calculateUniqueUnreadCount(JSONArray notifications) {
        int uniqueUnreadCount = 0;
        java.util.Set<String> seenNotifications = new java.util.HashSet<>();
        
        try {
            for (int i = 0; i < notifications.length(); i++) {
                JSONObject notif = notifications.getJSONObject(i);
                
                // Create unique key for duplicate detection
                String uniqueKey = createNotificationUniqueKey(notif);
                
                // Skip if duplicate
                if (seenNotifications.contains(uniqueKey)) {
                    continue;
                }
                
                // Mark as seen
                seenNotifications.add(uniqueKey);
                
                // Count if unread
                String status = notif.optString("notif_status", "unread");
                if ("unread".equals(status)) {
                    uniqueUnreadCount++;
                }
            }
        } catch (JSONException e) {
            android.util.Log.e("NotificationBadge", "Error calculating unique unread count", e);
        }
        
        return uniqueUnreadCount;
    }
    
    /**
     * Create unique key for notification (same logic as Notification.java)
     */
    private String createNotificationUniqueKey(JSONObject notif) throws JSONException {
        String type = notif.optString("notif_type", "");
        String title = notif.optString("notif_title", "");
        String message = notif.optString("notif_message", "");
        String createdAt = notif.optString("notif_created_at", "");
        
        // For message notifications
        if ("message".equals(type) || (title != null && title.contains("New Message")) || 
            (message != null && message.contains("New message from"))) {
            
            if (message != null && message.contains("New message from")) {
                String[] parts = message.split(": ", 2);
                if (parts.length == 2) {
                    String senderPart = parts[0].replace("New message from ", "");
                    String messageContent = parts[1];
                    return "msg_" + senderPart.hashCode() + "_" + messageContent.hashCode();
                }
            }
            
            return "msg_" + title.hashCode() + "_" + message.hashCode();
        }
        
        // For other notifications, use notif_id
        if (notif.has("notif_id")) {
            int notifId = notif.getInt("notif_id");
            return "notif_" + notifId;
        }
        
        // Last resort: use title + message + timestamp
        String timestampKey = "";
        if (!createdAt.isEmpty()) {
            try {
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                java.util.Date date = inputFormat.parse(createdAt);
                long timeInMinutes = date.getTime() / 60000;
                timestampKey = "_" + timeInMinutes;
            } catch (Exception e) {
                // Ignore
            }
        }
        
        return "notif_" + title.hashCode() + "_" + message.hashCode() + timestampKey;
    }

    private void showNotificationBadge(int count) {
        android.util.Log.d("NotificationBadge", "showNotificationBadge called with count: " + count);
        android.util.Log.d("NotificationBadge", "badgeNotif is null: " + (badgeNotif == null));
        android.util.Log.d("NotificationBadge", "badgeNotifCount is null: " + (badgeNotifCount == null));
        
        if (count > 0) {
            // Hide the simple red dot
            if (badgeNotif != null) {
                badgeNotif.setVisibility(View.GONE);
                android.util.Log.d("NotificationBadge", "Hiding simple red dot");
            }
            
            // Show the count badge
            if (badgeNotifCount != null) {
                android.util.Log.d("NotificationBadge", "Setting up count badge");
                String displayText;
                if (count > 99) {
                    displayText = "99+";
                } else {
                    displayText = String.valueOf(count);
                }
                
                android.util.Log.d("NotificationBadge", "Display text: " + displayText);
                badgeNotifCount.setText(displayText);
                
                // Reset any previous animation state
                badgeNotifCount.clearAnimation();
                badgeNotifCount.setAlpha(1f);
                badgeNotifCount.setScaleX(1f);
                badgeNotifCount.setScaleY(1f);
                badgeNotifCount.setVisibility(View.VISIBLE);
                android.util.Log.d("NotificationBadge", "Count badge visibility set to VISIBLE");
                android.util.Log.d("NotificationBadge", "Badge parent: " + (badgeNotifCount.getParent() != null ? badgeNotifCount.getParent().getClass().getSimpleName() : "null"));
                android.util.Log.d("NotificationBadge", "Badge visibility: " + badgeNotifCount.getVisibility());
                android.util.Log.d("NotificationBadge", "Badge text: " + badgeNotifCount.getText());
                
                // Check layout parameters
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) badgeNotifCount.getLayoutParams();
                if (params != null) {
                    android.util.Log.d("NotificationBadge", "Badge layout params - gravity: " + params.gravity + ", topMargin: " + params.topMargin + ", rightMargin: " + params.rightMargin);
                } else {
                    android.util.Log.d("NotificationBadge", "Badge layout params is null!");
                }
                
                // Add scale animation
                badgeNotifCount.setScaleX(0f);
                badgeNotifCount.setScaleY(0f);
                badgeNotifCount.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start();
                android.util.Log.d("NotificationBadge", "Count badge animation started");
            } else {
                android.util.Log.d("NotificationBadge", "badgeNotifCount is null, cannot show count badge");
            }
        } else {
            hideNotificationBadge();
        }
    }

    private void hideNotificationBadge() {
        android.util.Log.d("NotificationBadge", "hideNotificationBadge called");
        if (badgeNotif != null) {
            badgeNotif.setVisibility(View.GONE);
        }
        if (badgeNotifCount != null) {
            android.util.Log.d("NotificationBadge", "Hiding count badge with animation");
            // Clear any existing animation first
            badgeNotifCount.clearAnimation();
            // Add fade out animation
            badgeNotifCount.animate()
                .alpha(0f)
                .scaleX(0f)
                .scaleY(0f)
                .setDuration(150)
                .withEndAction(() -> {
                    badgeNotifCount.setVisibility(View.GONE);
                    android.util.Log.d("NotificationBadge", "Count badge hidden and set to GONE");
                })
                .start();
        }
    }

    private void setupBadgeUpdateReceiver() {
        badgeUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("com.example.mock.UPDATE_NOTIFICATION_BADGE".equals(action)) {
                    // Refresh notification badge count
                    checkUnreadNotifications();
                } else if ("com.example.mock.UPDATE_BADGE".equals(action)) {
                    // Refresh message badge count
                    checkUnreadMessages();
                }
            }
        };
        
        // Register the receiver
        if (getContext() != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.example.mock.UPDATE_NOTIFICATION_BADGE");
            filter.addAction("com.example.mock.UPDATE_BADGE");
            getContext().registerReceiver(badgeUpdateReceiver, filter);
        }
    }

    private void startPeriodicNotificationCheck() {
        if (notificationCheckHandler == null) {
            notificationCheckHandler = new android.os.Handler();
        }
        
        notificationCheckRunnable = new Runnable() {
            @Override
            public void run() {
                // Check for notifications every 10 seconds
                checkUnreadNotifications();
                
                // Schedule next check
                if (notificationCheckHandler != null) {
                    notificationCheckHandler.postDelayed(this, 10000); // 10 seconds
                }
            }
        };
        
        // Start the periodic check
        notificationCheckHandler.postDelayed(notificationCheckRunnable, 10000);
        android.util.Log.d("NotificationBadge", "Started periodic notification check");
    }
    
    private void stopPeriodicNotificationCheck() {
        if (notificationCheckHandler != null && notificationCheckRunnable != null) {
            notificationCheckHandler.removeCallbacks(notificationCheckRunnable);
            android.util.Log.d("NotificationBadge", "Stopped periodic notification check");
        }
    }
    
    private void startPeriodicMessageCheck() {
        if (messageCheckHandler == null) {
            messageCheckHandler = new android.os.Handler();
        }
        
        messageCheckRunnable = new Runnable() {
            @Override
            public void run() {
                // Check for messages every 10 seconds
                checkUnreadMessages();
                
                // Schedule next check
                if (messageCheckHandler != null) {
                    messageCheckHandler.postDelayed(this, 10000); // 10 seconds
                }
            }
        };
        
        // Start the periodic check
        messageCheckHandler.postDelayed(messageCheckRunnable, 10000);
        android.util.Log.d("MessageBadge", "Started periodic message check");
    }
    
    private void stopPeriodicMessageCheck() {
        if (messageCheckHandler != null && messageCheckRunnable != null) {
            messageCheckHandler.removeCallbacks(messageCheckRunnable);
            android.util.Log.d("MessageBadge", "Stopped periodic message check");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop periodic checks
        stopPolling();
        
        // Unregister broadcast receiver
        if (badgeUpdateReceiver != null && getContext() != null) {
            try {
                getContext().unregisterReceiver(badgeUpdateReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver was not registered
            }
        }
    }


    
    // Chart methods removed - moved to AnalyticsActivity
    // All revenue analytics and charts are now in AnalyticsActivity

    private void checkUserStatus() {
        if (getContext() == null) return;
        
        android.content.SharedPreferences sharedPreferences = getContext().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
        String status = sharedPreferences.getString("user_status", "approved");
        
        if (cardVerificationStatus == null) return;
        
        // Reset defaults
        int accentColor = android.graphics.Color.parseColor("#F57C00"); // Orange
        int iconRes = R.drawable.ic_info;
        String title = "Status";
        String message = "";
        boolean visible = true;
        
        switch (status) {
            case "profile_incomplete":
                accentColor = android.graphics.Color.parseColor("#455A64"); // Blue Gray
                iconRes = R.drawable.ic_info1;
                title = "Profile Incomplete";
                message = "Please complete your profile to unlock all features.";
                break;
            case "pending_admin_review":
                accentColor = android.graphics.Color.parseColor("#FFA000"); // Amber
                iconRes = R.drawable.ic_info;
                title = "Verification Pending";
                message = "Admin is currently reviewing your documents.";
                break;
            case "rejected":
                accentColor = android.graphics.Color.parseColor("#D32F2F"); // Red
                iconRes = R.drawable.ic_close;
                title = "Account Rejected";
                message = "Your application was rejected. Please check your email for details.";
                break;
            case "approved":
            default:
                visible = false;
                break;
        }
        
        if (visible) {
            cardVerificationStatus.setVisibility(View.VISIBLE);
            if (viewStatusAccent != null) viewStatusAccent.setBackgroundColor(accentColor);
            if (ivStatusIcon != null) {
                ivStatusIcon.setImageResource(iconRes);
                ivStatusIcon.setColorFilter(accentColor);
            }
            if (tvStatusTitle != null) {
                tvStatusTitle.setText(title);
            }
            if (tvStatusMessage != null) {
                tvStatusMessage.setText(message);
            }
        } else {
            cardVerificationStatus.setVisibility(View.GONE);
        }
    }
}
