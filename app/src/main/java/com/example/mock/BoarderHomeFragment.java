package com.example.mock;

import android.content.Intent;
import android.os.Bundle;
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
    private RecyclerView rvRecommendedBH;
    private RecyclerView rvNearbyBH;
    private ProgressBar progressBarRecommended;
    private ProgressBar progressBarNearby;
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
    private List<Listing> allBoardingHouses;
    private List<Listing> recommendedBoardingHouses;
    private List<Listing> nearbyBoardingHouses;
    
    // Boarder info
    private String boarderFirstName;
    private String boarderLastName;
    private String boarderSuffix;
    private String boarderAddress;
    private String boarderProvince;
    private String boarderMunicipality;

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
        // Load boarder info first, then boarding houses
        loadBoarderInfo();
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
        // Refresh boarding houses data
        if (allBoardingHouses != null && allBoardingHouses.isEmpty()) {
            loadBoarderInfo();
        }
    }

    private void initializeViews(View view) {
        try {
            etSearch = view.findViewById(R.id.etSearch);
            rvRecommendedBH = view.findViewById(R.id.rvRecommendedBH);
            rvNearbyBH = view.findViewById(R.id.rvNearbyBH);
            progressBarRecommended = view.findViewById(R.id.progressBarRecommended);
            progressBarNearby = view.findViewById(R.id.progressBarNearby);
            btnSeeAll = view.findViewById(R.id.btnSeeAll);
            btnMyBookings = (MaterialCardView) view.findViewById(R.id.cardMyBookings);
            btnFavorites = (MaterialCardView) view.findViewById(R.id.cardFavorites);
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

    private void loadBoarderInfo() {
        String userId = Login.getCurrentUserId(getContext());
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID not found");
            // Load boarding houses anyway (without filtering)
            loadBoardingHouses();
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
                            loadBoardingHouses();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Volley error loading boarder info: " + error.getMessage());
                        // Load boarding houses anyway
                        loadBoardingHouses();
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
                            
                            parseBoardingHousesData(dataArray);
                            filterBoardingHouses();
                            updateAdapters();
                            
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            hideProgressBars();
                        } catch (Exception e) {
                            Log.e(TAG, "Unexpected error: " + e.getMessage());
                            hideProgressBars();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Volley error loading boarding houses: " + error.getMessage());
                        hideProgressBars();
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
            return;
        }
        
        // If no boarder address info, show all boarding houses in both sections
        if (boarderProvince == null || boarderProvince.isEmpty()) {
            Log.d(TAG, "No boarder address info, showing all boarding houses");
            recommendedBoardingHouses.addAll(allBoardingHouses);
            nearbyBoardingHouses.addAll(allBoardingHouses);
            Log.d(TAG, "Showing all - Recommended: " + recommendedBoardingHouses.size() + 
                       ", Nearby: " + nearbyBoardingHouses.size());
            return;
        }
        
        // Filter recommended: same province as boarder
        // Filter nearby: same province AND municipality as boarder (closer/nearby)
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
            
            // Check if province matches (case-insensitive)
            boolean provinceMatch = boarderProvince != null && 
                                   !boarderProvince.isEmpty() && 
                                   bhProvince.equalsIgnoreCase(boarderProvince);
            
            // Check if municipality matches (case-insensitive)
            boolean municipalityMatch = boarderMunicipality != null && 
                                       !boarderMunicipality.isEmpty() && 
                                       bhMunicipality.equalsIgnoreCase(boarderMunicipality);
            
            // Recommended: same province as boarder
            if (provinceMatch) {
                recommendedBoardingHouses.add(bh);
            }
            
            // Nearby: same province AND municipality (closer to boarder)
            if (provinceMatch && municipalityMatch) {
                nearbyBoardingHouses.add(bh);
            }
        }
        
        // If no matches found, show all boarding houses
        if (recommendedBoardingHouses.isEmpty()) {
            recommendedBoardingHouses.addAll(allBoardingHouses);
            Log.d(TAG, "No recommended matches, showing all");
        }
        if (nearbyBoardingHouses.isEmpty()) {
            nearbyBoardingHouses.addAll(allBoardingHouses);
            Log.d(TAG, "No nearby matches, showing all");
        }
        
        Log.d(TAG, "Filtered - Recommended: " + recommendedBoardingHouses.size() + 
                   ", Nearby: " + nearbyBoardingHouses.size());
    }

    private void updateAdapters() {
        if (recommendedAdapter != null) {
            recommendedAdapter.notifyDataSetChanged();
        }
        if (nearbyAdapter != null) {
            nearbyAdapter.notifyDataSetChanged();
        }
        
        // Hide progress bars and show RecyclerViews when data is loaded
        hideProgressBars();
        
        // Show RecyclerViews even if empty (they will show empty state)
        if (rvRecommendedBH != null) {
            rvRecommendedBH.setVisibility(View.VISIBLE);
        }
        if (rvNearbyBH != null) {
            rvNearbyBH.setVisibility(View.VISIBLE);
        }
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