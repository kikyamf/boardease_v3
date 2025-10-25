package com.example.mock;

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
        chatId = getIntent().getIntExtra("chatId", -1);
        chatType = getIntent().getStringExtra("chatType");
        otherUserId = getIntent().getIntExtra("otherUserId", -1);
        otherUserName = getIntent().getStringExtra("otherUserName");
        groupId = getIntent().getIntExtra("groupId", -1);

        chatUserName.setText(name);
        chatProfileImage.setImageResource(imageRes);

        // Show/hide members button based on chat type
        if (chatType != null && chatType.equals("group")) {
            btnMembers.setVisibility(View.VISIBLE);
        } else {
            btnMembers.setVisibility(View.GONE);
        }

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Members button
        btnMembers.setOnClickListener(v -> showGroupMembers());

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

        // Load real messages from database
        loadMessages();

        // Send button
        btnSend.setOnClickListener(v -> {
            String msg = editMessage.getText().toString().trim();
            if (!msg.isEmpty() && !isSendingMessage && !msg.equals(lastSentMessage)) {
                sendMessage(msg);
            }
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
    
    @Override
    protected void onResume() {
        super.onResume();
        // Don't mark messages as read automatically - let user view them first
        // markMessagesAsRead();
    }
    
    private void markMessagesAsRead() {
        android.util.Log.d("Conversation", "markMessagesAsRead called for user: " + currentUserId);
        android.util.Log.d("Conversation", "Chat type: " + chatType);
        android.util.Log.d("Conversation", "Other user ID: " + otherUserId);
        android.util.Log.d("Conversation", "Group ID: " + groupId);
        
        String url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/mark_specific_messages_read.php";
        
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

    private void loadMessages() {
        String url = "";
        
        if (chatType.equals("individual")) {
            url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_messages.php?user1_id=" + currentUserId + "&user2_id=" + otherUserId;
            android.util.Log.d("LoadMessages", "=== LOADING INDIVIDUAL MESSAGES ===");
            android.util.Log.d("LoadMessages", "URL: " + url);
            android.util.Log.d("LoadMessages", "User1 ID: " + currentUserId);
            android.util.Log.d("LoadMessages", "User2 ID: " + otherUserId);
        } else {
            // Group chat
            url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_group_messages.php?group_id=" + groupId + "&current_user_id=" + currentUserId;
            android.util.Log.d("LoadMessages", "=== LOADING GROUP MESSAGES ===");
            android.util.Log.d("LoadMessages", "URL: " + url);
            android.util.Log.d("LoadMessages", "Group ID: " + groupId);
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            JSONArray messagesArray = data.getJSONArray("messages");
                            messageList.clear();
                            
                            for (int i = 0; i < messagesArray.length(); i++) {
                                JSONObject messageObj = messagesArray.getJSONObject(i);
                                MessageModel message;
                                
                                if (chatType.equals("individual")) {
                                    // Apply profanity filter to loaded message
                                    String messageText = ProfanityFilter.filterMessage(messageObj.getString("message"));
                                    
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
                            }
                            
                            messageAdapter.notifyDataSetChanged();
                            updateMessagesEmptyState();
                            if (!messageList.isEmpty()) {
                                // Scroll to bottom with smooth animation
                                scrollToBottom();
                                
                                // Mark messages as read after scrolling to bottom (user has seen the conversation)
                                android.os.Handler handler = new android.os.Handler();
                                handler.postDelayed(() -> {
                                    markMessagesAsRead();
                                }, 1000); // 1 second delay after loading messages
                            }
                        } else {
                            Toast.makeText(this, "Error loading messages: " + response.getString("error"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing messages data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(this, "Error loading messages: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
            // Show warning to user
            Toast.makeText(this, ProfanityFilter.getWarningMessage(), Toast.LENGTH_LONG).show();
        }
        
        // Use filtered message for sending
        messageText = filteredMessage;
        
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
        messageAdapter.notifyDataSetChanged();
        
        // Clear input
        editMessage.setText("");
        
        // Scroll to bottom with smooth animation
        scrollToBottom();
        
        // Send message with FCM notification
        if (chatType.equals("individual")) {
            sendIndividualMessageWithNotification(messageText, newMessage);
        } else {
            sendGroupMessageWithNotification(messageText, newMessage);
        }
    }

    private void sendIndividualMessageWithNotification(String messageText, MessageModel messageModel) {
        String url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/send_message.php";
        
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
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                response -> {
                    try {
                        android.util.Log.d("SendMessage", "Response received: " + response.toString());
                        if (response.getBoolean("success")) {
                            android.util.Log.d("SendMessage", "Message sent successfully!");
                            // Update message status to "Sent"
                            messageModel.setStatus("Sent");
                            messageAdapter.notifyDataSetChanged();
                            Toast.makeText(this, "Message sent with notification!", Toast.LENGTH_SHORT).show();
                        } else {
                            android.util.Log.e("SendMessage", "Failed to send message: " + response.getString("message"));
                            // Update message status to "Failed"
                            messageModel.setStatus("Failed");
                            messageAdapter.notifyDataSetChanged();
                            Toast.makeText(this, "Failed to send message: " + response.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                        // Reset sending flag and last sent message
                        isSendingMessage = false;
                        lastSentMessage = "";
                    } catch (JSONException e) {
                        android.util.Log.e("SendMessage", "JSON parsing error", e);
                        e.printStackTrace();
                        messageModel.setStatus("Failed");
                        messageAdapter.notifyDataSetChanged();
                        Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        // Reset sending flag and last sent message
                        isSendingMessage = false;
                        lastSentMessage = "";
                    }
                },
                error -> {
                    android.util.Log.e("SendMessage", "Network error", error);
                    android.util.Log.e("SendMessage", "Error details: " + error.getMessage());
                    if (error.networkResponse != null) {
                        android.util.Log.e("SendMessage", "Network response code: " + error.networkResponse.statusCode);
                        android.util.Log.e("SendMessage", "Network response data: " + new String(error.networkResponse.data));
                    }
                    // Update message status to "Failed"
                    messageModel.setStatus("Failed");
                    messageAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Error sending message: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    // Reset sending flag
                    isSendingMessage = false;
                });
        
        queue.add(request);
    }

    private void sendGroupMessageWithNotification(String messageText, MessageModel messageModel) {
        String url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/send_group_message.php";
        
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
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params),
                response -> {
                    try {
                        android.util.Log.d("SendGroupMessage", "Response received: " + response.toString());
                        if (response.getBoolean("success")) {
                            android.util.Log.d("SendGroupMessage", "Group message sent successfully!");
                            // Update message status to "Sent"
                            messageModel.setStatus("Sent");
                            messageAdapter.notifyDataSetChanged();
                            Toast.makeText(this, "Group message sent with notifications!", Toast.LENGTH_SHORT).show();
                        } else {
                            android.util.Log.e("SendGroupMessage", "Failed to send group message: " + response.getString("message"));
                            // Update message status to "Failed"
                            messageModel.setStatus("Failed");
                            messageAdapter.notifyDataSetChanged();
                            Toast.makeText(this, "Failed to send group message: " + response.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                        // Reset sending flag and last sent message
                        isSendingMessage = false;
                        lastSentMessage = "";
                    } catch (JSONException e) {
                        android.util.Log.e("SendGroupMessage", "JSON parsing error", e);
                        e.printStackTrace();
                        messageModel.setStatus("Failed");
                        messageAdapter.notifyDataSetChanged();
                        Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show();
                        // Reset sending flag and last sent message
                        isSendingMessage = false;
                        lastSentMessage = "";
                    }
                },
                error -> {
                    android.util.Log.e("SendGroupMessage", "Network error", error);
                    android.util.Log.e("SendGroupMessage", "Error details: " + error.getMessage());
                    if (error.networkResponse != null) {
                        android.util.Log.e("SendGroupMessage", "Network response code: " + error.networkResponse.statusCode);
                        android.util.Log.e("SendGroupMessage", "Network response data: " + new String(error.networkResponse.data));
                    }
                    // Update message status to "Failed"
                    messageModel.setStatus("Failed");
                    messageAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Error sending group message: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    // Reset sending flag
                    isSendingMessage = false;
                });
        
        // Set timeout for group messages (longer timeout)
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
            15000, // 15 seconds timeout
            com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        
        queue.add(request);
    }

    private void showGroupMembers() {
        // Use get_group_members.php endpoint to get real group members
        String url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_group_members.php?group_id=" + groupId;
        
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
                    messageAdapter.notifyDataSetChanged();
                    
                    // Scroll to bottom with smooth animation
                    scrollToBottom();
                    
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
                recyclerMessages.smoothScrollToPosition(messageList.size() - 1);
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
}
