package com.example.mock;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mock.adapters.BoardingHouseAdapter;
import com.example.mock.adapters.BoardingHouseCarouselAdapter;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * BoarderHomeFragment - Main home page for boarders
 * Displays recommended boarding houses carousel and nearby boarding houses list
 */
public class BoarderHomeFragment extends Fragment implements BoardingHouseAdapter.OnFavoriteClickListener, BoardingHouseCarouselAdapter.OnFavoriteClickListener {

    // Views
    private EditText etSearch;
    private RecyclerView rvRecommendedBH;
    private RecyclerView rvNearbyBH;
    private MaterialButton btnSeeAll;
    private MaterialButton btnMyBookings;
    private MaterialButton btnFavorites;
    private ImageView ivNotification;
    private ImageView ivMessage;
    private TextView tvBoarderName;

    // Adapters
    private BoardingHouseCarouselAdapter recommendedAdapter;
    private BoardingHouseAdapter nearbyAdapter;

    // Data
    private List<Listing> recommendedBoardingHouses;
    private List<Listing> nearbyBoardingHouses;

    public BoarderHomeFragment() {
        // Required empty public constructor
    }

    public static BoarderHomeFragment newInstance() {
        return new BoarderHomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_boarder_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupRecyclerViews();
        setupClickListeners();
        loadMockData();
    }

    private void initializeViews(View view) {
        try {
            etSearch = view.findViewById(R.id.etSearch);
            rvRecommendedBH = view.findViewById(R.id.rvRecommendedBH);
            rvNearbyBH = view.findViewById(R.id.rvNearbyBH);
            btnSeeAll = view.findViewById(R.id.btnSeeAll);
            btnMyBookings = view.findViewById(R.id.cardMyBookings);
            btnFavorites = view.findViewById(R.id.cardFavorites);
            ivNotification = view.findViewById(R.id.ivNotification);
            ivMessage = view.findViewById(R.id.ivMessage);
            tvBoarderName = view.findViewById(R.id.tvBoarderName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupRecyclerViews() {
        try {
            // Initialize data lists
            recommendedBoardingHouses = new ArrayList<>();
            nearbyBoardingHouses = new ArrayList<>();

            // Setup Recommended BHs RecyclerView (Horizontal)
            if (rvRecommendedBH != null && getContext() != null) {
                recommendedAdapter = new BoardingHouseCarouselAdapter(getContext(), recommendedBoardingHouses, this);
                LinearLayoutManager recommendedLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
                rvRecommendedBH.setLayoutManager(recommendedLayoutManager);
                rvRecommendedBH.setAdapter(recommendedAdapter);
            }

            // Setup Nearby BHs RecyclerView (Vertical)
            if (rvNearbyBH != null && getContext() != null) {
                nearbyAdapter = new BoardingHouseAdapter(getContext(), nearbyBoardingHouses, this);
                LinearLayoutManager nearbyLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
                rvNearbyBH.setLayoutManager(nearbyLayoutManager);
                rvNearbyBH.setAdapter(nearbyAdapter);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupClickListeners() {
        // Search functionality
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    try {
                        // TODO: Implement search functionality
                        // For now, just show a toast
                        if (s.length() > 0 && getContext() != null) {
                            Toast.makeText(getContext(), "Searching for: " + s.toString(), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // See All button - Navigate to BoarderExploreFragment
        if (btnSeeAll != null) {
            btnSeeAll.setOnClickListener(v -> {
                try {
                    // Navigate to explore fragment
                    if (getActivity() instanceof BoarderDashboard) {
                        BoarderDashboard dashboard = (BoarderDashboard) getActivity();
                        // Switch to explore tab (nav_post)
                        dashboard.switchToTab(R.id.nav_post);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "Navigation error", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // My Bookings button
        if (btnMyBookings != null) {
            btnMyBookings.setOnClickListener(v -> {
                Toast.makeText(getContext(), "My Bookings - Coming Soon!", Toast.LENGTH_SHORT).show();
                // TODO: Navigate to bookings fragment
            });
        }

        // Favorites button
        if (btnFavorites != null) {
            btnFavorites.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Favorites - Coming Soon!", Toast.LENGTH_SHORT).show();
                // TODO: Navigate to favorites fragment
            });
        }

        // Notification icon
        if (ivNotification != null) {
            ivNotification.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Notifications - Coming Soon!", Toast.LENGTH_SHORT).show();
                // TODO: Navigate to notifications
            });
        }

        // Message icon
        if (ivMessage != null) {
            ivMessage.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Messages - Coming Soon!", Toast.LENGTH_SHORT).show();
                // TODO: Navigate to messages
            });
        }
    }

    private void loadMockData() {
        try {
            // Create mock data for recommended boarding houses
            createRecommendedMockData();
            
            // Create mock data for nearby boarding houses
            createNearbyMockData();
            
            // Update adapters
            if (recommendedAdapter != null) {
                recommendedAdapter.notifyDataSetChanged();
            }
            if (nearbyAdapter != null) {
                nearbyAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createRecommendedMockData() {
        recommendedBoardingHouses.clear();
        
        // Recommended boarding houses (featured/popular ones)
        recommendedBoardingHouses.add(new Listing(1, "Sunshine Boarding House", "sample_listing"));
        recommendedBoardingHouses.add(new Listing(2, "Green Valley Dormitory", "sample_listing"));
        recommendedBoardingHouses.add(new Listing(3, "Metro Student Housing", "sample_listing"));
        recommendedBoardingHouses.add(new Listing(4, "Quezon City Boarding", "sample_listing"));
        recommendedBoardingHouses.add(new Listing(5, "Manila Central Dorm", "sample_listing"));
    }

    private void createNearbyMockData() {
        nearbyBoardingHouses.clear();
        
        // Nearby boarding houses
        nearbyBoardingHouses.add(new Listing(6, "Downtown Boarding House", "sample_listing"));
        nearbyBoardingHouses.add(new Listing(7, "University Dormitory", "sample_listing"));
        nearbyBoardingHouses.add(new Listing(8, "City Center Housing", "sample_listing"));
        nearbyBoardingHouses.add(new Listing(9, "Student Plaza Dorm", "sample_listing"));
        nearbyBoardingHouses.add(new Listing(10, "Metro Boarding Inn", "sample_listing"));
        nearbyBoardingHouses.add(new Listing(11, "Campus View Dormitory", "sample_listing"));
        nearbyBoardingHouses.add(new Listing(12, "Central Station Boarding", "sample_listing"));
    }

    @Override
    public void onFavoriteClick(Listing boardingHouse) {
        try {
            // Toggle favorite status
            boolean isCurrentlyFavorite = BoarderFavoriteFragment.isFavorite(getContext(), boardingHouse.getBhId());
            if (isCurrentlyFavorite) {
                // Remove from favorites
                String message = "Removed from favorites: " + boardingHouse.getBhName();
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            } else {
                // Add to favorites
                BoarderFavoriteFragment.addToFavorites(getContext(), boardingHouse);
                String message = "Added to favorites: " + boardingHouse.getBhName();
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Custom ItemDecoration for horizontal spacing
    public static class HorizontalSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spacing;

        public HorizontalSpacingItemDecoration(int spacing) {
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(android.graphics.Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            
            // Add spacing to the right of each item except the last one
            if (position != parent.getAdapter().getItemCount() - 1) {
                outRect.right = spacing;
            }
        }
    }
}