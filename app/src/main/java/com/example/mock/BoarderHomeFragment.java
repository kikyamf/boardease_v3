package com.example.mock;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mock.adapters.BoardingHouseAdapter;
import com.example.mock.adapters.BoardingHouseCarouselAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * BoarderHomeFragment - Main home page for boarders
 * Displays recommended boarding houses carousel and nearby boarding houses list
 */
public class BoarderHomeFragment extends Fragment implements BoardingHouseAdapter.OnFavoriteClickListener, BoardingHouseCarouselAdapter.OnFavoriteClickListener {

    private static final String TAG = "BoarderHomeFragment";
    private static final String BASE_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/";
    private static final String BOARDER_INFO_API = BASE_URL + "get_boarder_info.php";
    private static final String BOARDING_HOUSES_API = BASE_URL + "get_boarding_houses1.php";

    // Views
    private EditText etSearch;
    private ImageView ivClearSearch;
    private RecyclerView rvRecommendedBH;
    private RecyclerView rvNearbyBH;
    private ProgressBar progressBarRecommended;
    private ProgressBar progressBarNearby;
    private TextView tvRecommendedEmpty;
    private TextView tvNearbyEmpty;
    private MaterialButton btnSeeAll;
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
    private List<Listing> allBoardingHouses;
    private List<Listing> recommendedBoardingHouses;
    private List<Listing> nearbyBoardingHouses;
    
    // Store original filtered lists (before search)
    private List<Listing> originalRecommendedBoardingHouses;
    private List<Listing> originalNearbyBoardingHouses;
    
    // Boarder info
    private String boarderFirstName;
    private String boarderLastName;
    private String boarderSuffix;
    private String boarderAddress;
    private String boarderProvince;
    private String boarderMunicipality;
    
    // Boarder coordinates for distance calculation
    private Double boarderLatitude;
    private Double boarderLongitude;
    
    // Distance threshold in kilometers
    private static final double NEARBY_RADIUS_KM = 5.0;
    
    // Flags to track filtering completion
    private boolean recommendedFiltered = false;
    private boolean nearbyFiltered = false;
    
    // Flag to track if data has been loaded (to prevent reloading on navigation)
    private boolean dataLoaded = false;
    
    // Flag to track if we should refresh data (set when activity pauses while fragment is visible)
    private boolean shouldRefreshOnResume = false;

    public BoarderHomeFragment() {
        // Required empty public constructor
    }

    public static BoarderHomeFragment newInstance() {
        return new BoarderHomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_boarder_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        android.util.Log.d("BoarderHomeFragment", "=== onViewCreated called ===");
        android.util.Log.d("BoarderHomeFragment", "=== dataLoaded: " + dataLoaded + " ===");
        
        // Check if views are already initialized (fragment was hidden/shown, not recreated)
        if (tvBoarderName == null) {
            // Views not initialized yet, initialize them
        initializeViews(view);
        setupRecyclerViews();
        setupClickListeners();
        }
        
        // Only load data if it hasn't been loaded yet (first time only)
        if (!dataLoaded) {
            android.util.Log.d("BoarderHomeFragment", "=== Loading data for the first time ===");
            // Load boarder info first, then boarding houses
            loadBoarderInfo();
        } else {
            android.util.Log.d("BoarderHomeFragment", "=== Data already loaded, skipping reload ===");
            // Data already loaded, just ensure UI is visible
            if (rvRecommendedBH != null) {
                rvRecommendedBH.setVisibility(View.VISIBLE);
            }
            if (rvNearbyBH != null) {
                rvNearbyBH.setVisibility(View.VISIBLE);
            }
            hideProgressBars();
        }
        
        // Always refresh badge counts when fragment becomes visible
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
        android.util.Log.d("BoarderHomeFragment", "=== shouldRefreshOnResume: " + shouldRefreshOnResume + ", isVisible: " + isVisible() + " ===");
        
        // If we should refresh (activity was paused while fragment was visible) AND fragment is now visible
        // AND data was already loaded, refresh the data to show any changes
        if (shouldRefreshOnResume && isVisible() && dataLoaded && getActivity() != null && getActivity().hasWindowFocus()) {
            android.util.Log.d("BoarderHomeFragment", "=== Activity was resumed, fragment visible, refreshing data ===");
            // Reset flags to trigger reload
            dataLoaded = false;
            recommendedFiltered = false;
            nearbyFiltered = false;
            // Clear existing data
            if (allBoardingHouses != null) {
                allBoardingHouses.clear();
            }
            if (recommendedBoardingHouses != null) {
                recommendedBoardingHouses.clear();
            }
            if (nearbyBoardingHouses != null) {
                nearbyBoardingHouses.clear();
            }
            // Reload data to reflect any changes (bookings, favorites, new listings, etc.)
            loadBoarderInfo();
        }
        
        // Always refresh badge counts when fragment becomes visible
        android.util.Log.d("BoarderHomeFragment", "=== About to call loadUnreadCount from onResume ===");
        loadUnreadCount();
        android.util.Log.d("BoarderHomeFragment", "=== loadUnreadCount called from onResume ===");
        loadNotificationCount();
        android.util.Log.d("BoarderHomeFragment", "=== loadNotificationCount called from onResume ===");
        
        // Reset flag after checking
        shouldRefreshOnResume = false;
    }
    
    @Override
    public void onPause() {
        super.onPause();
        android.util.Log.d("BoarderHomeFragment", "=== onPause called, isVisible: " + isVisible() + " ===");
        // If fragment is visible when activity pauses, mark that we should refresh on resume
        // This means user navigated to another activity (not just fragment navigation)
        if (isVisible()) {
            android.util.Log.d("BoarderHomeFragment", "=== Fragment is visible on pause, will refresh on resume ===");
            shouldRefreshOnResume = true;
        }
    }
    
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        android.util.Log.d("BoarderHomeFragment", "=== onHiddenChanged called, hidden: " + hidden + " ===");
        // When fragment is hidden/shown (navigation between fragments), clear refresh flag
        // This is just fragment navigation, not activity pause, so don't refresh
        if (hidden) {
            shouldRefreshOnResume = false;
        }
    }

    private void initializeViews(View view) {
        try {
            etSearch = view.findViewById(R.id.etSearch);
            ivClearSearch = view.findViewById(R.id.ivClearSearch);
            rvRecommendedBH = view.findViewById(R.id.rvRecommendedBH);
            rvNearbyBH = view.findViewById(R.id.rvNearbyBH);
            progressBarRecommended = view.findViewById(R.id.progressBarRecommended);
            progressBarNearby = view.findViewById(R.id.progressBarNearby);
            tvRecommendedEmpty = view.findViewById(R.id.tvRecommendedEmpty);
            tvNearbyEmpty = view.findViewById(R.id.tvNearbyEmpty);
            
            // Initially hide empty state messages
            if (tvRecommendedEmpty != null) {
                tvRecommendedEmpty.setVisibility(View.GONE);
            }
            if (tvNearbyEmpty != null) {
                tvNearbyEmpty.setVisibility(View.GONE);
            }
            
            btnSeeAll = view.findViewById(R.id.btnSeeAll);
            ivNotification = view.findViewById(R.id.ivNotification);
            ivMessage = view.findViewById(R.id.ivMessage);
            tvBoarderName = view.findViewById(R.id.tvBoarderName);
            badgeMsg = view.findViewById(R.id.badgeMsg);
            badgeNotif = view.findViewById(R.id.badgeNotif);
            
            // Initially show progress bars and hide RecyclerViews
            if (progressBarRecommended != null) {
                progressBarRecommended.setVisibility(View.VISIBLE);
            }
            if (progressBarNearby != null) {
                progressBarNearby.setVisibility(View.VISIBLE);
            }
            if (rvRecommendedBH != null) {
                rvRecommendedBH.setVisibility(View.GONE);
            }
            if (rvNearbyBH != null) {
                rvNearbyBH.setVisibility(View.GONE);
            }
            
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
            allBoardingHouses = new ArrayList<>();
            recommendedBoardingHouses = new ArrayList<>();
            nearbyBoardingHouses = new ArrayList<>();

            // Setup Recommended BHs RecyclerView (Horizontal)
            if (rvRecommendedBH != null && getContext() != null) {
                recommendedAdapter = new BoardingHouseCarouselAdapter(getContext(), recommendedBoardingHouses, this);
                LinearLayoutManager recommendedLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
                rvRecommendedBH.setLayoutManager(recommendedLayoutManager);
                rvRecommendedBH.setAdapter(recommendedAdapter);
                // Ensure RecyclerView scrolls to start position (left side) - use post to ensure layout is complete
                rvRecommendedBH.post(new Runnable() {
                    @Override
                    public void run() {
                        if (rvRecommendedBH != null && recommendedBoardingHouses != null && !recommendedBoardingHouses.isEmpty()) {
                            rvRecommendedBH.scrollToPosition(0);
                        }
                    }
                });
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
                        // Show/hide clear icon based on text
                        if (ivClearSearch != null) {
                            ivClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                        }
                        // Filter boarding houses based on search query
                        filterBoardingHousesBySearch(s.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Setup clear search icon
        if (ivClearSearch != null) {
            ivClearSearch.setOnClickListener(v -> {
                if (etSearch != null) {
                    etSearch.setText("");
                    etSearch.clearFocus();
                    ivClearSearch.setVisibility(View.GONE);
                    // Restore original filtered lists when search is cleared
                    restoreOriginalLists();
                }
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

    private void loadBoarderInfo() {
        // Check if fragment is attached and context is available
        if (getContext() == null || !isAdded()) {
            Log.e(TAG, "Cannot load boarder info - fragment not attached or context is null");
            return;
        }
        
        String userId = Login.getCurrentUserId(getContext());
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID not found");
            // Load boarding houses anyway (without filtering)
            if (isAdded() && getContext() != null) {
                loadBoardingHouses();
            }
            return;
        }

        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        String url = BOARDER_INFO_API + "?user_id=" + userId;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Log.d(TAG, "Boarder info response: " + response);
                            
                            if (response == null || response.trim().isEmpty()) {
                                Log.e(TAG, "Received null or empty response");
                                loadBoardingHouses();
                                return;
                            }
                            
                            if (response.trim().startsWith("<!DOCTYPE html>") || response.trim().startsWith("<html")) {
                                Log.e(TAG, "Received HTML page instead of JSON");
                                loadBoardingHouses();
                                return;
                            }
                            
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success");
                            
                            if (success) {
                                JSONObject data = jsonResponse.getJSONObject("data");
                                boarderFirstName = data.optString("first_name", "");
                                boarderLastName = data.optString("last_name", "");
                                boarderSuffix = data.optString("suffix", "");
                                boarderAddress = data.optString("address", "");
                                
                                // Normalize suffix - handle "none", "null", or empty
                                if (boarderSuffix == null || boarderSuffix.isEmpty() || 
                                    boarderSuffix.equalsIgnoreCase("null") || 
                                    boarderSuffix.equalsIgnoreCase("none")) {
                                    boarderSuffix = "";
                                }
                                
                                // Parse address to extract province and municipality
                                parseBoarderAddress(boarderAddress);
                                
                                // Update boarder name in UI
                                updateBoarderName();
                                
                                // Geocode boarder address to get coordinates for distance calculation
                                geocodeBoarderAddress(boarderAddress);
                                
                                Log.d(TAG, "Boarder loaded: " + boarderFirstName + " " + boarderLastName + 
                                          (boarderSuffix.isEmpty() ? "" : ", " + boarderSuffix));
                                Log.d(TAG, "Boarder address: " + boarderAddress);
                                Log.d(TAG, "Boarder province: " + boarderProvince);
                                Log.d(TAG, "Boarder municipality: " + boarderMunicipality);
                            } else {
                                Log.e(TAG, "Failed to load boarder info");
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
        } catch (Exception e) {
                            Log.e(TAG, "Unexpected error: " + e.getMessage());
                        } finally {
                            // Load boarding houses after boarder info is loaded (or failed)
                            // Only if fragment is still attached
                            if (isAdded() && getContext() != null) {
                                loadBoardingHouses();
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Volley error loading boarder info: " + error.getMessage());
                        // Load boarding houses anyway, but only if fragment is still attached
                        if (isAdded() && getContext() != null) {
                            loadBoardingHouses();
                        }
                    }
                });

        requestQueue.add(stringRequest);
    }

    private void parseBoarderAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            boarderProvince = "";
            boarderMunicipality = "";
            return;
        }

        // Address format is typically: "Detailed Address, Barangay, Municipality, Province"
        // Split by comma and trim
        String[] parts = address.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }

        // Province is usually the last part
        if (parts.length > 0) {
            boarderProvince = parts[parts.length - 1];
        } else {
            boarderProvince = "";
        }

        // Municipality is usually the second to last part
        if (parts.length > 1) {
            boarderMunicipality = parts[parts.length - 2];
        } else {
            boarderMunicipality = "";
        }

        Log.d(TAG, "Parsed address - Province: " + boarderProvince + ", Municipality: " + boarderMunicipality);
    }

    private void updateBoarderName() {
        if (tvBoarderName != null) {
            StringBuilder displayName = new StringBuilder();
            
            // Add first name
            if (boarderFirstName != null && !boarderFirstName.isEmpty()) {
                displayName.append(boarderFirstName);
            }
            
            // Add last name
            if (boarderLastName != null && !boarderLastName.isEmpty()) {
                if (displayName.length() > 0) {
                    displayName.append(" ");
                }
                displayName.append(boarderLastName);
            }
            
            // Add suffix if available and not "none"
            if (boarderSuffix != null && !boarderSuffix.isEmpty() && 
                !boarderSuffix.equalsIgnoreCase("none") && 
                !boarderSuffix.equalsIgnoreCase("null")) {
                if (displayName.length() > 0) {
                    displayName.append(", ");
                }
                displayName.append(boarderSuffix);
            }
            
            // Set the display name, or default to "Boarder" if empty
            if (displayName.length() > 0) {
                tvBoarderName.setText(displayName.toString());
            } else {
                tvBoarderName.setText("Boarder");
            }
        }
    }

    private void loadBoardingHouses() {
        // Check if fragment is attached and context is available
        if (getContext() == null || !isAdded()) {
            Log.e(TAG, "Cannot load boarding houses - fragment not attached or context is null");
            hideProgressBars();
            return;
        }
        
        // Show progress bars when starting to load boarding houses
        showProgressBars();
        
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());

        StringRequest stringRequest = new StringRequest(Request.Method.GET, BOARDING_HOUSES_API,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Log.d(TAG, "Boarding houses response received");
                            
                            if (response == null || response.trim().isEmpty()) {
                                Log.e(TAG, "Received null or empty response");
                                return;
                            }
                            
                            if (response.trim().startsWith("<!DOCTYPE html>") || response.trim().startsWith("<html")) {
                                Log.e(TAG, "Received HTML page instead of JSON");
                                return;
                            }
                            
                            JSONArray dataArray;
                            // Try to parse as wrapped JSON object first
                            try {
                                JSONObject jsonResponse = new JSONObject(response);
                                boolean success = jsonResponse.getBoolean("success");
                                
                                if (success) {
                                    dataArray = jsonResponse.getJSONArray("data");
                                } else {
                                    Log.e(TAG, "API returned success=false");
                                    return;
                                }
                            } catch (JSONException e) {
                                // If wrapped format fails, try parsing as direct array
                                Log.d(TAG, "Wrapped format failed, trying direct array format");
                                dataArray = new JSONArray(response);
                            }
                            
                            // Check if fragment is still attached before processing
                            if (!isAdded() || getContext() == null) {
                                Log.d(TAG, "Fragment not attached, skipping boarding houses processing");
                                return;
                            }
                            
                            // Reset flags for new filtering cycle
                            recommendedFiltered = false;
                            nearbyFiltered = false;
                            
                            parseBoardingHousesData(dataArray);
                            filterBoardingHouses();
                            // Mark data as loaded
                            dataLoaded = true;
                            // Don't call updateAdapters() here - wait for both sections to be ready
                            
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            if (isAdded() && getContext() != null) {
                                hideProgressBars();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Unexpected error: " + e.getMessage());
                            if (isAdded() && getContext() != null) {
                                hideProgressBars();
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Volley error loading boarding houses: " + error.getMessage());
                        if (isAdded() && getContext() != null) {
                            hideProgressBars();
                        }
                    }
                }) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("User-Agent", "BoardEase-Android-App");
                headers.put("Accept", "application/json");
                return headers;
            }
        };

        requestQueue.add(stringRequest);
    }

    private void parseBoardingHousesData(JSONArray dataArray) throws JSONException {
        allBoardingHouses.clear();
        
        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject boardingHouseJson = dataArray.getJSONObject(i);
            
            int bhId = boardingHouseJson.getInt("bh_id");
            String bhName = boardingHouseJson.getString("bh_name");
            String bhAddress = boardingHouseJson.optString("bh_address", "");
            String bhDescription = boardingHouseJson.optString("bh_description", "");
            String bhRules = boardingHouseJson.optString("bh_rules", "");
            String bhBathrooms = boardingHouseJson.optString("number_of_bathroom", "");
            String area = boardingHouseJson.optString("area", "");
            String buildYear = boardingHouseJson.optString("build_year", "");
            String imagePath = boardingHouseJson.optString("image_path", "");
            
            // Parse price data
            Integer minPrice = null;
            Integer maxPrice = null;
            if (!boardingHouseJson.isNull("min_price")) {
                minPrice = boardingHouseJson.optInt("min_price");
            }
            if (!boardingHouseJson.isNull("max_price")) {
                maxPrice = boardingHouseJson.optInt("max_price");
            }
            
            // Create image paths list
            ArrayList<String> imagePaths = new ArrayList<>();
            if (imagePath != null && !imagePath.isEmpty()) {
                imagePaths.add(imagePath);
            }
            
            // Create Listing object with full details
            Listing boardingHouse = new Listing(
                bhId, bhName, bhAddress, bhDescription, bhRules,
                bhBathrooms, area, buildYear, imagePath, imagePaths, minPrice, maxPrice
            );
            
            allBoardingHouses.add(boardingHouse);
        }
        
        Log.d(TAG, "Loaded " + allBoardingHouses.size() + " boarding houses from API");
    }

    private void filterBoardingHouses() {
        recommendedBoardingHouses.clear();
        nearbyBoardingHouses.clear();
        
        if (allBoardingHouses == null || allBoardingHouses.isEmpty()) {
            Log.d(TAG, "No boarding houses to filter");
            // Mark both as filtered and update (show empty state)
            recommendedFiltered = true;
            nearbyFiltered = true;
            updateBothSections();
            return;
        }
        
        // If no boarder address info, show all boarding houses in both sections
        if (boarderProvince == null || boarderProvince.isEmpty()) {
            Log.d(TAG, "No boarder address info, showing all boarding houses");
            recommendedBoardingHouses.addAll(allBoardingHouses);
            nearbyBoardingHouses.addAll(allBoardingHouses);
            Log.d(TAG, "Showing all - Recommended: " + recommendedBoardingHouses.size() + 
                       ", Nearby: " + nearbyBoardingHouses.size());
            
            // Mark both as filtered and update together
            recommendedFiltered = true;
            nearbyFiltered = true;
            updateBothSections();
            return;
        }
        
        // Filter recommended: same province as boarder (fast - no geocoding needed)
        for (Listing bh : allBoardingHouses) {
            String bhAddress = bh.getBhAddress();
            if (bhAddress == null || bhAddress.trim().isEmpty()) {
                continue;
            }
            
            // Parse boarding house address
            String[] bhParts = bhAddress.split(",");
            for (int i = 0; i < bhParts.length; i++) {
                bhParts[i] = bhParts[i].trim();
            }
            
            String bhProvince = "";
            
            if (bhParts.length > 0) {
                bhProvince = bhParts[bhParts.length - 1];
            }
            
            // Check if province matches (case-insensitive)
            boolean provinceMatch = boarderProvince != null && 
                                   !boarderProvince.isEmpty() && 
                                   bhProvince.equalsIgnoreCase(boarderProvince);
            
            // Recommended: same province as boarder
            if (provinceMatch) {
                recommendedBoardingHouses.add(bh);
            }
        }
        
        // If no recommended matches found, show all boarding houses
        if (recommendedBoardingHouses.isEmpty()) {
            recommendedBoardingHouses.addAll(allBoardingHouses);
            Log.d(TAG, "No recommended matches, showing all");
        }
        
        Log.d(TAG, "Filtered - Recommended: " + recommendedBoardingHouses.size());
        
        // Mark recommended as filtered, but don't update UI yet
        recommendedFiltered = true;
        
        // Filter nearby: within 5km radius (async geocoding)
        // Both sections will update together when nearby filtering completes
        filterNearbyBoardingHousesAsync();
    }
    
    private void saveOriginalLists() {
        // Save copies of the current filtered lists for search restoration
        if (originalRecommendedBoardingHouses == null) {
            originalRecommendedBoardingHouses = new ArrayList<>();
        } else {
            originalRecommendedBoardingHouses.clear();
        }
        originalRecommendedBoardingHouses.addAll(recommendedBoardingHouses);
        
        if (originalNearbyBoardingHouses == null) {
            originalNearbyBoardingHouses = new ArrayList<>();
        } else {
            originalNearbyBoardingHouses.clear();
        }
        originalNearbyBoardingHouses.addAll(nearbyBoardingHouses);
    }
    
    private void restoreOriginalLists() {
        // Restore original filtered lists when search is cleared
        if (originalRecommendedBoardingHouses != null && originalNearbyBoardingHouses != null) {
            recommendedBoardingHouses.clear();
            recommendedBoardingHouses.addAll(originalRecommendedBoardingHouses);
            
            nearbyBoardingHouses.clear();
            nearbyBoardingHouses.addAll(originalNearbyBoardingHouses);
            
            // Update adapters and empty states
            updateSearchResults();
            updateEmptyStates();
        }
    }
    
    private void filterBoardingHousesBySearch(String query) {
        if (recommendedBoardingHouses == null || nearbyBoardingHouses == null) {
            return;
        }
        
        // If search is empty, restore original lists
        if (query == null || query.trim().isEmpty()) {
            restoreOriginalLists();
            return;
        }
        
        // Ensure original lists are saved before searching
        if (originalRecommendedBoardingHouses == null || originalNearbyBoardingHouses == null ||
            originalRecommendedBoardingHouses.isEmpty()) {
            saveOriginalLists();
        }
        
        String lowerQuery = query.toLowerCase().trim();
        Log.d(TAG, "Searching with query: '" + lowerQuery + "'");
        
        // Filter recommended boarding houses
        List<Listing> filteredRecommended = new ArrayList<>();
        for (Listing bh : originalRecommendedBoardingHouses) {
            if (matchesSearch(bh, lowerQuery)) {
                filteredRecommended.add(bh);
            }
        }
        
        // Filter nearby boarding houses
        List<Listing> filteredNearby = new ArrayList<>();
        for (Listing bh : originalNearbyBoardingHouses) {
            if (matchesSearch(bh, lowerQuery)) {
                filteredNearby.add(bh);
            }
        }
        
        // Update the lists
        recommendedBoardingHouses.clear();
        recommendedBoardingHouses.addAll(filteredRecommended);
        
        nearbyBoardingHouses.clear();
        nearbyBoardingHouses.addAll(filteredNearby);
        
        Log.d(TAG, "Search results - Recommended: " + recommendedBoardingHouses.size() + 
                   ", Nearby: " + nearbyBoardingHouses.size());
        
        // Update adapters and empty states
        updateSearchResults();
        updateEmptyStates();
    }
    
    private boolean matchesSearch(Listing boardingHouse, String lowerQuery) {
        // Search by name
        if (boardingHouse.getBhName() != null && !boardingHouse.getBhName().isEmpty()) {
            String name = boardingHouse.getBhName().toLowerCase();
            if (name.contains(lowerQuery)) {
                return true;
            }
        }
        
        // Search by address/location
        if (boardingHouse.getBhAddress() != null && !boardingHouse.getBhAddress().isEmpty()) {
            String address = boardingHouse.getBhAddress().toLowerCase();
            if (address.contains(lowerQuery)) {
                return true;
            }
        }
        
        return false;
    }
    
    private void updateSearchResults() {
        // Check if fragment is still attached before updating UI
        if (!isAdded() || getContext() == null) {
            Log.d(TAG, "Fragment not attached, skipping search results update");
            return;
        }
        
        // Update adapters on main thread
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (!isAdded() || getContext() == null) {
                    return;
                }
                
            if (recommendedAdapter != null) {
                recommendedAdapter.notifyDataSetChanged();
            }
            if (nearbyAdapter != null) {
                nearbyAdapter.notifyDataSetChanged();
            }
            });
        } else {
            // Fallback: update directly if on main thread
            if (recommendedAdapter != null) {
                recommendedAdapter.notifyDataSetChanged();
            }
            if (nearbyAdapter != null) {
                nearbyAdapter.notifyDataSetChanged();
            }
        }
    }
    
    private void updateEmptyStates() {
        // Check if fragment is still attached before updating UI
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        // Update empty states on main thread
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (!isAdded() || getContext() == null) {
                    return;
                }
                
                // Check if there's an active search
                String searchQuery = etSearch != null && etSearch.getText() != null ? 
                                   etSearch.getText().toString().trim() : "";
                boolean hasActiveSearch = !searchQuery.isEmpty();
                
                // Update recommended empty state - only show when search is active and no results
                if (tvRecommendedEmpty != null && rvRecommendedBH != null) {
                    if (hasActiveSearch && recommendedBoardingHouses != null && recommendedBoardingHouses.isEmpty()) {
                        // Search is active and no results, show empty message
                        tvRecommendedEmpty.setVisibility(View.VISIBLE);
                        rvRecommendedBH.setVisibility(View.GONE);
                    } else {
                        // Either no search or there are results, hide empty message and show RecyclerView if there's data
                        tvRecommendedEmpty.setVisibility(View.GONE);
                        if (recommendedBoardingHouses != null && !recommendedBoardingHouses.isEmpty()) {
                            rvRecommendedBH.setVisibility(View.VISIBLE);
                        } else if (!hasActiveSearch) {
                            // No search active and no data - keep RecyclerView hidden (normal state)
                            rvRecommendedBH.setVisibility(View.GONE);
                        }
                    }
                }
                
                // Update nearby empty state - only show when search is active and no results
                if (tvNearbyEmpty != null && rvNearbyBH != null) {
                    if (hasActiveSearch && nearbyBoardingHouses != null && nearbyBoardingHouses.isEmpty()) {
                        // Search is active and no results, show empty message
                        tvNearbyEmpty.setVisibility(View.VISIBLE);
                        rvNearbyBH.setVisibility(View.GONE);
                    } else {
                        // Either no search or there are results, hide empty message and show RecyclerView if there's data
                        tvNearbyEmpty.setVisibility(View.GONE);
                        if (nearbyBoardingHouses != null && !nearbyBoardingHouses.isEmpty()) {
                            rvNearbyBH.setVisibility(View.VISIBLE);
                        } else if (!hasActiveSearch) {
                            // No search active and no data - keep RecyclerView hidden (normal state)
                            rvNearbyBH.setVisibility(View.GONE);
                        }
                    }
                }
            });
        }
    }
    
    private void filterNearbyBoardingHousesAsync() {
        if (boarderLatitude == null || boarderLongitude == null) {
            // Fallback: use same province AND municipality if geocoding not available
            filterNearbyByMunicipality();
            nearbyFiltered = true;
            // Update both sections together now that both are ready
            updateBothSections();
            return;
        }
        
        // Create a snapshot of the list to avoid ConcurrentModificationException
        List<Listing> boardingHousesSnapshot;
        synchronized (allBoardingHouses) {
            boardingHousesSnapshot = new ArrayList<>(allBoardingHouses);
        }
        
        if (boardingHousesSnapshot.isEmpty()) {
            Log.d(TAG, "No boarding houses to filter for nearby");
            return;
        }
        
        // Filter nearby boarding houses in background thread
        new Thread(() -> {
            List<Listing> nearbyList = new ArrayList<>();
            
            for (Listing bh : boardingHousesSnapshot) {
                String bhAddress = bh.getBhAddress();
                if (bhAddress == null || bhAddress.trim().isEmpty()) {
                    continue;
                }
                
                double distance = calculateDistance(bhAddress);
                if (distance <= NEARBY_RADIUS_KM) {
                    nearbyList.add(bh);
                    Log.d(TAG, "BH " + bh.getBhName() + " is within " + String.format("%.2f", distance) + " km");
                }
            }
            
            // Update UI on main thread
            new Handler(Looper.getMainLooper()).post(() -> {
                // Check if fragment is still attached before updating UI
                if (!isAdded() || getContext() == null) {
                    Log.d(TAG, "Fragment not attached, skipping nearby filtering update");
                    return;
                }
                
        nearbyBoardingHouses.clear();
                if (!nearbyList.isEmpty()) {
                    nearbyBoardingHouses.addAll(nearbyList);
                } else {
                    // If no matches found, show all boarding houses from snapshot
                    synchronized (allBoardingHouses) {
                        nearbyBoardingHouses.addAll(new ArrayList<>(allBoardingHouses));
                    }
                    Log.d(TAG, "No nearby matches within 5km, showing all");
                }
                
                Log.d(TAG, "Nearby filtered: " + nearbyBoardingHouses.size());
                nearbyFiltered = true;
                
                // Update both sections together now that both are ready
                // saveOriginalLists() will be called in updateBothSections()
                updateBothSections();
            });
        }).start();
    }
    
    private void filterNearbyByMunicipality() {
        // Fallback: if geocoding failed, use same province AND municipality
        nearbyBoardingHouses.clear();
        
        for (Listing bh : allBoardingHouses) {
            String bhAddress = bh.getBhAddress();
            if (bhAddress == null || bhAddress.trim().isEmpty()) {
                continue;
            }
            
            // Parse boarding house address
            String[] bhParts = bhAddress.split(",");
            for (int i = 0; i < bhParts.length; i++) {
                bhParts[i] = bhParts[i].trim();
            }
            
            String bhProvince = "";
            String bhMunicipality = "";
            
            if (bhParts.length > 0) {
                bhProvince = bhParts[bhParts.length - 1];
            }
            if (bhParts.length > 1) {
                bhMunicipality = bhParts[bhParts.length - 2];
            }
            
            boolean provinceMatch = boarderProvince != null && 
                                   !boarderProvince.isEmpty() && 
                                   bhProvince.equalsIgnoreCase(boarderProvince);
            
            boolean municipalityMatch = boarderMunicipality != null && 
                                       !boarderMunicipality.isEmpty() && 
                                       bhMunicipality.equalsIgnoreCase(boarderMunicipality);
            
            if (provinceMatch && municipalityMatch) {
                nearbyBoardingHouses.add(bh);
            }
        }
        
        if (nearbyBoardingHouses.isEmpty()) {
            nearbyBoardingHouses.addAll(allBoardingHouses);
            Log.d(TAG, "No nearby matches, showing all");
        }
        
        Log.d(TAG, "Nearby filtered (by municipality): " + nearbyBoardingHouses.size());
        
        // Save original lists will be called in updateBothSections()
    }
    
    private void updateBothSections() {
        // Check if fragment is still attached before updating UI
        if (!isAdded() || getContext() == null) {
            Log.d(TAG, "Fragment not attached, skipping section update");
            return;
        }
        
        // Only update when both sections are ready
        if (recommendedFiltered && nearbyFiltered) {
            Log.d(TAG, "Both sections ready, updating UI");
            
            // Save original lists once after both sections are fully filtered (for search functionality)
            if (originalRecommendedBoardingHouses == null || originalRecommendedBoardingHouses.isEmpty()) {
                saveOriginalLists();
            }
            
            // Update adapters
            if (recommendedAdapter != null) {
                recommendedAdapter.notifyDataSetChanged();
                // Ensure RecyclerView scrolls to start (left side) after data update
                if (rvRecommendedBH != null) {
                    rvRecommendedBH.post(() -> rvRecommendedBH.scrollToPosition(0));
                }
            }
            if (nearbyAdapter != null) {
                nearbyAdapter.notifyDataSetChanged();
            }
            
            // Hide progress bars and show RecyclerViews together
            hideProgressBars();
            
            // Show RecyclerViews if they have data, otherwise they'll remain hidden
            // Empty states will be shown by updateEmptyStates() if needed (when search is active)
            if (rvRecommendedBH != null && recommendedBoardingHouses != null && !recommendedBoardingHouses.isEmpty()) {
                rvRecommendedBH.setVisibility(View.VISIBLE);
                // Ensure RecyclerView scrolls to start (left side) when shown - this makes first item stick to left
                rvRecommendedBH.post(() -> {
                    if (rvRecommendedBH != null) {
                        rvRecommendedBH.scrollToPosition(0);
                    }
                });
            }
            if (rvNearbyBH != null && nearbyBoardingHouses != null && !nearbyBoardingHouses.isEmpty()) {
                rvNearbyBH.setVisibility(View.VISIBLE);
            }
            
            // Update empty states in case there's an active search
            updateEmptyStates();
        } else {
            Log.d(TAG, "Waiting for both sections - Recommended: " + recommendedFiltered + 
                       ", Nearby: " + nearbyFiltered);
        }
    }
    
    private void geocodeBoarderAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            Log.e(TAG, "Cannot geocode empty address");
            return;
        }
        
        // Use background thread for geocoding
        new Thread(() -> {
            try {
                // Check if fragment is still attached before geocoding
                if (getContext() == null || !isAdded()) {
                    Log.d(TAG, "Cannot geocode - fragment not attached or context is null");
                    return;
                }
                
                Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(address, 1);
                
                if (addresses != null && !addresses.isEmpty()) {
                    Address addressObj = addresses.get(0);
                    boarderLatitude = addressObj.getLatitude();
                    boarderLongitude = addressObj.getLongitude();
                    
                    // Update UI on main thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        // Check if fragment is still attached before updating UI
                        if (!isAdded() || getContext() == null) {
                            Log.d(TAG, "Fragment not attached, skipping coordinate update");
                            return;
                        }
                        
                        Log.d(TAG, "Boarder coordinates: " + boarderLatitude + ", " + boarderLongitude);
                        // Re-filter only nearby boarding houses with coordinates (recommended already filtered)
                        if (allBoardingHouses != null && !allBoardingHouses.isEmpty()) {
                            // Reset nearby flag since we're re-filtering
                            nearbyFiltered = false;
                            filterNearbyBoardingHousesAsync();
                        } else if (recommendedFiltered) {
                            // If no boarding houses but recommended is already filtered, mark nearby as done too
                            nearbyFiltered = true;
                            updateBothSections();
                        }
                    });
                } else {
                    Log.e(TAG, "No coordinates found for boarder address: " + address);
                    // If geocoding fails, use fallback municipality-based filtering
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (isAdded() && getContext() != null && 
                            allBoardingHouses != null && !allBoardingHouses.isEmpty()) {
                            nearbyFiltered = false;
                            filterNearbyBoardingHousesAsync();
                        }
                    });
                }
            } catch (IOException e) {
                Log.e(TAG, "Geocoding error for boarder address: " + e.getMessage());
                // If geocoding fails, use fallback municipality-based filtering
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isAdded() && getContext() != null && 
                        allBoardingHouses != null && !allBoardingHouses.isEmpty()) {
                        nearbyFiltered = false;
                        filterNearbyBoardingHousesAsync();
                    }
                });
        } catch (Exception e) {
                Log.e(TAG, "Unexpected error geocoding boarder address: " + e.getMessage());
                // If geocoding fails, use fallback municipality-based filtering
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isAdded() && getContext() != null && 
                        allBoardingHouses != null && !allBoardingHouses.isEmpty()) {
                        nearbyFiltered = false;
                        filterNearbyBoardingHousesAsync();
                    }
                });
            }
        }).start();
    }
    
    private double calculateDistance(String destinationAddress) {
        if (destinationAddress == null || destinationAddress.trim().isEmpty()) {
            return Double.MAX_VALUE;
        }
        
        try {
            // Check if fragment is still attached before geocoding
            if (getContext() == null || !isAdded()) {
                Log.d(TAG, "Cannot calculate distance - fragment not attached or context is null");
                return Double.MAX_VALUE;
            }
            
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocationName(destinationAddress, 1);
            
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                double destLatitude = address.getLatitude();
                double destLongitude = address.getLongitude();
                
                return calculateDistanceInKm(boarderLatitude, boarderLongitude, destLatitude, destLongitude);
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoding error for destination: " + destinationAddress + " - " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error calculating distance: " + e.getMessage());
        }
        
        return Double.MAX_VALUE;
    }
    
    /**
     * Calculate distance between two points in kilometers using Haversine formula
     */
    private double calculateDistanceInKm(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_KM * c;
        
        return distance;
    }

    private void updateAdapters() {
        // This method is kept for backward compatibility
        // But now we use updateBothSections() instead
        updateBothSections();
    }
    
    private void showProgressBars() {
        if (progressBarRecommended != null) {
            progressBarRecommended.setVisibility(View.VISIBLE);
        }
        if (progressBarNearby != null) {
            progressBarNearby.setVisibility(View.VISIBLE);
        }
        if (rvRecommendedBH != null) {
            rvRecommendedBH.setVisibility(View.GONE);
        }
        if (rvNearbyBH != null) {
            rvNearbyBH.setVisibility(View.GONE);
        }
    }
    
    private void hideProgressBars() {
        if (progressBarRecommended != null) {
            progressBarRecommended.setVisibility(View.GONE);
        }
        if (progressBarNearby != null) {
            progressBarNearby.setVisibility(View.GONE);
        }
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
                            // Get notifications array and calculate unique unread count
                            org.json.JSONObject data = response.getJSONObject("data");
                            org.json.JSONArray notifications = data.getJSONArray("notifications");
                            
                            // Filter duplicates and count unique unread notifications
                            int uniqueUnreadCount = calculateUniqueUnreadCount(notifications);
                            android.util.Log.d("BoarderHomeFragment", "✅ Total notifications: " + notifications.length() + 
                                              ", Unique unread count: " + uniqueUnreadCount);
                            android.util.Log.d("BoarderHomeFragment", "=== Calling showNotificationBadge ===");
                            showNotificationBadge(uniqueUnreadCount);
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
    
    /**
     * Calculate unique unread notification count by filtering duplicates
     */
    private int calculateUniqueUnreadCount(org.json.JSONArray notifications) {
        int uniqueUnreadCount = 0;
        java.util.Set<String> seenNotifications = new java.util.HashSet<>();
        
        try {
            for (int i = 0; i < notifications.length(); i++) {
                org.json.JSONObject notif = notifications.getJSONObject(i);
                
                // Create unique key for duplicate detection (same logic as Notification.java)
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
        } catch (org.json.JSONException e) {
            android.util.Log.e("BoarderHomeFragment", "Error calculating unique unread count", e);
        }
        
        return uniqueUnreadCount;
    }
    
    /**
     * Create unique key for notification (same logic as Notification.java)
     */
    private String createNotificationUniqueKey(org.json.JSONObject notif) throws org.json.JSONException {
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
}