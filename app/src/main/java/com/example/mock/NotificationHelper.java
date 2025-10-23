package com.example.mock;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class NotificationHelper {
    
    private static final String TAG = "NotificationHelper";
    private static final String BASE_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/";
    
    private Context context;
    private RequestQueue requestQueue;
    private SharedPreferences sharedPreferences;
    
    public NotificationHelper(Context context) {
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(context);
        this.sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
    }
    
    // Create notification for booking activities
    public void notifyBookingActivity(int userId, String activity, String details) {
        String title = "";
        String message = "";
        
        switch (activity) {
            case "new_booking":
                title = "New Booking Request";
                message = "You have a new booking request: " + details;
                break;
            case "booking_approved":
                title = "Booking Approved";
                message = "Your booking request has been approved: " + details;
                break;
            case "booking_rejected":
                title = "Booking Rejected";
                message = "Your booking request has been rejected: " + details;
                break;
            case "booking_cancelled":
                title = "Booking Cancelled";
                message = "Booking has been cancelled: " + details;
                break;
        }
        
        createNotification(userId, title, message, "booking");
    }
    
    // Create notification for payment activities
    public void notifyPaymentActivity(int userId, String activity, String details, double amount) {
        String title = "";
        String message = "";
        
        switch (activity) {
            case "payment_received":
                title = "Payment Received";
                message = "Payment of ₱" + String.format("%.2f", amount) + " received: " + details;
                break;
            case "payment_overdue":
                title = "Payment Overdue";
                message = "Payment of ₱" + String.format("%.2f", amount) + " is overdue: " + details;
                break;
            case "payment_reminder":
                title = "Payment Reminder";
                message = "Reminder: Payment of ₱" + String.format("%.2f", amount) + " due soon: " + details;
                break;
            case "payment_failed":
                title = "Payment Failed";
                message = "Payment of ₱" + String.format("%.2f", amount) + " failed: " + details;
                break;
        }
        
        createNotification(userId, title, message, "payment");
    }
    
    // Create notification for maintenance activities
    public void notifyMaintenanceActivity(int userId, String activity, String details) {
        String title = "";
        String message = "";
        
        switch (activity) {
            case "maintenance_scheduled":
                title = "Maintenance Scheduled";
                message = "Maintenance scheduled: " + details;
                break;
            case "maintenance_completed":
                title = "Maintenance Completed";
                message = "Maintenance completed: " + details;
                break;
            case "maintenance_cancelled":
                title = "Maintenance Cancelled";
                message = "Maintenance cancelled: " + details;
                break;
            case "maintenance_reminder":
                title = "Maintenance Reminder";
                message = "Reminder: Maintenance scheduled: " + details;
                break;
        }
        
        createNotification(userId, title, message, "maintenance");
    }
    
    // Create notification for announcements
    public void notifyAnnouncement(int userId, String title, String message, boolean isUrgent) {
        String finalTitle = isUrgent ? "🚨 URGENT: " + title : title;
        createNotification(userId, finalTitle, message, "announcement");
    }
    
    // Create general notification
    public void notifyGeneral(int userId, String title, String message) {
        createNotification(userId, title, message, "general");
    }
    
    // Core method to create notification
    private void createNotification(int userId, String title, String message, String type) {
        String url = BASE_URL + "create_notification.php";
        
        JSONObject params = new JSONObject();
        try {
            params.put("user_id", userId);
            params.put("notif_title", title);
            params.put("notif_message", message);
            params.put("notif_type", type);
            params.put("send_fcm", true);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating notification params", e);
            return;
        }
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getBoolean("success")) {
                                Log.d(TAG, "Notification created successfully: " + title);
                            } else {
                                Log.e(TAG, "Failed to create notification: " + response.getString("message"));
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing notification response", e);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Network error creating notification: " + error.getMessage());
                    }
                });
        
        requestQueue.add(request);
    }
    
    // Get unread notification count
    public void getUnreadCount(int userId, UnreadCountCallback callback) {
        String url = BASE_URL + "get_unread_notif_count.php?user_id=" + userId;
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getBoolean("success")) {
                                JSONObject data = response.getJSONObject("data");
                                int totalUnread = data.getInt("total_unread");
                                callback.onSuccess(totalUnread);
                            } else {
                                callback.onError("Failed to get unread count");
                            }
                        } catch (JSONException e) {
                            callback.onError("Error parsing unread count response");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        callback.onError("Network error: " + error.getMessage());
                    }
                });
        
        requestQueue.add(request);
    }
    
    // Interface for unread count callback
    public interface UnreadCountCallback {
        void onSuccess(int unreadCount);
        void onError(String error);
    }
    
    // Mark all notifications as read
    public void markAllAsRead(int userId) {
        String url = BASE_URL + "mark_all_notifications_read.php";
        
        JSONObject params = new JSONObject();
        try {
            params.put("user_id", userId);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating mark all read params", e);
            return;
        }
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if (response.getBoolean("success")) {
                                Log.d(TAG, "All notifications marked as read");
                            } else {
                                Log.e(TAG, "Failed to mark all as read: " + response.getString("message"));
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing mark all read response", e);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Network error marking all as read: " + error.getMessage());
                    }
                });
        
        requestQueue.add(request);
    }
}









