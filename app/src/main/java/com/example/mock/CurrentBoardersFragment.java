package com.example.mock;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CurrentBoardersFragment extends Fragment {

    private static final String TAG = "CurrentBoardersFragment";
    private static final String ARG_USER_ID = "user_id";

    private RecyclerView recyclerView;
    private LinearLayout tvNoActiveRentals;
    private TextView tvCount;
    private LinearLayout headerLayout;
    private ImageView ivHeaderIcon;
    private ProgressDialog progressDialog;
    private BoardersListAdapter adapter;
    private List<BoarderData> currentBoarders;
    private BoarderApiService boarderApiService;
    private int userId;
    private boolean hasLoaded = false;

    public static CurrentBoardersFragment newInstance(int userId) {
        CurrentBoardersFragment fragment = new CurrentBoardersFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_current_boarders, container, false);

        initViews(view);
        setupRecyclerView();
        // Don't load immediately - wait for loadIfNeeded() to be called

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        tvNoActiveRentals = view.findViewById(R.id.tvNoActiveRentals);
        tvCount = view.findViewById(R.id.tvCount);
        headerLayout = view.findViewById(R.id.headerLayout);
        ivHeaderIcon = view.findViewById(R.id.ivHeaderIcon);
        
        // Set header styling (green for Current Boarders)
        if (headerLayout != null) {
            headerLayout.setBackgroundColor(android.graphics.Color.parseColor("#E8F5E8")); // Light green
        }
        if (ivHeaderIcon != null) {
            ivHeaderIcon.setColorFilter(android.graphics.Color.parseColor("#4CAF50")); // Green
        }
        if (tvCount != null) {
            tvCount.setBackgroundResource(R.drawable.bg_rounded_green);
        }
        
        // Get userId from arguments
        if (getArguments() != null) {
            userId = getArguments().getInt(ARG_USER_ID, 0);
            Log.d(TAG, "Received user_id from arguments: " + userId);
        } else {
            // Fallback: try to get from activity intent
            if (getActivity() != null) {
                userId = getActivity().getIntent().getIntExtra("user_id", 0);
                Log.d(TAG, "Received user_id from intent: " + userId);
            }
        }
        
        // Fallback to SharedPreferences if userId is still 0
        if (userId <= 0 && getContext() != null) {
            android.content.SharedPreferences sharedPreferences = getContext().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
            String userIdString = sharedPreferences.getString("user_id", null);
            if (userIdString != null) {
                try {
                    userId = Integer.parseInt(userIdString);
                    Log.d(TAG, "Got user_id from SharedPreferences: " + userId);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing user_id from SharedPreferences", e);
                    userId = 0;
                }
            }
        }
        
        Log.d(TAG, "Final user_id: " + userId);
        
        boarderApiService = new BoarderApiService(getContext());
        currentBoarders = new ArrayList<>();
    }
    
    public void loadIfNeeded() {
        // Try to get user_id again if it's still 0
        if (userId <= 0 && getContext() != null) {
            android.content.SharedPreferences sharedPreferences = getContext().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
            String userIdString = sharedPreferences.getString("user_id", null);
            if (userIdString != null) {
                try {
                    userId = Integer.parseInt(userIdString);
                    Log.d(TAG, "Got user_id from SharedPreferences in loadIfNeeded: " + userId);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing user_id from SharedPreferences in loadIfNeeded", e);
                }
            }
        }
        
        if (!hasLoaded) {
            if (userId > 0) {
                hasLoaded = true;
                Log.d(TAG, "Loading current boarders with user_id: " + userId);
                loadCurrentBoarders();
            } else {
                Log.e(TAG, "Cannot load current boarders: user_id is still 0");
            }
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BoardersListAdapter(currentBoarders);
        adapter.setOnBoarderClickListener(boarder -> {
            // Open stay details activity
            Intent intent = new Intent(getContext(), BoarderStayDetailsActivity.class);
            intent.putExtra("boarder_id", boarder.getBoarderId());
            intent.putExtra("boarder_name", boarder.getBoarderName());
            intent.putExtra("boarder_email", boarder.getBoarderEmail());
            intent.putExtra("boarder_phone", boarder.getBoarderPhone());
            intent.putExtra("boarding_house_name", boarder.getBoardingHouseName());
            intent.putExtra("room_number", boarder.getRoomNumber());
            intent.putExtra("rent_type", boarder.getRentType());
            intent.putExtra("start_date", boarder.getStartDate());
            intent.putExtra("end_date", boarder.getEndDate());
            intent.putExtra("status", boarder.getStatus());
            intent.putExtra("profile_picture", boarder.getProfilePicture());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void loadCurrentBoarders() {
        // Final check - try to get user_id from SharedPreferences if still 0
        if (userId <= 0 && getContext() != null) {
            android.content.SharedPreferences sharedPreferences = getContext().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
            String userIdString = sharedPreferences.getString("user_id", null);
            if (userIdString != null) {
                try {
                    userId = Integer.parseInt(userIdString);
                    Log.d(TAG, "Got user_id from SharedPreferences in loadCurrentBoarders: " + userId);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing user_id from SharedPreferences in loadCurrentBoarders", e);
                }
            }
        }
        
        if (userId <= 0) {
            Log.e(TAG, "Invalid user_id: " + userId + ". Cannot load current boarders.");
            showEmptyState();
            return;
        }

        showProgressDialog("Loading current boarders...");

        boarderApiService.getCurrentBoarders(userId, new BoarderApiService.BoarderApiCallback() {
            @Override
            public void onSuccess(List<BoarderData> boarders) {
                hideProgressDialog();
                currentBoarders.clear();
                currentBoarders.addAll(boarders);
                adapter.notifyDataSetChanged();
                updateEmptyState();
                Log.d(TAG, "Loaded " + boarders.size() + " current boarders");
            }

            @Override
            public void onError(String error) {
                hideProgressDialog();
                Log.e(TAG, "Error loading current boarders: " + error);
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }

    private void updateEmptyState() {
        // Update count
        if (tvCount != null) {
            tvCount.setText(String.valueOf(currentBoarders.size()));
        }
        
        if (currentBoarders.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvNoActiveRentals.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvNoActiveRentals.setVisibility(View.GONE);
        }
    }

    private void showEmptyState() {
        // Update count to 0
        if (tvCount != null) {
            tvCount.setText("0");
        }
        recyclerView.setVisibility(View.GONE);
        tvNoActiveRentals.setVisibility(View.VISIBLE);
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    public void refreshCurrentBoarders() {
        loadCurrentBoarders();
    }
}



































