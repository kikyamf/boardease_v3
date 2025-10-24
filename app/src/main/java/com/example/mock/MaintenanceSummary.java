package com.example.mock;

public class MaintenanceSummary {
    private int totalRequests;
    private int pendingRequests;
    private int inProgressRequests;
    private int completedRequests;
    private int cancelledRequests;
    private double averageRating;
    private double totalCost;
    private String mostCommonType;
    private String averageCompletionTime;

    public MaintenanceSummary() {
        // Default constructor
    }

    public MaintenanceSummary(int totalRequests, int pendingRequests, int inProgressRequests, 
                            int completedRequests, int cancelledRequests, double averageRating, 
                            double totalCost, String mostCommonType, String averageCompletionTime) {
        this.totalRequests = totalRequests;
        this.pendingRequests = pendingRequests;
        this.inProgressRequests = inProgressRequests;
        this.completedRequests = completedRequests;
        this.cancelledRequests = cancelledRequests;
        this.averageRating = averageRating;
        this.totalCost = totalCost;
        this.mostCommonType = mostCommonType;
        this.averageCompletionTime = averageCompletionTime;
    }

    // Getters
    public int getTotalRequests() { return totalRequests; }
    public int getPendingRequests() { return pendingRequests; }
    public int getInProgressRequests() { return inProgressRequests; }
    public int getCompletedRequests() { return completedRequests; }
    public int getCancelledRequests() { return cancelledRequests; }
    public double getAverageRating() { return averageRating; }
    public double getTotalCost() { return totalCost; }
    public String getMostCommonType() { return mostCommonType; }
    public String getAverageCompletionTime() { return averageCompletionTime; }

    // Setters
    public void setTotalRequests(int totalRequests) { this.totalRequests = totalRequests; }
    public void setPendingRequests(int pendingRequests) { this.pendingRequests = pendingRequests; }
    public void setInProgressRequests(int inProgressRequests) { this.inProgressRequests = inProgressRequests; }
    public void setCompletedRequests(int completedRequests) { this.completedRequests = completedRequests; }
    public void setCancelledRequests(int cancelledRequests) { this.cancelledRequests = cancelledRequests; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
    public void setMostCommonType(String mostCommonType) { this.mostCommonType = mostCommonType; }
    public void setAverageCompletionTime(String averageCompletionTime) { this.averageCompletionTime = averageCompletionTime; }
}










