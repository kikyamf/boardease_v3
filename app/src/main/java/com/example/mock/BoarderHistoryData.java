package com.example.mock;

public class BoarderHistoryData {
    private String boarderName;
    private String roomName;
    private String startDate;
    private String endDate;
    private String status;
    private String boardingHouseName;
    private String rentType;
    private String profilePicture;

    public BoarderHistoryData() {
        // Default constructor
    }

    public BoarderHistoryData(String boarderName, String roomName, String startDate, String endDate, String status) {
        this.boarderName = boarderName;
        this.roomName = roomName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public BoarderHistoryData(String boarderName, String roomName, String startDate, String endDate, String status, 
                             String boardingHouseName, String rentType, String profilePicture) {
        this.boarderName = boarderName;
        this.roomName = roomName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.boardingHouseName = boardingHouseName;
        this.rentType = rentType;
        this.profilePicture = profilePicture;
    }

    // Getters
    public String getBoarderName() { return boarderName; }
    public String getRoomName() { return roomName; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getStatus() { return status; }
    public String getBoardingHouseName() { return boardingHouseName; }
    public String getRentType() { return rentType; }
    public String getProfilePicture() { return profilePicture; }

    // Setters
    public void setBoarderName(String boarderName) { this.boarderName = boarderName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public void setStatus(String status) { this.status = status; }
    public void setBoardingHouseName(String boardingHouseName) { this.boardingHouseName = boardingHouseName; }
    public void setRentType(String rentType) { this.rentType = rentType; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
}























