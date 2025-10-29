package com.example.mock;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class BoardingHouseDetailsActivity extends AppCompatActivity {
    
    private static final String TAG = "BoardingHouseDetails";
    private static final String API_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_boarding_house_details.php";
    
    private ViewPager2 viewPagerImages;
    private LinearLayout layoutIndicators;
    private ImageButton btnBack, btnShare, btnFavorite, btnCall;
    private MaterialButton btnChooseAccommodation;
    
    private TextView tvBoardingHouseName, tvLocation, tvPrice, tvDescription, tvRules, 
                     tvBathrooms, tvArea, tvYear, tvOwnerName, tvOwnerPhone, tvOwnerEmail;
    private RecyclerView rvRoomCategories;
    private ProgressBar progressBar;
    
    private ImageCarouselAdapter imageAdapter;
    private RoomCategoryAdapter roomCategoryAdapter;
    private List<String> imageUrls;
    private List<String> roomCategories;
    
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
        
        // Buttons
        btnBack = findViewById(R.id.btnBack);
        btnShare = findViewById(R.id.btnShare);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnCall = findViewById(R.id.btnCall);
        btnChooseAccommodation = findViewById(R.id.btnChooseAccommodation);
        
        // Text views
        tvBoardingHouseName = findViewById(R.id.tvBoardingHouseName);
        tvLocation = findViewById(R.id.tvLocation);
        tvPrice = findViewById(R.id.tvPrice);
        tvDescription = findViewById(R.id.tvDescription);
        tvRules = findViewById(R.id.tvRules);
        tvBathrooms = findViewById(R.id.tvBathrooms);
        tvArea = findViewById(R.id.tvArea);
        tvYear = findViewById(R.id.tvYear);
        tvOwnerName = findViewById(R.id.tvOwnerName);
        tvOwnerPhone = findViewById(R.id.tvOwnerPhone);
        tvOwnerEmail = findViewById(R.id.tvOwnerEmail);
        
        // Other views
        rvRoomCategories = findViewById(R.id.rvRoomCategories);
        progressBar = findViewById(R.id.progressBar);
        
        // Initialize lists
        imageUrls = new ArrayList<>();
        roomCategories = new ArrayList<>();
        
        // Setup room categories recycler view
        roomCategoryAdapter = new RoomCategoryAdapter(roomCategories);
        rvRoomCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvRoomCategories.setAdapter(roomCategoryAdapter);
    }
    
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnShare.setOnClickListener(v -> shareBoardingHouse());
        
        btnFavorite.setOnClickListener(v -> toggleFavorite());
        
        btnCall.setOnClickListener(v -> contactOwner());
        
        btnChooseAccommodation.setOnClickListener(v -> showAccommodationDialog());
    }
    
    private void loadBoardingHouseDetails() {
        progressBar.setVisibility(View.VISIBLE);
        
        String url = API_URL + "?bh_id=" + boardingHouseId;
        Log.d(TAG, "Loading boarding house details for ID: " + boardingHouseId);
        Log.d(TAG, "API URL: " + url);
        
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressBar.setVisibility(View.GONE);
                        
                        // Debug: Log the first 200 characters of response
                        Log.d(TAG, "Response preview: " + response.substring(0, Math.min(200, response.length())));
                        
                        // Check if response is HTML (ngrok warning page)
                        if (response.trim().startsWith("<!DOCTYPE html>") || (response.contains("ngrok") && response.contains("<html"))) {
                            Log.e(TAG, "Received ngrok warning page instead of JSON");
                            Log.e(TAG, "Full response: " + response);
                            Log.e(TAG, "SOLUTION: Visit https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_boarding_house_details.php in your browser first");
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
                                parseBoardingHouseDetails(data);
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
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Volley error: " + error.getMessage());
                        Toast.makeText(BoardingHouseDetailsActivity.this, "Network error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("ngrok-skip-browser-warning", "any");
                headers.put("User-Agent", "BoardEase-Android-App");
                headers.put("Accept", "application/json");
                return headers;
            }
        };
        
        requestQueue.add(stringRequest);
    }
    
    private void parseBoardingHouseDetails(JSONObject data) throws JSONException {
        boardingHouseDetails = new BoardingHouseDetails();
        
        // Basic info
        boardingHouseDetails.setBhId(data.getInt("bh_id"));
        boardingHouseDetails.setBhName(data.getString("bh_name"));
        boardingHouseDetails.setBhAddress(data.getString("bh_address"));
        boardingHouseDetails.setBhDescription(data.getString("bh_description"));
        boardingHouseDetails.setBhRules(data.getString("bh_rules"));
        boardingHouseDetails.setNumberOfBathroom(data.getInt("number_of_bathroom"));
        boardingHouseDetails.setArea(data.getDouble("area"));
        boardingHouseDetails.setBuildYear(data.getInt("build_year"));
        boardingHouseDetails.setStatus(data.getString("status"));
        boardingHouseDetails.setBhCreatedAt(data.getString("bh_created_at"));
        
        // Images
        JSONArray imagesArray = data.getJSONArray("images");
        List<String> images = new ArrayList<>();
        for (int i = 0; i < imagesArray.length(); i++) {
            images.add(imagesArray.getString(i));
        }
        boardingHouseDetails.setImages(images);
        
        // Room categories
        JSONArray roomCategoriesArray = data.getJSONArray("room_categories");
        List<String> categories = new ArrayList<>();
        for (int i = 0; i < roomCategoriesArray.length(); i++) {
            categories.add(roomCategoriesArray.getString(i));
        }
        boardingHouseDetails.setRoomCategories(categories);
        
        // Room details
        JSONArray roomDetailsArray = data.getJSONArray("room_details");
        List<BoardingHouseDetails.RoomDetail> roomDetails = new ArrayList<>();
        for (int i = 0; i < roomDetailsArray.length(); i++) {
            JSONObject roomJson = roomDetailsArray.getJSONObject(i);
            BoardingHouseDetails.RoomDetail room = new BoardingHouseDetails.RoomDetail();
            room.setRoomCategory(roomJson.getString("room_category"));
            room.setRoomName(roomJson.getString("room_name"));
            room.setPrice(roomJson.getInt("price"));
            room.setCapacity(roomJson.getInt("capacity"));
            room.setRoomDescription(roomJson.getString("room_description"));
            room.setTotalRooms(roomJson.getInt("total_rooms"));
            roomDetails.add(room);
        }
        boardingHouseDetails.setRoomDetails(roomDetails);
        
        // Price range
        if (!data.isNull("min_price")) {
            boardingHouseDetails.setMinPrice(data.getInt("min_price"));
        }
        if (!data.isNull("max_price")) {
            boardingHouseDetails.setMaxPrice(data.getInt("max_price"));
        }
        
        // Owner info
        JSONObject ownerJson = data.getJSONObject("owner");
        BoardingHouseDetails.OwnerInfo owner = new BoardingHouseDetails.OwnerInfo();
        owner.setFirstName(ownerJson.optString("first_name", ""));
        owner.setMiddleName(ownerJson.optString("middle_name", ""));
        owner.setLastName(ownerJson.optString("last_name", ""));
        owner.setPhone(ownerJson.optString("phone", ""));
        owner.setEmail(ownerJson.optString("email", ""));
        owner.setRole(ownerJson.optString("role", ""));
        boardingHouseDetails.setOwner(owner);
    }
    
    private void displayBoardingHouseDetails() {
        if (boardingHouseDetails == null) return;
        
        // Basic info
        tvBoardingHouseName.setText(boardingHouseDetails.getBhName());
        tvLocation.setText(boardingHouseDetails.getBhAddress());
        tvPrice.setText(boardingHouseDetails.getFormattedPriceRange());
        tvDescription.setText(boardingHouseDetails.getBhDescription());
        tvRules.setText(boardingHouseDetails.getBhRules());
        tvBathrooms.setText(String.valueOf(boardingHouseDetails.getNumberOfBathroom()));
        tvArea.setText(String.format("%.1f sqm", boardingHouseDetails.getArea()));
        tvYear.setText(String.valueOf(boardingHouseDetails.getBuildYear()));
        
        // Owner info
        tvOwnerName.setText(boardingHouseDetails.getOwnerFullName());
        tvOwnerPhone.setText(boardingHouseDetails.getOwner().getPhone());
        tvOwnerEmail.setText(boardingHouseDetails.getOwner().getEmail());
        
        // Setup image carousel
        setupImageCarousel();
        
        // Setup room categories
        setupRoomCategories();
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
    }
    
    private void setupIndicators() {
            layoutIndicators.removeAllViews();
            
            for (int i = 0; i < imageUrls.size(); i++) {
                ImageView indicator = new ImageView(this);
            indicator.setImageResource(R.drawable.ic_dot);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            indicator.setLayoutParams(params);
                layoutIndicators.addView(indicator);
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
    
    private void showAccommodationDialog() {
        if (boardingHouseDetails.getRoomDetails().isEmpty()) {
            Toast.makeText(this, "No room details available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] roomNames = new String[boardingHouseDetails.getRoomDetails().size()];
        for (int i = 0; i < boardingHouseDetails.getRoomDetails().size(); i++) {
            BoardingHouseDetails.RoomDetail room = boardingHouseDetails.getRoomDetails().get(i);
            roomNames[i] = room.getRoomName() + " - " + room.getFormattedPrice();
        }
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Choose Accommodation")
                .setItems(roomNames, (dialog, which) -> {
                    BoardingHouseDetails.RoomDetail selectedRoom = boardingHouseDetails.getRoomDetails().get(which);
                    Toast.makeText(this, "Selected: " + selectedRoom.getRoomName(), Toast.LENGTH_SHORT).show();
                    // TODO: Navigate to booking screen
                })
                .setNegativeButton("Cancel", null)
                .show();
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
}