package com.example.mock;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import androidx.viewpager2.widget.ViewPager2;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GuestHomeActivity - Simple guest view of the main page
 * Displays basic listings with guest restrictions
 */
public class GuestHomeActivity extends AppCompatActivity {

    private static final String TAG = "GuestHomeActivity";
    private static final String GET_ALL_BOARDING_HOUSES_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_all_boarding_houses_public.php";

    // Views
    private EditText etSearch;
    private LinearLayout layoutListings;
    private TextView tvWelcome;
    private ImageView ivLogin;
    private ImageView ivSignup;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private ImageView ivClearSearch;
    
    // Data
    private List<Listing> allBoardingHouses;
    private List<Listing> filteredBoardingHouses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_guest_home);

        initializeViews();
        
        // Small delay to ensure views are initialized before loading
        android.os.Handler handler = new android.os.Handler();
        handler.postDelayed(() -> {
            Log.d(TAG, "Starting to load listings after delay");
        loadSimpleListings();
        }, 100);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeViews() {
        etSearch = findViewById(R.id.etSearch);
        layoutListings = findViewById(R.id.layoutListings);
        tvWelcome = findViewById(R.id.tvWelcome);
        ivLogin = findViewById(R.id.ivLogin);
        ivSignup = findViewById(R.id.ivSignup);
        ivClearSearch = findViewById(R.id.ivClearSearch);
        
        // Initialize data lists
        allBoardingHouses = new ArrayList<>();
        filteredBoardingHouses = new ArrayList<>();
        
        // Find progress bar and empty state if they exist in layout
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        
        // Set click listeners for login and signup buttons
        ivLogin.setOnClickListener(v -> navigateToLogin());
        ivSignup.setOnClickListener(v -> navigateToSignup());
        
        // Setup search functionality
        setupSearchFunctionality();
        
        // Setup clear search icon
        setupClearSearchIcon();
    }
    
    private void setupSearchFunctionality() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBoardingHouses(s.toString());
                // Show/hide clear icon based on text
                if (ivClearSearch != null) {
                    ivClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void setupClearSearchIcon() {
        if (ivClearSearch != null) {
            // Clear search text when X icon is clicked
            ivClearSearch.setOnClickListener(v -> {
                etSearch.setText("");
                etSearch.clearFocus();
                ivClearSearch.setVisibility(View.GONE);
            });
        }
    }
    
    private void filterBoardingHouses(String query) {
        Log.d(TAG, "=== filterBoardingHouses START ===");
        Log.d(TAG, "Search query: '" + query + "'");
        Log.d(TAG, "Total boarding houses: " + allBoardingHouses.size());
        
        filteredBoardingHouses.clear();
        
        if (query == null || query.trim().isEmpty()) {
            // Show all if search is empty
            filteredBoardingHouses.addAll(allBoardingHouses);
            Log.d(TAG, "Empty query - showing all boarding houses");
        } else {
            String lowerQuery = query.toLowerCase().trim();
            Log.d(TAG, "Searching with query: '" + lowerQuery + "'");
            
            for (Listing boardingHouse : allBoardingHouses) {
                boolean matches = false;
                
                // Search by name
                if (boardingHouse.getBhName() != null && !boardingHouse.getBhName().isEmpty()) {
                    String name = boardingHouse.getBhName().toLowerCase();
                    if (name.contains(lowerQuery)) {
                        matches = true;
                        Log.d(TAG, "Match found by name: " + boardingHouse.getBhName());
                    }
                }
                
                // Search by location/address (even if name matched, we still want to check)
                if (!matches && boardingHouse.getBhAddress() != null && !boardingHouse.getBhAddress().isEmpty()) {
                    String address = boardingHouse.getBhAddress().toLowerCase();
                    if (address.contains(lowerQuery)) {
                        matches = true;
                        Log.d(TAG, "Match found by address: " + boardingHouse.getBhAddress());
                    }
                }
                
                if (matches) {
                    filteredBoardingHouses.add(boardingHouse);
                }
            }
            
            Log.d(TAG, "Filtered results: " + filteredBoardingHouses.size());
        }
        
        updateListingsDisplay();
        Log.d(TAG, "=== filterBoardingHouses END ===");
    }

    private void loadSimpleListings() {
        Log.d(TAG, "=== loadSimpleListings called ===");
        // Show loading indicator
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
            Log.d(TAG, "Progress bar set to visible");
        } else {
            Log.w(TAG, "Progress bar is null!");
        }
        layoutListings.setVisibility(View.GONE);
        
        // Fetch boarding houses from database
        Log.d(TAG, "Calling fetchBoardingHouses...");
        fetchBoardingHouses();
    }
    
    private void fetchBoardingHouses() {
        Log.d(TAG, "=== Starting to fetch boarding houses ===");
        RequestQueue queue = Volley.newRequestQueue(this);
        
        // Use the public endpoint specifically for guests (no user_id needed)
        String url = GET_ALL_BOARDING_HOUSES_URL;
        Log.d(TAG, "Using URL: " + url);
        
        // Use POST method (consistent with other endpoints)
        StringRequest request = new StringRequest(Request.Method.POST, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "=== FETCH RESPONSE START ===");
                    if (response != null) {
                        Log.d(TAG, "Response length: " + response.length());
                        Log.d(TAG, "Response (first 500 chars): " + (response.length() > 500 ? response.substring(0, 500) : response));
                    } else {
                        Log.e(TAG, "Response is NULL!");
                    }
                    
                    try {
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        
                        allBoardingHouses.clear();
                        
                        // Check if response is empty
                        if (response == null || response.trim().isEmpty()) {
                            Log.e(TAG, "Empty response from server");
                            showEmptyState("No boarding houses available");
                            return;
                        }
                        
                        // Check if response is a JSON array or object
                        String trimmedResponse = response.trim();
                        if (trimmedResponse.startsWith("[")) {
                            // JSON Array response
                            Log.d(TAG, "Parsing as JSON Array");
                            JSONArray array = new JSONArray(response);
                            Log.d(TAG, "Array length: " + array.length());
                            
                            if (array.length() == 0) {
                                Log.w(TAG, "API returned empty array");
                                showEmptyState("No boarding houses available");
                                return;
                            }
                            
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject obj = array.getJSONObject(i);
                                parseBoardingHouse(obj);
                            }
                        } else if (trimmedResponse.startsWith("{")) {
                            // JSON Object response
                            Log.d(TAG, "Parsing as JSON Object");
                            JSONObject jsonResponse = new JSONObject(response);
                            
                            if (jsonResponse.has("error")) {
                                String error = jsonResponse.getString("error");
                                Log.e(TAG, "Server error: " + error);
                                showEmptyState("Error: " + error);
                                return;
                            }
                            
                            if (jsonResponse.has("boarding_houses")) {
                                JSONArray array = jsonResponse.getJSONArray("boarding_houses");
                                Log.d(TAG, "Found boarding_houses array, length: " + array.length());
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject obj = array.getJSONObject(i);
                                    parseBoardingHouse(obj);
                                }
                            } else if (jsonResponse.has("success") && jsonResponse.getBoolean("success")) {
                                if (jsonResponse.has("data")) {
                                    JSONArray array = jsonResponse.getJSONArray("data");
                                    Log.d(TAG, "Found data array, length: " + array.length());
                                    for (int i = 0; i < array.length(); i++) {
                                        JSONObject obj = array.getJSONObject(i);
                                        parseBoardingHouse(obj);
                                    }
                                }
                            } else {
                                // Try to parse as array directly in the object
                                JSONArray array = jsonResponse.optJSONArray("data");
                                if (array == null) {
                                    array = jsonResponse.optJSONArray("boarding_houses");
                                }
                                if (array != null) {
                                    Log.d(TAG, "Found alternative array, length: " + array.length());
                                    for (int i = 0; i < array.length(); i++) {
                                        JSONObject obj = array.getJSONObject(i);
                                        parseBoardingHouse(obj);
                                    }
                                } else {
                                    Log.e(TAG, "Could not find boarding houses array in response");
                                    showEmptyState("No boarding houses found");
                                    return;
                                }
                            }
                        } else {
                            Log.e(TAG, "Invalid JSON response format");
                            showEmptyState("Invalid server response");
                            return;
                        }
                        
                        Log.d(TAG, "Total boarding houses parsed: " + allBoardingHouses.size());
                        
                        if (allBoardingHouses.size() == 0) {
                            Log.w(TAG, "No boarding houses were parsed from response");
                            // Try to parse again with different approach - maybe the response format is different
                            Log.w(TAG, "This might mean the API endpoint requires user_id or returns empty for guests");
                            showEmptyState("No boarding houses available. Please check back later.");
                            return;
                        }
                        
                        // Initially show all boarding houses
                        filteredBoardingHouses.clear();
                        filteredBoardingHouses.addAll(allBoardingHouses);
                        Log.d(TAG, "About to call updateListingsDisplay with " + filteredBoardingHouses.size() + " items");
                        updateListingsDisplay();
                        
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response", e);
                        Log.e(TAG, "Response that failed: " + response);
                        e.printStackTrace();
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        showEmptyState("Error parsing response: " + e.getMessage());
                    } catch (Exception e) {
                        Log.e(TAG, "Unexpected error", e);
                        e.printStackTrace();
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        showEmptyState("Unexpected error: " + e.getMessage());
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "=== VOLLEY ERROR ===");
                    Log.e(TAG, "Error message: " + error.getMessage());
                    Log.e(TAG, "Error class: " + error.getClass().getSimpleName());
                    if (error.networkResponse != null) {
                        Log.e(TAG, "Network response code: " + error.networkResponse.statusCode);
                        Log.e(TAG, "Network response data: " + new String(error.networkResponse.data));
                    }
                    error.printStackTrace();
                    
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    showEmptyState("Network error: " + (error.getMessage() != null ? error.getMessage() : "Unknown error"));
                }
            }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                // Empty params - get all boarding houses
                Log.d(TAG, "Request parameters: (empty - getting all)");
                return params;
            }
            
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/x-www-form-urlencoded");
                // Add ngrok header to skip browser warning
                headers.put("ngrok-skip-browser-warning", "true");
                Log.d(TAG, "Request headers set");
                return headers;
            }
        };
        
        // Add retry policy with shorter timeout to fail faster
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
            15000, // 15 seconds timeout
            1, // 1 retry only
            com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ) {
            @Override
            public void retry(com.android.volley.VolleyError error) throws com.android.volley.VolleyError {
                Log.e(TAG, "Retrying request due to error: " + error.getMessage());
                super.retry(error);
            }
        });
        
        // Add request tag for potential cancellation
        request.setTag("fetchBoardingHouses");
        
        Log.d(TAG, "Adding request to queue");
        queue.add(request);
        
        // Add a timeout check - if no response after 20 seconds, show error
        android.os.Handler handler = new android.os.Handler();
        handler.postDelayed(() -> {
            if (progressBar != null && progressBar.getVisibility() == View.VISIBLE) {
                Log.e(TAG, "Request timeout - no response received after 20 seconds");
                Log.e(TAG, "This might indicate:");
                Log.e(TAG, "1. The PHP file is not on the server");
                Log.e(TAG, "2. The endpoint URL is incorrect");
                Log.e(TAG, "3. There's a PHP fatal error preventing response");
                Log.e(TAG, "4. Network connectivity issue");
                
                // Cancel the request
                queue.cancelAll("fetchBoardingHouses");
                
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                showEmptyState("Unable to connect to server. Please check:\n1. The PHP file is uploaded\n2. Your internet connection\n3. The endpoint URL is correct");
            }
        }, 20000);
    }
    
    private void showEmptyState(String message) {
        if (tvEmptyState != null) {
            tvEmptyState.setText(message);
            tvEmptyState.setVisibility(View.VISIBLE);
        }
        layoutListings.setVisibility(View.GONE);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private void parseBoardingHouse(JSONObject obj) throws JSONException {
        try {
            int bhId = obj.optInt("bh_id", 0);
            if (bhId == 0) {
                Log.w(TAG, "Skipping boarding house with invalid ID");
                return;
            }
            
            String name = obj.optString("bh_name", "");
            String address = obj.optString("bh_address", "");
            String description = obj.optString("bh_description", "");
            String imagePath = obj.optString("image_path", "");
            
            // Try alternative field names
            if (name.isEmpty()) {
                name = obj.optString("name", "");
            }
            if (address.isEmpty()) {
                address = obj.optString("address", "");
            }
            if (description.isEmpty()) {
                description = obj.optString("description", "");
            }
            if (imagePath.isEmpty()) {
                imagePath = obj.optString("image", "");
            }
            
            Log.d(TAG, "Parsed boarding house - ID: " + bhId + ", Name: " + name + ", Address: " + address);
            
            // Create image paths list - get all images from JSON
            ArrayList<String> imagePaths = new ArrayList<>();
            
            // First, add the main image_path if available
            if (!imagePath.isEmpty()) {
                if (imagePath.startsWith("http")) {
                    imagePaths.add(imagePath);
                } else {
                    imagePaths.add("https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/" + imagePath);
                }
            }
            
            // Then, get all images from the "images" array if available
            if (obj.has("images") && obj.get("images") instanceof JSONArray) {
                JSONArray imagesArray = obj.getJSONArray("images");
                Log.d(TAG, "Found images array with " + imagesArray.length() + " items");
                for (int i = 0; i < imagesArray.length(); i++) {
                    try {
                        Object imageItem = imagesArray.get(i);
                        String imgPath = null;
                        
                        // Check if it's a JSONObject or a String
                        if (imageItem instanceof JSONObject) {
                            // If it's a JSONObject, extract the image_path field
                            JSONObject imageObj = (JSONObject) imageItem;
                            imgPath = imageObj.optString("image_path", "");
                            // Also try alternative field names
                            if (imgPath.isEmpty()) {
                                imgPath = imageObj.optString("path", "");
                            }
                            if (imgPath.isEmpty()) {
                                imgPath = imageObj.optString("url", "");
                            }
                        } else if (imageItem instanceof String) {
                            // If it's already a string, use it directly
                            imgPath = (String) imageItem;
                        } else {
                            // Try to get as string
                            imgPath = imagesArray.optString(i, "");
                        }
                        
                        if (imgPath != null && !imgPath.isEmpty() && !imgPath.equals("null") && !imgPath.startsWith("{")) {
                            String fullUrl;
                            if (imgPath.startsWith("http://") || imgPath.startsWith("https://")) {
                                fullUrl = imgPath;
                            } else {
                                // Remove leading slash if present
                                if (imgPath.startsWith("/")) {
                                    imgPath = imgPath.substring(1);
                                }
                                fullUrl = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/" + imgPath;
                            }
                            // Avoid duplicates
                            if (!imagePaths.contains(fullUrl)) {
                                imagePaths.add(fullUrl);
                                Log.d(TAG, "Added image " + (i + 1) + ": " + fullUrl);
                            }
                        } else {
                            Log.w(TAG, "Invalid image path at index " + i + ": " + imgPath);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing image at index " + i, e);
                    }
                }
            }
            
            // If no images found, add placeholder
            if (imagePaths.isEmpty()) {
                imagePaths.add("placeholder");
            }
            
            // Create Listing with address and description
            Listing listing = new Listing(bhId, name, address, description, "", "", "", "", imagePath, imagePaths);
            allBoardingHouses.add(listing);
            Log.d(TAG, "Added boarding house to list. Total: " + allBoardingHouses.size());
        } catch (Exception e) {
            Log.e(TAG, "Error parsing boarding house object: " + obj.toString(), e);
        }
    }
    
    private void updateListingsDisplay() {
        Log.d(TAG, "=== updateListingsDisplay START ===");
        Log.d(TAG, "Filtered boarding houses count: " + filteredBoardingHouses.size());
        Log.d(TAG, "All boarding houses count: " + allBoardingHouses.size());
        
        // Clear existing views
        layoutListings.removeAllViews();
        
        if (filteredBoardingHouses.isEmpty()) {
            Log.d(TAG, "No boarding houses to display");
            if (tvEmptyState != null) {
                tvEmptyState.setVisibility(View.VISIBLE);
                // If we have data but search returned nothing, show different message
                if (allBoardingHouses.isEmpty()) {
                    tvEmptyState.setText("No boarding houses available");
                } else {
                    tvEmptyState.setText("No boarding houses found matching your search");
                }
            }
            layoutListings.setVisibility(View.GONE);
        } else {
            Log.d(TAG, "Displaying " + filteredBoardingHouses.size() + " boarding houses");
            if (tvEmptyState != null) {
                tvEmptyState.setVisibility(View.GONE);
            }
            layoutListings.setVisibility(View.VISIBLE);
            
            // Create cards for filtered boarding houses
            for (Listing listing : filteredBoardingHouses) {
                String location = listing.getBhAddress() != null && !listing.getBhAddress().isEmpty() 
                    ? listing.getBhAddress() 
                    : "Location not available";
                String name = listing.getBhName() != null && !listing.getBhName().isEmpty()
                    ? listing.getBhName()
                    : "Unnamed Boarding House";
                
                Log.d(TAG, "Creating card for: " + name + " at " + location);
                createListingCard(name, location, listing);
            }
            Log.d(TAG, "All cards created");
        }
        Log.d(TAG, "=== updateListingsDisplay END ===");
    }

    private void createListingCard(String name, String location, Listing listing) {
        // Create a modern Flutter-style card layout with image slider on top
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setPadding(0, 0, 0, 0);
        
        // Use modern card background with rounded corners and ripple effect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Create card background with rounded corners
            android.graphics.drawable.GradientDrawable cardBg = new android.graphics.drawable.GradientDrawable();
            cardBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            cardBg.setCornerRadius(getResources().getDisplayMetrics().density * 16);
            cardBg.setColor(0xFFFFFFFF); // White background
            
            // Add ripple effect for modern Flutter-style interaction
            android.content.res.ColorStateList rippleColor = android.content.res.ColorStateList.valueOf(0x1A000000);
            android.graphics.drawable.RippleDrawable ripple = new android.graphics.drawable.RippleDrawable(
                rippleColor, 
                cardBg, 
                null
            );
            cardLayout.setBackground(ripple);
            cardLayout.setElevation(8f); // Flutter-style elevation
        } else {
            cardLayout.setBackgroundResource(R.drawable.modern_card_background);
        }
        
        // Add margins - expand sides (smaller horizontal margins), smaller vertical margins
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int horizontalMargin = (int) (getResources().getDisplayMetrics().density * 8); // 8dp horizontal margins (expanded)
        int verticalMargin = (int) (getResources().getDisplayMetrics().density * 6); // 6dp vertical margins (smaller)
        params.setMargins(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin);
        cardLayout.setLayoutParams(params);

        // Image Slider Container with rounded top corners (Flutter-style) - smaller size
        RelativeLayout imageContainer = new RelativeLayout(this);
        int imageHeight = (int) (getResources().getDisplayMetrics().density * 140); // 140dp - smaller/compact
        RelativeLayout.LayoutParams containerParams = new RelativeLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            imageHeight
        );
        imageContainer.setLayoutParams(containerParams);
        
        // Add rounded top corners to image container to match card
        float cornerRadius = getResources().getDisplayMetrics().density * 16; // 16dp radius
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        float[] radii = {cornerRadius, cornerRadius, cornerRadius, cornerRadius, 0, 0, 0, 0}; // Top corners only
        shape.setCornerRadii(radii);
        shape.setColor(0x00000000); // Transparent background
        imageContainer.setBackground(shape);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            imageContainer.setClipToOutline(true);
        }
        
        // ViewPager2 for image slider
        ViewPager2 viewPager = new ViewPager2(this);
        RelativeLayout.LayoutParams vpParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        );
        viewPager.setLayoutParams(vpParams);
        viewPager.setId(View.generateViewId());
        // Preload adjacent pages to ensure images are loaded
        viewPager.setOffscreenPageLimit(2);
        
        // Get all images from listing
        ArrayList<String> imageUrls = new ArrayList<>();
        if (listing != null && listing.getImagePaths() != null && !listing.getImagePaths().isEmpty()) {
            for (String imgUrl : listing.getImagePaths()) {
                if (imgUrl != null && !imgUrl.isEmpty() && !imgUrl.equals("placeholder") && !imgUrl.equals("null")) {
                    // Ensure URL is complete
                    String finalUrl = imgUrl;
                    if (!imgUrl.startsWith("http://") && !imgUrl.startsWith("https://")) {
                        if (imgUrl.startsWith("/")) {
                            imgUrl = imgUrl.substring(1);
                        }
                        finalUrl = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/" + imgUrl;
                    }
                    imageUrls.add(finalUrl);
                    Log.d(TAG, "Adding image URL to slider: " + finalUrl);
                }
            }
        } else if (listing != null && listing.getImagePath() != null && !listing.getImagePath().isEmpty()) {
            String imageUrl = listing.getImagePath();
            if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                if (imageUrl.startsWith("/")) {
                    imageUrl = imageUrl.substring(1);
                }
                imageUrl = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/" + imageUrl;
            }
            imageUrls.add(imageUrl);
            Log.d(TAG, "Using single image URL: " + imageUrl);
        }
        
        // If no images, add placeholder
        if (imageUrls.isEmpty()) {
            imageUrls.add("placeholder");
            Log.d(TAG, "No images found, using placeholder");
        } else {
            Log.d(TAG, "Total images for slider: " + imageUrls.size());
        }
        
        // Create adapter for ViewPager2
        ImageSliderAdapter adapter = new ImageSliderAdapter(imageUrls);
        // Set click listener on adapter
        adapter.setImageClickListener(() -> {
            Log.d(TAG, "Image clicked from adapter - showing dialog");
            showGuestRestrictionDialog();
        });
        viewPager.setAdapter(adapter);
        
        // Add page indicators if multiple images
        if (imageUrls.size() > 1) {
            LinearLayout indicatorLayout = new LinearLayout(this);
            indicatorLayout.setOrientation(LinearLayout.HORIZONTAL);
            RelativeLayout.LayoutParams indicatorParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            indicatorParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            indicatorParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            indicatorParams.bottomMargin = (int) (getResources().getDisplayMetrics().density * 8);
            indicatorLayout.setLayoutParams(indicatorParams);
            
            // Create dots for each image
            for (int i = 0; i < imageUrls.size(); i++) {
                View dot = new View(this);
                int dotSize = (int) (getResources().getDisplayMetrics().density * 6);
                LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dotSize, dotSize);
                dotParams.setMargins(4, 0, 4, 0);
                dot.setLayoutParams(dotParams);
                dot.setBackgroundResource(i == 0 ? R.drawable.dot_active : R.drawable.dot_inactive);
                if (dot.getBackground() == null) {
                    // Fallback if drawable doesn't exist
                    dot.setBackgroundColor(i == 0 ? 0xFFFFFFFF : 0x80FFFFFF);
                }
                indicatorLayout.addView(dot);
            }
            
            // Update indicators on page change
            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    for (int i = 0; i < indicatorLayout.getChildCount(); i++) {
                        View dot = indicatorLayout.getChildAt(i);
                        if (dot.getBackground() != null) {
                            dot.setBackgroundResource(i == position ? R.drawable.dot_active : R.drawable.dot_inactive);
                            if (dot.getBackground() == null) {
                                dot.setBackgroundColor(i == position ? 0xFFFFFFFF : 0x80FFFFFF);
                            }
                        }
                    }
                }
            });
            
            imageContainer.addView(indicatorLayout);
        }
        
        imageContainer.addView(viewPager);
        
        // Track touch events to distinguish tap from swipe
        final float[] touchStartX = {0};
        final float[] touchStartY = {0};
        final boolean[] isSwiping = {false};
        final int[] initialPage = {-1};
        final long[] touchStartTime = {0};
        final float SWIPE_THRESHOLD = 30f; // Minimum pixels for swipe
        final long TAP_TIME_THRESHOLD = 200; // Maximum milliseconds for tap
        
        // Set touch listener on ViewPager2 to detect taps
        viewPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        touchStartX[0] = event.getX();
                        touchStartY[0] = event.getY();
                        isSwiping[0] = false;
                        initialPage[0] = viewPager.getCurrentItem();
                        touchStartTime[0] = System.currentTimeMillis();
                        Log.d(TAG, "=== TOUCH ACTION_DOWN ===");
                        Log.d(TAG, "Start X: " + touchStartX[0] + ", Start Y: " + touchStartY[0]);
                        Log.d(TAG, "Initial Page: " + initialPage[0]);
                        Log.d(TAG, "Touch Start Time: " + touchStartTime[0]);
                        // Don't consume - let ViewPager2 handle it
                        return false;
                        
                    case android.view.MotionEvent.ACTION_MOVE:
                        float deltaX = Math.abs(event.getX() - touchStartX[0]);
                        float deltaY = Math.abs(event.getY() - touchStartY[0]);
                        // If horizontal movement is significant, it's a swipe
                        if (deltaX > SWIPE_THRESHOLD || deltaY > SWIPE_THRESHOLD) {
                            isSwiping[0] = true;
                            Log.d(TAG, "=== SWIPE DETECTED ===");
                            Log.d(TAG, "Delta X: " + deltaX + ", Delta Y: " + deltaY);
                        }
                        // Let ViewPager2 handle swipe
                        return false;
                        
                    case android.view.MotionEvent.ACTION_UP:
                        long touchDuration = System.currentTimeMillis() - touchStartTime[0];
                        int finalPage = viewPager.getCurrentItem();
                        float deltaXUp = Math.abs(event.getX() - touchStartX[0]);
                        float deltaYUp = Math.abs(event.getY() - touchStartY[0]);
                        
                        Log.d(TAG, "=== TOUCH ACTION_UP ===");
                        Log.d(TAG, "Final Page: " + finalPage + ", Initial Page: " + initialPage[0]);
                        Log.d(TAG, "Delta X: " + deltaXUp + ", Delta Y: " + deltaYUp);
                        Log.d(TAG, "Touch Duration: " + touchDuration + "ms");
                        Log.d(TAG, "Is Swiping: " + isSwiping[0]);
                        Log.d(TAG, "Page Changed: " + (finalPage != initialPage[0]));
                        
                        // It's a tap if:
                        // 1. Page didn't change
                        // 2. Movement was minimal
                        // 3. Touch duration was short (quick tap)
                        // 4. Not marked as swiping
                        boolean pageUnchanged = finalPage == initialPage[0];
                        boolean minimalMovement = deltaXUp < SWIPE_THRESHOLD && deltaYUp < SWIPE_THRESHOLD;
                        boolean quickTouch = touchDuration < TAP_TIME_THRESHOLD;
                        boolean notSwiping = !isSwiping[0];
                        
                        Log.d(TAG, "Tap Conditions Check:");
                        Log.d(TAG, "  - Page unchanged: " + pageUnchanged);
                        Log.d(TAG, "  - Minimal movement: " + minimalMovement + " (deltaX: " + deltaXUp + ", deltaY: " + deltaYUp + " < " + SWIPE_THRESHOLD + ")");
                        Log.d(TAG, "  - Quick touch: " + quickTouch + " (duration: " + touchDuration + " < " + TAP_TIME_THRESHOLD + ")");
                        Log.d(TAG, "  - Not swiping: " + notSwiping);
                        
                        boolean isTap = pageUnchanged && minimalMovement && quickTouch && notSwiping;
                        Log.d(TAG, "Is Tap: " + isTap);
                        
                        if (isTap) {
                            // It's a tap - show dialog
                            Log.d(TAG, "*** TAP DETECTED - Showing dialog ***");
                            viewPager.postDelayed(() -> {
                                // Double check page didn't change during delay
                                int checkPage = viewPager.getCurrentItem();
                                Log.d(TAG, "Post-delay check - Current Page: " + checkPage + ", Initial Page: " + initialPage[0]);
                                if (checkPage == initialPage[0]) {
                                    Log.d(TAG, "Page still unchanged - Calling showGuestRestrictionDialog()");
                                    showGuestRestrictionDialog();
                                } else {
                                    Log.d(TAG, "Page changed during delay - Cancelling dialog");
                                }
                            }, 50);
                        } else {
                            Log.d(TAG, "Not a tap - No dialog shown");
                        }
                        return false;
                        
                    case android.view.MotionEvent.ACTION_CANCEL:
                        Log.d(TAG, "=== TOUCH ACTION_CANCEL ===");
                        return false;
                }
                return false;
            }
        });
        
        cardLayout.addView(imageContainer);

        // Details container - below the image (compact padding)
        LinearLayout detailsLayout = new LinearLayout(this);
        detailsLayout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (getResources().getDisplayMetrics().density * 12); // 12dp padding (smaller)
        detailsLayout.setPadding(padding, padding, padding, padding);
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        detailsLayout.setLayoutParams(detailsParams);

        // Name - Compact size
        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(16); // Smaller size
        tvName.setTextColor(0xFF1A1A1A); // Modern dark gray
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tvName.setTypeface(getResources().getFont(R.font.poppins_bold));
        }
        tvName.setPadding(0, 0, 0, (int)(getResources().getDisplayMetrics().density * 4));
        detailsLayout.addView(tvName);

        // Location with Map Icon - Horizontal layout
        LinearLayout locationLayout = new LinearLayout(this);
        locationLayout.setOrientation(LinearLayout.HORIZONTAL);
        locationLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        locationLayout.setPadding(0, 0, 0, (int)(getResources().getDisplayMetrics().density * 6));
        
        // Location text
        TextView tvLocation = new TextView(this);
        tvLocation.setText(location);
        tvLocation.setTextSize(12); // Smaller
        tvLocation.setTextColor(0xFF666666); // Modern gray
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tvLocation.setTypeface(getResources().getFont(R.font.poppins_regular));
        }
        LinearLayout.LayoutParams locationParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        );
        tvLocation.setLayoutParams(locationParams);
        locationLayout.addView(tvLocation);
        
        // Map icon button (toggles between map and close icon)
        ImageView ivMapIcon = new ImageView(this);
        int iconSize = (int)(getResources().getDisplayMetrics().density * 24); // 24dp
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconParams.setMargins(
            (int)(getResources().getDisplayMetrics().density * 8),
            0,
            0,
            0
        );
        ivMapIcon.setLayoutParams(iconParams);
        ivMapIcon.setImageResource(R.drawable.ic_map); // Will use a map icon or create one
        ivMapIcon.setColorFilter(0xFFA18167); // Brown color matching app theme
        ivMapIcon.setPadding(
            (int)(getResources().getDisplayMetrics().density * 4),
            (int)(getResources().getDisplayMetrics().density * 4),
            (int)(getResources().getDisplayMetrics().density * 4),
            (int)(getResources().getDisplayMetrics().density * 4)
        );
        ivMapIcon.setClickable(true);
        ivMapIcon.setFocusable(true);
        
        locationLayout.addView(ivMapIcon);
        detailsLayout.addView(locationLayout);

        // Description - Compact size
        TextView tvDescription = new TextView(this);
        String description = listing != null && listing.getBhDescription() != null && !listing.getBhDescription().isEmpty()
            ? listing.getBhDescription()
            : "No description available";
        tvDescription.setText(description);
        tvDescription.setTextSize(12); // Smaller
        tvDescription.setTextColor(0xFF888888); // Lighter gray for description
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tvDescription.setTypeface(getResources().getFont(R.font.poppins_regular));
        }
        tvDescription.setMaxLines(2);
        tvDescription.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tvDescription.setLineSpacing((int)(getResources().getDisplayMetrics().density * 2), 1.1f); // Tighter line spacing
        tvDescription.setPadding(0, 0, 0, 0);
        detailsLayout.addView(tvDescription);

        // Map Preview Container using WebView with iframe HTML (initially hidden)
        WebView webViewMap = new WebView(this);
        int mapHeight = (int)(getResources().getDisplayMetrics().density * 150); // 150dp height
        LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            mapHeight
        );
        mapParams.setMargins(0, (int)(getResources().getDisplayMetrics().density * 8), 0, 0);
        webViewMap.setLayoutParams(mapParams);
        webViewMap.setVisibility(View.GONE); // Initially hidden
        webViewMap.setBackgroundColor(0xFFF5F5F5);
        
        // Configure WebView for iframe
        webViewMap.getSettings().setJavaScriptEnabled(true);
        webViewMap.getSettings().setBuiltInZoomControls(false);
        webViewMap.getSettings().setDisplayZoomControls(false);
        webViewMap.getSettings().setSupportZoom(false);
        webViewMap.getSettings().setUseWideViewPort(true);
        webViewMap.getSettings().setLoadWithOverviewMode(true);
        webViewMap.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webViewMap.setHorizontalScrollBarEnabled(false);
        webViewMap.setVerticalScrollBarEnabled(false);
        
        // Set WebViewClient to intercept clicks and handle page loading
        webViewMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "Google Maps iframe page finished loading");
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e(TAG, "Error loading map: " + description + " (Code: " + errorCode + ")");
            }
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                // Intercept ALL navigation attempts - if it's trying to navigate away from embed, open Maps app
                String url = request.getUrl().toString();
                Log.d(TAG, "Intercepted URL in preview: " + url);
                
                if (url != null && (url.contains("google.com/maps") || url.contains("maps.google.com"))) {
                    // Check if it's NOT the embed URL - if it's a regular Google Maps URL, open in app
                    // Embed URLs typically have: output=embed or mapclient=embed
                    boolean isEmbedUrl = url.contains("output=embed") || url.contains("mapclient=embed");
                    
                    if (!isEmbedUrl) {
                        // This is "View larger map" or similar - open in Google Maps app
                        Log.d(TAG, "Non-embed URL detected - opening Google Maps app for: " + location);
                        openMapForAddress(location);
                        return true; // Block the default navigation
                    } else {
                        // Check if URL changed significantly (different coordinates or removed embed params)
                        // This might indicate "View larger map" was clicked
                        String currentUrl = view.getUrl();
                        if (currentUrl != null && !url.equals(currentUrl) && url.contains("google.com/maps")) {
                            Log.d(TAG, "URL changed significantly - might be View larger map, opening app");
                            openMapForAddress(location);
                            return true;
                        }
                        // It's still an embed URL, allow it to load
                        Log.d(TAG, "Embed URL detected, allowing navigation within iframe");
                        return false;
                    }
                }
                return false;
            }
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Intercept ALL navigation attempts - if it's trying to navigate away from embed, open Maps app
                Log.d(TAG, "Intercepted URL in preview (legacy): " + url);
                
                if (url != null && (url.contains("google.com/maps") || url.contains("maps.google.com"))) {
                    // Check if it's NOT the embed URL - if it's a regular Google Maps URL, open in app
                    boolean isEmbedUrl = url.contains("output=embed") || url.contains("mapclient=embed");
                    
                    if (!isEmbedUrl) {
                        // This is "View larger map" or similar - open in Google Maps app
                        Log.d(TAG, "Non-embed URL detected (legacy) - opening Google Maps app for: " + location);
                        openMapForAddress(location);
                        return true; // Block the default navigation
                    } else {
                        // Check if URL changed significantly (different coordinates or removed embed params)
                        String currentUrl = view.getUrl();
                        if (currentUrl != null && !url.equals(currentUrl) && url.contains("google.com/maps")) {
                            Log.d(TAG, "URL changed significantly (legacy) - might be View larger map, opening app");
                            openMapForAddress(location);
                            return true;
                        }
                        // It's still an embed URL, allow it to load
                        Log.d(TAG, "Embed URL detected (legacy), allowing navigation within iframe");
                        return false;
                    }
                }
                return false;
            }
        });
        
        // Generate Google Maps Embed API URL
        String mapEmbedUrl = generateMapEmbedUrl(location);
        Log.d(TAG, "Loading Google Maps Embed API for address: " + location + ", URL: " + mapEmbedUrl);
        
        // Create HTML with iframe to properly embed Google Maps
        if (mapEmbedUrl != null && !mapEmbedUrl.isEmpty()) {
            String htmlContent = generateMapIframeHtml(mapEmbedUrl);
            webViewMap.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
        }
        
        // Make map preview clickable to open Google Maps app directly
        // Also intercept any navigation attempts from the iframe
        webViewMap.setOnClickListener(v -> {
            Log.d(TAG, "Map preview clicked - opening Google Maps app");
            openMapForAddress(location);
        });
        
        detailsLayout.addView(webViewMap);
        
        // Toggle map visibility when icon is clicked
        ivMapIcon.setOnClickListener(v -> {
            boolean isMapVisible = webViewMap.getVisibility() == View.VISIBLE;
            if (isMapVisible) {
                // Hide map, show map icon
                webViewMap.setVisibility(View.GONE);
                ivMapIcon.setImageResource(R.drawable.ic_map);
                ivMapIcon.setColorFilter(0xFFA18167); // Brown color
            } else {
                // Show map, show close icon
                webViewMap.setVisibility(View.VISIBLE);
                ivMapIcon.setImageResource(R.drawable.ic_close); // Close icon
                ivMapIcon.setColorFilter(0xFFA18167); // Brown color
            }
        });

        cardLayout.addView(detailsLayout);

        // Make card clickable - show dialog when clicking on card details area
        // (Image area click is handled separately by ViewPager2 touch listener)
        cardLayout.setOnClickListener(v -> {
            showGuestRestrictionDialog();
        });
        cardLayout.setClickable(true);
        cardLayout.setFocusable(true);

        layoutListings.addView(cardLayout);
    }

    private void showGuestRestrictionDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_guest_restriction, null);
            builder.setView(dialogView);

            // Initialize views
            MaterialButton btnLogin = dialogView.findViewById(R.id.btnLogin);
            MaterialButton btnSignup = dialogView.findViewById(R.id.btnSignup);

            AlertDialog dialog = builder.create();
            dialog.show();

            // Login button click listener
            btnLogin.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(GuestHomeActivity.this, Login.class);
                startActivity(intent);
            });

            // Signup button click listener
            btnSignup.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(GuestHomeActivity.this, RegistrationActivity.class);
                startActivity(intent);
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error showing dialog", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, Login.class);
        startActivity(intent);
    }

    private void navigateToSignup() {
        Intent intent = new Intent(this, RegistrationActivity.class);
        startActivity(intent);
    }
    
    private String generateMapEmbedUrl(String address) {
        try {
            // Generate Google Maps Embed API URL format
            String encodedAddress = android.net.Uri.encode(address);
            Log.d(TAG, "Generating Google Maps Embed API URL for address: " + address);
            
            // Google Maps Embed API format
            // Note: For production, you'll need to add your Google Maps API key
            // Get your API key from: https://console.cloud.google.com/google/maps-apis
            String apiKey = ""; // Add your Google Maps API key here if needed
            
            // Google Maps Embed API URL format
            // Format: https://www.google.com/maps/embed/v1/place?key=API_KEY&q=ADDRESS&zoom=ZOOM
            String embedUrl;
            if (apiKey != null && !apiKey.isEmpty()) {
                // With API key (recommended for production)
                embedUrl = "https://www.google.com/maps/embed/v1/place?key=" + apiKey + 
                          "&q=" + encodedAddress + 
                          "&zoom=15";
            } else {
                // Use the standard Google Maps embed format (works without API key)
                embedUrl = "https://www.google.com/maps?q=" + encodedAddress + 
                          "&output=embed&zoom=15";
            }
            
            Log.d(TAG, "Generated Google Maps Embed API URL: " + embedUrl);
            return embedUrl;
        } catch (Exception e) {
            Log.e(TAG, "Error generating map embed URL: " + e.getMessage(), e);
            return null;
        }
    }
    
    private String generateMapIframeHtml(String embedUrl) {
        // Generate HTML with iframe to properly embed Google Maps
        // This satisfies Google Maps Embed API requirement for iframe usage
        return "<!DOCTYPE html>" +
               "<html>" +
               "<head>" +
               "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
               "<style>" +
               "body { margin: 0; padding: 0; overflow: hidden; } " +
               "iframe { width: 100%; height: 100%; border: 0; } " +
               "</style>" +
               "</head>" +
               "<body>" +
               "<iframe src=\"" + embedUrl + "\" " +
               "width=\"100%\" " +
               "height=\"100%\" " +
               "frameborder=\"0\" " +
               "style=\"border:0\" " +
               "allowfullscreen>" +
               "</iframe>" +
               "</body>" +
               "</html>";
    }
    
    private void openFullScreenMap(String address) {
        try {
            Log.d(TAG, "Opening full screen map for address: " + address);
            
            // Create a full-screen dialog that covers the ENTIRE GuestHomeActivity screen
            // Use custom style to ensure it covers everything
            android.app.Dialog fullScreenDialog = new android.app.Dialog(this);
            fullScreenDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
            
            // Set full screen window flags - ensure it takes WHOLE screen of GuestHomeActivity
            android.view.Window window = fullScreenDialog.getWindow();
            if (window != null) {
                // Clear any existing flags and set full screen
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN |
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
                    android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN |
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                );
                
                // Get screen dimensions to ensure full coverage
                android.view.Display display = getWindowManager().getDefaultDisplay();
                android.graphics.Point size = new android.graphics.Point();
                display.getSize(size);
                
                // Force full screen dimensions - must cover entire GuestHomeActivity screen
                android.view.WindowManager.LayoutParams params = window.getAttributes();
                params.width = size.x; // Full screen width
                params.height = size.y; // Full screen height
                params.x = 0;
                params.y = 0;
                params.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
                params.format = android.graphics.PixelFormat.TRANSLUCENT;
                params.type = android.view.WindowManager.LayoutParams.TYPE_APPLICATION;
                window.setAttributes(params);
                
                // Ensure window covers entire screen
                window.setLayout(size.x, size.y);
                
                // Hide status and navigation bars completely
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    window.setStatusBarColor(0xFF000000);
                    window.setNavigationBarColor(0xFF000000);
                }
                
                // Set immersive fullscreen mode - hide ALL system UI
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                    window.getDecorView().setSystemUiVisibility(flags);
                }
            }
            
            // Create WebView for full screen map - load non-embed version directly
            // WebView must fill ENTIRE screen with NO padding or margins
            WebView fullScreenWebView = new WebView(this);
            // Layout params will be set later when adding to container
            
            // Configure WebView
            fullScreenWebView.getSettings().setJavaScriptEnabled(true);
            fullScreenWebView.getSettings().setBuiltInZoomControls(true);
            fullScreenWebView.getSettings().setDisplayZoomControls(true);
            fullScreenWebView.getSettings().setUseWideViewPort(true);
            fullScreenWebView.getSettings().setLoadWithOverviewMode(true);
            fullScreenWebView.getSettings().setDomStorageEnabled(true);
            fullScreenWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
            
            // Set WebViewClient
            fullScreenWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    Log.d(TAG, "Full screen map loaded: " + url);
                }
                
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                    // Allow all navigation within the WebView in full screen
                    return false;
                }
                
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    // Allow all navigation within the WebView in full screen
                    return false;
                }
            });
            
            // Load the non-embed version of Google Maps directly (full screen map)
            String encodedAddress = android.net.Uri.encode(address);
            String fullScreenMapUrl = "https://www.google.com/maps?q=" + encodedAddress + "&z=15";
            Log.d(TAG, "Loading full screen map (non-embed) with URL: " + fullScreenMapUrl);
            fullScreenWebView.loadUrl(fullScreenMapUrl);
            
            // Create container with close button - NO PADDING, NO MARGINS
            android.widget.FrameLayout container = new android.widget.FrameLayout(this);
            android.widget.FrameLayout.LayoutParams containerParams = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            );
            containerParams.setMargins(0, 0, 0, 0);
            container.setLayoutParams(containerParams);
            container.setPadding(0, 0, 0, 0);
            container.setBackgroundColor(0xFF000000);
            
            // WebView must fill entire container with NO margins
            fullScreenWebView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            ));
            
            // Close button with rounded background - positioned absolutely
            ImageView closeButton = new ImageView(this);
            closeButton.setImageResource(R.drawable.ic_close);
            closeButton.setColorFilter(0xFFFFFFFF);
            
            // Create rounded background for close button
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
                bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                bg.setColor(0xCC000000); // More opaque black for better visibility
                closeButton.setBackground(bg);
            } else {
                closeButton.setBackgroundColor(0xCC000000);
            }
            
            int buttonSize = (int)(getResources().getDisplayMetrics().density * 48);
            android.widget.FrameLayout.LayoutParams closeParams = new android.widget.FrameLayout.LayoutParams(
                buttonSize,
                buttonSize
            );
            closeParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
            closeParams.setMargins(0, (int)(getResources().getDisplayMetrics().density * 16), 
                                  (int)(getResources().getDisplayMetrics().density * 16), 0);
            closeButton.setLayoutParams(closeParams);
            closeButton.setPadding(
                (int)(getResources().getDisplayMetrics().density * 12),
                (int)(getResources().getDisplayMetrics().density * 12),
                (int)(getResources().getDisplayMetrics().density * 12),
                (int)(getResources().getDisplayMetrics().density * 12)
            );
            closeButton.setClickable(true);
            closeButton.setFocusable(true);
            closeButton.setElevation(16f); // High elevation to ensure it's above map
            closeButton.setOnClickListener(v -> {
                fullScreenDialog.dismiss();
            });
            
            container.addView(fullScreenWebView);
            container.addView(closeButton);
            
            // Set content view with NO padding
            fullScreenDialog.setContentView(container);
            fullScreenDialog.setCancelable(true);
            fullScreenDialog.setCanceledOnTouchOutside(false);
            
            // Show dialog
            fullScreenDialog.show();
            
            // Ensure window is still full screen after show - get actual screen size
            if (window != null) {
                android.view.Display display = getWindowManager().getDefaultDisplay();
                android.graphics.Point size = new android.graphics.Point();
                display.getSize(size);
                
                // Force full screen dimensions again after show
                window.setLayout(size.x, size.y);
                
                // Re-apply window attributes to ensure full coverage
                android.view.WindowManager.LayoutParams params = window.getAttributes();
                params.width = size.x;
                params.height = size.y;
                params.x = 0;
                params.y = 0;
                window.setAttributes(params);
                
                // Re-apply immersive flags
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                    window.getDecorView().setSystemUiVisibility(flags);
                }
            }
            
            Log.d(TAG, "Full screen map dialog opened - covering entire GuestHomeActivity screen");
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening full screen map: " + e.getMessage(), e);
            // Fallback to opening in Google Maps app
            openMapForAddress(address);
        }
    }
    
    private void openMapForAddress(String address) {
        try {
            Log.d(TAG, "Attempting to open Google Maps app for address: " + address);
            String encodedAddress = android.net.Uri.encode(address);
            
            // Method 1: Try to open Google Maps app directly using package name
            try {
                Intent mapIntent = new Intent(android.content.Intent.ACTION_VIEW);
                mapIntent.setData(android.net.Uri.parse("geo:0,0?q=" + encodedAddress));
                mapIntent.setPackage("com.google.android.apps.maps"); // Force Google Maps app
                
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    Log.d(TAG, "Opening Google Maps app directly");
                    startActivity(mapIntent);
                    return;
                }
            } catch (Exception e) {
                Log.d(TAG, "Google Maps app not found, trying alternative methods");
            }
            
            // Method 2: Try geo URI without package name (lets user choose)
            try {
                Intent mapIntent = new Intent(android.content.Intent.ACTION_VIEW);
                mapIntent.setData(android.net.Uri.parse("geo:0,0?q=" + encodedAddress));
                
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    Log.d(TAG, "Opening map app via geo URI");
                    startActivity(mapIntent);
                    return;
                }
            } catch (Exception e) {
                Log.d(TAG, "Geo URI failed, trying web browser");
            }
            
            // Method 3: Try Google Maps URL scheme
            try {
                Intent mapIntent = new Intent(android.content.Intent.ACTION_VIEW);
                mapIntent.setData(android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=" + encodedAddress));
                mapIntent.setPackage("com.google.android.apps.maps"); // Try to force Google Maps
                
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    Log.d(TAG, "Opening Google Maps app via URL");
                    startActivity(mapIntent);
                    return;
                }
            } catch (Exception e) {
                Log.d(TAG, "Google Maps URL failed");
            }
            
            // Fallback: Open in web browser
            Intent webIntent = new Intent(android.content.Intent.ACTION_VIEW);
            webIntent.setData(android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=" + encodedAddress));
            Log.d(TAG, "Falling back to web browser");
            startActivity(webIntent);
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening map: " + e.getMessage(), e);
            Toast.makeText(this, "Unable to open map for this address", Toast.LENGTH_SHORT).show();
        }
    }

}
