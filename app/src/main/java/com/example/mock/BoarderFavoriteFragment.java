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
    private MaterialButton btnExploreNow;

    // Adapter and Data
    private BoardingHouseAdapter favoritesAdapter;
    private List<Listing> allFavorites;
    private List<Listing> filteredFavorites;

    // SharedPreferences for persistence
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "boarder_favorites";
    private static final String KEY_FAVORITES = "favorite_ids";

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
        loadFavorites();
    }

    private void initializeViews(View view) {
        try {
            rvFavorites = view.findViewById(R.id.rvFavorites);
            layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
            progressBar = view.findViewById(R.id.progressBar);
            tvFavoritesCount = view.findViewById(R.id.tvFavoritesCount);
            etSearchFavorites = view.findViewById(R.id.etSearchFavorites);
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
                }

                @Override
                public void afterTextChanged(Editable s) {}
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
            
            // Create mock data for favorites (in real app, this would come from API)
            createMockFavoritesData(favoriteIds);

            // Hide loading indicator and update UI
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            updateUI();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createMockFavoritesData(Set<String> favoriteIds) {
        allFavorites.clear();
        
        // Create all possible boarding houses
        List<Listing> allBoardingHouses = new ArrayList<>();
        allBoardingHouses.add(new Listing(1, "Sunshine Boarding House", "sample_listing"));
        allBoardingHouses.add(new Listing(2, "Green Valley Dormitory", "sample_listing"));
        allBoardingHouses.add(new Listing(3, "Metro Student Housing", "sample_listing"));
        allBoardingHouses.add(new Listing(4, "Quezon City Boarding", "sample_listing"));
        allBoardingHouses.add(new Listing(5, "Manila Central Dorm", "sample_listing"));
        allBoardingHouses.add(new Listing(6, "Downtown Boarding House", "sample_listing"));
        allBoardingHouses.add(new Listing(7, "University Dormitory", "sample_listing"));
        allBoardingHouses.add(new Listing(8, "City Center Housing", "sample_listing"));

        // Add only the ones that are in favorites
        for (Listing bh : allBoardingHouses) {
            if (favoriteIds.contains(String.valueOf(bh.getBhId()))) {
                allFavorites.add(bh);
            }
        }

        // If no favorites, add some sample ones for demo
        if (allFavorites.isEmpty()) {
            allFavorites.add(new Listing(1, "Sunshine Boarding House", "sample_listing"));
            allFavorites.add(new Listing(3, "Metro Student Housing", "sample_listing"));
        }

        // Initially show all favorites
        filteredFavorites.clear();
        filteredFavorites.addAll(allFavorites);
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
            
            // Update SharedPreferences
            Set<String> favoriteIds = sharedPreferences.getStringSet(KEY_FAVORITES, new HashSet<>());
            favoriteIds.remove(String.valueOf(boardingHouse.getBhId()));
            
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putStringSet(KEY_FAVORITES, favoriteIds);
            editor.apply();
            
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
        // Refresh favorites when returning to this fragment
        loadFavorites();
    }
}
