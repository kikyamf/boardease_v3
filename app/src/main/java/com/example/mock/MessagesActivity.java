package com.example.mock;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Firebase imports - uncomment when Firebase is added to project
// import com.google.firebase.database.DataSnapshot;
// import com.google.firebase.database.DatabaseError;
// import com.google.firebase.database.DatabaseReference;
// import com.google.firebase.database.FirebaseDatabase;
// import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * MessagesActivity - Main messaging interface for boarders
 * Allows boarders to view and access their direct messages and group chats
 * Boarders can only view existing group chats, cannot create new ones
 */
public class MessagesActivity extends AppCompatActivity {

    // UI Components
    private ImageView backButton, searchButton, addGroupButton;
    private RecyclerView profileRecyclerView, chatListRecyclerView;
    private TextView sectionTitle;

    // Adapters
    private ProfileAdapter profileAdapter;
    private ChatListAdapter chatListAdapter;

    // Data Lists
    private List<ChatProfile> activeProfiles;
    private List<ChatItem> allChats;

    // Firebase - will be used when Firebase is added to project
    // private DatabaseReference databaseReference;
    private String currentUserId;
    private String userRole;

    // Constants
    private static final String TAG = "MessagesActivity";
    private static final String CHAT_TYPE_DM = "DM";
    private static final String CHAT_TYPE_GC = "GC";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        initializeViews();
        initializeFirebase();
        setupClickListeners();
        loadUserData();
        setupRecyclerViews();
        loadChats();
    }

    private void initializeViews() {
        try {
            // Top bar components
            backButton = findViewById(R.id.backButton);
            searchButton = findViewById(R.id.searchButton);
            addGroupButton = findViewById(R.id.addGroupButton);

            // RecyclerViews
            profileRecyclerView = findViewById(R.id.profileRecyclerView);
            chatListRecyclerView = findViewById(R.id.chatListRecyclerView);

            // Section title
            sectionTitle = findViewById(R.id.sectionTitle);

            // Initialize data lists
            activeProfiles = new ArrayList<>();
            allChats = new ArrayList<>();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            Toast.makeText(this, "Error initializing interface", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeFirebase() {
        try {
            // Firebase initialization - uncomment when Firebase is added to project
            // databaseReference = FirebaseDatabase.getInstance().getReference();
            
            // Get current user ID and role from SharedPreferences or Firebase Auth
            currentUserId = getCurrentUserId();
            userRole = getUserRole();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase", e);
        }
    }

    private void setupClickListeners() {
        try {
            // Back button
            if (backButton != null) {
                backButton.setOnClickListener(v -> {
                    try {
                        finish();
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling back button", e);
                    }
                });
            }

            // Search button
            if (searchButton != null) {
                searchButton.setOnClickListener(v -> {
                    try {
                        openSearchDialog();
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling search button", e);
                    }
                });
            }

            // Add Group button - Hide for boarders
            if (addGroupButton != null) {
                if ("Boarder".equals(userRole)) {
                    // Hide the add group button for boarders
                    addGroupButton.setVisibility(View.GONE);
                    // Also hide the parent LinearLayout if needed
                    View addGroupContainer = findViewById(R.id.addGroupContainer);
                    if (addGroupContainer != null) {
                        addGroupContainer.setVisibility(View.GONE);
                    }
                } else {
                    // Show for boarding house owners
                    addGroupButton.setOnClickListener(v -> {
                        try {
                            createNewGroupChat();
                        } catch (Exception e) {
                            Log.e(TAG, "Error handling add group button", e);
                        }
                    });
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners", e);
        }
    }

    private void loadUserData() {
        try {
            // Load current user data from SharedPreferences or Firebase Auth
            // This would typically come from your authentication system
            currentUserId = "boarder_user_123"; // Replace with actual user ID
            userRole = "Boarder"; // Replace with actual user role
        } catch (Exception e) {
            Log.e(TAG, "Error loading user data", e);
        }
    }

    private void setupRecyclerViews() {
        try {
            // Profile RecyclerView (horizontal)
            profileAdapter = new ProfileAdapter(activeProfiles, new ProfileAdapter.OnProfileClickListener() {
                @Override
                public void onProfileClick(ChatProfile profile) {
                    MessagesActivity.this.onProfileClick(profile);
                }
            });
            profileRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            profileRecyclerView.setAdapter(profileAdapter);

            // Chat List RecyclerView (vertical)
            chatListAdapter = new ChatListAdapter(allChats, new ChatListAdapter.OnChatClickListener() {
                @Override
                public void onChatClick(ChatItem chat) {
                    MessagesActivity.this.onChatClick(chat);
                }
            });
            chatListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            chatListRecyclerView.setAdapter(chatListAdapter);

        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerViews", e);
        }
    }

    private void loadChats() {
        try {
            // Load direct messages
            loadDirectMessages();
            
            // Load group chats
            loadGroupChats();

        } catch (Exception e) {
            Log.e(TAG, "Error loading chats", e);
        }
    }

    private void loadDirectMessages() {
        try {
            // Mock data - replace with Firebase when available
            List<ChatItem> dmChats = new ArrayList<>();
            
            // Sample DM chats
            ChatItem dm1 = new ChatItem();
            dm1.setChatId("dm_001");
            dm1.setChatType(CHAT_TYPE_DM);
            dm1.setLastMessage("Hey, how are you?");
            dm1.setTimestamp(System.currentTimeMillis() - 300000); // 5 minutes ago
            dm1.setUnreadCount(2);
            dm1.setOtherParticipantId("user_002");
            dm1.setOtherParticipantName("John Doe");
            dm1.setOtherParticipantImageUrl("");
            dmChats.add(dm1);
            
            ChatItem dm2 = new ChatItem();
            dm2.setChatId("dm_002");
            dm2.setChatType(CHAT_TYPE_DM);
            dm2.setLastMessage("Thanks for the help!");
            dm2.setTimestamp(System.currentTimeMillis() - 1800000); // 30 minutes ago
            dm2.setUnreadCount(0);
            dm2.setOtherParticipantId("user_003");
            dm2.setOtherParticipantName("Jane Smith");
            dm2.setOtherParticipantImageUrl("");
            dmChats.add(dm2);
            
            // Add to main chat list
            allChats.addAll(dmChats);
            updateActiveProfiles();
            sortAndUpdateChats();
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading mock DMs", e);
        }
    }

    private void loadGroupChats() {
        try {
            // Mock data - replace with Firebase when available
            List<ChatItem> gcChats = new ArrayList<>();
            
            // Sample Group Chat
            ChatItem gc1 = new ChatItem();
            gc1.setChatId("gc_001");
            gc1.setChatType(CHAT_TYPE_GC);
            gc1.setLastMessage("Welcome to the boarding house group!");
            gc1.setTimestamp(System.currentTimeMillis() - 600000); // 10 minutes ago
            gc1.setUnreadCount(5);
            gc1.setGroupId("group_001");
            gc1.setGroupName("Boarding House Group");
            gc1.setGroupImageUrl("");
            gc1.setCreatedBy("owner_001");
            gcChats.add(gc1);
            
            ChatItem gc2 = new ChatItem();
            gc2.setChatId("gc_002");
            gc2.setChatType(CHAT_TYPE_GC);
            gc2.setLastMessage("Meeting tomorrow at 2 PM");
            gc2.setTimestamp(System.currentTimeMillis() - 3600000); // 1 hour ago
            gc2.setUnreadCount(0);
            gc2.setGroupId("group_002");
            gc2.setGroupName("House Rules Discussion");
            gc2.setGroupImageUrl("");
            gc2.setCreatedBy("owner_001");
            gcChats.add(gc2);
            
            // Add to main chat list
            allChats.addAll(gcChats);
            updateActiveProfiles();
            sortAndUpdateChats();
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading mock GCs", e);
        }
    }

    private void updateActiveProfiles() {
        try {
            activeProfiles.clear();
            
            // Get the most recent chat for each contact/group
            for (ChatItem chat : allChats) {
                ChatProfile profile = new ChatProfile();
                profile.setChatId(chat.getChatId());
                profile.setChatType(chat.getChatType());
                profile.setLastMessage(chat.getLastMessage());
                profile.setTimestamp(chat.getTimestamp());
                profile.setUnreadCount(chat.getUnreadCount());
                
                if (CHAT_TYPE_DM.equals(chat.getChatType())) {
                    // For DMs, get the other participant's info
                    profile.setDisplayName(chat.getOtherParticipantName());
                    profile.setProfileImageUrl(chat.getOtherParticipantImageUrl());
                } else {
                    // For GCs, use group info
                    profile.setDisplayName(chat.getGroupName());
                    profile.setProfileImageUrl(chat.getGroupImageUrl());
                }
                
                activeProfiles.add(profile);
            }
            
            // Update profile adapter
            if (profileAdapter != null) {
                profileAdapter.notifyDataSetChanged();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating active profiles", e);
        }
    }

    private void sortAndUpdateChats() {
        try {
            // Sort chats by timestamp (most recent first)
            Collections.sort(allChats, new Comparator<ChatItem>() {
                @Override
                public int compare(ChatItem chat1, ChatItem chat2) {
                    return Long.compare(chat2.getTimestamp(), chat1.getTimestamp());
                }
            });
            
            // Update chat list adapter
            if (chatListAdapter != null) {
                chatListAdapter.notifyDataSetChanged();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error sorting chats", e);
        }
    }

    private void onProfileClick(ChatProfile profile) {
        try {
            if (CHAT_TYPE_DM.equals(profile.getChatType())) {
                openDirectMessage(profile.getChatId());
            } else {
                openGroupChat(profile.getChatId());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling profile click", e);
        }
    }

    private void onChatClick(ChatItem chat) {
        try {
            if (CHAT_TYPE_DM.equals(chat.getChatType())) {
                openDirectMessage(chat.getChatId());
            } else {
                openGroupChat(chat.getChatId());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling chat click", e);
        }
    }

    private void openDirectMessage(String chatId) {
        try {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("chatId", chatId);
            intent.putExtra("chatType", CHAT_TYPE_DM);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening direct message", e);
        }
    }

    private void openGroupChat(String chatId) {
        try {
            Intent intent = new Intent(this, GroupChatActivity.class);
            intent.putExtra("chatId", chatId);
            intent.putExtra("chatType", CHAT_TYPE_GC);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening group chat", e);
        }
    }

    private void openSearchDialog() {
        try {
            Intent intent = new Intent(this, SearchUsersActivity.class);
            intent.putExtra("currentUserId", currentUserId);
            intent.putExtra("userRole", userRole);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening search dialog", e);
        }
    }

    private void createNewGroupChat() {
        try {
            if ("BH Owner".equals(userRole)) {
                Intent intent = new Intent(this, CreateGroupChatActivity.class);
                intent.putExtra("currentUserId", currentUserId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Only boarding house owners can create group chats", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating group chat", e);
        }
    }

    private String getCurrentUserId() {
        // Get from SharedPreferences or Firebase Auth
        return "boarder_user_123"; // Replace with actual implementation
    }

    private String getUserRole() {
        // Get from SharedPreferences or Firebase Auth
        return "Boarder"; // Replace with actual implementation
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh chat data when returning to activity
        loadChats();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up Firebase listeners if needed
    }
}
