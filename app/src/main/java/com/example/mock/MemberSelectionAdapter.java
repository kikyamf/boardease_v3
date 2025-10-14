package com.example.mock;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        holder.profileName.setText(profile.getName());
        holder.profileImage.setImageResource(profile.getImageResId());
        
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

























