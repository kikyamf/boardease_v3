package com.example.mock;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mock.adapters.BoardingHouseAdapter;
import com.example.mock.adapters.BoardingHouseCarouselAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;
import java.util.List;

/**
 * BoarderHomeFragment - Main home page for boarders
 * Displays recommended boarding houses carousel and nearby boarding houses list
 */
public class BoarderHomeFragment extends Fragment implements BoardingHouseAdapter.OnFavoriteClickListener, BoardingHouseCarouselAdapter.OnFavoriteClickListener {

    // Views
    private EditText etSearch;
    private RecyclerView rvRecommendedBH;
    private RecyclerView rvNearbyBH;
    private MaterialButton btnSeeAll;
    private MaterialCardView btnMyBookings;
    private MaterialCardView btnFavorites;
    private ImageView ivNotification;
    private ImageView ivMessage;
    private TextView tvBoarderName;
    private View badgeMsg;
    private TextView badgeCount;
    private View badgeNotif;
    private TextView badgeNotifCount;

    // Adapters
    private BoardingHouseCarouselAdapter recommendedAdapter;
    private BoardingHouseAdapter nearbyAdapter;

    // Data
    private List<Listing> recommendedBoardingHouses;
    private List<Listing> nearbyBoardingHouses;

    public BoarderHomeFragment() {
        // Required empty public constructor
    }

    public static BoarderHomeFragment newInstance() {
        return new BoarderHomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_boarder_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        android.util.Log.d("BoarderHomeFragment", "=== onViewCreated called ===");
        initializeViews(view);
        setupRecyclerViews();
        setupClickListeners();
        loadMockData();
        android.util.Log.d("BoarderHomeFragment", "=== About to call loadUnreadCount ===");
        loadUnreadCount();
        android.util.Log.d("BoarderHomeFragment", "=== loadUnreadCount called ===");
        loadNotificationCount();
        android.util.Log.d("BoarderHomeFragment", "=== loadNotificationCount called ===");
    }
    
    @Override
    public void onResume() {
        super.onResume();
        android.util.Log.d("BoarderHomeFragment", "=== onResume called ===");
        // Refresh badge count when returning from Messages activity
        android.util.Log.d("BoarderHomeFragment", "=== About to call loadUnreadCount from onResume ===");
        loadUnreadCount();
        android.util.Log.d("BoarderHomeFragment", "=== loadUnreadCount called from onResume ===");
        loadNotificationCount();
        android.util.Log.d("BoarderHomeFragment", "=== loadNotificationCount called from onResume ===");
    }

    private void initializeViews(View view) {
        try {
            etSearch = view.findViewById(R.id.etSearch);
            rvRecommendedBH = view.findViewById(R.id.rvRecommendedBH);
            rvNearbyBH = view.findViewById(R.id.rvNearbyBH);
            btnSeeAll = view.findViewById(R.id.btnSeeAll);
            btnMyBookings = (MaterialCardView) view.findViewById(R.id.cardMyBookings);
            btnFavorites = (MaterialCardView) view.findViewById(R.id.cardFavorites);
            ivNotification = view.findViewById(R.id.ivNotification);
            ivMessage = view.findViewById(R.id.ivMessage);
            tvBoarderName = view.findViewById(R.id.tvBoarderName);
            badgeMsg = view.findViewById(R.id.badgeMsg);
            badgeNotif = view.findViewById(R.id.badgeNotif);
            
            // Create a TextView for message badge count if it doesn't exist
            android.util.Log.d("BoarderHomeFragment", "=== BADGE INITIALIZATION ===");
            android.util.Log.d("BoarderHomeFragment", "badgeMsg is null: " + (badgeMsg == null));
            android.util.Log.d("BoarderHomeFragment", "ivMessage is null: " + (ivMessage == null));
            if (badgeMsg != null) {
                android.util.Log.d("BoarderHomeFragment", "=== Creating badgeCount TextView ===");
                badgeCount = new TextView(getContext());
                
                // Create FrameLayout.LayoutParams for proper positioning
                android.widget.FrameLayout.LayoutParams badgeParams = new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT, 
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT);
                badgeParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
                badgeParams.topMargin = 6;
                badgeParams.rightMargin = 6;
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
                android.util.Log.d("BoarderHomeFragment", "=== Adding badgeCount to parent ===");
                if (ivMessage.getParent() instanceof ViewGroup) {
                    ViewGroup parent = (ViewGroup) ivMessage.getParent();
                    parent.addView(badgeCount);
                    android.util.Log.d("BoarderHomeFragment", "✅ Added badgeCount to parent ViewGroup");
                    android.util.Log.d("BoarderHomeFragment", "Parent ViewGroup: " + parent.getClass().getSimpleName());
                    android.util.Log.d("BoarderHomeFragment", "Parent child count: " + parent.getChildCount());
                    android.util.Log.d("BoarderHomeFragment", "badgeCount created successfully: " + (badgeCount != null));
                } else {
                    android.util.Log.e("BoarderHomeFragment", "❌ ivMessage parent is not a ViewGroup: " + ivMessage.getParent().getClass().getSimpleName());
                }
            } else {
                android.util.Log.e("BoarderHomeFragment", "❌ badgeMsg is null - cannot create badgeCount");
            }
            
            // Create a TextView for notification badge count if it doesn't exist
            android.util.Log.d("BoarderHomeFragment", "=== NOTIFICATION BADGE INITIALIZATION ===");
            android.util.Log.d("BoarderHomeFragment", "badgeNotif is null: " + (badgeNotif == null));
            android.util.Log.d("BoarderHomeFragment", "ivNotification is null: " + (ivNotification == null));
            if (badgeNotif != null) {
                android.util.Log.d("BoarderHomeFragment", "=== Creating badgeNotifCount TextView ===");
                badgeNotifCount = new TextView(getContext());
                
                // Create FrameLayout.LayoutParams for proper positioning
                android.widget.FrameLayout.LayoutParams badgeParams = new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT, 
                    android.widget.FrameLayout.LayoutParams.WRAP_CONTENT);
                badgeParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
                badgeParams.topMargin = 6;
                badgeParams.rightMargin = 6;
                badgeNotifCount.setLayoutParams(badgeParams);
                
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
                
                // Add elevation for better visual separation
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    badgeNotifCount.setElevation(4f);
                }
                
                // Add the badge count to the notification icon's parent FrameLayout
                android.util.Log.d("BoarderHomeFragment", "=== Adding badgeNotifCount to parent ===");
                if (ivNotification.getParent() instanceof ViewGroup) {
                    ViewGroup parent = (ViewGroup) ivNotification.getParent();
                    parent.addView(badgeNotifCount);
                    android.util.Log.d("BoarderHomeFragment", "✅ Added badgeNotifCount to parent ViewGroup");
                    android.util.Log.d("BoarderHomeFragment", "Parent ViewGroup: " + parent.getClass().getSimpleName());
                    android.util.Log.d("BoarderHomeFragment", "Parent child count: " + parent.getChildCount());
                    android.util.Log.d("BoarderHomeFragment", "badgeNotifCount created successfully: " + (badgeNotifCount != null));
                } else {
                    android.util.Log.e("BoarderHomeFragment", "❌ ivNotification parent is not a ViewGroup: " + ivNotification.getParent().getClass().getSimpleName());
                }
            } else {
                android.util.Log.e("BoarderHomeFragment", "❌ badgeNotif is null - cannot create badgeNotifCount");
            }
            
            android.util.Log.d("BoarderHomeFragment", "Views initialized - ivMessage: " + (ivMessage != null ? "found" : "null"));
        } catch (Exception e) {
            android.util.Log.e("BoarderHomeFragment", "Error initializing views", e);
            e.printStackTrace();
        }
    }

    private void setupRecyclerViews() {
        try {
            // Initialize data lists
            recommendedBoardingHouses = new ArrayList<>();
            nearbyBoardingHouses = new ArrayList<>();

            // Setup Recommended BHs RecyclerView (Horizontal)
            if (rvRecommendedBH != null && getContext() != null) {
                recommendedAdapter = new BoardingHouseCarouselAdapter(getContext(), recommendedBoardingHouses, this);
                LinearLayoutManager recommendedLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
                rvRecommendedBH.setLayoutManager(recommendedLayoutManager);
                rvRecommendedBH.setAdapter(recommendedAdapter);
            }

            // Setup Nearby BHs RecyclerView (Vertical)
            if (rvNearbyBH != null && getContext() != null) {
                nearbyAdapter = new BoardingHouseAdapter(getContext(), nearbyBoardingHouses, this);
                LinearLayoutManager nearbyLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
                rvNearbyBH.setLayoutManager(nearbyLayoutManager);
                rvNearbyBH.setAdapter(nearbyAdapter);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupClickListeners() {
        android.util.Log.d("BoarderHomeFragment", "setupClickListeners called");
        // Search functionality
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        // TODO: Implement search functionality
                        // For now, just show a toast
                        if (s.length() > 0 && getContext() != null) {
                            Toast.makeText(getContext(), "Searching for: " + s.toString(), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // See All button - Navigate to BoarderExploreFragment
        if (btnSeeAll != null) {
            btnSeeAll.setOnClickListener(v -> {
                try {
                    // Navigate to explore fragment
                    if (getActivity() instanceof BoarderDashboard) {
                        BoarderDashboard dashboard = (BoarderDashboard) getActivity();
                        // Switch to explore tab (nav_post)
                        dashboard.switchToTab(R.id.nav_post);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Navigation error", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // My Bookings button
        if (btnMyBookings != null) {
            btnMyBookings.setOnClickListener(v -> {
                Toast.makeText(getContext(), "My Bookings - Coming Soon!", Toast.LENGTH_SHORT).show();
                // TODO: Navigate to bookings fragment
            });
        }

        // Favorites button
        if (btnFavorites != null) {
            btnFavorites.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Favorites - Coming Soon!", Toast.LENGTH_SHORT).show();
                // TODO: Navigate to favorites fragment
            });
        }

        // Notification icon
        if (ivNotification != null) {
            ivNotification.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Notifications - Coming Soon!", Toast.LENGTH_SHORT).show();
                // TODO: Navigate to notifications
            });
        }

        // Message icon
        if (ivMessage != null) {
            android.util.Log.d("BoarderHomeFragment", "Setting up message icon click listener");
            ivMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    android.util.Log.d("BoarderHomeFragment", "Message icon clicked!");
                    Toast.makeText(getContext(), "Message icon clicked!", Toast.LENGTH_SHORT).show();
                    try {
                        Intent intent = new Intent(getContext(), Messages.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        android.util.Log.e("BoarderHomeFragment", "Error starting Messages activity", e);
                        Toast.makeText(getContext(), "Error opening messages: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            android.util.Log.e("BoarderHomeFragment", "ivMessage is null!");
        }
        
        // Notification icon
        if (ivNotification != null) {
            android.util.Log.d("BoarderHomeFragment", "Setting up notification icon click listener");
            ivNotification.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    android.util.Log.d("BoarderHomeFragment", "Notification icon clicked!");
                    try {
                        // Hide notification badge when opening notifications
                        hideNotificationBadge();
                        Intent intent = new Intent(getContext(), Notification.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        android.util.Log.e("BoarderHomeFragment", "Error starting Notification activity", e);
                        Toast.makeText(getContext(), "Error opening notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            android.util.Log.e("BoarderHomeFragment", "ivNotification is null!");
        }
    }

    private void loadMockData() {
        try {
            // Create mock data for recommended boarding houses
            createRecommendedMockData();
            
            // Create mock data for nearby boarding houses
            createNearbyMockData();
            
            // Update adapters
            if (recommendedAdapter != null) {
                recommendedAdapter.notifyDataSetChanged();
            }
            if (nearbyAdapter != null) {
                nearbyAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createRecommendedMockData() {
        recommendedBoardingHouses.clear();
        
        // Recommended boarding houses (featured/popular ones)
        recommendedBoardingHouses.add(new Listing(1, "Sunshine Boarding House", "sample_listing"));
        recommendedBoardingHouses.add(new Listing(2, "Green Valley Dormitory", "sample_listing"));
        recommendedBoardingHouses.add(new Listing(3, "Metro Student Housing", "sample_listing"));
        recommendedBoardingHouses.add(new Listing(4, "Quezon City Boarding", "sample_listing"));
        recommendedBoardingHouses.add(new Listing(5, "Manila Central Dorm", "sample_listing"));
    }

    private void createNearbyMockData() {
        nearbyBoardingHouses.clear();
        
        // Nearby boarding houses
        nearbyBoardingHouses.add(new Listing(6, "Downtown Boarding House", "sample_listing"));
        nearbyBoardingHouses.add(new Listing(7, "University Dormitory", "sample_listing"));
        nearbyBoardingHouses.add(new Listing(8, "City Center Housing", "sample_listing"));
        nearbyBoardingHouses.add(new Listing(9, "Student Plaza Dorm", "sample_listing"));
        nearbyBoardingHouses.add(new Listing(10, "Metro Boarding Inn", "sample_listing"));
        nearbyBoardingHouses.add(new Listing(11, "Campus View Dormitory", "sample_listing"));
        nearbyBoardingHouses.add(new Listing(12, "Central Station Boarding", "sample_listing"));
    }

    @Override
    public void onFavoriteClick(Listing boardingHouse, boolean isFavorite) {
        try {
            if (isFavorite) {
                // Add to favorites
                BoarderFavoriteFragment.addToFavorites(getContext(), boardingHouse);
                String message = "Added to favorites: " + boardingHouse.getBhName();
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            } else {
                // Remove from favorites
                String message = "Removed from favorites: " + boardingHouse.getBhName();
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                // TODO: Implement remove from favorites functionality
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Custom ItemDecoration for horizontal spacing
    public static class HorizontalSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spacing;

        public HorizontalSpacingItemDecoration(int spacing) {
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(android.graphics.Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            
            // Add spacing to the right of each item except the last one
            if (position != parent.getAdapter().getItemCount() - 1) {
                outRect.right = spacing;
            }
        }
    }
    
    private void loadUnreadCount() {
        try {
            android.util.Log.d("BoarderHomeFragment", "=== loadUnreadCount START ===");
            android.util.Log.d("BoarderHomeFragment", "badgeCount is null: " + (badgeCount == null));
            
            // Get current user ID from SharedPreferences
            android.content.SharedPreferences prefs = getActivity().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
            String currentUserId = prefs.getString("user_id", "");
            
            android.util.Log.d("BoarderHomeFragment", "Current user ID: " + currentUserId);
            if (currentUserId.isEmpty()) {
                android.util.Log.e("BoarderHomeFragment", "❌ No user ID found in SharedPreferences");
                return;
            }
            
            String url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_unread_count.php?user_id=" + currentUserId;
            android.util.Log.d("BoarderHomeFragment", "API URL: " + url);
            android.util.Log.d("BoarderHomeFragment", "=== Making API request ===");
            
            com.android.volley.toolbox.JsonObjectRequest request = new com.android.volley.toolbox.JsonObjectRequest(
                com.android.volley.Request.Method.GET, url, null,
                response -> {
                    try {
                        android.util.Log.d("BoarderHomeFragment", "=== API RESPONSE RECEIVED ===");
                        android.util.Log.d("BoarderHomeFragment", "Unread count response: " + response.toString());
                        if (response.getBoolean("success")) {
                            android.util.Log.d("BoarderHomeFragment", "✅ API success = true");
                            // Get unread count from data.total_unread
                            org.json.JSONObject data = response.getJSONObject("data");
                            int unreadCount = data.getInt("total_unread");
                            android.util.Log.d("BoarderHomeFragment", "✅ Parsed unread count: " + unreadCount);
                            android.util.Log.d("BoarderHomeFragment", "=== Calling updateMessageBadge ===");
                            updateMessageBadge(unreadCount);
                        } else {
                            android.util.Log.e("BoarderHomeFragment", "❌ API success = false: " + response.getString("message"));
                            android.util.Log.d("BoarderHomeFragment", "=== Calling updateMessageBadge with 0 ===");
                            updateMessageBadge(0);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("BoarderHomeFragment", "❌ Error parsing unread count response", e);
                        android.util.Log.d("BoarderHomeFragment", "=== Calling updateMessageBadge with 0 due to error ===");
                        updateMessageBadge(0);
                    }
                },
                error -> {
                    android.util.Log.e("BoarderHomeFragment", "❌ API ERROR: " + error.getMessage());
                    android.util.Log.d("BoarderHomeFragment", "=== Calling updateMessageBadge with 0 due to API error ===");
                    updateMessageBadge(0);
                }
            );
            
            // Add request to queue
            android.util.Log.d("BoarderHomeFragment", "=== Adding request to Volley queue ===");
            RequestQueue queue = Volley.newRequestQueue(getContext());
            queue.add(request);
            android.util.Log.d("BoarderHomeFragment", "✅ Request added to queue");
            
        } catch (Exception e) {
            android.util.Log.e("BoarderHomeFragment", "❌ Exception in loadUnreadCount", e);
            android.util.Log.d("BoarderHomeFragment", "=== Calling updateMessageBadge with 0 due to exception ===");
            updateMessageBadge(0);
        }
    }
    
    private void loadNotificationCount() {
        try {
            android.util.Log.d("BoarderHomeFragment", "=== loadNotificationCount START ===");
            android.util.Log.d("BoarderHomeFragment", "badgeNotifCount is null: " + (badgeNotifCount == null));
            
            // Get current user ID from SharedPreferences
            android.content.SharedPreferences prefs = getActivity().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
            String currentUserId = prefs.getString("user_id", "");
            
            android.util.Log.d("BoarderHomeFragment", "Current user ID for notifications: " + currentUserId);
            if (currentUserId.isEmpty()) {
                android.util.Log.e("BoarderHomeFragment", "❌ No user ID found in SharedPreferences for notifications");
                return;
            }
            
            String url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_notifications.php?user_id=" + currentUserId;
            android.util.Log.d("BoarderHomeFragment", "Notification API URL: " + url);
            android.util.Log.d("BoarderHomeFragment", "=== Making notification API request ===");
            
            com.android.volley.toolbox.JsonObjectRequest request = new com.android.volley.toolbox.JsonObjectRequest(
                com.android.volley.Request.Method.GET, url, null,
                response -> {
                    try {
                        android.util.Log.d("BoarderHomeFragment", "=== NOTIFICATION API RESPONSE RECEIVED ===");
                        android.util.Log.d("BoarderHomeFragment", "Notification response: " + response.toString());
                        if (response.getBoolean("success")) {
                            android.util.Log.d("BoarderHomeFragment", "✅ Notification API success = true");
                            // Get unread notification count from data object
                            org.json.JSONObject data = response.getJSONObject("data");
                            int unreadCount = data.getInt("unread_count");
                            android.util.Log.d("BoarderHomeFragment", "✅ Parsed notification count: " + unreadCount);
                            android.util.Log.d("BoarderHomeFragment", "=== Calling showNotificationBadge ===");
                            showNotificationBadge(unreadCount);
                        } else {
                            android.util.Log.e("BoarderHomeFragment", "❌ Notification API success = false: " + response.getString("message"));
                            android.util.Log.d("BoarderHomeFragment", "=== Calling hideNotificationBadge due to API error ===");
                            hideNotificationBadge();
                        }
                    } catch (Exception e) {
                        android.util.Log.e("BoarderHomeFragment", "❌ Error parsing notification response", e);
                        android.util.Log.d("BoarderHomeFragment", "=== Calling hideNotificationBadge due to parsing error ===");
                        hideNotificationBadge();
                    }
                },
                error -> {
                    android.util.Log.e("BoarderHomeFragment", "❌ NOTIFICATION API ERROR: " + error.getMessage());
                    android.util.Log.d("BoarderHomeFragment", "=== Calling hideNotificationBadge due to API error ===");
                    hideNotificationBadge();
                }
            );
            
            // Add request to queue
            android.util.Log.d("BoarderHomeFragment", "=== Adding notification request to Volley queue ===");
            RequestQueue queue = Volley.newRequestQueue(getContext());
            queue.add(request);
            android.util.Log.d("BoarderHomeFragment", "✅ Notification request added to queue");
            
        } catch (Exception e) {
            android.util.Log.e("BoarderHomeFragment", "❌ Exception in loadNotificationCount", e);
            android.util.Log.d("BoarderHomeFragment", "=== Calling hideNotificationBadge due to exception ===");
            hideNotificationBadge();
        }
    }
    
    private void updateMessageBadge(int count) {
        try {
            android.util.Log.d("BoarderHomeFragment", "=== updateMessageBadge START ===");
            android.util.Log.d("BoarderHomeFragment", "Count received: " + count);
            android.util.Log.d("BoarderHomeFragment", "badgeCount is null: " + (badgeCount == null));
            
            if (badgeCount != null) {
                android.util.Log.d("BoarderHomeFragment", "✅ badgeCount is NOT null");
                if (count > 0) {
                    android.util.Log.d("BoarderHomeFragment", "✅ Setting badge VISIBLE with count: " + count);
                    badgeCount.setText(String.valueOf(count));
                    badgeCount.setVisibility(View.VISIBLE);
                    android.util.Log.d("BoarderHomeFragment", "✅ Badge set to VISIBLE with text: " + count);
                } else {
                    android.util.Log.d("BoarderHomeFragment", "✅ Setting badge GONE (count: " + count + ")");
                    badgeCount.setVisibility(View.GONE);
                    android.util.Log.d("BoarderHomeFragment", "✅ Badge set to GONE");
                }
            } else {
                android.util.Log.e("BoarderHomeFragment", "❌ badgeCount is NULL - cannot update badge");
                android.util.Log.e("BoarderHomeFragment", "❌ This means badgeCount was not created properly");
            }
            android.util.Log.d("BoarderHomeFragment", "=== updateMessageBadge END ===");
        } catch (Exception e) {
            android.util.Log.e("BoarderHomeFragment", "❌ Exception in updateMessageBadge", e);
            e.printStackTrace();
        }
    }
    
    private void showNotificationBadge(int count) {
        try {
            android.util.Log.d("BoarderHomeFragment", "=== showNotificationBadge START ===");
            android.util.Log.d("BoarderHomeFragment", "Count received: " + count);
            android.util.Log.d("BoarderHomeFragment", "badgeNotif is null: " + (badgeNotif == null));
            android.util.Log.d("BoarderHomeFragment", "badgeNotifCount is null: " + (badgeNotifCount == null));
            
            if (count > 0) {
                // Hide the simple red dot
                if (badgeNotif != null) {
                    badgeNotif.setVisibility(View.GONE);
                    android.util.Log.d("BoarderHomeFragment", "Hiding simple red dot");
                }
                
                // Show the count badge
                if (badgeNotifCount != null) {
                    android.util.Log.d("BoarderHomeFragment", "Setting up count badge");
                    String displayText;
                    if (count > 99) {
                        displayText = "99+";
                    } else {
                        displayText = String.valueOf(count);
                    }
                    
                    android.util.Log.d("BoarderHomeFragment", "Display text: " + displayText);
                    badgeNotifCount.setText(displayText);
                    badgeNotifCount.setVisibility(View.VISIBLE);
                    android.util.Log.d("BoarderHomeFragment", "✅ Notification badge set to VISIBLE with text: " + displayText);
                } else {
                    android.util.Log.e("BoarderHomeFragment", "❌ badgeNotifCount is null, cannot show count badge");
                }
            } else {
                hideNotificationBadge();
            }
            android.util.Log.d("BoarderHomeFragment", "=== showNotificationBadge END ===");
        } catch (Exception e) {
            android.util.Log.e("BoarderHomeFragment", "❌ Exception in showNotificationBadge", e);
            e.printStackTrace();
        }
    }
    
    private void hideNotificationBadge() {
        try {
            android.util.Log.d("BoarderHomeFragment", "=== hideNotificationBadge START ===");
            if (badgeNotif != null) {
                badgeNotif.setVisibility(View.GONE);
            }
            if (badgeNotifCount != null) {
                badgeNotifCount.setVisibility(View.GONE);
                android.util.Log.d("BoarderHomeFragment", "✅ Notification badge hidden");
            }
            android.util.Log.d("BoarderHomeFragment", "=== hideNotificationBadge END ===");
        } catch (Exception e) {
            android.util.Log.e("BoarderHomeFragment", "❌ Exception in hideNotificationBadge", e);
            e.printStackTrace();
        }
    }
}