package com.example.mock;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MaintenanceFragment extends Fragment {

    private static final String TAG = "MaintenanceFragment";
    private static final String API_URL = "https://boardease.calapebohol.com/get_maintenance_logs.php";

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private MaintenanceAdapter adapter;
    private List<MaintenanceLog> maintenanceLogs;
    private int userId;
    private ProgressDialog progressDialog;
    private boolean isLoading = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maintenance, container, false);
        
        // Get user_id from arguments
        if (getArguments() != null) {
            userId = getArguments().getInt("user_id", 0);
        }
        
        // Fallback to SharedPreferences if userId is still 0
        if (userId <= 0 && getContext() != null) {
            SharedPreferences sharedPreferences = getContext().getSharedPreferences("UserSession", android.content.Context.MODE_PRIVATE);
            String userIdString = sharedPreferences.getString("user_id", "0");
            try {
                userId = Integer.parseInt(userIdString);
            } catch (NumberFormatException e) {
                userId = 0;
            }
        }
        
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyState = view.findViewById(R.id.emptyState);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        maintenanceLogs = new ArrayList<>();
        adapter = new MaintenanceAdapter(maintenanceLogs, this::onMaintenanceClick);
        recyclerView.setAdapter(adapter);
        
        // Load real maintenance data
        loadMaintenanceData();
        
        return view;
    }

    private void loadMaintenanceData() {
        if (isLoading || userId <= 0) {
            return;
        }

        isLoading = true;
        showProgressDialog("Loading maintenance requests...");

        RequestQueue queue = Volley.newRequestQueue(getContext());
        
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("user_id", userId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, API_URL, jsonBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        hideProgressDialog();
                        isLoading = false;
                        Log.d(TAG, "API Response: " + response.toString());
                        
                        try {
                            if (response.getBoolean("success")) {
                                JSONArray maintenanceArray = response.getJSONArray("maintenance");
                                maintenanceLogs.clear();
                                
                                for (int i = 0; i < maintenanceArray.length(); i++) {
                                    JSONObject maintenanceObj = maintenanceArray.getJSONObject(i);
                                    
                                    // Format created date for display
                                    String createdDate = maintenanceObj.getString("created_at");
                                    String formattedDate = formatDateTime(createdDate);
                                    
                                    MaintenanceLog log = new MaintenanceLog(
                                            maintenanceObj.getString("boarding_house_name"),
                                            maintenanceObj.getString("issue_type"),
                                            formattedDate,
                                            maintenanceObj.getString("status"),
                                            maintenanceObj.optString("description", ""),
                                            maintenanceObj.optString("boarding_house_image", "")
                                    );
                                    maintenanceLogs.add(log);
                                }
                                
                                adapter.notifyDataSetChanged();
                                
                                if (maintenanceLogs.isEmpty()) {
                                    showEmptyState();
                                } else {
                                    hideEmptyState();
                                }
                            } else {
                                String error = response.optString("error", "Failed to load maintenance requests");
                                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                                showEmptyState();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing response", e);
                            Toast.makeText(getContext(), "Error parsing response", Toast.LENGTH_SHORT).show();
                            showEmptyState();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        hideProgressDialog();
                        isLoading = false;
                        Log.e(TAG, "Volley Error: " + error.getMessage(), error);
                        Toast.makeText(getContext(), "Error loading maintenance requests", Toast.LENGTH_SHORT).show();
                        showEmptyState();
                    }
                });

        queue.add(request);
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null && getContext() != null) {
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setCancelable(false);
        }
        if (progressDialog != null) {
            progressDialog.setMessage(message);
            progressDialog.show();
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showEmptyState() {
        if (recyclerView != null) recyclerView.setVisibility(View.GONE);
        if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
        if (emptyState != null) emptyState.setVisibility(View.GONE);
    }
    
    private String formatDateTime(String dateTime) {
        // Format from "YYYY-MM-DD HH:MM:SS" to "MMM DD, YYYY hh:mm a" (e.g., "Jan 15, 2025 02:00 PM")
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault());
            java.util.Date date = inputFormat.parse(dateTime);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateTime; // Return original if parsing fails
        }
    }

    private void onMaintenanceClick(MaintenanceLog maintenance) {
        // Handle maintenance click - could open details dialog or activity
    }

    // Data class for maintenance logs
    public static class MaintenanceLog {
        private String propertyName;
        private String issueType;
        private String timestamp;
        private String status;
        private String description;
        private String imageUrl;

        public MaintenanceLog(String propertyName, String issueType, String timestamp, String status, String description, String imageUrl) {
            this.propertyName = propertyName;
            this.issueType = issueType;
            this.timestamp = timestamp;
            this.status = status;
            this.description = description;
            this.imageUrl = imageUrl;
        }

        // Getters
        public String getPropertyName() { return propertyName; }
        public String getIssueType() { return issueType; }
        public String getTimestamp() { return timestamp; }
        public String getStatus() { return status; }
        public String getDescription() { return description; }
        public String getImageUrl() { return imageUrl; }
    }
}
