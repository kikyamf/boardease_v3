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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaintenanceApiService {
    private static final String TAG = "MaintenanceApiService";
    private static final String BASE_URL = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/";
    
    private Context context;
    private RequestQueue requestQueue;

    public MaintenanceApiService(Context context) {
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(context);
    }

    public interface MaintenanceApiCallback {
        void onSuccess(List<MaintenanceRequest> maintenanceRequests);
        void onError(String error);
    }

    public interface MaintenanceDetailsCallback {
        void onSuccess(MaintenanceRequest maintenanceRequest);
        void onError(String error);
    }

    public interface MaintenanceSummaryCallback {
        void onSuccess(MaintenanceSummary summary);
        void onError(String error);
    }

    public interface SimpleCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    /**
     * Get maintenance requests with filtering
     */
    public void getMaintenanceRequests(int userId, String userType, String status, String priority, String type, MaintenanceApiCallback callback) {
        String url = BASE_URL + "get_maintenance_requests.php";
        
        // Build URL with parameters
        StringBuilder urlBuilder = new StringBuilder(url);
        urlBuilder.append("?user_id=").append(userId);
        urlBuilder.append("&user_type=").append(userType);
        urlBuilder.append("&status=").append(status);
        urlBuilder.append("&priority=").append(priority);
        urlBuilder.append("&type=").append(type);
        urlBuilder.append("&limit=50&offset=0");

        StringRequest request = new StringRequest(
            Request.Method.GET,
            urlBuilder.toString(),
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "Maintenance requests response: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            JSONArray requestsArray = jsonResponse.getJSONArray("maintenance_requests");
                            List<MaintenanceRequest> maintenanceRequests = new ArrayList<>();
                            
                            for (int i = 0; i < requestsArray.length(); i++) {
                                JSONObject requestObj = requestsArray.getJSONObject(i);
                                MaintenanceRequest maintenanceRequest = new MaintenanceRequest(
                                    requestObj.optInt("maintenance_id", 0),
                                    requestObj.optString("boarder_name", ""),
                                    requestObj.optString("boarding_house_name", ""),
                                    requestObj.optString("room_number", ""),
                                    requestObj.optString("maintenance_type", ""),
                                    requestObj.optString("description", ""),
                                    requestObj.optString("request_date", ""),
                                    requestObj.optString("status", ""),
                                    requestObj.optString("priority", ""),
                                    requestObj.optString("title", ""),
                                    requestObj.optString("location", ""),
                                    requestObj.optString("contact_phone", ""),
                                    requestObj.optString("preferred_date", ""),
                                    requestObj.optString("preferred_time", ""),
                                    requestObj.optString("assigned_to", ""),
                                    requestObj.optString("estimated_cost", ""),
                                    requestObj.optString("actual_cost", ""),
                                    requestObj.optString("work_started_date", ""),
                                    requestObj.optString("work_completed_date", ""),
                                    requestObj.optString("notes", ""),
                                    requestObj.optString("images", ""),
                                    requestObj.optString("feedback_rating", ""),
                                    requestObj.optString("feedback_comment", "")
                                );
                                maintenanceRequests.add(maintenanceRequest);
                            }
                            
                            callback.onSuccess(maintenanceRequests);
                        } else {
                            String error = jsonResponse.optString("error", "Failed to load maintenance requests");
                            callback.onError(error);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing maintenance requests response", e);
                        callback.onError("Error parsing response");
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Volley error (Maintenance Requests)", error);
                    callback.onError("Network error: " + error.getMessage());
                }
            }
        );

        requestQueue.add(request);
    }

    /**
     * Get maintenance request details
     */
    public void getMaintenanceDetails(int maintenanceId, MaintenanceDetailsCallback callback) {
        String url = BASE_URL + "get_maintenance_details.php?maintenance_id=" + maintenanceId;

        StringRequest request = new StringRequest(
            Request.Method.GET,
            url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "Maintenance details response: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            JSONObject requestObj = jsonResponse.getJSONObject("maintenance_request");
                            MaintenanceRequest maintenanceRequest = new MaintenanceRequest(
                                requestObj.optInt("maintenance_id", 0),
                                requestObj.optString("boarder_name", ""),
                                requestObj.optString("boarding_house_name", ""),
                                requestObj.optString("room_number", ""),
                                requestObj.optString("maintenance_type", ""),
                                requestObj.optString("description", ""),
                                requestObj.optString("request_date", ""),
                                requestObj.optString("status", ""),
                                requestObj.optString("priority", ""),
                                requestObj.optString("title", ""),
                                requestObj.optString("location", ""),
                                requestObj.optString("contact_phone", ""),
                                requestObj.optString("preferred_date", ""),
                                requestObj.optString("preferred_time", ""),
                                requestObj.optString("assigned_to", ""),
                                requestObj.optString("estimated_cost", ""),
                                requestObj.optString("actual_cost", ""),
                                requestObj.optString("work_started_date", ""),
                                requestObj.optString("work_completed_date", ""),
                                requestObj.optString("notes", ""),
                                requestObj.optString("images", ""),
                                requestObj.optString("feedback_rating", ""),
                                requestObj.optString("feedback_comment", "")
                            );
                            callback.onSuccess(maintenanceRequest);
                        } else {
                            String error = jsonResponse.optString("error", "Failed to load maintenance details");
                            callback.onError(error);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing maintenance details response", e);
                        callback.onError("Error parsing response");
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Volley error (Maintenance Details)", error);
                    callback.onError("Network error: " + error.getMessage());
                }
            }
        );

        requestQueue.add(request);
    }

    /**
     * Submit new maintenance request
     */
    public void submitMaintenanceRequest(int boarderId, int roomId, String maintenanceType, String priority, 
                                       String title, String description, String location, String contactPhone, 
                                       String preferredDate, String preferredTime, SimpleCallback callback) {
        String url = BASE_URL + "submit_maintenance_request.php";
        
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("boarder_id", boarderId);
            requestBody.put("room_id", roomId);
            requestBody.put("maintenance_type", maintenanceType);
            requestBody.put("priority", priority);
            requestBody.put("title", title);
            requestBody.put("description", description);
            requestBody.put("location", location);
            requestBody.put("contact_phone", contactPhone);
            requestBody.put("preferred_date", preferredDate);
            requestBody.put("preferred_time", preferredTime);
            requestBody.put("images", new JSONArray()); // Empty array for now
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
                    Log.d(TAG, "Submit maintenance request response: " + response.toString());
                    try {
                        if (response.getBoolean("success")) {
                            String message = response.optString("message", "Maintenance request submitted successfully");
                            callback.onSuccess(message);
                        } else {
                            String error = response.optString("error", "Failed to submit maintenance request");
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
                    Log.e(TAG, "Volley error (Submit Maintenance Request)", error);
                    callback.onError("Network error: " + error.getMessage());
                }
            }
        );

        requestQueue.add(request);
    }

    /**
     * Update maintenance request status
     */
    public void updateMaintenanceStatus(int maintenanceId, String status, String assignedTo, 
                                      String estimatedCost, String actualCost, String workStartedDate, 
                                      String workCompletedDate, String notes, int updatedBy, SimpleCallback callback) {
        String url = BASE_URL + "update_maintenance_status.php";
        
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("maintenance_id", maintenanceId);
            requestBody.put("status", status);
            requestBody.put("assigned_to", assignedTo);
            requestBody.put("estimated_cost", estimatedCost);
            requestBody.put("actual_cost", actualCost);
            requestBody.put("work_started_date", workStartedDate);
            requestBody.put("work_completed_date", workCompletedDate);
            requestBody.put("notes", notes);
            requestBody.put("updated_by", updatedBy);
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
                    Log.d(TAG, "Update maintenance status response: " + response.toString());
                    try {
                        if (response.getBoolean("success")) {
                            String message = response.optString("message", "Maintenance status updated successfully");
                            callback.onSuccess(message);
                        } else {
                            String error = response.optString("error", "Failed to update maintenance status");
                            callback.onError(error);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing update response", e);
                        callback.onError("Error parsing response");
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Volley error (Update Maintenance Status)", error);
                    callback.onError("Network error: " + error.getMessage());
                }
            }
        );

        requestQueue.add(request);
    }

    /**
     * Submit maintenance feedback
     */
    public void submitMaintenanceFeedback(int maintenanceId, int rating, String comment, int feedbackBy, SimpleCallback callback) {
        String url = BASE_URL + "submit_maintenance_feedback.php";
        
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("maintenance_id", maintenanceId);
            requestBody.put("rating", rating);
            requestBody.put("comment", comment);
            requestBody.put("feedback_by", feedbackBy);
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
                    Log.d(TAG, "Submit feedback response: " + response.toString());
                    try {
                        if (response.getBoolean("success")) {
                            String message = response.optString("message", "Feedback submitted successfully");
                            callback.onSuccess(message);
                        } else {
                            String error = response.optString("error", "Failed to submit feedback");
                            callback.onError(error);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing feedback response", e);
                        callback.onError("Error parsing response");
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Volley error (Submit Feedback)", error);
                    callback.onError("Network error: " + error.getMessage());
                }
            }
        );

        requestQueue.add(request);
    }

    /**
     * Get maintenance summary
     */
    public void getMaintenanceSummary(int userId, String userType, String period, MaintenanceSummaryCallback callback) {
        String url = BASE_URL + "get_maintenance_summary.php";
        
        // Build URL with parameters
        StringBuilder urlBuilder = new StringBuilder(url);
        urlBuilder.append("?user_id=").append(userId);
        urlBuilder.append("&user_type=").append(userType);
        urlBuilder.append("&period=").append(period);

        StringRequest request = new StringRequest(
            Request.Method.GET,
            urlBuilder.toString(),
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, "Maintenance summary response: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            JSONObject summaryObj = jsonResponse.getJSONObject("summary");
                            MaintenanceSummary summary = new MaintenanceSummary(
                                summaryObj.optInt("total_requests", 0),
                                summaryObj.optInt("pending_requests", 0),
                                summaryObj.optInt("in_progress_requests", 0),
                                summaryObj.optInt("completed_requests", 0),
                                summaryObj.optInt("cancelled_requests", 0),
                                summaryObj.optDouble("average_rating", 0.0),
                                summaryObj.optDouble("total_cost", 0.0),
                                summaryObj.optString("most_common_type", ""),
                                summaryObj.optString("average_completion_time", "")
                            );
                            callback.onSuccess(summary);
                        } else {
                            String error = jsonResponse.optString("error", "Failed to load maintenance summary");
                            callback.onError(error);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing maintenance summary response", e);
                        callback.onError("Error parsing response");
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Volley error (Maintenance Summary)", error);
                    callback.onError("Network error: " + error.getMessage());
                }
            }
        );

        requestQueue.add(request);
    }
}









