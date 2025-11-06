package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

interface ImageClickListener {
    void onImageClick();
}

/**
 * Adapter for ViewPager2 to display swipeable images
 */
public class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.ImageViewHolder> {
    
    private ArrayList<String> imageUrls;
    private ImageClickListener clickListener;
    
    public ImageSliderAdapter(ArrayList<String> imageUrls) {
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
    }
    
    public void setImageClickListener(ImageClickListener listener) {
        this.clickListener = listener;
    }
    
    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(parent.getContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return new ImageViewHolder(imageView);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        if (position < 0 || position >= imageUrls.size()) {
            android.util.Log.e("ImageSliderAdapter", "Invalid position: " + position);
            holder.imageView.setImageResource(R.drawable.sample_listing);
            return;
        }
        
        String imageUrl = imageUrls.get(position);
        
        android.util.Log.d("ImageSliderAdapter", "Loading image " + (position + 1) + " of " + imageUrls.size() + ": " + imageUrl);
        
        if (imageUrl == null || imageUrl.equals("placeholder") || imageUrl.isEmpty()) {
            holder.imageView.setImageResource(R.drawable.sample_listing);
            android.util.Log.d("ImageSliderAdapter", "Using placeholder for position " + position);
        } else {
            // Clear previous image first to prevent showing wrong image during recycling
            Glide.with(holder.imageView.getContext()).clear(holder.imageView);
            holder.imageView.setImageDrawable(null);
            
            // Load image with Glide - use listener to verify loading
            Glide.with(holder.imageView.getContext())
                .load(imageUrl)
                .placeholder(R.drawable.sample_listing)
                .error(R.drawable.sample_listing)
                .centerCrop()
                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                        android.util.Log.e("ImageSliderAdapter", "Failed to load image at position " + position + ": " + imageUrl, e);
                        return false;
                    }
                    
                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        android.util.Log.d("ImageSliderAdapter", "Successfully loaded image at position " + position + ": " + imageUrl);
                        return false;
                    }
                })
                .into(holder.imageView);
        }
        
        // Add click listener to ImageView
        holder.imageView.setOnClickListener(v -> {
            android.util.Log.d("ImageSliderAdapter", "ImageView clicked at position " + position);
            if (clickListener != null) {
                clickListener.onImageClick();
            }
        });
        
        // Make ImageView focusable and clickable
        holder.imageView.setFocusable(true);
        holder.imageView.setClickable(true);
    }
    
    @Override
    public long getItemId(int position) {
        // Return unique ID for each position to help with recycling
        if (position >= 0 && position < imageUrls.size()) {
            return imageUrls.get(position).hashCode();
        }
        return position;
    }
    
    @Override
    public int getItemCount() {
        return imageUrls.size();
    }
    
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        
        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = (ImageView) itemView;
        }
    }
}

