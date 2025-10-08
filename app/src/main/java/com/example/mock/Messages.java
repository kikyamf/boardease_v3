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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private RecyclerView profileRecyclerView, chatListRecyclerView;
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

        // Get current user info - using hardcoded user_id = 1 for testing
        currentUserId = 1; // Hardcoded user_id for testing
        currentUserType = "owner"; // Set user type as owner

        // Initialize views
        backButton = findViewById(R.id.backButton);
        searchButton = findViewById(R.id.searchButton);
        addGroupButton = findViewById(R.id.addGroupButton);
        profileRecyclerView = findViewById(R.id.profileRecyclerView);
        chatListRecyclerView = findViewById(R.id.chatListRecyclerView);

        // Back button action
        backButton.setOnClickListener(v -> finish());

        // Search button action
        searchButton.setOnClickListener(v -> showSearchDialog());

        // Add group action
        addGroupButton.setOnClickListener(v -> {
            Intent intent = new Intent(Messages.this, CreateGroupChat.class);
            startActivityForResult(intent, 1001); // Request code for group creation
        });

        // Setup RecyclerViews
        setupRecyclerViews();
        
        // Load real data from database
        loadUsersForMessaging();
        loadChatList();
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
        LinearLayoutManager horizontalLayoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        profileRecyclerView.setLayoutManager(horizontalLayoutManager);
        profileAdapter = new ProfileAdapter(this, profileList);
        profileRecyclerView.setAdapter(profileAdapter);
        
        // Set click listener for profiles to open conversations
        profileAdapter.setOnItemClickListener(profile -> {
            // Open conversation with the selected profile
            Intent intent = new Intent(Messages.this, Conversation.class);
            intent.putExtra("chatName", profile.getName());
            intent.putExtra("chatImage", profile.getImageResId());
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
        String url = "http://192.168.101.6/BoardEase2/get_users_for_messaging.php?current_user_id=" + currentUserId;
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            JSONArray usersArray = data.getJSONArray("users");
                            profileList.clear();
                            
                            for (int i = 0; i < usersArray.length(); i++) {
                                JSONObject userObj = usersArray.getJSONObject(i);
                                ProfileModel profile = new ProfileModel(
                                    userObj.getInt("user_id"),
                                    userObj.getString("full_name"),
                                    userObj.getString("user_type"),
                                    userObj.getString("email"),
                                    userObj.getString("phone"),
                                    R.drawable.ic_profile,
                                    userObj.getBoolean("has_device_token"),
                                    userObj.getBoolean("has_device_token") ? "Online" : "Offline"
                                );
                                profileList.add(profile);
                            }
                            
                            profileAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(this, "Error loading users: " + response.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing users data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(this, "Error loading users: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
        android.util.Log.d("Messages", "loadChatList called for user: " + currentUserId + ", showLoading: " + showLoading);
        if (showLoading) {
            showProgressDialog("Loading messages...");
        }
        String url = "http://192.168.101.6/BoardEase2/get_chat_list.php?user_id=" + currentUserId;
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        android.util.Log.d("LoadChatList", "Response received: " + response.toString());
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            JSONArray chatsArray = data.getJSONArray("chats");
                            android.util.Log.d("LoadChatList", "Found " + chatsArray.length() + " chats");
                            chatList.clear();
                            
                            for (int i = 0; i < chatsArray.length(); i++) {
                                JSONObject chatObj = chatsArray.getJSONObject(i);
                                ChatModel chat;
                                
                                if (chatObj.getString("chat_type").equals("individual")) {
                                    chat = new ChatModel(
                                        chatObj.getString("other_user_name"),
                                        chatObj.getString("last_message"),
                                        chatObj.getString("last_message_time"),
                                        R.drawable.ic_profile,
                                        chatObj.getInt("other_user_id"),
                                        chatObj.getString("chat_type"),
                                        chatObj.getInt("unread_count"),
                                        chatObj.getString("last_message_status"),
                                        chatObj.getInt("other_user_id"),
                                        chatObj.getString("other_user_name")
                                    );
                                } else {
                                    chat = new ChatModel(
                                        chatObj.getString("group_name"),
                                        chatObj.getString("last_message"),
                                        chatObj.getString("last_message_time"),
                                        R.drawable.ic_profile,
                                        chatObj.getInt("group_id"),
                                        chatObj.getString("chat_type"),
                                        chatObj.getInt("unread_count"),
                                        "Sent",
                                        chatObj.getString("group_name"),
                                        chatObj.getInt("group_id")
                                    );
                                }
                                
                                chatList.add(chat);
                            }
                            
                            chatListAdapter.notifyDataSetChanged();
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
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing chat data", Toast.LENGTH_SHORT).show();
                        if (showLoading) {
                            hideProgressDialog();
                        }
                    }
                },
                error -> {
                    Toast.makeText(this, "Error loading chats: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    if (showLoading) {
                        hideProgressDialog();
                    }
                });

        requestQueue.add(request);
    }

    private void loadDummyUsers() {
        // Create dummy users for testing (both boarders and owners)
        profileList.clear();
        
        // Add some dummy boarders
        profileList.add(new ProfileModel(2, "John Doe", "boarder", "john@example.com", "123-456-7890", R.drawable.ic_profile, true, "Online"));
        profileList.add(new ProfileModel(3, "Jane Smith", "boarder", "jane@example.com", "123-456-7891", R.drawable.ic_profile, false, "2 hours ago"));
        profileList.add(new ProfileModel(5, "Sarah Wilson", "boarder", "sarah@example.com", "123-456-7893", R.drawable.ic_profile, false, "1 day ago"));
        profileList.add(new ProfileModel(7, "Lisa Garcia", "boarder", "lisa@example.com", "123-456-7895", R.drawable.ic_profile, true, "Online"));
        profileList.add(new ProfileModel(8, "Tom Anderson", "boarder", "tom@example.com", "123-456-7896", R.drawable.ic_profile, false, "3 hours ago"));
        
        // Add some dummy owners
        profileList.add(new ProfileModel(4, "Mike Johnson", "owner", "mike@example.com", "123-456-7892", R.drawable.ic_profile, true, "Online"));
        profileList.add(new ProfileModel(6, "David Brown", "owner", "david@example.com", "123-456-7894", R.drawable.ic_profile, true, "Online"));
        profileList.add(new ProfileModel(9, "Maria Rodriguez", "owner", "maria@example.com", "123-456-7897", R.drawable.ic_profile, false, "2 days ago"));
        profileList.add(new ProfileModel(10, "Robert Taylor", "owner", "robert@example.com", "123-456-7898", R.drawable.ic_profile, true, "Online"));
        
        profileAdapter.notifyDataSetChanged();
    }

    private void loadDummyChatList() {
        // Create dummy chat list for testing
        chatList.clear();
        
        // Individual chats
        chatList.add(new ChatModel(
            "John Doe",
            "Hey, how are you?",
            "2m ago",
            R.drawable.ic_profile,
            1,
            "individual",
            2,
            "Delivered",
            2,
            "John Doe"
        ));
        
        chatList.add(new ChatModel(
            "Jane Smith",
            "Thanks for the help!",
            "1h ago",
            R.drawable.ic_profile,
            2,
            "individual",
            0,
            "Read",
            3,
            "Jane Smith"
        ));
        
        chatList.add(new ChatModel(
            "Mike Johnson",
            "The room looks great!",
            "3h ago",
            R.drawable.ic_profile,
            3,
            "individual",
            1,
            "Sent",
            4,
            "Mike Johnson"
        ));
        
        // Group chats
        chatList.add(new ChatModel(
            "Study Group",
            "Sarah: Let's meet tomorrow at 2pm",
            "30m ago",
            R.drawable.ic_profile,
            4,
            "group",
            3,
            "Delivered",
            "Study Group",
            1
        ));
        
        chatList.add(new ChatModel(
            "Boarding House Updates",
            "David: New maintenance schedule posted",
            "2h ago",
            R.drawable.ic_profile,
            5,
            "group",
            0,
            "Read",
            "Boarding House Updates",
            2
        ));
        
        chatListAdapter.notifyDataSetChanged();
    }

    private void addNewGroupChatToList(String groupName) {
        // Create a new group chat and add it to the top of the list
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
            nextGroupId
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
        String url = "http://192.168.101.6/BoardEase2/search_users.php?current_user_id=" + currentUserId + "&search_term=" + query;
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            JSONArray usersArray = data.getJSONArray("users");
                            suggestions.clear();
                            
                            for (int i = 0; i < usersArray.length(); i++) {
                                JSONObject userObj = usersArray.getJSONObject(i);
                                ProfileModel profile = new ProfileModel(
                                    userObj.getInt("user_id"),
                                    userObj.getString("full_name"),
                                    userObj.getString("user_type"),
                                    userObj.getString("email"),
                                    userObj.getString("phone"),
                                    R.drawable.ic_profile,
                                    userObj.getBoolean("has_device_token"),
                                    userObj.getBoolean("has_device_token") ? "Online" : "Offline"
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
        String message = "Are you sure you want to delete this chat?";
        
        if (chat.getChatType().equals("group")) {
            message = "Are you sure you want to leave this group chat?";
        }
        
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Chat")
                .setMessage(message)
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteChat(chat);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void deleteChat(ChatModel chat) {
        String url = "http://192.168.101.6/BoardEase2/delete_chat.php";
        
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
    
    private int getCurrentUserId() {
        // Get current user ID from SharedPreferences or wherever it's stored
        // This should match how you get the user ID in other parts of the app
        return 1; // Replace with actual user ID retrieval logic
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
}
