package com.example.mock;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class BoardersHistoryFragment extends Fragment {

    private static final String TAG = "BoardersHistoryFragment";
    private static final String ARG_USER_ID = "user_id";

    private RecyclerView recyclerView;
    private TextView emptyState;
    private ProgressDialog progressDialog;
    private BoardersHistoryAdapter adapter;
    private List<BoarderHistoryData> boardersHistory;
    private BoarderApiService boarderApiService;
    private int userId;
    private boolean hasLoaded = false;

    public static BoardersHistoryFragment newInstance(int userId) {
        BoardersHistoryFragment fragment = new BoardersHistoryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_boarders_history, container, false);

        initViews(view);
        setupRecyclerView();
        // Don't load immediately - wait for loadIfNeeded() to be called

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyState = view.findViewById(R.id.emptyState);
        
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
        boardersHistory = new ArrayList<>();
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
                Log.d(TAG, "Loading boarders history with user_id: " + userId);
                loadBoardersHistory();
            } else {
                Log.e(TAG, "Cannot load boarders history: user_id is still 0");
            }
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BoardersHistoryAdapter(boardersHistory);
        adapter.setOnBoarderHistoryClickListener(boarder -> {
            // Open stay details activity
            Intent intent = new Intent(getContext(), BoarderStayDetailsActivity.class);
            intent.putExtra("boarder_name", boarder.getBoarderName());
            intent.putExtra("room_name", boarder.getRoomName());
            intent.putExtra("start_date", boarder.getStartDate());
            intent.putExtra("end_date", boarder.getEndDate());
            intent.putExtra("status", boarder.getStatus());
            intent.putExtra("boarding_house_name", boarder.getBoardingHouseName());
            intent.putExtra("rent_type", boarder.getRentType());
            intent.putExtra("profile_picture", boarder.getProfilePicture());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);
    }

    private void loadBoardersHistory() {
        // Final check - try to get user_id from SharedPreferences if still 0
        if (userId <= 0 && getContext() != null) {
            android.content.SharedPreferences sharedPreferences = getContext().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
            String userIdString = sharedPreferences.getString("user_id", null);
            if (userIdString != null) {
                try {
                    userId = Integer.parseInt(userIdString);
                    Log.d(TAG, "Got user_id from SharedPreferences in loadBoardersHistory: " + userId);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing user_id from SharedPreferences in loadBoardersHistory", e);
                }
            }
        }
        
        if (userId <= 0) {
            Log.e(TAG, "Invalid user_id: " + userId + ". Cannot load boarders history.");
            showEmptyState();
            return;
        }

        showProgressDialog("Loading boarders history...");

        boarderApiService.getBoardersHistory(userId, new BoarderApiService.BoarderHistoryApiCallback() {
            @Override
            public void onSuccess(List<BoarderHistoryData> history) {
                hideProgressDialog();
                boardersHistory.clear();
                boardersHistory.addAll(history);
                adapter.notifyDataSetChanged();
                updateEmptyState();
                Log.d(TAG, "Loaded " + history.size() + " boarders history");
            }

            @Override
            public void onError(String error) {
                hideProgressDialog();
                Log.e(TAG, "Error loading boarders history: " + error);
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }

    private void updateEmptyState() {
        if (boardersHistory.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
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

    public void refreshBoardersHistory() {
        loadBoardersHistory();
    }
}



































