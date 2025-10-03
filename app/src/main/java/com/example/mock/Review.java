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

    // Getters
    public int getReviewId() { return reviewId; }
    public String getBoarderName() { return boarderName; }
    public String getBoardingHouseName() { return boardingHouseName; }
    public String getRoomNumber() { return roomNumber; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public String getReviewDate() { return reviewDate; }
    public String getProfilePicture() { return profilePicture; }

    // Setters
    public void setReviewId(int reviewId) { this.reviewId = reviewId; }
    public void setBoarderName(String boarderName) { this.boarderName = boarderName; }
    public void setBoardingHouseName(String boardingHouseName) { this.boardingHouseName = boardingHouseName; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public void setRating(int rating) { this.rating = rating; }
    public void setComment(String comment) { this.comment = comment; }
    public void setReviewDate(String reviewDate) { this.reviewDate = reviewDate; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
}



