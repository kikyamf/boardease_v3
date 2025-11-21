package com.example.mock;

public class ChatModel {
    private String name;
    private String lastMessage;
    private String time;
    private int imageResId; // drawable resource (fallback if no profile picture URL)
    private String profilePictureUrl; // URL to profile picture from database
    private int chatId; // For individual chats: other user's ID, For group chats: group ID
    private String chatType; // "individual" or "group"
    private int unreadCount;
    private String lastMessageStatus; // Sent, Delivered, Read
    private int otherUserId; // For individual chats
    private String otherUserName; // For individual chats
    private String groupName; // For group chats
    private int groupId; // For group chats
    private int creatorId; // For group chats - ID of user who created the group
    private boolean isOnline; // For individual chats - whether the user is online

    // Constructor for individual chats
    public ChatModel(String name, String lastMessage, String time, int imageResId, 
                    int chatId, String chatType, int unreadCount, String lastMessageStatus,
                    int otherUserId, String otherUserName) {
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.imageResId = imageResId;
        this.chatId = chatId;
        this.chatType = chatType;
        this.unreadCount = unreadCount;
        this.lastMessageStatus = lastMessageStatus;
        this.otherUserId = otherUserId;
        this.otherUserName = otherUserName;
        this.profilePictureUrl = "";
        this.isOnline = false; // Default to offline
    }
    
    // Constructor for individual chats with profile picture
    public ChatModel(String name, String lastMessage, String time, int imageResId, 
                    int chatId, String chatType, int unreadCount, String lastMessageStatus,
                    int otherUserId, String otherUserName, String profilePictureUrl) {
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.imageResId = imageResId;
        this.chatId = chatId;
        this.chatType = chatType;
        this.unreadCount = unreadCount;
        this.lastMessageStatus = lastMessageStatus;
        this.otherUserId = otherUserId;
        this.otherUserName = otherUserName;
        this.profilePictureUrl = profilePictureUrl != null ? profilePictureUrl : "";
        this.isOnline = false; // Default to offline
    }

    // Constructor for group chats
    public ChatModel(String name, String lastMessage, String time, int imageResId, 
                    int chatId, String chatType, int unreadCount, String lastMessageStatus,
                    String groupName, int groupId) {
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.imageResId = imageResId;
        this.chatId = chatId;
        this.chatType = chatType;
        this.unreadCount = unreadCount;
        this.lastMessageStatus = lastMessageStatus;
        this.groupName = groupName;
        this.groupId = groupId;
        this.creatorId = -1; // Default to -1 if not set
        this.profilePictureUrl = "";
    }
    
    // Constructor for group chats with creator ID
    public ChatModel(String name, String lastMessage, String time, int imageResId, 
                    int chatId, String chatType, int unreadCount, String lastMessageStatus,
                    String groupName, int groupId, int creatorId) {
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.imageResId = imageResId;
        this.chatId = chatId;
        this.chatType = chatType;
        this.unreadCount = unreadCount;
        this.lastMessageStatus = lastMessageStatus;
        this.groupName = groupName;
        this.groupId = groupId;
        this.creatorId = creatorId;
        this.profilePictureUrl = "";
    }

    // Legacy constructor for backward compatibility
    public ChatModel(String name, String lastMessage, String time, int imageResId) {
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.imageResId = imageResId;
        this.chatType = "individual";
        this.unreadCount = 0;
    }

    // Getters
    public String getName() { return name; }
    public String getLastMessage() { return lastMessage; }
    public String getTime() { return time; }
    public int getImageResId() { return imageResId; }
    public String getProfilePictureUrl() { return profilePictureUrl; }
    public int getChatId() { return chatId; }
    public String getChatType() { return chatType; }
    public int getUnreadCount() { return unreadCount; }
    public String getLastMessageStatus() { return lastMessageStatus; }
    public int getOtherUserId() { return otherUserId; }
    public String getOtherUserName() { return otherUserName; }
    public String getGroupName() { return groupName; }
    public int getGroupId() { return groupId; }
    public int getCreatorId() { return creatorId; }
    public boolean isOnline() { return isOnline; }

    // Setters
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public void setTime(String time) { this.time = time; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
    public void setLastMessageStatus(String lastMessageStatus) { this.lastMessageStatus = lastMessageStatus; }
    public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl != null ? profilePictureUrl : ""; }
    public void setCreatorId(int creatorId) { this.creatorId = creatorId; }
    public void setOnline(boolean isOnline) { this.isOnline = isOnline; }
}

