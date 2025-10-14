package com.example.mock.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mock.R;

public class CarouselAdapter extends RecyclerView.Adapter<CarouselAdapter.CarouselViewHolder> {
    
    private int[] images;
    private String[] titles;
    
    public CarouselAdapter(int[] images, String[] titles) {
        this.images = images;
        this.titles = titles;
    }
    
    @NonNull
    @Override
    public CarouselViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_carousel, parent, false);
        return new CarouselViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CarouselViewHolder holder, int position) {
        holder.imageView.setImageResource(images[position]);
        holder.titleText.setText(titles[position]);
    }
    
    @Override
    public int getItemCount() {
        return images.length;
    }
    
    public static class CarouselViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleText;
        
        public CarouselViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.carouselImage);
            titleText = itemView.findViewById(R.id.carouselTitle);
        }
    }
}
