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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    // User session
    private SharedPreferences userSessionPrefs;
    private int userId;
    
    // Flag to track if data has been loaded (to prevent reloading on navigation)
    private boolean dataLoaded = false;
    
    // API URL
    private static final String TAG = "BoarderFavoriteFragment";
    private static final String BASE_URL = "http://192.168.1.9/boardease_v3/";
    private static final String BOARD_EASE2_URL = BASE_URL + "BoardEase2/";
    private static final String GET_FAVORITES_URL = BOARD_EASE2_URL + "get_favorites.php";
    private static final String ADD_FAVORITE_URL = BOARD_EASE2_URL + "add_favorite.php";
    private static final String REMOVE_FAVORITE_URL = BOARD_EASE2_URL + "remove_favorite.php";
    
    // Request queue
    private RequestQueue requestQueue;

    public BoarderFavoriteFragment() {
        // Required empty public constructor
    }

    public static BoarderFavoriteFragment newInstance() {
        return new BoarderFavoriteFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get user ID from UserSession
        if (getContext() != null) {
            userSessionPrefs = getContext().getSharedPreferences("UserSession", 0);
            String userIdString = userSessionPrefs.getString("user_id", null);
            if (userIdString != null) {
                try {
                    userId = Integer.parseInt(userIdString);
                } catch (NumberFormatException e) {
                    userId = 0;
                }
            }
            requestQueue = Volley.newRequestQueue(getContext());
        }
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
            if (userId == 0) {
                Log.e(TAG, "User ID is 0, cannot load favorites");
                showError("User not logged in");
                return;
            }
            
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
            
            // Fetch favorites from database API
            fetchFavoritesFromAPI();
        } catch (Exception e) {
            Log.e(TAG, "Error loading favorites: " + e.getMessage());
            e.printStackTrace();
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            showError("Error loading favorites");
        }
    }

    private void fetchFavoritesFromAPI() {
        try {
            StringRequest stringRequest = new StringRequest(Request.Method.POST, GET_FAVORITES_URL,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                Log.d(TAG, "Favorites API Response received");
                                
                                // Check if response is null or empty
                                if (response == null || response.trim().isEmpty()) {
                                    Log.e(TAG, "Received null or empty response");
                                    showError("Server returned empty response");
                                    return;
                                }
                                
                                // Parse JSON response
                                JSONObject jsonResponse = new JSONObject(response);
                                boolean success = jsonResponse.getBoolean("success");
                                
                                if (success) {
                                    JSONArray dataArray = jsonResponse.getJSONArray("data");
                                    Log.d(TAG, "Successfully parsed JSON with " + dataArray.length() + " favorites");
                                    parseBoardingHouses(dataArray);
                                } else {
                                    String error = jsonResponse.optString("error", "Unknown error occurred");
                                    Log.e(TAG, "API Error: " + error);
                                    showError("Failed to load favorites: " + error);
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "JSON parsing error: " + e.getMessage());
                                showError("Error parsing server response: " + e.getMessage());
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
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("user_id", String.valueOf(userId));
                    return params;
                }
                
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
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
    
    private void parseBoardingHouses(JSONArray dataArray) throws JSONException {
        allFavorites.clear();
        
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
            
            Log.d(TAG, "Loaded " + allFavorites.size() + " favorite boarding houses");
            
            // Update cache with favorite IDs
            favoriteIdsCache.clear();
            for (Listing listing : allFavorites) {
                favoriteIdsCache.add(listing.getBhId());
            }
            cacheInitialized = true;
            
            // Initially show all favorites
            filteredFavorites.clear();
            filteredFavorites.addAll(allFavorites);
            
            // Update tracking flags
            dataLoaded = true;
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
            // Remove from database via API
            removeFavoriteFromAPI(boardingHouse);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error removing favorite", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void removeFavoriteFromAPI(Listing boardingHouse) {
        try {
            StringRequest stringRequest = new StringRequest(Request.Method.POST, REMOVE_FAVORITE_URL,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject jsonResponse = new JSONObject(response);
                                boolean success = jsonResponse.getBoolean("success");
                                
                                if (success) {
                                    // Remove from cache
                                    removeFromCache(boardingHouse.getBhId());
                                    // Remove from local lists
                                    allFavorites.remove(boardingHouse);
                                    filteredFavorites.remove(boardingHouse);
                                    // Update UI
                                    updateUI();
                                } else {
                                    String error = jsonResponse.optString("error", "Failed to remove favorite");
                                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing remove favorite response: " + e.getMessage());
                                Toast.makeText(getContext(), "Error removing favorite", Toast.LENGTH_SHORT).show();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "Error removing favorite: " + error.getMessage());
                            Toast.makeText(getContext(), "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("user_id", String.valueOf(userId));
                    params.put("bh_id", String.valueOf(boardingHouse.getBhId()));
                    return params;
                }
                
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("User-Agent", "BoardEase-Android-App");
                    headers.put("Accept", "application/json");
                    return headers;
                }
            };
            
            requestQueue.add(stringRequest);
        } catch (Exception e) {
            Log.e(TAG, "Error creating remove favorite request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Public method to add a boarding house to favorites (called from other fragments)
    public static void addToFavorites(android.content.Context context, Listing boardingHouse) {
        try {
            // Optimistically update cache immediately for responsive UI
            addToCache(boardingHouse.getBhId());
            
            // Get user ID from SharedPreferences
            SharedPreferences userSessionPrefs = context.getSharedPreferences("UserSession", 0);
            String userIdString = userSessionPrefs.getString("user_id", null);
            if (userIdString == null) {
                Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
                removeFromCache(boardingHouse.getBhId()); // Revert optimistic update
                return;
            }
            
            int userId = Integer.parseInt(userIdString);
            
            // Create request queue
            RequestQueue requestQueue = Volley.newRequestQueue(context);
            
            String BASE_URL = "http://192.168.1.9/boardease_v3/";
            String ADD_FAVORITE_URL = BASE_URL + "BoardEase2/add_favorite.php";
            
            StringRequest stringRequest = new StringRequest(Request.Method.POST, ADD_FAVORITE_URL,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject jsonResponse = new JSONObject(response);
                                boolean success = jsonResponse.getBoolean("success");
                                if (success) {
                                    // Success - favorite added to database (cache already updated)
                                    Log.d("BoarderFavoriteFragment", "Added to favorites: " + boardingHouse.getBhName());
                                } else {
                                    // Revert optimistic update on error
                                    removeFromCache(boardingHouse.getBhId());
                                    String error = jsonResponse.optString("error", "Failed to add favorite");
                                    Log.e("BoarderFavoriteFragment", "Error: " + error);
                                }
                            } catch (JSONException e) {
                                // Revert optimistic update on error
                                removeFromCache(boardingHouse.getBhId());
                                Log.e("BoarderFavoriteFragment", "Error parsing add favorite response: " + e.getMessage());
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // Revert optimistic update on error
                            removeFromCache(boardingHouse.getBhId());
                            Log.e("BoarderFavoriteFragment", "Error adding favorite: " + error.getMessage());
                        }
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("user_id", String.valueOf(userId));
                    params.put("bh_id", String.valueOf(boardingHouse.getBhId()));
                    return params;
                }
                
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("User-Agent", "BoardEase-Android-App");
                    headers.put("Accept", "application/json");
                    return headers;
                }
            };
            
            requestQueue.add(stringRequest);
        } catch (Exception e) {
            // Revert optimistic update on error
            removeFromCache(boardingHouse.getBhId());
            Log.e("BoarderFavoriteFragment", "Error adding favorite: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Public method to remove a boarding house from favorites (called from other fragments)
    public static void removeFromFavorites(android.content.Context context, Listing boardingHouse) {
        try {
            // Optimistically update cache immediately for responsive UI
            removeFromCache(boardingHouse.getBhId());
            
            // Get user ID from SharedPreferences
            SharedPreferences userSessionPrefs = context.getSharedPreferences("UserSession", 0);
            String userIdString = userSessionPrefs.getString("user_id", null);
            if (userIdString == null) {
                addToCache(boardingHouse.getBhId()); // Revert optimistic update
                return;
            }
            
            int userId = Integer.parseInt(userIdString);
            
            // Create request queue
            RequestQueue requestQueue = Volley.newRequestQueue(context);
            
            String BASE_URL = "http://192.168.1.9/boardease_v3/";
            String REMOVE_FAVORITE_URL = BASE_URL + "BoardEase2/remove_favorite.php";
            
            StringRequest stringRequest = new StringRequest(Request.Method.POST, REMOVE_FAVORITE_URL,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject jsonResponse = new JSONObject(response);
                                boolean success = jsonResponse.getBoolean("success");
                                if (success) {
                                    // Success - favorite removed from database (cache already updated)
                                    Log.d("BoarderFavoriteFragment", "Removed from favorites: " + boardingHouse.getBhName());
                                } else {
                                    // Revert optimistic update on error
                                    addToCache(boardingHouse.getBhId());
                                    String error = jsonResponse.optString("error", "Failed to remove favorite");
                                    Log.e("BoarderFavoriteFragment", "Error: " + error);
                                }
                            } catch (JSONException e) {
                                // Revert optimistic update on error
                                addToCache(boardingHouse.getBhId());
                                Log.e("BoarderFavoriteFragment", "Error parsing remove favorite response: " + e.getMessage());
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // Revert optimistic update on error
                            addToCache(boardingHouse.getBhId());
                            Log.e("BoarderFavoriteFragment", "Error removing favorite: " + error.getMessage());
                        }
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("user_id", String.valueOf(userId));
                    params.put("bh_id", String.valueOf(boardingHouse.getBhId()));
                    return params;
                }
                
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("User-Agent", "BoardEase-Android-App");
                    headers.put("Accept", "application/json");
                    return headers;
                }
            };
            
            requestQueue.add(stringRequest);
        } catch (Exception e) {
            // Revert optimistic update on error
            addToCache(boardingHouse.getBhId());
            Log.e("BoarderFavoriteFragment", "Error removing favorite: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Static cache for favorite IDs (shared across fragments)
    private static Set<Integer> favoriteIdsCache = new HashSet<>();
    private static boolean cacheInitialized = false;
    
    // Public method to check if a boarding house is in favorites
    public static boolean isFavorite(android.content.Context context, int boardingHouseId) {
        // Check cache first
        return favoriteIdsCache.contains(boardingHouseId);
    }
    
    // Method to update the favorites cache
    public static void updateFavoriteCache(android.content.Context context) {
        try {
            SharedPreferences userSessionPrefs = context.getSharedPreferences("UserSession", 0);
            String userIdString = userSessionPrefs.getString("user_id", null);
            if (userIdString == null) {
                favoriteIdsCache.clear();
                cacheInitialized = true;
                return;
            }
            
            int userId = Integer.parseInt(userIdString);
            
            // Create request queue
            RequestQueue requestQueue = Volley.newRequestQueue(context);
            
            String BASE_URL = "http://192.168.1.9/boardease_v3/";
            String GET_FAVORITES_URL = BASE_URL + "BoardEase2/get_favorites.php";
            
            StringRequest stringRequest = new StringRequest(Request.Method.POST, GET_FAVORITES_URL,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject jsonResponse = new JSONObject(response);
                                boolean success = jsonResponse.getBoolean("success");
                                
                                favoriteIdsCache.clear();
                                
                                if (success) {
                                    JSONArray dataArray = jsonResponse.getJSONArray("data");
                                    for (int i = 0; i < dataArray.length(); i++) {
                                        JSONObject item = dataArray.getJSONObject(i);
                                        int bhId = item.getInt("bh_id");
                                        favoriteIdsCache.add(bhId);
                                    }
                                    cacheInitialized = true;
                                    Log.d("BoarderFavoriteFragment", "Updated favorite cache with " + favoriteIdsCache.size() + " items");
                                } else {
                                    cacheInitialized = true;
                                }
                            } catch (JSONException e) {
                                Log.e("BoarderFavoriteFragment", "Error parsing favorites cache: " + e.getMessage());
                                cacheInitialized = true;
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("BoarderFavoriteFragment", "Error fetching favorites cache: " + error.getMessage());
                            cacheInitialized = true;
                        }
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("user_id", String.valueOf(userId));
                    return params;
                }
                
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("User-Agent", "BoardEase-Android-App");
                    headers.put("Accept", "application/json");
                    return headers;
                }
            };
            
            requestQueue.add(stringRequest);
        } catch (Exception e) {
            Log.e("BoarderFavoriteFragment", "Error updating favorite cache: " + e.getMessage());
            e.printStackTrace();
            cacheInitialized = true;
        }
    }
    
    // Method to add to cache
    public static void addToCache(int bhId) {
        favoriteIdsCache.add(bhId);
    }
    
    // Method to remove from cache
    public static void removeFromCache(int bhId) {
        favoriteIdsCache.remove(bhId);
    }
    
    // Method to check if cache is initialized
    public static boolean isCacheInitialized() {
        return cacheInitialized;
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
            // Always reload from database when fragment becomes visible
            // to ensure we have the latest data
            if (!dataLoaded || allFavorites == null || allFavorites.isEmpty()) {
                Log.d(TAG, "Refreshing favorites from database...");
                loadFavorites();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking favorites: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
