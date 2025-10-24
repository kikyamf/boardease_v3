package com.example.mock;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    private RecyclerView recyclerView;
    private LinearLayout tvNoActiveRentals;
    private ProgressDialog progressDialog;
    private BoardersListAdapter adapter;
    private List<BoarderData> currentBoarders;
    private BoarderApiService boarderApiService;
    private int userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_current_boarders, container, false);

        initViews(view);
        setupRecyclerView();
        loadCurrentBoarders();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        tvNoActiveRentals = view.findViewById(R.id.tvNoActiveRentals);
        
        // Get userId from ActivityDetailsActivity
        if (getActivity() != null) {
            userId = getActivity().getIntent().getIntExtra("user_id", 0);
            Log.d(TAG, "Received user_id: " + userId);
        }
        
        boarderApiService = new BoarderApiService(getContext());
        currentBoarders = new ArrayList<>();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BoardersListAdapter(currentBoarders);
        recyclerView.setAdapter(adapter);
    }

    private void loadCurrentBoarders() {
        if (userId <= 0) {
            Log.e(TAG, "Invalid user_id: " + userId);
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
        if (currentBoarders.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvNoActiveRentals.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvNoActiveRentals.setVisibility(View.GONE);
        }
    }

    private void showEmptyState() {
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
































