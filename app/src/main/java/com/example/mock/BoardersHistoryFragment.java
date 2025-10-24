package com.example.mock;

import android.app.ProgressDialog;
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

    private RecyclerView recyclerView;
    private TextView emptyState;
    private ProgressDialog progressDialog;
    private BoardersHistoryAdapter adapter;
    private List<BoarderHistoryData> boardersHistory;
    private BoarderApiService boarderApiService;
    private int userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_boarders_history, container, false);

        initViews(view);
        setupRecyclerView();
        loadBoardersHistory();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyState = view.findViewById(R.id.emptyState);
        
        // Get userId from ActivityDetailsActivity
        if (getActivity() != null) {
            userId = getActivity().getIntent().getIntExtra("user_id", 0);
            Log.d(TAG, "Received user_id: " + userId);
        }
        
        boarderApiService = new BoarderApiService(getContext());
        boardersHistory = new ArrayList<>();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BoardersHistoryAdapter(boardersHistory);
        recyclerView.setAdapter(adapter);
    }

    private void loadBoardersHistory() {
        if (userId <= 0) {
            Log.e(TAG, "Invalid user_id: " + userId);
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
































