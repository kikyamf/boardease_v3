package com.example.mock.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.mock.R;
import java.util.List;

public class ImageCarouselAdapter extends RecyclerView.Adapter<ImageCarouselAdapter.ImageViewHolder> {
    
    private List<String> imageUrls;
    
    public ImageCarouselAdapter(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
    
    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_carousel, parent, false);
        return new ImageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);
        
        // Load image with Glide
        Glide.with(holder.itemView.getContext())
            .load(imageUrl)
            .placeholder(R.drawable.image_loading_placeholder)
            .error(R.drawable.image_loading_placeholder)
            .centerCrop()
            .into(holder.imageView);
    }
    
    @Override
    public int getItemCount() {
        return imageUrls != null ? imageUrls.size() : 0;
    }
    
    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        
        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgCarousel);
        }
    }
}

