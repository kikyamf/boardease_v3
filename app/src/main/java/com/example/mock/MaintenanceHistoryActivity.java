package com.example.mock;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MaintenanceHistoryActivity extends AppCompatActivity {
    
    // Views
    private RecyclerView recyclerViewMaintenance;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout layoutEmpty;
    private ImageView ivBack;
    private Spinner spinnerFilter;
    
    private BoarderMaintenanceHistoryAdapter adapter;
    private List<MaintenanceHistoryItem> maintenanceList;
    private List<MaintenanceHistoryItem> filteredMaintenanceList;
    private RequestQueue requestQueue;
    private ProgressDialog progressDialog;
    private int userId;
    
    private String currentFilter = "all";
    private boolean isFromPullRefresh = false;
    
    private static final String BASE_URL = "https://boardease.calapebohol.com/";
    private static final String TAG = "MaintenanceHistoryActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintenance_history);
        
        // Get user ID from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        String userIdString = sharedPreferences.getString("user_id", "0");
        try {
            userId = Integer.parseInt(userIdString);
        } catch (NumberFormatException e) {
            userId = 0;
        }
        
        if (userId <= 0) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initializeViews();
        setupClickListeners();
        setupSpinners();
        
        // Initialize request queue
        requestQueue = Volley.newRequestQueue(this);
        
        // Initialize maintenance list
        maintenanceList = new ArrayList<>();
        filteredMaintenanceList = new ArrayList<>();
        adapter = new BoarderMaintenanceHistoryAdapter(filteredMaintenanceList);
        
        // Setup RecyclerView - now it handles scrolling itself (no ScrollView)
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerViewMaintenance.setLayoutManager(layoutManager);
        recyclerViewMaintenance.setHasFixedSize(false);
        recyclerViewMaintenance.setAdapter(adapter);
        
        // Load maintenance history
        loadMaintenanceHistory();
    }
    
    private void initializeViews() {
        recyclerViewMaintenance = findViewById(R.id.recyclerViewMaintenance);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        layoutEmpty = findViewById(R.id.tvEmpty);
        ivBack = findViewById(R.id.ivBack);
        spinnerFilter = findViewById(R.id.spinnerFilter);
    }
    
    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
        
        swipeRefreshLayout.setOnRefreshListener(() -> {
            isFromPullRefresh = true;
            loadMaintenanceHistory();
        });
    }
    
    private void setupSpinners() {
        // Filter options: All, Pending, In Progress, Resolved, Declined
        String[] filterOptions = {"All Requests", "Pending", "In Progress", "Resolved", "Declined"};
        android.widget.ArrayAdapter<String> filterAdapter = new android.widget.ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, filterOptions) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(android.graphics.Color.WHITE);
                return view;
            }
            
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(android.graphics.Color.WHITE);
                return view;
            }
        };
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(filterAdapter);
        
        // Automatic filtering when spinner selection changes
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newFilter = "all";
                switch (position) {
                    case 0: newFilter = "all"; break;
                    case 1: newFilter = "pending"; break;
                    case 2: newFilter = "in_progress"; break;
                    case 3: newFilter = "resolved"; break;
                    case 4: newFilter = "declined"; break;
                }
                
                // Only reload if filter changed
                if (!newFilter.equals(currentFilter)) {
                    currentFilter = newFilter;
                    loadMaintenanceHistory();
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    
    private void loadMaintenanceHistory() {
        // Only show loading dialog if NOT from pull refresh
        if (!isFromPullRefresh) {
            showProgressDialog("Loading maintenance history...");
        }
        
        String url = BASE_URL + "get_boarder_maintenance_history.php";
        
        JSONObject params = new JSONObject();
        try {
            params.put("user_id", userId);
            params.put("filter_status", currentFilter);
            params.put("sort_by", "newest"); // Default to newest
        } catch (JSONException e) {
            hideProgressDialog();
            swipeRefreshLayout.setRefreshing(false);
            isFromPullRefresh = false;
            Toast.makeText(this, "Error creating request", Toast.LENGTH_SHORT).show();
            return;
        }
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    hideProgressDialog();
                    swipeRefreshLayout.setRefreshing(false);
                    isFromPullRefresh = false;
                    try {
                        if (response.getBoolean("success")) {
                            // Parse maintenance requests
                            JSONArray maintenanceArray = response.getJSONArray("maintenance_requests");
                            parseMaintenanceHistory(maintenanceArray);
                        } else {
                            String error = response.optString("error", "Unknown error");
                            Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
                            showEmptyState();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        showEmptyState();
                    }
                },
                error -> {
                    hideProgressDialog();
                    swipeRefreshLayout.setRefreshing(false);
                    isFromPullRefresh = false;
                    Toast.makeText(this, "Error loading maintenance history: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmptyState();
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("ngrok-skip-browser-warning", "true");
                return headers;
            }
        };
        
        requestQueue.add(request);
    }
    
    private void parseMaintenanceHistory(JSONArray maintenanceArray) {
        maintenanceList.clear();
        filteredMaintenanceList.clear();
        
        try {
            int arrayLength = maintenanceArray.length();
            android.util.Log.d(TAG, "Parsing " + arrayLength + " maintenance requests with filter: " + currentFilter);
            
            if (arrayLength == 0) {
                android.util.Log.w(TAG, "No maintenance requests found in response");
                showEmptyState();
                return;
            }
            
            for (int i = 0; i < arrayLength; i++) {
                try {
                    JSONObject maintenanceObj = maintenanceArray.getJSONObject(i);
                    
                    MaintenanceHistoryItem item = new MaintenanceHistoryItem();
                    item.setRequestId(maintenanceObj.optInt("request_id", 0));
                    item.setSubject(maintenanceObj.optString("subject", ""));
                    item.setAreaForMaintenance(maintenanceObj.optString("area_for_maintenance", ""));
                    item.setDescription(maintenanceObj.optString("description", ""));
                    item.setStatus(maintenanceObj.optString("status", "Pending"));
                    item.setCreatedAt(maintenanceObj.optString("created_at", ""));
                    item.setFormattedCreatedDate(formatDate(maintenanceObj.optString("created_at", "")));
                    item.setApprovedAt(maintenanceObj.optString("approved_at", ""));
                    item.setFormattedApprovedDate(formatDate(maintenanceObj.optString("approved_at", "")));
                    item.setCompletedAt(maintenanceObj.optString("completed_at", ""));
                    item.setFormattedCompletedDate(formatDate(maintenanceObj.optString("completed_at", "")));
                    item.setRoomInfo(maintenanceObj.optString("room_info", ""));
                    item.setRoomName(maintenanceObj.optString("room_name", ""));
                    item.setRoomNumber(maintenanceObj.optString("room_number", ""));
                    item.setBoardingHouseName(maintenanceObj.optString("boarding_house_name", ""));
                    item.setBhImageUrl(maintenanceObj.optString("bh_image_url", ""));
                    
                    maintenanceList.add(item);
                    // Add directly to filtered list since server already filtered
                    filteredMaintenanceList.add(item);
                    android.util.Log.d(TAG, "Added maintenance #" + (i + 1) + ": request_id=" + item.getRequestId() + ", status=" + item.getStatus());
                } catch (JSONException e) {
                    android.util.Log.e(TAG, "Error parsing maintenance item at index " + i, e);
                    // Continue with next item instead of stopping
                }
            }
            
            android.util.Log.d(TAG, "Total maintenance requests parsed: " + maintenanceList.size() + ", filtered: " + filteredMaintenanceList.size());
            
            // Sort by newest first
            Collections.sort(filteredMaintenanceList, new Comparator<MaintenanceHistoryItem>() {
                @Override
                public int compare(MaintenanceHistoryItem a, MaintenanceHistoryItem b) {
                    String dateA = a.getCreatedAt() != null ? a.getCreatedAt() : "";
                    String dateB = b.getCreatedAt() != null ? b.getCreatedAt() : "";
                    return dateB.compareTo(dateA); // Newest first
                }
            });
            
            // Update adapter - ensure it's using the updated list
            if (adapter != null) {
                // Update the adapter's list reference to ensure it has the latest data
                adapter.updateList(filteredMaintenanceList);
                android.util.Log.d(TAG, "Adapter updated with " + filteredMaintenanceList.size() + " items, adapter item count: " + adapter.getItemCount());
            } else {
                android.util.Log.e(TAG, "Adapter is null! Recreating...");
                adapter = new BoarderMaintenanceHistoryAdapter(filteredMaintenanceList);
                recyclerViewMaintenance.setAdapter(adapter);
            }
            
            // RecyclerView now handles scrolling itself (no ScrollView), so all items should be accessible
            // Just notify the adapter of the data change
            recyclerViewMaintenance.post(new Runnable() {
                @Override
                public void run() {
                    if (adapter != null) {
                        android.util.Log.d(TAG, "RecyclerView updated - Item count: " + adapter.getItemCount());
                        adapter.notifyDataSetChanged();
                    }
                }
            });
            
            if (filteredMaintenanceList.isEmpty()) {
                showEmptyState();
            } else {
                hideEmptyState();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e(TAG, "Error parsing maintenance data", e);
            Toast.makeText(this, "Error parsing maintenance data", Toast.LENGTH_SHORT).show();
            showEmptyState();
        }
    }
    
    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty() || dateString.equals("null")) {
            return "";
        }
        
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
            
            Date date = inputFormat.parse(dateString);
            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (ParseException e) {
            // Try alternative format
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                
                Date date = inputFormat.parse(dateString);
                if (date != null) {
                    return outputFormat.format(date);
                }
            } catch (ParseException e2) {
                // Return original string if parsing fails
            }
        }
        
        return dateString;
    }
    
    private void showEmptyState() {
        layoutEmpty.setVisibility(View.VISIBLE);
        recyclerViewMaintenance.setVisibility(View.GONE);
    }
    
    private void hideEmptyState() {
        layoutEmpty.setVisibility(View.GONE);
        recyclerViewMaintenance.setVisibility(View.VISIBLE);
    }
    
    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
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
    
    public static class MaintenanceHistoryItem implements java.io.Serializable {
        private int requestId;
        private String subject;
        private String areaForMaintenance;
        private String description;
        private String status;
        private String createdAt;
        private String formattedCreatedDate;
        private String approvedAt;
        private String formattedApprovedDate;
        private String completedAt;
        private String formattedCompletedDate;
        private String roomInfo;
        private String roomName;
        private String roomNumber;
        private String boardingHouseName;
        private String bhImageUrl;
        
        // Getters
        public int getRequestId() { return requestId; }
        public String getSubject() { return subject; }
        public String getAreaForMaintenance() { return areaForMaintenance; }
        public String getDescription() { return description; }
        public String getStatus() { return status; }
        public String getCreatedAt() { return createdAt; }
        public String getFormattedCreatedDate() { return formattedCreatedDate; }
        public String getApprovedAt() { return approvedAt; }
        public String getFormattedApprovedDate() { return formattedApprovedDate; }
        public String getCompletedAt() { return completedAt; }
        public String getFormattedCompletedDate() { return formattedCompletedDate; }
        public String getRoomInfo() { return roomInfo; }
        public String getRoomName() { return roomName; }
        public String getRoomNumber() { return roomNumber; }
        public String getBoardingHouseName() { return boardingHouseName; }
        public String getBhImageUrl() { return bhImageUrl; }
        
        // Setters
        public void setRequestId(int requestId) { this.requestId = requestId; }
        public void setSubject(String subject) { this.subject = subject; }
        public void setAreaForMaintenance(String areaForMaintenance) { this.areaForMaintenance = areaForMaintenance; }
        public void setDescription(String description) { this.description = description; }
        public void setStatus(String status) { this.status = status; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public void setFormattedCreatedDate(String formattedCreatedDate) { this.formattedCreatedDate = formattedCreatedDate; }
        public void setApprovedAt(String approvedAt) { this.approvedAt = approvedAt; }
        public void setFormattedApprovedDate(String formattedApprovedDate) { this.formattedApprovedDate = formattedApprovedDate; }
        public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
        public void setFormattedCompletedDate(String formattedCompletedDate) { this.formattedCompletedDate = formattedCompletedDate; }
        public void setRoomInfo(String roomInfo) { this.roomInfo = roomInfo; }
        public void setRoomName(String roomName) { this.roomName = roomName; }
        public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
        public void setBoardingHouseName(String boardingHouseName) { this.boardingHouseName = boardingHouseName; }
        public void setBhImageUrl(String bhImageUrl) { this.bhImageUrl = bhImageUrl; }
    }
}

