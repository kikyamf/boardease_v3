package com.example.mock;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ReviewDialogHelper {
    private static final String TAG = "ReviewDialogHelper";
    private static final String BASE_URL = "https://boardease.calapebohol.com/";

    public interface ReviewCallback {
        void onReviewSubmitted(boolean success);
    }

    public static void showReviewDialog(Context context, BoarderBookingFragment.Booking booking, ReviewCallback callback) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_review, null);
            builder.setView(dialogView);

            // Initialize dialog views
            ImageButton btnClose = dialogView.findViewById(R.id.btnCloseReview);
            ImageView ivStar1 = dialogView.findViewById(R.id.ivStar1);
            ImageView ivStar2 = dialogView.findViewById(R.id.ivStar2);
            ImageView ivStar3 = dialogView.findViewById(R.id.ivStar3);
            ImageView ivStar4 = dialogView.findViewById(R.id.ivStar4);
            ImageView ivStar5 = dialogView.findViewById(R.id.ivStar5);
            TextView tvRatingText = dialogView.findViewById(R.id.tvRatingText);
            com.google.android.material.textfield.TextInputEditText etReviewComments = dialogView.findViewById(R.id.etReviewComments);
            com.google.android.material.button.MaterialButton btnSubmitReview = dialogView.findViewById(R.id.btnSubmitReview);

            // Array of star ImageViews
            ImageView[] stars = {ivStar1, ivStar2, ivStar3, ivStar4, ivStar5};
            final int[] currentRating = {0};

            // Function to validate and update button state
            Runnable updateSubmitButtonState = () -> {
                boolean hasRating = currentRating[0] > 0;
                String comments = etReviewComments.getText() != null ? etReviewComments.getText().toString().trim() : "";
                boolean hasComments = !comments.isEmpty();
                
                boolean isEnabled = hasRating && hasComments;
                btnSubmitReview.setEnabled(isEnabled);
                
                if (isEnabled) {
                    btnSubmitReview.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.blue));
                } else {
                    btnSubmitReview.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.blue_disabled));
                }
            };

            // Stars update logic
            Runnable updateStars = () -> {
                for (int i = 0; i < stars.length; i++) {
                    if (i < currentRating[0]) {
                        stars[i].setImageResource(R.drawable.ic_star_filled);
                        ImageViewCompat.setImageTintList(stars[i], ColorStateList.valueOf(0xFFFFC107));
                    } else {
                        stars[i].setImageResource(R.drawable.ic_star_empty);
                        ImageViewCompat.setImageTintList(stars[i], ColorStateList.valueOf(0xFFCCCCCC));
                    }
                }
                
                String ratingText;
                switch (currentRating[0]) {
                    case 1: ratingText = "Poor"; break;
                    case 2: ratingText = "Fair"; break;
                    case 3: ratingText = "Good"; break;
                    case 4: ratingText = "Very Good"; break;
                    case 5: ratingText = "Excellent"; break;
                    default: ratingText = "Tap to rate"; break;
                }
                tvRatingText.setText(ratingText);
                updateSubmitButtonState.run();
            };

            for (int i = 0; i < stars.length; i++) {
                final int rating = i + 1;
                stars[i].setOnClickListener(v -> {
                    currentRating[0] = rating;
                    updateStars.run();
                });
            }

            updateStars.run();

            etReviewComments.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateSubmitButtonState.run(); }
                @Override public void afterTextChanged(Editable s) {}
            });

            AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.show();

            btnClose.setOnClickListener(v -> dialog.dismiss());
            btnSubmitReview.setOnClickListener(v -> {
                if (!btnSubmitReview.isEnabled()) return;
                String comments = etReviewComments.getText().toString().trim();
                submitReview(context, booking.getBhId(), currentRating[0], comments, dialog, callback);
            });

        } catch (Exception e) {
            Log.e(TAG, "Error showing review dialog: " + e.getMessage());
            Toast.makeText(context, "Error showing review dialog", Toast.LENGTH_SHORT).show();
        }
    }

    private static void submitReview(Context context, int bhId, int rating, String comments, AlertDialog dialog, ReviewCallback callback) {
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
            int userId = Integer.parseInt(sharedPreferences.getString("user_id", "0"));

            if (userId == 0) {
                Toast.makeText(context, "Error: User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            ProgressDialog progressDialog = new ProgressDialog(context);
            progressDialog.setMessage("Submitting review...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            String url = BASE_URL + "submit_review.php";
            JSONObject requestBody = new JSONObject();
            requestBody.put("user_id", userId);
            requestBody.put("bh_id", bhId);
            requestBody.put("rating", rating);
            requestBody.put("comment", comments);

            JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    progressDialog.dismiss();
                    try {
                        if (response.getBoolean("success")) {
                            dialog.dismiss();
                            Toast.makeText(context, "Review submitted successfully!", Toast.LENGTH_SHORT).show();
                            if (callback != null) callback.onReviewSubmitted(true);
                        } else {
                            Toast.makeText(context, response.optString("error", "Failed to submit review"), Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(context, "Error parsing server response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, "Network error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("User-Agent", "BoardEase-Android-App");
                    headers.put("Accept", "application/json");
                    return headers;
                }
            };

            Volley.newRequestQueue(context).add(jsonRequest);

        } catch (Exception e) {
            Log.e(TAG, "Error submitting review: " + e.getMessage());
            Toast.makeText(context, "Error submitting review", Toast.LENGTH_SHORT).show();
        }
    }
}
