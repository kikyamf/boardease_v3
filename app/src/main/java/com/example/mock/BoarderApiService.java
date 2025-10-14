package com.example.mock;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BoarderApiService {
    private static final String TAG = "BoarderApiService";
    private static final String BASE_URL = "http://192.168.101.6/BoardEase2/";
    
    private Context context;
    private RequestQueue requestQueue;

    public BoarderApiService(Context context) {
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(context);
    }

    public interface BoarderApiCallback {
        void onSuccess(List<BoarderData> boarders);
        void onError(String error);
    }

    public interface BoarderHistoryApiCallback {
        void onSuccess(List<BoarderHistoryData> boardersHistory);
        void onError(String error);
    }

    /**
     * Get current active boarders for an owner
     */
    public void getCurrentBoarders(int userId, BoarderApiCallback callback) {
        String url = BASE_URL + "get_current_boarders.php";
        
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("user_id", userId);
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
                    Log.d(TAG, "Current boarders response: " + response.toString());
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray boardersArray = response.getJSONArray("boarders");
                            List<BoarderData> boarders = new ArrayList<>();
                            
                            for (int i = 0; i < boardersArray.length(); i++) {
                                JSONObject boarderObj = boardersArray.getJSONObject(i);
                                BoarderData boarder = new BoarderData(
                                    boarderObj.optInt("boarder_id", 0),
                                    boarderObj.optString("boarder_name", ""),
                                    boarderObj.optString("boarder_email", ""),
                                    boarderObj.optString("boarder_phone", ""),
                                    boarderObj.optString("boarding_house_name", ""),
                                    boarderObj.optString("room_number", ""),
                                    boarderObj.optString("rent_type", ""),
                                    boarderObj.optString("start_date", ""),
                                    boarderObj.optString("end_date", ""),
                                    boarderObj.optString("status", ""),
                                    boarderObj.optString("profile_picture", "")
                                );
                                boarders.add(boarder);
                            }
                            
                            callback.onSuccess(boarders);
                        } else {
                            String error = response.optString("error", "Failed to load current boarders");
                            callback.onError(error);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing current boarders response", e);
                        callback.onError("Error parsing response");
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Volley error (Current Boarders)", error);
                    callback.onError("Network error: " + error.getMessage());
                }
            }
        );

        requestQueue.add(request);
    }

    /**
     * Get boarders history for an owner
     */
    public void getBoardersHistory(int userId, BoarderHistoryApiCallback callback) {
        String url = BASE_URL + "get_boarders_history.php";
        
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("user_id", userId);
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
                    Log.d(TAG, "Boarders history response: " + response.toString());
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray historyArray = response.getJSONArray("boarders_history");
                            List<BoarderHistoryData> boardersHistory = new ArrayList<>();
                            
                            for (int i = 0; i < historyArray.length(); i++) {
                                JSONObject historyObj = historyArray.getJSONObject(i);
                                BoarderHistoryData boarderHistory = new BoarderHistoryData(
                                    historyObj.optString("boarder_name", ""),
                                    historyObj.optString("room_number", ""),
                                    historyObj.optString("start_date", ""),
                                    historyObj.optString("end_date", ""),
                                    historyObj.optString("status", ""),
                                    historyObj.optString("boarding_house_name", ""),
                                    historyObj.optString("rent_type", ""),
                                    historyObj.optString("profile_picture", "")
                                );
                                boardersHistory.add(boarderHistory);
                            }
                            
                            callback.onSuccess(boardersHistory);
                        } else {
                            String error = response.optString("error", "Failed to load boarders history");
                            callback.onError(error);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing boarders history response", e);
                        callback.onError("Error parsing response");
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Volley error (Boarders History)", error);
                    callback.onError("Network error: " + error.getMessage());
                }
            }
        );

        requestQueue.add(request);
    }

    /**
     * Get all boarders (current + history) for an owner
     */
    public void getAllBoarders(int userId, BoarderApiCallback callback) {
        String url = BASE_URL + "get_owner_boarders.php";
        
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("user_id", userId);
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
                    Log.d(TAG, "All boarders response: " + response.toString());
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray boardersArray = response.getJSONArray("boarders");
                            List<BoarderData> boarders = new ArrayList<>();
                            
                            for (int i = 0; i < boardersArray.length(); i++) {
                                JSONObject boarderObj = boardersArray.getJSONObject(i);
                                BoarderData boarder = new BoarderData(
                                    boarderObj.optInt("boarder_id", 0),
                                    boarderObj.optString("boarder_name", ""),
                                    boarderObj.optString("boarder_email", ""),
                                    boarderObj.optString("boarder_phone", ""),
                                    boarderObj.optString("boarding_house_name", ""),
                                    boarderObj.optString("room_number", ""),
                                    boarderObj.optString("rent_type", ""),
                                    boarderObj.optString("start_date", ""),
                                    boarderObj.optString("end_date", ""),
                                    boarderObj.optString("status", ""),
                                    boarderObj.optString("profile_picture", "")
                                );
                                boarders.add(boarder);
                            }
                            
                            callback.onSuccess(boarders);
                        } else {
                            String error = response.optString("error", "Failed to load boarders");
                            callback.onError(error);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing all boarders response", e);
                        callback.onError("Error parsing response");
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "Volley error (All Boarders)", error);
                    callback.onError("Network error: " + error.getMessage());
                }
            }
        );

        requestQueue.add(request);
    }
}







