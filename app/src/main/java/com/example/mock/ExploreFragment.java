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
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
    private MaterialButton btnFilter, btnSort;
    
    private BoardingHouseAdapter adapter;
    private List<Listing> allBoardingHouses;
    private List<Listing> filteredBoardingHouses;
    private int userId;
    
    // Filter and Sort state
    private String currentSortBy = "sortby"; // sortby, name, price_low, price_high, date
    private String currentFilter = "all"; // all, private_room, bed_spacer
    
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
            setupFilterAndSortButtons();
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
        btnFilter = view.findViewById(R.id.btnFilter);
        btnSort = view.findViewById(R.id.btnSort);
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
    
    private void setupFilterAndSortButtons() {
        // Filter button click listener
        btnFilter.setOnClickListener(v -> showFilterDialog());
        
        // Sort button click listener
        btnSort.setOnClickListener(v -> showSortDialog());
        
        // Update button text
        updateButtonText();
    }
    
    private void updateButtonText() {
        // Update filter button text
        switch (currentFilter) {
            case "all":
                btnFilter.setText("All Types");
                break;
            case "private_room":
                btnFilter.setText("Private Rooms");
                break;
            case "bed_spacer":
                btnFilter.setText("Bed Spacers");
                break;
        }
        
        // Update sort button text
        switch (currentSortBy) {
            case "sortby":
                btnSort.setText("Sort by");
                break;
            case "name":
                btnSort.setText("Name (A-Z)");
                break;
            case "price_low":
                btnSort.setText("Price (Low-High)");
                break;
            case "price_high":
                btnSort.setText("Price (High-Low)");
                break;
            case "date":
                btnSort.setText("Date (Newest)");
                break;
        }
    }
    
    private void showFilterDialog() {
        String[] filterOptions = {"All Types", "Private Rooms", "Bed Spacers"};
        int currentSelection = getCurrentFilterIndex();
        
        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Filter by Room Type")
                .setSingleChoiceItems(filterOptions, currentSelection, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            currentFilter = "all";
                            break;
                        case 1:
                            currentFilter = "private_room";
                            break;
                        case 2:
                            currentFilter = "bed_spacer";
                            break;
                    }
                    dialog.dismiss();
                    updateButtonText();
                    applyFiltersAndSort();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showSortDialog() {
        String[] sortOptions = {"Sort by","Name (A-Z)", "Price (Low to High)", "Price (High to Low)", "Date (Newest)"};
        int currentSelection = getCurrentSortIndex();
        
        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Sort by")
                .setSingleChoiceItems(sortOptions, currentSelection, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            currentSortBy = "sortby";
                            break;
                        case 1:
                            currentSortBy = "name";
                            break;
                        case 2:
                            currentSortBy = "price_low";
                            break;
                        case 3:
                            currentSortBy = "price_high";
                            break;
                        case 4:
                            currentSortBy = "date";
                            break;
                    }
                    dialog.dismiss();
                    updateButtonText();
                    applyFiltersAndSort();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private int getCurrentFilterIndex() {
        switch (currentFilter) {
            case "all": return 0;
            case "private_room": return 1;
            case "bed_spacer": return 2;
            default: return 0;
        }
    }
    
    private int getCurrentSortIndex() {
        switch (currentSortBy) {
            case "sortby": return 0;
            case "name": return 1;
            case "price_low": return 2;
            case "price_high": return 3;
            case "date": return 4;
            default: return 0;
        }
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
                            
                            // Debug: Log the first 200 characters of response
                            Log.d(TAG, "Response preview: " + response.substring(0, Math.min(200, response.length())));
                            
                            // Check if response is HTML (ngrok warning page)
                            if (response.trim().startsWith("<!DOCTYPE html>") || response.contains("ngrok")) {
                                Log.e(TAG, "Received ngrok warning page instead of JSON");
                                Log.e(TAG, "Full response: " + response);
                                Log.e(TAG, "SOLUTION: Visit https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_boarding_houses.php in your browser first");
                                showError("Ngrok warning! Visit API URL in browser first.");
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
                headers.put("ngrok-skip-browser-warning", "any");
                headers.put("User-Agent", "BoardEase-Android-App");
                headers.put("Accept", "application/json");
                Log.d(TAG, "Sending headers: " + headers.toString());
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
            
        Log.d(TAG, "Loaded " + allBoardingHouses.size() + " boarding houses from API");
        
        // Debug: Log first boarding house details
        if (!allBoardingHouses.isEmpty()) {
            Listing first = allBoardingHouses.get(0);
            Log.d(TAG, "First boarding house: " + first.getBhName() + " - " + first.getBhAddress());
        }
        
        // Apply current filters and sort
        applyFiltersAndSort();
        
        Log.d(TAG, "After filtering: " + filteredBoardingHouses.size() + " boarding houses");
        
        // Handle empty results
        if (allBoardingHouses.isEmpty()) {
            Log.d(TAG, "No boarding houses loaded from API");
            showEmptyState();
        } else if (filteredBoardingHouses.isEmpty()) {
            Log.d(TAG, "Boarding houses loaded but filtered out");
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
        applyFiltersAndSort(query);
    }
    
    private void applyFiltersAndSort() {
        applyFiltersAndSort(etSearch.getText().toString());
    }
    
    private void applyFiltersAndSort(String query) {
        filteredBoardingHouses.clear();
        
        // Apply search filter
        List<Listing> searchFiltered = new ArrayList<>();
        if (query.isEmpty()) {
            searchFiltered.addAll(allBoardingHouses);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Listing boardingHouse : allBoardingHouses) {
                // Search by name, address, and description
                if (boardingHouse.getBhName().toLowerCase().contains(lowerQuery) ||
                    (boardingHouse.getBhAddress() != null && boardingHouse.getBhAddress().toLowerCase().contains(lowerQuery)) ||
                    (boardingHouse.getBhDescription() != null && boardingHouse.getBhDescription().toLowerCase().contains(lowerQuery))) {
                    searchFiltered.add(boardingHouse);
                }
            }
        }
        
        // Apply room type filter
        for (Listing boardingHouse : searchFiltered) {
            if (currentFilter.equals("all") || 
                (currentFilter.equals("private_room") && hasPrivateRooms(boardingHouse)) ||
                (currentFilter.equals("bed_spacer") && hasBedSpacers(boardingHouse))) {
                filteredBoardingHouses.add(boardingHouse);
            }
        }
        
        // Apply sorting
        sortBoardingHouses();
        
        updateUI();
    }
    
    private boolean hasPrivateRooms(Listing boardingHouse) {
        // For now, assume all boarding houses have private rooms
        // In a real implementation, you'd check the room data
        return true;
    }
    
    private boolean hasBedSpacers(Listing boardingHouse) {
        // For now, assume all boarding houses have bed spacers
        // In a real implementation, you'd check the room data
        return true;
    }
    
    private void sortBoardingHouses() {
        switch (currentSortBy) {
            case "sortby":
                // Default: No sorting (keep original order)
                break;
            case "name":
                filteredBoardingHouses.sort((a, b) -> a.getBhName().compareToIgnoreCase(b.getBhName()));
                break;
            case "price_low":
                filteredBoardingHouses.sort((a, b) -> {
                    int priceA = a.getMinPrice() != null ? a.getMinPrice() : Integer.MAX_VALUE;
                    int priceB = b.getMinPrice() != null ? b.getMinPrice() : Integer.MAX_VALUE;
                    return Integer.compare(priceA, priceB);
                });
                break;
            case "price_high":
                filteredBoardingHouses.sort((a, b) -> {
                    int priceA = a.getMinPrice() != null ? a.getMinPrice() : 0;
                    int priceB = b.getMinPrice() != null ? b.getMinPrice() : 0;
                    return Integer.compare(priceB, priceA);
                });
                break;
            case "date":
                // Sort by ID (assuming higher ID = newer)
                filteredBoardingHouses.sort((a, b) -> Integer.compare(b.getBhId(), a.getBhId()));
                break;
        }
    }
    
    private void updateUI() {
        Log.d(TAG, "updateUI called - filteredBoardingHouses size: " + filteredBoardingHouses.size());
        adapter.notifyDataSetChanged();
        updateResultsCount();
        
        if (filteredBoardingHouses.isEmpty()) {
            Log.d(TAG, "Showing empty state");
            rvBoardingHouses.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG, "Showing boarding houses list");
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
