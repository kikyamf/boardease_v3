package com.example.mock;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class BoardersListActivity extends AppCompatActivity {

    private static final String TAG = "BoardersList";

    private int userId;
    private ProgressDialog progressDialog;
    private boolean isLoading = false;

    // Views
    private ImageButton btnBack;
    private TextView tvTitle;
    private LinearLayout tvEmptyState;
    private RecyclerView recyclerView;

    // Data
    private List<BoarderData> boardersList;
    private BoardersListAdapter adapter;
    private BoarderApiService boarderApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boarders_list);

        // Get userId from intent
        userId = getIntent().getIntExtra("user_id", 0);
        Log.d(TAG, "Received user_id from intent: " + userId);

        initViews();
        setupClickListeners();
        loadBoarders();
    }

    private void initViews() {
        // Back button
        btnBack = findViewById(R.id.btnBack);

        // Title
        tvTitle = findViewById(R.id.tvTitle);

        // RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Empty state
        tvEmptyState = findViewById(R.id.tvEmptyState);

        // Initialize API service
        boarderApiService = new BoarderApiService(this);

        // Initialize data
        boardersList = new ArrayList<>();
        adapter = new BoardersListAdapter(boardersList);
        
        // Set up click listener for boarder items
        adapter.setOnBoarderClickListener(boarder -> {
            // Open boarder details activity when a boarder is clicked
            Intent intent = new Intent(BoardersListActivity.this, BoarderStayDetailsActivity.class);
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

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadBoarders() {
        // Prevent multiple simultaneous requests
        if (isLoading) {
            Log.d(TAG, "Already loading boarders, skipping request");
            return;
        }
        
        isLoading = true;
        showProgressDialog("Loading boarders...");

        // Use BoarderApiService to get current boarders (same as boarders rented section)
        boarderApiService.getCurrentBoarders(userId, new BoarderApiService.BoarderApiCallback() {
            @Override
            public void onSuccess(List<BoarderData> boarders) {
                isLoading = false;
                hideProgressDialog();
                Log.d(TAG, "Loaded " + boarders.size() + " boarders");
                
                boardersList.clear();
                boardersList.addAll(boarders);
                adapter.notifyDataSetChanged();
                updateEmptyState();
            }

            @Override
            public void onError(String error) {
                isLoading = false;
                hideProgressDialog();
                Log.e(TAG, "Error loading boarders: " + error);
                Toast.makeText(BoardersListActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
    }

    private void updateEmptyState() {
        if (boardersList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showProgressDialog(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}










