package com.example.mock;

import android.app.Application;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Global Application class to handle app-wide logic like the Heartbeat mechanism.
 * This ensures the user stays "Online" regardless of which screen they are on.
 */
public class BoardEaseApp extends Application {
    private static final String TAG = "BoardEaseApp";
    private Handler heartbeatHandler = new Handler(Looper.getMainLooper());
    private static final long HEARTBEAT_INTERVAL = 30000; // 30 seconds
    private int currentUserId = -1;
    private int runningActivities = 0;
    private RequestQueue requestQueue;

    private Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (runningActivities > 0 && currentUserId != -1) {
                sendHeartbeatToServer();
            }
            heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Volley RequestQueue
        requestQueue = Volley.newRequestQueue(this);
        
        // Register listener to track when the app is in the foreground
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                runningActivities++;
                updateCurrentUserId(activity);
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                updateCurrentUserId(activity);
                // Trigger an immediate heartbeat when an activity is resumed
                if (currentUserId != -1) {
                    sendHeartbeatToServer();
                }
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {}

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                runningActivities--;
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {}
        });

        // Start the heartbeat loop
        heartbeatHandler.post(heartbeatRunnable);
    }

    private void updateCurrentUserId(Activity activity) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String userIdString = sharedPreferences.getString("user_id", null);
        if (userIdString != null) {
            try {
                currentUserId = Integer.parseInt(userIdString);
            } catch (NumberFormatException e) {
                currentUserId = -1;
            }
        } else {
            currentUserId = -1;
        }
    }

    private void sendHeartbeatToServer() {
        String token = FCMTokenManager.getCachedToken(this);
        if (token == null || token.isEmpty() || currentUserId == -1) {
            return;
        }

        // Using the same endpoint as token registration for simplicity
        String url = "https://boardease.calapebohol.com/register_device_token.php";
        
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
            response -> Log.d(TAG, "Global Heartbeat success"),
            error -> Log.e(TAG, "Global Heartbeat failed: " + error.getMessage())) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(currentUserId));
                params.put("device_token", token);
                params.put("device_type", "android");
                params.put("app_version", "1.0.0"); // Could be dynamic
                return params;
            }
        };
        
        requestQueue.add(stringRequest);
    }
}
