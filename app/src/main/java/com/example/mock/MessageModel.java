package com.example.mock;

public class MessageModel {
    private int messageId;
    private int senderId;
    private int receiverId;
    private String messageText;
    private String timestamp;
    private String status; // Sent, Delivered, Read
    private boolean isReceiver; // true = received, false = sent
    private String senderName;
    private String receiverName;

    // Constructor for individual messages
    public MessageModel(int messageId, int senderId, int receiverId, String messageText, 
                       String timestamp, String status, boolean isReceiver, 
                       String senderName, String receiverName) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageText = messageText;
        this.timestamp = timestamp;
        this.status = status;
        this.isReceiver = isReceiver;
        this.senderName = senderName;
        this.receiverName = receiverName;
    }

    // Constructor for group messages
    public MessageModel(int messageId, int senderId, String messageText, 
                       String timestamp, String status, String senderName) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.messageText = messageText;
        this.timestamp = timestamp;
        this.status = status;
        this.senderName = senderName;
        this.isReceiver = false; // Will be set based on current user
    }

    // Legacy constructor for backward compatibility
    public MessageModel(String message, boolean isReceiver) {
        this.messageText = message;
        this.isReceiver = isReceiver;
    }

    // Getters
    public int getMessageId() { return messageId; }
    public int getSenderId() { return senderId; }
    public int getReceiverId() { return receiverId; }
    public String getMessageText() { return messageText; }
    public String getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
    public boolean isReceiver() { return isReceiver; }
    public String getSenderName() { return senderName; }
    public String getReceiverName() { return receiverName; }

    // Setters
    public void setReceiver(boolean receiver) { isReceiver = receiver; }
    public void setStatus(String status) { this.status = status; }

    // Legacy getter for backward compatibility
    public String getMessage() { return messageText; }
}
