package com.example.mock;

/**
 * ChatProfile - Data class representing a chat profile for the horizontal RecyclerView
 * Used in MessagesActivity to display active chat profiles
 */
public class ChatProfile {
    private String chatId;
    private String chatType; // "DM" or "GC"
    private String displayName;
    private String profileImageUrl;
    private String lastMessage;
    private long timestamp;
    private int unreadCount;
    
    // Default constructor
    public ChatProfile() {
    }
    
    // Constructor
    public ChatProfile(String chatId, String chatType, String displayName, String profileImageUrl,
                      String lastMessage, long timestamp, int unreadCount) {
        this.chatId = chatId;
        this.chatType = chatType;
        this.displayName = displayName;
        this.profileImageUrl = profileImageUrl;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.unreadCount = unreadCount;
    }
    
    // Getters and Setters
    public String getChatId() {
        return chatId;
    }
    
    public void setChatId(String chatId) {
        this.chatId = chatId;
    }
    
    public String getChatType() {
        return chatType;
    }
    
    public void setChatType(String chatType) {
        this.chatType = chatType;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getProfileImageUrl() {
        return profileImageUrl;
    }
    
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
    
    public String getLastMessage() {
        return lastMessage;
    }
    
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public int getUnreadCount() {
        return unreadCount;
    }
    
    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
