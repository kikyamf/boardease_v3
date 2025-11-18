package com.example.mock;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class RejectedMaintenanceAdapter extends RecyclerView.Adapter<RejectedMaintenanceAdapter.ViewHolder> {

    private ArrayList<MaintenanceRequest> maintenanceRequests;
    private Context context;

    public RejectedMaintenanceAdapter(ArrayList<MaintenanceRequest> maintenanceRequests, Context context) {
        this.maintenanceRequests = maintenanceRequests;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_maintenance_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MaintenanceRequest request = maintenanceRequests.get(position);

        holder.tvBoarderName.setText(request.getBoarderName());
        holder.tvBoardingHouse.setText(request.getBoardingHouseName());
        holder.tvRoomNumber.setText("Room " + request.getRoomNumber());
        holder.tvMaintenanceType.setText(request.getMaintenanceType());
        holder.tvDescription.setText(request.getDescription());
        
        // Format date as "Nov 18, 2025" (no time)
        String formattedDate = formatDate(request.getRequestDate());
        // Use tvDateReported if available, otherwise tvRequestDate
        if (holder.tvDateReported != null) {
            holder.tvDateReported.setText("Date Reported: " + formattedDate);
        } else if (holder.tvRequestDate != null) {
            holder.tvRequestDate.setText("Requested: " + formattedDate);
        }
        
        // Hide "Date Approved" and "Approved On" fields for rejected requests
        if (holder.tvDateApproved != null) {
            holder.tvDateApproved.setVisibility(android.view.View.GONE);
        }
        if (holder.tvApprovedOn != null) {
            holder.tvApprovedOn.setVisibility(android.view.View.GONE);
        }
        
        // Hide "Mark as Completed" button for rejected requests
        if (holder.btnMarkAsCompleted != null) {
            holder.btnMarkAsCompleted.setVisibility(android.view.View.GONE);
        }
        
        holder.tvStatus.setText(request.getStatus());
        holder.tvPriority.setText(request.getPriority());

        // Set priority color
        String priority = request.getPriority().toLowerCase();
        if ("high".equals(priority)) {
            holder.tvPriority.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
        } else if ("medium".equals(priority)) {
            holder.tvPriority.setTextColor(context.getResources().getColor(android.R.color.holo_orange_dark));
        } else {
            holder.tvPriority.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
        }

        // Set status badge and card border for Rejected/Declined status
        String status = request.getStatus().toLowerCase();
        if ("declined".equals(status) || "rejected".equals(status)) {
            // Red badge and border
            holder.tvStatus.setBackgroundResource(R.drawable.bg_rounded_red); // Red
            holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.white));
            holder.cardView.setStrokeColor(context.getResources().getColor(android.R.color.holo_red_dark));
            holder.cardView.setStrokeWidth((int) (2 * context.getResources().getDisplayMetrics().density));
        } else if ("resolved".equals(status) || "completed".equals(status)) {
            // Green badge and border
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_approved); // Green
            holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.white));
            holder.cardView.setStrokeColor(context.getResources().getColor(android.R.color.holo_green_dark));
            holder.cardView.setStrokeWidth((int) (2 * context.getResources().getDisplayMetrics().density));
        } else if ("in progress".equals(status)) {
            // Blue badge and border
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed); // Blue
            holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.white));
            holder.cardView.setStrokeColor(context.getResources().getColor(android.R.color.holo_blue_dark));
            holder.cardView.setStrokeWidth((int) (2 * context.getResources().getDisplayMetrics().density));
        } else if ("pending".equals(status)) {
            // Orange badge and border
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending); // Orange
            holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.white));
            holder.cardView.setStrokeColor(context.getResources().getColor(android.R.color.holo_orange_dark));
            holder.cardView.setStrokeWidth((int) (2 * context.getResources().getDisplayMetrics().density));
        }
    }

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "N/A";
        }
        try {
            // Try to parse various date formats
            SimpleDateFormat[] inputFormats = {
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            };
            
            Date date = null;
            for (SimpleDateFormat format : inputFormats) {
                try {
                    date = format.parse(dateString);
                    break;
                } catch (ParseException e) {
                    // Try next format
                }
            }
            
            if (date != null) {
                // Format as "Nov 18, 2025" (no time)
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                return outputFormat.format(date);
            }
        } catch (Exception e) {
            // If parsing fails, return original string
        }
        return dateString;
    }

    @Override
    public int getItemCount() {
        return maintenanceRequests.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvBoarderName, tvBoardingHouse, tvRoomNumber, tvMaintenanceType, 
                tvDescription, tvRequestDate, tvStatus, tvPriority, tvDateReported, tvDateApproved, tvApprovedOn;
        com.google.android.material.button.MaterialButton btnMarkAsCompleted;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvBoarderName = itemView.findViewById(R.id.tvBoarderName);
            tvBoardingHouse = itemView.findViewById(R.id.tvBoardingHouse);
            tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
            tvMaintenanceType = itemView.findViewById(R.id.tvMaintenanceType);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvRequestDate = itemView.findViewById(R.id.tvRequestDate);
            tvDateReported = itemView.findViewById(R.id.tvDateReported);
            tvDateApproved = itemView.findViewById(R.id.tvDateApproved);
            tvApprovedOn = itemView.findViewById(R.id.tvApprovedOn);
            btnMarkAsCompleted = itemView.findViewById(R.id.btnMarkAsCompleted);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPriority = itemView.findViewById(R.id.tvPriority);
        }
    }
}


