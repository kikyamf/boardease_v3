package com.example.mock;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.mock.adapters.BoardingHouseAdapter;
import com.example.mock.adapters.BoardingHouseAdapter.OnFavoriteClickListener;
import java.util.ArrayList;
import java.util.List;

public class ExploreFragment extends Fragment implements OnFavoriteClickListener {
    
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
        
        // Simulate loading data (replace with actual API call)
        // For now, creating sample data
        createSampleData();
        
        // Hide loading indicator and show results
        progressBar.setVisibility(View.GONE);
        updateUI();
    }
    
    private void createSampleData() {
        allBoardingHouses.clear();
        
        // Sample boarding houses using the correct constructor
        Listing bh1 = new Listing(1, "Sunshine Boarding House", "sample_listing");
        allBoardingHouses.add(bh1);
        
        Listing bh2 = new Listing(2, "Green Valley Dormitory", "sample_listing");
        allBoardingHouses.add(bh2);
        
        Listing bh3 = new Listing(3, "Metro Student Housing", "sample_listing");
        allBoardingHouses.add(bh3);
        
        Listing bh4 = new Listing(4, "Quezon City Boarding", "sample_listing");
        allBoardingHouses.add(bh4);
        
        Listing bh5 = new Listing(5, "Manila Central Dorm", "sample_listing");
        allBoardingHouses.add(bh5);
        
        // Initially show all boarding houses
        filteredBoardingHouses.clear();
        filteredBoardingHouses.addAll(allBoardingHouses);
    }
    
    private void filterBoardingHouses(String query) {
        filteredBoardingHouses.clear();
        
        if (query.isEmpty()) {
            filteredBoardingHouses.addAll(allBoardingHouses);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Listing boardingHouse : allBoardingHouses) {
                if (boardingHouse.getBhName().toLowerCase().contains(lowerQuery)) {
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
