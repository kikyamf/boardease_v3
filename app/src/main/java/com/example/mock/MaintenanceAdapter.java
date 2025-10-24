package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        
        // Set status with appropriate color
        holder.tvStatus.setText(maintenance.getStatus());
        switch (maintenance.getStatus().toLowerCase()) {
            case "new":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                break;
            case "in progress":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_approved);
                break;
            case "completed":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
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
        TextView tvPropertyName, tvIssueType, tvTimestamp, tvStatus, tvDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPropertyName = itemView.findViewById(R.id.tvPropertyName);
            tvIssueType = itemView.findViewById(R.id.tvIssueType);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }
    }
}

































