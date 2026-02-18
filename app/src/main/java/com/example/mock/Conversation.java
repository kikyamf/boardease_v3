package com.example.mock;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Conversation extends AppCompatActivity {

    private ImageView btnBack, chatProfileImage, btnMembers;
    private TextView chatUserName;
    private RecyclerView recyclerMessages;
    private LinearLayout layoutEmptyMessages;
    private EditText editMessage;
    private ImageButton btnSend;

    private MessageAdapter messageAdapter;
    private List<MessageModel> messageList;
    private RequestQueue requestQueue;
    private int currentUserId;
    private int chatId;
    private String chatType;
    private int otherUserId;
    private String otherUserName;
    private int groupId;
    private boolean isSendingMessage = false; // Prevent duplicate sends
    private String lastSentMessage = ""; // Track last sent message to prevent duplicates
    private long lastMarkAsReadTime = 0; // Track when messages were last marked as read
    private static final long MARK_AS_READ_DELAY = 2000; // 2 seconds delay between mark as read calls
    private ProgressDialog progressDialog; // Progress dialog for loading messages
    private String currentUserType; // Store user role
    
    // Group Chat Waiting Modal
    private AlertDialog waitingDialog;
    private boolean hasShownWaitingDialog = false;

    private void showWaitingDialog() {
        if (isFinishing()) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Welcome to " + chatUserName.getText().toString());
        builder.setMessage("This community is ready. Members will be automatically added once their rental becomes active.\n\nNo members yet. Waiting for approved boarders.");
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.setCancelable(false); // Make it persistent until dismissed by user or logic
        waitingDialog = builder.create();
        waitingDialog.show();
    }
    
    // Polling for realtime updates
    private android.os.Handler pollingHandler = new android.os.Handler();
    private static final long POLLING_INTERVAL = 2000; // 2 seconds (faster for conversation)
    private Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            // Load messages silently
            loadMessages(false);
            // Check user online status
            checkUserStatus();
            // Schedule next run
            pollingHandler.postDelayed(this, POLLING_INTERVAL);
        }
    };
    
    // Broadcast receiver for real-time message updates
    private final android.content.BroadcastReceiver messageUpdateReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            if ("com.example.mock.NEW_MESSAGE_RECEIVED".equals(intent.getAction())) {
                handleNewMessageReceived(intent);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensure layout resizes when keyboard appears
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        );

        setContentView(R.layout.activity_conversation);

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
        android.util.Log.d("Conversation", "Current user type: " + currentUserType);

        // Bind views
        btnBack = findViewById(R.id.btnBack);
        chatProfileImage = findViewById(R.id.chatProfileImage);
        chatUserName = findViewById(R.id.chatUserName);
        btnMembers = findViewById(R.id.btnMembers);
        recyclerMessages = findViewById(R.id.recyclerMessages);
        layoutEmptyMessages = findViewById(R.id.layoutEmptyMessages);
        editMessage = findViewById(R.id.editMessage);
        btnSend = findViewById(R.id.btnSend);

        // Get data from intent
        String name = getIntent().getStringExtra("chatName");
        int imageRes = getIntent().getIntExtra("chatImage", R.drawable.ic_profile);
        String profilePictureUrl = getIntent().getStringExtra("profilePictureUrl");
        chatId = getIntent().getIntExtra("chatId", -1);
        chatType = getIntent().getStringExtra("chatType");
        otherUserId = getIntent().getIntExtra("otherUserId", -1);
        otherUserName = getIntent().getStringExtra("otherUserName");
        groupId = getIntent().getIntExtra("groupId", -1);

        chatUserName.setText(name);
        
        // Load profile picture from URL if available, otherwise use default
        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
            String fullImageUrl = "https://boardease.calapebohol.com/" + profilePictureUrl;
            Glide.with(this)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.btn_profile)
                    .error(R.drawable.btn_profile)
                    .centerCrop()
                    .circleCrop()
                    .into(chatProfileImage);
        } else {
            // Use default profile picture icon
            if (chatType != null && chatType.equals("group")) {
                // For group chats, show first letter of group name
                String groupName = name != null ? name : "Group";
                String firstLetter = getFirstLetter(groupName);
                android.graphics.drawable.Drawable letterAvatar = createLetterAvatar(firstLetter, 40);
                chatProfileImage.setImageDrawable(letterAvatar);
                chatProfileImage.setBackground(null);
                chatProfileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                chatProfileImage.setPadding(0, 0, 0, 0);
            } else {
                // For individual chats, use default profile icon with background
                chatProfileImage.setBackgroundResource(R.drawable.circle_bg_dark_gray);
                chatProfileImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
                chatProfileImage.setPadding(8, 8, 8, 8);
                chatProfileImage.setImageResource(R.drawable.btn_profile);
            }
        }

        // Show/hide members button based on chat type
        if (chatType != null && chatType.equals("group")) {
            btnMembers.setVisibility(View.VISIBLE);
        } else {
            btnMembers.setVisibility(View.GONE);
        }

        // Online status indicator
        View onlineStatus = findViewById(R.id.onlineStatus);
        boolean isOnline = getIntent().getBooleanExtra("isOnline", false);
        
        if (chatType != null && chatType.equals("individual")) {
            onlineStatus.setVisibility(isOnline ? View.VISIBLE : View.GONE);
        } else {
            onlineStatus.setVisibility(View.GONE);
        }

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Members button
        btnMembers.setOnClickListener(v -> showGroupMembers());

        // Rename Group functionality (Click on profile image)
        if (chatType != null && chatType.equals("group") && ("BH Owner".equals(currentUserType) || "Owner".equals(currentUserType))) {
            chatProfileImage.setOnClickListener(v -> showRenameDialog());
            // Optional: Provide visual feedback that it's clickable (e.g., toast or tooltip)
            // For now, just making it functional as requested.
        }

        // Setup RecyclerView
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageList, chatType);
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerMessages.setAdapter(messageAdapter);
        
        // Add scroll listener to mark messages as read when user scrolls to bottom
        recyclerMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                // Check if user has scrolled to the bottom (meaning they've seen all messages)
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
                    int totalItemCount = layoutManager.getItemCount();
                    
                    // If user is at the bottom of the conversation, mark messages as read
                    if (lastVisiblePosition >= totalItemCount - 1) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastMarkAsReadTime > MARK_AS_READ_DELAY) {
                            markMessagesAsRead();
                            lastMarkAsReadTime = currentTime;
                        }
                    }
                }
            }
        });

        // Send button
        btnSend.setOnClickListener(v -> {
            String msg = editMessage.getText().toString().trim();
            if (!msg.isEmpty() && !isSendingMessage && !msg.equals(lastSentMessage)) {
                sendMessage(msg);
            }
        });
        
        // Handle Enter key - allow multi-line input
        // Enter key will create new line, Send button will send message
        editMessage.setOnEditorActionListener((v, actionId, event) -> {
            // If user presses Enter (without Shift), allow it to create new line
            if (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER) {
                if (event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                    // Allow default behavior (new line) unless Shift is held
                    if (!event.isShiftPressed()) {
                        // For now, allow new line. User can use Send button to send
                        return false; // Allow default behavior (new line)
                    }
                }
            }
            // If action is IME_ACTION_SEND, send the message
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                String msg = editMessage.getText().toString().trim();
                if (!msg.isEmpty() && !isSendingMessage && !msg.equals(lastSentMessage)) {
                    sendMessage(msg);
                }
                return true;
            }
            return false;
        });
        
        // Auto-scroll to bottom when keyboard appears (EditText gains focus)
        editMessage.setOnFocusChangeListener((v, hasFocus) -> {
            android.util.Log.d("Conversation", "EditText focus changed - hasFocus: " + hasFocus + ", messageList size: " + messageList.size());
            if (hasFocus && !messageList.isEmpty()) {
                // Scroll to bottom when user starts typing with delay for keyboard animation
                scrollToBottomWithDelay();
            }
        });
        
        // Also scroll when user clicks on the EditText
        editMessage.setOnClickListener(v -> {
            android.util.Log.d("Conversation", "EditText clicked - messageList size: " + messageList.size());
            if (!messageList.isEmpty()) {
                // Force immediate scroll first, then delayed scroll
                scrollToBottom();
                scrollToBottomWithDelay();
            }
        });
        
        // Also handle touch events
        editMessage.setOnTouchListener((v, event) -> {
            android.util.Log.d("Conversation", "EditText touched - messageList size: " + messageList.size());
            if (!messageList.isEmpty()) {
                scrollToBottom();
            }
            return false; // Let the event continue to be processed
        });
        
        // Listen for layout changes to detect keyboard appearance
        recyclerMessages.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            // This will be called when the layout changes (including keyboard appearance)
            android.util.Log.d("Conversation", "Layout changed - messageList size: " + messageList.size() + ", hasFocus: " + editMessage.hasFocus());
            if (!messageList.isEmpty() && editMessage.hasFocus()) {
                // Scroll to bottom when keyboard appears and EditText has focus
                android.util.Log.d("Conversation", "Layout change triggered scroll");
                scrollToBottomWithDelay();
            }
        });
        
        // Don't mark messages as read immediately - let user view them first
        // markMessagesAsRead();
        
        // Register broadcast receiver for real-time message updates
        android.content.IntentFilter filter = new android.content.IntentFilter("com.example.mock.NEW_MESSAGE_RECEIVED");
        registerReceiver(messageUpdateReceiver, filter);

        // Load real messages from database
        showProgressDialog("Loading conversation...");
        loadMessages(true);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Start polling
        startPolling();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Stop polling
        stopPolling();
    }

    private void startPolling() {
        // Run immediately
        pollingHandler.post(pollingRunnable);
    }

    private void stopPolling() {
        pollingHandler.removeCallbacks(pollingRunnable);
    }

    private void loadMessages() {
        loadMessages(true);
    }


    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister broadcast receiver
        if (messageUpdateReceiver != null) {
            try {
                unregisterReceiver(messageUpdateReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver was not registered
            }
        }
    }
    

    
    private void markMessagesAsRead() {
        android.util.Log.d("Conversation", "markMessagesAsRead called for user: " + currentUserId);
        android.util.Log.d("Conversation", "Chat type: " + chatType);
        android.util.Log.d("Conversation", "Other user ID: " + otherUserId);
        android.util.Log.d("Conversation", "Group ID: " + groupId);
        
        String url = "https://boardease.calapebohol.com/mark_specific_messages_read.php";
        
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("user_id", currentUserId);
            jsonObject.put("conversation_type", chatType);
            
            if ("individual".equals(chatType) && otherUserId != -1) {
                jsonObject.put("other_user_id", otherUserId);
                android.util.Log.d("Conversation", "Marking individual messages as read from user: " + otherUserId);
            } else if ("group".equals(chatType) && groupId != -1) {
                jsonObject.put("group_id", groupId);
                android.util.Log.d("Conversation", "Marking group messages as read from group: " + groupId);
            }
            
            android.util.Log.d("Conversation", "Request JSON: " + jsonObject.toString());
        } catch (JSONException e) {
            android.util.Log.e("Conversation", "Error creating JSON", e);
            e.printStackTrace();
            return;
        }
        
        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            response -> {
                try {
                    android.util.Log.d("Conversation", "Mark messages read response: " + response.toString());
                    if (response.getBoolean("success")) {
                        String message = response.getString("message");
                        android.util.Log.d("Conversation", "Messages marked as read: " + message);
                        
                        // Send broadcast to update message badge
                        Intent intent = new Intent("com.example.mock.UPDATE_BADGE");
                        sendBroadcast(intent);
                        android.util.Log.d("Conversation", "Sent broadcast to update message badge");
                    } else {
                        android.util.Log.e("Conversation", "Failed to mark messages as read: " + response.getString("message"));
                    }
                } catch (JSONException e) {
                    android.util.Log.e("Conversation", "JSON parsing error", e);
                    e.printStackTrace();
                }
            },
            error -> {
                android.util.Log.e("Conversation", "API request error", error);
                android.util.Log.e("Conversation", "Error details: " + error.getMessage());
                android.util.Log.e("Conversation", "Network response code: " + (error.networkResponse != null ? error.networkResponse.statusCode : "null"));
                android.util.Log.e("Conversation", "Network response data: " + (error.networkResponse != null ? new String(error.networkResponse.data) : "null"));
                error.printStackTrace();
            }
        );
        
        requestQueue.add(request);
    }

    private void loadMessages(boolean showLoading) {
        if (showLoading) {
             // Only show logs if loading explicitly
             android.util.Log.d("LoadMessages", "loadMessages called for chat: " + chatId + ", showLoading: " + showLoading);
        }
        
        String url = "";
        
        if (chatType.equals("individual")) {
            url = "https://boardease.calapebohol.com/get_messages.php?user1_id=" + currentUserId + "&user2_id=" + otherUserId;
            android.util.Log.d("LoadMessages", "=== LOADING INDIVIDUAL MESSAGES ===");
            android.util.Log.d("LoadMessages", "URL: " + url);
            android.util.Log.d("LoadMessages", "User1 ID: " + currentUserId);
            android.util.Log.d("LoadMessages", "User2 ID: " + otherUserId);
        } else {
            // Group chat
            url = "https://boardease.calapebohol.com/get_group_messages.php?group_id=" + groupId + "&current_user_id=" + currentUserId;
            android.util.Log.d("LoadMessages", "=== LOADING GROUP MESSAGES ===");
            android.util.Log.d("LoadMessages", "URL: " + url);
            android.util.Log.d("LoadMessages", "Group ID: " + groupId);
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        android.util.Log.d("LoadMessages", "=== API RESPONSE RECEIVED ===");
                        android.util.Log.d("LoadMessages", "Full response: " + response.toString());
                        
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            JSONArray messagesArray = data.getJSONArray("messages");
                            
                            android.util.Log.d("LoadMessages", "Messages array length: " + messagesArray.length());
                            android.util.Log.d("LoadMessages", "Messages array content: " + messagesArray.toString());
                            
                            messageList.clear();
                            
                            for (int i = 0; i < messagesArray.length(); i++) {
                                JSONObject messageObj = messagesArray.getJSONObject(i);
                                android.util.Log.d("LoadMessages", "Processing message " + i + ": " + messageObj.toString());
                                
                                MessageModel message;
                                
                                if (chatType.equals("individual")) {
                                    // Apply profanity filter to loaded message
                                    String messageText = ProfanityFilter.filterMessage(messageObj.getString("message"));
                                    android.util.Log.d("LoadMessages", "Message text: " + messageText);
                                    
                                    message = new MessageModel(
                                        messageObj.getInt("message_id"),
                                        messageObj.getInt("sender_id"),
                                        messageObj.getInt("receiver_id"),
                                        messageText,
                                        formatTime(messageObj.getString("timestamp")),
                                        messageObj.getString("status"),
                                        messageObj.getBoolean("is_from_current_user"),
                                        messageObj.getString("sender_name"),
                                        messageObj.getString("receiver_name")
                                    );
                                    
                                    android.util.Log.d("LoadMessages", "Created message model: " + message.toString());
                                } else {
                                    // Group message - apply profanity filter
                                    String messageText = ProfanityFilter.filterMessage(messageObj.getString("message_text"));
                                    
                                    message = new MessageModel(
                                        messageObj.getInt("message_id"),
                                        messageObj.getInt("sender_id"),
                                        messageText,
                                        formatTime(messageObj.getString("timestamp")),
                                        messageObj.getString("status"),
                                        messageObj.getString("sender_name")
                                    );
                                    message.setReceiver(messageObj.getBoolean("is_sender"));
                                }
                                
                                messageList.add(message);
                                android.util.Log.d("LoadMessages", "Added message to list. Total messages now: " + messageList.size());
                            }
                            
                            android.util.Log.d("LoadMessages", "Final message list size: " + messageList.size());
                            
                            // Check member count for Group Chat Modal
                            if ("group".equals(chatType)) {
                                int memberCount = data.optInt("member_count", 0);
                                if (memberCount <= 1) {
                                    if (!hasShownWaitingDialog) {
                                        showWaitingDialog();
                                        hasShownWaitingDialog = true;
                                    }
                                } else {
                                    // If member joined, dismiss the dialog if it's showing
                                    if (waitingDialog != null && waitingDialog.isShowing()) {
                                        waitingDialog.dismiss();
                                    }
                                }
                            }
                            
                            // Update empty state first
                            updateMessagesEmptyState();
                            
                            // Notify adapter of all changes
                            messageAdapter.notifyDataSetChanged();
                            
                            if (!messageList.isEmpty()) {
                                // Scroll to bottom with smooth animation - ensure it happens after layout
                                recyclerMessages.post(() -> {
                                    recyclerMessages.smoothScrollToPosition(messageList.size() - 1);
                                    
                                    // Mark messages as read after scrolling to bottom (user has seen the conversation)
                                    android.os.Handler handler = new android.os.Handler();
                                    handler.postDelayed(() -> {
                                        markMessagesAsRead();
                                    }, 1000); // 1 second delay after loading messages
                                });
                            }
                        } else {
                            android.util.Log.e("LoadMessages", "API returned success=false");
                            android.util.Log.e("LoadMessages", "Error message: " + response.optString("message", "Unknown error"));
                            Toast.makeText(this, "Error loading messages: " + response.optString("message", "Unknown error"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        android.util.Log.e("LoadMessages", "JSON parsing error", e);
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing messages data", Toast.LENGTH_SHORT).show();
                    } finally {
                        // Hide progress dialog
                        if (showLoading) {
                            hideProgressDialog();
                        }
                    }
                },
                error -> {
                    android.util.Log.e("LoadMessages", "Network error", error);
                    android.util.Log.e("LoadMessages", "Error details: " + error.getMessage());
                    if (error.networkResponse != null) {
                        android.util.Log.e("LoadMessages", "Network response code: " + error.networkResponse.statusCode);
                        android.util.Log.e("LoadMessages", "Network response data: " + new String(error.networkResponse.data));
                    }
                    Toast.makeText(this, "Error loading messages: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    // Hide progress dialog
                    if (showLoading) {
                        hideProgressDialog();
                    }
                });

        requestQueue.add(request);
    }

    private void loadDummyMessages() {
        // Create dummy messages for testing
        messageList.clear();
        
        String chatName = getIntent().getStringExtra("chatName");
        String chatType = getIntent().getStringExtra("chatType");
        
        if (chatType.equals("individual")) {
            // Individual chat dummy messages
            // isReceiver = false means from other user (gray bubble)
            // isReceiver = true means from current user (brown bubble)
            messageList.add(new MessageModel(1, 2, 1, "Hey, how are you?", "2024-01-15 10:30:00", "Delivered", false, "John Doe", "You")); // Gray - from John
            messageList.add(new MessageModel(2, 1, 2, "I'm doing great! How about you?", "2024-01-15 10:32:00", "Read", true, "You", "John Doe")); // Brown - from you
            messageList.add(new MessageModel(3, 2, 1, "Good! Just working on some projects", "2024-01-15 10:35:00", "Delivered", false, "John Doe", "You")); // Gray - from John
            messageList.add(new MessageModel(4, 1, 2, "That sounds interesting! What kind of projects?", "2024-01-15 10:36:00", "Read", true, "You", "John Doe")); // Brown - from you
            messageList.add(new MessageModel(5, 2, 1, "Mobile app development. It's quite challenging but fun!", "2024-01-15 10:40:00", "Delivered", false, "John Doe", "You")); // Gray - from John
        } else {
            // Group chat dummy messages
            messageList.add(new MessageModel(1, 5, 0, "Hey everyone! How's it going?", "2024-01-15 09:00:00", "Delivered", false, "Sarah Wilson", "")); // Gray - from Sarah
            messageList.add(new MessageModel(2, 1, 0, "All good here! Thanks for asking", "2024-01-15 09:05:00", "Read", true, "You", "")); // Brown - from you
            messageList.add(new MessageModel(3, 6, 0, "Great! Just finished my morning workout", "2024-01-15 09:10:00", "Delivered", false, "David Brown", "")); // Gray - from David
            messageList.add(new MessageModel(4, 5, 0, "Nice! I need to start working out too", "2024-01-15 09:15:00", "Read", false, "Sarah Wilson", "")); // Gray - from Sarah
            messageList.add(new MessageModel(5, 1, 0, "Anyone up for a study session later?", "2024-01-15 09:20:00", "Read", true, "You", "")); // Brown - from you
        }
        
        messageAdapter.notifyDataSetChanged();
        if (!messageList.isEmpty()) {
            recyclerMessages.scrollToPosition(messageList.size() - 1);
        }
    }

    private void sendMessage(String messageText) {
        // Prevent duplicate sends
        if (isSendingMessage || messageText.equals(lastSentMessage)) {
            return;
        }
        
        // Apply profanity filter
        String originalMessage = messageText;
        String filteredMessage = ProfanityFilter.filterMessage(messageText);
        
        // Check if message was filtered
        if (!originalMessage.equals(filteredMessage)) {
            // Show dialog asking if user wants to send filtered or original message
            showProfanityFilterDialog(originalMessage, filteredMessage);
            return; // Don't send yet, wait for user's choice
        }
        
        // Use original message for sending (no profanity detected)
        sendMessageInternal(messageText);
    }
    
    private void showProfanityFilterDialog(String originalMessage, String filteredMessage) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("⚠️ Inappropriate Language Detected")
            .setMessage("Your message contains inappropriate language. Would you like to:\n\n" +
                       "• Send filtered message (profane words will be replaced)\n" +
                       "• Send original message anyway\n" +
                       "• Cancel")
            .setPositiveButton("Send Filtered", (dialog, which) -> {
                // Send filtered message
                sendMessageInternal(filteredMessage);
            })
            .setNeutralButton("Send Original", (dialog, which) -> {
                // Send original unfiltered message
                sendMessageInternal(originalMessage);
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                // Cancel sending
                dialog.dismiss();
            })
            .setCancelable(true)
            .show();
    }
    
    private void sendMessageInternal(String messageText) {
        
        isSendingMessage = true;
        lastSentMessage = messageText; // Track the message being sent
        
        // Format timestamp with date (e.g., Oct 5, 7:30 PM)
        String currentTime = new java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault()).format(new java.util.Date());
        
        // Create new message
        MessageModel newMessage;
        if (chatType.equals("individual")) {
            newMessage = new MessageModel(
                messageList.size() + 1,
                currentUserId,
                otherUserId,
                messageText,
                currentTime,
                "Sending",
                true, // is_receiver = true means it's from current user
                "You",
                otherUserName
            );
        } else {
            newMessage = new MessageModel(
                messageList.size() + 1,
                currentUserId,
                messageText,
                currentTime,
                "Sending",
                "You"
            );
            newMessage.setReceiver(true); // This is from current user
        }
        
        // Add message to list
        messageList.add(newMessage);
        
        // Update empty state to hide empty message view and show RecyclerView
        updateMessagesEmptyState();
        
        // Notify adapter of the new item (use notifyItemInserted for better performance)
        int newPosition = messageList.size() - 1;
        messageAdapter.notifyItemInserted(newPosition);
        
        // Clear input
        editMessage.setText("");
        
        // Scroll to bottom with smooth animation - ensure it happens after layout
        recyclerMessages.post(() -> {
            if (!messageList.isEmpty()) {
                recyclerMessages.smoothScrollToPosition(messageList.size() - 1);
            }
        });
        
        // Send message with FCM notification
        if (chatType.equals("individual")) {
            sendIndividualMessageWithNotification(messageText, newMessage);
        } else {
            sendGroupMessageWithNotification(messageText, newMessage);
        }
    }

    private void sendIndividualMessageWithNotification(String messageText, MessageModel messageModel) {
        String url = "https://boardease.calapebohol.com/send_message.php";

        android.util.Log.d("SendMessage", "=== SENDING INDIVIDUAL MESSAGE ===");
        android.util.Log.d("SendMessage", "URL: " + url);
        android.util.Log.d("SendMessage", "Sender ID: " + currentUserId);
        android.util.Log.d("SendMessage", "Receiver ID: " + otherUserId);
        android.util.Log.d("SendMessage", "Message: " + messageText);

        RequestQueue queue = Volley.newRequestQueue(this);

        // Create request parameters
        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("sender_id", String.valueOf(currentUserId));
        params.put("receiver_id", String.valueOf(otherUserId));
        params.put("message", messageText);

        android.util.Log.d("SendMessage", "Request params: " + params.toString());

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                new JSONObject(params),
                response -> {
                    try {
                        android.util.Log.d("SendMessage", "Response received: " + response.toString());
                        if (response.getBoolean("success")) {
                            android.util.Log.d("SendMessage", "Message sent successfully!");
                            messageModel.setStatus("Sent");
                            messageAdapter.notifyDataSetChanged();
                            Toast.makeText(this, "Message sent with notification!", Toast.LENGTH_SHORT).show();
                        } else {
                            android.util.Log.e("SendMessage", "Failed to send message: " + response.getString("message"));
                            messageModel.setStatus("Failed");
                            messageAdapter.notifyDataSetChanged();
                            Toast.makeText(this, "Failed: " + response.getString("message"), Toast.LENGTH_SHORT).show();
                        }

                        isSendingMessage = false;
                        lastSentMessage = "";

                    } catch (JSONException e) {
                        android.util.Log.e("SendMessage", "JSON parsing error", e);
                        messageModel.setStatus("Failed");
                        messageAdapter.notifyDataSetChanged();
                        Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();

                        isSendingMessage = false;
                        lastSentMessage = "";
                    }
                },
                error -> {
                    android.util.Log.e("SendMessage", "Network error", error);
                    android.util.Log.e("SendMessage", "Error details: " + error.toString());

                    if (error.networkResponse != null) {
                        android.util.Log.e("SendMessage", "Network response code: " + error.networkResponse.statusCode);
                        android.util.Log.e("SendMessage", "Network response data: " + new String(error.networkResponse.data));
                    }

                    messageModel.setStatus("Failed");
                    messageAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Error sending message: " + error.toString(), Toast.LENGTH_SHORT).show();

                    isSendingMessage = false;
                }
        );

        // ⭐ ADD TIMEOUT HERE ⭐
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                15000, // 15 seconds timeout (recommended for ngrok)
                com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        queue.add(request);
    }


    private void checkUserStatus() {
        if (chatType != null && chatType.equals("individual") && otherUserId != -1) {
            // Reuse get_chat_list to find the specific user's status since it contains is_online
            String url = "https://boardease.calapebohol.com/get_chat_list.php?user_id=" + currentUserId;
            
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            JSONArray chatsArray = data.getJSONArray("chats");
                            
                            for (int i = 0; i < chatsArray.length(); i++) {
                                JSONObject chatObj = chatsArray.getJSONObject(i);
                                if (chatObj.getString("chat_type").equals("individual")) {
                                    int id = chatObj.getInt("other_user_id");
                                    if (id == otherUserId) {
                                        boolean isOnline = false;
                                        if (chatObj.has("is_online") && !chatObj.isNull("is_online")) {
                                            isOnline = chatObj.getBoolean("is_online");
                                        } else if (chatObj.has("online_status") && !chatObj.isNull("online_status")) {
                                            String status = chatObj.getString("online_status");
                                            isOnline = "Active".equalsIgnoreCase(status) || "Online".equalsIgnoreCase(status);
                                        }
                                        updateOnlineStatus(isOnline);
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    // Ignore errors for polling
                });
                
            requestQueue.add(request);
        }
    }

    private void updateOnlineStatus(boolean isOnline) {
        View onlineStatus = findViewById(R.id.onlineStatus);
        if (onlineStatus != null) {
            onlineStatus.setVisibility(isOnline ? View.VISIBLE : View.GONE);
        }
    }

    private void sendGroupMessageWithNotification(String messageText, MessageModel messageModel) {
        String url = "https://boardease.calapebohol.com/send_group_message.php";

        android.util.Log.d("SendGroupMessage", "=== SENDING GROUP MESSAGE ===");
        android.util.Log.d("SendGroupMessage", "URL: " + url);
        android.util.Log.d("SendGroupMessage", "Sender ID: " + currentUserId);
        android.util.Log.d("SendGroupMessage", "Group ID: " + groupId);
        android.util.Log.d("SendGroupMessage", "Message: " + messageText);

        RequestQueue queue = Volley.newRequestQueue(this);

        // Create request parameters
        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("sender_id", String.valueOf(currentUserId));
        params.put("group_id", String.valueOf(groupId));
        params.put("message", messageText);

        android.util.Log.d("SendGroupMessage", "Request params: " + params.toString());

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                new JSONObject(params),
                response -> {
                    try {
                        android.util.Log.d("SendGroupMessage", "Response received: " + response.toString());
                        if (response.getBoolean("success")) {
                            android.util.Log.d("SendGroupMessage", "Group message sent successfully!");
                            messageModel.setStatus("Sent");
                            messageAdapter.notifyDataSetChanged();
                            Toast.makeText(this, "Group message sent with notifications!", Toast.LENGTH_SHORT).show();
                        } else {
                            android.util.Log.e("SendGroupMessage", "Failed to send group message: " + response.getString("message"));
                            messageModel.setStatus("Failed");
                            messageAdapter.notifyDataSetChanged();
                            Toast.makeText(this, "Failed: " + response.getString("message"), Toast.LENGTH_SHORT).show();
                        }

                        isSendingMessage = false;
                        lastSentMessage = "";

                    } catch (JSONException e) {
                        android.util.Log.e("SendGroupMessage", "JSON parsing error", e);
                        messageModel.setStatus("Failed");
                        messageAdapter.notifyDataSetChanged();
                        Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();

                        isSendingMessage = false;
                        lastSentMessage = "";
                    }
                },
                error -> {
                    android.util.Log.e("SendGroupMessage", "Network error", error);
                    android.util.Log.e("SendGroupMessage", "Error details: " + error.toString());

                    if (error.networkResponse != null) {
                        android.util.Log.e("SendGroupMessage", "Network response code: " + error.networkResponse.statusCode);
                        android.util.Log.e("SendGroupMessage", "Network response data: " + new String(error.networkResponse.data));
                    }

                    messageModel.setStatus("Failed");
                    messageAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Error sending group message: " + error.toString(), Toast.LENGTH_SHORT).show();

                    isSendingMessage = false;
                }
        );

        // ⭐ Apply timeout here (same as individual message) ⭐
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                15000, // 15 seconds timeout (ngrok-friendly)
                com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        queue.add(request);
    }


    private void showGroupMembers() {
        // Use get_group_members.php endpoint to get real group members
        String url = "https://boardease.calapebohol.com/get_group_members.php?group_id=" + groupId;
        
        android.util.Log.d("GroupMembers", "=== LOADING GROUP MEMBERS ===");
        android.util.Log.d("GroupMembers", "URL: " + url);
        android.util.Log.d("GroupMembers", "Group ID: " + groupId);
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        android.util.Log.d("GroupMembers", "Response received: " + response.toString());
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            JSONArray membersArray = data.getJSONArray("members");
                            android.util.Log.d("GroupMembers", "Found " + membersArray.length() + " members");
                            
                            // Create member names array
                            String[] memberNames = new String[membersArray.length()];
                            for (int i = 0; i < membersArray.length(); i++) {
                                JSONObject memberObj = membersArray.getJSONObject(i);
                                android.util.Log.d("GroupMembers", "Member " + i + ": " + memberObj.toString());
                                String memberName = memberObj.getString("full_name");
                                String memberType = memberObj.getString("user_type");
                                
                                // Format member name without online status
                                memberNames[i] = memberName + " - " + memberType;
                            }
                            
                            // Create dialog with real members
                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setTitle("Group Members (" + data.getInt("member_count") + ")");
                            
                            builder.setItems(memberNames, (dialog, which) -> {
                                // Handle member selection if needed
                                Toast.makeText(this, "Selected: " + memberNames[which], Toast.LENGTH_SHORT).show();
                            });
                            
                            // Check if current user is owner, if so, add "Add Member" button
                            if ("BH Owner".equals(currentUserType) || "Owner".equals(currentUserType)) {
                                builder.setNeutralButton("Add Member", (dialog, which) -> {
                                    showAddMemberDialog();
                                });
                            }
                            
                            builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
                            
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        } else {
                            android.util.Log.e("GroupMembers", "Failed to load group members: " + response.getString("message"));
                            Toast.makeText(this, "Error loading group members: " + response.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        android.util.Log.e("GroupMembers", "JSON parsing error", e);
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing group members data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    android.util.Log.e("GroupMembers", "Network error", error);
                    android.util.Log.e("GroupMembers", "Error details: " + error.getMessage());
                    if (error.networkResponse != null) {
                        android.util.Log.e("GroupMembers", "Network response code: " + error.networkResponse.statusCode);
                        android.util.Log.e("GroupMembers", "Network response data: " + new String(error.networkResponse.data));
                    }
                    Toast.makeText(this, "Error loading group members: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(request);
    }
    
    private void handleNewMessageReceived(android.content.Intent intent) {
        try {
            String chatType = intent.getStringExtra("chat_type");
            String messageText = intent.getStringExtra("message_text");
            String senderName = intent.getStringExtra("sender_name");
            String timestamp = intent.getStringExtra("timestamp");
            int senderId = intent.getIntExtra("sender_id", -1);
            int receiverId = intent.getIntExtra("receiver_id", -1);
            int groupId = intent.getIntExtra("group_id", -1);
            
            android.util.Log.d("Conversation", "Real-time message received: " + messageText);
            
            // Check if this message is for the current conversation
            boolean isForCurrentConversation = false;
            
            if ("individual".equals(chatType)) {
                // For individual chats, check if sender is the other user in this conversation
                isForCurrentConversation = (senderId == otherUserId && receiverId == currentUserId);
            } else if ("group".equals(chatType)) {
                // For group chats, check if this is the current group
                isForCurrentConversation = (groupId == this.groupId);
            }
            
            if (isForCurrentConversation) {
                android.util.Log.d("Conversation", "Message is for current conversation, adding to UI");
                
                // Format timestamp
                String formattedTime = new java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault()).format(new java.util.Date());
                
                // Apply profanity filter to incoming message
                String filteredIncomingMessage = ProfanityFilter.filterMessage(messageText);
                
                // Create new message model
                MessageModel newMessage;
                if ("individual".equals(chatType)) {
                    newMessage = new MessageModel(
                        messageList.size() + 1,
                        senderId,
                        receiverId,
                        filteredIncomingMessage,
                        formattedTime,
                        "Sent",
                        true, // is_receiver = true means it's from other user
                        senderName,
                        "You"
                    );
                } else {
                    newMessage = new MessageModel(
                        messageList.size() + 1,
                        senderId,
                        filteredIncomingMessage,
                        formattedTime,
                        "Sent",
                        senderName
                    );
                    newMessage.setReceiver(true); // This is from other user
                }
                
                // Add message to list on UI thread
                runOnUiThread(() -> {
                    messageList.add(newMessage);
                    
                    // Update empty state to ensure RecyclerView is visible
                    updateMessagesEmptyState();
                    
                    // Notify adapter of the new item
                    int newPosition = messageList.size() - 1;
                    messageAdapter.notifyItemInserted(newPosition);
                    
                    // Scroll to bottom with smooth animation - ensure it happens after layout
                    recyclerMessages.post(() -> {
                        if (!messageList.isEmpty()) {
                            recyclerMessages.smoothScrollToPosition(messageList.size() - 1);
                        }
                    });
                    
                    // Mark messages as read since user is in the conversation
                    markMessagesAsRead();
                });
            }
        } catch (Exception e) {
            android.util.Log.e("Conversation", "Error handling new message", e);
        }
    }
    
    private void updateMessagesEmptyState() {
        if (messageList.isEmpty()) {
            recyclerMessages.setVisibility(View.GONE);
            layoutEmptyMessages.setVisibility(View.VISIBLE);
        } else {
            recyclerMessages.setVisibility(View.VISIBLE);
            layoutEmptyMessages.setVisibility(View.GONE);
        }
    }
    
    private void scrollToBottom() {
        if (!messageList.isEmpty()) {
            recyclerMessages.post(() -> {
                // Ensure RecyclerView is laid out before scrolling
                if (recyclerMessages.getLayoutManager() != null) {
                    int lastPosition = messageList.size() - 1;
                    recyclerMessages.smoothScrollToPosition(lastPosition);
                    
                    // Also try immediate scroll as fallback for first message
                    android.os.Handler handler = new android.os.Handler();
                    handler.postDelayed(() -> {
                        if (recyclerMessages.getLayoutManager() != null) {
                            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerMessages.getLayoutManager();
                            if (layoutManager != null && lastPosition < messageList.size()) {
                                layoutManager.scrollToPositionWithOffset(lastPosition, 0);
                            }
                        }
                    }, 100);
                }
            });
        }
    }
    
    private void scrollToBottomWithDelay() {
        if (!messageList.isEmpty()) {
            android.util.Log.d("Conversation", "scrollToBottomWithDelay called - messageList size: " + messageList.size());
            // Add a small delay to ensure keyboard animation is complete
            recyclerMessages.postDelayed(() -> {
                android.util.Log.d("Conversation", "Executing delayed scroll to position: " + (messageList.size() - 1));
                recyclerMessages.smoothScrollToPosition(messageList.size() - 1);
            }, 300); // 300ms delay
        }
    }
    
    // Helper method to format time as "2:50 PM"
    private String formatTime(String timestamp) {
        try {
            // Parse the timestamp (assuming format like "2025-10-14 14:32:48")
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            java.util.Date date = inputFormat.parse(timestamp);
            
            // Format as "2:50 PM"
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("h:mm a");
            return outputFormat.format(date);
        } catch (Exception e) {
            // If parsing fails, return the original timestamp
            return timestamp;
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
    
    /**
     * Get the first letter of a string, handling empty/null strings
     */
    private String getFirstLetter(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "?";
        }
        String trimmed = text.trim();
        return trimmed.substring(0, 1).toUpperCase();
    }
    
    /**
     * Create a circular avatar with a letter (like Gmail)
     * @param letter The letter to display
     * @param size The size of the avatar in dp (will be converted to pixels)
     * @return A Drawable containing the circular letter avatar
     */
    private android.graphics.drawable.Drawable createLetterAvatar(String letter, int size) {
        // Convert dp to pixels
        float density = getResources().getDisplayMetrics().density;
        int sizePx = (int) (size * density);
        
        // Create bitmap
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        
        // Generate a color based on the letter (for consistency)
        int color = getColorForLetter(letter);
        
        // Draw circle background
        android.graphics.Paint circlePaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(color);
        float radius = sizePx / 2.0f;
        canvas.drawCircle(radius, radius, radius, circlePaint);
        
        // Draw letter
        android.graphics.Paint textPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(android.graphics.Color.WHITE);
        textPaint.setTextSize(sizePx * 0.5f); // Letter size is 50% of circle
        textPaint.setTextAlign(android.graphics.Paint.Align.CENTER);
        textPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        
        // Center the text vertically
        android.graphics.Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float textHeight = fontMetrics.bottom - fontMetrics.top;
        float textOffset = textHeight / 2 - fontMetrics.bottom;
        canvas.drawText(letter, radius, radius + textOffset, textPaint);
        
        return new android.graphics.drawable.BitmapDrawable(getResources(), bitmap);
    }
    
    /**
     * Generate a consistent color for a letter (like Gmail does)
     * Uses a simple hash function to map letters to colors
     */
    private int getColorForLetter(String letter) {
        // Array of nice colors (similar to Gmail's palette)
        int[] colors = {
            0xFF4285F4, // Blue
            0xFF34A853, // Green
            0xFFEA4335, // Red
            0xFFFBBC04, // Yellow
            0xFF9C27B0, // Purple
            0xFF00BCD4, // Cyan
            0xFFFF9800, // Orange
            0xFF795548, // Brown
            0xFF607D8B, // Blue Grey
            0xFFE91E63, // Pink
            0xFF3F51B5, // Indigo
            0xFF009688, // Teal
        };
        
        // Use the letter's character code to pick a color
        char ch = letter.charAt(0);
        int index = Math.abs(ch) % colors.length;
        return colors[index];
    }
    
    private void showAddMemberDialog() {
        String url = "https://boardease.calapebohol.com/get_non_group_members.php?group_id=" + groupId + "&current_user_id=" + currentUserId;
        
        showProgressDialog("Loading potential members...");
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    hideProgressDialog();
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            JSONArray usersArray = data.getJSONArray("users");
                            
                            if (usersArray.length() == 0) {
                                Toast.makeText(this, "No valid boarders found to add.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            
                            String[] userNames = new String[usersArray.length()];
                            int[] userIds = new int[usersArray.length()];
                            boolean[] checkedItems = new boolean[usersArray.length()];
                            
                            for (int i = 0; i < usersArray.length(); i++) {
                                JSONObject userObj = usersArray.getJSONObject(i);
                                String fullName = userObj.getString("full_name");
                                String bhName = "";
                                if (userObj.has("boarding_house_name") && !userObj.isNull("boarding_house_name")) {
                                    bhName = " (" + userObj.getString("boarding_house_name") + ")";
                                }
                                userNames[i] = fullName + bhName;
                                userIds[i] = userObj.getInt("user_id");
                                checkedItems[i] = false; // Default unchecked
                            }
                            
                            // Create multi-select dialog
                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setTitle("Add Users to Group");
                            
                            List<Integer> selectedUserIds = new ArrayList<>();
                            
                            builder.setMultiChoiceItems(userNames, checkedItems, (dialog, which, isChecked) -> {
                                if (isChecked) {
                                    selectedUserIds.add(userIds[which]);
                                } else {
                                    selectedUserIds.remove(Integer.valueOf(userIds[which]));
                                }
                            });
                            
                            builder.setPositiveButton("Add", (dialog, which) -> {
                                if (!selectedUserIds.isEmpty()) {
                                    addMembersToGroup(selectedUserIds);
                                } else {
                                    Toast.makeText(this, "No users selected", Toast.LENGTH_SHORT).show();
                                }
                            });
                            
                            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
                            
                            builder.create().show();
                            
                        } else {
                            Toast.makeText(this, "Error: " + response.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing users data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    hideProgressDialog();
                    Toast.makeText(this, "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });
        
        requestQueue.add(request);
    }
    
    private void addMembersToGroup(List<Integer> selectedUserIds) {
        String url = "https://boardease.calapebohol.com/add_group_members.php";
        
        showProgressDialog("Adding members...");
        
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("group_id", groupId);
            jsonBody.put("added_by", currentUserId);
            
            JSONArray idsArray = new JSONArray();
            for (int id : selectedUserIds) {
                idsArray.put(id);
            }
            jsonBody.put("member_ids", idsArray);
            
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                    response -> {
                        hideProgressDialog();
                        try {
                            if (response.getBoolean("success")) {
                                Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show();
                                // Refresh member list by showing it again? Or just stay on chat
                                // Maybe add a system message?
                                // For now, simple success toast.
                            } else {
                                Toast.makeText(this, "Failed: " + response.getString("message"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        hideProgressDialog();
                        Toast.makeText(this, "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            
            requestQueue.add(request);
            
        } catch (JSONException e) {
            hideProgressDialog();
            e.printStackTrace();
        }
    }

    private void showRenameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Group");
        // builder.setMessage("Enter the new name for this group chat:");
        
        // Input field
        final EditText input = new EditText(this);
        input.setText(chatUserName.getText()); // Pre-fill with current name
        input.setSingleLine(true);
        input.setPadding(40, 20, 40, 20); // Add padding for better look
        
        // Add layout params to give margins around edit text
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(40, 20, 40, 20); // Left, Top, Right, Bottom margins
        input.setLayoutParams(params);
        container.addView(input);
        
        builder.setView(container);
        
        // Buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(chatUserName.getText().toString())) {
                renameGroup(newName);
            } else if (newName.isEmpty()) {
                Toast.makeText(this, "Group name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        builder.create().show();
    }
    
    private void renameGroup(String newName) {
        String url = "https://boardease.calapebohol.com/update_group_name.php";
        
        showProgressDialog("Updating group name...");
        
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("group_id", groupId);
            jsonBody.put("new_name", newName);
            jsonBody.put("user_id", currentUserId);
            
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                    response -> {
                        hideProgressDialog();
                        try {
                            if (response.getBoolean("success")) {
                                // Update UI
                                chatUserName.setText(newName);
                                Toast.makeText(this, "Group name updated successfully", Toast.LENGTH_SHORT).show();
                                
                                // Send broadcast to update other screens if needed
                                Intent intent = new Intent("com.example.mock.GROUP_RENAMED");
                                intent.putExtra("group_id", groupId);
                                intent.putExtra("new_name", newName);
                                sendBroadcast(intent);
                                
                            } else {
                                Toast.makeText(this, "Failed: " + response.getString("message"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        hideProgressDialog();
                        Toast.makeText(this, "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            
            requestQueue.add(request);
            
        } catch (JSONException e) {
            hideProgressDialog();
            e.printStackTrace();
        }
    }

}
