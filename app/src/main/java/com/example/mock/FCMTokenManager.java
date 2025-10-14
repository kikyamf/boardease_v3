package com.example.mock;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import androidx.annotation.NonNull;

public class FCMTokenManager {
    private static final String TAG = "FCMTokenManager";
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_FCM_TOKEN = "fcm_token";

    public interface TokenCallback {
        void onTokenReceived(String token);
        void onTokenError(Exception error);
    }

    /**
     * Get the current FCM token
     */
    public static void getCurrentToken(Context context, TokenCallback callback) {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(new OnCompleteListener<String>() {
                @Override
                public void onComplete(@NonNull Task<String> task) {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        if (callback != null) {
                            callback.onTokenError(task.getException());
                        }
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    Log.d(TAG, "FCM Registration Token: " + token);
                    
                    // Save to preferences
                    saveTokenToPreferences(context, token);
                    
                    if (callback != null) {
                        callback.onTokenReceived(token);
                    }
                }
            });
    }

    /**
     * Get token from SharedPreferences (cached)
     */
    public static String getCachedToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_FCM_TOKEN, null);
    }

    /**
     * Save token to SharedPreferences
     */
    public static void saveTokenToPreferences(Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply();
        Log.d(TAG, "FCM token saved to preferences");
    }

    /**
     * Clear token from SharedPreferences
     */
    public static void clearToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_FCM_TOKEN).apply();
        Log.d(TAG, "FCM token cleared from preferences");
    }

    /**
     * Check if token exists in preferences
     */
    public static boolean hasToken(Context context) {
        return getCachedToken(context) != null;
    }
}





















