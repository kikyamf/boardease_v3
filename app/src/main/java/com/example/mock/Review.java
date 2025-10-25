package com.example.mock;

public class Review {
    private int reviewId;
    private String boarderName;
    private String boardingHouseName;
    private String roomNumber;
    private int rating;
    private String comment;
    private String reviewDate;
    private String profilePicture;
    private String title;
    private int cleanlinessRating;
    private int locationRating;
    private int valueRating;
    private int amenitiesRating;
    private int safetyRating;
    private int managementRating;
    private double averageRating;
    private String images;
    private boolean wouldRecommend;
    private String stayDuration;
    private String visitType;
    private String status;
    private int helpfulCount;
    private String ownerResponse;
    private String ownerResponseDate;
    private String university;
    private String studentId;
    private String boardingHouseAddress;
    private String ownerName;

    public Review() {
        // Default constructor
    }

    public Review(int reviewId, String boarderName, String boardingHouseName, 
                 String roomNumber, int rating, String comment, String reviewDate, String profilePicture) {
        this.reviewId = reviewId;
        this.boarderName = boarderName;
        this.boardingHouseName = boardingHouseName;
        this.roomNumber = roomNumber;
        this.rating = rating;
        this.comment = comment;
        this.reviewDate = reviewDate;
        this.profilePicture = profilePicture;
    }

    public Review(int reviewId, String boarderName, String boardingHouseName, 
                 String roomNumber, int rating, String comment, String reviewDate, String profilePicture,
                 String title, int cleanlinessRating, int locationRating, int valueRating, 
                 int amenitiesRating, int safetyRating, int managementRating, double averageRating,
                 String images, boolean wouldRecommend, String stayDuration, String visitType,
                 String status, int helpfulCount, String ownerResponse, String ownerResponseDate,
                 String university, String studentId, String boardingHouseAddress, String ownerName) {
        this.reviewId = reviewId;
        this.boarderName = boarderName;
        this.boardingHouseName = boardingHouseName;
        this.roomNumber = roomNumber;
        this.rating = rating;
        this.comment = comment;
        this.reviewDate = reviewDate;
        this.profilePicture = profilePicture;
        this.title = title;
        this.cleanlinessRating = cleanlinessRating;
        this.locationRating = locationRating;
        this.valueRating = valueRating;
        this.amenitiesRating = amenitiesRating;
        this.safetyRating = safetyRating;
        this.managementRating = managementRating;
        this.averageRating = averageRating;
        this.images = images;
        this.wouldRecommend = wouldRecommend;
        this.stayDuration = stayDuration;
        this.visitType = visitType;
        this.status = status;
        this.helpfulCount = helpfulCount;
        this.ownerResponse = ownerResponse;
        this.ownerResponseDate = ownerResponseDate;
        this.university = university;
        this.studentId = studentId;
        this.boardingHouseAddress = boardingHouseAddress;
        this.ownerName = ownerName;
    }

    // Getters
    public int getReviewId() { return reviewId; }
    public String getBoarderName() { return boarderName; }
    public String getBoardingHouseName() { return boardingHouseName; }
    public String getRoomNumber() { return roomNumber; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public String getReviewDate() { return reviewDate; }
    public String getProfilePicture() { return profilePicture; }
    public String getTitle() { return title; }
    public int getCleanlinessRating() { return cleanlinessRating; }
    public int getLocationRating() { return locationRating; }
    public int getValueRating() { return valueRating; }
    public int getAmenitiesRating() { return amenitiesRating; }
    public int getSafetyRating() { return safetyRating; }
    public int getManagementRating() { return managementRating; }
    public double getAverageRating() { return averageRating; }
    public String getImages() { return images; }
    public boolean isWouldRecommend() { return wouldRecommend; }
    public String getStayDuration() { return stayDuration; }
    public String getVisitType() { return visitType; }
    public String getStatus() { return status; }
    public int getHelpfulCount() { return helpfulCount; }
    public String getOwnerResponse() { return ownerResponse; }
    public String getOwnerResponseDate() { return ownerResponseDate; }
    public String getUniversity() { return university; }
    public String getStudentId() { return studentId; }
    public String getBoardingHouseAddress() { return boardingHouseAddress; }
    public String getOwnerName() { return ownerName; }

    // Setters
    public void setReviewId(int reviewId) { this.reviewId = reviewId; }
    public void setBoarderName(String boarderName) { this.boarderName = boarderName; }
    public void setBoardingHouseName(String boardingHouseName) { this.boardingHouseName = boardingHouseName; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public void setRating(int rating) { this.rating = rating; }
    public void setComment(String comment) { this.comment = comment; }
    public void setReviewDate(String reviewDate) { this.reviewDate = reviewDate; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
    public void setTitle(String title) { this.title = title; }
    public void setCleanlinessRating(int cleanlinessRating) { this.cleanlinessRating = cleanlinessRating; }
    public void setLocationRating(int locationRating) { this.locationRating = locationRating; }
    public void setValueRating(int valueRating) { this.valueRating = valueRating; }
    public void setAmenitiesRating(int amenitiesRating) { this.amenitiesRating = amenitiesRating; }
    public void setSafetyRating(int safetyRating) { this.safetyRating = safetyRating; }
    public void setManagementRating(int managementRating) { this.managementRating = managementRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public void setImages(String images) { this.images = images; }
    public void setWouldRecommend(boolean wouldRecommend) { this.wouldRecommend = wouldRecommend; }
    public void setStayDuration(String stayDuration) { this.stayDuration = stayDuration; }
    public void setVisitType(String visitType) { this.visitType = visitType; }
    public void setStatus(String status) { this.status = status; }
    public void setHelpfulCount(int helpfulCount) { this.helpfulCount = helpfulCount; }
    public void setOwnerResponse(String ownerResponse) { this.ownerResponse = ownerResponse; }
    public void setOwnerResponseDate(String ownerResponseDate) { this.ownerResponseDate = ownerResponseDate; }
    public void setUniversity(String university) { this.university = university; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setBoardingHouseAddress(String boardingHouseAddress) { this.boardingHouseAddress = boardingHouseAddress; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
}

































