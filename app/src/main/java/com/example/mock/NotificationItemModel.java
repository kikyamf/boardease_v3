package com.example.mock;

public class NotificationItemModel {
    private int notifId;    // Database notification ID
    private String title;   // e.g., "Payment Due"
    private String message; // e.g., "Your rent is due tomorrow."
    private String time;    // e.g., "5 min ago"
    private String type;    // e.g., "payment", "maintenance", "announcement", "booking", "general"
    private String status;  // "read" or "unread"
    private String createdAt; // Full timestamp
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

    // Constructor for API notifications
    public NotificationItemModel(int notifId, String title, String message, String type, String status, String createdAt) {
        this.notifId = notifId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.status = status;
        this.createdAt = createdAt;
        this.time = formatTime(createdAt);
        this.isHeader = false;
    }

    // Helper method to format time
    private String formatTime(String timestamp) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            java.util.Date date = sdf.parse(timestamp);
            long timeDiff = System.currentTimeMillis() - date.getTime();
            
            long seconds = timeDiff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;
            
            if (days > 0) {
                return days + " day" + (days > 1 ? "s" : "") + " ago";
            } else if (hours > 0) {
                return hours + " hr" + (hours > 1 ? "s" : "") + " ago";
            } else if (minutes > 0) {
                return minutes + " min ago";
            } else {
                return "Just now";
            }
        } catch (Exception e) {
            return timestamp;
        }
    }

    // Getters
    public int getNotifId() {
        return notifId;
    }

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

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public boolean isHeader() {
        return isHeader;
    }

    public boolean isUnread() {
        return "unread".equals(status);
    }
}

