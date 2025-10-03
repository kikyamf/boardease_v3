package com.example.mock;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import java.util.List;

public class ListingAdapter extends RecyclerView.Adapter<ListingAdapter.ListingViewHolder> {

    private Context context;
    private List<Listing> listingList;
    private OnItemActionListener listener;

    // Interface for edit/delete/view actions
    public interface OnItemActionListener {
        void onEdit(Listing listing);
        void onDelete(Listing listing);
        void onView(Listing listing);
    }

    public ListingAdapter(Context context, List<Listing> listingList, OnItemActionListener listener) {
        this.context = context;
        this.listingList = listingList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_listing, parent, false);
        return new ListingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListingViewHolder holder, int position) {
        Listing listing = listingList.get(position);

        // Set boarding house name
        holder.tvListingTitle.setText(listing.getBhName());

        // Load image with Glide
        if (listing.getImagePath() != null && !listing.getImagePath().isEmpty()) {
            Glide.with(context)
                    .load(listing.getImagePath())
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(holder.ivListingImage);
        } else {
            holder.ivListingImage.setImageResource(R.drawable.ic_launcher_background);
        }

        // Edit action: send full Listing object
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(listing);
            }
        });

        // Delete action
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(listing);
            }
        });

        // Card click action - view rooms
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onView(listing);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listingList.size();
    }

    public static class ListingViewHolder extends RecyclerView.ViewHolder {
        ImageView ivListingImage;
        TextView tvListingTitle;
        ImageButton btnEdit, btnDelete;

        public ListingViewHolder(@NonNull View itemView) {
            super(itemView);
            ivListingImage = itemView.findViewById(R.id.ivListingImage);
            tvListingTitle = itemView.findViewById(R.id.tvListingTitle);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
