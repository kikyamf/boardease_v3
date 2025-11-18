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

public class PendingMaintenanceAdapter extends RecyclerView.Adapter<PendingMaintenanceAdapter.ViewHolder> {

    private ArrayList<MaintenanceRequest> maintenanceRequests;
    private Context context;
    private OnStatusUpdateListener listener;

    public interface OnStatusUpdateListener {
        void onStatusUpdate(int requestId, String newStatus);
    }

    public PendingMaintenanceAdapter(ArrayList<MaintenanceRequest> maintenanceRequests, Context context, OnStatusUpdateListener listener) {
        this.maintenanceRequests = maintenanceRequests;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_maintenance_request, parent, false);
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
        
        // Format date with time: "Nov 18, 2025 02:00 PM"
        String formattedDate = formatDateTime(request.getRequestDate());
        holder.tvRequestDate.setText("Requested: " + formattedDate);
        
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

        // Set status badge for pending - Orange background
        holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
        holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.white));
        
        // Set card border color - Orange (#FF9800)
        holder.cardView.setStrokeColor(context.getResources().getColor(android.R.color.holo_orange_dark));
        holder.cardView.setStrokeWidth((int) (2 * context.getResources().getDisplayMetrics().density));

        // Set up action buttons
        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStatusUpdate(request.getRequestId(), "In Progress");
            }
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStatusUpdate(request.getRequestId(), "Rejected");
            }
        });
    }

    private String formatDateTime(String dateString) {
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
                // Format as "Nov 18, 2025 02:00 PM"
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
                tvDescription, tvRequestDate, tvStatus, tvPriority;
        MaterialButton btnApprove, btnReject;

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
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}






























