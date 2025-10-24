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

public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ViewHolder> {

    private ArrayList<Review> reviews;
    private Context context;

    public ReviewsAdapter(ArrayList<Review> reviews, Context context) {
        this.reviews = reviews;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviews.get(position);

        holder.tvBoarderName.setText(review.getBoarderName());
        holder.tvBoardingHouse.setText(review.getBoardingHouseName());
        holder.tvRoomNumber.setText("Room " + review.getRoomNumber());
        holder.tvComment.setText(review.getComment());
        holder.tvReviewDate.setText(review.getReviewDate());

        // Set rating stars
        setRatingStars(holder.ratingContainer, review.getRating());

        // Load profile picture
        if (review.getProfilePicture() != null && !review.getProfilePicture().isEmpty()) {
            Glide.with(context)
                    .load(review.getProfilePicture())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(holder.ivProfilePicture);
        } else {
            holder.ivProfilePicture.setImageResource(R.drawable.ic_person);
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
        ImageView ivProfilePicture;
        TextView tvBoarderName, tvBoardingHouse, tvRoomNumber, tvComment, tvReviewDate;
        LinearLayout ratingContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfilePicture = itemView.findViewById(R.id.ivProfilePicture);
            tvBoarderName = itemView.findViewById(R.id.tvBoarderName);
            tvBoardingHouse = itemView.findViewById(R.id.tvBoardingHouse);
            tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvReviewDate = itemView.findViewById(R.id.tvReviewDate);
            ratingContainer = itemView.findViewById(R.id.ratingContainer);
        }
    }
}





























