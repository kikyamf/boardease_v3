package com.example.mock;

public class ProfileModel {
    private String name;
    private int imageResId; // drawable resource (can switch to URL if using Glide/Picasso)
    private int userId;
    private String userType; // "boarder" or "owner"
    private String email;
    private String phone;
    private boolean isOnline;
    private String lastSeen;
    private boolean selected = false;

    // Constructor for database data
    public ProfileModel(int userId, String name, String userType, String email, 
                       String phone, int imageResId, boolean isOnline, String lastSeen) {
        this.userId = userId;
        this.name = name;
        this.userType = userType;
        this.email = email;
        this.phone = phone;
        this.imageResId = imageResId;
        this.isOnline = isOnline;
        this.lastSeen = lastSeen;
    }

    // Legacy constructor for backward compatibility
    public ProfileModel(String name, int imageResId) {
        this.name = name;
        this.imageResId = imageResId;
        this.userType = "boarder";
        this.isOnline = false;
    }

    // Getters
    public String getName() { return name; }
    public int getImageResId() { return imageResId; }
    public int getUserId() { return userId; }
    public String getUserType() { return userType; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public boolean isOnline() { return isOnline; }
    public String getLastSeen() { return lastSeen; }
    public boolean isSelected() { return selected; }

    // Setters
    public void setOnline(boolean online) { isOnline = online; }
    public void setLastSeen(String lastSeen) { this.lastSeen = lastSeen; }
    public void setSelected(boolean selected) { this.selected = selected; }
}

