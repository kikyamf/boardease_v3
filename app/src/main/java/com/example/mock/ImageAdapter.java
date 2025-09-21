package com.example.mock;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private Context context;
    private ArrayList<Uri> imageUris;
    private OnImageRemoveListener removeListener;

    public interface OnImageRemoveListener {
        void onImageRemoved(int position);
    }

    public ImageAdapter(Context context, ArrayList<Uri> imageUris, OnImageRemoveListener listener) {
        this.context = context;
        this.imageUris = imageUris;
        this.removeListener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Uri imageUri = imageUris.get(position);
        holder.ivImage.setImageURI(imageUri);

        holder.btnDelete.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onImageRemoved(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        ImageButton btnDelete;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
