package com.example.mock;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class OwnerReviewsAdapter extends RecyclerView.Adapter<OwnerReviewsAdapter.ViewHolder> {

    private ArrayList<Review> reviews;
    private Context context;

    public OwnerReviewsAdapter(ArrayList<Review> reviews, Context context) {
        this.reviews = reviews;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review_owner, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviews.get(position);

        // Set boarder name
        String boarderName = review.getBoarderName();
        if (boarderName == null || boarderName.trim().isEmpty()) {
            boarderName = "Anonymous";
        }
        holder.tvBoarderName.setText(boarderName);

        // Set boarding house name
        String boardingHouseName = review.getBoardingHouseName();
        if (boardingHouseName == null || boardingHouseName.trim().isEmpty()) {
            boardingHouseName = "Boarding House";
        }
        holder.tvBoardingHouse.setText(boardingHouseName);

        // Set room info - API already formats it as "Room Name: Room Number"
        String roomInfo = review.getRoomNumber(); // Contains formatted room info from API
        if (roomInfo != null && !roomInfo.trim().isEmpty()) {
            holder.tvRoomInfo.setText(roomInfo);
            holder.tvRoomInfo.setVisibility(View.VISIBLE);
        } else {
            holder.tvRoomInfo.setVisibility(View.GONE);
        }

        // Set comment
        String comment = review.getComment();
        if (comment == null || comment.trim().isEmpty()) {
            comment = "No comment provided.";
        }
        holder.tvComment.setText(comment);

        // Set review date
        String reviewDate = review.getReviewDate();
        if (reviewDate == null || reviewDate.trim().isEmpty()) {
            reviewDate = "N/A";
        }
        holder.tvReviewDate.setText(reviewDate);

        // Set rating stars (ensure rating is between 1-5)
        int rating = review.getRating();
        if (rating < 1) rating = 1;
        if (rating > 5) rating = 5;
        setRatingStars(holder.ratingContainer, rating);

        // Load profile picture
        if (review.getProfilePicture() != null && !review.getProfilePicture().isEmpty()) {
            Glide.with(context)
                    .load(review.getProfilePicture())
                    .placeholder(R.drawable.btn_profile)
                    .error(R.drawable.btn_profile)
                    .into(holder.ivProfilePicture);
        } else {
            holder.ivProfilePicture.setImageResource(R.drawable.btn_profile);
        }
    }

    private void setRatingStars(LinearLayout container, int rating) {
        container.removeAllViews();
        
        for (int i = 1; i <= 5; i++) {
            ImageView star = new ImageView(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) (24 * context.getResources().getDisplayMetrics().density),
                    (int) (24 * context.getResources().getDisplayMetrics().density)
            );
            params.setMargins(0, 0, (int) (4 * context.getResources().getDisplayMetrics().density), 0);
            star.setLayoutParams(params);
            
            if (i <= rating) {
                star.setImageResource(R.drawable.ic_star_filled);
                star.setColorFilter(context.getResources().getColor(android.R.color.holo_orange_dark));
            } else {
                star.setImageResource(R.drawable.ic_star_empty);
                star.setColorFilter(context.getResources().getColor(android.R.color.darker_gray));
            }
            
            container.addView(star);
        }
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CircularImageView ivProfilePicture;
        TextView tvBoarderName, tvBoardingHouse, tvRoomInfo, tvComment, tvReviewDate;
        LinearLayout ratingContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfilePicture = itemView.findViewById(R.id.ivProfilePicture);
            tvBoarderName = itemView.findViewById(R.id.tvBoarderName);
            tvBoardingHouse = itemView.findViewById(R.id.tvBoardingHouse);
            tvRoomInfo = itemView.findViewById(R.id.tvRoomInfo);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvReviewDate = itemView.findViewById(R.id.tvReviewDate);
            ratingContainer = itemView.findViewById(R.id.ratingContainer);
        }
    }
}

