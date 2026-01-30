package com.example.mock;

import android.app.ProgressDialog;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class PendingMaintenanceFragment extends Fragment {

    private static final String TAG = "PendingMaintenance";
    private static final String GET_MAINTENANCE_REQUESTS_URL = "https://boardease.calapebohol.com/get_maintenance_requests.php";
    private static final String UPDATE_MAINTENANCE_STATUS_URL = "https://boardease.calapebohol.com/update_maintenance_status.php";

    private RecyclerView recyclerView;
    private LinearLayout emptyLayout;
    private TextView tvCount;
    private LinearLayout headerLayout;
    private ImageView ivHeaderIcon;
    private SwipeRefreshLayout swipeRefreshLayout;
    private PendingMaintenanceAdapter adapter;
    private ArrayList<MaintenanceRequest> maintenanceRequests;
    private int userId;
    private ProgressDialog progressDialog;

    public static PendingMaintenanceFragment newInstance(int userId) {
        PendingMaintenanceFragment fragment = new PendingMaintenanceFragment();
        Bundle args = new Bundle();
        args.putInt("user_id", userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getInt("user_id");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pending_maintenance, container, false);

        initViews(view);
        setupRecyclerView();
        loadMaintenanceRequests();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyLayout = view.findViewById(R.id.emptyLayout);
        tvCount = view.findViewById(R.id.tvCount);
        headerLayout = view.findViewById(R.id.headerLayout);
        ivHeaderIcon = view.findViewById(R.id.ivHeaderIcon);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        
        // Set header styling (orange for Pending)
        if (headerLayout != null) {
            headerLayout.setBackgroundColor(android.graphics.Color.parseColor("#FFF3E0")); // Light orange
        }
        if (ivHeaderIcon != null) {
            ivHeaderIcon.setColorFilter(android.graphics.Color.parseColor("#FF9800")); // Orange
        }
        if (tvCount != null) {
            tvCount.setBackgroundResource(R.drawable.bg_rounded_orange);
        }
        
        // Setup swipe refresh
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadMaintenanceRequests(true);
            });
        }
    }

    private void setupRecyclerView() {
        maintenanceRequests = new ArrayList<>();
        adapter = new PendingMaintenanceAdapter(maintenanceRequests, getContext(), new PendingMaintenanceAdapter.OnStatusUpdateListener() {
            @Override
            public void onStatusUpdate(int requestId, String newStatus) {
                // Show confirmation dialog before updating
                String action = "Approve";
                String message = "Are you sure you want to approve this maintenance request?";
                if ("Rejected".equals(newStatus) || "Declined".equals(newStatus)) {
                    action = "Reject";
                    message = "Are you sure you want to reject this maintenance request?";
                }
                
                new android.app.AlertDialog.Builder(getContext())
                    .setTitle(action + " Maintenance Request")
                    .setMessage(message)
                    .setPositiveButton("Yes", (dialog, which) -> {
                        updateMaintenanceStatus(requestId, newStatus);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private android.os.Handler pollingHandler = new android.os.Handler();
    private Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            loadMaintenanceRequests(false);
            pollingHandler.postDelayed(this, 5000); // Poll every 5 seconds
        }
    };
    
    private void startPolling() {
        pollingHandler.removeCallbacks(pollingRunnable);
        pollingHandler.postDelayed(pollingRunnable, 5000);
    }
    
    private void stopPolling() {
        pollingHandler.removeCallbacks(pollingRunnable);
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
        hideProgressDialog();
    }

    private void loadMaintenanceRequests() {
        loadMaintenanceRequests(false);
    }
    
    private void loadMaintenanceRequests(boolean isRefresh) {
        if (isRefresh) {
            // Show swipe refresh indicator only (no loading dialog)
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(true);
            }
        } else {
             // Polling update - silent
             // We only show progress dialog on very first load if needed, but here we assume it's either refresh or poll
             // If we wanted to track initial load we could add a flag like in other fragments
        }

        MaintenanceApiService apiService = new MaintenanceApiService(getContext());
        apiService.getMaintenanceRequests(userId, "owner", "pending", "all", "all", new MaintenanceApiService.MaintenanceApiCallback() {
            @Override
            public void onSuccess(List<MaintenanceRequest> requests) {
                if (getContext() == null) return;
                
                // Only hide progress dialog if it was shown (not during refresh)
                if (!isRefresh) {
                    hideProgressDialog();
                }
                // Always hide swipe refresh indicator
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                maintenanceRequests.clear();
                maintenanceRequests.addAll(requests);
                adapter.notifyDataSetChanged();
                updateEmptyState();
                Log.d(TAG, "Loaded " + requests.size() + " pending maintenance requests");
            }

            @Override
            public void onError(String error) {
                if (getContext() == null) return;
                
                // Only hide progress dialog if it was shown (not during refresh)
                if (!isRefresh) {
                    hideProgressDialog();
                }
                // Always hide swipe refresh indicator
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                
                // Only show toast on manual refresh to avoid spam
                if (isRefresh) {
                     Log.e(TAG, "Error loading pending maintenance requests: " + error);
                     Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                }
                updateEmptyState();
            }
        });
    }

    private void updateMaintenanceStatus(int requestId, String newStatus) {
        // Trigger pull-to-refresh instead of showing progress dialog
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }

        MaintenanceApiService apiService = new MaintenanceApiService(getContext());
        apiService.updateMaintenanceStatus(requestId, newStatus, "", "", "", "", "", "", userId, new MaintenanceApiService.SimpleCallback() {
            @Override
            public void onSuccess(String message) {
                // Show success dialog
                String action = "approved";
                String title = "Successfully Approved";
                if ("Rejected".equals(newStatus) || "Declined".equals(newStatus)) {
                    action = "rejected";
                    title = "Successfully Rejected";
                }
                
                new android.app.AlertDialog.Builder(getContext())
                    .setTitle(title)
                    .setMessage("Maintenance request has been " + action + " successfully.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        // Keep refresh indicator showing while reloading
                        loadMaintenanceRequests(true); // Pass true to use swipe refresh indicator
                        // Refresh all fragments in the activity
                        if (getActivity() instanceof MaintenanceRequestsActivity) {
                            ((MaintenanceRequestsActivity) getActivity()).refreshAllFragments();
                        }
                    })
                    .setCancelable(false)
                    .show();
            }

            @Override
            public void onError(String error) {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                Log.e(TAG, "Error updating maintenance status: " + error);
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    public void refreshMaintenanceRequests() {
        // Always use pull-to-refresh when refreshing (no loading dialog)
        loadMaintenanceRequests(true);
    }

    private void updateEmptyState() {
        // Update count
        if (tvCount != null) {
            tvCount.setText(String.valueOf(maintenanceRequests.size()));
        }
        
        if (maintenanceRequests.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
        }
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
}















