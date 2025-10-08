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
        
        // Set location (shortened for carousel)
        holder.tvLocation.setText("Quezon City");
        
        // Set price
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
            Intent intent = new Intent(context, BoardingHouseDetailsActivity.class);
            intent.putExtra("boarding_house_id", boardingHouse.getBhId());
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
        TextView tvBoardingHouseName, tvLocation, tvPrice;
        ImageButton btnFavorite;
        
        public CarouselViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBoardingHouse = itemView.findViewById(R.id.imgBoardingHouse);
            tvBoardingHouseName = itemView.findViewById(R.id.tvBoardingHouseName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }
    }
}
