package com.example.mock;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.content.Context;
import android.location.LocationManager;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;

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

import android.webkit.WebView;
import android.webkit.WebViewClient;

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
    private static final String BASE_URL = "https://boardease.calapebohol.com/";
    private static final String BOARDER_INFO_API = BASE_URL + "get_boarder_info.php";
    private static final String BOARDING_HOUSES_API = BASE_URL + "get_boarding_houses1.php";
    
    // Mapbox Configuration - Sync with Guest section for compatibility
    private static final String MAPBOX_ACCESS_TOKEN = "pk.eyJ1IjoibmFtem1hcDA0IiwiYSI6ImNtanhubnN3MzJncTMzZHFzNHc4azB2MWUifQ.84NPjWYDgq3i20GLhbFTtg"; 
    private static final String MAPBOX_GEOCODING_URL = "https://api.mapbox.com/geocoding/v5/mapbox.places/";

    // Views
    private EditText etSearch;
    private ImageView ivClearSearch;
    private RecyclerView rvRecommendedBH;
    private RecyclerView rvNearbyBH;
    private ProgressBar progressBarRecommended;
    private ProgressBar progressBarNearby;
    private TextView tvRecommendedEmpty, tvNearbyEmpty;
    private MaterialButton btnSeeAll;
    private ImageView ivNotification;
    private ImageView ivMessage;
    private View badgeMsg;
    private TextView badgeCount;
    private View badgeNotif;
    private TextView badgeNotifCount;
    private SwipeRefreshLayout swipeRefreshLayout;
    private com.google.android.material.card.MaterialCardView cardVerificationStatus;
    private TextView tvStatusTitle, tvStatusMessage;
    private View viewStatusAccent;
    private ImageView ivStatusIcon;

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
    
    // Dynamic Search Center (User chosen location)
    private Double searchCenterLat;
    private Double searchCenterLon;
    private String currentSearchLocationName;
    
    // UI for dynamic location
    private LinearLayout layoutLocation;
    private TextView tvCurrentSearchLocation;
    
    private void setupSearch(View view) {
        etSearch = view.findViewById(R.id.etSearch);
        // Make EditText behave like a button
        etSearch.setFocusable(false);
        etSearch.setClickable(true);
        etSearch.setLongClickable(false);
        etSearch.setInputType(0); // Disable soft keyboard
        
        ivClearSearch = view.findViewById(R.id.ivClearSearch);
        btnSeeAll = view.findViewById(R.id.btnSeeAll);
        
        ivNotification = view.findViewById(R.id.ivNotification);
        ivMessage = view.findViewById(R.id.ivMessage);
        badgeMsg = view.findViewById(R.id.badgeMsg);
        badgeCount = view.findViewById(R.id.badgeCount);
        badgeNotif = view.findViewById(R.id.badgeNotif);
        badgeNotifCount = view.findViewById(R.id.badgeNotifCount);
    }
    
    // Flags to track filtering completion
    private boolean recommendedFiltered = false;
    private boolean nearbyFiltered = false;
    
    // Flag to track if data has been loaded (to prevent reloading on navigation)
    private boolean dataLoaded = false;
    
    // Flag to track if we should refresh data (set when activity pauses while fragment is visible)
    private boolean shouldRefreshOnResume = false;
    
    private void loadUserInfoFromSession() {
        android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        boarderFirstName = prefs.getString("user_first_name", "Guest");
        boarderLastName = prefs.getString("user_last_name", "");
        boarderAddress = prefs.getString("user_address", "");
        boarderProvince = prefs.getString("user_province", ""); // Assuming these are saved or parsed from address
        boarderMunicipality = prefs.getString("user_municipality", ""); 
        
        // For incomplete profiles, address might be empty, so we can set defaults or just hide location logic
        if (boarderAddress.isEmpty()) {
            boarderAddress = "Bohol, Philippines"; // Default
        }
    }
    
    // Real-time Location
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isRealTimeLocationEnabled = false;
    private boolean isNewLogin = false;
    
    private androidx.activity.result.ActivityResultLauncher<String> requestNotificationPermissionLauncher;

    // Polling specifics
    private android.os.Handler refreshHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable refreshRunnable;
    private static final long REFRESH_INTERVAL = 2000; // 2 seconds (faster updates)

    public BoarderHomeFragment() {
        // Required empty public constructor
    }

    public static BoarderHomeFragment newInstance(boolean isNewLogin) {
        BoarderHomeFragment fragment = new BoarderHomeFragment();
        Bundle args = new Bundle();
        args.putBoolean("IS_NEW_LOGIN", isNewLogin);
        fragment.setArguments(args);
        return fragment;
    }
    
    // Maintain default constructor for system recreation
    public static BoarderHomeFragment newInstance() {
        return new BoarderHomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        
        // Restore dataLoaded flag if fragment was recreated
        if (savedInstanceState != null) {
            dataLoaded = savedInstanceState.getBoolean("dataLoaded", false);
            isNewLogin = savedInstanceState.getBoolean("isNewLogin", false);
        } else if (getArguments() != null) {
            isNewLogin = getArguments().getBoolean("IS_NEW_LOGIN", false);
        }
        
        // Initialize Notification Permission Launcher
        requestNotificationPermissionLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
            isGranted -> {
                // After notification permission decision (allow or deny), proceed to Location Permission
                checkLocationPermission();
            }
        );
    }
    
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save dataLoaded flag to prevent reloading after recreation
        outState.putBoolean("dataLoaded", dataLoaded);
        outState.putBoolean("isNewLogin", isNewLogin);
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
        if (layoutLocation == null) {
            // Initialize search UI
            layoutLocation = view.findViewById(R.id.layoutLocation);
            tvCurrentSearchLocation = view.findViewById(R.id.tvCurrentSearchLocation);
            
            // Load user info from session immediately
            loadUserInfoFromSession();
            
            // Setup Search
            setupSearch(view);
            // Views not initialized yet, initialize them
        initializeViews(view);
        setupRecyclerViews();
        setupClickListeners();
        
        cardVerificationStatus = view.findViewById(R.id.cardVerificationStatus);
        tvStatusTitle = view.findViewById(R.id.tvStatusTitle);
        tvStatusMessage = view.findViewById(R.id.tvStatusMessage);
        viewStatusAccent = view.findViewById(R.id.viewStatusAccent);
        ivStatusIcon = view.findViewById(R.id.ivStatusIcon);
        
        checkUserStatus();
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
        loadUnreadCount();
        loadNotificationCount();
        
        // Start real-time location tracking if possible
        checkLocationPermissionAndStartTracking();
    }
    
    private void checkLocationPermissionAndStartTracking() {
        // Step 1: Request Notification Permission (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return; // Wait for callback
            }
        }
        
        // Step 2: If notification not needed or already granted, proceed to Location
        checkLocationPermission();
    }
    
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            // Request permission
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1002) { // GPS Enable Request
            if (resultCode == android.app.Activity.RESULT_OK) {
                // User agreed to make required location settings changes
                Toast.makeText(getContext(), "GPS enabled", Toast.LENGTH_SHORT).show();
                getCurrentLocation();
            } else {
                Toast.makeText(getContext(), "GPS required for location features", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getCurrentLocation() {
        if (!isAdded() || getContext() == null) return;
        
        if (androidx.core.app.ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Check if location services are enabled
        LocationManager lm = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = false;
        boolean networkEnabled = false;

        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {
            Log.e(TAG, "Error checking GPS status", ex);
        }

        try {
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {
            Log.e(TAG, "Error checking Network status", ex);
        }

        if (!gpsEnabled && !networkEnabled) {
            Log.w(TAG, "Location services are disabled, prompting user to enable them");
            turnOnGPS();
            return;
        }

        Toast.makeText(getContext(), "Syncing location...", Toast.LENGTH_SHORT).show();

        // Try getting a fresh location first
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null && isAdded()) {
                        Log.d(TAG, "Fresh GPS location acquired: " + location.getLatitude() + ", " + location.getLongitude());
                        onLocationReceived(location);
                    } else {
                        Log.w(TAG, "Fresh location is null, trying last known location fallback");
                        tryLastKnownLocation();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get fresh location: " + e.getMessage());
                    tryLastKnownLocation();
                });
    }

    private void turnOnGPS() {
        com.google.android.gms.location.LocationRequest locationRequest = new com.google.android.gms.location.LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(500)
                .setMaxUpdateDelayMillis(1000)
                .build();

        com.google.android.gms.location.LocationSettingsRequest.Builder builder = new com.google.android.gms.location.LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        
        builder.setAlwaysShow(true);

        com.google.android.gms.tasks.Task<com.google.android.gms.location.LocationSettingsResponse> result =
                LocationServices.getSettingsClient(requireContext()).checkLocationSettings(builder.build());

        result.addOnCompleteListener(new com.google.android.gms.tasks.OnCompleteListener<com.google.android.gms.location.LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull com.google.android.gms.tasks.Task<com.google.android.gms.location.LocationSettingsResponse> task) {
                try {
                    com.google.android.gms.location.LocationSettingsResponse response = task.getResult(com.google.android.gms.common.api.ApiException.class);
                    // All location settings are satisfied. The client can initialize location requests here.
                    Toast.makeText(getContext(), "GPS is already enabled", Toast.LENGTH_SHORT).show();
                    getCurrentLocation();
                } catch (com.google.android.gms.common.api.ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case com.google.android.gms.common.api.CommonStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the user a dialog.
                            try {
                                // Cast to a resolvable exception.
                                com.google.android.gms.common.api.ResolvableApiException resolvable = (com.google.android.gms.common.api.ResolvableApiException) exception;
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                startIntentSenderForResult(resolvable.getResolution().getIntentSender(), 1002, null, 0, 0, 0, null);
                            } catch (android.content.IntentSender.SendIntentException e) {
                                // Ignore the error.
                            } catch (Exception e) {
                                // Ignore
                            }
                            break;
                        case com.google.android.gms.location.LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            Toast.makeText(getContext(), "Please enable GPS in settings", Toast.LENGTH_LONG).show();
                            break;
                    }
                }
            }
        });
    }

    private void tryLastKnownLocation() {
        if (!isAdded() || getContext() == null) return;
        
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null && isAdded()) {
                        Log.d(TAG, "Using last known location as fallback: " + location.getLatitude() + ", " + location.getLongitude());
                        Toast.makeText(getContext(), "Using last known location", Toast.LENGTH_SHORT).show();
                        onLocationReceived(location);
                    } else {
                        Log.w(TAG, "Last known location is also null");
                        Toast.makeText(getContext(), "Unable to get location. Please move to a clearer spot or check GPS settings.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Last location fetch failed: " + e.getMessage());
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void onLocationReceived(Location location) {
        if (!isAdded()) return;
        
        searchCenterLat = location.getLatitude();
        searchCenterLon = location.getLongitude();
        isRealTimeLocationEnabled = true;
        
        if (getContext() != null) {
            Toast.makeText(getContext(), "Location updated!", Toast.LENGTH_SHORT).show();
        }
        
        // Reverse geocode to get the address name
        reverseGeocode(searchCenterLat, searchCenterLon);
        
        // Refresh listings based on real location
        if (allBoardingHouses != null) {
            filterNearbyBoardingHousesAsync();
        }
        
        // Show location selection dialog ONLY if this is a fresh login (and only once)
        if (isNewLogin) {
            showLocationSelectionDialog();
            isNewLogin = false; // Disable for future updates
        }
    }

    private void reverseGeocode(double lat, double lon) {
        // Specifically ask for neighborhood (Barangay), address, and locality to get more detail
        String url = MAPBOX_GEOCODING_URL + lon + "," + lat + ".json?access_token=" + MAPBOX_ACCESS_TOKEN + 
                     "&types=neighborhood,address,locality,place&limit=1";
        
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        JSONArray features = jsonResponse.getJSONArray("features");
                        if (features.length() > 0) {
                            String address = features.getJSONObject(0).getString("place_name");
                            
                            // Clean up address: remove country and zip code more robustly
                            if (address.contains(",")) {
                                String[] parts = address.split(",");
                                if (parts.length > 1) {
                                    StringBuilder sb = new StringBuilder();
                                    
                                    // Normally the last part is the country
                                    int lastIndexToInclude = parts.length - 1;
                                    
                                    // If last part is a country or country-like, exclude it
                                    String lastPart = parts[parts.length - 1].trim();
                                    if (lastPart.equalsIgnoreCase("Philippines") || lastPart.length() > 15) {
                                        lastIndexToInclude--;
                                    }
                                    
                                    // If the new last part is a zip code, exclude it too
                                    if (lastIndexToInclude > 0) {
                                        String maybeZip = parts[lastIndexToInclude].trim();
                                        if (maybeZip.matches("\\d{4}")) {
                                            lastIndexToInclude--;
                                        }
                                    }
                                    
                                    for (int i = 0; i <= lastIndexToInclude; i++) {
                                        sb.append(parts[i].trim());
                                        if (i < lastIndexToInclude) sb.append(", ");
                                    }
                                    address = sb.toString();
                                }
                            }
                            
                            currentSearchLocationName = address;
                            if (tvCurrentSearchLocation != null) {
                                tvCurrentSearchLocation.setText(address);
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing reverse geocode response", e);
                    }
                },
                error -> Log.e(TAG, "Reverse geocoding failed", error));
        
        Volley.newRequestQueue(requireContext()).add(request);
    }
    


    @Override
    public void onResume() {
        super.onResume();
        android.util.Log.d("BoarderHomeFragment", "=== onResume called ===");
        
        // Force update UI adapters to reflect favorite changes immediately (from Details screen returns)
        // This is necessary because the DATA list might not have changed, but local favorites state has
        if (dataLoaded) {
            if (nearbyAdapter != null) nearbyAdapter.notifyDataSetChanged();
            if (recommendedAdapter != null) recommendedAdapter.notifyDataSetChanged();
        }

        // Always refresh and start polling when visible
        if (isVisible()) {
            if (dataLoaded) {
                refreshDataSilently();
            } else {
                refreshData();
            }
            startPolling();
        }
        
        // Always refresh badge counts when fragment becomes visible
        loadUnreadCount();
        loadNotificationCount();
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
            // Fragment became visible (e.g. tab switch)
            // Force update UI for favorites
            if (dataLoaded) {
                if (nearbyAdapter != null) nearbyAdapter.notifyDataSetChanged();
                if (recommendedAdapter != null) recommendedAdapter.notifyDataSetChanged();
                refreshDataSilently();
            } else {
                refreshData();
            }
            startPolling();
        } else {
            // Fragment hidden
            stopPolling();
        }
    }
    
    private void startPolling() {
        if (refreshRunnable == null) {
            refreshRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isVisible()) {
                        Log.d(TAG, "Polling: Refreshing home data...");
                        refreshDataSilently();
                        refreshHandler.postDelayed(this, REFRESH_INTERVAL);
                    }
                }
            };
        }
        refreshHandler.removeCallbacks(refreshRunnable);
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
    }
    
    private void stopPolling() {
        if (refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }
    
    private void refreshData() {
        // Reset flags and reload
        dataLoaded = false;
        recommendedFiltered = false;
        nearbyFiltered = false;
        
        // We don't clear lists immediately to avoid blinking, we'll clear them when new data arrives if needed
        // or just let the adapter update handle it
        
        loadBoarderInfo();
        loadUnreadCount();
        loadNotificationCount();
    }
    
    private void refreshDataSilently() {
        // Background fetch without indicators
        loadBoarderInfoSilently();
        
        // Also update counts (these are small/fast enough to just run)
        loadUnreadCount();
        loadNotificationCount();
    }
    
    private void loadBoarderInfoSilently() {
         String userId = Login.getCurrentUserId(getContext());
        if (userId == null || userId.isEmpty()) {
            loadBoardingHousesSilently();
            return;
        }

        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        String url = BOARDER_INFO_API + "?user_id=" + userId;

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Just parse basics roughly or skip straight to boarding houses
                        // We primarily care about boarding houses lists updating
                        if (isAdded() && getContext() != null) {
                            loadBoardingHousesSilently();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (isAdded() && getContext() != null) {
                            loadBoardingHousesSilently();
                        }
                    }
                });
        requestQueue.add(stringRequest);
    }
    
    private void loadBoardingHousesSilently() {
        if (getContext() == null || !isAdded()) return;
        
        // NO progress bars
        
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        StringRequest stringRequest = new StringRequest(Request.Method.GET, BOARDING_HOUSES_API,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            if (response == null || response.trim().isEmpty()) return;
                            
                            JSONArray dataArray = null;
                             try {
                                JSONObject jsonResponse = new JSONObject(response);
                                if (jsonResponse.getBoolean("success")) {
                                    dataArray = jsonResponse.getJSONArray("data");
                                }
                            } catch (JSONException e) {
                                dataArray = new JSONArray(response);
                            }
                            
                            if (dataArray == null) return;
                            
                            // Parse into NEW list
                            List<Listing> newAllBoardingHouses = new ArrayList<>();
                            
                           for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject obj = dataArray.getJSONObject(i);
                                // Inline parsing or reuse (reusing is harder without refactor, inline for safety)
                                int bhId = obj.getInt("bh_id");
                                String bhName = obj.getString("bh_name");
                                String imagePath = obj.optString("image_path", "");
                                String address = obj.optString("bh_address", "");
                                String desc = obj.optString("bh_description", "");
                                String rules = obj.optString("bh_rules", "");
                                String baths = obj.optString("number_of_bathroom", "");
                                String area = obj.optString("area", "");
                                String year = obj.optString("build_year", "");
                                
                                Integer minP = null;
                                if (!obj.isNull("min_price")) minP = obj.optInt("min_price");
                                Integer maxP = null;
                                if (!obj.isNull("max_price")) maxP = obj.optInt("max_price");
                                
                                double rating = obj.optDouble("average_rating", 0.0);
                                if (rating == 0.0) rating = obj.optDouble("avg_rating", 0.0);
                                
                                ArrayList<String> paths = new ArrayList<>();
                                if (!imagePath.isEmpty()) paths.add(imagePath);
                                
                                Listing l = new Listing(bhId, bhName, address, desc, rules, baths, area, year, imagePath, paths, minP, maxP, rating);
                                newAllBoardingHouses.add(l);
                            }
                           
                           // DIFFING
                           boolean different = false;
                           if (allBoardingHouses == null || allBoardingHouses.size() != newAllBoardingHouses.size()) {
                               different = true;
                           } else {
                               for (int i = 0; i < allBoardingHouses.size(); i++) {
                                   if (!allBoardingHouses.get(i).equals(newAllBoardingHouses.get(i))) {
                                       different = true;
                                       break;
                                   }
                               }
                           }
                           
                           if (different) {
                               Log.d(TAG, "Silent Refresh: Home data changed, updating...");
                               allBoardingHouses.clear();
                               allBoardingHouses.addAll(newAllBoardingHouses);
                               
                               // Re-filter to update UI
                               // Use the existing filter logic which updates adapters, now elegantly SILENT
                               filterBoardingHouses(true); 
                               // Note: filterBoardingHouses() shows progress bars currently.
                               // We might want to make a 'silent' version of that too, but filtering is usually fast local op.
                               // The progress bar flicker from filterBoardingHouses might be okay or we can modify it.
                               // Ideally, we pass a flag to filterBoardingHouses(boolean silent).
                               // For now, let's just let it run. If it flickers, we fix that next.
                               // Actually, let's just create a quick silent filter call that just updates adapters directly
                               updateAdaptersSilently(); 
                           } else {
                               Log.d(TAG, "Silent Refresh: Home data same.");
                           }

                        } catch (Exception e) {
                            Log.e(TAG, "Silent refresh error: " + e.getMessage());
                        }
                    }
                },
                error -> {} // Ignore errors
        );
        requestQueue.add(stringRequest);
    }
    
    private void updateAdaptersSilently() {
        if (recommendedAdapter != null) recommendedAdapter.notifyDataSetChanged();
        if (nearbyAdapter != null) nearbyAdapter.notifyDataSetChanged();
    }

    private void initializeViews(View view) {
        try {
            swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
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
            MaterialButton btnViewMap = view.findViewById(R.id.btnViewMap);
            
            if (btnViewMap != null) {
                btnViewMap.setOnClickListener(v -> {
                    // Open full screen map with ALL boarding houses, but focus on NEARBY ones
                    if (allBoardingHouses != null && !allBoardingHouses.isEmpty()) {
                        // If nearby list is empty (e.g. no results nearby), focus on all instead
                        List<Listing> focusList = (nearbyBoardingHouses != null && !nearbyBoardingHouses.isEmpty()) 
                                                  ? nearbyBoardingHouses : allBoardingHouses;
                        openFullScreenMap(allBoardingHouses, focusList);
                    } else {
                        Toast.makeText(getContext(), "No boarding houses to show", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            ivNotification = view.findViewById(R.id.ivNotification);
            ivMessage = view.findViewById(R.id.ivMessage);
            
            // Dynamic Location Views
            layoutLocation = view.findViewById(R.id.layoutLocation);
            tvCurrentSearchLocation = view.findViewById(R.id.tvCurrentSearchLocation);
            
            if (layoutLocation != null) {
                layoutLocation.setOnClickListener(v -> showLocationSelectionDialog());
            }
            badgeMsg = view.findViewById(R.id.badgeMsg);
            badgeNotif = view.findViewById(R.id.badgeNotif);
            
            // Initially show progress bars and hide RecyclerViews
            if (progressBarRecommended != null) {
                progressBarRecommended.setVisibility(View.VISIBLE);
            }
            if (progressBarNearby != null) {
                progressBarNearby.setVisibility(View.VISIBLE);
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
        
        // Setup pull-to-refresh
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    // Reset flags to reload data
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
                    
                    // Reload data
                    loadBoarderInfo();
                    
                    // Refresh badge counts
                    loadUnreadCount();
                    loadNotificationCount();
                }
            });
        }
        
        // Search functionality
        // Search functionality - Open SearchActivity
        if (etSearch != null) {
            etSearch.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(getContext(), SearchActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Error opening search", Toast.LENGTH_SHORT).show();
                }
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
                    if (Login.checkFeatureAccess(getContext())) {
                        android.util.Log.d("BoarderHomeFragment", "Message icon clicked!");
                        try {
                            Intent intent = new Intent(getContext(), Messages.class);
                            startActivity(intent);
                        } catch (Exception e) {
                            android.util.Log.e("BoarderHomeFragment", "Error starting Messages activity", e);
                            Toast.makeText(getContext(), "Error opening messages: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
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
                    if (Login.checkFeatureAccess(getContext())) {
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

        // Get email from session to support robust lookup for incomplete profiles
        android.content.SharedPreferences prefs = getContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String email = prefs.getString("user_email", "");

        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        String url = BOARDER_INFO_API + "?user_id=" + userId + "&email=" + email;

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
                                
                                
                                // Geocode boarder address to get coordinates for distance calculation
                                geocodeBoarderAddress(boarderAddress);
                                
                                // AUTO-PROMPT: If we don't have a specific location name yet, prompt user
                                // This happens after boarder info is loaded
                                if (currentSearchLocationName == null || currentSearchLocationName.isEmpty()) {
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        if (isAdded() && getContext() != null) {
                                            showLocationSelectionDialog();
                                        }
                                    }, 2000); // Wait 2s for UI to settle
                                }
                                
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
            
            // Parse average rating (check both keys)
            double averageRating = boardingHouseJson.optDouble("average_rating", 0.0);
            if (averageRating == 0.0) {
                averageRating = boardingHouseJson.optDouble("avg_rating", 0.0);
            }
            if (averageRating == 0.0) {
               // Try string parsing if optDouble failed
               String ratingStr = boardingHouseJson.optString("average_rating");
               if (ratingStr.isEmpty()) ratingStr = boardingHouseJson.optString("avg_rating");
               if (!ratingStr.isEmpty()) {
                   try {
                       averageRating = Double.parseDouble(ratingStr);
                   } catch (NumberFormatException e) {
                       averageRating = 0.0;
                   }
               }
            }
            
            // Create image paths list
            ArrayList<String> imagePaths = new ArrayList<>();
            if (imagePath != null && !imagePath.isEmpty()) {
                imagePaths.add(imagePath);
            }
            
            // Create Listing object with full details
            Listing boardingHouse = new Listing(
                bhId, bhName, bhAddress, bhDescription, bhRules,
                bhBathrooms, area, buildYear, imagePath, imagePaths, minPrice, maxPrice, averageRating
            );
            
            allBoardingHouses.add(boardingHouse);
        }
        
        Log.d(TAG, "Loaded " + allBoardingHouses.size() + " boarding houses from API");
    }

    private void filterBoardingHouses() {
        filterBoardingHouses(false);
    }

    private void filterBoardingHouses(boolean silent) {
        recommendedBoardingHouses.clear();
        nearbyBoardingHouses.clear();
        
        if (allBoardingHouses == null || allBoardingHouses.isEmpty()) {
            Log.d(TAG, "No boarding houses to filter");
            recommendedFiltered = true;
            nearbyFiltered = true;
            updateBothSections(silent);
            return;
        }
        
        // Mark as starting filtering
        recommendedFiltered = false;
        nearbyFiltered = false;
        
        if (!silent) {
            showProgressBars();
        }

        // Both Recommended and Nearby will now be handled inside filterNearbyBoardingHousesAsync
        // which uses the more accurate Mapbox-based proximity filtering
        filterNearbyBoardingHousesAsync(silent);
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
                
                // Update recommended empty state
                if (tvRecommendedEmpty != null && rvRecommendedBH != null) {
                    if (recommendedBoardingHouses != null && recommendedBoardingHouses.isEmpty()) {
                        // Empty - show message
                        tvRecommendedEmpty.setVisibility(View.VISIBLE);
                        tvRecommendedEmpty.setText("No recommended boarding houses found in this area.");
                        rvRecommendedBH.setVisibility(View.GONE);
                    } else {
                        // Not empty - show RecyclerView
                        tvRecommendedEmpty.setVisibility(View.GONE);
                        rvRecommendedBH.setVisibility(View.VISIBLE);
                    }
                }
                
                // Update nearby empty state
                if (tvNearbyEmpty != null && rvNearbyBH != null) {
                    if (nearbyBoardingHouses != null && nearbyBoardingHouses.isEmpty()) {
                        // Empty - show message
                        tvNearbyEmpty.setVisibility(View.VISIBLE);
                        tvNearbyEmpty.setText("No boarding houses found nearby.");
                        rvNearbyBH.setVisibility(View.GONE);
                    } else {
                        // Not empty - show RecyclerView
                        tvNearbyEmpty.setVisibility(View.GONE);
                        rvNearbyBH.setVisibility(View.VISIBLE);
                    }
                }
                
                // Adjust alignment for single-item results
                adjustRecommendedRecyclerViewLayout();
            });
        }
    }
    
    /**
     * Dynamically adjusts the Recommended RecyclerView layout parameters.
     * If there is only one item, it centers it. If there are more, it uses standard carousel layout.
     */
    private void adjustRecommendedRecyclerViewLayout() {
        if (rvRecommendedBH == null || recommendedBoardingHouses == null || !isAdded()) {
            return;
        }

        ViewGroup.LayoutParams layoutParams = rvRecommendedBH.getLayoutParams();
        if (layoutParams instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) layoutParams;
            float scale = getResources().getDisplayMetrics().density;
            if (recommendedBoardingHouses.size() == 1) {
                // Center the single card
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.gravity = android.view.Gravity.CENTER_HORIZONTAL;
                
                // Add 12dp start padding to balance the 12dp marginEnd in the item layout
                int compensationPadding = (int) (12 * scale + 0.5f);
                rvRecommendedBH.setPadding(compensationPadding, 0, 0, 0);
            } else {
                // Standard carousel behavior (sticks to left, scrolls right)
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.gravity = android.view.Gravity.START;
                
                // Restore standard padding (16dp paddingEnd from XML, 0 start)
                int paddingEnd = (int) (16 * scale + 0.5f);
                rvRecommendedBH.setPadding(0, 0, paddingEnd, 0);
            }
            rvRecommendedBH.setLayoutParams(params);
        }
    }
    
    private void filterNearbyBoardingHousesAsync() {
        filterNearbyBoardingHousesAsync(false);
    }

    private void filterNearbyBoardingHousesAsync(boolean silent) {
        // Clear original lists to ensure they are re-captured for the new location
        originalRecommendedBoardingHouses = null;
        originalNearbyBoardingHouses = null;

        // ALWAYS use searchCenter coordinates if available, otherwise fallback to permanent address
        Double targetLat = (searchCenterLat != null) ? searchCenterLat : boarderLatitude;
        Double targetLon = (searchCenterLon != null) ? searchCenterLon : boarderLongitude;

        if (targetLat == null || targetLon == null) {
            Log.d(TAG, "No coordinates for nearby filtering (targetLat/Lon is null)");
            // Fallback: use same province AND municipality if geocoding not available
            filterNearbyByMunicipality(silent);
            nearbyFiltered = true;
            updateBothSections(silent);
            return;
        }
        
        // Final coordinates to use for calculations
        final double lat = targetLat;
        final double lon = targetLon;
        
        Log.d(TAG, "Filtering nearby BHs near: [" + lat + ", " + lon + "]");

        // Create a snapshot of the list to avoid ConcurrentModificationException
        List<Listing> boardingHousesSnapshot;
        synchronized (allBoardingHouses) {
            boardingHousesSnapshot = new ArrayList<>(allBoardingHouses);
        }
        
        if (boardingHousesSnapshot.isEmpty()) {
            Log.d(TAG, "No boarding houses to filter for nearby");
            nearbyFiltered = true;
            updateBothSections(silent);
            return;
        }
        
        // Filter nearby boarding houses using Mapbox (sequential async)
        new Handler(Looper.getMainLooper()).post(() -> {
            processNextNearbyBH(new ArrayList<>(boardingHousesSnapshot), 0, new ArrayList<>(), lat, lon, silent);
        });
    }


    private void processNextNearbyBH(List<Listing> list, int index, List<Listing> nearbyList, double targetLat, double targetLon, boolean silent) {
        if (!isAdded() || getContext() == null || index >= list.size()) {
            // Done processing all items
            Log.d(TAG, "Finished nearby filtering. Found in range: " + nearbyList.size());
            
            nearbyBoardingHouses.clear();
            nearbyBoardingHouses.addAll(nearbyList);
            
            // RECOMMENDED logic: same items as nearby but must have a rating > 0
            recommendedBoardingHouses.clear();
            for (Listing bh : nearbyList) {
                if (bh.getAverageRating() > 0) {
                    recommendedBoardingHouses.add(bh);
                }
            }
            
            // Sort recommended by rating
            if (!recommendedBoardingHouses.isEmpty()) {
                java.util.Collections.sort(recommendedBoardingHouses, (o1, o2) -> 
                    Double.compare(o2.getAverageRating(), o1.getAverageRating()));
            }
            
            nearbyFiltered = true;
            recommendedFiltered = true;
            updateBothSections(silent);
            return;
        }

        Listing bh = list.get(index);
        String bhAddress = bh.getBhAddress();

        if (bhAddress == null || bhAddress.isEmpty()) {
            processNextNearbyBH(list, index + 1, nearbyList, targetLat, targetLon, silent);
            return;
        }

        geocodeWithMapbox(bhAddress, new GeocodeCallback() {
            @Override
            public void onSuccess(double lat, double lon) {
                double distance = calculateDistanceInKm(targetLat, targetLon, lat, lon);
                if (distance <= NEARBY_RADIUS_KM) {
                    nearbyList.add(bh);
                    // Log.d(TAG, "BH " + bh.getBhName() + " is within " + String.format("%.2f", distance) + " km");
                }
                processNextNearbyBH(list, index + 1, nearbyList, targetLat, targetLon, silent);
            }

            @Override
            public void onFailure(String errorMessage) {
                // Log.w(TAG, "Geocoding failed for " + bh.getBhName() + ": " + errorMessage);
                processNextNearbyBH(list, index + 1, nearbyList, targetLat, targetLon, silent);
            }
        });
    }

    private void filterNearbyBoardingHousesAsyncOld() {
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
                
                // double distance = calculateDistance(bhAddress);
                double distance = 100.0; // Placeholder for legacy code
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
                    // If no matches found, keep empty
                    // synchronized (allBoardingHouses) {
                    //    nearbyBoardingHouses.addAll(new ArrayList<>(allBoardingHouses));
                    // }
                    Log.d(TAG, "No nearby matches within 5km");
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
        filterNearbyByMunicipality(false);
    }
    
    private void filterNearbyByMunicipality(boolean silent) {
        // Fallback: if geocoding failed, use same province AND municipality
        nearbyBoardingHouses.clear();
        recommendedBoardingHouses.clear();
        
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
                                   bhProvince.toLowerCase().contains(boarderProvince.toLowerCase());
            
            boolean municipalityMatch = boarderMunicipality != null && 
                                       !boarderMunicipality.isEmpty() && 
                                       bhMunicipality.toLowerCase().contains(boarderMunicipality.toLowerCase());
            
            if (provinceMatch && municipalityMatch) {
                nearbyBoardingHouses.add(bh);
                // Recommended must have rating
                if (bh.getAverageRating() > 0) {
                    recommendedBoardingHouses.add(bh);
                }
            }
        }
        
        // Sort recommended
        if (!recommendedBoardingHouses.isEmpty()) {
            java.util.Collections.sort(recommendedBoardingHouses, (o1, o2) -> 
                Double.compare(o2.getAverageRating(), o1.getAverageRating()));
        }
        
        Log.d(TAG, "Nearby filtered (by municipality): " + nearbyBoardingHouses.size());
        nearbyFiltered = true;
        recommendedFiltered = true;
    }
    
    private void updateBothSections() {
        updateBothSections(false);
    }

    private void updateBothSections(boolean silent) {
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
            
            // Re-apply search filter if there is active search text
            if (etSearch != null && etSearch.getText().length() > 0) {
                 Log.d(TAG, "Re-applying search filter after location update");
                 filterBoardingHousesBySearch(etSearch.getText().toString());
            }
            
            // Update adapters
            if (recommendedAdapter != null) {
                recommendedAdapter.notifyDataSetChanged();
                // Ensure RecyclerView scrolls to start (left side) after data update
                // SKIP scrolling if silent (background update) -> preserve user scroll position
                if (!silent && rvRecommendedBH != null) {
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
                // SKIP scrolling if silent
                if (!silent) {
                    rvRecommendedBH.post(() -> {
                        if (rvRecommendedBH != null) {
                            rvRecommendedBH.scrollToPosition(0);
                        }
                    });
                }
            }
            if (rvNearbyBH != null && nearbyBoardingHouses != null && !nearbyBoardingHouses.isEmpty()) {
                rvNearbyBH.setVisibility(View.VISIBLE);
            }
            
            // Update empty states in case there's an active search
            updateEmptyStates();
            
            // Adjust alignment for single-item results
            adjustRecommendedRecyclerViewLayout();
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
        
        // Use Mapbox for boarder address geocoding
        geocodeWithMapbox(address, new GeocodeCallback() {
            @Override
            public void onSuccess(double latitude, double longitude) {
                boarderLatitude = latitude;
                boarderLongitude = longitude;
                Log.d(TAG, "Boarder Mapbox coordinates updated: " + boarderLatitude + ", " + boarderLongitude);
                
                // If search center hasn't been set by user yet, use boarder address as initial center
                if (searchCenterLat == null || searchCenterLon == null) {
                    searchCenterLat = boarderLatitude;
                    searchCenterLon = boarderLongitude;
                    
                    if (tvCurrentSearchLocation != null) {
                        tvCurrentSearchLocation.setText(address);
                    }
                    currentSearchLocationName = address;
                }
                
                // Update UI on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                     // Re-filter only nearby boarding houses with coordinates
                     if (allBoardingHouses != null && !allBoardingHouses.isEmpty()) {
                         nearbyFiltered = false;
                         filterNearbyBoardingHousesAsync();
                     } else if (recommendedFiltered) {
                         nearbyFiltered = true;
                         updateBothSections();
                     }
                });
            }
            
            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Mapbox geocoding error for boarder: " + errorMessage);
                // Fallback to municipality filtering
                 new Handler(Looper.getMainLooper()).post(() -> {
                    if (isAdded() && getContext() != null && 
                        allBoardingHouses != null && !allBoardingHouses.isEmpty()) {
                        nearbyFiltered = false;
                        filterNearbyBoardingHousesAsync();
                    }
                });
            }
        });
    }
    

    
    private void geocodeWithMapbox(String address, final GeocodeCallback callback) {
        if (address == null || address.trim().isEmpty()) {
            callback.onFailure("Empty address");
            return;
        }
        
        // Encode address for URL
        String encodedAddress = "";
        try {
            encodedAddress = java.net.URLEncoder.encode(address, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            callback.onFailure("Encoding error: " + e.getMessage());
            return;
        }
        
        String url = MAPBOX_GEOCODING_URL + encodedAddress + ".json?access_token=" + MAPBOX_ACCESS_TOKEN + "&limit=1";
        Log.d(TAG, "Mapbox URL: " + url);
        
        StringRequest request = new StringRequest(Request.Method.GET, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        JSONArray features = jsonResponse.getJSONArray("features");
                        
                        if (features.length() > 0) {
                            JSONObject feature = features.getJSONObject(0);
                            JSONArray center = feature.getJSONArray("center");
                            
                            // Mapbox returns [longitude, latitude]
                            double longitude = center.getDouble(0);
                            double latitude = center.getDouble(1);
                            
                            callback.onSuccess(latitude, longitude);
                        } else {
                            callback.onFailure("No results found");
                        }
                    } catch (JSONException e) {
                        callback.onFailure("JSON parsing error: " + e.getMessage());
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    callback.onFailure("Network error: " + error.getMessage());
                }
            }
        );
        
        if (getContext() != null) {
            Volley.newRequestQueue(getContext()).add(request);
        } else {
            callback.onFailure("Context is null");
        }
    }
    
    private interface GeocodeCallback {
        void onSuccess(double latitude, double longitude);
        void onFailure(String errorMessage);
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
        
        // Stop pull-to-refresh indicator
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
        if (progressBarNearby != null) {
            progressBarNearby.setVisibility(View.GONE);
        }
    }

    @Override
    public void onFavoriteClick(Listing boardingHouse, boolean isFavorite) {
        try {
            if (isFavorite) {
                // Add to favorites (Updates DB and Local Prefs)
                BoarderFavoriteFragment.addToFavorites(getContext(), boardingHouse);
                String message = "Added to favorites: " + boardingHouse.getBhName();
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            } else {
                // Remove from favorites (Updates DB and Local Prefs)
                BoarderFavoriteFragment.removeFromFavorites(getContext(), boardingHouse);
                String message = "Removed from favorites: " + boardingHouse.getBhName();
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
            
            // Sync both adapters immediately so the "Heart" icon updates in both lists (Nearby & Recommended)
            if (nearbyAdapter != null) {
                nearbyAdapter.notifyDataSetChanged();
            }
            if (recommendedAdapter != null) {
                recommendedAdapter.notifyDataSetChanged();
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
            
            String url = "https://boardease.calapebohol.com/get_unread_count.php?user_id=" + currentUserId;
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
            
            String url = "https://boardease.calapebohol.com/get_notifications.php?user_id=" + currentUserId;
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
    // Recursive function to process BHs one by one for Geocoding using Mapbox
    private void processNextNearbyBH(final List<Listing> bhList, final int index, final List<Listing> nearbyList) {
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        if (index >= bhList.size()) {
            // All done. Safely update list and UI.
            nearbyBoardingHouses.clear();
            
            if (nearbyList != null && !nearbyList.isEmpty()) {
                nearbyBoardingHouses.addAll(nearbyList);
            } else {
                Log.d(TAG, "No nearby matches within " + NEARBY_RADIUS_KM + "km (Mapbox)");
            }
            
            Log.d(TAG, "Nearby filtered (Mapbox): " + nearbyBoardingHouses.size());
            nearbyFiltered = true;
            updateBothSections();
            return;
        }

        
        Listing bh = bhList.get(index);
        String bhAddress = bh.getBhAddress();
        
        if (bhAddress == null || bhAddress.trim().isEmpty()) {
            processNextNearbyBH(bhList, index + 1, nearbyList);
            return;
        }
        
        geocodeWithMapbox(bhAddress, new GeocodeCallback() {
            @Override
            public void onSuccess(double latitude, double longitude) {
                double distance = calculateDistanceInKm(boarderLatitude, boarderLongitude, latitude, longitude);
                if (distance <= NEARBY_RADIUS_KM) {
                    nearbyList.add(bh);
                }
                processNextNearbyBH(bhList, index + 1, nearbyList);
            }
            
            @Override
            public void onFailure(String errorMessage) {
                // Failed to geocode, skip
                processNextNearbyBH(bhList, index + 1, nearbyList);
            }
        });

    }

    private void openFullScreenMap(List<Listing> allListings, List<Listing> focusListings) {
        if (getContext() == null || allListings == null || allListings.isEmpty()) {
            Toast.makeText(getContext(), "No listings to show on map", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            Log.d(TAG, "Opening full screen map with " + allListings.size() + " listings");
            
            // Create a full-screen dialog with explicit immersive theme
            // Create a full-screen dialog with explicit immersive theme
            android.app.Dialog fullScreenDialog = new android.app.Dialog(getContext(), android.R.style.Theme_Light_NoTitleBar_Fullscreen);
            fullScreenDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
            
            android.view.Window window = fullScreenDialog.getWindow();
            if (window != null) {
                // Ensure the window covers the entire screen but has transparent background for modal effect
                window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT);
                window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                window.setDimAmount(0.6f); // Dim the background
                window.setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
            
            // Create WebView for full screen map
            WebView fullScreenWebView = new WebView(getContext());
            
            // Configure WebView with robust settings
            android.webkit.WebSettings settings = fullScreenWebView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setBuiltInZoomControls(true);
            settings.setDisplayZoomControls(false);
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);
            settings.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
            
            // Enable mixed content to allow Mapbox resources
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                settings.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }
            
            // Set WebChromeClient for console logging (helpful for debugging)
            fullScreenWebView.setWebChromeClient(new android.webkit.WebChromeClient() {
                @Override
                public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                    Log.d("MapWebViewConsole", consoleMessage.message() + " -- From line " + 
                         consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
                    return true;
                }
            });
            
            // Set WebViewClient to handle loading
            fullScreenWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    Log.d(TAG, "Map WebView finished loading");
                }
            });
            
            // Load Mapbox map HTML
            String htmlContent = generateMapboxMapHtml(allListings, focusListings);
            fullScreenWebView.loadDataWithBaseURL("https://www.mapbox.com", htmlContent, "text/html", "UTF-8", null);
            
            // Root Container (Transparent with Padding for Margins)
            android.widget.FrameLayout rootContainer = new android.widget.FrameLayout(getContext());
            int margin = (int)(getResources().getDisplayMetrics().density * 20); // 20dp margin on all sides
            rootContainer.setPadding(margin, margin, margin, margin);
            rootContainer.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            ));
            
            // Card Container (Holds the WebView with Rounded Corners)
            MaterialCardView cardContainer = new MaterialCardView(getContext());
            cardContainer.setRadius(getResources().getDisplayMetrics().density * 16); // 16dp rounded corners
            cardContainer.setCardElevation(getResources().getDisplayMetrics().density * 8);
            cardContainer.setStrokeWidth(0);
            // Ensure clipping works for rounded corners
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                cardContainer.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
                cardContainer.setClipToOutline(true);
            }
            
            android.widget.FrameLayout.LayoutParams cardParams = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            );
            
            // Add WebView to Card
            cardContainer.addView(fullScreenWebView, new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            ));
            
            // Add Close Button (Inside Card, Top Right)
            ImageView closeButton = new ImageView(getContext());
            closeButton.setImageResource(R.drawable.ic_close);
            closeButton.setColorFilter(android.graphics.Color.WHITE);
            
            // Create background circle for button
            android.graphics.drawable.GradientDrawable closeBg = new android.graphics.drawable.GradientDrawable();
            closeBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            closeBg.setColor(android.graphics.Color.parseColor("#80000000")); // Semi-transparent black
            closeButton.setBackground(closeBg);
            
            int btnSize = (int)(getResources().getDisplayMetrics().density * 44);
            int btnMargin = (int)(getResources().getDisplayMetrics().density * 16);
            
            android.widget.FrameLayout.LayoutParams closeParams = new android.widget.FrameLayout.LayoutParams(btnSize, btnSize);
            closeParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
            closeParams.setMargins(0, btnMargin, btnMargin, 0); // Margin relative to card edges
            
            closeButton.setPadding(20, 20, 20, 20);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                closeButton.setElevation(15f);
            }
            closeButton.setOnClickListener(v -> fullScreenDialog.dismiss());
            
            // Add Close Button to Card (Overlaying WebView)
            cardContainer.addView(closeButton, closeParams);
            
            // Add Card to Root
            rootContainer.addView(cardContainer, cardParams);
            
            fullScreenDialog.setContentView(rootContainer);
            fullScreenDialog.setCancelable(true);
            fullScreenDialog.show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening full screen map: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error opening map: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private String generateMapboxMapHtml(List<Listing> allListings, List<Listing> focusListings) {
        try {
            // Build JS array of markers
            StringBuilder markersJs = new StringBuilder();
            markersJs.append("var bounds = new mapboxgl.LngLatBounds();\n");
            
            // Helper set to check for focus items quickly
            Set<String> focusIds = new HashSet<>();
            if (focusListings != null) {
                for (Listing l : focusListings) {
                    focusIds.add(String.valueOf(l.getBhId()));
                }
            }
            
            for (Listing listing : allListings) {
                String name = listing.getBhName().replace("'", "\\'").replace("\n", " ");
                String address = listing.getBhAddress().replace("'", "\\'").replace("\n", " ");
                String id = String.valueOf(listing.getBhId());
                boolean shouldExtendBounds = focusIds.contains(id);

                markersJs.append(String.format(
                    "console.log('Geocoding listing%s: %s');\n" +
                    "fetch('https://api.mapbox.com/geocoding/v5/mapbox.places/' + encodeURIComponent('%s') + '.json?access_token=' + mapboxgl.accessToken + '&limit=1')\n" +
                    "  .then(response => response.json())\n" +
                    "  .then(data => {\n" +
                    "    if (data && data.features && data.features.length > 0) {\n" +
                    "      var coords = data.features[0].center;\n" +
                    "      console.log('Found coords for %s:', coords);\n" +
                    "      var el = document.createElement('div');\n" +
                    "      el.className = 'marker';\n" +
                    "      el.innerHTML = '🏠';\n" + 
                    "      new mapboxgl.Marker(el)\n" +
                    "        .setLngLat(coords)\n" +
                    "        .setPopup(new mapboxgl.Popup({ offset: 25 }).setHTML('<b>%s</b><br>%s'))\n" +
                    "        .addTo(map);\n" +
                    // Only extend bounds if this listing is in the focus list (nearby)
                    (shouldExtendBounds ? "      bounds.extend(coords);\n" : "") +
                    (shouldExtendBounds ? "      map.fitBounds(bounds, { padding: 50, maxZoom: 15 });\n" : "") +
                    "    } else {\n" +
                    "      console.warn('No geocoding results for address: %s');\n" +
                    "    }\n" +
                    "  }).catch(err => console.error('Geocoding error for %s:', err));\n",
                    (shouldExtendBounds ? " (FOCUS)" : ""), name, address, name, name, address, address, name
                ));
            }
            
            // Include boarder location marker if available
            Double targetLat = (searchCenterLat != null) ? searchCenterLat : boarderLatitude;
            Double targetLon = (searchCenterLon != null) ? searchCenterLon : boarderLongitude;

            if (targetLat != null && targetLon != null) {
                 markersJs.append(String.format(Locale.US,
                    "console.log('Adding user marker (person) at: [%f, %f]');\n" +
                    "var userEl = document.createElement('div');\n" +
                    "userEl.className = 'user-marker';\n" +
                    "userEl.innerHTML = '<div class=\"pulse\"></div><div class=\"user-icon\">🚶‍♂️</div>';\n" +
                    
                    "new mapboxgl.Marker(userEl)\n" +
                    "  .setLngLat([%f, %f])\n" +
                    "  .setPopup(new mapboxgl.Popup({ offset: 25 }).setText('Your Location'))\n" +
                    "  .addTo(map);\n" +
                    "bounds.extend([%f, %f]);\n",
                    targetLon, targetLat, targetLon, targetLat, targetLon, targetLat
                ));
            }

            return "<!DOCTYPE html>" +
                   "<html>" +
                   "<head>" +
                   "<meta charset=\"utf-8\">" +
                   "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\">" +
                   "<script src=\"https://api.mapbox.com/mapbox-gl-js/v2.15.0/mapbox-gl.js\"></script>" +
                   "<link href=\"https://api.mapbox.com/mapbox-gl-js/v2.15.0/mapbox-gl.css\" rel=\"stylesheet\" />" +
                   "<style>" +
                   "body { margin: 0; padding: 0; width: 100vw; height: 100vh; overflow: hidden; }" +
                   "#map { position: absolute; top: 0; bottom: 0; width: 100%; height: 100%; }" +
                   
                   /* Person Marker Style */
                   ".user-marker { width: 60px; height: 60px; display: flex; justify-content: center; align-items: center; position: relative; cursor: pointer; z-index: 100!important; }" +
                   ".user-icon { background-color: #4285F4; width: 32px; height: 32px; border-radius: 50%; border: 3px solid white; display: flex; justify-content: center; align-items: center; color: white; font-size: 20px; box-shadow: 0 0 15px rgba(66,133,244,0.8); position: relative; z-index: 2; }" +
                   ".pulse { position: absolute; width: 100%; height: 100%; border-radius: 50%; background: rgba(66,133,244,0.5); animation: pulse 2s infinite ease-out; z-index: 1; }" +
                   "@keyframes pulse { 0% { transform: scale(0.4); opacity: 1; } 100% { transform: scale(1); opacity: 0; } }" +
                   
                   /* House Marker Style */
                   ".marker { width: 36px; height: 36px; background-color: #795548; border-radius: 50%; border: 2px solid white; display: flex; justify-content: center; align-items: center; color: white; font-size: 18px; cursor: pointer; box-shadow: 0 2px 5px rgba(0,0,0,0.3); z-index: 10; }" +
                   ".marker:hover { transform: scale(1.1); z-index: 20; }" +
                   
                   ".mapboxgl-popup { z-index: 1000; }" +
                   "</style>" +
                   "</head>" +
                   "<body>" +
                   "<div id=\"map\"></div>" +
                   "<script>" +
                   "mapboxgl.accessToken = '" + MAPBOX_ACCESS_TOKEN + "';" +
                   "var map = new mapboxgl.Map({ container: 'map', style: 'mapbox://styles/mapbox/streets-v12', " +
                   String.format(Locale.US, "center: [%f, %f], zoom: 12 });", targetLon, targetLat) +
                   "var bounds = new mapboxgl.LngLatBounds();" +
                   
                   "map.on('load', function() {" +
                   markersJs.toString() +
                   "map.fitBounds(bounds, { padding: 50, maxZoom: 15 });" +
                   "});" +
                   
                   "function addBHMarker(lng, lat, title) {" +
                   "  var el = document.createElement('div');" +
                   "  el.className = 'marker';" +
                   "  el.innerHTML = '🏠';" + // House Emoji
                   "  new mapboxgl.Marker(el)" +
                   "    .setLngLat([lng, lat])" +
                   "    .setPopup(new mapboxgl.Popup({ offset: 25 }).setHTML(title))" +
                   "    .addTo(map);" +
                   "  bounds.extend([lng, lat]);" +
                   "}" +
                   "</script>" +
                   "</body>" +
                   "</html>";
        } catch (Exception e) {
            Log.e(TAG, "Error generating map HTML", e);
            return "<html><body><h3 style='padding:20px'>Error loading map data. Please check your connection.</h3></body></html>";
        }
    }

    private void showLocationSelectionDialog() {
        if (getContext() == null) return;
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_select_location, null);
        builder.setView(dialogView);
        
        EditText etLocationInput = dialogView.findViewById(R.id.etLocationInput);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirmLocation);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelLocation);
        MaterialButton btnUseCurrent = dialogView.findViewById(R.id.btnUseCurrentLocation);
        
        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        
        if (currentSearchLocationName != null && !currentSearchLocationName.isEmpty()) {
            etLocationInput.setText(currentSearchLocationName);
        }
        
        if (btnUseCurrent != null) {
            btnUseCurrent.setOnClickListener(v -> {
                getCurrentLocation();
                dialog.dismiss();
            });
        }

        btnConfirm.setOnClickListener(v -> {
            String input = etLocationInput.getText().toString().trim();
            if (!input.isEmpty()) {
                updateSearchLocation(input);
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Please enter a location", Toast.LENGTH_SHORT).show();
            }
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private void updateSearchLocation(String locationString) {
        if (locationString == null || locationString.isEmpty()) return;
        
        Log.d(TAG, "Updating search location to: " + locationString);
        Toast.makeText(getContext(), "Updating location...", Toast.LENGTH_SHORT).show();
        
        geocodeWithMapbox(locationString, new GeocodeCallback() {
            @Override
            public void onSuccess(double latitude, double longitude) {
                if (!isAdded()) return;
                
                searchCenterLat = latitude;
                searchCenterLon = longitude;
                currentSearchLocationName = locationString;
                
                if (tvCurrentSearchLocation != null) {
                    tvCurrentSearchLocation.setText(locationString);
                }
                
                Log.d(TAG, "Search center updated: [" + searchCenterLat + ", " + searchCenterLon + "]");
                
                // Re-filter boarding houses with new coordinates
                nearbyFiltered = false;
                showProgressBars();
                filterNearbyBoardingHousesAsync();
            }

            @Override
            public void onFailure(String errorMessage) {
                if (!isAdded()) return;
                Log.e(TAG, "Failed to geocode new location: " + errorMessage);
                Toast.makeText(getContext(), "Could not find that location. Please be more specific.", Toast.LENGTH_LONG).show();
            }
        });
    }

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
                iconRes = R.drawable.ic_info;
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
