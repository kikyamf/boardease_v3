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
import com.example.mock.BoarderFavoriteFragment;
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
        
        // Set location (you might need to add location field to Listing model)
        holder.tvLocation.setText("Quezon City, Metro Manila"); // Placeholder
        
        // Set description (you might need to add description field to Listing model)
        holder.tvDescription.setText("Cozy and affordable boarding house with modern amenities. Perfect for students and working professionals.");
        
        // Set accommodation types (you might need to add accommodation types field to Listing model)
        holder.tvAccommodationTypes.setText("Private Rooms • Bed Spacer");
        
        // Set price (you might need to add price field to Listing model)
        holder.tvPrice.setText("₱3,500/month");
        
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
        
        // Show/hide delete button based on adapter configuration
        if (holder.btnDelete != null) {
            holder.btnDelete.setVisibility(showDeleteButton ? View.VISIBLE : View.GONE);
        }
        
        // Set favorite button click listener
        holder.btnFavorite.setOnClickListener(v -> {
            if (favoriteClickListener != null) {
                // Check if already in favorites
                boolean isCurrentlyFavorite = BoarderFavoriteFragment.isFavorite(context, boardingHouse.getBhId());
                favoriteClickListener.onFavoriteClick(boardingHouse, !isCurrentlyFavorite);
            }
        });
        
        // Set delete button click listener
        if (holder.btnDelete != null) {
            holder.btnDelete.setOnClickListener(v -> {
                if (deleteClickListener != null) {
                    deleteClickListener.onDeleteClick(boardingHouse);
                }
            });
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
