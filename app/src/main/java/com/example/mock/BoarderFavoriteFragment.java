package com.example.mock;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mock.adapters.BoardingHouseAdapter;
import com.google.android.material.button.MaterialButton;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * BoarderFavoriteFragment - Displays user's favorite boarding houses
 * Allows users to view, search, and manage their favorite listings
 */
public class BoarderFavoriteFragment extends Fragment implements BoardingHouseAdapter.OnFavoriteClickListener, BoardingHouseAdapter.OnDeleteClickListener {

    // Views
    private RecyclerView rvFavorites;
    private LinearLayout layoutEmptyState;
    private ProgressBar progressBar;
    private TextView tvFavoritesCount;
    private EditText etSearchFavorites;
    private ImageView ivClearSearch;
    private MaterialButton btnExploreNow;

    // Adapter and Data
    private BoardingHouseAdapter favoritesAdapter;
    private List<Listing> allFavorites;
    private List<Listing> filteredFavorites;

    // SharedPreferences for persistence
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "boarder_favorites";
    private static final String KEY_FAVORITES = "favorite_ids";
    
    // Flag to track if data has been loaded (to prevent reloading on navigation)
    private boolean dataLoaded = false;
    
    // Track last known favorite count to detect changes
    private int lastFavoriteCount = 0;
    
    // API URL
    private static final String TAG = "BoarderFavoriteFragment";
    private static final String BASE_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/";
    private static final String API_URL = BASE_URL + "get_boarding_houses1.php";

    public BoarderFavoriteFragment() {
        // Required empty public constructor
    }

    public static BoarderFavoriteFragment newInstance() {
        return new BoarderFavoriteFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_boarder_favorite, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupRecyclerView();
        setupClickListeners();
        
        // Only load data if it hasn't been loaded yet (first time only)
        if (!dataLoaded) {
            loadFavorites();
        } else {
            // Data already loaded, but check if favorites changed
            checkAndRefreshIfNeeded();
        }
    }

    private void initializeViews(View view) {
        try {
            rvFavorites = view.findViewById(R.id.rvFavorites);
            layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
            progressBar = view.findViewById(R.id.progressBar);
            tvFavoritesCount = view.findViewById(R.id.tvFavoritesCount);
            etSearchFavorites = view.findViewById(R.id.etSearchFavorites);
            ivClearSearch = view.findViewById(R.id.ivClearSearch);
            btnExploreNow = view.findViewById(R.id.btnExploreNow);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupRecyclerView() {
        try {
            allFavorites = new ArrayList<>();
            filteredFavorites = new ArrayList<>();

            favoritesAdapter = new BoardingHouseAdapter(getContext(), filteredFavorites, this, this, true);
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
            rvFavorites.setLayoutManager(layoutManager);
            rvFavorites.setAdapter(favoritesAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupClickListeners() {
        // Search functionality
        if (etSearchFavorites != null) {
            etSearchFavorites.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterFavorites(s.toString());
                    // Show/hide clear icon based on text
                    if (ivClearSearch != null) {
                        ivClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // Setup clear search icon
        if (ivClearSearch != null) {
            ivClearSearch.setOnClickListener(v -> {
                if (etSearchFavorites != null) {
                    etSearchFavorites.setText("");
                    etSearchFavorites.clearFocus();
                    ivClearSearch.setVisibility(View.GONE);
                }
            });
        }

        // Explore Now button
        if (btnExploreNow != null) {
            btnExploreNow.setOnClickListener(v -> {
                try {
                    // Navigate to explore fragment
                    if (getActivity() instanceof BoarderDashboard) {
                        BoarderDashboard dashboard = (BoarderDashboard) getActivity();
                        dashboard.switchToTab(R.id.nav_post);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }


    private void loadFavorites() {
        try {
            // Show loading indicator
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            if (rvFavorites != null) {
                rvFavorites.setVisibility(View.GONE);
            }
            if (layoutEmptyState != null) {
                layoutEmptyState.setVisibility(View.GONE);
            }

            // Get favorite IDs from SharedPreferences
            Set<String> favoriteIds = sharedPreferences.getStringSet(KEY_FAVORITES, new HashSet<>());
            
            // If no favorites, show empty state immediately
            if (favoriteIds.isEmpty()) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                allFavorites.clear();
                filteredFavorites.clear();
                // Update tracking flags
                dataLoaded = true;
                lastFavoriteCount = 0;
                updateUI();
                return;
            }
            
            // Fetch real data from API
            fetchBoardingHousesFromAPI(favoriteIds);
        } catch (Exception e) {
            Log.e(TAG, "Error loading favorites: " + e.getMessage());
            e.printStackTrace();
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            showError("Error loading favorites");
        }
    }

    private void fetchBoardingHousesFromAPI(Set<String> favoriteIds) {
        try {
            // Create request queue
            RequestQueue requestQueue = Volley.newRequestQueue(getContext());
            
            // Create string request with custom headers
            StringRequest stringRequest = new StringRequest(Request.Method.GET, API_URL,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                Log.d(TAG, "API Response received");
                                
                                // Check if response is null or empty
                                if (response == null || response.trim().isEmpty()) {
                                    Log.e(TAG, "Received null or empty response");
                                    showError("Server returned empty response");
                                    return;
                                }
                                
                                // Check if response is HTML (error page)
                                if (response.trim().startsWith("<!DOCTYPE html>") || response.trim().startsWith("<html")) {
                                    Log.e(TAG, "Received HTML page instead of JSON");
                                    showError("Server returned HTML instead of JSON. Please check the API endpoint.");
                                    return;
                                }
                                
                                // Parse JSON response
                                JSONObject jsonResponse = new JSONObject(response);
                                boolean success = jsonResponse.getBoolean("success");
                                
                                if (success) {
                                    JSONArray dataArray = jsonResponse.getJSONArray("data");
                                    Log.d(TAG, "Successfully parsed JSON with " + dataArray.length() + " items");
                                    parseAndFilterBoardingHouses(dataArray, favoriteIds);
                                } else {
                                    String error = jsonResponse.optString("error", "Unknown error occurred");
                                    Log.e(TAG, "API Error: " + error);
                                    showError("Failed to load boarding houses: " + error);
                                }
                            } catch (JSONException e) {
                                // Try parsing as direct array
                                try {
                                    JSONArray dataArray = new JSONArray(response);
                                    Log.d(TAG, "Successfully parsed direct array with " + dataArray.length() + " items");
                                    parseAndFilterBoardingHouses(dataArray, favoriteIds);
                                } catch (JSONException e2) {
                                    Log.e(TAG, "JSON parsing error: " + e.getMessage());
                                    showError("Error parsing server response: " + e.getMessage());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Unexpected error: " + e.getMessage());
                                showError("Unexpected error: " + e.getMessage());
                            } finally {
                                // Hide loading indicator
                                if (progressBar != null) {
                                    progressBar.setVisibility(View.GONE);
                                }
                                updateUI();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "Volley error: " + error.getMessage());
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                            showError("Network error: " + error.getMessage());
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
            
            // Add request to queue
            requestQueue.add(stringRequest);
        } catch (Exception e) {
            Log.e(TAG, "Error creating request: " + e.getMessage());
            e.printStackTrace();
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            showError("Error loading favorites");
        }
    }
    
    private void parseAndFilterBoardingHouses(JSONArray dataArray, Set<String> favoriteIds) throws JSONException {
        allFavorites.clear();
        
        try {
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject boardingHouseJson = dataArray.getJSONObject(i);
                
                int bhId = boardingHouseJson.getInt("bh_id");
                
                // Only add if this boarding house is in favorites
                if (favoriteIds.contains(String.valueOf(bhId))) {
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
                    
                    // Parse and set owner contact information
                    String ownerName = boardingHouseJson.optString("owner_name", "");
                    String ownerPhone = boardingHouseJson.optString("owner_phone", "");
                    String ownerEmail = boardingHouseJson.optString("owner_email", "");
                    
                    // If owner_name is not directly available, build it from separate fields
                    if (ownerName.isEmpty()) {
                        String firstName = boardingHouseJson.optString("owner_first_name", "");
                        String middleName = boardingHouseJson.optString("owner_middle_name", "");
                        String lastName = boardingHouseJson.optString("owner_last_name", "");
                        
                        StringBuilder nameBuilder = new StringBuilder();
                        if (!firstName.isEmpty()) nameBuilder.append(firstName);
                        if (!middleName.isEmpty()) {
                            if (nameBuilder.length() > 0) nameBuilder.append(" ");
                            nameBuilder.append(middleName);
                        }
                        if (!lastName.isEmpty()) {
                            if (nameBuilder.length() > 0) nameBuilder.append(" ");
                            nameBuilder.append(lastName);
                        }
                        ownerName = nameBuilder.toString();
                    }
                    
                    boardingHouse.setOwnerName(ownerName);
                    boardingHouse.setOwnerPhone(ownerPhone);
                    boardingHouse.setOwnerEmail(ownerEmail);
                    
                    allFavorites.add(boardingHouse);
                }
            }
            
            Log.d(TAG, "Loaded " + allFavorites.size() + " favorite boarding houses");
            
            // Initially show all favorites
            filteredFavorites.clear();
            filteredFavorites.addAll(allFavorites);
            
            // Update tracking flags
            dataLoaded = true;
            lastFavoriteCount = allFavorites.size();
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing boarding house data: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error parsing boarding house data: " + e.getMessage());
            throw new JSONException("Error parsing boarding house data: " + e.getMessage());
        }
    }
    
    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        allFavorites.clear();
        filteredFavorites.clear();
        updateUI();
    }

    private void filterFavorites(String query) {
        try {
            filteredFavorites.clear();
            
            if (query.isEmpty()) {
                filteredFavorites.addAll(allFavorites);
            } else {
                String lowerQuery = query.toLowerCase();
                for (Listing favorite : allFavorites) {
                    if (favorite.getBhName().toLowerCase().contains(lowerQuery)) {
                        filteredFavorites.add(favorite);
                    }
                }
            }
            
            updateUI();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUI() {
        try {
            if (favoritesAdapter != null) {
                favoritesAdapter.notifyDataSetChanged();
            }
            updateFavoritesCount();
            
            if (filteredFavorites.isEmpty()) {
                if (rvFavorites != null) {
                    rvFavorites.setVisibility(View.GONE);
                }
                if (layoutEmptyState != null) {
                    layoutEmptyState.setVisibility(View.VISIBLE);
                }
            } else {
                if (rvFavorites != null) {
                    rvFavorites.setVisibility(View.VISIBLE);
                }
                if (layoutEmptyState != null) {
                    layoutEmptyState.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateFavoritesCount() {
        try {
            if (tvFavoritesCount != null) {
                int count = filteredFavorites.size();
                String text = count == 1 ? "1 favorite" : count + " favorites";
                tvFavoritesCount.setText(text);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFavoriteClick(Listing boardingHouse, boolean isFavorite) {
        try {
            if (!isFavorite) {
                // Remove from favorites
                removeFromFavorites(boardingHouse);
                String message = "Removed from favorites: " + boardingHouse.getBhName();
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            } else {
                // This shouldn't happen in favorites fragment, but handle it gracefully
                Toast.makeText(getContext(), "Already in favorites!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDeleteClick(Listing boardingHouse) {
        try {
            showDeleteConfirmationDialog(boardingHouse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showDeleteConfirmationDialog(Listing boardingHouse) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Delete from Favorites");
            builder.setMessage("Do you really want to delete \"" + boardingHouse.getBhName() + "\" from favorites?");
            
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // User confirmed deletion
                    removeFromFavorites(boardingHouse);
                    String message = "Removed from favorites: " + boardingHouse.getBhName();
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
            
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // User cancelled deletion
                    dialog.dismiss();
                }
            });
            
            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeFromFavorites(Listing boardingHouse) {
        try {
            // Remove from local lists
            allFavorites.remove(boardingHouse);
            filteredFavorites.remove(boardingHouse);
            
            // Update SharedPreferences using static method
            BoarderFavoriteFragment.removeFromFavorites(getContext(), boardingHouse);
            
            // Update UI
            updateUI();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Public method to add a boarding house to favorites (called from other fragments)
    public static void addToFavorites(android.content.Context context, Listing boardingHouse) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
            Set<String> favoriteIds = prefs.getStringSet(KEY_FAVORITES, new HashSet<>());
            favoriteIds.add(String.valueOf(boardingHouse.getBhId()));
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putStringSet(KEY_FAVORITES, favoriteIds);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Public method to remove a boarding house from favorites (called from other fragments)
    public static void removeFromFavorites(android.content.Context context, Listing boardingHouse) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
            Set<String> favoriteIds = prefs.getStringSet(KEY_FAVORITES, new HashSet<>());
            favoriteIds.remove(String.valueOf(boardingHouse.getBhId()));
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putStringSet(KEY_FAVORITES, favoriteIds);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Public method to check if a boarding house is in favorites
    public static boolean isFavorite(android.content.Context context, int boardingHouseId) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
            Set<String> favoriteIds = prefs.getStringSet(KEY_FAVORITES, new HashSet<>());
            return favoriteIds.contains(String.valueOf(boardingHouseId));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check if favorites have changed when fragment becomes visible
        if (dataLoaded && isVisible()) {
            checkAndRefreshIfNeeded();
        }
    }
    
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // When fragment becomes visible, check if favorites changed
        if (!hidden && dataLoaded) {
            checkAndRefreshIfNeeded();
        }
    }
    
    private void checkAndRefreshIfNeeded() {
        try {
            // Get current favorite count from SharedPreferences
            Set<String> currentFavoriteIds = sharedPreferences.getStringSet(KEY_FAVORITES, new HashSet<>());
            int currentCount = currentFavoriteIds.size();
            
            // If count changed, or if we don't have data, refresh
            if (currentCount != lastFavoriteCount || allFavorites == null || allFavorites.isEmpty()) {
                Log.d(TAG, "Favorites changed (count: " + lastFavoriteCount + " -> " + currentCount + "), refreshing...");
                loadFavorites();
            } else {
                Log.d(TAG, "Favorites unchanged, no refresh needed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking favorites: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
