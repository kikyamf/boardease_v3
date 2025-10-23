package com.example.mock;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class OwnerHomeFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private int userId = -1; // default to -1

    // UI elements
    private TextView tvOwnerName, tvListingsCount, tvBoardersCount, tvViewsCount, tvPopularTitle, tvPopularVisits;
    private ImageView imgPopularListing, ivNotification, ivMessage;
    private View badgeMsg, badgeNotif;
    private TextView badgeCount, badgeNotifCount;
    private LinearLayout numofListings, layoutTotalBoarders;
    
    // Broadcast receiver for badge updates
    private BroadcastReceiver badgeUpdateReceiver;
    
    // Periodic notification check
    private android.os.Handler notificationCheckHandler;
    private Runnable notificationCheckRunnable;
    
    // Periodic message check
    private android.os.Handler messageCheckHandler;
    private Runnable messageCheckRunnable;

    public OwnerHomeFragment() {
        // Required empty public constructor
    }

    public static OwnerHomeFragment newInstance(int userId) {
        OwnerHomeFragment fragment = new OwnerHomeFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getInt(ARG_USER_ID, -1);
        }
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_owner_home, container, false);

        // Bind views
        tvOwnerName = view.findViewById(R.id.tvOwnerName);
        tvListingsCount = view.findViewById(R.id.tvListingsCount);
        tvBoardersCount = view.findViewById(R.id.tvBoardersCount);
        tvViewsCount = view.findViewById(R.id.tvViewsCount);
        tvPopularTitle = view.findViewById(R.id.tvPopularTitle);
        tvPopularVisits = view.findViewById(R.id.tvPopularVisits);
        imgPopularListing = view.findViewById(R.id.imgPopularListing);
        numofListings = view.findViewById(R.id.noofListings);
        layoutTotalBoarders = view.findViewById(R.id.layoutTotalBoarders);
        ivNotification = view.findViewById(R.id.ivNotification);
        ivMessage = view.findViewById(R.id.ivMessage);
        badgeMsg = view.findViewById(R.id.badgeMsg);
        badgeNotif = view.findViewById(R.id.badgeNotif);
        
        // Create a TextView for message badge count if it doesn't exist
        if (badgeMsg != null) {
            badgeCount = new TextView(getContext());
            
            // Create FrameLayout.LayoutParams for proper positioning (same as notification badge)
            FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, 
                FrameLayout.LayoutParams.WRAP_CONTENT);
            badgeParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
            badgeParams.topMargin = 6; // Same as notification badge
            badgeParams.rightMargin = 6; // Same as notification badge
            badgeCount.setLayoutParams(badgeParams);
            
            // Enhanced badge styling
            badgeCount.setBackground(getResources().getDrawable(R.drawable.red_dot));
            badgeCount.setTextColor(getResources().getColor(android.R.color.white));
            badgeCount.setTextSize(11);
            badgeCount.setTypeface(null, android.graphics.Typeface.BOLD);
            badgeCount.setPadding(8, 4, 8, 4);
            badgeCount.setMinWidth(24);
            badgeCount.setMinHeight(24);
            badgeCount.setGravity(android.view.Gravity.CENTER);
            badgeCount.setVisibility(View.GONE);
            
            // Add elevation for better visual separation
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                badgeCount.setElevation(4f);
            }
            
            // Add the badge count to the message icon's parent FrameLayout
            if (ivMessage.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) ivMessage.getParent();
                parent.addView(badgeCount);
                android.util.Log.d("MessageBadge", "Added badgeCount to parent ViewGroup");
                android.util.Log.d("MessageBadge", "Parent ViewGroup: " + parent.getClass().getSimpleName());
                android.util.Log.d("MessageBadge", "Parent child count: " + parent.getChildCount());
            }
        }
        
        // Create a TextView for notification badge count if it doesn't exist
        android.util.Log.d("NotificationBadge", "Initializing notification badge, badgeNotif is null: " + (badgeNotif == null));
        if (badgeNotif != null) {
            badgeNotifCount = new TextView(getContext());
            android.util.Log.d("NotificationBadge", "Created badgeNotifCount TextView");
            
            // Create FrameLayout.LayoutParams for proper positioning (same as badgeNotif)
            FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, 
                FrameLayout.LayoutParams.WRAP_CONTENT
            );
            badgeParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
            badgeParams.topMargin = 6; // Same as badgeNotif
            badgeParams.rightMargin = 6; // Same as badgeNotif
            badgeNotifCount.setLayoutParams(badgeParams);
            
            // Enhanced badge styling
            badgeNotifCount.setBackground(getResources().getDrawable(R.drawable.red_dot));
            badgeNotifCount.setTextColor(getResources().getColor(android.R.color.white));
            badgeNotifCount.setTextSize(11);
            badgeNotifCount.setTypeface(null, android.graphics.Typeface.BOLD);
            badgeNotifCount.setPadding(8, 4, 8, 4);
            badgeNotifCount.setMinWidth(24);
            badgeNotifCount.setMinHeight(24);
            badgeNotifCount.setGravity(android.view.Gravity.CENTER);
            badgeNotifCount.setVisibility(View.GONE);
            
            // Add elevation for better visibility
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                badgeNotifCount.setElevation(4f);
            }
            
            // Add the badge count to the notification icon's parent FrameLayout
            if (ivNotification.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) ivNotification.getParent();
                parent.addView(badgeNotifCount);
                android.util.Log.d("NotificationBadge", "Added badgeNotifCount to parent ViewGroup");
                android.util.Log.d("NotificationBadge", "Parent ViewGroup: " + parent.getClass().getSimpleName());
                android.util.Log.d("NotificationBadge", "Parent child count: " + parent.getChildCount());
            } else {
                android.util.Log.d("NotificationBadge", "ivNotification parent is not a ViewGroup: " + ivNotification.getParent().getClass().getSimpleName());
            }
        }

        ivNotification.setOnClickListener(v -> {
            if (getContext() != null) { // or getActivity() if inside a fragment
                // Hide badge when opening notifications
                hideNotificationBadge();
                Intent intent = new Intent(getContext(), Notification.class);
                startActivity(intent);
            }
        });

        ivMessage.setOnClickListener(v -> {
            if (getContext() != null) { // or getActivity() if inside a fragment
                // Don't hide badge here - only hide when opening actual conversation
                Intent intent = new Intent(getContext(), Messages.class);
                startActivity(intent);
            }
        });

        // Check for unread messages and show badge
        checkUnreadMessages();
        
        // Check for unread notifications and show badge
        checkUnreadNotifications();
        
        // Setup broadcast receiver for badge updates
        setupBadgeUpdateReceiver();

        // Transactions & Logs button click listener
        view.findViewById(R.id.cardTransactionsLogs).setOnClickListener(v -> {
            if (getContext() != null) {
                Intent intent = new Intent(getContext(), TransactionsLogsActivity.class);
                startActivity(intent);
            }
        });


        // Click listener for LinearLayout to navigate to ManageFragment
        numofListings.setOnClickListener(v -> {
            // 1️⃣ Replace fragment
            ManageFragment manageFragment = ManageFragment.newInstance(userId);
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, manageFragment)
                        .addToBackStack(null)
                        .commit();

                // 2️⃣ Update BottomNavigationView selection
                BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.nav_manage); // highlight Manage icon
                }
            }
        });

        // Click listener for Total Boarders to navigate to BoardersListActivity
        layoutTotalBoarders.setOnClickListener(v -> {
            if (getContext() != null) {
                Intent intent = new Intent(getContext(), BoardersListActivity.class);
                intent.putExtra("user_id", userId);
                startActivity(intent);
            }
        });



        if (userId != -1) {
            fetchOwnerDashboardData();
        } else {
            Toast.makeText(getContext(), "User not logged in!", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void fetchOwnerDashboardData() {
        String url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_owner_dashboard.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d("OwnerHomeFragment", "Server Response: " + response);

                    if (response == null || response.trim().isEmpty()) {
                        Toast.makeText(getContext(), "Empty response from server", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        JSONObject obj = new JSONObject(response);

                        // Validate JSON content
                        if (!obj.has("owner_name")) {
                            Toast.makeText(getContext(), "Invalid server response", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Owner name
                        String ownerName = obj.optString("owner_name", "Owner");
                        tvOwnerName.setText(ownerName);

                        // Listings count
                        int listingsCount = obj.optInt("listings_count", 0);
                        tvListingsCount.setText(String.valueOf(listingsCount));

                        // Boarders count
                        int boardersCount = obj.optInt("boarders_count", 0);
                        tvBoardersCount.setText(String.valueOf(boardersCount));

                        // Views count
                        int viewsCount = obj.optInt("views_count", 0);
                        tvViewsCount.setText(String.valueOf(viewsCount));

                        // Popular listing
                        JSONObject popular = obj.optJSONObject("popular_listing");
                        if (popular != null) {
                            tvPopularTitle.setText(popular.optString("bh_name", "No Listing"));
                            tvPopularVisits.setText(popular.optInt("visits", 0) + " visits");

                            String imageUrl = popular.optString("image_path", "");
                            if (!imageUrl.isEmpty()) {
                                Glide.with(getContext())
                                        .load(imageUrl)
                                        .placeholder(R.drawable.sample_listing)
                                        .error(R.drawable.sample_listing)
                                        .into(imgPopularListing);
                            } else {
                                imgPopularListing.setImageResource(R.drawable.sample_listing);
                            }
                        } else {
                            tvPopularTitle.setText("No Popular Listing");
                            tvPopularVisits.setText("0 visits");
                            imgPopularListing.setImageResource(R.drawable.sample_listing);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "JSON Parsing error", Toast.LENGTH_SHORT).show();
                        Log.e("OwnerHomeFragment", "JSON Parse Error: " + e.getMessage());
                    }
                },
                error -> {
                    Toast.makeText(getContext(), "Error fetching dashboard", Toast.LENGTH_SHORT).show();
                    Log.e("OwnerHomeFragment", "Volley Error: " + error.getMessage(), error);
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                Log.d("OwnerHomeFragment", "Sending user_id: " + userId);
                return params;
            }
        };

        Volley.newRequestQueue(getContext()).add(request);
    }

    private void checkUnreadMessages() {
        android.util.Log.d("MessageBadge", "Checking unread messages for user: " + userId);
        
        String url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_unread_count.php?user_id=" + userId;
        
        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.GET, url, null,
            response -> {
                try {
                    android.util.Log.d("MessageBadge", "API Response: " + response.toString());
                    
                    if (response.getBoolean("success")) {
                        JSONObject data = response.getJSONObject("data");
                        int totalUnread = data.getInt("total_unread");
                        android.util.Log.d("MessageBadge", "Unread count: " + totalUnread);
                        showMessageBadge(totalUnread);
                    } else {
                        android.util.Log.e("MessageBadge", "API Error: " + response.getString("message"));
                        hideMessageBadge();
                    }
                } catch (JSONException e) {
                    android.util.Log.e("MessageBadge", "JSON parsing error", e);
                    e.printStackTrace();
                    hideMessageBadge();
                }
            },
            error -> {
                android.util.Log.e("MessageBadge", "API request error", error);
                error.printStackTrace();
                hideMessageBadge();
            }
        );
        
        // Add request to queue
        if (getContext() != null) {
            RequestQueue queue = Volley.newRequestQueue(getContext());
            queue.add(request);
        }
    }

    private void showMessageBadge(int count) {
        android.util.Log.d("MessageBadge", "showMessageBadge called with count: " + count);
        android.util.Log.d("MessageBadge", "badgeMsg is null: " + (badgeMsg == null));
        android.util.Log.d("MessageBadge", "badgeCount is null: " + (badgeCount == null));
        
        if (count > 0) {
            // Hide the simple red dot
            if (badgeMsg != null) {
                badgeMsg.setVisibility(View.GONE);
                android.util.Log.d("MessageBadge", "Hiding simple red dot");
            }
            
            // Show the count badge
            if (badgeCount != null) {
                android.util.Log.d("MessageBadge", "Setting up count badge");
                // Format the count display
                String displayText;
                if (count > 99) {
                    displayText = "99+";
                } else {
                    displayText = String.valueOf(count);
                }
                
                android.util.Log.d("MessageBadge", "Display text: " + displayText);
                badgeCount.setText(displayText);
                
                // Reset any previous animation state
                badgeCount.clearAnimation();
                badgeCount.setAlpha(1f);
                badgeCount.setScaleX(1f);
                badgeCount.setScaleY(1f);
                badgeCount.setVisibility(View.VISIBLE);
                android.util.Log.d("MessageBadge", "Count badge visibility set to VISIBLE");
                android.util.Log.d("MessageBadge", "Badge parent: " + (badgeCount.getParent() != null ? badgeCount.getParent().getClass().getSimpleName() : "null"));
                android.util.Log.d("MessageBadge", "Badge visibility: " + badgeCount.getVisibility());
                android.util.Log.d("MessageBadge", "Badge text: " + badgeCount.getText());
                
                // Check layout parameters
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) badgeCount.getLayoutParams();
                if (params != null) {
                    android.util.Log.d("MessageBadge", "Badge layout params - gravity: " + params.gravity + ", topMargin: " + params.topMargin + ", rightMargin: " + params.rightMargin);
                } else {
                    android.util.Log.d("MessageBadge", "Badge layout params is null!");
                }
                
                // Add scale animation
                badgeCount.setScaleX(0f);
                badgeCount.setScaleY(0f);
                badgeCount.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start();
                android.util.Log.d("MessageBadge", "Count badge animation started");
            } else {
                android.util.Log.d("MessageBadge", "badgeCount is null, cannot show count badge");
            }
        } else {
            hideMessageBadge();
        }
    }

    private void hideMessageBadge() {
        android.util.Log.d("MessageBadge", "hideMessageBadge called");
        if (badgeMsg != null) {
            badgeMsg.setVisibility(View.GONE);
        }
        if (badgeCount != null) {
            android.util.Log.d("MessageBadge", "Hiding count badge with animation");
            // Clear any existing animation first
            badgeCount.clearAnimation();
            // Add fade out animation
            badgeCount.animate()
                .alpha(0f)
                .scaleX(0f)
                .scaleY(0f)
                .setDuration(150)
                .withEndAction(() -> {
                    badgeCount.setVisibility(View.GONE);
                    android.util.Log.d("MessageBadge", "Count badge hidden and set to GONE");
                })
                .start();
        }
    }

    private void checkUnreadNotifications() {
        if (userId == -1) {
            android.util.Log.d("NotificationBadge", "User ID is -1, skipping notification check");
            return;
        }
        
        android.util.Log.d("NotificationBadge", "Checking unread notifications for user: " + userId);
        String url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_unread_notif_count.php?user_id=" + userId;
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    android.util.Log.d("NotificationBadge", "API Response: " + response.toString());
                    if (response.getBoolean("success")) {
                        JSONObject data = response.getJSONObject("data");
                        int unreadCount = data.getInt("total_unread");
                        android.util.Log.d("NotificationBadge", "Unread count: " + unreadCount);
                        showNotificationBadge(unreadCount);
                    } else {
                        android.util.Log.d("NotificationBadge", "API returned success: false");
                        hideNotificationBadge();
                    }
                } catch (JSONException e) {
                    android.util.Log.e("NotificationBadge", "JSON parsing error", e);
                    e.printStackTrace();
                    hideNotificationBadge();
                }
            },
            error -> {
                android.util.Log.e("NotificationBadge", "API request error", error);
                error.printStackTrace();
                hideNotificationBadge();
            }
        );
        
        // Add request to queue
        if (getContext() != null) {
            RequestQueue queue = Volley.newRequestQueue(getContext());
            queue.add(request);
        }
    }

    private void showNotificationBadge(int count) {
        android.util.Log.d("NotificationBadge", "showNotificationBadge called with count: " + count);
        android.util.Log.d("NotificationBadge", "badgeNotif is null: " + (badgeNotif == null));
        android.util.Log.d("NotificationBadge", "badgeNotifCount is null: " + (badgeNotifCount == null));
        
        if (count > 0) {
            // Hide the simple red dot
            if (badgeNotif != null) {
                badgeNotif.setVisibility(View.GONE);
                android.util.Log.d("NotificationBadge", "Hiding simple red dot");
            }
            
            // Show the count badge
            if (badgeNotifCount != null) {
                android.util.Log.d("NotificationBadge", "Setting up count badge");
                String displayText;
                if (count > 99) {
                    displayText = "99+";
                } else {
                    displayText = String.valueOf(count);
                }
                
                android.util.Log.d("NotificationBadge", "Display text: " + displayText);
                badgeNotifCount.setText(displayText);
                
                // Reset any previous animation state
                badgeNotifCount.clearAnimation();
                badgeNotifCount.setAlpha(1f);
                badgeNotifCount.setScaleX(1f);
                badgeNotifCount.setScaleY(1f);
                badgeNotifCount.setVisibility(View.VISIBLE);
                android.util.Log.d("NotificationBadge", "Count badge visibility set to VISIBLE");
                android.util.Log.d("NotificationBadge", "Badge parent: " + (badgeNotifCount.getParent() != null ? badgeNotifCount.getParent().getClass().getSimpleName() : "null"));
                android.util.Log.d("NotificationBadge", "Badge visibility: " + badgeNotifCount.getVisibility());
                android.util.Log.d("NotificationBadge", "Badge text: " + badgeNotifCount.getText());
                
                // Check layout parameters
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) badgeNotifCount.getLayoutParams();
                if (params != null) {
                    android.util.Log.d("NotificationBadge", "Badge layout params - gravity: " + params.gravity + ", topMargin: " + params.topMargin + ", rightMargin: " + params.rightMargin);
                } else {
                    android.util.Log.d("NotificationBadge", "Badge layout params is null!");
                }
                
                // Add scale animation
                badgeNotifCount.setScaleX(0f);
                badgeNotifCount.setScaleY(0f);
                badgeNotifCount.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start();
                android.util.Log.d("NotificationBadge", "Count badge animation started");
            } else {
                android.util.Log.d("NotificationBadge", "badgeNotifCount is null, cannot show count badge");
            }
        } else {
            hideNotificationBadge();
        }
    }

    private void hideNotificationBadge() {
        android.util.Log.d("NotificationBadge", "hideNotificationBadge called");
        if (badgeNotif != null) {
            badgeNotif.setVisibility(View.GONE);
        }
        if (badgeNotifCount != null) {
            android.util.Log.d("NotificationBadge", "Hiding count badge with animation");
            // Clear any existing animation first
            badgeNotifCount.clearAnimation();
            // Add fade out animation
            badgeNotifCount.animate()
                .alpha(0f)
                .scaleX(0f)
                .scaleY(0f)
                .setDuration(150)
                .withEndAction(() -> {
                    badgeNotifCount.setVisibility(View.GONE);
                    android.util.Log.d("NotificationBadge", "Count badge hidden and set to GONE");
                })
                .start();
        }
    }

    private void setupBadgeUpdateReceiver() {
        badgeUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("com.example.mock.UPDATE_NOTIFICATION_BADGE".equals(action)) {
                    // Refresh notification badge count
                    checkUnreadNotifications();
                } else if ("com.example.mock.UPDATE_BADGE".equals(action)) {
                    // Refresh message badge count
                    checkUnreadMessages();
                }
            }
        };
        
        // Register the receiver
        if (getContext() != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.example.mock.UPDATE_NOTIFICATION_BADGE");
            filter.addAction("com.example.mock.UPDATE_BADGE");
            getContext().registerReceiver(badgeUpdateReceiver, filter);
        }
    }

    private void startPeriodicNotificationCheck() {
        if (notificationCheckHandler == null) {
            notificationCheckHandler = new android.os.Handler();
        }
        
        notificationCheckRunnable = new Runnable() {
            @Override
            public void run() {
                // Check for notifications every 10 seconds
                checkUnreadNotifications();
                
                // Schedule next check
                if (notificationCheckHandler != null) {
                    notificationCheckHandler.postDelayed(this, 10000); // 10 seconds
                }
            }
        };
        
        // Start the periodic check
        notificationCheckHandler.postDelayed(notificationCheckRunnable, 10000);
        android.util.Log.d("NotificationBadge", "Started periodic notification check");
    }
    
    private void stopPeriodicNotificationCheck() {
        if (notificationCheckHandler != null && notificationCheckRunnable != null) {
            notificationCheckHandler.removeCallbacks(notificationCheckRunnable);
            android.util.Log.d("NotificationBadge", "Stopped periodic notification check");
        }
    }
    
    private void startPeriodicMessageCheck() {
        if (messageCheckHandler == null) {
            messageCheckHandler = new android.os.Handler();
        }
        
        messageCheckRunnable = new Runnable() {
            @Override
            public void run() {
                // Check for messages every 10 seconds
                checkUnreadMessages();
                
                // Schedule next check
                if (messageCheckHandler != null) {
                    messageCheckHandler.postDelayed(this, 10000); // 10 seconds
                }
            }
        };
        
        // Start the periodic check
        messageCheckHandler.postDelayed(messageCheckRunnable, 10000);
        android.util.Log.d("MessageBadge", "Started periodic message check");
    }
    
    private void stopPeriodicMessageCheck() {
        if (messageCheckHandler != null && messageCheckRunnable != null) {
            messageCheckHandler.removeCallbacks(messageCheckRunnable);
            android.util.Log.d("MessageBadge", "Stopped periodic message check");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop periodic checks
        stopPeriodicNotificationCheck();
        stopPeriodicMessageCheck();
        
        // Unregister broadcast receiver
        if (badgeUpdateReceiver != null && getContext() != null) {
            try {
                getContext().unregisterReceiver(badgeUpdateReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver was not registered
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check for unread messages when returning to this fragment
        checkUnreadMessages();
        // Check for unread notifications when returning to this fragment
        checkUnreadNotifications();
        
        // Also check notifications periodically for real-time updates
        startPeriodicNotificationCheck();
        
        // Start periodic message check
        startPeriodicMessageCheck();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Stop periodic checks when fragment is not visible
        stopPeriodicNotificationCheck();
        stopPeriodicMessageCheck();
    }
}
