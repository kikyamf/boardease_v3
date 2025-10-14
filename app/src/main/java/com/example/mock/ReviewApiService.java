package com.example.mock;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ReviewApiService {
    private static final String TAG = "ReviewApiService";
    private static final String BASE_URL = "http://192.168.101.6/BoardEase2/";
    
    private Context context;
    private RequestQueue requestQueue;

    public ReviewApiService(Context context) {
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(context);
    }

    public interface ReviewApiCallback {
        void onSuccess(List<Review> reviews);
        void onError(String error);
    }

    public interface ReviewDetailsCallback {
        void onSuccess(Review review);
        void onError(String error);
    }

    public interface ReviewSummaryCallback {
        void onSuccess(ReviewSummary summary);
        void onError(String error);
    }

    public interface SimpleCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    /**
     * Get reviews with filtering
     */
    public void getReviews(int boardingHouseId, int ownerId, int boarderId, String rating, 
                          String status, String sortBy, ReviewApiCallback callback) {
        String url = BASE_URL + "get_reviews.php";
        
        // Build URL with parameters
        StringBuilder urlBuilder = new StringBuilder(url);
        urlBuilder.append("?");
        
        if (boardingHouseId > 0) {
            urlBuilder.append("boarding_house_id=").append(boardingHouseId).append("&");
        }
        if (ownerId > 0) {
            urlBuilder.append("owner_id=").append(ownerId).append("&");
        }
        if (boarderId > 0) {
            urlBuilder.append("boarder_id=").append(boarderId).append("&");
        }
        urlBuilder.append("rating=").append(rating).append("&");
        urlBuilder.append("status=").append(status).append("&");
        urlBuilder.append("sort_by=").append(sortBy).append("&");
        urlBuilder.append("limit=50&offset=0");

        StringRequest request = new StringRequest(
            Request.Method.GET,
            urlBuilder.toString(),
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "Reviews response: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            JSONObject data = jsonResponse.getJSONObject("data");
                            JSONArray reviewsArray = data.getJSONArray("reviews");
                            List<Review> reviews = new ArrayList<>();
                            
                            for (int i = 0; i < reviewsArray.length(); i++) {
                                JSONObject reviewObj = reviewsArray.getJSONObject(i);
                                Review review = new Review(
                                    reviewObj.optInt("review_id", 0),
                                    reviewObj.optString("boarder_name", ""),
                                    reviewObj.optString("boarding_house_name", ""),
                                    reviewObj.optString("room_name", ""),
                                    reviewObj.optInt("overall_rating", 0),
                                    reviewObj.optString("review_text", ""),
                                    reviewObj.optString("created_at", ""),
                                    reviewObj.optString("boarder_profile_picture", ""),
                                    reviewObj.optString("title", ""),
                                    reviewObj.optInt("cleanliness_rating", 0),
                                    reviewObj.optInt("location_rating", 0),
                                    reviewObj.optInt("value_rating", 0),
                                    reviewObj.optInt("amenities_rating", 0),
                                    reviewObj.optInt("safety_rating", 0),
                                    reviewObj.optInt("management_rating", 0),
                                    reviewObj.optDouble("average_rating", 0.0),
                                    reviewObj.optString("images", ""),
                                    reviewObj.optBoolean("would_recommend", false),
                                    reviewObj.optString("stay_duration", ""),
                                    reviewObj.optString("visit_type", ""),
                                    reviewObj.optString("status", ""),
                                    reviewObj.optInt("helpful_count", 0),
                                    reviewObj.optString("owner_response", ""),
                                    reviewObj.optString("owner_response_date", ""),
                                    reviewObj.optString("university", ""),
                                    reviewObj.optString("student_id", ""),
                                    reviewObj.optString("boarding_house_address", ""),
                                    reviewObj.optString("owner_name", "")
                                );
                                reviews.add(review);
                            }
                            
                            callback.onSuccess(reviews);
                        } else {
                            String error = jsonResponse.optString("error", "Failed to load reviews");
                            callback.onError(error);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing reviews response", e);
                        callback.onError("Error parsing response");
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Volley error (Reviews)", error);
                    callback.onError("Network error: " + error.getMessage());
                }
            }
        );

        requestQueue.add(request);
    }

    /**
     * Get review details
     */
    public void getReviewDetails(int reviewId, ReviewDetailsCallback callback) {
        String url = BASE_URL + "get_review_details.php?review_id=" + reviewId;

        StringRequest request = new StringRequest(
            Request.Method.GET,
            url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "Review details response: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            JSONObject reviewObj = jsonResponse.getJSONObject("review");
                            Review review = new Review(
                                reviewObj.optInt("review_id", 0),
                                reviewObj.optString("boarder_name", ""),
                                reviewObj.optString("boarding_house_name", ""),
                                reviewObj.optString("room_name", ""),
                                reviewObj.optInt("overall_rating", 0),
                                reviewObj.optString("review_text", ""),
                                reviewObj.optString("created_at", ""),
                                reviewObj.optString("boarder_profile_picture", ""),
                                reviewObj.optString("title", ""),
                                reviewObj.optInt("cleanliness_rating", 0),
                                reviewObj.optInt("location_rating", 0),
                                reviewObj.optInt("value_rating", 0),
                                reviewObj.optInt("amenities_rating", 0),
                                reviewObj.optInt("safety_rating", 0),
                                reviewObj.optInt("management_rating", 0),
                                reviewObj.optDouble("average_rating", 0.0),
                                reviewObj.optString("images", ""),
                                reviewObj.optBoolean("would_recommend", false),
                                reviewObj.optString("stay_duration", ""),
                                reviewObj.optString("visit_type", ""),
                                reviewObj.optString("status", ""),
                                reviewObj.optInt("helpful_count", 0),
                                reviewObj.optString("owner_response", ""),
                                reviewObj.optString("owner_response_date", ""),
                                reviewObj.optString("university", ""),
                                reviewObj.optString("student_id", ""),
                                reviewObj.optString("boarding_house_address", ""),
                                reviewObj.optString("owner_name", "")
                            );
                            callback.onSuccess(review);
                        } else {
                            String error = jsonResponse.optString("error", "Failed to load review details");
                            callback.onError(error);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing review details response", e);
                        callback.onError("Error parsing response");
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Volley error (Review Details)", error);
                    callback.onError("Network error: " + error.getMessage());
                }
            }
        );

        requestQueue.add(request);
    }

    /**
     * Submit new review
     */
    public void submitReview(int boarderId, int boardingHouseId, int bookingId, int overallRating,
                           int cleanlinessRating, int locationRating, int valueRating, int amenitiesRating,
                           int safetyRating, int managementRating, String title, String reviewText,
                           String images, boolean wouldRecommend, String stayDuration, String visitType,
                           SimpleCallback callback) {
        String url = BASE_URL + "submit_review.php";
        
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("boarder_id", boarderId);
            requestBody.put("boarding_house_id", boardingHouseId);
            requestBody.put("booking_id", bookingId);
            requestBody.put("overall_rating", overallRating);
            requestBody.put("cleanliness_rating", cleanlinessRating);
            requestBody.put("location_rating", locationRating);
            requestBody.put("value_rating", valueRating);
            requestBody.put("amenities_rating", amenitiesRating);
            requestBody.put("safety_rating", safetyRating);
            requestBody.put("management_rating", managementRating);
            requestBody.put("title", title);
            requestBody.put("review_text", reviewText);
            requestBody.put("images", new JSONArray()); // Empty array for now
            requestBody.put("would_recommend", wouldRecommend);
            requestBody.put("stay_duration", stayDuration);
            requestBody.put("visit_type", visitType);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating request body", e);
            callback.onError("Error creating request");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.POST,
            url,
            requestBody,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d(TAG, "Submit review response: " + response.toString());
                    try {
                        if (response.getBoolean("success")) {
                            String message = response.optString("message", "Review submitted successfully");
                            callback.onSuccess(message);
                        } else {
                            String error = response.optString("error", "Failed to submit review");
                            callback.onError(error);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing submit response", e);
                        callback.onError("Error parsing response");
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Volley error (Submit Review)", error);
                    callback.onError("Network error: " + error.getMessage());
                }
            }
        );

        requestQueue.add(request);
    }

    /**
     * Submit owner response to review
     */
    public void submitOwnerResponse(int reviewId, int ownerId, String responseText, SimpleCallback callback) {
        String url = BASE_URL + "submit_owner_response.php";
        
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("review_id", reviewId);
            requestBody.put("owner_id", ownerId);
            requestBody.put("response_text", responseText);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating request body", e);
            callback.onError("Error creating request");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.POST,
            url,
            requestBody,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d(TAG, "Submit owner response: " + response.toString());
                    try {
                        if (response.getBoolean("success")) {
                            String message = response.optString("message", "Response submitted successfully");
                            callback.onSuccess(message);
                        } else {
                            String error = response.optString("error", "Failed to submit response");
                            callback.onError(error);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response", e);
                        callback.onError("Error parsing response");
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Volley error (Submit Owner Response)", error);
                    callback.onError("Network error: " + error.getMessage());
                }
            }
        );

        requestQueue.add(request);
    }

    /**
     * Get reviews summary
     */
    public void getReviewsSummary(int boardingHouseId, int ownerId, String period, ReviewSummaryCallback callback) {
        String url = BASE_URL + "get_reviews_summary.php";
        
        // Build URL with parameters
        StringBuilder urlBuilder = new StringBuilder(url);
        urlBuilder.append("?");
        
        if (boardingHouseId > 0) {
            urlBuilder.append("boarding_house_id=").append(boardingHouseId).append("&");
        }
        if (ownerId > 0) {
            urlBuilder.append("owner_id=").append(ownerId).append("&");
        }
        urlBuilder.append("period=").append(period);

        StringRequest request = new StringRequest(
            Request.Method.GET,
            urlBuilder.toString(),
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "Reviews summary response: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            JSONObject summaryObj = jsonResponse.getJSONObject("summary");
                            ReviewSummary summary = new ReviewSummary(
                                summaryObj.optInt("total_reviews", 0),
                                summaryObj.optDouble("average_rating", 0.0),
                                summaryObj.optInt("rating_5", 0),
                                summaryObj.optInt("rating_4", 0),
                                summaryObj.optInt("rating_3", 0),
                                summaryObj.optInt("rating_2", 0),
                                summaryObj.optInt("rating_1", 0),
                                summaryObj.optInt("recommendation_count", 0),
                                summaryObj.optDouble("cleanliness_avg", 0.0),
                                summaryObj.optDouble("location_avg", 0.0),
                                summaryObj.optDouble("value_avg", 0.0),
                                summaryObj.optDouble("amenities_avg", 0.0),
                                summaryObj.optDouble("safety_avg", 0.0),
                                summaryObj.optDouble("management_avg", 0.0),
                                summaryObj.optString("most_common_visit_type", ""),
                                summaryObj.optString("average_stay_duration", "")
                            );
                            callback.onSuccess(summary);
                        } else {
                            String error = jsonResponse.optString("error", "Failed to load reviews summary");
                            callback.onError(error);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing reviews summary response", e);
                        callback.onError("Error parsing response");
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Volley error (Reviews Summary)", error);
                    callback.onError("Network error: " + error.getMessage());
                }
            }
        );

        requestQueue.add(request);
    }
}







