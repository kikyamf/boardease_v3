package com.example.mock;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Messages extends AppCompatActivity {

    private ImageView backButton, searchButton, addGroupButton;
    private TextView textNewGC;
    private RecyclerView profileRecyclerView, chatListRecyclerView;
    private CardView layoutEmptyChatList;
    private LinearLayout layoutEmptyProfileList;
    private View dividerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RequestQueue requestQueue;
    private int currentUserId;
    private String currentUserType;
    private List<ProfileModel> profileList = new ArrayList<>();
    private List<ChatModel> chatList = new ArrayList<>();
    private ProfileAdapter profileAdapter;
    private ChatListAdapter chatListAdapter;
    private ProgressDialog progressDialog;
    private static int nextGroupId = 10; // For tracking new group chats
    private boolean isFirstLoad = true; // Flag to control loading dialog
    private int refreshCallCount = 0; // Track refresh API calls

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_messages);

        // Adjust for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Volley
        requestQueue = Volley.newRequestQueue(this);

        // Get current user info from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        
        // Handle String type for user_id (stored as String in Login.java)
        String userIdString = sharedPreferences.getString("user_id", "1");
        try {
            currentUserId = Integer.parseInt(userIdString);
        } catch (NumberFormatException e) {
            currentUserId = 1; // Default fallback
        }
        // Get user type from session
        currentUserType = sharedPreferences.getString("user_role", "Boarder");
        Log.d("Messages", "Current user type: " + currentUserType);
        
        // Debug: Check all SharedPreferences keys
        Map<String, ?> allPrefs = sharedPreferences.getAll();
        for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
            Log.d("Messages", "SharedPrefs - " + entry.getKey() + ": " + entry.getValue());
        }

        // Initialize views
        backButton = findViewById(R.id.backButton);
        searchButton = findViewById(R.id.searchButton);
        addGroupButton = findViewById(R.id.addGroupButton);
        textNewGC = findViewById(R.id.textNewGC);
        dividerView = findViewById(R.id.dividerView);
        profileRecyclerView = findViewById(R.id.profileRecyclerView);
        chatListRecyclerView = findViewById(R.id.chatListRecyclerView);
        layoutEmptyChatList = findViewById(R.id.layoutEmptyChatList);
        layoutEmptyProfileList = findViewById(R.id.layoutEmptyProfileList);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Back button action
        backButton.setOnClickListener(v -> finish());

        // Search button action
        searchButton.setOnClickListener(v -> showSearchDialog());

        // Add group action - only for owners
        addGroupButton.setOnClickListener(v -> {
            Log.d("Messages", "Add button clicked. Current user type: '" + currentUserType + "'");
            if ("BH Owner".equals(currentUserType) || "Owner".equals(currentUserType)) {
                Intent intent = new Intent(Messages.this, CreateGroupChat.class);
                startActivityForResult(intent, 1001);
            } else {
                Toast.makeText(this, "Only boarding house owners can create group chats", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Show/hide add button, text, and divider based on user role
        if ("BH Owner".equals(currentUserType) || "Owner".equals(currentUserType)) {
            addGroupButton.setVisibility(View.VISIBLE);
            textNewGC.setVisibility(View.VISIBLE);
            dividerView.setVisibility(View.VISIBLE);
            Log.d("Messages", "Add button, text, and divider visible for owner. Role: " + currentUserType);
        } else {
            addGroupButton.setVisibility(View.GONE);
            textNewGC.setVisibility(View.GONE);
            dividerView.setVisibility(View.GONE);
            Log.d("Messages", "Add button, text, and divider hidden for boarder. Role: " + currentUserType);
        }

        // Setup RecyclerViews
        setupRecyclerViews();
        
        // Setup pull-to-refresh
        setupSwipeRefresh();
        
        // Load real data from database
        loadUsersForMessaging();
        loadChatList();
    }
    
    private void setupSwipeRefresh() {
        if (swipeRefreshLayout == null) {
            Log.e("Messages", "SwipeRefreshLayout is null! Check if the view ID is correct in the layout file.");
            return;
        }
        
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Reset counter for tracking refresh calls
            refreshCallCount = 0;
            // Refresh both profile list and chat list
            loadUsersForMessaging(true); // Pass true to indicate it's a refresh
            loadChatList(false, true); // Don't show loading dialog when refreshing, but mark as refresh
        });
        
        // Set refresh colors (optional - customize as needed)
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        );
    }
    
    private void stopRefreshIfComplete() {
        refreshCallCount++;
        // Both calls complete (loadUsersForMessaging and loadChatList)
        if (refreshCallCount >= 2 && swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
            refreshCallCount = 0; // Reset for next refresh
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh chat list to update unread indicators when returning from conversation
        // Don't show loading dialog when coming back from conversation
        loadChatList(false);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        // Register broadcast receiver for badge updates
        android.content.IntentFilter filter = new android.content.IntentFilter("com.example.mock.UPDATE_BADGE");
        registerReceiver(badgeUpdateReceiver, filter);
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        // Unregister broadcast receiver
        try {
            unregisterReceiver(badgeUpdateReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver was not registered
        }
    }
    
    private final android.content.BroadcastReceiver badgeUpdateReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            android.util.Log.d("Messages", "Badge update broadcast received - refreshing chat list");
            // Refresh chat list when badge is updated - don't show loading dialog
            loadChatList(false);
        }
    };

    private void setupRecyclerViews() {
        // Horizontal RecyclerView for profiles
        Log.d("Messages", "Setting up profile RecyclerView");
        LinearLayoutManager horizontalLayoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        profileRecyclerView.setLayoutManager(horizontalLayoutManager);
        profileAdapter = new ProfileAdapter(this, profileList);
        profileRecyclerView.setAdapter(profileAdapter);
        
        Log.d("Messages", "Profile RecyclerView setup complete. Initial profile list size: " + profileList.size());
        
        // Set click listener for profiles to open conversations
        profileAdapter.setOnItemClickListener(profile -> {
            // Open conversation with the selected profile
            Intent intent = new Intent(Messages.this, Conversation.class);
            intent.putExtra("chatName", profile.getName());
            intent.putExtra("chatImage", profile.getImageResId());
            intent.putExtra("profilePictureUrl", profile.getProfilePictureUrl());
            intent.putExtra("chatId", profile.getUserId());
            intent.putExtra("chatType", "individual");
            intent.putExtra("otherUserId", profile.getUserId());
            intent.putExtra("otherUserName", profile.getName());
            startActivity(intent);
        });

        // Vertical RecyclerView for chat list
        LinearLayoutManager verticalLayoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        chatListRecyclerView.setLayoutManager(verticalLayoutManager);

        chatListAdapter = new ChatListAdapter(this, chatList);

        // Click handler for chat list items
        chatListAdapter.setOnItemClickListener(new ChatListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ChatModel chat) {
                Intent intent = new Intent(Messages.this, Conversation.class);
                intent.putExtra("chatName", chat.getName());
                intent.putExtra("chatImage", chat.getImageResId());
                intent.putExtra("profilePictureUrl", chat.getProfilePictureUrl());
                intent.putExtra("chatId", chat.getChatId());
                intent.putExtra("chatType", chat.getChatType());
                intent.putExtra("otherUserId", chat.getOtherUserId());
                intent.putExtra("otherUserName", chat.getOtherUserName());
                intent.putExtra("groupId", chat.getGroupId());
                startActivity(intent);
            }
            
            @Override
            public void onItemLongClick(ChatModel chat) {
                showDeleteChatDialog(chat);
            }
        });

        chatListRecyclerView.setAdapter(chatListAdapter);
    }

    private void loadUsersForMessaging() {
        loadUsersForMessaging(false);
    }
    
    private void loadUsersForMessaging(boolean isRefresh) {
        String url = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/get_users_for_messaging.php?current_user_id=" + currentUserId;
        
        Log.d("Messages", "Loading users from URL: " + url);
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Log.d("Messages", "API Response: " + response.toString());
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            JSONArray usersArray = data.getJSONArray("users");
                            profileList.clear();
                            
                            Log.d("Messages", "Found " + usersArray.length() + " users");
                            
                            if (usersArray.length() > 0) {
                                for (int i = 0; i < usersArray.length(); i++) {
                                    JSONObject userObj = usersArray.getJSONObject(i);
                                    
                                    // Get boarding house info if available
                                    String boardingHouseName = "";
                                    String boardingHouseAddress = "";
                                    if (userObj.has("boarding_house_name")) {
                                        boardingHouseName = userObj.getString("boarding_house_name");
                                    }
                                    if (userObj.has("boarding_house_address")) {
                                        boardingHouseAddress = userObj.getString("boarding_house_address");
                                    }
                                    
                                    // Get profile picture URL if available
                                    String profilePictureUrl = "";
                                    if (userObj.has("profile_picture") && !userObj.isNull("profile_picture")) {
                                        profilePictureUrl = userObj.getString("profile_picture");
                                    }
                                    
                                    ProfileModel profile = new ProfileModel(
                                        userObj.getInt("user_id"),
                                        userObj.getString("full_name"),
                                        userObj.getString("user_type"),
                                        userObj.getString("email"),
                                        userObj.getString("phone"),
                                        R.drawable.ic_profile,
                                        userObj.getBoolean("has_device_token"),
                                        userObj.getBoolean("has_device_token") ? "Online" : "Offline",
                                        boardingHouseName,
                                        boardingHouseAddress,
                                        profilePictureUrl
                                    );
                                    profileList.add(profile);
                                    Log.d("Messages", "Added profile: " + profile.getName());
                                }
                            } else {
                                // Show empty state - no users found
                                Log.d("Messages", "No users found for messaging");
                            }
                            
                            profileAdapter.notifyDataSetChanged();
                            updateProfileEmptyState();
                        } else {
                            Log.e("Messages", "API Error: " + response.getString("message"));
                            Toast.makeText(this, "Error loading users: " + response.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e("Messages", "Error parsing users data", e);
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing users data", Toast.LENGTH_SHORT).show();
                    } finally {
                        // Stop refresh indicator if this was a refresh call
                        if (isRefresh) {
                            stopRefreshIfComplete();
                        }
                    }
                },
                error -> {
                    Log.e("Messages", "Network error loading users", error);
                    Toast.makeText(this, "Error loading users: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    // Stop refresh indicator if this was a refresh call
                    if (isRefresh) {
                        stopRefreshIfComplete();
                    }
                });

        requestQueue.add(request);
    }
    

    private void loadChatList() {
        loadChatList(isFirstLoad);
    }
    
    // Method to manually refresh with loading dialog (for when user clicks Messages icon)
    public void refreshChatListWithLoading() {
        loadChatList(true);
    }
    
    private void loadChatList(boolean showLoading) {
        loadChatList(showLoading, false);
    }
    
    private void loadChatList(boolean showLoading, boolean isRefresh) {
        android.util.Log.d("Messages", "loadChatList called for user: " + currentUserId + ", showLoading: " + showLoading);
        if (showLoading) {
            showProgressDialog("Loading messages...");
        }
        String url = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/get_chat_list.php?user_id=" + currentUserId;
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        android.util.Log.d("LoadChatList", "Response received: " + response.toString());
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            JSONArray chatsArray = data.getJSONArray("chats");
                            android.util.Log.d("LoadChatList", "Found " + chatsArray.length() + " chats");
                            chatList.clear();
                            
                            if (chatsArray.length() > 0) {
                                for (int i = 0; i < chatsArray.length(); i++) {
                                JSONObject chatObj = chatsArray.getJSONObject(i);
                                ChatModel chat;
                                
                                if (chatObj.getString("chat_type").equals("individual")) {
                                    // Format individual message with sender name
                                    // Only show "You: message" when current user sent it
                                    // Don't show other person's name since it's obvious in 1-on-1 conversation
                                    String lastMessage = chatObj.getString("last_message");
                                    String senderName = chatObj.optString("last_sender_name", "");
                                    
                                    String formattedMessage = lastMessage;
                                    if (!senderName.isEmpty() && senderName.equals("You")) {
                                        formattedMessage = senderName + ": " + lastMessage;
                                    }
                                    
                                    // Get profile picture URL if available
                                    String profilePictureUrl = "";
                                    if (chatObj.has("other_user_profile_picture") && !chatObj.isNull("other_user_profile_picture")) {
                                        profilePictureUrl = chatObj.getString("other_user_profile_picture");
                                    }
                                    
                                    chat = new ChatModel(
                                        chatObj.getString("other_user_name"),
                                        formattedMessage,
                                        formatTime(chatObj.getString("last_message_time")),
                                        R.drawable.ic_profile,
                                        chatObj.getInt("other_user_id"),
                                        chatObj.getString("chat_type"),
                                        chatObj.getInt("unread_count"),
                                        chatObj.getString("last_message_status"),
                                        chatObj.getInt("other_user_id"),
                                        chatObj.getString("other_user_name"),
                                        profilePictureUrl
                                    );
                                } else {
                                    // Format group message with sender name
                                    String lastMessage = chatObj.getString("last_message");
                                    String senderName = chatObj.optString("last_sender_name", "");
                                    
                                    String formattedMessage = lastMessage;
                                    if (!senderName.isEmpty() && !lastMessage.equals("No messages yet")) {
                                        formattedMessage = senderName + ": " + lastMessage;
                                    }
                                    
                                    // Get creator_id if available - try different field names
                                    int creatorId = -1;
                                    if (chatObj.has("creator_id") && !chatObj.isNull("creator_id")) {
                                        creatorId = chatObj.getInt("creator_id");
                                    } else if (chatObj.has("created_by") && !chatObj.isNull("created_by")) {
                                        creatorId = chatObj.getInt("created_by");
                                    } else if (chatObj.has("owner_id") && !chatObj.isNull("owner_id")) {
                                        creatorId = chatObj.getInt("owner_id");
                                    }
                                    android.util.Log.d("LoadChatList", "Group: " + chatObj.getString("group_name") + ", Creator ID: " + creatorId);
                                    
                                    ChatModel groupChat = new ChatModel(
                                        chatObj.getString("group_name"),
                                        formattedMessage,
                                        formatTime(chatObj.getString("last_message_time")),
                                        R.drawable.ic_profile,
                                        chatObj.getInt("group_id"),
                                        chatObj.getString("chat_type"),
                                        chatObj.getInt("unread_count"),
                                        "Sent",
                                        chatObj.getString("group_name"),
                                        chatObj.getInt("group_id"),
                                        creatorId
                                    );
                                    chat = groupChat;
                                }
                                
                                chatList.add(chat);
                                }
                            } else {
                                // Show empty state - no chats found
                                android.util.Log.d("LoadChatList", "No chats found");
                            }
                            
                            chatListAdapter.notifyDataSetChanged();
                            updateChatEmptyState();
                        } else {
                            Toast.makeText(this, "Error loading chats: " + response.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                        if (showLoading) {
                            hideProgressDialog();
                        }
                        // Set first load to false after first successful load
                        if (isFirstLoad) {
                            isFirstLoad = false;
                        }
                        // Stop refresh indicator if this was a refresh call
                        if (isRefresh) {
                            stopRefreshIfComplete();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing chat data", Toast.LENGTH_SHORT).show();
                        if (showLoading) {
                            hideProgressDialog();
                        }
                        // Stop refresh indicator if this was a refresh call
                        if (isRefresh) {
                            stopRefreshIfComplete();
                        }
                    }
                },
                error -> {
                    Toast.makeText(this, "Error loading chats: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    if (showLoading) {
                        hideProgressDialog();
                    }
                    // Stop refresh indicator if this was a refresh call
                    if (isRefresh) {
                        stopRefreshIfComplete();
                    }
                });

        requestQueue.add(request);
    }



    private void addNewGroupChatToList(String groupName) {
        // Create a new group chat and add it to the top of the list
        int currentUserId = getCurrentUserId();
        ChatModel newGroupChat = new ChatModel(
            groupName,
            "Group created successfully!",
            "Just now",
            R.drawable.ic_profile,
            nextGroupId,
            "group",
            0,
            "Sent",
            groupName,
            nextGroupId,
            currentUserId // Set creator ID to current user
        );
        
        // Add to the beginning of the list (most recent first)
        chatList.add(0, newGroupChat);
        chatListAdapter.notifyDataSetChanged();
        
        // Increment for next group
        nextGroupId++;
        
        // Show success message
        Toast.makeText(this, "Group chat '" + groupName + "' added to your messages!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // Group chat was created successfully, add it to the chat list
            String groupName = data.getStringExtra("group_name");
            if (groupName != null && !groupName.isEmpty()) {
                addNewGroupChatToList(groupName);
            }
        }
    }

    private void showSearchDialog() {
        // Create search dialog with real-time suggestions
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔍 Search Users");

        // Create a LinearLayout to hold search input and suggestions
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 30, 40, 30);

        // Create EditText for search input
        final EditText searchInput = new EditText(this);
        searchInput.setHint("Enter user's name...");
        searchInput.setPadding(25, 20, 25, 20);
        searchInput.setBackground(getResources().getDrawable(R.drawable.edittext_background));
        searchInput.setTextSize(18);
        searchInput.setTextColor(getResources().getColor(android.R.color.black));
        searchInput.setHintTextColor(getResources().getColor(android.R.color.darker_gray));
        
        // Set layout parameters for search input
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT);
        inputParams.setMargins(0, 0, 0, 20);
        searchInput.setLayoutParams(inputParams);
        layout.addView(searchInput);

        // Create RecyclerView for suggestions
        final RecyclerView suggestionsRecyclerView = new RecyclerView(this);
        suggestionsRecyclerView.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 400)); // Increased height
        suggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        suggestionsRecyclerView.setBackground(getResources().getDrawable(R.drawable.edittext_background));
        suggestionsRecyclerView.setPadding(10, 10, 10, 10);
        layout.addView(suggestionsRecyclerView);

        // Setup suggestions RecyclerView
        final List<ProfileModel> suggestions = new ArrayList<>();
        final SearchResultAdapter suggestionsAdapter = new SearchResultAdapter(this, suggestions);
        suggestionsRecyclerView.setAdapter(suggestionsAdapter);
        
        // Set click listener for suggestions
        suggestionsAdapter.setOnItemClickListener(profile -> {
            // Open conversation with selected user
            Intent intent = new Intent(Messages.this, Conversation.class);
            intent.putExtra("chatName", profile.getName());
            intent.putExtra("chatImage", profile.getImageResId());
            intent.putExtra("profilePictureUrl", profile.getProfilePictureUrl());
            intent.putExtra("chatId", profile.getUserId());
            intent.putExtra("chatType", "individual");
            intent.putExtra("otherUserId", profile.getUserId());
            intent.putExtra("otherUserName", profile.getName());
            startActivity(intent);
        });

        // Add text change listener for real-time suggestions
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() > 0) {
                    updateSuggestions(query, suggestions, suggestionsAdapter);
                } else {
                    suggestions.clear();
                    suggestionsAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        builder.setView(layout);

        // Cancel button
        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateSuggestions(String query, List<ProfileModel> suggestions, SearchResultAdapter adapter) {
        // Use search_users.php endpoint for real-time search
        String url = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/search_users.php?current_user_id=" + currentUserId + "&search_term=" + query;
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            JSONArray usersArray = data.getJSONArray("users");
                            suggestions.clear();
                            
                            for (int i = 0; i < usersArray.length(); i++) {
                                JSONObject userObj = usersArray.getJSONObject(i);
                                
                                // Get profile picture URL if available
                                String profilePictureUrl = "";
                                if (userObj.has("profile_picture") && !userObj.isNull("profile_picture")) {
                                    profilePictureUrl = userObj.getString("profile_picture");
                                }
                                
                                ProfileModel profile = new ProfileModel(
                                    userObj.getInt("user_id"),
                                    userObj.getString("full_name"),
                                    userObj.getString("user_type"),
                                    userObj.getString("email"),
                                    userObj.getString("phone"),
                                    R.drawable.ic_profile,
                                    userObj.getBoolean("has_device_token"),
                                    userObj.getBoolean("has_device_token") ? "Online" : "Offline",
                                    "",
                                    "",
                                    profilePictureUrl
                                );
                                suggestions.add(profile);
                            }
                            
                            adapter.notifyDataSetChanged();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    // Fallback to local search if API fails
                    suggestions.clear();
                    for (ProfileModel profile : profileList) {
                        String fullName = profile.getName().toLowerCase();
                        String searchQuery = query.toLowerCase();
                        
                        if (fullName.contains(searchQuery)) {
                            suggestions.add(profile);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });

        requestQueue.add(request);
    }

    private void searchUsers(String searchQuery) {
        // Filter users based on search query
        List<ProfileModel> filteredUsers = new ArrayList<>();
        
        for (ProfileModel profile : profileList) {
            // Search in first name, last name, or full name
            String fullName = profile.getName().toLowerCase();
            String query = searchQuery.toLowerCase();
            
            if (fullName.contains(query)) {
                filteredUsers.add(profile);
            }
        }

        if (filteredUsers.isEmpty()) {
            Toast.makeText(this, "No users found with name: " + searchQuery, Toast.LENGTH_SHORT).show();
        } else {
            showSearchResults(filteredUsers, searchQuery);
        }
    }

    private void showSearchResults(List<ProfileModel> results, String searchQuery) {
        // Create results dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Search Results for: " + searchQuery);

        // Create list of names
        String[] names = new String[results.size()];
        for (int i = 0; i < results.size(); i++) {
            names[i] = results.get(i).getName();
        }

        builder.setItems(names, (dialog, which) -> {
            ProfileModel selectedProfile = results.get(which);
            // Open conversation with selected user
            Intent intent = new Intent(Messages.this, Conversation.class);
            intent.putExtra("chatName", selectedProfile.getName());
            intent.putExtra("chatImage", selectedProfile.getImageResId());
            intent.putExtra("profilePictureUrl", selectedProfile.getProfilePictureUrl());
            intent.putExtra("chatId", selectedProfile.getUserId());
            intent.putExtra("chatType", "individual");
            intent.putExtra("otherUserId", selectedProfile.getUserId());
            intent.putExtra("otherUserName", selectedProfile.getName());
            startActivity(intent);
        });

        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void showDeleteChatDialog(ChatModel chat) {
        String chatName = chat.getName();
        
        // Check if this is a group chat and if current user is the creator
        boolean isGroupChat = chat.getChatType().equals("group");
        boolean isOwner = false;
        int currentUserId = getCurrentUserId();
        
        if (isGroupChat) {
            int creatorId = chat.getCreatorId();
            isOwner = (creatorId == currentUserId);
            android.util.Log.d("ShowDeleteDialog", "Group Chat: " + chatName);
            android.util.Log.d("ShowDeleteDialog", "Creator ID: " + creatorId + ", Current User ID: " + currentUserId);
            android.util.Log.d("ShowDeleteDialog", "Is Owner: " + isOwner);
            
            // If creatorId is -1 (not set), try to verify ownership via API
            if (creatorId == -1) {
                android.util.Log.d("ShowDeleteDialog", "Creator ID not set, verifying ownership via API...");
                verifyGroupOwnership(chat, currentUserId);
                return; // Exit early, will show dialog after verification
            }
        }
        
        // Show the appropriate dialog based on chat type and ownership
        showDeleteChatDialogAfterVerification(chat, isOwner);
    }
    
    private void showDeleteChatDialogAfterVerification(ChatModel chat, boolean isOwner) {
        String chatName = chat.getName();
        boolean isGroupChat = chat.getChatType().equals("group");
        
        if (isGroupChat && isOwner) {
            // Show options for group chat owner: Leave or Delete
            String[] options = {"Delete GC", "Leave GC"};
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Group Chat Options")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            // Delete GC option
                            new android.app.AlertDialog.Builder(this)
                                    .setTitle("Delete Group Chat")
                                    .setMessage("Are you sure you want to delete \"" + chatName + "\"? This will permanently delete the group chat for all members.")
                                    .setPositiveButton("Delete", (d, w) -> {
                                        deleteGroupChat(chat);
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        } else if (which == 1) {
                            // Leave GC option
                            new android.app.AlertDialog.Builder(this)
                                    .setTitle("Leave Group Chat")
                                    .setMessage("Are you sure you want to leave \"" + chatName + "\"?")
                                    .setPositiveButton("Leave", (d, w) -> {
                                        leaveGroupChat(chat);
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else if (isGroupChat) {
            // Regular member can only leave
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Leave Group Chat")
                    .setMessage("Are you sure you want to leave \"" + chatName + "\"?")
                    .setPositiveButton("Leave", (dialog, which) -> {
                        leaveGroupChat(chat);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            // Individual chat - delete
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Delete Chat")
                    .setMessage("Are you sure you want to delete this chat?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        deleteChat(chat);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }
    
    private void deleteChat(ChatModel chat) {
        String url = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/delete_chat.php";
        
        android.util.Log.d("DeleteChat", "=== DELETING CHAT ===");
        android.util.Log.d("DeleteChat", "URL: " + url);
        android.util.Log.d("DeleteChat", "User ID: " + getCurrentUserId());
        android.util.Log.d("DeleteChat", "Chat Type: " + chat.getChatType());
        android.util.Log.d("DeleteChat", "Chat ID: " + chat.getChatId());
        
        RequestQueue queue = Volley.newRequestQueue(this);
        
        // Create request parameters
        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("user_id", String.valueOf(getCurrentUserId()));
        params.put("chat_type", chat.getChatType());
        params.put("chat_id", String.valueOf(chat.getChatId()));
        
        android.util.Log.d("DeleteChat", "Request params: " + params.toString());
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                response -> {
                    try {
                        android.util.Log.d("DeleteChat", "Response received: " + response.toString());
                        if (response.getBoolean("success")) {
                            android.util.Log.d("DeleteChat", "Chat deleted successfully!");
                            Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show();
                            // Refresh chat list - don't show loading dialog
                            loadChatList(false);
                        } else {
                            android.util.Log.e("DeleteChat", "Failed to delete chat: " + response.getString("message"));
                            Toast.makeText(this, "Failed to delete chat: " + response.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        android.util.Log.e("DeleteChat", "JSON parsing error", e);
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    android.util.Log.e("DeleteChat", "Network error", error);
                    android.util.Log.e("DeleteChat", "Error details: " + error.getMessage());
                    if (error.networkResponse != null) {
                        android.util.Log.e("DeleteChat", "Network response code: " + error.networkResponse.statusCode);
                        android.util.Log.e("DeleteChat", "Network response data: " + new String(error.networkResponse.data));
                    }
                    Toast.makeText(this, "Error deleting chat: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });
        
        queue.add(request);
    }
    
    private void leaveGroupChat(ChatModel chat) {
        String url = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/leave_group_chat.php";
        
        android.util.Log.d("LeaveGroupChat", "=== LEAVING GROUP CHAT ===");
        android.util.Log.d("LeaveGroupChat", "URL: " + url);
        android.util.Log.d("LeaveGroupChat", "User ID: " + getCurrentUserId());
        android.util.Log.d("LeaveGroupChat", "Group ID: " + chat.getGroupId());
        
        RequestQueue queue = Volley.newRequestQueue(this);
        
        // Create request parameters
        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("user_id", String.valueOf(getCurrentUserId()));
        params.put("group_id", String.valueOf(chat.getGroupId()));
        
        android.util.Log.d("LeaveGroupChat", "Request params: " + params.toString());
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                response -> {
                    try {
                        android.util.Log.d("LeaveGroupChat", "Response received: " + response.toString());
                        if (response.getBoolean("success")) {
                            android.util.Log.d("LeaveGroupChat", "Left group chat successfully!");
                            Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show();
                            // Refresh chat list - don't show loading dialog
                            loadChatList(false);
                        } else {
                            android.util.Log.e("LeaveGroupChat", "Failed to leave group chat: " + response.getString("message"));
                            Toast.makeText(this, "Failed to leave group chat: " + response.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        android.util.Log.e("LeaveGroupChat", "JSON parsing error", e);
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    android.util.Log.e("LeaveGroupChat", "Network error", error);
                    android.util.Log.e("LeaveGroupChat", "Error details: " + error.getMessage());
                    if (error.networkResponse != null) {
                        android.util.Log.e("LeaveGroupChat", "Network response code: " + error.networkResponse.statusCode);
                        android.util.Log.e("LeaveGroupChat", "Network response data: " + new String(error.networkResponse.data));
                    }
                    Toast.makeText(this, "Error leaving group chat: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });
        
        queue.add(request);
    }
    
    private void deleteGroupChat(ChatModel chat) {
        String url = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/delete_group_chat.php";
        
        android.util.Log.d("DeleteGroupChat", "=== DELETING GROUP CHAT ===");
        android.util.Log.d("DeleteGroupChat", "URL: " + url);
        android.util.Log.d("DeleteGroupChat", "User ID: " + getCurrentUserId());
        android.util.Log.d("DeleteGroupChat", "Group ID: " + chat.getGroupId());
        
        RequestQueue queue = Volley.newRequestQueue(this);
        
        // Create request parameters
        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("user_id", String.valueOf(getCurrentUserId()));
        params.put("group_id", String.valueOf(chat.getGroupId()));
        
        android.util.Log.d("DeleteGroupChat", "Request params: " + params.toString());
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                response -> {
                    try {
                        android.util.Log.d("DeleteGroupChat", "Response received: " + response.toString());
                        if (response.getBoolean("success")) {
                            android.util.Log.d("DeleteGroupChat", "Group chat deleted successfully!");
                            Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show();
                            // Refresh chat list - don't show loading dialog
                            loadChatList(false);
                        } else {
                            android.util.Log.e("DeleteGroupChat", "Failed to delete group chat: " + response.getString("message"));
                            Toast.makeText(this, "Failed to delete group chat: " + response.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        android.util.Log.e("DeleteGroupChat", "JSON parsing error", e);
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    android.util.Log.e("DeleteGroupChat", "Network error", error);
                    android.util.Log.e("DeleteGroupChat", "Error details: " + error.getMessage());
                    if (error.networkResponse != null) {
                        android.util.Log.e("DeleteGroupChat", "Network response code: " + error.networkResponse.statusCode);
                        android.util.Log.e("DeleteGroupChat", "Network response data: " + new String(error.networkResponse.data));
                    }
                    Toast.makeText(this, "Error deleting group chat: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });
        
        queue.add(request);
    }
    
    private void verifyGroupOwnership(ChatModel chat, int currentUserId) {
        // Verify if current user is the owner of the group chat
        String url = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/get_group_info.php?group_id=" + chat.getGroupId();
        
        android.util.Log.d("VerifyOwnership", "Verifying ownership for group: " + chat.getGroupId());
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        android.util.Log.d("VerifyOwnership", "Response: " + response.toString());
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            int creatorId = -1;
                            
                            // Try different field names for creator
                            if (data.has("creator_id") && !data.isNull("creator_id")) {
                                creatorId = data.getInt("creator_id");
                            } else if (data.has("created_by") && !data.isNull("created_by")) {
                                creatorId = data.getInt("created_by");
                            } else if (data.has("owner_id") && !data.isNull("owner_id")) {
                                creatorId = data.getInt("owner_id");
                            }
                            
                            // Update the chat model with creator ID
                            chat.setCreatorId(creatorId);
                            
                            boolean isOwner = (creatorId == currentUserId);
                            android.util.Log.d("VerifyOwnership", "Creator ID from API: " + creatorId + ", Is Owner: " + isOwner);
                            
                            // Now show the appropriate dialog
                            showDeleteChatDialogAfterVerification(chat, isOwner);
                        } else {
                            // If verification fails, assume not owner and show leave option
                            android.util.Log.e("VerifyOwnership", "Failed to verify ownership");
                            showDeleteChatDialogAfterVerification(chat, false);
                        }
                    } catch (JSONException e) {
                        android.util.Log.e("VerifyOwnership", "JSON parsing error", e);
                        // If parsing fails, assume not owner and show leave option
                        showDeleteChatDialogAfterVerification(chat, false);
                    }
                },
                error -> {
                    android.util.Log.e("VerifyOwnership", "Network error", error);
                    // If network error, assume not owner and show leave option
                    showDeleteChatDialogAfterVerification(chat, false);
                });
        
        requestQueue.add(request);
    }
    
    private int getCurrentUserId() {
        // Get current user ID from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        
        // Handle String type for user_id (stored as String in Login.java)
        String userIdString = sharedPreferences.getString("user_id", "1");
        try {
            return Integer.parseInt(userIdString);
        } catch (NumberFormatException e) {
            return 1; // Default fallback
        }
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
    
    @Override
    protected void onPause() {
        super.onPause();
        // Hide progress dialog when activity is paused
        hideProgressDialog();
    }
    
    private void updateProfileEmptyState() {
        Log.d("Messages", "updateProfileEmptyState called. Profile list size: " + profileList.size());
        if (profileList.isEmpty()) {
            Log.d("Messages", "Profile list is empty, showing empty state");
            profileRecyclerView.setVisibility(View.GONE);
            layoutEmptyProfileList.setVisibility(View.VISIBLE);
        } else {
            Log.d("Messages", "Profile list has " + profileList.size() + " items, showing RecyclerView");
            profileRecyclerView.setVisibility(View.VISIBLE);
            layoutEmptyProfileList.setVisibility(View.GONE);
        }
    }
    
    private void updateChatEmptyState() {
        if (chatList.isEmpty()) {
            chatListRecyclerView.setVisibility(View.GONE);
            layoutEmptyChatList.setVisibility(View.VISIBLE);
        } else {
            chatListRecyclerView.setVisibility(View.VISIBLE);
            layoutEmptyChatList.setVisibility(View.GONE);
        }
    }
    
    // Helper method to format time as "2:50 PM"
    private String formatTime(String timestamp) {
        try {
            // Handle null or empty timestamps
            if (timestamp == null || timestamp.isEmpty() || timestamp.equals("null")) {
                return "";
            }
            
            // Parse the timestamp (assuming format like "2025-10-14 14:32:48")
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            java.util.Date date = inputFormat.parse(timestamp);
            
            // Format as "2:50 PM"
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("h:mm a");
            return outputFormat.format(date);
        } catch (Exception e) {
            // If parsing fails, return empty string for null/empty timestamps
            if (timestamp == null || timestamp.isEmpty() || timestamp.equals("null")) {
                return "";
            }
            return timestamp;
        }
    }
}
