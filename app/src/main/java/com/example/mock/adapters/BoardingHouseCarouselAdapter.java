package com.example.mock.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mock.R;

import java.util.List;

public class BoardingHouseCarouselAdapter extends RecyclerView.Adapter<BoardingHouseCarouselAdapter.CarouselViewHolder> {
    
    private Context context;
    private List<Listing> boardingHouses;
    private OnFavoriteClickListener onFavoriteClickListener;
    
    public interface OnFavoriteClickListener {
        void onFavoriteClick(Listing boardingHouse);
    }
    
    public BoardingHouseCarouselAdapter(Context context, List<Listing> boardingHouses, OnFavoriteClickListener favoriteListener) {
        this.context = context;
        this.boardingHouses = boardingHouses;
        this.onFavoriteClickListener = favoriteListener;
    }
    
    @NonNull
    @Override
    public CarouselViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_boarding_house_carousel, parent, false);
        return new CarouselViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CarouselViewHolder holder, int position) {
        Listing boardingHouse = boardingHouses.get(position);
        
        // Set boarding house data
        holder.tvBoardingHouseName.setText(boardingHouse.getName());
        holder.tvLocation.setText(boardingHouse.getLocation());
        holder.tvPrice.setText(boardingHouse.getPrice());
        holder.tvRating.setText(String.valueOf(boardingHouse.getRating()));
        
        // Set image
        if (boardingHouse.getImagePath() != null && !boardingHouse.getImagePath().isEmpty()) {
            Glide.with(context)
                    .load(boardingHouse.getImagePath())
                    .placeholder(R.drawable.sample_listing)
                    .error(R.drawable.sample_listing)
                    .into(holder.imgBoardingHouse);
        } else {
            holder.imgBoardingHouse.setImageResource(R.drawable.sample_listing);
        }
        
        // Set favorite button
        if (boardingHouse.isFavorite()) {
            holder.btnFavorite.setImageResource(R.drawable.ic_star_filled);
        } else {
            holder.btnFavorite.setImageResource(R.drawable.ic_star_empty);
        }
        
        // Set click listeners
        holder.btnFavorite.setOnClickListener(v -> {
            if (onFavoriteClickListener != null) {
                onFavoriteClickListener.onFavoriteClick(boardingHouse);
            }
        });
        
        holder.itemView.setOnClickListener(v -> {
            // TODO: Navigate to boarding house details
        });
    }
    
    @Override
    public int getItemCount() {
        return boardingHouses.size();
    }
    
    public static class CarouselViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBoardingHouse;
        TextView tvBoardingHouseName;
        TextView tvLocation;
        TextView tvPrice;
        TextView tvRating;
        ImageView btnFavorite;
        
        public CarouselViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBoardingHouse = itemView.findViewById(R.id.imgBoardingHouse);
            tvBoardingHouseName = itemView.findViewById(R.id.tvBoardingHouseName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvRating = itemView.findViewById(R.id.tvRating);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }
    }
}
