package com.example.mock;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = NotificationUtils.CHANNEL_ID;
    
    // Track recent notifications to prevent duplicates (stores notification hash)
    private static final Set<String> recentNotifications = new HashSet<>();
    private static final long DUPLICATE_CHECK_WINDOW_MS = 60000; // 60 seconds (increased for better duplicate detection)
    private static final String PREFS_NOTIFICATIONS = "recent_notifications";
    private static final String PREFS_NOTIFICATION_TIMESTAMPS = "notification_timestamps";

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        
        // Save token to SharedPreferences
        saveTokenToPreferences(token);
        
        // Send token to your app server
        sendRegistrationToServer(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "=== onMessageReceived START ===");
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        boolean isForeground = isAppInForeground();
        Log.d(TAG, "App state: " + (isForeground ? "Foreground" : "Background/Closed"));
        Log.d(TAG, "Has notification payload: " + (remoteMessage.getNotification() != null));
        Log.d(TAG, "Data payload size: " + remoteMessage.getData().size());

        // IMPORTANT Firebase behavior:
        // - App FOREGROUND + "notification" payload: onMessageReceived called, Firebase does NOT auto-show → WE show it
        // - App BACKGROUND + "notification" payload: Firebase auto-shows, onMessageReceived also called → WE skip to avoid duplicate
        // - App CLOSED + "notification" payload: Firebase auto-shows, onMessageReceived NOT called → No duplicate
        // - App ANY STATE + "data" only payload: onMessageReceived called, Firebase does NOT show → WE show it
        
        // CRITICAL: If app is in BACKGROUND and message has "notification" payload,
        // Firebase already automatically showed it, so we should NOT show it again to avoid duplicates
        if (!isForeground && remoteMessage.getNotification() != null) {
            Log.d(TAG, "App is in background with notification payload - Firebase already auto-displayed, skipping duplicate");
            return; // Don't show notification again, Firebase already handled it
        }
        
        // Extract message_id FIRST for duplicate detection (most reliable)
        String messageId = null;
        String senderId = null;
        String receiverId = null;
        String messageText = null;
        
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Data payload: " + remoteMessage.getData().toString());
            
            if (remoteMessage.getData().containsKey("message_id")) {
                messageId = remoteMessage.getData().get("message_id");
                Log.d(TAG, "Message ID from data: " + messageId);
            }
            if (remoteMessage.getData().containsKey("sender_id")) {
                senderId = remoteMessage.getData().get("sender_id");
            }
            if (remoteMessage.getData().containsKey("receiver_id")) {
                receiverId = remoteMessage.getData().get("receiver_id");
            }
            if (remoteMessage.getData().containsKey("message_text")) {
                messageText = remoteMessage.getData().get("message_text");
            }
            
            // Create unique identifier for duplicate detection
            String duplicateKey = createDuplicateKey(messageId, senderId, receiverId, messageText);
            Log.d(TAG, "Duplicate key: " + duplicateKey);
            
            // Check for duplicate using persistent storage + in-memory cache
            if (isDuplicateNotification(duplicateKey)) {
                Log.d(TAG, "❌ DUPLICATE notification detected, skipping: " + duplicateKey);
                return; // Skip duplicate notification completely
            }
            
            // Mark as processed immediately
            markNotificationAsProcessed(duplicateKey);
            Log.d(TAG, "✅ Notification marked as processed: " + duplicateKey);
        }
        
        // Check if message contains a data payload (after duplicate check)
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }

        // Extract title and body
        String title = "BoardEase";
        String body = "You have a new notification";
        
        if (remoteMessage.getNotification() != null) {
            // Notification payload exists
            title = remoteMessage.getNotification().getTitle() != null ? 
                    remoteMessage.getNotification().getTitle() : title;
            body = remoteMessage.getNotification().getBody() != null ? 
                   remoteMessage.getNotification().getBody() : body;
            Log.d(TAG, "Message Notification Body: " + body);
        }
        
        // Also check data payload for title/body (in case notification payload is missing)
        if (remoteMessage.getData().size() > 0) {
            if (remoteMessage.getData().containsKey("title")) {
                title = remoteMessage.getData().get("title");
            }
            if (remoteMessage.getData().containsKey("body")) {
                body = remoteMessage.getData().get("body");
            }
        }
        
        // Show notification in notification center (only if not duplicate - already checked above)
        // This works in ALL app states:
        // - App is OPEN/FOREGROUND with "data" only: Show notification
        // - App is in BACKGROUND: Show notification
        // - App is CLOSED with "notification" payload: Firebase automatically shows it (onMessageReceived not called)
        // - App is CLOSED with "data" payload only: Show notification
        Log.d(TAG, "=== Sending notification to center ===");
        sendHeadsUpNotification(title, body, messageId, senderId);
        Log.d(TAG, "=== onMessageReceived END ===");
    }

    private void handleDataMessage(java.util.Map<String, String> data) {
        // Handle data payload (e.g., update UI, refresh badge)
        if (data.containsKey("unread_count")) {
            int unreadCount = Integer.parseInt(data.get("unread_count"));
            Log.d(TAG, "Unread message count received: " + unreadCount);
            
            // Update badge count in SharedPreferences
            SharedPreferences prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            prefs.edit().putInt("unread_count", unreadCount).apply();
            
            // You can broadcast this to your UI components
            Intent updateIntent = new Intent("com.example.mock.UPDATE_BADGE");
            updateIntent.putExtra("unread_count", unreadCount);
            sendBroadcast(updateIntent);
        }
        
        // Handle notification badge updates
        if (data.containsKey("type") && "new_notification".equals(data.get("type"))) {
            Log.d(TAG, "New notification received, updating badge");
            
            // Broadcast to update notification badge
            Intent notificationUpdateIntent = new Intent("com.example.mock.UPDATE_NOTIFICATION_BADGE");
            sendBroadcast(notificationUpdateIntent);
        }
        
        // Handle real-time message updates
        if (data.containsKey("type") && "new_message".equals(data.get("type"))) {
            Log.d(TAG, "New message received for real-time update");
            
            // Broadcast to update conversation in real-time
            Intent messageUpdateIntent = new Intent("com.example.mock.NEW_MESSAGE_RECEIVED");
            messageUpdateIntent.putExtra("message_id", data.get("message_id"));
            messageUpdateIntent.putExtra("sender_id", data.get("sender_id"));
            messageUpdateIntent.putExtra("sender_name", data.get("sender_name"));
            messageUpdateIntent.putExtra("receiver_id", data.get("receiver_id"));
            messageUpdateIntent.putExtra("message_text", data.get("message_text"));
            messageUpdateIntent.putExtra("timestamp", data.get("timestamp"));
            messageUpdateIntent.putExtra("chat_type", data.get("chat_type"));
            messageUpdateIntent.putExtra("group_id", data.get("group_id"));
            sendBroadcast(messageUpdateIntent);
        }
    }

    /**
     * Send notification to notification center
     * This notification will appear in the system notification center
     * regardless of app state (open, background, or closed)
     * Prevents duplicate notifications using content hash
     */
    private void sendHeadsUpNotification(String title, String messageBody) {
        sendHeadsUpNotification(title, messageBody, null, null);
    }
    
    /**
     * Send notification to notification center with message ID
     * Prevents duplicate notifications
     */
    private void sendHeadsUpNotification(String title, String messageBody, String messageId, String senderId) {
        // Duplicate check was already done in onMessageReceived, so we can proceed directly
        Log.d(TAG, "sendHeadsUpNotification called - duplicate already checked");
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        
        // Create notification that appears in notification center
        // Works when app is: OPEN, BACKGROUND, or CLOSED
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setVibrate(new long[]{1000, 1000, 1000}) // Vibrate pattern
                        .setLights(0xFF0000FF, 1000, 1000) // Blue light
                        .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for banner
                        .setDefaults(NotificationCompat.DEFAULT_ALL) // Use default settings
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE) // Message category
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
                        .setContentIntent(pendingIntent)
                        .setOngoing(false) // Not ongoing
                        .setOnlyAlertOnce(false) // Alert every time
                        .setTicker(messageBody) // Ticker text for heads-up
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody)); // Big text style

        // Ensure notification channel exists
        NotificationUtils.createNotificationChannel(this);
        
        // Check if notifications are enabled
        if (!NotificationUtils.canPostNotifications(this)) {
            Log.w(TAG, "Notifications are disabled - cannot show notification in notification center");
            return;
        }
        
        NotificationManager notificationManager = NotificationUtils.getNotificationManager(this);
        if (notificationManager == null) {
            Log.e(TAG, "NotificationManager is null - cannot show notification");
            return;
        }

        // Use a unique ID for each notification
        // CRITICAL: Same message_id = same notification ID = Android replaces old notification
        int notificationId;
        if (messageId != null && !messageId.isEmpty()) {
            // Use message_id if available for consistent notification ID
            // This ensures same message replaces old notification instead of creating duplicate
            notificationId = Math.abs(messageId.hashCode());
            Log.d(TAG, "Using message_id for notification ID: " + notificationId + " (message_id: " + messageId + ")");
        } else {
            // Use hash of title + body + sender for unique ID (if available)
            String uniqueContent = title + messageBody;
            if (senderId != null) {
                uniqueContent += senderId;
            }
            notificationId = Math.abs(uniqueContent.hashCode());
            Log.d(TAG, "Using content hash for notification ID: " + notificationId);
        }
        
        try {
            // This will add the notification to the system notification center
            // It will appear whether the app is open, in background, or closed
        notificationManager.notify(notificationId, notificationBuilder.build());
            Log.d(TAG, "Notification sent to notification center (ID: " + notificationId + 
                  ", app state: " + (isAppInForeground() ? "OPEN" : "BACKGROUND/CLOSED") + 
                  "): " + title + " - " + messageBody);
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException when showing notification - permission may be denied: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification: " + e.getMessage());
        }
    }
    
    /**
     * Create a unique key for duplicate detection
     */
    private String createDuplicateKey(String messageId, String senderId, String receiverId, String messageText) {
        if (messageId != null && !messageId.isEmpty()) {
            return "msg_" + messageId;
        }
        // Fallback: use sender + receiver + message text hash
        if (senderId != null && receiverId != null && messageText != null) {
            return "msg_" + senderId + "_" + receiverId + "_" + Math.abs(messageText.hashCode());
        }
        // Last resort: use timestamp (less reliable)
        return "notif_" + System.currentTimeMillis() / 1000;
    }
    
    /**
     * Check if notification is duplicate using persistent storage
     */
    private boolean isDuplicateNotification(String duplicateKey) {
        // Check in-memory cache first (fast)
        synchronized (recentNotifications) {
            if (recentNotifications.contains(duplicateKey)) {
                Log.d(TAG, "Duplicate found in memory cache: " + duplicateKey);
                return true;
            }
        }
        
        // Check persistent storage (SharedPreferences)
        SharedPreferences prefs = getSharedPreferences(PREFS_NOTIFICATIONS, Context.MODE_PRIVATE);
        SharedPreferences timestamps = getSharedPreferences(PREFS_NOTIFICATION_TIMESTAMPS, Context.MODE_PRIVATE);
        
        long lastTimestamp = timestamps.getLong(duplicateKey, 0);
        long currentTime = System.currentTimeMillis();
        
        if (lastTimestamp > 0 && (currentTime - lastTimestamp) < DUPLICATE_CHECK_WINDOW_MS) {
            Log.d(TAG, "Duplicate found in persistent storage: " + duplicateKey + " (age: " + (currentTime - lastTimestamp) + "ms)");
            return true;
        }
        
        return false;
    }
    
    /**
     * Mark notification as processed
     */
    private void markNotificationAsProcessed(String duplicateKey) {
        long currentTime = System.currentTimeMillis();
        
        // Add to in-memory cache
        synchronized (recentNotifications) {
            recentNotifications.add(duplicateKey);
            
            // Clean up after window expires
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                synchronized (recentNotifications) {
                    recentNotifications.remove(duplicateKey);
                }
            }, DUPLICATE_CHECK_WINDOW_MS);
        }
        
        // Store in persistent storage
        SharedPreferences timestamps = getSharedPreferences(PREFS_NOTIFICATION_TIMESTAMPS, Context.MODE_PRIVATE);
        timestamps.edit().putLong(duplicateKey, currentTime).apply();
        
        // Clean up old entries from persistent storage
        SharedPreferences.Editor editor = timestamps.edit();
        java.util.Map<String, ?> allTimestamps = timestamps.getAll();
        for (java.util.Map.Entry<String, ?> entry : allTimestamps.entrySet()) {
            if (entry.getValue() instanceof Long) {
                long timestamp = (Long) entry.getValue();
                if (currentTime - timestamp > DUPLICATE_CHECK_WINDOW_MS) {
                    editor.remove(entry.getKey());
                }
            }
        }
        editor.apply();
    }
    
    /**
     * Create a unique hash for notification to detect duplicates (legacy method)
     */
    private String createNotificationHash(String title, String body, String messageId) {
        if (messageId != null && !messageId.isEmpty()) {
            return "msg_" + messageId;
        }
        // Use title + body + current second (to allow same notification after 1 second)
        long currentSecond = System.currentTimeMillis() / 1000;
        return "notif_" + title + "_" + body + "_" + currentSecond;
    }
    
    /**
     * Check if the app is currently in the foreground
     */
    private boolean isAppInForeground() {
        android.app.ActivityManager activityManager = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        java.util.List<android.app.ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();
        
        for (android.app.ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
            if (processInfo.processName.equals(getPackageName())) {
                return processInfo.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
            }
        }
        return false;
    }
    
    // Removed createBannerNotification() - using sendHeadsUpNotification() instead to prevent duplicates

    private void saveTokenToPreferences(String token) {
        SharedPreferences prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        prefs.edit().putString("fcm_token", token).apply();
        Log.d(TAG, "FCM token saved to preferences: " + token);
    }

    private void sendRegistrationToServer(String token) {
        Log.d(TAG, "Sending token to server: " + token);
        
        // Get user_id from SharedPreferences (actual logged-in user)
        SharedPreferences prefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "1"); // Use actual user ID, fallback to 1 if not found
        
        // Server URL - update this to match your server path
        String url = "https://boardease.calapebohol.com/register_device_token.php";
        
        // Create request using Volley
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
            response -> {
                Log.d(TAG, "Token sent to server successfully: " + response);
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.getBoolean("success")) {
                        Log.d(TAG, "Device token registered: " + jsonResponse.getString("message"));
                    } else {
                        Log.e(TAG, "Server error: " + jsonResponse.getString("message"));
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing server response: " + e.getMessage());
                }
            },
            error -> {
                Log.e(TAG, "Error sending token to server: " + error.getMessage());
            }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", userId);
                params.put("device_token", token);
                params.put("device_type", "android");
                params.put("app_version", "1.0.0");
                return params;
            }
        };
        
        // Add request to queue
        Volley.newRequestQueue(this).add(stringRequest);
    }
}


























