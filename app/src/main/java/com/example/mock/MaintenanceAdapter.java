package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

public class MaintenanceAdapter extends RecyclerView.Adapter<MaintenanceAdapter.ViewHolder> {

    private List<MaintenanceFragment.MaintenanceLog> maintenanceLogs;
    private OnMaintenanceClickListener clickListener;

    public interface OnMaintenanceClickListener {
        void onMaintenanceClick(MaintenanceFragment.MaintenanceLog maintenance);
    }

    public MaintenanceAdapter(List<MaintenanceFragment.MaintenanceLog> maintenanceLogs, OnMaintenanceClickListener clickListener) {
        this.maintenanceLogs = maintenanceLogs;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_maintenance_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MaintenanceFragment.MaintenanceLog maintenance = maintenanceLogs.get(position);
        
        holder.tvPropertyName.setText(maintenance.getPropertyName());
        holder.tvIssueType.setText(maintenance.getIssueType());
        holder.tvTimestamp.setText(maintenance.getTimestamp());
        holder.tvDescription.setText(maintenance.getDescription());
        
        // Load boarding house image
        String imageUrl = maintenance.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_home)
                    .error(R.drawable.ic_home)
                    .centerCrop()
                    .into(holder.ivPropertyImage);
        } else {
            holder.ivPropertyImage.setImageResource(R.drawable.ic_home);
        }
        
        // Set status with appropriate color
        String status = maintenance.getStatus();
        holder.tvStatus.setText(status);
        
        // Handle maintenance statuses: Pending (orange), In Progress (blue), Resolved/Completed (green), Declined (red)
        switch (status.toLowerCase()) {
            case "new":
            case "pending":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending); // Orange
                break;
            case "in progress":
            case "in_progress":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed); // Blue
                break;
            case "resolved":
            case "completed":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_approved); // Green
                break;
            case "declined":
            case "rejected":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_declined); // Red
                break;
            default:
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending); // Orange
                break;
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onMaintenanceClick(maintenance);
            }
        });
    }

    @Override
    public int getItemCount() {
        return maintenanceLogs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPropertyImage;
        TextView tvPropertyName, tvIssueType, tvTimestamp, tvStatus, tvDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPropertyImage = itemView.findViewById(R.id.ivPropertyImage);
            tvPropertyName = itemView.findViewById(R.id.tvPropertyName);
            tvIssueType = itemView.findViewById(R.id.tvIssueType);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }
    }
}


































