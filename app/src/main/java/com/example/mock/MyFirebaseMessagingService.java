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
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "default_channel";

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
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }

        // Always show notification, even when app is in foreground
        String title = "New Message";
        String body = "You have a new message";
        
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
            Log.d(TAG, "Message Notification Body: " + body);
        } else if (remoteMessage.getData().size() > 0) {
            // If no notification payload, create one from data
            if (remoteMessage.getData().containsKey("title")) {
                title = remoteMessage.getData().get("title");
            }
            if (remoteMessage.getData().containsKey("body")) {
                body = remoteMessage.getData().get("body");
            }
        }
        
        // Always send notification with heads-up display
        sendHeadsUpNotification(title, body);
        
        // Also create a banner notification to ensure it shows up at top of screen
        createBannerNotification(title, body);
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

    private void sendHeadsUpNotification(String title, String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        
        // Create banner notification that appears at top of screen
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

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since Android 8.0 (API level 26) and above, notification channels are required
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "BoardEase Notifications",
                NotificationManager.IMPORTANCE_HIGH // High importance for banner
            );
            channel.setDescription("Notifications for BoardEase app");
            channel.enableLights(true);
            channel.setLightColor(0xFF0000FF);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{1000, 1000, 1000});
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            channel.setBypassDnd(true); // Bypass Do Not Disturb
            channel.setImportance(NotificationManager.IMPORTANCE_HIGH); // Ensure high importance
            notificationManager.createNotificationChannel(channel);
        }

        // Use a unique ID for each notification
        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, notificationBuilder.build());
        
        Log.d(TAG, "Heads-up notification sent: " + title + " - " + messageBody);
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
    
    /**
     * Create a banner notification that appears at the top of the screen
     */
    private void createBannerNotification(String title, String message) {
        // Create a banner notification that appears at top of screen
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority for banner
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setTicker(message) // Ticker text for banner
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message)) // Big text style
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE));
        
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        
        Log.d(TAG, "Banner notification created: " + title + " - " + message);
    }

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
        String url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/register_device_token.php";
        
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


























