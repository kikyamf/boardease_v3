package com.example.mock;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.example.mock.adapters.RoomAdapter;
import com.google.android.material.button.MaterialButton;
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
    
    private TextView tvBoardingHouseName, tvLocation, tvPrice, tvDescription, tvRules, tvBathrooms, tvArea, tvYear;
    private LinearLayout layoutAccommodations;
    private RecyclerView rvRooms;
    
    private ImageCarouselAdapter imageAdapter;
    private RoomAdapter roomAdapter;
    private List<String> imageUrls;
    private List<RoomData> rooms;
    
    private int boardingHouseId;
    private String boardingHouseName;
    private String boardingHouseImage;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boarding_house_details);
        
        // Get data from intent
        getIntentData();
        
        // Initialize views
        initializeViews();
        
        // Setup image carousel
        setupImageCarousel();
        
        // Setup click listeners
        setupClickListeners();
        
        // Load boarding house details
        loadBoardingHouseDetails();
    }
    
    private void getIntentData() {
        Intent intent = getIntent();
        boardingHouseId = intent.getIntExtra("bh_id", 0);
        boardingHouseName = intent.getStringExtra("boarding_house_name");
        boardingHouseImage = intent.getStringExtra("boarding_house_image");
    }
    
    private void initializeViews() {
        viewPagerImages = findViewById(R.id.viewPagerImages);
        layoutIndicators = findViewById(R.id.layoutIndicators);
        btnBack = findViewById(R.id.btnBack);
        btnShare = findViewById(R.id.btnShare);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnCall = findViewById(R.id.btnCall);
        btnChooseAccommodation = findViewById(R.id.btnChooseAccommodation);
        
        tvBoardingHouseName = findViewById(R.id.tvBoardingHouseName);
        tvLocation = findViewById(R.id.tvLocation);
        tvPrice = findViewById(R.id.tvPrice);
        tvDescription = findViewById(R.id.tvDescription);
        tvRules = findViewById(R.id.tvRules);
        tvBathrooms = findViewById(R.id.tvBathrooms);
        tvArea = findViewById(R.id.tvArea);
        tvYear = findViewById(R.id.tvYear);
        layoutAccommodations = findViewById(R.id.layoutAccommodations);
        rvRooms = findViewById(R.id.rvRooms);
        
        // Initialize lists
        imageUrls = new ArrayList<>();
        rooms = new ArrayList<>();
        
        // Setup rooms RecyclerView
        roomAdapter = new RoomAdapter(this, rooms);
        rvRooms.setLayoutManager(new LinearLayoutManager(this));
        rvRooms.setAdapter(roomAdapter);
    }
    
    private void setupImageCarousel() {
        try {
            imageAdapter = new ImageCarouselAdapter(imageUrls);
            viewPagerImages.setAdapter(imageAdapter);
            
            // Setup page change listener for indicators
            viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    updateIndicators(position);
                }
            });
            
            // Create indicators
            createIndicators();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void createIndicators() {
        try {
            layoutIndicators.removeAllViews();
            
            for (int i = 0; i < imageUrls.size(); i++) {
                ImageView indicator = new ImageView(this);
                indicator.setImageResource(R.drawable.dot_inactive);
                indicator.setPadding(8, 0, 8, 0);
                layoutIndicators.addView(indicator);
            }
            
            // Set first indicator as active
            if (imageUrls.size() > 0) {
                updateIndicators(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void updateIndicators(int position) {
        try {
            for (int i = 0; i < layoutIndicators.getChildCount(); i++) {
                ImageView indicator = (ImageView) layoutIndicators.getChildAt(i);
                if (i == position) {
                    indicator.setImageResource(R.drawable.dot_active);
                } else {
                    indicator.setImageResource(R.drawable.dot_inactive);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnShare.setOnClickListener(v -> {
            // TODO: Implement share functionality
            // This could involve:
            // 1. Creating a share intent with boarding house details
            // 2. Generating a shareable link
            // 3. Opening share dialog
        });
        
        btnFavorite.setOnClickListener(v -> {
            // TODO: Implement favorite functionality
            // This could involve:
            // 1. Toggling favorite state
            // 2. Updating UI to show favorite state
            // 3. Saving to local database or sending to server
        });
        
        btnCall.setOnClickListener(v -> {
            // TODO: Implement call functionality
            // This could involve:
            // 1. Opening phone dialer with owner's number
            // 2. Making a direct call
        });
        
        btnChooseAccommodation.setOnClickListener(v -> {
            // Navigate to Pre-Booking Phase 1
            Intent intent = new Intent(this, PreBookingPhase1Activity.class);
            intent.putExtra("boarding_house_id", boardingHouseId);
            intent.putExtra("boarding_house_name", boardingHouseName);
            intent.putExtra("boarding_house_image", boardingHouseImage);
            startActivity(intent);
        });
    }
    
    private void loadBoardingHouseDetails() {
        if (boardingHouseId <= 0) {
            Toast.makeText(this, "Invalid boarding house ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Show loading state
        tvBoardingHouseName.setText("Loading...");
        
        // Create request queue
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        
        // Create string request
        StringRequest stringRequest = new StringRequest(Request.Method.GET, API_URL + "?bh_id=" + boardingHouseId,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Log.d(TAG, "API Response: " + response);
                            
                            JSONObject jsonResponse = new JSONObject(response);
                            boolean success = jsonResponse.getBoolean("success");
                            
                            if (success) {
                                JSONObject data = jsonResponse.getJSONObject("data");
                                updateUIWithData(data);
                            } else {
                                String error = jsonResponse.optString("error", "Unknown error occurred");
                                Log.e(TAG, "API Error: " + error);
                                Toast.makeText(BoardingHouseDetailsActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            Toast.makeText(BoardingHouseDetailsActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Volley error: " + error.getMessage());
                        Toast.makeText(BoardingHouseDetailsActivity.this, "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("ngrok-skip-browser-warning", "true");
                return headers;
            }
        };
        
        // Add request to queue
        requestQueue.add(stringRequest);
    }
    
    private void updateUIWithData(JSONObject data) throws JSONException {
        // Set basic information
        tvBoardingHouseName.setText(data.getString("bh_name"));
        tvLocation.setText(data.getString("bh_address"));
        tvDescription.setText(data.getString("bh_description"));
        
        // Set rules
        if (data.has("bh_rules") && !data.isNull("bh_rules")) {
            tvRules.setText(data.getString("bh_rules"));
        } else {
            tvRules.setText("No specific rules");
        }
        
        // Set bathrooms
        if (data.has("number_of_bathroom") && !data.isNull("number_of_bathroom")) {
            tvBathrooms.setText(data.getString("number_of_bathroom") + " bathrooms");
        } else {
            tvBathrooms.setText("Bathroom info not available");
        }
        
        // Set area
        if (data.has("area") && !data.isNull("area")) {
            tvArea.setText(data.getString("area") + " sqm");
        } else {
            tvArea.setText("Area info not available");
        }
        
        // Set build year
        if (data.has("build_year") && !data.isNull("build_year")) {
            tvYear.setText("Built in " + data.getString("build_year"));
        } else {
            tvYear.setText("Year built not available");
        }
        
        // Set price range
        if (data.has("lowest_price") && data.has("highest_price") && 
            !data.isNull("lowest_price") && !data.isNull("highest_price")) {
            int lowestPrice = data.getInt("lowest_price");
            int highestPrice = data.getInt("highest_price");
            if (lowestPrice == highestPrice) {
                tvPrice.setText("₱" + String.format("%,d", lowestPrice) + "/month");
            } else {
                tvPrice.setText("₱" + String.format("%,d", lowestPrice) + " - ₱" + String.format("%,d", highestPrice) + "/month");
            }
        } else {
            tvPrice.setText("Contact for pricing");
        }
        
        // Load images
        if (data.has("images") && !data.isNull("images")) {
            JSONArray imagesArray = data.getJSONArray("images");
            imageUrls.clear();
            for (int i = 0; i < imagesArray.length(); i++) {
                imageUrls.add(imagesArray.getString(i));
            }
        }
        
        // If no images, add a placeholder
        if (imageUrls.isEmpty()) {
            imageUrls.add("sample_listing");
        }
        
        // Update image carousel
        setupImageCarousel();
        
        // Load rooms
        if (data.has("rooms") && !data.isNull("rooms")) {
            JSONArray roomsArray = data.getJSONArray("rooms");
            rooms.clear();
            for (int i = 0; i < roomsArray.length(); i++) {
                JSONObject roomJson = roomsArray.getJSONObject(i);
                RoomData room = new RoomData(
                    roomJson.getString("room_name"),
                    roomJson.getInt("price"),
                    roomJson.getInt("capacity"),
                    roomJson.getString("room_category"),
                    roomJson.getString("room_description"),
                    roomJson.getInt("total_rooms")
                );
                rooms.add(room);
            }
        }
        
        // Update room adapter
        roomAdapter.notifyDataSetChanged();
    }
}
