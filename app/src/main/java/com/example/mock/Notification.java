package com.example.mock;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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

public class Notification extends AppCompatActivity implements NotificationsAdapter.OnNotificationClickListener {

    private RecyclerView recyclerNotifications;
    private NotificationsAdapter notificationsAdapter;
    private ImageButton btnBack;
    private List<NotificationItemModel> notifList;
    private LinearLayout emptyLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressDialog progressDialog;
    private RequestQueue requestQueue;
    private SharedPreferences sharedPreferences;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // Initialize components
        initializeViews();
        initializeData();
        setupRecyclerView();
        loadNotifications();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        recyclerNotifications = findViewById(R.id.rvNotifications);
        emptyLayout = findViewById(R.id.emptyLayout);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        
        // Initialize request queue
        requestQueue = Volley.newRequestQueue(this);
        
        // Get user ID from shared preferences
        sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        
        // Handle both String and Integer types for user_id
        try {
            currentUserId = sharedPreferences.getInt("user_id", 1);
        } catch (ClassCastException e) {
            // If stored as String, convert to int
            String userIdString = sharedPreferences.getString("user_id", "1");
            try {
                currentUserId = Integer.parseInt(userIdString);
            } catch (NumberFormatException ex) {
                currentUserId = 1; // Default fallback
            }
        }
    }

    private void initializeData() {
        notifList = new ArrayList<>();

        // Back button action
        btnBack.setOnClickListener(v -> finish());
        // Setup swipe refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadNotifications);
    }

    private void setupRecyclerView() {
        recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));
        notificationsAdapter = new NotificationsAdapter(notifList);
        notificationsAdapter.setOnNotificationClickListener(this);
        recyclerNotifications.setAdapter(notificationsAdapter);
    }

    private void loadNotifications() {
        // Only show progress dialog if not refreshing
        if (!swipeRefreshLayout.isRefreshing()) {
            showProgressDialog("Loading notifications...");
        }
        
        String url = "http://192.168.101.6/BoardEase2/get_notifications.php?user_id=" + currentUserId;
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        hideProgressDialog();
                        swipeRefreshLayout.setRefreshing(false);
                        
                        try {
                            if (response.getBoolean("success")) {
                                JSONObject data = response.getJSONObject("data");
                                JSONArray notifications = data.getJSONArray("notifications");
                                
                                android.util.Log.d("Notification", "Received " + notifications.length() + " notifications from server");
                                
                                notifList.clear();
                                
                                // Add notifications with date headers
                                addNotificationsWithHeaders(notifications);
                                
                                android.util.Log.d("Notification", "After processing, notifList size: " + notifList.size());
                                
                                // Update UI
                                updateUI();
                                
                            } else {
                                String errorMsg = response.getString("message");
                                showToast("Failed to load notifications: " + errorMsg);
                                
                                // Show debug info if available
                                if (response.has("debug_info")) {
                                    JSONObject debugInfo = response.getJSONObject("debug_info");
                                    showToast("Debug: User ID = " + debugInfo.getString("received_user_id"));
                                }
                            }
                        } catch (JSONException e) {
                            showToast("Error parsing notifications: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        hideProgressDialog();
                        swipeRefreshLayout.setRefreshing(false);
                        showToast("Network error: " + error.getMessage());
                        error.printStackTrace();
                    }
                });
        
        requestQueue.add(request);
    }

    private void addNotificationsWithHeaders(JSONArray notifications) throws JSONException {
        String currentDate = "";
        
        for (int i = 0; i < notifications.length(); i++) {
            JSONObject notif = notifications.getJSONObject(i);
            
            String createdAt = notif.getString("notif_created_at");
            String notificationDate = getDateFromTimestamp(createdAt);
            
            // Add date header if date changed
            if (!notificationDate.equals(currentDate)) {
                notifList.add(new NotificationItemModel(notificationDate, true));
                currentDate = notificationDate;
            }
            
            // Add notification
            NotificationItemModel notification = new NotificationItemModel(
                    notif.getInt("notif_id"),
                    notif.getString("notif_title"),
                    notif.getString("notif_message"),
                    notif.getString("notif_type"),
                    notif.getString("notif_status"),
                    createdAt
            );
            
            notifList.add(notification);
        }
    }

    private String getDateFromTimestamp(String timestamp) {
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy");
            java.util.Date date = inputFormat.parse(timestamp);
            
            java.util.Calendar cal = java.util.Calendar.getInstance();
            java.util.Calendar today = java.util.Calendar.getInstance();
            java.util.Calendar yesterday = java.util.Calendar.getInstance();
            yesterday.add(java.util.Calendar.DAY_OF_MONTH, -1);
            
            if (isSameDay(date, today.getTime())) {
                return "Today";
            } else if (isSameDay(date, yesterday.getTime())) {
                return "Yesterday";
            } else {
                return outputFormat.format(date);
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private boolean isSameDay(java.util.Date date1, java.util.Date date2) {
        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
               cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR);
    }

    private void updateUI() {
        notificationsAdapter.notifyDataSetChanged();
        
        if (notifList.isEmpty()) {
            // Show empty state
            recyclerNotifications.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
            android.util.Log.d("Notification", "Showing empty state - no notifications found");
        } else {
            // Show notifications list
            recyclerNotifications.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
            android.util.Log.d("Notification", "Showing notifications list - count: " + notifList.size());
        }
    }

    @Override
    public void onNotificationClick(NotificationItemModel notification) {
        // Note: All notifications are already marked as read when opening the activity
        // Handle notification click based on type
        switch (notification.getType()) {
            case "booking":
                // Navigate to booking details
                showToast("Opening booking details...");
                break;
            case "payment":
                // Navigate to payment details
                showToast("Opening payment details...");
                break;
            case "maintenance":
                // Navigate to maintenance details
                showToast("Opening maintenance details...");
                break;
            case "announcement":
                // Show announcement details
                showToast("Opening announcement...");
                break;
            default:
                showToast("Notification: " + notification.getTitle());
                break;
        }
    }

    private void markNotificationAsRead(int notifId) {
        String url = "http://192.168.101.6/BoardEase2/mark_notification_read.php";
        
        JSONObject params = new JSONObject();
        try {
            params.put("notif_id", notifId);
            params.put("user_id", currentUserId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Refresh notifications to update read status
                        loadNotifications();
                        
                        // Send broadcast to update notification badge
                        Intent badgeUpdateIntent = new Intent("com.example.mock.UPDATE_NOTIFICATION_BADGE");
                        sendBroadcast(badgeUpdateIntent);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Silent fail for read status
                    }
                });
        
        requestQueue.add(request);
    }

    private void showProgressDialog(String message) {
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(message);
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideProgressDialog() {
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            progressDialog = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void markAllNotificationsAsRead() {
        android.util.Log.d("NotificationBadge", "markAllNotificationsAsRead() called for user: " + currentUserId);
        android.util.Log.d("NotificationBadge", "SharedPreferences user_id: " + sharedPreferences.getString("user_id", "1"));
        String url = "http://192.168.101.6/BoardEase2/mark_all_notifications_read.php";
        
        JSONObject params = new JSONObject();
        try {
            params.put("user_id", currentUserId);
            android.util.Log.d("NotificationBadge", "Sending user_id: " + currentUserId + " in JSON params");
            android.util.Log.d("NotificationBadge", "Complete JSON params: " + params.toString());
        } catch (JSONException e) {
            android.util.Log.e("NotificationBadge", "Error creating JSON params", e);
            e.printStackTrace();
        }
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            android.util.Log.d("NotificationBadge", "markAllNotificationsAsRead API Response: " + response.toString());
                            if (response.getBoolean("success")) {
                                android.util.Log.d("NotificationBadge", "All notifications marked as read successfully");
                                
                                // Send broadcast to update notification badge
                                Intent badgeUpdateIntent = new Intent("com.example.mock.UPDATE_NOTIFICATION_BADGE");
                                sendBroadcast(badgeUpdateIntent);
                                android.util.Log.d("NotificationBadge", "Sent badge update broadcast");
                            } else {
                                android.util.Log.d("NotificationBadge", "Failed to mark notifications as read");
                            }
                        } catch (JSONException e) {
                            android.util.Log.e("NotificationBadge", "Error parsing mark all as read response", e);
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        android.util.Log.e("NotificationBadge", "Error marking all notifications as read", error);
                    }
                }
        );
        
        // Add request to queue
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        android.util.Log.d("NotificationBadge", "Notification activity onResume() called");
        // Mark all notifications as read when opening the activity
        markAllNotificationsAsRead();
        // Refresh notifications when returning to this activity
        loadNotifications();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        hideProgressDialog();
        if (requestQueue != null) {
            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                @Override
                public boolean apply(Request<?> request) {
                    return true;
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Hide progress dialog when activity is paused
        hideProgressDialog();
    }
}

