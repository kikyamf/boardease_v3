package com.example.mock;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ReviewsActivity extends AppCompatActivity {

    private static final String TAG = "ReviewsActivity";
    private static final String GET_REVIEWS_URL = "http://192.168.101.6/BoardEase2/get_reviews.php";

    private RecyclerView recyclerView;
    private LinearLayout emptyLayout;

    private ImageButton btnBack;
    private ReviewsAdapter adapter;
    private ArrayList<Review> reviews;
    private int userId;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews);

        // Get user ID from intent
        userId = getIntent().getIntExtra("user_id", 0);
        Log.d(TAG, "User ID: " + userId);

        initViews();
        setupRecyclerView();
        loadReviews();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        recyclerView = findViewById(R.id.recyclerView);
        emptyLayout = findViewById(R.id.emptyLayout);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        reviews = new ArrayList<>();
        adapter = new ReviewsAdapter(reviews, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadReviews() {
        showProgressDialog("Loading reviews...");

        StringRequest request = new StringRequest(Request.Method.POST, GET_REVIEWS_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        hideProgressDialog();
                        Log.d(TAG, "Server Response: " + response);
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            if (jsonResponse.getBoolean("success")) {
                                JSONArray reviewsArray = jsonResponse.getJSONArray("reviews");
                                reviews.clear();

                                for (int i = 0; i < reviewsArray.length(); i++) {
                                    JSONObject reviewObj = reviewsArray.getJSONObject(i);
                                    Review review = new Review(
                                            reviewObj.getInt("review_id"),
                                            reviewObj.getString("boarder_name"),
                                            reviewObj.getString("boarding_house_name"),
                                            reviewObj.getString("room_number"),
                                            reviewObj.getInt("rating"),
                                            reviewObj.getString("comment"),
                                            reviewObj.getString("review_date"),
                                            reviewObj.getString("profile_picture")
                                    );
                                    reviews.add(review);
                                }

                                adapter.notifyDataSetChanged();
                                updateEmptyState();
                            } else {
                                Toast.makeText(ReviewsActivity.this, "Failed to load reviews", Toast.LENGTH_SHORT).show();
                                updateEmptyState();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            Toast.makeText(ReviewsActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                            updateEmptyState();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        hideProgressDialog();
                        Log.e(TAG, "Volley Error: " + error.getMessage());
                        Toast.makeText(ReviewsActivity.this, "Error loading reviews", Toast.LENGTH_SHORT).show();
                        updateEmptyState();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", String.valueOf(userId));
                return params;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void updateEmptyState() {
        if (reviews.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
        }
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}



