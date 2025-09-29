package com.example.mock;

public class NotificationItemModel {
    private String title;   // e.g., "Payment Due"
    private String message; // e.g., "Your rent is due tomorrow."
    private String time;    // e.g., "5 min ago"
    private String type;    // e.g., "payment", "maintenance", "announcement"
    private boolean isHeader;

    // Constructor for headers (like "Today", "Earlier")
    public NotificationItemModel(String title, boolean isHeader) {
        this.title = title;
        this.isHeader = isHeader;
    }

    // Constructor for normal notifications
    public NotificationItemModel(String title, String message, String time, String type) {
        this.title = title;
        this.message = message;
        this.time = time;
        this.type = type;
        this.isHeader = false;
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getTime() {
        return time;
    }

    public String getType() {
        return type;
    }

    public boolean isHeader() {
        return isHeader;
    }
}

