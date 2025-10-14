package com.example.mock;

public class ReviewSummary {
    private int totalReviews;
    private double averageRating;
    private int rating5;
    private int rating4;
    private int rating3;
    private int rating2;
    private int rating1;
    private int recommendationCount;
    private double cleanlinessAvg;
    private double locationAvg;
    private double valueAvg;
    private double amenitiesAvg;
    private double safetyAvg;
    private double managementAvg;
    private String mostCommonVisitType;
    private String averageStayDuration;

    public ReviewSummary() {
        // Default constructor
    }

    public ReviewSummary(int totalReviews, double averageRating, int rating5, int rating4, 
                        int rating3, int rating2, int rating1, int recommendationCount,
                        double cleanlinessAvg, double locationAvg, double valueAvg, 
                        double amenitiesAvg, double safetyAvg, double managementAvg,
                        String mostCommonVisitType, String averageStayDuration) {
        this.totalReviews = totalReviews;
        this.averageRating = averageRating;
        this.rating5 = rating5;
        this.rating4 = rating4;
        this.rating3 = rating3;
        this.rating2 = rating2;
        this.rating1 = rating1;
        this.recommendationCount = recommendationCount;
        this.cleanlinessAvg = cleanlinessAvg;
        this.locationAvg = locationAvg;
        this.valueAvg = valueAvg;
        this.amenitiesAvg = amenitiesAvg;
        this.safetyAvg = safetyAvg;
        this.managementAvg = managementAvg;
        this.mostCommonVisitType = mostCommonVisitType;
        this.averageStayDuration = averageStayDuration;
    }

    // Getters
    public int getTotalReviews() { return totalReviews; }
    public double getAverageRating() { return averageRating; }
    public int getRating5() { return rating5; }
    public int getRating4() { return rating4; }
    public int getRating3() { return rating3; }
    public int getRating2() { return rating2; }
    public int getRating1() { return rating1; }
    public int getRecommendationCount() { return recommendationCount; }
    public double getCleanlinessAvg() { return cleanlinessAvg; }
    public double getLocationAvg() { return locationAvg; }
    public double getValueAvg() { return valueAvg; }
    public double getAmenitiesAvg() { return amenitiesAvg; }
    public double getSafetyAvg() { return safetyAvg; }
    public double getManagementAvg() { return managementAvg; }
    public String getMostCommonVisitType() { return mostCommonVisitType; }
    public String getAverageStayDuration() { return averageStayDuration; }

    // Setters
    public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
    public void setRating5(int rating5) { this.rating5 = rating5; }
    public void setRating4(int rating4) { this.rating4 = rating4; }
    public void setRating3(int rating3) { this.rating3 = rating3; }
    public void setRating2(int rating2) { this.rating2 = rating2; }
    public void setRating1(int rating1) { this.rating1 = rating1; }
    public void setRecommendationCount(int recommendationCount) { this.recommendationCount = recommendationCount; }
    public void setCleanlinessAvg(double cleanlinessAvg) { this.cleanlinessAvg = cleanlinessAvg; }
    public void setLocationAvg(double locationAvg) { this.locationAvg = locationAvg; }
    public void setValueAvg(double valueAvg) { this.valueAvg = valueAvg; }
    public void setAmenitiesAvg(double amenitiesAvg) { this.amenitiesAvg = amenitiesAvg; }
    public void setSafetyAvg(double safetyAvg) { this.safetyAvg = safetyAvg; }
    public void setManagementAvg(double managementAvg) { this.managementAvg = managementAvg; }
    public void setMostCommonVisitType(String mostCommonVisitType) { this.mostCommonVisitType = mostCommonVisitType; }
    public void setAverageStayDuration(String averageStayDuration) { this.averageStayDuration = averageStayDuration; }
}







