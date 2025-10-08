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
import java.util.List;
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

        ReviewApiService apiService = new ReviewApiService(this);
        // Get reviews for this owner (assuming userId is owner_id)
        apiService.getReviews(0, userId, 0, "all", "published", "newest", new ReviewApiService.ReviewApiCallback() {
            @Override
            public void onSuccess(List<Review> reviewList) {
                hideProgressDialog();
                reviews.clear();
                reviews.addAll(reviewList);
                adapter.notifyDataSetChanged();
                updateEmptyState();
                Log.d(TAG, "Loaded " + reviewList.size() + " reviews");
            }

            @Override
            public void onError(String error) {
                hideProgressDialog();
                Log.e(TAG, "Error loading reviews: " + error);
                Toast.makeText(ReviewsActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
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




















