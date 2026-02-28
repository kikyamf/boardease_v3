package com.example.mock;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChangeRoomRequestsFragment extends Fragment {

    private static final String TAG = "ChangeRoomRequests";
    private static final String ARG_USER_ID = "user_id";
    private RecyclerView recyclerView;
    private ChangeRoomRequestsAdapter adapter;
    private List<ChangeRoomRequestData> requests;
    private RequestQueue requestQueue;
    private int ownerId;
    private TextView tvCount;
    private TextView emptyState;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private ProgressDialog progressDialog;
    private boolean isInitialLoad = true;

    // Real-time polling
    private final android.os.Handler pollingHandler = new android.os.Handler();
    private final Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            loadRequests(false); // silent poll
            pollingHandler.postDelayed(this, 5000);
        }
    };

    private void startPolling() {
        pollingHandler.removeCallbacks(pollingRunnable);
        pollingHandler.postDelayed(pollingRunnable, 5000);
    }

    private void stopPolling() {
        pollingHandler.removeCallbacks(pollingRunnable);
    }

    public static ChangeRoomRequestsFragment newInstance(int ownerId) {
        ChangeRoomRequestsFragment fragment = new ChangeRoomRequestsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, ownerId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_change_room_requests, container, false);

        if (getArguments() != null) {
            ownerId = getArguments().getInt(ARG_USER_ID);
        }

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        tvCount = view.findViewById(R.id.tvCount);
        emptyState = view.findViewById(R.id.emptyState);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        progressBar = view.findViewById(R.id.progressBar);

        requestQueue = Volley.newRequestQueue(getContext());
        requests = new ArrayList<>();

        swipeRefreshLayout.setOnRefreshListener(() -> loadRequests(true));

        loadRequests(true); // initial load shows spinner

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        startPolling();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPolling();
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
    }

    private void loadRequests(boolean isRefresh) {
        if (isRefresh) {
            if (isInitialLoad) {
                isInitialLoad = false;
                progressDialog = new ProgressDialog(getContext());
                progressDialog.setMessage("Loading requests...");
                progressDialog.setCancelable(false);
                progressDialog.show();
            } else {
                swipeRefreshLayout.setRefreshing(true);
            }
        }
        // If polling (isRefresh=false): completely silent

        String url = "https://boardease.calapebohol.com/get_change_room_requests.php?owner_id=" + ownerId;
        
        android.util.Log.d(TAG, "Loading requests from URL: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                    swipeRefreshLayout.setRefreshing(false);
                    android.util.Log.d(TAG, "Response received: " + response.toString());
                    try {
                        if (response.getBoolean("success")) {
                            android.util.Log.d(TAG, "Success = true");
                            JSONArray array = response.getJSONObject("data").getJSONArray("change_room_requests");
                            android.util.Log.d(TAG, "Array length: " + array.length());
                            requests.clear();
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject obj = array.getJSONObject(i);
                                android.util.Log.d(TAG, "Processing request " + i + ": " + obj.toString());
                                requests.add(new ChangeRoomRequestData(
                                        obj.getInt("change_request_id"),
                                        obj.getInt("booking_id"),
                                        obj.getInt("user_id"),
                                        obj.getInt("new_room_id"),
                                        obj.getString("reason"),
                                        obj.getString("details"),
                                        obj.getString("status"),
                                        obj.getString("created_at"),
                                        obj.getString("f_name"),
                                        obj.getString("l_name"),
                                        obj.getString("bh_name"),
                                        obj.getString("old_room_name"),
                                        obj.getString("new_room_name"),
                                        obj.optString("old_room_number", ""),
                                        obj.optString("new_room_number", "")
                                ));
                            }
                            android.util.Log.d(TAG, "Total requests added: " + requests.size());
                            updateUI();
                        } else {
                            android.util.Log.e(TAG, "Success = false, message: " + response.getString("message"));
                            Toast.makeText(getContext(), response.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        android.util.Log.e(TAG, "JSON parsing error: " + e.getMessage());
                        e.printStackTrace();
                    }
                },
                error -> {
                    if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                    swipeRefreshLayout.setRefreshing(false);

                    // Log detailed error information
                    android.util.Log.e(TAG, "Error loading change room requests");
                    if (error.networkResponse != null) {
                        android.util.Log.e(TAG, "Status Code: " + error.networkResponse.statusCode);
                        android.util.Log.e(TAG, "Response Data: " + new String(error.networkResponse.data));
                    } else {
                        android.util.Log.e(TAG, "Network error: " + (error.getMessage() != null ? error.getMessage() : "Unknown error"));
                    }
                    if (error.getCause() != null) {
                        android.util.Log.e(TAG, "Cause: " + error.getCause().getMessage());
                    }
                    
                    Toast.makeText(getContext(), "Error loading requests", Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(request);
    }

    private void updateUI() {
        if (adapter == null) {
            adapter = new ChangeRoomRequestsAdapter(requests, request -> openRequestDetails(request));
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }

        tvCount.setText(String.valueOf(requests.size()));
        emptyState.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openRequestDetails(ChangeRoomRequestData request) {
        Intent intent = new Intent(getContext(), ChangeRoomRequestDetailsActivity.class);
        intent.putExtra("request_id", request.getRequestId());
        intent.putExtra("boarder_name", request.getBoarderName());
        intent.putExtra("bh_name", request.getBhName());
        intent.putExtra("old_room_name", request.getOldRoomName());
        intent.putExtra("new_room_name", request.getNewRoomName());
        intent.putExtra("reason", request.getReason());
        intent.putExtra("details", request.getDetails());
        intent.putExtra("created_at", request.getCreatedAt());
        intent.putExtra("status", request.getStatus());
        intent.putExtra("old_room_number", request.getOldRoomNumber());
        intent.putExtra("new_room_number", request.getNewRoomNumber());
        startActivityForResult(intent, 2001);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2001 && resultCode == android.app.Activity.RESULT_OK) {
            loadRequests(false);
        }
    }


    // Public methods for consistency with other booking fragments
    public void loadIfNeeded() {
        loadRequests(false);
    }

    public void refreshBookings() {
        loadRequests(false);
    }

    // Data class
    public static class ChangeRoomRequestData {
        private int requestId, bookingId, userId, newRoomId;
        private String reason, details, status, createdAt, fName, lName, bhName, oldRoomName, newRoomName, oldRoomNumber, newRoomNumber;

        public ChangeRoomRequestData(int requestId, int bookingId, int userId, int newRoomId, String reason, String details, String status, String createdAt, String fName, String lName, String bhName, String oldRoomName, String newRoomName, String oldRoomNumber, String newRoomNumber) {
            this.requestId = requestId;
            this.bookingId = bookingId;
            this.userId = userId;
            this.newRoomId = newRoomId;
            this.reason = reason;
            this.details = details;
            this.status = status;
            this.createdAt = createdAt;
            this.fName = fName;
            this.lName = lName;
            this.bhName = bhName;
            this.oldRoomName = oldRoomName;
            this.newRoomName = newRoomName;
            this.oldRoomNumber = oldRoomNumber;
            this.newRoomNumber = newRoomNumber;
        }

        public int getRequestId() { return requestId; }
        public String getBoarderName() { return fName + " " + lName; }
        public String getBhName() { return bhName; }
        public String getOldRoomName() { return oldRoomName; }
        public String getNewRoomName() { return newRoomName; }
        public String getOldRoomNumber() { return oldRoomNumber; }
        public String getNewRoomNumber() { return newRoomNumber; }
        public String getReason() { return reason; }
        public String getDetails() { return details; }
        public String getStatus() { return status; }
        public String getCreatedAt() { return createdAt; }
    }
}
