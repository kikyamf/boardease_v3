package com.example.mock;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.mock.adapters.BoardingHouseAdapter;
import com.example.mock.adapters.BoardingHouseAdapter.OnFavoriteClickListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ExploreFragment extends Fragment implements OnFavoriteClickListener {
    
    private static final String TAG = "ExploreFragment";
    private static final String API_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_boarding_houses.php";
    
    private EditText etSearch;
    private RecyclerView rvBoardingHouses;
    private ProgressBar progressBar;
    private LinearLayout layoutEmptyState;
    private TextView tvResultsCount;
    
    private BoardingHouseAdapter adapter;
    private List<Listing> allBoardingHouses;
    private List<Listing> filteredBoardingHouses;
    private int userId;
    
    // Factory method to create new instance with user ID
    public static ExploreFragment newInstance(int userId) {
        ExploreFragment fragment = new ExploreFragment();
        Bundle args = new Bundle();
        args.putInt("user_id", userId);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_explore, container, false);
        
        // Get user ID from arguments
        if (getArguments() != null) {
            userId = getArguments().getInt("user_id", 1);
        }
        
        try {
            initializeViews(view);
            setupRecyclerView();
            setupSearchFunctionality();
            loadBoardingHouses();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return view;
    }
    
    private void initializeViews(View view) {
        etSearch = view.findViewById(R.id.etSearch);
        rvBoardingHouses = view.findViewById(R.id.rvBoardingHouses);
        progressBar = view.findViewById(R.id.progressBar);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        tvResultsCount = view.findViewById(R.id.tvResultsCount);
    }
    
    private void setupRecyclerView() {
        allBoardingHouses = new ArrayList<>();
        filteredBoardingHouses = new ArrayList<>();
        
        adapter = new BoardingHouseAdapter(getContext(), filteredBoardingHouses, this, null, false);
        rvBoardingHouses.setLayoutManager(new LinearLayoutManager(getContext()));
        rvBoardingHouses.setAdapter(adapter);
    }
    
    private void setupSearchFunctionality() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBoardingHouses(s.toString());
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void loadBoardingHouses() {
        // Show loading indicator
        progressBar.setVisibility(View.VISIBLE);
        rvBoardingHouses.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);
        
        // Create request queue
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        
        // Create string request with custom headers
        StringRequest stringRequest = new StringRequest(Request.Method.GET, API_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Log.d(TAG, "API Response: " + response);
                            
                            // Check if response is null or empty
                            if (response == null || response.trim().isEmpty()) {
                                Log.e(TAG, "Received null or empty response");
                                showError("Server returned empty response");
                                return;
                            }
                            
                            // Check if response is HTML (ngrok warning page)
                            if (response.trim().startsWith("<!DOCTYPE html>") || response.contains("ngrok")) {
                                Log.e(TAG, "Received ngrok warning page instead of JSON");
                                showError("Please visit the API URL in your browser first to bypass ngrok warning, then try again.");
                                return;
                            }
                            
                            // Try to parse as wrapped JSON object first
                            try {
                                JSONObject jsonResponse = new JSONObject(response);
                                boolean success = jsonResponse.getBoolean("success");
                                
                            if (success) {
                                JSONArray dataArray = jsonResponse.getJSONArray("data");
                                Log.d(TAG, "Successfully parsed wrapped JSON with " + dataArray.length() + " items");
                                parseBoardingHousesData(dataArray);
                            } else {
                                    String error = jsonResponse.optString("error", "Unknown error occurred");
                                    Log.e(TAG, "API Error: " + error);
                                    showError("Failed to load boarding houses: " + error);
                                }
                            } catch (JSONException e) {
                                // If wrapped format fails, try parsing as direct array
                                Log.d(TAG, "Wrapped format failed, trying direct array format");
                                try {
                                    JSONArray dataArray = new JSONArray(response);
                                    Log.d(TAG, "Successfully parsed direct array with " + dataArray.length() + " items");
                                    parseBoardingHousesData(dataArray);
                                } catch (JSONException e2) {
                                    Log.e(TAG, "Both wrapped and direct array parsing failed");
                                    throw e; // Re-throw original exception
                                }
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            Log.e(TAG, "Response that failed to parse: " + response);
                            showError("Error parsing server response: " + e.getMessage());
                        } catch (Exception e) {
                            Log.e(TAG, "Unexpected error: " + e.getMessage());
                            showError("Unexpected error: " + e.getMessage());
                        } finally {
                            // Hide loading indicator
                            progressBar.setVisibility(View.GONE);
                            updateUI();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Volley error: " + error.getMessage());
                        progressBar.setVisibility(View.GONE);
                        showError("Network error: " + error.getMessage());
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
    
    private void parseBoardingHousesData(JSONArray dataArray) throws JSONException {
        allBoardingHouses.clear();
        
        try {
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
                
                // Create Listing object with full details including prices
                Listing boardingHouse = new Listing(
                    bhId, bhName, bhAddress, bhDescription, bhRules,
                    bhBathrooms, area, buildYear, imagePath, imagePaths, minPrice, maxPrice
                );
                
                allBoardingHouses.add(boardingHouse);
            }
            
            // Initially show all boarding houses
            filteredBoardingHouses.clear();
            filteredBoardingHouses.addAll(allBoardingHouses);
            
            Log.d(TAG, "Loaded " + allBoardingHouses.size() + " boarding houses from API");
            
            // Handle empty results
            if (allBoardingHouses.isEmpty()) {
                showEmptyState();
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing boarding house data: " + e.getMessage());
            throw e; // Re-throw to be handled by the calling method
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error parsing boarding house data: " + e.getMessage());
            throw new JSONException("Error parsing boarding house data: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        // Show empty state
        rvBoardingHouses.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
        
        // Update results count to show error state
        tvResultsCount.setText("Failed to load boarding houses");
    }
    
    private void showEmptyState() {
        rvBoardingHouses.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
        tvResultsCount.setText("No boarding houses found");
    }
    
    // Public method to refresh data (can be called from parent activity)
    public void refreshData() {
        loadBoardingHouses();
    }
    
    private void filterBoardingHouses(String query) {
        filteredBoardingHouses.clear();
        
        if (query.isEmpty()) {
            filteredBoardingHouses.addAll(allBoardingHouses);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Listing boardingHouse : allBoardingHouses) {
                // Search by name, address, and description
                if (boardingHouse.getBhName().toLowerCase().contains(lowerQuery) ||
                    (boardingHouse.getBhAddress() != null && boardingHouse.getBhAddress().toLowerCase().contains(lowerQuery)) ||
                    (boardingHouse.getBhDescription() != null && boardingHouse.getBhDescription().toLowerCase().contains(lowerQuery))) {
                    filteredBoardingHouses.add(boardingHouse);
                }
            }
        }
        
        updateUI();
    }
    
    private void updateUI() {
        adapter.notifyDataSetChanged();
        updateResultsCount();
        
        if (filteredBoardingHouses.isEmpty()) {
            rvBoardingHouses.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvBoardingHouses.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }
    
    private void updateResultsCount() {
        int count = filteredBoardingHouses.size();
        String text = count == 1 ? "Found 1 boarding house" : "Found " + count + " boarding houses";
        tvResultsCount.setText(text);
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
}
