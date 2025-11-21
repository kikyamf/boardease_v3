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

public class MemberSelectionAdapter extends RecyclerView.Adapter<MemberSelectionAdapter.MemberViewHolder> {

    private Context context;
    private List<ProfileModel> memberList;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(ProfileModel profile);
    }

    public MemberSelectionAdapter(Context context, List<ProfileModel> memberList) {
        this.context = context;
        this.memberList = memberList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_member_selection, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        ProfileModel profile = memberList.get(position);
        // Format name to ensure suffixes are preserved
        String displayName = NameFormatter.formatFullName(profile.getName());
        holder.profileName.setText(displayName);
        
        // Load profile picture from URL if available, otherwise use default
        String profilePictureUrl = profile.getProfilePictureUrl();
        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
            // Reset background and scaleType for actual profile pictures
            holder.profileImage.setBackgroundResource(R.drawable.circle_bg);
            holder.profileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.profileImage.setPadding(0, 0, 0, 0);
            String fullImageUrl = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/" + profilePictureUrl;
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
        
        // Set user role
        String userRole = profile.getUserType();
        if (userRole != null) {
            if (userRole.equalsIgnoreCase("owner")) {
                holder.userRole.setText("🏠 Boarding House Owner");
            } else if (userRole.equalsIgnoreCase("boarder")) {
                holder.userRole.setText("🏠 Boarder");
            } else {
                holder.userRole.setText("👤 " + userRole);
            }
        } else {
            holder.userRole.setText("👤 User");
        }
        
        // Set boarding house info for boarders
        if (profile.getUserType() != null && profile.getUserType().equalsIgnoreCase("boarder") 
            && !profile.getBoardingHouseName().isEmpty()) {
            holder.boardingHouseInfo.setText("📍 " + profile.getBoardingHouseName());
            holder.boardingHouseInfo.setVisibility(View.VISIBLE);
        } else {
            holder.boardingHouseInfo.setVisibility(View.GONE);
        }
        
        // No background color change, just show/hide check mark
        holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
        
        // Show/hide selection indicator
        holder.selectionIndicator.setVisibility(profile.isSelected() ? 
            View.VISIBLE : View.GONE);
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(profile);
            }
        });
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView profileName;
        TextView userRole;
        TextView boardingHouseInfo;
        TextView selectionIndicator;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            profileName = itemView.findViewById(R.id.profileName);
            userRole = itemView.findViewById(R.id.userRole);
            boardingHouseInfo = itemView.findViewById(R.id.boardingHouseInfo);
            selectionIndicator = itemView.findViewById(R.id.selectionIndicator);
        }
    }
}

























