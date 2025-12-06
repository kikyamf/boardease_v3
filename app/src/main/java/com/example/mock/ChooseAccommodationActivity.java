package com.example.mock;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChooseAccommodationActivity extends AppCompatActivity {
    
    private static final String TAG = "ChooseAccommodation";
    // Local development URL - Update this to match your local IP
    private static final String BASE_URL = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/";
    private static final String API_URL = BASE_URL + "get_boarding_house_rooms.php";
    private static final String FALLBACK_API_URL = BASE_URL + "get_boarding_house_details1.php";
    
    private ImageButton btnBack;
    private ProgressBar progressBar;
    private android.widget.LinearLayout layoutAccommodations;
    private TextView tvNoAccommodations;
    
    private int boardingHouseId;
    private RequestQueue requestQueue;
    private int currentUserId;
    private boolean hasActiveBooking = false;
    private String currentBookingStatus = ""; // Store booking status (Pending or Confirmed)
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_accommodation);
        
        // Get current user ID from SharedPreferences
        android.content.SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String userIdString = prefs.getString("user_id", null);
        if (userIdString != null) {
            try {
                currentUserId = Integer.parseInt(userIdString);
            } catch (NumberFormatException e) {
                currentUserId = 0;
            }
        } else {
            currentUserId = 0;
        }
        
        // Get data from intent
        getIntentData();
        
        // Initialize views
        initializeViews();
        
        // Setup click listeners
        setupClickListeners();
        
        // Initialize request queue
        requestQueue = Volley.newRequestQueue(this);
        
        // Check for active booking first, then load accommodations
        if (currentUserId > 0) {
            checkActiveBooking();
        } else {
            // If no user ID, proceed to load accommodations (button will be disabled if no rooms available)
            loadAccommodations();
        }
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
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);
        layoutAccommodations = findViewById(R.id.layoutAccommodations);
        tvNoAccommodations = findViewById(R.id.tvNoAccommodations);
    }
    
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }
    
    private void loadAccommodations() {
        progressBar.setVisibility(View.VISIBLE);
        layoutAccommodations.setVisibility(View.GONE);
        tvNoAccommodations.setVisibility(View.GONE);
        
        String url = API_URL + "?bh_id=" + boardingHouseId;
        Log.d(TAG, "Loading accommodations for bh_id: " + boardingHouseId);
        Log.d(TAG, "API URL: " + url);
        
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressBar.setVisibility(View.GONE);
                        
                        // Check if response is HTML (ngrok warning page)
                        if (response.trim().startsWith("<!DOCTYPE html>") || (response.contains("ngrok") && response.contains("<html"))) {
                            Log.e(TAG, "Received ngrok warning page instead of JSON");
                            Toast.makeText(ChooseAccommodationActivity.this, "Ngrok warning! Visit API URL in browser first.", Toast.LENGTH_LONG).show();
                            showNoAccommodations();
                            return;
                        }
                        
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success");
                            
                            Log.d(TAG, "Primary endpoint response success: " + success);
                            
                            if (success) {
                                JSONObject data = jsonResponse.getJSONObject("data");
                                Log.d(TAG, "Data object keys: " + (data.names() != null ? data.names().toString() : "null"));
                                
                                JSONObject roomsByCategory = data.optJSONObject("rooms_by_category");
                                if (roomsByCategory != null) {
                                    Log.d(TAG, "Found rooms_by_category with " + roomsByCategory.length() + " categories");
                                    // Log first room to see structure
                                    JSONArray categoryNames = roomsByCategory.names();
                                    if (categoryNames != null && categoryNames.length() > 0) {
                                        String firstCategory = categoryNames.getString(0);
                                        JSONArray firstCategoryRooms = roomsByCategory.getJSONArray(firstCategory);
                                        if (firstCategoryRooms.length() > 0) {
                                            Log.d(TAG, "Sample room from API: " + firstCategoryRooms.getJSONObject(0).toString());
                                        }
                                    }
                                    displayAccommodations(roomsByCategory);
                                } else {
                                    Log.e(TAG, "rooms_by_category not found in response. Data keys: " + (data.names() != null ? data.names().toString() : "null"));
                                    // Try fallback if primary endpoint structure is wrong
                                    Log.d(TAG, "Primary endpoint structure mismatch, trying fallback");
                                    loadAccommodationsFromFallback();
                                }
                            } else {
                                String error = jsonResponse.optString("error", "Unknown error occurred");
                                Log.e(TAG, "API Error: " + error);
                                Toast.makeText(ChooseAccommodationActivity.this, "Failed to load accommodations: " + error, Toast.LENGTH_LONG).show();
                                showNoAccommodations();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            Log.e(TAG, "Response that failed to parse: " + response);
                            e.printStackTrace();
                            // Try fallback on parsing error
                            Log.d(TAG, "Primary endpoint parsing failed, trying fallback");
                            loadAccommodationsFromFallback();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Check if 404 error and try fallback endpoint
                        int statusCode = -1;
                        if (error.networkResponse != null) {
                            statusCode = error.networkResponse.statusCode;
                        }
                        
                        Log.d(TAG, "Error response received. Status code: " + statusCode);
                        
                        // If 404, try fallback endpoint
                        if (statusCode == 404) {
                            Log.d(TAG, "Primary endpoint returned 404, trying fallback endpoint");
                            // Don't hide progress bar yet, fallback will handle it
                            loadAccommodationsFromFallback();
                            return;
                        }
                        
                        progressBar.setVisibility(View.GONE);
                        
                        String errorMessage = "Network error occurred";
                        if (error.getMessage() != null) {
                            errorMessage = error.getMessage();
                        } else if (statusCode != -1) {
                            errorMessage = "Server error: " + statusCode;
                        } else if (error.getCause() != null) {
                            errorMessage = error.getCause().getMessage();
                        }
                        
                        Log.e(TAG, "Volley error: " + errorMessage);
                        Log.e(TAG, "Error details: " + error.toString());
                        if (error.networkResponse != null) {
                            Log.e(TAG, "Network response code: " + error.networkResponse.statusCode);
                            if (error.networkResponse.data != null) {
                                Log.e(TAG, "Network response data: " + new String(error.networkResponse.data));
                            }
                        }
                        
                        Toast.makeText(ChooseAccommodationActivity.this, "Network error: " + errorMessage, Toast.LENGTH_LONG).show();
                        showNoAccommodations();
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", "BoardEase-Android-App");
                headers.put("Accept", "application/json");
                return headers;
                //changes
            }
        };
        
        requestQueue.add(stringRequest);
    }
    
    private void displayAccommodations(JSONObject roomsByCategory) throws JSONException {
        layoutAccommodations.removeAllViews();
        
        if (roomsByCategory.length() == 0) {
            showNoAccommodations();
            return;
        }
        
        layoutAccommodations.setVisibility(View.VISIBLE);
        tvNoAccommodations.setVisibility(View.GONE);
        
        // Process each category
        JSONArray categoryNames = roomsByCategory.names();
        if (categoryNames == null) {
            showNoAccommodations();
            return;
        }
        
        // Check if all rooms are occupied (no available rooms)
        boolean allRoomsOccupied = true;
        int totalRooms = 0;
        int totalAvailableRooms = 0;
        
        for (int i = 0; i < categoryNames.length(); i++) {
            String categoryName = categoryNames.getString(i);
            JSONArray roomsInCategory = roomsByCategory.getJSONArray(categoryName);
            
            for (int j = 0; j < roomsInCategory.length(); j++) {
                JSONObject room = roomsInCategory.getJSONObject(j);
                totalRooms++;
                int availableRooms = room.optInt("available_rooms", 0);
                totalAvailableRooms += availableRooms;
                
                if (availableRooms > 0) {
                    allRoomsOccupied = false;
                }
            }
        }
        
        // Show modal if all rooms are occupied (and boarder doesn't have active booking)
        if (allRoomsOccupied && totalRooms > 0 && !hasActiveBooking) {
            showAllRoomsOccupiedModal();
        }
        
        for (int i = 0; i < categoryNames.length(); i++) {
            String categoryName = categoryNames.getString(i);
            JSONArray roomsInCategory = roomsByCategory.getJSONArray(categoryName);
            
            // Create category section
            createCategorySection(categoryName, roomsInCategory);
        }
    }
    
    private void createCategorySection(String categoryName, JSONArray rooms) throws JSONException {
        // Category header
        TextView categoryHeader = new TextView(this);
        categoryHeader.setText(categoryName);
        categoryHeader.setTextSize(20);
        try {
            Typeface semiboldTypeface = Typeface.createFromAsset(getAssets(), "fonts/poppins_semibold.ttf");
            categoryHeader.setTypeface(semiboldTypeface);
        } catch (Exception e) {
            categoryHeader.setTypeface(null, Typeface.BOLD);
        }
        categoryHeader.setTextColor(getResources().getColor(R.color.brown));
        categoryHeader.setPadding(24, 24, 24, 12);
        
        android.widget.LinearLayout.LayoutParams headerParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        headerParams.setMargins(0, 0, 0, 8);
        categoryHeader.setLayoutParams(headerParams);
        
        layoutAccommodations.addView(categoryHeader);
        
        // Create a card for each room in this category
        for (int i = 0; i < rooms.length(); i++) {
            JSONObject room = rooms.getJSONObject(i);
            createRoomCard(room);
        }
    }
    
    private void createRoomCard(JSONObject room) throws JSONException {
        // Log the room object to see what fields are available
        Log.d(TAG, "Creating card for room: " + room.toString());
        
        // Create MaterialCardView
        com.google.android.material.card.MaterialCardView cardView = new com.google.android.material.card.MaterialCardView(this);
        cardView.setCardElevation(6);
        cardView.setRadius(16);
        cardView.setCardBackgroundColor(getResources().getColor(android.R.color.white));
        cardView.setStrokeWidth(1);
        cardView.setStrokeColor(getResources().getColor(R.color.brown));
        
        android.widget.LinearLayout.LayoutParams cardParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(16, 0, 16, 16);
        cardView.setLayoutParams(cardParams);
        
        // Create inner LinearLayout
        android.widget.LinearLayout cardContent = new android.widget.LinearLayout(this);
        cardContent.setOrientation(android.widget.LinearLayout.VERTICAL);
        cardContent.setPadding(20, 20, 20, 20);
        
        // Room Name
        TextView tvRoomName = new TextView(this);
        tvRoomName.setText(room.getString("room_name"));
        tvRoomName.setTextSize(18);
        try {
            Typeface semiboldTypeface = Typeface.createFromAsset(getAssets(), "fonts/poppins_semibold.ttf");
            tvRoomName.setTypeface(semiboldTypeface);
        } catch (Exception e) {
            tvRoomName.setTypeface(null, Typeface.BOLD);
        }
        tvRoomName.setTextColor(getResources().getColor(android.R.color.black));
        
        android.widget.LinearLayout.LayoutParams nameParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        nameParams.setMargins(0, 0, 0, 8);
        tvRoomName.setLayoutParams(nameParams);
        
        // Room Description
        TextView tvDescription = new TextView(this);
        String description = room.optString("room_description", "No description available");
        if (description.isEmpty() || description.equals("0")) {
            description = "No description available";
        }
        tvDescription.setText(description);
        tvDescription.setTextSize(14);
        tvDescription.setTextColor(getResources().getColor(R.color.dark_gray));
        tvDescription.setLineSpacing(4, 1.2f);
        
        android.widget.LinearLayout.LayoutParams descParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descParams.setMargins(0, 0, 0, 12);
        tvDescription.setLayoutParams(descParams);
        
        // Price and Capacity Row
        android.widget.LinearLayout infoRow = new android.widget.LinearLayout(this);
        infoRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        
        // Price
        TextView tvPrice = new TextView(this);
        double price = room.getDouble("price");
        tvPrice.setText("₱" + String.format("%,.0f", price) + "/month");
        tvPrice.setTextSize(16);
        try {
            Typeface boldTypeface = Typeface.createFromAsset(getAssets(), "fonts/poppins_bold.ttf");
            tvPrice.setTypeface(boldTypeface);
        } catch (Exception e) {
            tvPrice.setTypeface(null, Typeface.BOLD);
        }
        tvPrice.setTextColor(getResources().getColor(R.color.brown));
        
        android.widget.LinearLayout.LayoutParams priceParams = new android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        );
        tvPrice.setLayoutParams(priceParams);
        
        // Capacity
        TextView tvCapacity = new TextView(this);
        int capacity = room.getInt("capacity");
        tvCapacity.setText("Capacity: " + capacity + " person(s)");
        tvCapacity.setTextSize(14);
        tvCapacity.setTextColor(getResources().getColor(R.color.dark_gray));
        
        android.widget.LinearLayout.LayoutParams capacityParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        tvCapacity.setLayoutParams(capacityParams);
        
        infoRow.addView(tvPrice);
        infoRow.addView(tvCapacity);
        
        android.widget.LinearLayout.LayoutParams infoRowParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        infoRowParams.setMargins(0, 0, 0, 12);
        infoRow.setLayoutParams(infoRowParams);
        
        // Availability (actual available rooms)
        TextView tvAvailability = new TextView(this);
        int availableRooms = -1;
        String roomCategory = room.optString("room_category", "");
        
        // Check if available_rooms exists in the response
        if (room.has("available_rooms") && !room.isNull("available_rooms")) {
            availableRooms = room.optInt("available_rooms", 0);
            Log.d(TAG, "Room " + room.optString("room_name") + " - available_rooms from API: " + availableRooms);
        } else {
            // If available_rooms is not in the response, fallback to total_rooms
            availableRooms = room.optInt("total_rooms", 0);
            Log.d(TAG, "Room " + room.optString("room_name") + " - available_rooms not in response, using total_rooms: " + availableRooms);
        }
        
        // For Bed Spacer, show only room units count (no capacity ratio)
        if ("Bed Spacer".equals(roomCategory)) {
            // Show room units count
            if (availableRooms == 0) {
                tvAvailability.setText("No available rooms as of the moment");
                tvAvailability.setTextColor(getResources().getColor(R.color.red));
            } else {
                tvAvailability.setText("Room(s): " + availableRooms);
                tvAvailability.setTextColor(getResources().getColor(R.color.green));
            }
        } else {
            // For Private Room, show available rooms count
            if (availableRooms == 0) {
                tvAvailability.setText("No available rooms as of the moment");
                tvAvailability.setTextColor(getResources().getColor(R.color.red));
            } else {
                tvAvailability.setText("Room(s): " + availableRooms);
                tvAvailability.setTextColor(getResources().getColor(R.color.green));
            }
        }
        tvAvailability.setTextSize(14);
        
        android.widget.LinearLayout.LayoutParams availParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        availParams.setMargins(0, 0, 0, 16);
        tvAvailability.setLayoutParams(availParams);
        
        // Select Button
        MaterialButton btnSelect = new MaterialButton(this);
        btnSelect.setText("Select");
        btnSelect.setTextSize(14);
        btnSelect.setBackgroundColor(getResources().getColor(R.color.brown));
        btnSelect.setTextColor(getResources().getColor(android.R.color.white));
        btnSelect.setCornerRadius(8);
        
        android.widget.LinearLayout.LayoutParams btnParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnSelect.setLayoutParams(btnParams);
        
        // Check if button should be disabled
        boolean shouldDisable = false;
        String disableReason = "";
        
        // Disable if boarder has active booking
        if (hasActiveBooking) {
            shouldDisable = true;
            disableReason = "You already have an active booking";
        }
        
        // For Bed Spacer, disable only if capacity is full (occupied >= total)
        // For Private Room, disable if no available rooms
        if ("Bed Spacer".equals(roomCategory)) {
            int occupiedCapacity = room.optInt("occupied_capacity", 0);
            int totalCapacity = room.optInt("total_capacity", 0);
            
            if (totalCapacity > 0 && occupiedCapacity >= totalCapacity) {
                // Bed Spacer is at full capacity
                shouldDisable = true;
                disableReason = "Room is at full capacity";
            }
            // If capacity not full, button should be enabled (even if available_rooms == 0)
        } else {
            // For Private Room, disable if no available rooms
            if (availableRooms == 0) {
                shouldDisable = true;
                disableReason = "No available rooms";
            }
        }
        
        // Make variables final for lambda expression
        final boolean finalHasActiveBooking = hasActiveBooking;
        final int finalAvailableRooms = availableRooms;
        
        // Set button state
        if (shouldDisable) {
            btnSelect.setEnabled(false);
            btnSelect.setAlpha(0.5f); // Make it look disabled
            btnSelect.setText("Select"); // Keep text as "Select" only
            // Set click listener that shows modal
            btnSelect.setOnClickListener(v -> {
                if (finalHasActiveBooking) {
                    // Show appropriate modal based on booking status
                    if ("Pending".equals(currentBookingStatus)) {
                        showPendingBookingModal();
                    } else {
                        showActiveBookingModal();
                    }
                } else if (finalAvailableRooms == 0) {
                    showNoAvailableRoomsModal();
                }
            });
        } else {
            btnSelect.setEnabled(true);
            btnSelect.setAlpha(1.0f);
            // Set click listener for Select button
            int bhrId = room.getInt("bhr_id");
            btnSelect.setOnClickListener(v -> {
                try {
                    // Create a copy of the room object and add bh_id if not present
                    JSONObject roomData = new JSONObject(room.toString());
                    if (!roomData.has("bh_id") && boardingHouseId > 0) {
                        roomData.put("bh_id", boardingHouseId);
                        Log.d(TAG, "Added bh_id to room_data: " + boardingHouseId);
                    }
                    
                    Intent intent = new Intent(this, BookingActivity.class);
                    intent.putExtra("bhr_id", bhrId);
                    intent.putExtra("room_data", roomData.toString());
                    intent.putExtra("bh_id", boardingHouseId); // Also pass as separate extra for safety
                    Log.d(TAG, "Starting BookingActivity with bh_id: " + boardingHouseId + ", bhr_id: " + bhrId);
                    startActivity(intent);
                } catch (JSONException e) {
                    Log.e(TAG, "Error creating room_data: " + e.getMessage());
                    Toast.makeText(this, "Error loading room data", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // Add views to card content
        cardContent.addView(tvRoomName);
        cardContent.addView(tvDescription);
        cardContent.addView(infoRow);
        cardContent.addView(tvAvailability);
        cardContent.addView(btnSelect);
        
        // Add card content to card view
        cardView.addView(cardContent);
        
        // Add card to layout
        layoutAccommodations.addView(cardView);
    }
    
    private void loadAccommodationsFromFallback() {
        progressBar.setVisibility(View.VISIBLE);
        layoutAccommodations.setVisibility(View.GONE);
        tvNoAccommodations.setVisibility(View.GONE);
        
        String url = FALLBACK_API_URL + "?bh_id=" + boardingHouseId;
        Log.d(TAG, "Using fallback endpoint: " + url);
        
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressBar.setVisibility(View.GONE);
                        
                        // Check if response is HTML (ngrok warning page)
                        if (response.trim().startsWith("<!DOCTYPE html>") || (response.contains("ngrok") && response.contains("<html"))) {
                            Log.e(TAG, "Received ngrok warning page instead of JSON");
                            Toast.makeText(ChooseAccommodationActivity.this, "Ngrok warning! Visit API URL in browser first.", Toast.LENGTH_LONG).show();
                            showNoAccommodations();
                            return;
                        }
                        
                        try {
                            // Log the full response first
                            Log.d(TAG, "Fallback response (first 500 chars): " + (response.length() > 500 ? response.substring(0, 500) + "..." : response));
                            
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success");
                            
                            Log.d(TAG, "Fallback response success: " + success);
                            
                            if (success) {
                                JSONObject data = jsonResponse.getJSONObject("data");
                                Log.d(TAG, "Data object: " + data.toString());
                                Log.d(TAG, "Data object keys: " + java.util.Arrays.toString(data.names().toString().split(",")));
                                
                                // Try to get rooms from different possible locations
                                JSONArray roomDetailsArray = null;
                                
                                // First, try data.rooms (actual structure from API)
                                roomDetailsArray = data.optJSONArray("rooms");
                                if (roomDetailsArray != null) {
                                    Log.d(TAG, "Found rooms in data.rooms");
                                }
                                
                                // If not found, try data.boarding_house.room_details (alternative structure)
                                if (roomDetailsArray == null) {
                                    JSONObject boardingHouse = data.optJSONObject("boarding_house");
                                    if (boardingHouse != null) {
                                        Log.d(TAG, "Found boarding_house object");
                                        roomDetailsArray = boardingHouse.optJSONArray("room_details");
                                        if (roomDetailsArray != null) {
                                            Log.d(TAG, "Found room_details in data.boarding_house.room_details");
                                        }
                                    }
                                }
                                
                                // If not found, try data.room_details (another alternative structure)
                                if (roomDetailsArray == null) {
                                    roomDetailsArray = data.optJSONArray("room_details");
                                    if (roomDetailsArray != null) {
                                        Log.d(TAG, "Found room_details in data.room_details");
                                    }
                                }
                                
                                // If still not found, the response doesn't have room_details, show no accommodations
                                if (roomDetailsArray == null || roomDetailsArray.length() == 0) {
                                    Log.e(TAG, "room_details not found in response. This boarding house may not have rooms configured.");
                                    Log.e(TAG, "Full data structure: " + data.toString());
                                    showNoAccommodations();
                                    return;
                                }
                                
                                Log.d(TAG, "room_details array: " + (roomDetailsArray != null ? roomDetailsArray.length() + " items" : "null"));
                                
                                if (roomDetailsArray != null && roomDetailsArray.length() > 0) {
                                    Log.d(TAG, "Processing " + roomDetailsArray.length() + " room details");
                                    // Log first room to see structure from fallback
                                    if (roomDetailsArray.length() > 0) {
                                        Log.d(TAG, "Sample room from fallback API: " + roomDetailsArray.getJSONObject(0).toString());
                                    }
                                    // Group rooms by category
                                    JSONObject roomsByCategory = new JSONObject();
                                    for (int i = 0; i < roomDetailsArray.length(); i++) {
                                        JSONObject room = roomDetailsArray.getJSONObject(i);
                                        String category = room.getString("room_category");
                                        Log.d(TAG, "Room " + i + ": " + room.optString("room_name") + " - Category: " + category);
                                        
                                        if (!roomsByCategory.has(category)) {
                                            roomsByCategory.put(category, new JSONArray());
                                        }
                                        roomsByCategory.getJSONArray(category).put(room);
                                    }
                                    
                                    Log.d(TAG, "Grouped rooms into " + roomsByCategory.length() + " categories");
                                    displayAccommodations(roomsByCategory);
                                } else {
                                    Log.e(TAG, "No room details found. Data structure: " + data.toString());
                                    showNoAccommodations();
                                }
                            } else {
                                String error = jsonResponse.optString("error", "Unknown error occurred");
                                Log.e(TAG, "Fallback API Error: " + error);
                                Toast.makeText(ChooseAccommodationActivity.this, "Failed to load accommodations: " + error, Toast.LENGTH_LONG).show();
                                showNoAccommodations();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            Log.e(TAG, "Response that failed to parse: " + response);
                            e.printStackTrace();
                            showNoAccommodations();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressBar.setVisibility(View.GONE);
                        String errorMessage = "Network error occurred";
                        if (error.getMessage() != null) {
                            errorMessage = error.getMessage();
                        } else if (error.networkResponse != null) {
                            errorMessage = "Server error: " + error.networkResponse.statusCode;
                        }
                        Log.e(TAG, "Fallback endpoint error: " + errorMessage);
                        Toast.makeText(ChooseAccommodationActivity.this, "Failed to load accommodations", Toast.LENGTH_LONG).show();
                        showNoAccommodations();
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", "BoardEase-Android-App");
                headers.put("Accept", "application/json");
                return headers;
            }
        };
        
        requestQueue.add(stringRequest);
    }
    
    private void showNoAccommodations() {
        layoutAccommodations.setVisibility(View.GONE);
        tvNoAccommodations.setVisibility(View.VISIBLE);
    }
    
    private void checkActiveBooking() {
        if (currentUserId <= 0) {
            // No user ID, proceed to load accommodations
            loadAccommodations();
            return;
        }
        
        String url = BASE_URL + "check_active_booking.php?user_id=" + currentUserId;
        Log.d(TAG, "Checking active booking for user_id: " + currentUserId);
        
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success");
                            
                            if (success) {
                                hasActiveBooking = jsonResponse.getBoolean("has_active_booking");
                                currentBookingStatus = jsonResponse.optString("booking_status", "");
                                Log.d(TAG, "Active booking check: " + hasActiveBooking + ", status: " + currentBookingStatus);
                                
                                if (hasActiveBooking) {
                                    // Show different modal based on booking status
                                    if ("Pending".equals(currentBookingStatus)) {
                                        // Booking is pending approval - show pending booking modal
                                        showPendingBookingModal();
                                    } else {
                                        // Booking is confirmed - show active booking modal
                                        showActiveBookingModal();
                                    }
                                }
                            } else {
                                Log.e(TAG, "Error checking active booking: " + jsonResponse.optString("error", "Unknown error"));
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error in checkActiveBooking: " + e.getMessage());
                        }
                        
                        // Load accommodations after checking active booking
                        loadAccommodations();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error checking active booking: " + error.getMessage());
                        // Proceed to load accommodations even if check fails
                        loadAccommodations();
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("User-Agent", "BoardEase-Android-App");
                headers.put("Accept", "application/json");
                return headers;
            }
        };
        
        requestQueue.add(stringRequest);
    }
    
    /**
     * Show modal dialog when boarder has an active booking
     */
    private void showActiveBookingModal() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        
        // Create custom view for the dialog
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 32);
        layout.setBackgroundColor(getResources().getColor(android.R.color.black));
        
        // Title
        TextView titleView = new TextView(this);
        titleView.setText("Active Booking Found");
        titleView.setTextSize(20);
        try {
            Typeface boldTypeface = Typeface.createFromAsset(getAssets(), "fonts/poppins_bold.ttf");
            titleView.setTypeface(boldTypeface);
        } catch (Exception e) {
            titleView.setTypeface(null, Typeface.BOLD);
        }
        titleView.setTextColor(getResources().getColor(android.R.color.white));
        titleView.setGravity(android.view.Gravity.CENTER);
        
        android.widget.LinearLayout.LayoutParams titleParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, 0, 0, 24);
        titleView.setLayoutParams(titleParams);
        
        // Message
        TextView messageView = new TextView(this);
        messageView.setText("You already have an active booking. Please complete your current stay before booking another room.\n\nYou can view your current booking in the Bookings section.");
        messageView.setTextSize(16);
        messageView.setTextColor(getResources().getColor(android.R.color.white));
        messageView.setLineSpacing(8, 1.2f);
        messageView.setGravity(android.view.Gravity.CENTER);
        
        android.widget.LinearLayout.LayoutParams messageParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        messageParams.setMargins(0, 0, 0, 32);
        messageView.setLayoutParams(messageParams);
        
        layout.addView(titleView);
        layout.addView(messageView);
        
        builder.setView(layout);
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
        });
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        
        // Button uses default color (no custom styling)
    }
    
    /**
     * Show modal dialog when boarder has a pending booking (waiting for approval)
     */
    private void showPendingBookingModal() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        
        // Create custom view for the dialog
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 32);
        layout.setBackgroundColor(getResources().getColor(android.R.color.black));
        
        // Title
        TextView titleView = new TextView(this);
        titleView.setText("Pending Booking");
        titleView.setTextSize(20);
        try {
            Typeface boldTypeface = Typeface.createFromAsset(getAssets(), "fonts/poppins_bold.ttf");
            titleView.setTypeface(boldTypeface);
        } catch (Exception e) {
            titleView.setTypeface(null, Typeface.BOLD);
        }
        titleView.setTextColor(getResources().getColor(android.R.color.white));
        titleView.setGravity(android.view.Gravity.CENTER);
        
        android.widget.LinearLayout.LayoutParams titleParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, 0, 0, 24);
        titleView.setLayoutParams(titleParams);
        
        // Message
        TextView messageView = new TextView(this);
        messageView.setText("You have a pending booking that is waiting for owner approval. Please wait for your booking to be approved before booking another room.\n\nYou can check the status of your booking in the Bookings section.");
        messageView.setTextSize(16);
        messageView.setTextColor(getResources().getColor(android.R.color.white));
        messageView.setLineSpacing(8, 1.2f);
        messageView.setGravity(android.view.Gravity.CENTER);
        
        android.widget.LinearLayout.LayoutParams messageParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        messageParams.setMargins(0, 0, 0, 32);
        messageView.setLayoutParams(messageParams);
        
        layout.addView(titleView);
        layout.addView(messageView);
        
        builder.setView(layout);
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
        });
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        
        // Button uses default color (no custom styling)
    }
    
    /**
     * Show modal dialog when room has no available units
     */
    private void showNoAvailableRoomsModal() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        
        // Create custom view for the dialog
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 32);
        layout.setBackgroundColor(getResources().getColor(android.R.color.black));
        
        // Title
        TextView titleView = new TextView(this);
        titleView.setText("No Available Rooms");
        titleView.setTextSize(20);
        try {
            Typeface boldTypeface = Typeface.createFromAsset(getAssets(), "fonts/poppins_bold.ttf");
            titleView.setTypeface(boldTypeface);
        } catch (Exception e) {
            titleView.setTypeface(null, Typeface.BOLD);
        }
        titleView.setTextColor(getResources().getColor(android.R.color.white));
        titleView.setGravity(android.view.Gravity.CENTER);
        
        android.widget.LinearLayout.LayoutParams titleParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, 0, 0, 24);
        titleView.setLayoutParams(titleParams);
        
        // Message
        TextView messageView = new TextView(this);
        messageView.setText("Sorry, this room is currently fully occupied. All room units are booked.\n\nPlease check back later or choose a different room type.");
        messageView.setTextSize(16);
        messageView.setTextColor(getResources().getColor(android.R.color.white));
        messageView.setLineSpacing(8, 1.2f);
        messageView.setGravity(android.view.Gravity.CENTER);
        
        android.widget.LinearLayout.LayoutParams messageParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        messageParams.setMargins(0, 0, 0, 32);
        messageView.setLayoutParams(messageParams);
        
        layout.addView(titleView);
        layout.addView(messageView);
        
        builder.setView(layout);
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
        });
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        
        // Button uses default color (no custom styling)
    }
    
    /**
     * Show modal dialog when all rooms are occupied
     */
    private void showAllRoomsOccupiedModal() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        
        // Create custom view for the dialog
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 32);
        layout.setBackgroundColor(getResources().getColor(android.R.color.black));
        
        // Title
        TextView titleView = new TextView(this);
        titleView.setText("All Rooms Occupied");
        titleView.setTextSize(20);
        try {
            Typeface boldTypeface = Typeface.createFromAsset(getAssets(), "fonts/poppins_bold.ttf");
            titleView.setTypeface(boldTypeface);
        } catch (Exception e) {
            titleView.setTypeface(null, Typeface.BOLD);
        }
        titleView.setTextColor(getResources().getColor(android.R.color.white));
        titleView.setGravity(android.view.Gravity.CENTER);
        
        android.widget.LinearLayout.LayoutParams titleParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, 0, 0, 24);
        titleView.setLayoutParams(titleParams);
        
        // Message
        TextView messageView = new TextView(this);
        messageView.setText("Sorry, all rooms in this boarding house are currently fully occupied. All available room units have been booked.\n\nPlease check back later or search for other boarding houses.");
        messageView.setTextSize(16);
        messageView.setTextColor(getResources().getColor(android.R.color.white));
        messageView.setLineSpacing(8, 1.2f);
        messageView.setGravity(android.view.Gravity.CENTER);
        
        android.widget.LinearLayout.LayoutParams messageParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        messageParams.setMargins(0, 0, 0, 32);
        messageView.setLayoutParams(messageParams);
        
        layout.addView(titleView);
        layout.addView(messageView);
        
        builder.setView(layout);
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
        });
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        
        // Button uses default color (no custom styling)
    }
}

