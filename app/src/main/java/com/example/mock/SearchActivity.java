package com.example.mock;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.mock.adapters.BoardingHouseAdapter;
import com.example.mock.utils.SearchHistoryManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SearchActivity extends AppCompatActivity implements BoardingHouseAdapter.OnFavoriteClickListener {

    private static final String TAG = "SearchActivity";
    // Sync with existing token
    private static final String MAPBOX_ACCESS_TOKEN = "pk.eyJ1IjoibmFtem1hcDA0IiwiYSI6ImNtanhubnN3MzJncTMzZHFzNHc4azB2MWUifQ.84NPjWYDgq3i20GLhbFTtg";
    private static final String BASE_URL = "https://boardease.calapebohol.com/"; // Or your base URL
    private static final String BOARDING_HOUSES_API = BASE_URL + "get_boarding_houses1.php";
    private static final String MAPBOX_GEOCODING_URL = "https://api.mapbox.com/geocoding/v5/mapbox.places/";
    
    // UI Components
    private EditText etSearch;
    private ImageView ivBack, ivClearSearch, ivSearchAction;
    private WebView webViewMap;
    private ProgressBar progressBarMap;
    private RecyclerView rvSearchResults;
    private RecyclerView rvSearchHistory;
    private MaterialCardView cardHistory;
    private TextView tvClearHistory;
    private com.google.android.material.chip.Chip chipNearMe, chipNear, chipRadius;
    
    // Logic
    private SearchHistoryManager historyManager;
    private SearchHistoryAdapter historyAdapter;
    private BoardingHouseAdapter resultsAdapter;
    private List<Listing> allBoardingHouses = new ArrayList<>();
    private List<Listing> filteredBoardingHouses = new ArrayList<>();
    private List<LocalLandmark> localLandmarks = new ArrayList<>();
    private boolean isShowingFilteredResults = false; // Track if we're showing search/Near Me results
    
    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private Double userLat, userLon;
    private Double searchCenterLat, searchCenterLon;
    
    // Config
    private double currentSearchRadius = 1.5; // Default 1.5km
    private String currentUserMunicipality = null;
    private String currentUserProvince = null;
    private String currentUserBarangay = null;
    
    private boolean isWaitingForLocationSettings = false;

    private final android.content.BroadcastReceiver locationProviderReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (android.location.LocationManager.PROVIDERS_CHANGED_ACTION.equals(intent.getAction())) {
                if (isLocationEnabled()) {
                    Log.d(TAG, "Location provider enabled");
                    Toast.makeText(context, "Location enabled", Toast.LENGTH_SHORT).show();
                    isWaitingForLocationSettings = false;
                    
                    // If we were waiting or just casually detected it, refresh
                    // But if "Near Me" is active, we definitely want to trigger search
                    boolean triggerSearch = "Near Me".equalsIgnoreCase(etSearch.getText().toString());
                    getCurrentLocation(triggerSearch);
                    
                } else {
                    Log.d(TAG, "Location provider disabled");
                    Toast.makeText(context, "Location disabled", Toast.LENGTH_SHORT).show();
                    
                    // Clear location data
                    userLat = null;
                    userLon = null;
                    updateMapMarkers(); // Will remove user marker
                    
                    // If currently searching "Near Me", prompt user
                    if ("Near Me".equalsIgnoreCase(etSearch.getText().toString())) {
                         showLocationEnableDialog();
                    }
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        
        // Register receiver
        android.content.IntentFilter filter = new android.content.IntentFilter(android.location.LocationManager.PROVIDERS_CHANGED_ACTION);
        registerReceiver(locationProviderReceiver, filter);
        
        // Check manually as well (for the return from Settings case where broadcast might have fired while paused? 
        // Actually PROVIDERS_CHANGED usually fires sticky or system-wide, but explicit check is safer for the "Waiting" flow)
        if (isWaitingForLocationSettings) {
            if (isLocationEnabled()) {
                isWaitingForLocationSettings = false;
                Toast.makeText(this, "Location enabled! Updating...", Toast.LENGTH_SHORT).show();
                getCurrentLocation(true); 
            }
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(locationProviderReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver not registered
        }
    }

    private boolean isLocationEnabled() {
        android.location.LocationManager locationManager = (android.location.LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) || 
               locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.searchContainer), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        historyManager = new SearchHistoryManager(this);
        
        initializeViews();
        setupMapWebView();
        setupListeners();
        setupLocalLandmarks();
        setupRecyclerViews();
        
        // Initial data load
        getCurrentLocation(false);
        loadBoardingHouses();
    }
    
    private void initializeViews() {
        etSearch = findViewById(R.id.etSearch);
        ivBack = findViewById(R.id.ivBack);
        ivClearSearch = findViewById(R.id.ivClearSearch);
        ivSearchAction = findViewById(R.id.ivSearchAction); // Search icon
        webViewMap = findViewById(R.id.webViewMap);
        progressBarMap = findViewById(R.id.progressBarMap);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        rvSearchHistory = findViewById(R.id.rvSearchHistory);
        cardHistory = findViewById(R.id.cardHistory);
        tvClearHistory = findViewById(R.id.tvClearHistory);
        
        chipNearMe = findViewById(R.id.chipNearMe);
        chipNear = findViewById(R.id.chipNear);
        chipRadius = findViewById(R.id.chipRadius);
    }
    
    private void setupMapWebView() {
        WebSettings settings = webViewMap.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setGeolocationEnabled(true);
        
        // Add JavaScript interface for marker clicks
        webViewMap.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void onMarkerClick(int bhId) {
                runOnUiThread(() -> openBoardingHouseDetails(bhId));
            }
        }, "Android");
        
        webViewMap.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("MapBoxWebView", consoleMessage.message());
                return true;
            }
        });
        
        webViewMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                progressBarMap.setVisibility(View.GONE);
                // Initial update (will show user marker if available, but no results until searched)
                updateMapMarkers();
            }
        });
        
        // Load initial empty map or user location
        loadMap(null, null);
    }
    
    private void loadMap(Double lat, Double lon) {
        double centerLat = (lat != null) ? lat : 9.8913; // Default to Calape if null
        double centerLon = (lon != null) ? lon : 123.8824;
        
        String html = generateMapHtml(centerLat, centerLon, 13);
        webViewMap.loadDataWithBaseURL("https://www.mapbox.com", html, "text/html", "UTF-8", null);
    }
    
    private void setupRecyclerViews() {
        // Search Results (Horizontal Cards at bottom)
        // Search Results (Vertical List now)
        resultsAdapter = new BoardingHouseAdapter(this, filteredBoardingHouses, this);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResults.setAdapter(resultsAdapter);
        
        // History (Vertical List)
        historyAdapter = new SearchHistoryAdapter(historyManager.getHistory(), item -> {
            etSearch.setText(item);
            etSearch.setSelection(item.length());
            cardHistory.setVisibility(View.GONE);
            performSearch(item);
        }, item -> {
            historyManager.removeSearch(item);
            historyAdapter.updateData(historyManager.getHistory());
        });
        rvSearchHistory.setLayoutManager(new LinearLayoutManager(this));
        rvSearchHistory.setAdapter(historyAdapter);
    }
    
    private void setupListeners() {
        ivBack.setOnClickListener(v -> finish());
        
        ivClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            isShowingFilteredResults = false; // Reset flag
            filteredBoardingHouses.clear();
            filteredBoardingHouses.addAll(allBoardingHouses);
            resultsAdapter.notifyDataSetChanged();
            updateMapMarkers();
            ivClearSearch.setVisibility(View.GONE);
            showHistoryIfFocused();
        });
        
        tvClearHistory.setOnClickListener(v -> {
            historyManager.clearHistory();
            historyAdapter.updateData(historyManager.getHistory());
        });
        
        etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showHistoryIfFocused();
            } else {
                cardHistory.setVisibility(View.GONE);
            }
        });
        
        etSearch.setOnClickListener(v -> showHistoryIfFocused());
        
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ivClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                if (s.length() == 0) {
                     showHistoryIfFocused();
                } else {
                    cardHistory.setVisibility(View.GONE);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText().toString().trim();
                performSearch(query);
                return true;
            }
            return false;
        });
        
        ivSearchAction.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            performSearch(query);
        });
        
        // Chip Listeners
        chipNearMe.setOnClickListener(v -> {
            etSearch.setText("Near Me");
            performSearch("Near Me");
        });
        
        chipNear.setOnClickListener(v -> {
            etSearch.setText("Near ");
            etSearch.setSelection(etSearch.getText().length());
            etSearch.requestFocus();
             // Show keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
        });
        
        chipRadius.setOnClickListener(v -> {
            etSearch.setText("Within 3km of ");
            etSearch.setSelection(etSearch.getText().length());
            etSearch.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
        });
    }
    
    private void showHistoryIfFocused() {
        List<String> history = historyManager.getHistory();
        if (!history.isEmpty()) {
            historyAdapter.updateData(history);
            cardHistory.setVisibility(View.VISIBLE);
        } else {
            cardHistory.setVisibility(View.GONE);
        }
    }
    
    private void performSearch(String query) {
        if (query.isEmpty()) return;
        
        hideKeyboard();
        historyManager.addSearch(query);
        cardHistory.setVisibility(View.GONE);
        
        String actualQuery = query;
        currentSearchRadius = 1.5; // Reset default
        
        // Parse prefixes
        if (query.toLowerCase().startsWith("within 3km of ")) {
            currentSearchRadius = 3.0;
            actualQuery = query.substring("Within 3km of ".length()).trim();
        } else if (query.toLowerCase().startsWith("near ") && !query.equalsIgnoreCase("Near Me")) {
            currentSearchRadius = 1.5;
            actualQuery = query.substring("Near ".length()).trim();
        }
        
        // 0. Check for "Near Me"
        if (actualQuery.equalsIgnoreCase("Near Me") || actualQuery.equalsIgnoreCase("My Location") || actualQuery.equalsIgnoreCase("Nearby")) {
            if (userLat != null && userLon != null) {
                // Use current radius (default 1.5 unless overriden, but "Near Me" implies close)
                // If user typed "Within 3km of Near Me" (unlikely but possible), we honor 3km.
                isShowingFilteredResults = true; // Mark as showing filtered results
                 filterByProximity(userLat, userLon);
                return;
            } else {
                // Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
                showLocationEnableDialog();
                return;
            }
        }
        
        // 1. Check for name match first
        List<Listing> nameMatches = new ArrayList<>();
        String lowerQuery = actualQuery.toLowerCase();
        
        for (Listing bh : allBoardingHouses) {
            boolean nameMatch = bh.getBhName() != null && bh.getBhName().toLowerCase().contains(lowerQuery);
            if (nameMatch) {
                nameMatches.add(bh);
            }
        }
        
        if (!nameMatches.isEmpty()) {
            // Found by name!
            Log.d(TAG, "Found " + nameMatches.size() + " name matches");
            isShowingFilteredResults = true; // Mark as showing filtered results
            filteredBoardingHouses.clear();
            filteredBoardingHouses.addAll(nameMatches);
            resultsAdapter.notifyDataSetChanged();
            updateMapMarkers();
            
            // Focus map on the first result
            Listing first = nameMatches.get(0);
            geocodeAndFocus(first.getBhName(), first.getBhAddress(), false); 
        } else {
            // 2. CHECK LOCAL LANDMARKS FIRST (Offline/Fast Fallback)
            LocalLandmark localMatch = findLocalLandmark(actualQuery);
            if (localMatch != null) {
                Log.d(TAG, "Local Landmark Found: " + localMatch.name);
                Toast.makeText(this, "Found: " + localMatch.name, Toast.LENGTH_SHORT).show();
                searchCenterLat = localMatch.lat;
                searchCenterLon = localMatch.lon;
                isShowingFilteredResults = true; // Mark as showing filtered results
                filterByProximity(localMatch.lat, localMatch.lon);
                
                // Add specific marker for the landmark
                webViewMap.evaluateJavascript("addSearchMarker(" + localMatch.lon + ", " + localMatch.lat + ", '" + localMatch.name + "');", null);
            } else {
                // 3. Geocode location and filter by proximity (Mapbox)
                Log.d(TAG, "No name matches, geocoding location: " + actualQuery);
                geocodeAndFilterByProximity(actualQuery);
            }
        }
    }
    
    private LocalLandmark findLocalLandmark(String query) {
        String lowerQuery = query.toLowerCase();
        
        // 1. Exact Name Match (or close to it)
        for (LocalLandmark l : localLandmarks) {
            if (lowerQuery.contains(l.name.toLowerCase()) || l.name.toLowerCase().contains(lowerQuery)) {
                 return l;
            }
        }
        
        // 2. Keyword + Municipality Context Match
        // If user is in Calape and types "Market", matching "Calape Public Market"
        if (currentUserMunicipality != null) {
            for (LocalLandmark l : localLandmarks) {
                if (l.municipality.equalsIgnoreCase(currentUserMunicipality)) {
                    for (String keyword : l.keywords) {
                        if (lowerQuery.contains(keyword)) {
                            return l;
                        }
                    }
                }
            }
        }
        
        // 3. Explicit Municipality Mention in Query (e.g. "Tubigon Port" even if user is in Calape)
        for (LocalLandmark l : localLandmarks) {
            if (lowerQuery.contains(l.municipality.toLowerCase())) {
                 for (String keyword : l.keywords) {
                    if (lowerQuery.contains(keyword)) {
                        return l;
                    }
                }
            }
        }
        
        return null;
    }
    
    private void geocodeAndFocus(String name, String address, boolean isLocationSearch) {
        // Just used to center the map on a specific BH if found by name
        // We actually need coordinates for the BH. Boarding houses generally have addresses.
        // If we found partial match, we might already have displayed them.
        // Let's just update the map boundaries to include all results.
    }
    
    private void geocodeAndFilterByProximity(String query) {
            String url;
            try {
                StringBuilder urlBuilder = new StringBuilder(MAPBOX_GEOCODING_URL);
                urlBuilder.append(URLEncoder.encode(query, "UTF-8"));
                urlBuilder.append(".json?access_token=").append(MAPBOX_ACCESS_TOKEN);
                urlBuilder.append("&limit=1");
                urlBuilder.append("&country=ph");
                
                // Add proximity bias if user location is known
                if (userLat != null && userLon != null) {
                    urlBuilder.append("&proximity=").append(userLon).append(",").append(userLat);
                    // Add Bounding Box to strictly limit results to ~20km area around user
                    urlBuilder.append("&bbox=").append(getBBoxString(userLat, userLon));
                }
                
                url = urlBuilder.toString();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return;
            }
        

        
        Log.d(TAG, "Search URL: " + url);
        Toast.makeText(this, "Searching location...", Toast.LENGTH_SHORT).show();
        
        StringRequest request = new StringRequest(Request.Method.GET, url, 
            response -> {
                try {
                    JSONObject json = new JSONObject(response);
                    JSONArray features = json.getJSONArray("features");
                    if (features.length() > 0) {
                        JSONObject feature = features.getJSONObject(0);
                        JSONArray center = feature.getJSONArray("center");
                        double lon = center.getDouble(0);
                        double lat = center.getDouble(1);
                        
                        Log.d(TAG, "Geocoded " + query + " to " + lat + "," + lon);
                        searchCenterLat = lat;
                        searchCenterLon = lon;
                        
                        // Check place_type for region detection
                        boolean isRegion = false;
                        JSONArray placeTypes = feature.optJSONArray("place_type");
                        if (placeTypes != null) {
                            for (int i = 0; i < placeTypes.length(); i++) {
                                String type = placeTypes.getString(i);
                                if (type.equals("locality") || type.equals("place") || type.equals("neighborhood") || type.equals("district")) {
                                    isRegion = true;
                                    break;
                                }
                            }
                        }
                        
                        if (isRegion) {
                            String placeName = feature.getString("text");
                            isShowingFilteredResults = true; // Mark as showing filtered results
                            filterByRegion(placeName, lat, lon);
                        } else {
                            isShowingFilteredResults = true; // Mark as showing filtered results
                            filterByProximity(lat, lon);
                        }
                        
                    } else {

                        // Smart Hierarchical Retry Logic
                        boolean retrying = false;
                        String lowerQuery = query.toLowerCase();
                        
                        // 1. Try Barangay
                        if (currentUserBarangay != null && !lowerQuery.contains(currentUserBarangay.toLowerCase())) {
                             String retryQuery = query + " " + currentUserBarangay;
                             Toast.makeText(SearchActivity.this, "Trying " + currentUserBarangay + "...", Toast.LENGTH_SHORT).show();
                             geocodeAndFilterByProximity(retryQuery);
                             retrying = true;
                        } 
                        // 2. Try Municipality (Escalate)
                        else if (currentUserMunicipality != null && !lowerQuery.contains(currentUserMunicipality.toLowerCase())) {
                            // Strip Barangay from query if it was added in previous step
                            String baseQuery = query;
                            if (currentUserBarangay != null && lowerQuery.endsWith(" " + currentUserBarangay.toLowerCase())) {
                                baseQuery = query.substring(0, query.length() - (" " + currentUserBarangay).length());
                            }
                            
                            String retryQuery = baseQuery + " " + currentUserMunicipality;
                            Toast.makeText(SearchActivity.this, "Trying " + currentUserMunicipality + "...", Toast.LENGTH_SHORT).show();
                            geocodeAndFilterByProximity(retryQuery);
                            retrying = true;
                        }
                        // 3. Try Province (Escalate)
                        else if (currentUserProvince != null && !lowerQuery.contains(currentUserProvince.toLowerCase())) {
                            // Strip Municipality or Barangay
                            String baseQuery = query;
                            if (currentUserMunicipality != null && lowerQuery.endsWith(" " + currentUserMunicipality.toLowerCase())) {
                                baseQuery = query.substring(0, query.length() - (" " + currentUserMunicipality).length());
                            } else if (currentUserBarangay != null && lowerQuery.endsWith(" " + currentUserBarangay.toLowerCase())) {
                                baseQuery = query.substring(0, query.length() - (" " + currentUserBarangay).length());
                            }
                            
                            String retryQuery = baseQuery + " " + currentUserProvince;
                            Toast.makeText(SearchActivity.this, "Trying " + currentUserProvince + "...", Toast.LENGTH_SHORT).show();
                            geocodeAndFilterByProximity(retryQuery);
                            retrying = true;
                        }
                        
                        if (!retrying) {
                             Toast.makeText(this, "Location not found nearby", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            },
            error -> {
                Toast.makeText(this, "Search failed", Toast.LENGTH_SHORT).show();
                error.printStackTrace();
            }
        );
        Volley.newRequestQueue(this).add(request);
    }
    
    private void filterByProximity(double centerLat, double centerLon) {
        // Need to geocode all BHs if we don't have their coordinates stored.
        // Since the current Listing object doesn't have lat/lon, we have to rely on Geocoding them 
        // OR we can rely on the javascript map to handle the "display" and just filter the list blindly?
        // Better: We should really have lat/lon in the DB.
        // Assuming we don't, we have to geocode them one by one (Async) as seen in BoarderHomeFragment.
        // For 'SearchActivity', let's do this:
        // 1. Clear current results.
        // 2. Iterate all BHs.
        // 3. Geocode each (limited parallel).
        // 4. If within range, add to list.
        
        // Use the same recursive approach as BoarderHomeFragment
        
        filteredBoardingHouses.clear();
        resultsAdapter.notifyDataSetChanged();
        
        Toast.makeText(this, "Finding boarding houses nearby...", Toast.LENGTH_SHORT).show();
        
        // We will focus the map on the search center first
        webViewMap.evaluateJavascript("map.flyTo({center: [" + centerLon + ", " + centerLat + "], zoom: 14});", null);
        // User requested NOT to show the "Search Location" marker for general proximity searches
        // webViewMap.evaluateJavascript("addSearchMarker(" + centerLon + ", " + centerLat + ", 'Search Location');", null);
        
        processNextBHForFiltering(new ArrayList<>(allBoardingHouses), 0, centerLat, centerLon);
    }
    
    private void filterByRegion(String regionName, double centerLat, double centerLon) {
        filteredBoardingHouses.clear();
        String lowerRegion = regionName.toLowerCase();
        
        for (Listing bh : allBoardingHouses) {
            String addr = bh.getBhAddress();
            if (addr != null && addr.toLowerCase().contains(lowerRegion)) {
                filteredBoardingHouses.add(bh);
            }
        }
        
        Toast.makeText(this, "Found " + filteredBoardingHouses.size() + " places in " + regionName, Toast.LENGTH_SHORT).show();
        resultsAdapter.notifyDataSetChanged();
        updateMapMarkers();
        
        // Fly to region center
        webViewMap.evaluateJavascript("map.flyTo({center: [" + centerLon + ", " + centerLat + "], zoom: 13});", null);
    }

    private void processNextBHForFiltering(List<Listing> list, int index, double centerLat, double centerLon) {
        if (index >= list.size()) {
            Toast.makeText(this, "Found " + filteredBoardingHouses.size() + " places nearby", Toast.LENGTH_SHORT).show();
            updateMapMarkers();
            return;
        }
        
        Listing bh = list.get(index);
        String address = bh.getBhAddress();
        
        if (address == null || address.isEmpty()) {
            processNextBHForFiltering(list, index + 1, centerLat, centerLon);
            return;
        }
        
        // Encode address
        String url;
        try {
           url = MAPBOX_GEOCODING_URL + URLEncoder.encode(address, "UTF-8") + ".json?access_token=" + MAPBOX_ACCESS_TOKEN + "&limit=1&country=ph";
        } catch (Exception e) {
            processNextBHForFiltering(list, index + 1, centerLat, centerLon);
            return;
        }
        
        StringRequest request = new StringRequest(Request.Method.GET, url,
            response -> {
                try {
                    JSONObject json = new JSONObject(response);
                    JSONArray features = json.getJSONArray("features");
                    if (features.length() > 0) {
                        JSONArray center = features.getJSONObject(0).getJSONArray("center");
                        double lon = center.getDouble(0);
                        double lat = center.getDouble(1);
                        
                        double dist = calculateDistanceInKm(centerLat, centerLon, lat, lon);
                        if (dist <= currentSearchRadius) {
                            bh.setLat(lat);
                            bh.setLon(lon);
                            filteredBoardingHouses.add(bh);
                            resultsAdapter.notifyItemInserted(filteredBoardingHouses.size() - 1);
                        }
                    }
                } catch (Exception e) {}
                
                processNextBHForFiltering(list, index + 1, centerLat, centerLon);
            },
            error -> processNextBHForFiltering(list, index + 1, centerLat, centerLon)
        );
        Volley.newRequestQueue(this).add(request);
    }
    
    private double calculateDistanceInKm(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
    
    private void loadBoardingHouses() {
        StringRequest request = new StringRequest(Request.Method.GET, BOARDING_HOUSES_API, 
            response -> {
                try {
                    allBoardingHouses.clear();
                    // Basic parsing - matching BoarderHomeFragment but simplified
                    JSONArray dataArray;
                     try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            dataArray = jsonResponse.getJSONArray("data");
                        } else { return; }
                    } catch (JSONException e) {
                        dataArray = new JSONArray(response);
                    }
                    
                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONObject obj = dataArray.getJSONObject(i);
                        // Simplified parsing
                         int bhId = obj.getInt("bh_id");
                        String bhName = obj.getString("bh_name");
                        String imagePath = obj.optString("image_path", "");
                        String address = obj.optString("bh_address", "");
                        String desc = obj.optString("bh_description", "");
                        String rules = obj.optString("bh_rules", "");
                        String baths = obj.optString("number_of_bathroom", "");
                        String area = obj.optString("area", "");
                        String year = obj.optString("build_year", "");
                        
                        Integer minP = !obj.isNull("min_price") ? obj.optInt("min_price") : null;
                        Integer maxP = !obj.isNull("max_price") ? obj.optInt("max_price") : null;
                         double rating = obj.optDouble("average_rating", 0.0);
                         
                        ArrayList<String> paths = new ArrayList<>();
                        if (!imagePath.isEmpty()) paths.add(imagePath);
                        
                        Listing l = new Listing(bhId, bhName, address, desc, rules, baths, area, year, imagePath, paths, minP, maxP, rating);
                        allBoardingHouses.add(l);
                    }
                    
                    // Initially show all
                    // Initially show all
                    filteredBoardingHouses.addAll(allBoardingHouses);
                    resultsAdapter.notifyDataSetChanged();
                    updateMapMarkers();
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            },
            error -> Log.e(TAG, "Error loading BHs", error)
        );
        Volley.newRequestQueue(this).add(request);
    }
    
    private void getCurrentLocation(boolean triggerSearch) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }
        
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener(location -> {
                if (location != null) {
                    userLat = location.getLatitude();
                    userLon = location.getLongitude();
                    loadMap(userLat, userLon);
                    fetchUserLocationContext(userLat, userLon);
                    
                    if (triggerSearch) {
                        etSearch.setText("Near Me");
                        performSearch("Near Me");
                    }
                } else {
                     loadMap(null, null);
                     if (triggerSearch) {
                         Toast.makeText(this, "Could not retrieve location. Please check signal.", Toast.LENGTH_SHORT).show();
                     }
                }
            });
    }

    private void fetchUserLocationContext(double lat, double lon) {
        // Fetch Barangay (neighborhood/locality), Municipality (place), and Province (region)
        // types priority: neighborhood, locality, place, region
        String url = MAPBOX_GEOCODING_URL + lon + "," + lat + ".json?access_token=" + MAPBOX_ACCESS_TOKEN + "&types=neighborhood,locality,place,region&limit=3";
        
        StringRequest request = new StringRequest(Request.Method.GET, url, 
            response -> {
                try {
                    JSONObject json = new JSONObject(response);
                    JSONArray features = json.getJSONArray("features");
                    for (int i = 0; i < features.length(); i++) {
                        JSONObject feature = features.getJSONObject(i);
                        JSONArray placeTypes = feature.optJSONArray("place_type");
                        if (placeTypes != null && placeTypes.length() > 0) {
                            String type = placeTypes.getString(0);
                            if (type.equals("neighborhood") || type.equals("locality")) {
                                currentUserBarangay = feature.getString("text");
                            } else if (type.equals("place")) {
                                currentUserMunicipality = feature.getString("text");
                            } else if (type.equals("region")) {
                                currentUserProvince = feature.getString("text");
                            }
                        }
                    }
                    Log.d(TAG, "User Context: Brgy=" + currentUserBarangay + ", Muni=" + currentUserMunicipality + ", Prov=" + currentUserProvince);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            },
            error -> Log.e(TAG, "Error fetching location context", error)
        );
        Volley.newRequestQueue(this).add(request);
    }

    private void showLocationEnableDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Location Services Required")
            .setMessage("To use 'Near Me' search, please enable location services on your device.")
            .setPositiveButton("Settings", (dialog, which) -> {
                isWaitingForLocationSettings = true;
                Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void updateMapMarkers() {
        // Clear existing markers (handled in JS)
        webViewMap.evaluateJavascript("clearMarkers();", null);
        
        // Add User Marker
        if (userLat != null && userLon != null) {
             webViewMap.evaluateJavascript("addUserMarker(" + userLon + ", " + userLat + ");", null);
        }
        
        // Add Search Markers
        for (Listing bh : filteredBoardingHouses) {
            String name = bh.getBhName().replace("'", "\\'");
            String address = bh.getBhAddress().replace("'", "\\'");
            // We call JS to add marker which does geocoding internally on the map side if we pass address?
            // Actually, doing too many geocodes on Mapbox JS side might hit limits or be slow.
            // But for visual consistency with BoarderHomeFragment, let's use the same approach.
            
            // Show popup for all search results (not just single result)
            boolean openPopup = isShowingFilteredResults; // Show name for ALL filtered results
            boolean isSearchActive = isShowingFilteredResults; // Use flag instead of checking search box
            
            if (bh.getLat() != null && bh.getLon() != null) {
                // Use cached coordinates directly - FAST & SYNCED
                String js = String.format(Locale.US, "addBHMarker(%f, %f, '%s', %d, %b, %b);", bh.getLon(), bh.getLat(), name, bh.getBhId(), openPopup, isSearchActive);
                webViewMap.evaluateJavascript(js, null);
            } else {
                // Fallback to address geocoding (Slow/Risky)
                String js = String.format(Locale.US, "addBHMarkerByAddress('%s', '%s', %d, %b, %b);", address, name, bh.getBhId(), openPopup, isSearchActive);
                webViewMap.evaluateJavascript(js, null);
            }
        }
        
        // Fit bounds
        webViewMap.evaluateJavascript("fitBoundsToMarkers();", null);
    }

    private String getBBoxString(double lat, double lon) {
        // approx 10km box? 0.1 degree is roughly 11km.
        // Let's use +/- 0.1 degree
        double offset = 0.15; // Tighten to ~16km to exclude other islands/cities
        double minLon = lon - offset;
        double minLat = lat - offset;
        double maxLon = lon + offset;
        double maxLat = lat + offset;
        return String.format(Locale.US, "%f,%f,%f,%f", minLon, minLat, maxLon, maxLat);
    }
    
    private String generateMapHtml(double lat, double lon, int zoom) {
        return "<!DOCTYPE html>" +
               "<html>" +
               "<head>" +
               "<meta charset=\"utf-8\">" +
               "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
               "<script src=\"https://api.mapbox.com/mapbox-gl-js/v2.15.0/mapbox-gl.js\"></script>" +
               "<link href=\"https://api.mapbox.com/mapbox-gl-js/v2.15.0/mapbox-gl.css\" rel=\"stylesheet\" />" +
               "<style>" +
               "body { margin: 0; padding: 0; }" +
               "#map { position: absolute; top: 0; bottom: 0; width: 100%; height: 100%; }" +
                ".marker { width: 30px; height: 30px; background-color: #795548; border-radius: 50%; border: 2px solid white; display: flex; justify-content: center; align-items: center; color: white; cursor: pointer; }" +
                ".result-marker { width: 35px; height: 35px; border: 3px solid #FF9800 !important; box-shadow: 0 0 8px rgba(255, 152, 0, 0.6); }" +
               ".search-marker { background-color: #F44336; }" + // Red for search location
               "</style>" +
               "</head>" +
               "<body>" +
               "<div id=\"map\"></div>" +
               "<script>" +
               "mapboxgl.accessToken = '" + MAPBOX_ACCESS_TOKEN + "';" +
               "var map = new mapboxgl.Map({ container: 'map', style: 'mapbox://styles/mapbox/streets-v11', center: [" + lon + ", " + lat + "], zoom: " + zoom + " });" +
               "var markers = [];" +
               "var bounds = new mapboxgl.LngLatBounds();" +
               
               "function clearMarkers() { markers.forEach(m => m.remove()); markers = []; bounds = new mapboxgl.LngLatBounds(); }" +
               
               "function addUserMarker(lng, lat) {" +
               "  var el = document.createElement('div'); el.className = 'marker'; el.style.backgroundColor = '#2196F3'; el.innerHTML = '📍';" +
               "  var m = new mapboxgl.Marker(el).setLngLat([lng, lat]).addTo(map);" +
               "  markers.push(m);" +
               "}" +
               
               "function addSearchMarker(lng, lat, title) {" +
               "  var el = document.createElement('div'); el.className = 'marker search-marker'; el.innerHTML = '🔍';" +
               "  new mapboxgl.Marker(el).setLngLat([lng, lat]).setPopup(new mapboxgl.Popup({offset: 25}).setText(title)).addTo(map);" +
               "  bounds.extend([lng, lat]);" +
               "}" +
                
                 "function addBHMarker(lng, lat, title, bhId, openPopup, highlight) {" +
                 "  var el = document.createElement('div'); el.className = highlight ? 'marker result-marker' : 'marker'; el.innerHTML = '🏠';" +
                 "  var popup = new mapboxgl.Popup({offset: 25, closeOnClick: false, closeButton: false}).setHTML('<div style=\"cursor:pointer;\" onclick=\"Android.onMarkerClick(' + bhId + ')\">' + title + '</div>');" +
                 "  var m = new mapboxgl.Marker(el).setLngLat([lng, lat]).setPopup(popup).addTo(map);" +
                 "  if (openPopup) { m.togglePopup(); }" +
                 "  markers.push(m); bounds.extend([lng, lat]);" +
                 "}" +
                
                 "function addBHMarkerByAddress(address, title, bhId, openPopup, highlight) {" +
                "  fetch('https://api.mapbox.com/geocoding/v5/mapbox.places/' + encodeURIComponent(address) + '.json?access_token=' + mapboxgl.accessToken + '&limit=1&country=ph')" +
                "  .then(res => res.json())" +
                "  .then(data => {" +
                "     if (data.features && data.features.length > 0) {" +
                "       var coords = data.features[0].center;" +
                "       var el = document.createElement('div'); el.className = highlight ? 'marker result-marker' : 'marker'; el.innerHTML = '🏠';" +
                "       var popup = new mapboxgl.Popup({offset: 25, closeOnClick: false, closeButton: false}).setHTML('<div style=\"cursor:pointer;\" onclick=\"Android.onMarkerClick(' + bhId + ')\">' + title + '</div>');" +
                "       var m = new mapboxgl.Marker(el).setLngLat(coords).setPopup(popup).addTo(map);" +
                "       if (openPopup) { m.togglePopup(); }" +
                "       markers.push(m); bounds.extend(coords);" +
                "     }" +
                "  });" +
                "}" +
               
               "function fitBoundsToMarkers() { if (!bounds.isEmpty()) map.fitBounds(bounds, { padding: 50, maxZoom: 15 }); }" +
               
               "</script>" +
               "</body>" +
               "</html>";
    }

    private void openBoardingHouseDetails(int bhId) {
        Intent intent = new Intent(this, BoardingHouseDetailsActivity.class);
        intent.putExtra("bh_id", bhId);
        startActivity(intent);
    }
    
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onFavoriteClick(Listing boardingHouse, boolean isFavorite) {
        // Handle favorite click - similar to HomeFragment
        Toast.makeText(this, isFavorite ? "Added to favorites" : "Removed from favorites", Toast.LENGTH_SHORT).show();
    }
    
    // Inner class for Local Landmarks
    private static class LocalLandmark {
        String name;
        List<String> keywords;
        String municipality;
        double lat;
        double lon;

        public LocalLandmark(String name, List<String> keywords, String municipality, double lat, double lon) {
            this.name = name;
            this.keywords = keywords;
            this.municipality = municipality;
            this.lat = lat;
            this.lon = lon;
        }
    }
    
    private void setupLocalLandmarks() {
        localLandmarks.clear();
        try {
            java.io.InputStream is = getAssets().open("local_landmarks.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");
            
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String name = obj.getString("name");
                String municipality = obj.getString("municipality");
                double lat = obj.optDouble("lat", 0.0);
                double lon = obj.optDouble("lon", 0.0);
                
                List<String> keywords = new ArrayList<>();
                JSONArray keywordsArray = obj.getJSONArray("keywords");
                for (int j = 0; j < keywordsArray.length(); j++) {
                    keywords.add(keywordsArray.getString(j));
                }
                
                localLandmarks.add(new LocalLandmark(name, keywords, municipality, lat, lon));
            }
            Log.d(TAG, "Loaded " + localLandmarks.size() + " local landmarks from JSON.");
        } catch (Exception e) {
            Log.e(TAG, "Error loading local landmarks", e);
            e.printStackTrace();
        }
    }
    
    // Inner Adapter Class for History
    private static class SearchHistoryAdapter extends RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder> {
        private List<String> history;
        private final OnItemClickListener itemClickListener;
        private final OnItemClickListener deleteClickListener;

        interface OnItemClickListener {
            void onItemClick(String item);
        }

        public SearchHistoryAdapter(List<String> history, OnItemClickListener itemListener, OnItemClickListener deleteListener) {
            this.history = history;
            this.itemClickListener = itemListener;
            this.deleteClickListener = deleteListener;
        }
        
        public void updateData(List<String> newHistory) {
            this.history = newHistory;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String item = history.get(position);
            holder.tvQuery.setText(item);
            holder.itemView.setOnClickListener(v -> itemClickListener.onItemClick(item));
            holder.ivDelete.setOnClickListener(v -> deleteClickListener.onItemClick(item));
        }

        @Override
        public int getItemCount() {
            return history.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvQuery;
            ImageView ivDelete;

            ViewHolder(View itemView) {
                super(itemView);
                tvQuery = itemView.findViewById(R.id.tvQuery);
                ivDelete = itemView.findViewById(R.id.ivDelete);
            }
        }
    }
}


