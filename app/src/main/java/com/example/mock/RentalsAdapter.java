package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class RentalsAdapter extends RecyclerView.Adapter<RentalsAdapter.ViewHolder> {

    private List<RentalsFragment.RentalLog> rentalLogs;
    private OnRentalClickListener clickListener;

    public interface OnRentalClickListener {
        void onRentalClick(RentalsFragment.RentalLog rental);
    }

    public RentalsAdapter(List<RentalsFragment.RentalLog> rentalLogs, OnRentalClickListener clickListener) {
        this.rentalLogs = rentalLogs;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rental_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RentalsFragment.RentalLog rental = rentalLogs.get(position);
        
        holder.tvTenantName.setText(rental.getTenantName());
        holder.tvPropertyName.setText(rental.getPropertyName());
        holder.tvTimestamp.setText(rental.getTimestamp());
        holder.tvDescription.setText(rental.getDescription());
        
        // Set up circular clipping for profile picture
        holder.imgBoarderProfile.post(() -> {
            holder.imgBoarderProfile.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(android.view.View view, android.graphics.Outline outline) {
                    int size = Math.min(view.getWidth(), view.getHeight());
                    int left = (view.getWidth() - size) / 2;
                    int top = (view.getHeight() - size) / 2;
                    outline.setOval(left, top, left + size, top + size);
                }
            });
            holder.imgBoarderProfile.setClipToOutline(true);
        });
        
        // Load boarder profile picture
        String profileImageUrl = rental.getProfileImageUrl();
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(profileImageUrl)
                    .placeholder(R.drawable.btn_profile)
                    .error(R.drawable.btn_profile)
                    .circleCrop()
                    .into(holder.imgBoarderProfile);
        } else {
            holder.imgBoarderProfile.setImageResource(R.drawable.btn_profile);
        }
        
        // Set status with appropriate color
        String status = rental.getStatus();
        holder.tvStatus.setText(status);
        
        // Handle rental statuses: Current/Active (green), Completed (blue)
        switch (status.toLowerCase()) {
            case "current":
            case "active":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_approved); // Green
                break;
            case "completed":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed); // Blue
                break;
            default:
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                break;
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onRentalClick(rental);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rentalLogs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBoarderProfile;
        TextView tvTenantName, tvPropertyName, tvTimestamp, tvStatus, tvDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBoarderProfile = itemView.findViewById(R.id.imgBoarderProfile);
            tvTenantName = itemView.findViewById(R.id.tvTenantName);
            tvPropertyName = itemView.findViewById(R.id.tvPropertyName);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }
    }
}


































