package com.example.mock.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.mock.BoardingHouseDetailsActivity;
import com.example.mock.BoarderFavoriteFragment;
import com.example.mock.Listing;
import com.example.mock.R;
import java.util.List;

public class BoardingHouseCarouselAdapter extends RecyclerView.Adapter<BoardingHouseCarouselAdapter.CarouselViewHolder> {
    
    private Context context;
    private List<Listing> boardingHouseList;
    private OnFavoriteClickListener favoriteClickListener;
    
    public interface OnFavoriteClickListener {
        void onFavoriteClick(Listing boardingHouse, boolean isFavorite);
    }
    
    public BoardingHouseCarouselAdapter(Context context, List<Listing> boardingHouseList, OnFavoriteClickListener favoriteClickListener) {
        this.context = context;
        this.boardingHouseList = boardingHouseList;
        this.favoriteClickListener = favoriteClickListener;
    }
    
    @NonNull
    @Override
    public CarouselViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_boarding_house_carousel, parent, false);
        return new CarouselViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CarouselViewHolder holder, int position) {
        Listing boardingHouse = boardingHouseList.get(position);
        
        // Set boarding house name
        holder.tvBoardingHouseName.setText(boardingHouse.getBhName());
        
        // Set location using actual address (extract municipality/province for shorter display)
        String location = extractLocationForDisplay(boardingHouse.getBhAddress());
        holder.tvLocation.setText(location);
        
        // Set price using real data from database
        holder.tvPrice.setText(boardingHouse.getFormattedPrice());
        
        // Load image with Glide - construct full URL if needed
        String imagePath = boardingHouse.getImagePath();
        if (imagePath != null && !imagePath.isEmpty() && !imagePath.equals("null")) {
            // If image path doesn't start with http, it might be a relative path
            String fullImageUrl = imagePath;
            if (!imagePath.startsWith("http://") && !imagePath.startsWith("https://")) {
                // Construct full URL (assuming base URL structure)
                // Remove leading slash if present
                String cleanPath = imagePath.startsWith("/") ? imagePath.substring(1) : imagePath;
                // If path already contains 'uploads/', use as is, otherwise prepend it
                if (!cleanPath.startsWith("uploads/")) {
                    cleanPath = "uploads/" + cleanPath;
                }
                fullImageUrl = "https://boardease.calapebohol.com/" + cleanPath;
            }
            Glide.with(context)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.sample_listing)
                    .error(R.drawable.sample_listing)
                    .into(holder.imgBoardingHouse);
        } else {
            holder.imgBoardingHouse.setImageResource(R.drawable.sample_listing);
        }
        
        // Set Rating
        if (holder.ratingContainer != null && holder.tvRating != null) {
            double rating = boardingHouse.getAverageRating();
            if (rating > 0) {
                holder.ratingContainer.setVisibility(View.VISIBLE);
                holder.tvRating.setText(String.format("%.1f", rating));
            } else {
                holder.ratingContainer.setVisibility(View.GONE);
            }
        }
        
        // Set click listener for the entire card - use "bh_id" key to match BoardingHouseDetailsActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, BoardingHouseDetailsActivity.class);
            intent.putExtra("bh_id", boardingHouse.getBhId());
            intent.putExtra("boarding_house_name", boardingHouse.getBhName());
            intent.putExtra("boarding_house_image", boardingHouse.getImagePath());
            context.startActivity(intent);
        });
        
        // Set favorite button click listener
        holder.btnFavorite.setOnClickListener(v -> {
            if (favoriteClickListener != null) {
                // Check if already in favorites
                boolean isCurrentlyFavorite = BoarderFavoriteFragment.isFavorite(context, boardingHouse.getBhId());
                favoriteClickListener.onFavoriteClick(boardingHouse, !isCurrentlyFavorite);
            }
        });
    }
    
    /**
     * Extract a shortened location string from full address for carousel display
     * Format: "Municipality, Province" or just "Province" if municipality not available
     */
    private String extractLocationForDisplay(String fullAddress) {
        if (fullAddress == null || fullAddress.trim().isEmpty()) {
            return "Location not specified";
        }
        
        // Address format is typically: "Detailed Address, Barangay, Municipality, Province"
        // Split by comma and trim
        String[] parts = fullAddress.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        
        // If we have at least 2 parts, show "Municipality, Province"
        // If we have only 1 part, show that part
        if (parts.length >= 2) {
            // Municipality is usually second to last, Province is last
            String municipality = parts[parts.length - 2];
            String province = parts[parts.length - 1];
            return municipality + ", " + province;
        } else if (parts.length == 1) {
            return parts[0];
        } else {
            return fullAddress; // Fallback to full address
        }
    }
    
    @Override
    public int getItemCount() {
        return boardingHouseList != null ? boardingHouseList.size() : 0;
    }
    
    public void updateList(List<Listing> newList) {
        this.boardingHouseList = newList;
        notifyDataSetChanged();
    }
    
    public static class CarouselViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBoardingHouse;
        TextView tvBoardingHouseName, tvLocation, tvPrice, tvRating;
        ImageButton btnFavorite;
        View ratingContainer;
        
        public CarouselViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBoardingHouse = itemView.findViewById(R.id.imgBoardingHouse);
            tvBoardingHouseName = itemView.findViewById(R.id.tvBoardingHouseName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            tvRating = itemView.findViewById(R.id.tvRating);
            if (tvRating != null) {
                ratingContainer = (View) tvRating.getParent();
            }
        }
    }
}
