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

public class BoardingHouseAdapter extends RecyclerView.Adapter<BoardingHouseAdapter.BoardingHouseViewHolder> {
    
    private Context context;
    private List<Listing> boardingHouses;
    private OnFavoriteClickListener onFavoriteClickListener;
    private OnDeleteClickListener onDeleteClickListener;
    private boolean showDeleteButton;
    
    public interface OnFavoriteClickListener {
        void onFavoriteClick(Listing boardingHouse);
    }
    
    public interface OnDeleteClickListener {
        void onDeleteClick(Listing boardingHouse);
    }
    
    public BoardingHouseAdapter(Context context, List<Listing> boardingHouses, OnFavoriteClickListener favoriteListener) {
        this.context = context;
        this.boardingHouses = boardingHouses;
        this.onFavoriteClickListener = favoriteListener;
        this.showDeleteButton = false;
    }
    
    public BoardingHouseAdapter(Context context, List<Listing> boardingHouses, OnFavoriteClickListener favoriteListener, OnDeleteClickListener deleteListener, boolean showDeleteButton) {
        this.context = context;
        this.boardingHouses = boardingHouses;
        this.onFavoriteClickListener = favoriteListener;
        this.onDeleteClickListener = deleteListener;
        this.showDeleteButton = showDeleteButton;
    }
    
    @NonNull
    @Override
    public BoardingHouseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_boarding_house, parent, false);
        return new BoardingHouseViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull BoardingHouseViewHolder holder, int position) {
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
        
        // Show/hide delete button
        if (showDeleteButton) {
            holder.btnDelete.setVisibility(View.VISIBLE);
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }
        
        // Set click listeners
        holder.btnFavorite.setOnClickListener(v -> {
            if (onFavoriteClickListener != null) {
                onFavoriteClickListener.onFavoriteClick(boardingHouse);
            }
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(boardingHouse);
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
    
    public static class BoardingHouseViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBoardingHouse;
        TextView tvBoardingHouseName;
        TextView tvLocation;
        TextView tvPrice;
        TextView tvRating;
        ImageView btnFavorite;
        ImageView btnDelete;
        
        public BoardingHouseViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBoardingHouse = itemView.findViewById(R.id.imgBoardingHouse);
            tvBoardingHouseName = itemView.findViewById(R.id.tvBoardingHouseName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvRating = itemView.findViewById(R.id.tvRating);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
