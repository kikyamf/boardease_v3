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
import com.example.mock.BoardingHouseDetailsActivitySimple;
import com.example.mock.Listing;
import com.example.mock.R;
import java.util.List;

public class BoardingHouseAdapter extends RecyclerView.Adapter<BoardingHouseAdapter.BoardingHouseViewHolder> {
    
    private Context context;
    private List<Listing> boardingHouseList;
    private OnFavoriteClickListener favoriteClickListener;
    private OnDeleteClickListener deleteClickListener;
    private OnBoardingHouseClickListener boardingHouseClickListener;
    private boolean showDeleteButton = false;
    
    public interface OnFavoriteClickListener {
        void onFavoriteClick(Listing boardingHouse, boolean isFavorite);
    }
    
    public interface OnDeleteClickListener {
        void onDeleteClick(Listing boardingHouse);
    }
    
    public interface OnBoardingHouseClickListener {
        void onBoardingHouseClick(Listing boardingHouse);
    }
    
    public BoardingHouseAdapter(Context context, List<Listing> boardingHouseList, OnFavoriteClickListener favoriteClickListener) {
        this.context = context;
        this.boardingHouseList = boardingHouseList;
        this.favoriteClickListener = favoriteClickListener;
        this.showDeleteButton = false;
    }
    
    public BoardingHouseAdapter(Context context, List<Listing> boardingHouseList, OnBoardingHouseClickListener boardingHouseClickListener) {
        this.context = context;
        this.boardingHouseList = boardingHouseList;
        this.boardingHouseClickListener = boardingHouseClickListener;
    }
    
    public BoardingHouseAdapter(Context context, List<Listing> boardingHouseList, OnFavoriteClickListener favoriteClickListener, OnDeleteClickListener deleteClickListener, boolean showDeleteButton) {
        this.context = context;
        this.boardingHouseList = boardingHouseList;
        this.favoriteClickListener = favoriteClickListener;
        this.deleteClickListener = deleteClickListener;
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
        Listing boardingHouse = boardingHouseList.get(position);
        
        // Set boarding house name
        holder.tvBoardingHouseName.setText(boardingHouse.getBhName());
        
        // Set location using real address data
        String location = boardingHouse.getBhAddress() != null && !boardingHouse.getBhAddress().isEmpty() 
            ? boardingHouse.getBhAddress() 
            : "Location not specified";
        holder.tvLocation.setText(location);
        
        // Set description using real description data
        String description = boardingHouse.getBhDescription() != null && !boardingHouse.getBhDescription().isEmpty()
            ? boardingHouse.getBhDescription()
            : "No description available";
        holder.tvDescription.setText(description);
        
        // Set accommodation types (placeholder for now - could be enhanced with room data)
        holder.tvAccommodationTypes.setText("Private Rooms • Bed Spacer");
        
        // Set price using real data from database
        holder.tvPrice.setText(boardingHouse.getFormattedPrice());
        
        // Load image with Glide
        if (boardingHouse.getImagePath() != null && !boardingHouse.getImagePath().isEmpty()) {
            Glide.with(context)
                    .load(boardingHouse.getImagePath())
                    .placeholder(R.drawable.sample_listing)
                    .error(R.drawable.sample_listing)
                    .into(holder.imgBoardingHouse);
        } else {
            holder.imgBoardingHouse.setImageResource(R.drawable.sample_listing);
        }
        
        // Set click listener for the entire card
        holder.itemView.setOnClickListener(v -> {
            if (boardingHouseClickListener != null) {
                // Use callback if provided (for guest mode)
                boardingHouseClickListener.onBoardingHouseClick(boardingHouse);
            } else {
                // Default behavior (navigate to details)
                Intent intent = new Intent(context, BoardingHouseDetailsActivity.class);
                intent.putExtra("boarding_house_id", boardingHouse.getBhId());
                intent.putExtra("boarding_house_name", boardingHouse.getBhName());
                intent.putExtra("boarding_house_image", boardingHouse.getImagePath());
                context.startActivity(intent);
            }
        });
        
        // Set favorite button click listener
        holder.btnFavorite.setOnClickListener(v -> {
            if (favoriteClickListener != null) {
                // Toggle favorite state (you might need to add isFavorite field to Listing model)
                boolean isFavorite = false; // Placeholder - get from model
                favoriteClickListener.onFavoriteClick(boardingHouse, !isFavorite);
            }
        });
        
        // Set delete button visibility and click listener
        if (showDeleteButton) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                if (deleteClickListener != null) {
                    deleteClickListener.onDeleteClick(boardingHouse);
                }
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE);
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
    
    public static class BoardingHouseViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBoardingHouse;
        TextView tvBoardingHouseName, tvLocation, tvDescription, tvAccommodationTypes, tvPrice;
        ImageButton btnFavorite, btnDelete;
        
        public BoardingHouseViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBoardingHouse = itemView.findViewById(R.id.imgBoardingHouse);
            tvBoardingHouseName = itemView.findViewById(R.id.tvBoardingHouseName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvAccommodationTypes = itemView.findViewById(R.id.tvAccommodationTypes);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
