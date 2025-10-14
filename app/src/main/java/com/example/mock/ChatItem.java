package com.example.mock;

/**
 * ChatItem - Data class representing a chat conversation
 * Used in MessagesActivity to display chat list
 */
public class ChatItem {
    private String chatId;
    private String chatType; // "DM" or "GC"
    private String lastMessage;
    private long timestamp;
    private int unreadCount;
    
    // For Direct Messages
    private String otherParticipantId;
    private String otherParticipantName;
    private String otherParticipantImageUrl;
    
    // For Group Chats
    private String groupId;
    private String groupName;
    private String groupImageUrl;
    private String createdBy;
    
    // Default constructor
    public ChatItem() {
    }
    
    // Constructor for Direct Messages
    public ChatItem(String chatId, String chatType, String lastMessage, long timestamp, 
                   String otherParticipantId, String otherParticipantName, String otherParticipantImageUrl) {
        this.chatId = chatId;
        this.chatType = chatType;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.otherParticipantId = otherParticipantId;
        this.otherParticipantName = otherParticipantName;
        this.otherParticipantImageUrl = otherParticipantImageUrl;
        this.unreadCount = 0;
    }
    
    // Constructor for Group Chats
    public ChatItem(String chatId, String chatType, String lastMessage, long timestamp,
                   String groupId, String groupName, String groupImageUrl, String createdBy) {
        this.chatId = chatId;
        this.chatType = chatType;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.groupId = groupId;
        this.groupName = groupName;
        this.groupImageUrl = groupImageUrl;
        this.createdBy = createdBy;
        this.unreadCount = 0;
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
    
    public String getOtherParticipantId() {
        return otherParticipantId;
    }
    
    public void setOtherParticipantId(String otherParticipantId) {
        this.otherParticipantId = otherParticipantId;
    }
    
    public String getOtherParticipantName() {
        return otherParticipantName;
    }
    
    public void setOtherParticipantName(String otherParticipantName) {
        this.otherParticipantName = otherParticipantName;
    }
    
    public String getOtherParticipantImageUrl() {
        return otherParticipantImageUrl;
    }
    
    public void setOtherParticipantImageUrl(String otherParticipantImageUrl) {
        this.otherParticipantImageUrl = otherParticipantImageUrl;
    }
    
    public String getGroupId() {
        return groupId;
    }
    
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    
    public String getGroupImageUrl() {
        return groupImageUrl;
    }
    
    public void setGroupImageUrl(String groupImageUrl) {
        this.groupImageUrl = groupImageUrl;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
