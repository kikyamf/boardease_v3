package com.example.mock;

public class MaintenanceRequest {
    private int requestId;
    private String boarderName;
    private String boardingHouseName;
    private String roomNumber;
    private String maintenanceType;
    private String description;
    private String requestDate;
    private String status;
    private String priority;

    public MaintenanceRequest(int requestId, String boarderName, String boardingHouseName, 
                            String roomNumber, String maintenanceType, String description, 
                            String requestDate, String status, String priority) {
        this.requestId = requestId;
        this.boarderName = boarderName;
        this.boardingHouseName = boardingHouseName;
        this.roomNumber = roomNumber;
        this.maintenanceType = maintenanceType;
        this.description = description;
        this.requestDate = requestDate;
        this.status = status;
        this.priority = priority;
    }

    // Getters
    public int getRequestId() { return requestId; }
    public String getBoarderName() { return boarderName; }
    public String getBoardingHouseName() { return boardingHouseName; }
    public String getRoomNumber() { return roomNumber; }
    public String getMaintenanceType() { return maintenanceType; }
    public String getDescription() { return description; }
    public String getRequestDate() { return requestDate; }
    public String getStatus() { return status; }
    public String getPriority() { return priority; }

    // Setters
    public void setRequestId(int requestId) { this.requestId = requestId; }
    public void setBoarderName(String boarderName) { this.boarderName = boarderName; }
    public void setBoardingHouseName(String boardingHouseName) { this.boardingHouseName = boardingHouseName; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public void setMaintenanceType(String maintenanceType) { this.maintenanceType = maintenanceType; }
    public void setDescription(String description) { this.description = description; }
    public void setRequestDate(String requestDate) { this.requestDate = requestDate; }
    public void setStatus(String status) { this.status = status; }
    public void setPriority(String priority) { this.priority = priority; }
}



