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

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder> {

    private Context context;
    private List<ProfileModel> profileList;
    private boolean selectionMode = false;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(ProfileModel profile);
    }

    public ProfileAdapter(Context context, List<ProfileModel> profileList) {
        this.context = context;
        this.profileList = profileList;
    }

    public void setSelectionMode(boolean selectionMode) {
        this.selectionMode = selectionMode;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_profile, parent, false);
        return new ProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        ProfileModel profile = profileList.get(position);
        
        // Show full name for search results, first name for horizontal profiles
        String fullName = profile.getName();
        if (selectionMode) {
            // In search mode, show full name
            holder.profileName.setText(fullName);
        } else {
            // In horizontal profile mode, show full first name (all words except last name)
            String firstName = NameFormatter.getFullFirstName(fullName);
            holder.profileName.setText(firstName);
        }
        
        // Load profile picture from URL if available, otherwise use default
        String profilePictureUrl = profile.getProfilePictureUrl();
        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
            // Reset background and scaleType for actual profile pictures
            holder.profileImage.setBackgroundResource(R.drawable.circle_bg);
            holder.profileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.profileImage.setPadding(0, 0, 0, 0);
            String fullImageUrl = "https://boardease.calapebohol.com/" + profilePictureUrl;
            Glide.with(context)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.btn_profile)
                    .error(R.drawable.btn_profile)
                    .centerCrop()
                    .circleCrop()
                    .into(holder.profileImage);
        } else {
            // Use light gray background and fit icon inside circle for default profile picture
            holder.profileImage.setBackgroundResource(R.drawable.circle_bg_dark_gray);
            holder.profileImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
            holder.profileImage.setPadding(12, 12, 12, 12);
            holder.profileImage.setImageResource(R.drawable.btn_profile);
        }
        
        // Handle online status indicator
        boolean isOnline = profile.isOnline(); // Get online status from ProfileModel
        holder.onlineStatus.setVisibility(isOnline ? View.VISIBLE : View.GONE);
        
        // Handle selection mode
        if (selectionMode) {
            holder.itemView.setBackgroundColor(profile.isSelected() ? 
                context.getResources().getColor(android.R.color.holo_blue_light) : 
                context.getResources().getColor(android.R.color.transparent));
        } else {
            holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
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
        return profileList.size();
    }
    

    public static class ProfileViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView profileName;
        View onlineStatus;

        public ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            profileName = itemView.findViewById(R.id.profileName);
            onlineStatus = itemView.findViewById(R.id.onlineStatus);
        }
    }
}

