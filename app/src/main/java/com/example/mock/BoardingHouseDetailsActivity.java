package com.example.mock;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.mock.adapters.ImageCarouselAdapter;
import com.example.mock.adapters.RoomCategoryAdapter;
import com.google.android.material.button.MaterialButton;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class BoardingHouseDetailsActivity extends AppCompatActivity {
    
    private static final String TAG = "BoardingHouseDetails";
    // Local development URL - Update this to match your local IP
    private static final String BASE_URL = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/";
    private static final String API_URL = BASE_URL + "get_boarding_house_details1.php";
    
    private ViewPager2 viewPagerImages;
    private LinearLayout layoutIndicators, layoutThumbnails;
    private ImageView ivBack;
    private ImageButton btnShare, btnFavorite, btnCall, btnEmail;
    private MaterialButton btnChooseAccommodation;
    
    private TextView tvBoardingHouseName, tvLocation, tvPrice, tvDescription, tvRules, tvReadMore,
                     tvBathrooms, tvArea, tvYear, tvStatus,
                     tvOwnerName, tvOwnerRole, tvOwnerPhone, tvOwnerEmail,
                     tvReviewCount, tvNoReviews, tvSeeAllReviews;
    private ImageView ivOwnerProfile;
    private RecyclerView rvRoomCategories, rvReviews;
    private HorizontalScrollView scrollViewThumbnails;
    private ProgressBar progressBar;
    private WebView webViewMap;
    private AlertDialog loadingDialog;
    private boolean isDescriptionExpanded = false;
    
    private ImageCarouselAdapter imageAdapter;
    private RoomCategoryAdapter roomCategoryAdapter;
    private ReviewsAdapter reviewsAdapter;
    private List<String> imageUrls;
    private List<String> roomCategories;
    private List<Review> reviews;
    private List<ImageView> thumbnailViews = new ArrayList<>();
    private List<android.widget.FrameLayout> thumbnailWrappers = new ArrayList<>();
    private List<ImageView> indicatorViews = new ArrayList<>();
    private int currentImagePosition = 0;
    
    private int boardingHouseId;
    private BoardingHouseDetails boardingHouseDetails;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boarding_house_details);
        
        // Get data from intent
        getIntentData();
        
        // Initialize views
        initializeViews();
        
        // Setup click listeners
        setupClickListeners();
        
        // Load boarding house details
        loadBoardingHouseDetails();
    }
    
    private void getIntentData() {
        Intent intent = getIntent();
        boardingHouseId = intent.getIntExtra("bh_id", 0);
        
        if (boardingHouseId == 0) {
            Toast.makeText(this, "Invalid boarding house ID", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void initializeViews() {
        // Image carousel
        viewPagerImages = findViewById(R.id.viewPagerImages);
        layoutIndicators = findViewById(R.id.layoutIndicators);
        layoutThumbnails = findViewById(R.id.layoutThumbnails);
        scrollViewThumbnails = findViewById(R.id.scrollViewThumbnails);
        
        // Buttons
        ivBack = findViewById(R.id.ivBack);
        btnShare = findViewById(R.id.btnShare);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnCall = findViewById(R.id.btnCall);
        btnEmail = findViewById(R.id.btnEmail);
        btnChooseAccommodation = findViewById(R.id.btnChooseAccommodation);
        
        // Text views
        tvBoardingHouseName = findViewById(R.id.tvBoardingHouseName);
        tvLocation = findViewById(R.id.tvLocation);
        tvPrice = findViewById(R.id.tvPrice);
        tvDescription = findViewById(R.id.tvDescription);
        tvReadMore = findViewById(R.id.tvReadMore);
        tvRules = findViewById(R.id.tvRules);
        tvBathrooms = findViewById(R.id.tvBathrooms);
        tvArea = findViewById(R.id.tvArea);
        tvYear = findViewById(R.id.tvYear);
        tvStatus = findViewById(R.id.tvStatus);
        tvOwnerName = findViewById(R.id.tvOwnerName);
        tvOwnerRole = findViewById(R.id.tvOwnerRole);
        tvOwnerPhone = findViewById(R.id.tvOwnerPhone);
        tvOwnerEmail = findViewById(R.id.tvOwnerEmail);
        tvReviewCount = findViewById(R.id.tvReviewCount);
        tvNoReviews = findViewById(R.id.tvNoReviews);
        tvSeeAllReviews = findViewById(R.id.tvSeeAllReviews);
        
        // Image views
        ivOwnerProfile = findViewById(R.id.ivOwnerProfile);
        
        // Other views
        rvRoomCategories = findViewById(R.id.rvRoomCategories);
        rvReviews = findViewById(R.id.rvReviews);
        progressBar = findViewById(R.id.progressBar);
        webViewMap = findViewById(R.id.webViewMap);
        
        // Hide progress bar (we'll use dialog instead)
        progressBar.setVisibility(View.GONE);
        
        // Initialize lists
        imageUrls = new ArrayList<>();
        roomCategories = new ArrayList<>();
        reviews = new ArrayList<>();
        
        // Setup room categories recycler view
        roomCategoryAdapter = new RoomCategoryAdapter(roomCategories);
        rvRoomCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvRoomCategories.setAdapter(roomCategoryAdapter);
        
        // Setup reviews recycler view - horizontal scrolling
        reviewsAdapter = new ReviewsAdapter((ArrayList<Review>) reviews, this);
        reviewsAdapter.setOnReviewClickListener((position, review) -> {
            // Navigate to AllReviewsActivity when review card is clicked
            openAllReviewsActivity();
        });
        rvReviews.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvReviews.setAdapter(reviewsAdapter);
        
        // Setup read more click listener
        tvReadMore.setOnClickListener(v -> toggleDescription());
        
        // Configure WebView for map
        setupMapWebView();
    }
    
    private void setupMapWebView() {
        webViewMap.getSettings().setJavaScriptEnabled(true);
        webViewMap.getSettings().setBuiltInZoomControls(false);
        webViewMap.getSettings().setDisplayZoomControls(false);
        webViewMap.getSettings().setSupportZoom(false);
        webViewMap.getSettings().setUseWideViewPort(true);
        webViewMap.getSettings().setLoadWithOverviewMode(true);
        webViewMap.getSettings().setLayoutAlgorithm(android.webkit.WebSettings.LayoutAlgorithm.NORMAL);
        webViewMap.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webViewMap.setHorizontalScrollBarEnabled(false);
        webViewMap.setVerticalScrollBarEnabled(false);
        webViewMap.setBackgroundColor(0xFFF5F5F5);
        
        // Ensure WebView fits exactly in its container - remove any padding
        webViewMap.setPadding(0, 0, 0, 0);
        
        // Enable clipping to rounded corners
        webViewMap.setClipToOutline(true);
        
        // Set outline for rounded corners to match CardView
        float cornerRadius = getResources().getDisplayMetrics().density * 12; // 12dp radius
        webViewMap.post(() -> {
            webViewMap.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(android.view.View view, android.graphics.Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadius);
                }
            });
            webViewMap.setClipToOutline(true);
        });
        
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
                        Log.d(TAG, "Non-embed URL detected - opening Google Maps app");
                        if (boardingHouseDetails != null) {
                            openMapForAddress(boardingHouseDetails.getBhAddress());
                        }
                        return true; // Block the default navigation
                    } else {
                        // Check if URL changed significantly (different coordinates or removed embed params)
                        // This might indicate "View larger map" was clicked
                        String currentUrl = view.getUrl();
                        if (currentUrl != null && !url.equals(currentUrl) && url.contains("google.com/maps")) {
                            Log.d(TAG, "URL changed significantly - might be View larger map, opening app");
                            if (boardingHouseDetails != null) {
                                openMapForAddress(boardingHouseDetails.getBhAddress());
                            }
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
                        Log.d(TAG, "Non-embed URL detected (legacy) - opening Google Maps app");
                        if (boardingHouseDetails != null) {
                            openMapForAddress(boardingHouseDetails.getBhAddress());
                        }
                        return true; // Block the default navigation
                    } else {
                        // Check if URL changed significantly (different coordinates or removed embed params)
                        String currentUrl = view.getUrl();
                        if (currentUrl != null && !url.equals(currentUrl) && url.contains("google.com/maps")) {
                            Log.d(TAG, "URL changed significantly (legacy) - might be View larger map, opening app");
                            if (boardingHouseDetails != null) {
                                openMapForAddress(boardingHouseDetails.getBhAddress());
                            }
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
        
        // Make map preview clickable to open Google Maps app
        webViewMap.setOnClickListener(v -> {
            Log.d(TAG, "Map preview clicked - opening Google Maps app");
            if (boardingHouseDetails != null) {
                openMapForAddress(boardingHouseDetails.getBhAddress());
            }
        });
    }
    
    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
        
        btnShare.setOnClickListener(v -> shareBoardingHouse());
        
        btnFavorite.setOnClickListener(v -> toggleFavorite());
        
        btnCall.setOnClickListener(v -> contactOwner());
        
        btnEmail.setOnClickListener(v -> emailOwner());
        
        btnChooseAccommodation.setOnClickListener(v -> openChooseAccommodationActivity());
        
        tvSeeAllReviews.setOnClickListener(v -> openAllReviewsActivity());
    }
    
    private void loadBoardingHouseDetails() {
        showLoadingDialog();
        
        String url = API_URL + "?bh_id=" + boardingHouseId;
        Log.d(TAG, "Loading boarding house details for ID: " + boardingHouseId);
        Log.d(TAG, "API URL: " + url);
        
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        hideLoadingDialog();
                        
                        // Debug: Log the first 200 characters of response
                        Log.d(TAG, "Response preview: " + response.substring(0, Math.min(200, response.length())));
                        
                        // Debug: Check if response contains bh_rules
                        if (response.contains("bh_rules")) {
                            Log.d(TAG, "Response contains 'bh_rules' field");
                        } else {
                            Log.d(TAG, "Response does NOT contain 'bh_rules' field");
                        }
                        
                        // Check if response is HTML (ngrok warning page)
                        if (response.trim().startsWith("<!DOCTYPE html>") || (response.contains("ngrok") && response.contains("<html"))) {
                            Log.e(TAG, "Received ngrok warning page instead of JSON");
                            Log.e(TAG, "Full response: " + response);
                            Log.e(TAG, "SOLUTION: Visit https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/get_boarding_house_details1.php in your browser first");
                            Toast.makeText(BoardingHouseDetailsActivity.this, "Ngrok warning! Visit API URL in browser first.", Toast.LENGTH_LONG).show();
                            // Show fallback data for mock listings
                            showFallbackData();
                            return;
                        }
                        
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success");
                            
                            if (success) {
                                JSONObject data = jsonResponse.getJSONObject("data");
                                
                                // Check if data is wrapped in "boarding_house" object
                                JSONObject boardingHouseData;
                                if (data.has("boarding_house")) {
                                    boardingHouseData = data.getJSONObject("boarding_house");
                                    
                                    // Debug: Check if owner object exists
                                    if (boardingHouseData.has("owner")) {
                                        JSONObject ownerObj = boardingHouseData.getJSONObject("owner");
                                        Log.d(TAG, "Owner object found in response: " + ownerObj.toString());
                                        Log.d(TAG, "Owner first_name: " + ownerObj.optString("first_name", "null"));
                                        Log.d(TAG, "Owner phone: " + ownerObj.optString("phone", "null"));
                                        Log.d(TAG, "Owner email: " + ownerObj.optString("email", "null"));
                                    } else {
                                        Log.e(TAG, "WARNING: Owner object NOT found in boarding_house data!");
                                        Log.d(TAG, "Available keys in boarding_house: " + boardingHouseData.keys().toString());
                                    }
                                    
                                    // Also get rooms and statistics if available
                                    if (data.has("rooms")) {
                                        boardingHouseData.put("rooms", data.getJSONArray("rooms"));
                                    }
                                    if (data.has("statistics")) {
                                        boardingHouseData.put("statistics", data.getJSONObject("statistics"));
                                    }
                                } else {
                                    boardingHouseData = data;
                                }
                                
                                parseBoardingHouseDetails(boardingHouseData);
                                displayBoardingHouseDetails();
                            } else {
                                String error = jsonResponse.optString("error", "Unknown error occurred");
                                Log.e(TAG, "API Error: " + error);
                                // If boarding house not found, show fallback data
                                if (error.contains("not found")) {
                                    showFallbackData();
                                } else {
                                    Toast.makeText(BoardingHouseDetailsActivity.this, "Failed to load details: " + error, Toast.LENGTH_LONG).show();
                                }
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            Log.e(TAG, "Response that failed to parse: " + response);
                            // Show fallback data for mock listings
                            showFallbackData();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        hideLoadingDialog();
                        Log.e(TAG, "Volley error: " + error.getMessage());
                        Toast.makeText(BoardingHouseDetailsActivity.this, "Network error: " + error.getMessage(), Toast.LENGTH_LONG).show();
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
    
    private void parseBoardingHouseDetails(JSONObject data) throws JSONException {
        boardingHouseDetails = new BoardingHouseDetails();
        
        // Basic info from boarding_houses table
        boardingHouseDetails.setBhId(data.getInt("bh_id"));
        boardingHouseDetails.setBhName(data.getString("bh_name"));
        boardingHouseDetails.setBhAddress(data.getString("bh_address"));
        boardingHouseDetails.setBhDescription(data.getString("bh_description"));
        
        // Debug bh_rules data
        String bhRules = data.optString("bh_rules", "No specific rules");
        Log.d(TAG, "Raw bh_rules from API: '" + bhRules + "'");
        Log.d(TAG, "bh_rules length: " + bhRules.length());
        Log.d(TAG, "bh_rules is empty: " + bhRules.isEmpty());
        Log.d(TAG, "bh_rules equals 'No specific rules': " + bhRules.equals("No specific rules"));
        
        boardingHouseDetails.setBhRules(bhRules);
        boardingHouseDetails.setNumberOfBathroom(data.optInt("number_of_bathroom", 1));
        boardingHouseDetails.setArea(data.optDouble("area", 100.0));
        boardingHouseDetails.setBuildYear(data.optInt("build_year", 2020));
        boardingHouseDetails.setStatus(data.getString("status"));
        boardingHouseDetails.setBhCreatedAt(data.getString("bh_created_at"));
        
        // Images from boarding_house_images table
        List<String> images = new ArrayList<>();
        if (data.has("images")) {
            JSONArray imagesArray = data.getJSONArray("images");
            for (int i = 0; i < imagesArray.length(); i++) {
                images.add(imagesArray.getString(i));
            }
        }
        // If no images, add placeholder
        if (images.isEmpty()) {
            images.add("https://via.placeholder.com/400x300?text=No+Image+Available");
        }
        boardingHouseDetails.setImages(images);
        
        // Room details from boarding_house_rooms table
        List<String> categories = new ArrayList<>();
        List<BoardingHouseDetails.RoomDetail> roomDetails = new ArrayList<>();
        int minPrice = Integer.MAX_VALUE;
        int maxPrice = 0;
        
        if (data.has("room_details")) {
            JSONArray roomDetailsArray = data.getJSONArray("room_details");
            for (int i = 0; i < roomDetailsArray.length(); i++) {
                JSONObject roomJson = roomDetailsArray.getJSONObject(i);
                
                // Add to categories if not already present
                String category = roomJson.getString("room_category");
                if (!categories.contains(category)) {
                    categories.add(category);
                }
                
                // Create room detail
                BoardingHouseDetails.RoomDetail room = new BoardingHouseDetails.RoomDetail();
                room.setRoomCategory(category);
                room.setRoomName(roomJson.getString("room_name"));
                room.setPrice(roomJson.getInt("price"));
                room.setCapacity(roomJson.getInt("capacity"));
                room.setRoomDescription(roomJson.getString("room_description"));
                room.setTotalRooms(roomJson.getInt("total_rooms"));
                roomDetails.add(room);
                
                // Track price range
                int price = roomJson.getInt("price");
                if (price < minPrice) minPrice = price;
                if (price > maxPrice) maxPrice = price;
            }
        }
        
        // If no rooms, add default categories
        if (categories.isEmpty()) {
            categories.add("Private Room");
            categories.add("Bed Spacer");
        }
        
        boardingHouseDetails.setRoomCategories(categories);
        boardingHouseDetails.setRoomDetails(roomDetails);
        
        // Set price range from API or calculated values
        if (data.has("min_price") && !data.isNull("min_price")) {
            boardingHouseDetails.setMinPrice(data.getInt("min_price"));
        } else if (minPrice != Integer.MAX_VALUE) {
            boardingHouseDetails.setMinPrice(minPrice);
        }
        
        if (data.has("max_price") && !data.isNull("max_price")) {
            boardingHouseDetails.setMaxPrice(data.getInt("max_price"));
        } else if (maxPrice > 0) {
            boardingHouseDetails.setMaxPrice(maxPrice);
        }
        
        // Owner info from registrations table - owner data is nested in "owner" object
        BoardingHouseDetails.OwnerInfo owner = new BoardingHouseDetails.OwnerInfo();
        
        // Try to get owner info from nested "owner" object first
        if (data.has("owner")) {
            JSONObject ownerObj = data.getJSONObject("owner");
            owner.setFirstName(ownerObj.optString("first_name", ""));
            owner.setMiddleName(ownerObj.optString("middle_name", ""));
            owner.setLastName(ownerObj.optString("last_name", ""));
            owner.setPhone(ownerObj.optString("phone", ""));
            owner.setEmail(ownerObj.optString("email", ""));
            owner.setRole(ownerObj.optString("role", ""));
            owner.setProfilePicture(ownerObj.optString("profile_picture", ""));
        } else {
            // Fallback: try to get from main data object (for backward compatibility)
            owner.setFirstName(data.optString("first_name", ""));
            owner.setMiddleName(data.optString("middle_name", ""));
            owner.setLastName(data.optString("last_name", ""));
            owner.setPhone(data.optString("phone", ""));
            owner.setEmail(data.optString("email", ""));
            owner.setRole(data.optString("role", ""));
            owner.setProfilePicture(data.optString("profile_picture", ""));
        }
        
        boardingHouseDetails.setOwner(owner);
        
        // Debug: Log owner information
        Log.d(TAG, "Owner info - Name: " + owner.getFirstName() + " " + owner.getLastName() + 
                   ", Phone: " + owner.getPhone() + ", Email: " + owner.getEmail());
    }
    
    private void displayBoardingHouseDetails() {
        if (boardingHouseDetails == null) return;
        
        // Basic info
        tvBoardingHouseName.setText(boardingHouseDetails.getBhName());
        tvBoardingHouseName.setAlpha(1.0f);
        tvLocation.setText(boardingHouseDetails.getBhAddress());
        tvLocation.setAlpha(1.0f);
        
        // Set price with brown color only for the amount, not "/month"
        String priceText = boardingHouseDetails.getFormattedPriceRange();
        SpannableString spannablePrice = new SpannableString(priceText);
        int brownColor = Color.parseColor("#A18167");
        int grayColor = Color.parseColor("#666666");
        
        // Find the position of "/month" and color everything before it brown, "/month" gray
        int monthIndex = priceText.indexOf("/month");
        if (monthIndex != -1) {
            // Color the amount part (before "/month") brown and make it bigger
            spannablePrice.setSpan(new ForegroundColorSpan(brownColor), 0, monthIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannablePrice.setSpan(new RelativeSizeSpan(1.2f), 0, monthIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            // Color "/month" gray and keep it normal size
            spannablePrice.setSpan(new ForegroundColorSpan(grayColor), monthIndex, priceText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            // If no "/month" found, color entire text brown and make it bigger
            spannablePrice.setSpan(new ForegroundColorSpan(brownColor), 0, priceText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannablePrice.setSpan(new RelativeSizeSpan(1.2f), 0, priceText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        tvPrice.setText(spannablePrice);
        tvPrice.setAlpha(1.0f);
        
        // Description with read more
        String description = boardingHouseDetails.getBhDescription();
        tvDescription.setText(description);
        tvDescription.setAlpha(1.0f);
        // Set justification mode programmatically (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tvDescription.setJustificationMode(android.text.Layout.JUSTIFICATION_MODE_INTER_WORD);
        }
        if (description.length() > 100) {
            tvDescription.setMaxLines(3);
            tvReadMore.setVisibility(View.VISIBLE);
        } else {
            tvReadMore.setVisibility(View.GONE);
        }
        
        // Rules (hide if empty or default)
        String rules = boardingHouseDetails.getBhRules();
        if (rules != null && !rules.isEmpty() && !rules.equals("No specific rules")) {
            findViewById(R.id.layoutRules).setVisibility(View.VISIBLE);
            tvRules.setText(rules);
            // Set justification mode programmatically (API 26+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                tvRules.setJustificationMode(android.text.Layout.JUSTIFICATION_MODE_INTER_WORD);
            }
        } else {
            findViewById(R.id.layoutRules).setVisibility(View.GONE);
        }
        
        // BH Details
        tvBathrooms.setText(String.valueOf(boardingHouseDetails.getNumberOfBathroom()));
        tvBathrooms.setAlpha(1.0f);
        tvArea.setText(String.format("%.1f sqft", boardingHouseDetails.getArea()));
        tvArea.setAlpha(1.0f);
        tvYear.setText(String.valueOf(boardingHouseDetails.getBuildYear()));
        tvYear.setAlpha(1.0f);
        
        // Set status - if "active" show "For Rent"
        String status = boardingHouseDetails.getStatus();
        if (status != null && status.equalsIgnoreCase("active")) {
            tvStatus.setText("For Rent");
        } else {
            tvStatus.setText(status != null && !status.isEmpty() ? status : "For Rent");
        }
        tvStatus.setAlpha(1.0f);
        
        // Owner info
        String ownerName = boardingHouseDetails.getOwnerFullName();
        tvOwnerName.setText(ownerName != null && !ownerName.trim().isEmpty() ? ownerName : "Owner");
        tvOwnerName.setAlpha(1.0f);
        
        String ownerRole = boardingHouseDetails.getOwner().getRole();
        tvOwnerRole.setText(ownerRole != null && !ownerRole.isEmpty() ? 
                           capitalizeFirst(ownerRole) : "Property Owner");
        tvOwnerRole.setAlpha(1.0f);
        
        // Owner contact info
        String ownerPhone = boardingHouseDetails.getOwner().getPhone();
        tvOwnerPhone.setText(ownerPhone != null && !ownerPhone.isEmpty() ? ownerPhone : "N/A");
        tvOwnerPhone.setAlpha(1.0f);
        
        String ownerEmail = boardingHouseDetails.getOwner().getEmail();
        tvOwnerEmail.setText(ownerEmail != null && !ownerEmail.isEmpty() ? ownerEmail : "N/A");
        tvOwnerEmail.setAlpha(1.0f);
        
        // Load owner profile picture if available
        loadOwnerProfilePicture();
        
        // Setup image carousel
        setupImageCarousel();
        
        // Setup thumbnails (show even if only one image)
        if (imageUrls != null && !imageUrls.isEmpty()) {
            setupThumbnails();
            scrollViewThumbnails.setVisibility(View.VISIBLE);
        } else {
            scrollViewThumbnails.setVisibility(View.GONE);
        }
        
        // Setup room categories
        setupRoomCategories();
        
        // Setup reviews (for now, show no reviews)
        setupReviews();
        
        // Load map
        loadMap();
    }
    
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    private void toggleDescription() {
        if (isDescriptionExpanded) {
            tvDescription.setMaxLines(3);
            tvReadMore.setText("Read more");
            isDescriptionExpanded = false;
        } else {
            tvDescription.setMaxLines(Integer.MAX_VALUE);
            tvReadMore.setText("Read less");
            isDescriptionExpanded = true;
        }
    }
    
    private void setupThumbnails() {
        layoutThumbnails.removeAllViews();
        thumbnailViews.clear();
        thumbnailWrappers.clear();
        
        if (imageUrls == null || imageUrls.isEmpty()) return;
        
        // Show all images as thumbnails (scrollable)
        for (int i = 0; i < imageUrls.size(); i++) {
            // Create FrameLayout wrapper for border
            android.widget.FrameLayout wrapper = new android.widget.FrameLayout(this);
            LinearLayout.LayoutParams wrapperParams = new LinearLayout.LayoutParams(
                (int) (getResources().getDisplayMetrics().density * 80),
                (int) (getResources().getDisplayMetrics().density * 80)
            );
            wrapperParams.setMargins(0, 0, 12, 0);
            wrapper.setLayoutParams(wrapperParams);
            
            // Set border background
            if (i == 0) {
                wrapper.setBackgroundResource(R.drawable.thumbnail_border_active);
            } else {
                wrapper.setBackgroundResource(R.drawable.thumbnail_border_inactive);
            }
            
            // Create ImageView for the actual image
            ImageView thumbnail = new ImageView(this);
            android.widget.FrameLayout.LayoutParams imageParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            );
            imageParams.setMargins(4, 4, 4, 4); // Padding inside border
            thumbnail.setLayoutParams(imageParams);
            thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            
            // Create rounded shape for clipping the image
            float cornerRadiusPx = getResources().getDisplayMetrics().density * 6; // 6dp radius
            android.graphics.drawable.GradientDrawable roundedShape = new android.graphics.drawable.GradientDrawable();
            roundedShape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            roundedShape.setCornerRadius(cornerRadiusPx);
            thumbnail.setBackground(roundedShape);
            
            // Set outline for proper clipping - need to do this after layout
            thumbnail.post(() -> {
                thumbnail.setOutlineProvider(new android.view.ViewOutlineProvider() {
                    @Override
                    public void getOutline(android.view.View view, android.graphics.Outline outline) {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), cornerRadiusPx);
                    }
                });
                thumbnail.setClipToOutline(true);
            });
            
            // Load actual image using Glide with rounded corners
            String imageUrl = imageUrls.get(i);
            int cornerRadius = (int) cornerRadiusPx; // 6dp rounded corners
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .apply(RequestOptions.bitmapTransform(new RoundedCorners(cornerRadius)))
                .centerCrop()
                .into(thumbnail);
            
            // Add elevation for raised effect
            thumbnail.setElevation(2f);
            
            // Add image to wrapper
            wrapper.addView(thumbnail);
            
            final int position = i;
            wrapper.setOnClickListener(v -> {
                viewPagerImages.setCurrentItem(position, true);
            });
            
            thumbnailViews.add(thumbnail);
            thumbnailWrappers.add(wrapper);
            layoutThumbnails.addView(wrapper);
        }
    }
    
    private void updateThumbnailSelection(int position) {
        // Update all thumbnails based on current position
        for (int i = 0; i < thumbnailWrappers.size(); i++) {
            android.widget.FrameLayout wrapper = thumbnailWrappers.get(i);
            if (i == position) {
                // Active thumbnail - brown border
                wrapper.setBackgroundResource(R.drawable.thumbnail_border_active);
                // Scroll to make the active thumbnail visible
                scrollViewThumbnails.post(() -> {
                    int scrollX = wrapper.getLeft() - (scrollViewThumbnails.getWidth() / 2) + (wrapper.getWidth() / 2);
                    scrollViewThumbnails.smoothScrollTo(Math.max(0, scrollX), 0);
                });
            } else {
                // Inactive thumbnail - gray border
                wrapper.setBackgroundResource(R.drawable.thumbnail_border_inactive);
            }
        }
    }
    
    private void loadOwnerProfilePicture() {
        if (boardingHouseDetails == null || boardingHouseDetails.getOwner() == null) {
            ivOwnerProfile.setImageResource(R.drawable.ic_profile_placeholder);
            return;
        }
        
        String profilePicturePath = boardingHouseDetails.getOwner().getProfilePicture();
        
        if (profilePicturePath != null && !profilePicturePath.isEmpty()) {
            // Construct full URL
            String fullImageUrl;
            if (profilePicturePath.startsWith("http://") || profilePicturePath.startsWith("https://")) {
                // Already a full URL
                fullImageUrl = profilePicturePath;
            } else {
                // Relative path, prepend BASE_URL
                fullImageUrl = BASE_URL + profilePicturePath;
            }
            
            Log.d(TAG, "Loading owner profile picture from: " + fullImageUrl);
            
            // Load image using Glide
            Glide.with(this)
                .load(fullImageUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .circleCrop() // Make it circular
                .into(ivOwnerProfile);
        } else {
            // No profile picture, use placeholder
            ivOwnerProfile.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }
    
    private void setupReviews() {
        // Load real reviews from API for this boarding house
        reviews.clear();
        reviewsAdapter.notifyDataSetChanged();
        
        // Show loading state
        rvReviews.setVisibility(View.GONE);
        tvNoReviews.setVisibility(View.GONE);
        tvSeeAllReviews.setVisibility(View.GONE);
        
        ReviewApiService apiService = new ReviewApiService(this);
        // Get reviews for this specific boarding house (show first 3 for preview)
        apiService.getReviews(boardingHouseId, 0, 0, "all", "published", "newest", new ReviewApiService.ReviewApiCallback() {
            @Override
            public void onSuccess(List<Review> reviewList) {
                reviews.clear();
                // Show only first 3 reviews for preview in horizontal scroll
                int maxReviews = Math.min(reviewList.size(), 3);
                for (int i = 0; i < maxReviews; i++) {
                    reviews.add(reviewList.get(i));
                }
                
        reviewsAdapter.notifyDataSetChanged();
        
        // Show reviews if available
        if (reviews.isEmpty()) {
            rvReviews.setVisibility(View.GONE);
            tvNoReviews.setVisibility(View.VISIBLE);
            tvSeeAllReviews.setVisibility(View.GONE);
            tvReviewCount.setText("0");
        } else {
            rvReviews.setVisibility(View.VISIBLE);
            tvNoReviews.setVisibility(View.GONE);
            tvSeeAllReviews.setVisibility(View.VISIBLE);
                    // Show total count from API
                    tvReviewCount.setText(String.valueOf(reviewList.size()));
                }
                
                Log.d(TAG, "Loaded " + reviews.size() + " reviews (showing " + maxReviews + " for preview)");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading reviews: " + error);
                // Show empty state on error
                rvReviews.setVisibility(View.GONE);
                tvNoReviews.setVisibility(View.VISIBLE);
                tvSeeAllReviews.setVisibility(View.GONE);
                tvReviewCount.setText("0");
            }
        });
    }
    
    private void openAllReviewsActivity() {
        Intent intent = new Intent(this, AllReviewsActivity.class);
        intent.putExtra("bh_id", boardingHouseId);
        intent.putExtra("bh_name", boardingHouseDetails != null ? boardingHouseDetails.getBhName() : "Boarding House");
        startActivity(intent);
    }
    
    private void setupImageCarousel() {
        imageUrls.clear();
        imageUrls.addAll(boardingHouseDetails.getImages());
        
        if (imageUrls.isEmpty()) {
            // Add placeholder image if no images
            imageUrls.add("https://via.placeholder.com/400x300?text=No+Image");
        }
            
        imageAdapter = new ImageCarouselAdapter(imageUrls);
        viewPagerImages.setAdapter(imageAdapter);
        
        // Setup indicators
        setupIndicators();
        
        // Setup page change listener to update thumbnails and indicators
        viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentImagePosition = position;
                updateThumbnailSelection(position);
                updateIndicatorSelection(position);
            }
        });
        
        // Set initial selection
        updateThumbnailSelection(0);
        updateIndicatorSelection(0);
    }
    
    private void setupIndicators() {
        layoutIndicators.removeAllViews();
        indicatorViews.clear();
        
        for (int i = 0; i < imageUrls.size(); i++) {
            ImageView indicator = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            indicator.setLayoutParams(params);
            
            // Set initial state (first one active)
            if (i == 0) {
                indicator.setImageResource(R.drawable.dot_active);
            } else {
                indicator.setImageResource(R.drawable.dot_inactive);
            }
            
            indicatorViews.add(indicator);
            layoutIndicators.addView(indicator);
        }
    }
    
    private void updateIndicatorSelection(int position) {
        for (int i = 0; i < indicatorViews.size(); i++) {
            ImageView indicator = indicatorViews.get(i);
            if (i == position) {
                // Active indicator - bold and colored (brown)
                indicator.setImageResource(R.drawable.dot_active);
            } else {
                // Inactive indicator - faded
                indicator.setImageResource(R.drawable.dot_inactive);
            }
        }
    }
    
    private void setupRoomCategories() {
        roomCategories.clear();
        roomCategories.addAll(boardingHouseDetails.getRoomCategories());
        roomCategoryAdapter.notifyDataSetChanged();
    }
    
    private void shareBoardingHouse() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareText = "Check out this boarding house: " + boardingHouseDetails.getBhName() + 
                          "\nLocation: " + boardingHouseDetails.getBhAddress() + 
                          "\nPrice: " + boardingHouseDetails.getFormattedPriceRange();
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share Boarding House"));
    }
    
    private void toggleFavorite() {
            // TODO: Implement favorite functionality
        Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
    }
    
    private void contactOwner() {
        if (boardingHouseDetails.getOwner().getPhone() != null && !boardingHouseDetails.getOwner().getPhone().isEmpty()) {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:" + boardingHouseDetails.getOwner().getPhone()));
            startActivity(callIntent);
                } else {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void emailOwner() {
        if (boardingHouseDetails.getOwner().getEmail() != null && !boardingHouseDetails.getOwner().getEmail().isEmpty()) {
            String email = boardingHouseDetails.getOwner().getEmail();
            String subject = "Inquiry about " + boardingHouseDetails.getBhName();
            
            // Try to open Gmail specifically first
            Intent gmailIntent = new Intent(Intent.ACTION_SEND);
            gmailIntent.setType("message/rfc822");
            gmailIntent.setPackage("com.google.android.gm");
            gmailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
            gmailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            
            // If Gmail is available, use it
            if (gmailIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(gmailIntent);
            } else {
                // Fallback to generic email intent
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setType("message/rfc822");
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                
                // Try ACTION_SENDTO as another fallback
                if (emailIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(Intent.createChooser(emailIntent, "Send email"));
                } else {
                    // Last resort: use mailto URI
                    Intent mailtoIntent = new Intent(Intent.ACTION_SENDTO);
                    mailtoIntent.setData(Uri.parse("mailto:" + email + "?subject=" + Uri.encode(subject)));
                    if (mailtoIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mailtoIntent);
                    } else {
                        Toast.makeText(this, "No email app available. Please install Gmail or another email app.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } else {
            Toast.makeText(this, "Email address not available", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openChooseAccommodationActivity() {
        Intent intent = new Intent(this, ChooseAccommodationActivity.class);
        intent.putExtra("bh_id", boardingHouseId);
        startActivity(intent);
    }
    
    private void loadMap() {
        if (boardingHouseDetails == null || boardingHouseDetails.getBhAddress() == null) {
            return;
        }
        
        String address = boardingHouseDetails.getBhAddress();
        String mapEmbedUrl = generateMapEmbedUrl(address);
        Log.d(TAG, "Loading Google Maps Embed API for address: " + address + ", URL: " + mapEmbedUrl);
        
        // Create HTML with iframe to properly embed Google Maps
        if (mapEmbedUrl != null && !mapEmbedUrl.isEmpty()) {
            String htmlContent = generateMapIframeHtml(mapEmbedUrl);
            webViewMap.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
        }
    }
    
    private String generateMapEmbedUrl(String address) {
        try {
            // Generate Google Maps Embed API URL format
            String encodedAddress = Uri.encode(address);
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
        // Ensure exact fit with no margins or padding
        return "<!DOCTYPE html>" +
               "<html>" +
               "<head>" +
               "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\">" +
               "<style>" +
               "* { margin: 0; padding: 0; box-sizing: border-box; } " +
               "html, body { width: 100%; height: 100%; overflow: hidden; margin: 0; padding: 0; } " +
               "iframe { width: 100%; height: 100%; border: 0; margin: 0; padding: 0; display: block; } " +
               "</style>" +
               "</head>" +
               "<body style=\"margin:0; padding:0; overflow:hidden;\">" +
               "<iframe src=\"" + embedUrl + "\" " +
               "width=\"100%\" " +
               "height=\"100%\" " +
               "frameborder=\"0\" " +
               "style=\"border:0; margin:0; padding:0; width:100%; height:100%;\" " +
               "allowfullscreen>" +
               "</iframe>" +
               "</body>" +
               "</html>";
    }
    
    private void openFullScreenMap(String address) {
        try {
            Log.d(TAG, "Opening full screen map for address: " + address);
            
            // Create a full-screen dialog that covers the ENTIRE screen
            android.app.Dialog fullScreenDialog = new android.app.Dialog(this);
            fullScreenDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
            
            // Set full screen window flags - ensure it takes WHOLE screen
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
                
                // Force full screen dimensions - must cover entire screen
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
            String encodedAddress = Uri.encode(address);
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
            
            Log.d(TAG, "Full screen map dialog opened - covering entire screen");
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening full screen map: " + e.getMessage(), e);
            // Fallback to opening in Google Maps app
            openMapForAddress(address);
        }
    }
    
    private void openMapForAddress(String address) {
        try {
            Log.d(TAG, "Attempting to open Google Maps app for address: " + address);
            String encodedAddress = Uri.encode(address);
            
            // Method 1: Try to open Google Maps app directly using package name
            try {
                Intent mapIntent = new Intent(Intent.ACTION_VIEW);
                mapIntent.setData(Uri.parse("geo:0,0?q=" + encodedAddress));
                mapIntent.setPackage("com.google.android.apps.maps"); // Force Google Maps app
                
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    Log.d(TAG, "Opening Google Maps app directly");
                    startActivity(mapIntent);
                    return;
                } else {
                    Log.d(TAG, "Google Maps app not found, trying alternative methods");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error opening Google Maps app: " + e.getMessage());
            }
            
            // Method 2: Try generic geo URI
            try {
                Intent mapIntent = new Intent(Intent.ACTION_VIEW);
                mapIntent.setData(Uri.parse("geo:0,0?q=" + encodedAddress));
                
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    Log.d(TAG, "Opening map app via geo URI");
                    startActivity(mapIntent);
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error opening map via geo URI: " + e.getMessage());
            }
            
            // Method 3: Try Google Maps URL scheme
            try {
                Intent mapIntent = new Intent(Intent.ACTION_VIEW);
                mapIntent.setData(Uri.parse("https://www.google.com/maps/search/?api=1&query=" + encodedAddress));
                
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    Log.d(TAG, "Opening map via Google Maps URL");
                    startActivity(mapIntent);
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error opening map via URL: " + e.getMessage());
            }
            
            // Method 4: Fallback to browser
            Intent browserIntent = new Intent(Intent.ACTION_VIEW);
            browserIntent.setData(Uri.parse("https://www.google.com/maps/search/?api=1&query=" + encodedAddress));
            startActivity(browserIntent);
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening map: " + e.getMessage(), e);
            Toast.makeText(this, "Unable to open map", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showFallbackData() {
        Log.d(TAG, "Showing fallback data for mock listing");
        
        // Create fallback boarding house details
        boardingHouseDetails = new BoardingHouseDetails();
        
        // Basic info with random data
        boardingHouseDetails.setBhId(boardingHouseId);
        boardingHouseDetails.setBhName("Sample Boarding House " + boardingHouseId);
        boardingHouseDetails.setBhAddress("Sample Address, City");
        boardingHouseDetails.setBhDescription("This is a sample boarding house with modern amenities. Perfect for students and young professionals looking for affordable accommodation.");
        boardingHouseDetails.setBhRules("No smoking, No pets, Quiet hours 10PM-6AM");
        boardingHouseDetails.setNumberOfBathroom(2 + (boardingHouseId % 3));
        boardingHouseDetails.setArea(100.0 + (boardingHouseId % 5) * 50);
        boardingHouseDetails.setBuildYear(2018 + (boardingHouseId % 5));
        boardingHouseDetails.setStatus("active");
        boardingHouseDetails.setBhCreatedAt("2024-01-01");
        
        // Sample images
        List<String> sampleImages = new ArrayList<>();
        sampleImages.add("https://via.placeholder.com/400x300?text=Sample+Image+1");
        sampleImages.add("https://via.placeholder.com/400x300?text=Sample+Image+2");
        sampleImages.add("https://via.placeholder.com/400x300?text=Sample+Image+3");
        boardingHouseDetails.setImages(sampleImages);
        
        // Sample room categories
        List<String> sampleCategories = new ArrayList<>();
        sampleCategories.add("Private Room");
        sampleCategories.add("Bed Spacer");
        boardingHouseDetails.setRoomCategories(sampleCategories);
        
        // Sample room details
        List<BoardingHouseDetails.RoomDetail> sampleRoomDetails = new ArrayList<>();
        
        BoardingHouseDetails.RoomDetail privateRoom = new BoardingHouseDetails.RoomDetail();
        privateRoom.setRoomCategory("Private Room");
        privateRoom.setRoomName("Single Private Room");
        privateRoom.setPrice(2500 + (boardingHouseId % 5) * 500);
        privateRoom.setCapacity(1);
        privateRoom.setRoomDescription("Comfortable private room with basic amenities");
        privateRoom.setTotalRooms(3);
        sampleRoomDetails.add(privateRoom);
        
        BoardingHouseDetails.RoomDetail bedSpacer = new BoardingHouseDetails.RoomDetail();
        bedSpacer.setRoomCategory("Bed Spacer");
        bedSpacer.setRoomName("Shared Room");
        bedSpacer.setPrice(1500 + (boardingHouseId % 3) * 300);
        bedSpacer.setCapacity(4);
        bedSpacer.setRoomDescription("Shared room with bunk beds");
        bedSpacer.setTotalRooms(2);
        sampleRoomDetails.add(bedSpacer);
        
        boardingHouseDetails.setRoomDetails(sampleRoomDetails);
        
        // Price range
        boardingHouseDetails.setMinPrice(1500 + (boardingHouseId % 3) * 300);
        boardingHouseDetails.setMaxPrice(2500 + (boardingHouseId % 5) * 500);
        
        // Sample owner info
        BoardingHouseDetails.OwnerInfo sampleOwner = new BoardingHouseDetails.OwnerInfo();
        sampleOwner.setFirstName("John");
        sampleOwner.setMiddleName("M");
        sampleOwner.setLastName("Doe");
        sampleOwner.setPhone("+63 912 345 6789");
        sampleOwner.setEmail("john.doe@example.com");
        sampleOwner.setRole("owner");
        boardingHouseDetails.setOwner(sampleOwner);
        
        // Display the fallback data
        displayBoardingHouseDetails();
        
        Toast.makeText(this, "Showing sample data (real data unavailable)", Toast.LENGTH_LONG).show();
    }
    
    private void showLoadingDialog() {
        if (loadingDialog == null || !loadingDialog.isShowing()) {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
            loadingDialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(false)
                    .create();
            loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            
            // Set dialog window attributes to prevent expansion
            android.view.WindowManager.LayoutParams params = loadingDialog.getWindow().getAttributes();
            params.width = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            loadingDialog.getWindow().setAttributes(params);
            
            loadingDialog.show();
        }
    }
    
    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideLoadingDialog();
    }
}