package com.example.mock;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationUtils {
    
    private static final String TAG = "NotificationUtils";
    public static final String CHANNEL_ID = "default_channel";
    public static final String CHANNEL_NAME = "BoardEase Notifications";
    
    /**
     * Create notification channel for Android 8.0+
     * This should be called early in app lifecycle (e.g., Application or MainActivity onCreate)
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager == null) {
                Log.e(TAG, "NotificationManager is null");
                return;
            }
            
            // Check if channel already exists
            NotificationChannel existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
            if (existingChannel != null) {
                Log.d(TAG, "Notification channel already exists");
                return;
            }
            
            // Create the notification channel
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // High importance to show in notification center
            );
            channel.setDescription("Notifications for BoardEase app");
            channel.enableLights(true);
            channel.setLightColor(0xFF0000FF);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{1000, 1000, 1000});
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            
            // Try to set bypass DND (may require special permission)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    channel.setBypassDnd(false); // Set to false to avoid permission issues
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not set bypass DND: " + e.getMessage());
            }
            
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Notification channel created successfully");
        }
    }
    
    /**
     * Check if notification permission is granted (Android 13+)
     */
    public static boolean areNotificationsEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            return notificationManager.areNotificationsEnabled();
        }
        // For Android 12 and below, notifications are enabled by default
        return true;
    }
    
    /**
     * Check if we can post notifications
     */
    public static boolean canPostNotifications(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return areNotificationsEnabled(context);
        }
        return true;
    }
    
    /**
     * Get notification manager
     */
    public static NotificationManager getNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
}






