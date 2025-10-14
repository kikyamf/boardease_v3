package com.example.mock.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mock.ChatProfile;
import com.example.mock.R;

import java.util.List;

/**
 * ProfileAdapter - Adapter for horizontal profile RecyclerView in MessagesActivity
 * Displays active chat profiles with circular profile images
 */
public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder> {

    private List<ChatProfile> profiles;
    private OnProfileClickListener onProfileClickListener;

    public static interface OnProfileClickListener {
        void onProfileClick(ChatProfile profile);
    }

    public ProfileAdapter(List<ChatProfile> profiles, OnProfileClickListener listener) {
        this.profiles = profiles;
        this.onProfileClickListener = listener;
    }

    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile, parent, false);
        return new ProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        try {
            ChatProfile profile = profiles.get(position);
            holder.bind(profile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return profiles != null ? profiles.size() : 0;
    }

    public class ProfileViewHolder extends RecyclerView.ViewHolder {
        private ImageView profileImage;
        private TextView profileName;
        private TextView unreadBadge;

        public ProfileViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            profileName = itemView.findViewById(R.id.profileName);
            unreadBadge = itemView.findViewById(R.id.unreadBadge);

            // Set click listener
            itemView.setOnClickListener(v -> {
                try {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && onProfileClickListener != null) {
                        onProfileClickListener.onProfileClick(profiles.get(position));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        public void bind(ChatProfile profile) {
            try {
                // Set profile name
                if (profileName != null) {
                    profileName.setText(profile.getDisplayName());
                }

                // Set profile image (you can use Glide or Picasso for image loading)
                if (profileImage != null) {
                    // For now, set a default image
                    profileImage.setImageResource(R.drawable.ic_person1);
                    
                    // TODO: Load image from URL using Glide or Picasso
                    // Glide.with(itemView.getContext())
                    //     .load(profile.getProfileImageUrl())
                    //     .placeholder(R.drawable.ic_person1)
                    //     .into(profileImage);
                }

                // Show unread count badge
                if (unreadBadge != null) {
                    if (profile.getUnreadCount() > 0) {
                        unreadBadge.setVisibility(View.VISIBLE);
                        unreadBadge.setText(String.valueOf(profile.getUnreadCount()));
                    } else {
                        unreadBadge.setVisibility(View.GONE);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
