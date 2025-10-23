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
    private String title;
    private String location;
    private String contactPhone;
    private String preferredDate;
    private String preferredTime;
    private String assignedTo;
    private String estimatedCost;
    private String actualCost;
    private String workStartedDate;
    private String workCompletedDate;
    private String notes;
    private String images;
    private String feedbackRating;
    private String feedbackComment;

    public MaintenanceRequest() {
        // Default constructor
    }

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

    public MaintenanceRequest(int requestId, String boarderName, String boardingHouseName, 
                            String roomNumber, String maintenanceType, String description, 
                            String requestDate, String status, String priority, String title, 
                            String location, String contactPhone, String preferredDate, 
                            String preferredTime, String assignedTo, String estimatedCost, 
                            String actualCost, String workStartedDate, String workCompletedDate, 
                            String notes, String images, String feedbackRating, String feedbackComment) {
        this.requestId = requestId;
        this.boarderName = boarderName;
        this.boardingHouseName = boardingHouseName;
        this.roomNumber = roomNumber;
        this.maintenanceType = maintenanceType;
        this.description = description;
        this.requestDate = requestDate;
        this.status = status;
        this.priority = priority;
        this.title = title;
        this.location = location;
        this.contactPhone = contactPhone;
        this.preferredDate = preferredDate;
        this.preferredTime = preferredTime;
        this.assignedTo = assignedTo;
        this.estimatedCost = estimatedCost;
        this.actualCost = actualCost;
        this.workStartedDate = workStartedDate;
        this.workCompletedDate = workCompletedDate;
        this.notes = notes;
        this.images = images;
        this.feedbackRating = feedbackRating;
        this.feedbackComment = feedbackComment;
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
    public String getTitle() { return title; }
    public String getLocation() { return location; }
    public String getContactPhone() { return contactPhone; }
    public String getPreferredDate() { return preferredDate; }
    public String getPreferredTime() { return preferredTime; }
    public String getAssignedTo() { return assignedTo; }
    public String getEstimatedCost() { return estimatedCost; }
    public String getActualCost() { return actualCost; }
    public String getWorkStartedDate() { return workStartedDate; }
    public String getWorkCompletedDate() { return workCompletedDate; }
    public String getNotes() { return notes; }
    public String getImages() { return images; }
    public String getFeedbackRating() { return feedbackRating; }
    public String getFeedbackComment() { return feedbackComment; }

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
    public void setTitle(String title) { this.title = title; }
    public void setLocation(String location) { this.location = location; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public void setPreferredDate(String preferredDate) { this.preferredDate = preferredDate; }
    public void setPreferredTime(String preferredTime) { this.preferredTime = preferredTime; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public void setEstimatedCost(String estimatedCost) { this.estimatedCost = estimatedCost; }
    public void setActualCost(String actualCost) { this.actualCost = actualCost; }
    public void setWorkStartedDate(String workStartedDate) { this.workStartedDate = workStartedDate; }
    public void setWorkCompletedDate(String workCompletedDate) { this.workCompletedDate = workCompletedDate; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setImages(String images) { this.images = images; }
    public void setFeedbackRating(String feedbackRating) { this.feedbackRating = feedbackRating; }
    public void setFeedbackComment(String feedbackComment) { this.feedbackComment = feedbackComment; }
}





























