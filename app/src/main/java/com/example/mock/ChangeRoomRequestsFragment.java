package com.example.mock;

import android.app.ProgressDialog;
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

        swipeRefreshLayout.setOnRefreshListener(this::loadRequests);

        loadRequests();

        return view;
    }

    private void loadRequests() {
        swipeRefreshLayout.setRefreshing(true);
        String url = "https://boardease.calapebohol.com/get_change_room_requests.php?owner_id=" + ownerId;
        
        android.util.Log.d(TAG, "Loading requests from URL: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
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
                                        obj.getString("new_room_name")
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
            adapter = new ChangeRoomRequestsAdapter(requests, request -> showRequestDetailsDialog(request));
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }

        tvCount.setText(String.valueOf(requests.size()));
        emptyState.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showRequestDetailsDialog(ChangeRoomRequestData request) {
        if (getContext() == null) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_room_request_details, null);
        
        // Bind data to dialog views
        TextView tvBoarderName = dialogView.findViewById(R.id.tvBoarderName);
        TextView tvBhName = dialogView.findViewById(R.id.tvBhName);
        TextView tvOldRoom = dialogView.findViewById(R.id.tvOldRoom);
        TextView tvNewRoom = dialogView.findViewById(R.id.tvNewRoom);
        TextView tvReason = dialogView.findViewById(R.id.tvReason);
        TextView tvDetails = dialogView.findViewById(R.id.tvDetails);
        TextView tvDate = dialogView.findViewById(R.id.tvDate);
        
        tvBoarderName.setText(request.getBoarderName());
        tvBhName.setText(request.getBhName());
        tvOldRoom.setText(request.getOldRoomName());
        tvNewRoom.setText(request.getNewRoomName());
        tvReason.setText(request.getReason());
        tvDetails.setText(request.getDetails());
        tvDate.setText(request.getCreatedAt());
        
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();
        
        // Set up buttons
        dialogView.findViewById(R.id.btnApprove).setOnClickListener(v -> {
            dialog.dismiss();
            processRequest(request, "Approve");
        });
        
        dialogView.findViewById(R.id.btnDecline).setOnClickListener(v -> {
            dialog.dismiss();
            processRequest(request, "Decline");
        });
        
        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void processRequest(ChangeRoomRequestData request, String action) {
        showProgressDialog(action + "ing request...");
        String url = "https://boardease.calapebohol.com/process_change_room_request.php";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    hideProgressDialog();
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            Toast.makeText(getContext(), "Request " + action.toLowerCase() + "d", Toast.LENGTH_SHORT).show();
                            loadRequests();
                        } else {
                            Toast.makeText(getContext(), jsonResponse.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    hideProgressDialog();
                    Toast.makeText(getContext(), "Error processing request", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("change_request_id", String.valueOf(request.getRequestId()));
                params.put("action", action);
                return params;
            }
        };

        requestQueue.add(stringRequest);
    }

    private void showProgressDialog(String message) {
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    // Public methods for consistency with other booking fragments
    public void loadIfNeeded() {
        // Called when tab is selected - delegate to loadRequests
        loadRequests();
    }

    public void refreshBookings() {
        // Called when returning from detail views - delegate to loadRequests
        loadRequests();
    }

    // Data class
    public static class ChangeRoomRequestData {
        private int requestId, bookingId, userId, newRoomId;
        private String reason, details, status, createdAt, fName, lName, bhName, oldRoomName, newRoomName;

        public ChangeRoomRequestData(int requestId, int bookingId, int userId, int newRoomId, String reason, String details, String status, String createdAt, String fName, String lName, String bhName, String oldRoomName, String newRoomName) {
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
        }

        public int getRequestId() { return requestId; }
        public String getBoarderName() { return fName + " " + lName; }
        public String getBhName() { return bhName; }
        public String getOldRoomName() { return oldRoomName; }
        public String getNewRoomName() { return newRoomName; }
        public String getReason() { return reason; }
        public String getDetails() { return details; }
        public String getCreatedAt() { return createdAt; }
    }
}
