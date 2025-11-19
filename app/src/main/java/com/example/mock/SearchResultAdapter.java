package com.example.mock;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.SearchResultViewHolder> {

    private Context context;
    private List<ProfileModel> searchResults;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(ProfileModel profile);
    }

    public SearchResultAdapter(Context context, List<ProfileModel> searchResults) {
        this.context = context;
        this.searchResults = searchResults;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public SearchResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_result, parent, false);
        return new SearchResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchResultViewHolder holder, int position) {
        ProfileModel profile = searchResults.get(position);
        
        // Show full name for search results with suffixes preserved
        String displayName = NameFormatter.formatFullName(profile.getName());
        holder.profileName.setText(displayName);
        
        // Load profile picture from URL if available, otherwise use default
        String profilePictureUrl = profile.getProfilePictureUrl();
        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
            String fullImageUrl = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/" + profilePictureUrl;
            Glide.with(context)
                    .load(fullImageUrl)
                    .placeholder(profile.getImageResId())
                    .error(profile.getImageResId())
                    .centerCrop()
                    .circleCrop()
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(profile.getImageResId());
        }
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(profile);
            }
        });
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
    }

    public static class SearchResultViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView profileName;

        public SearchResultViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            profileName = itemView.findViewById(R.id.profileName);
        }
    }
}

























