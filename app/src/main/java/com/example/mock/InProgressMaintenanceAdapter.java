package com.example.mock;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class InProgressMaintenanceAdapter extends RecyclerView.Adapter<InProgressMaintenanceAdapter.ViewHolder> {

    private ArrayList<MaintenanceRequest> maintenanceRequests;
    private Context context;
    private OnMarkCompletedListener onMarkCompletedListener;

    public interface OnMarkCompletedListener {
        void onMarkCompleted(MaintenanceRequest request, int position);
    }

    public InProgressMaintenanceAdapter(ArrayList<MaintenanceRequest> maintenanceRequests, Context context) {
        this.maintenanceRequests = maintenanceRequests;
        this.context = context;
    }

    public void setOnMarkCompletedListener(OnMarkCompletedListener listener) {
        this.onMarkCompletedListener = listener;
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
        holder.tvStatus.setText(request.getStatus());
        holder.tvPriority.setText(request.getPriority());

        // Format and set Date Reported
        String dateReported = formatDate(request.getRequestDate());
        holder.tvDateReported.setText("Date Reported: " + dateReported);

        // Format and set Date Approved (hide tvDateApproved for In Progress, use tvApprovedOn)
        if (holder.tvDateApproved != null) {
            holder.tvDateApproved.setVisibility(android.view.View.GONE);
        }
        
        // Format and set Approved On
        String approvedDate = request.getApprovedDate();
        if (approvedDate != null && !approvedDate.isEmpty()) {
            holder.tvApprovedOn.setText("Approved On: " + formatDate(approvedDate));
        } else {
            // If no approved date, use request date as fallback
            holder.tvApprovedOn.setText("Approved On: " + dateReported);
        }

        // Set priority color
        String priority = request.getPriority().toLowerCase();
        if ("high".equals(priority)) {
            holder.tvPriority.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
        } else if ("medium".equals(priority)) {
            holder.tvPriority.setTextColor(context.getResources().getColor(android.R.color.holo_orange_dark));
        } else {
            holder.tvPriority.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
        }

        // Set status badge and card border based on status
        String status = request.getStatus().toLowerCase();
        if ("in progress".equals(status)) {
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

        // Set click listener for Mark as Completed button
        holder.btnMarkAsCompleted.setOnClickListener(v -> {
            if (onMarkCompletedListener != null) {
                onMarkCompletedListener.onMarkCompleted(request, position);
            }
        });
    }

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "N/A";
        }
        try {
            // Try to parse various date formats
            SimpleDateFormat[] inputFormats = {
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()),
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
                // Format as "Nov 18, 2025 02:00 PM" (with time)
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
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
        MaterialButton btnMarkAsCompleted;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvBoarderName = itemView.findViewById(R.id.tvBoarderName);
            tvBoardingHouse = itemView.findViewById(R.id.tvBoardingHouse);
            tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
            tvMaintenanceType = itemView.findViewById(R.id.tvMaintenanceType);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvRequestDate = itemView.findViewById(R.id.tvRequestDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvDateReported = itemView.findViewById(R.id.tvDateReported);
            tvDateApproved = itemView.findViewById(R.id.tvDateApproved);
            tvApprovedOn = itemView.findViewById(R.id.tvApprovedOn);
            btnMarkAsCompleted = itemView.findViewById(R.id.btnMarkAsCompleted);
        }
    }
}






























