package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BoarderMaintenanceHistoryAdapter extends RecyclerView.Adapter<BoarderMaintenanceHistoryAdapter.ViewHolder> {

    private List<MaintenanceHistoryActivity.MaintenanceHistoryItem> maintenanceList;

    public BoarderMaintenanceHistoryAdapter(List<MaintenanceHistoryActivity.MaintenanceHistoryItem> maintenanceList) {
        this.maintenanceList = maintenanceList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_boarder_maintenance_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MaintenanceHistoryActivity.MaintenanceHistoryItem maintenance = maintenanceList.get(position);

        // Set subject
        String subject = maintenance.getSubject();
        if (subject == null || subject.isEmpty()) {
            subject = "Maintenance Request";
        }
        holder.tvSubject.setText(subject);

        // Set area for maintenance
        String area = maintenance.getAreaForMaintenance();
        if (area == null || area.isEmpty()) {
            area = "N/A";
        }
        holder.tvArea.setText(area);

        // Set description
        String description = maintenance.getDescription();
        if (description == null || description.isEmpty()) {
            description = "No description provided";
        }
        holder.tvDescription.setText(description);

        // Set status
        String status = maintenance.getStatus();
        if (status == null || status.isEmpty()) {
            status = "Pending";
        }
        holder.tvStatus.setText(status);
        setStatusBackground(holder.tvStatus, status);

        // Set created date and time
        String formattedDate = maintenance.getFormattedCreatedDate();
        if (formattedDate == null || formattedDate.isEmpty()) {
            formattedDate = formatDate(maintenance.getCreatedAt());
        }
        
        String[] dateTimeParts = splitDateTime(formattedDate);
        holder.tvCreatedDate.setText(dateTimeParts[0]);
        if (dateTimeParts.length > 1 && !dateTimeParts[1].isEmpty()) {
            holder.tvCreatedTime.setText(dateTimeParts[1]);
            holder.tvCreatedTime.setVisibility(View.VISIBLE);
        } else {
            holder.tvCreatedTime.setVisibility(View.GONE);
        }

        // Set approved date (if available)
        String approvedDate = maintenance.getFormattedApprovedDate();
        if (approvedDate != null && !approvedDate.isEmpty() && !approvedDate.equals("null")) {
            holder.layoutApprovedDate.setVisibility(View.VISIBLE);
            String[] approvedParts = splitDateTime(approvedDate);
            holder.tvApprovedDate.setText(approvedParts[0]);
            if (approvedParts.length > 1 && !approvedParts[1].isEmpty()) {
                holder.tvApprovedTime.setText(approvedParts[1]);
                holder.tvApprovedTime.setVisibility(View.VISIBLE);
            } else {
                holder.tvApprovedTime.setVisibility(View.GONE);
            }
        } else {
            holder.layoutApprovedDate.setVisibility(View.GONE);
        }

        // Set completed date (if available)
        String completedDate = maintenance.getFormattedCompletedDate();
        if (completedDate != null && !completedDate.isEmpty() && !completedDate.equals("null")) {
            holder.layoutCompletedDate.setVisibility(View.VISIBLE);
            String[] completedParts = splitDateTime(completedDate);
            holder.tvCompletedDate.setText(completedParts[0]);
            if (completedParts.length > 1 && !completedParts[1].isEmpty()) {
                holder.tvCompletedTime.setText(completedParts[1]);
                holder.tvCompletedTime.setVisibility(View.VISIBLE);
            } else {
                holder.tvCompletedTime.setVisibility(View.GONE);
            }
        } else {
            holder.layoutCompletedDate.setVisibility(View.GONE);
        }

        // Set room information (optional - only show if available)
        String roomInfo = maintenance.getRoomInfo();
        if (roomInfo == null || roomInfo.isEmpty()) {
            String roomName = maintenance.getRoomName();
            String roomNumber = maintenance.getRoomNumber();
            if (roomName != null && !roomName.isEmpty()) {
                roomInfo = roomName;
                if (roomNumber != null && !roomNumber.isEmpty()) {
                    roomInfo += " - " + roomNumber;
                }
            } else if (roomNumber != null && !roomNumber.isEmpty()) {
                roomInfo = roomNumber;
            }
        }
        
        if (roomInfo != null && !roomInfo.isEmpty() && !roomInfo.equals("N/A")) {
            holder.layoutRoomInfo.setVisibility(View.VISIBLE);
            holder.tvRoomInfo.setText(roomInfo);
        } else {
            holder.layoutRoomInfo.setVisibility(View.GONE);
        }

        // Set boarding house name
        String bhName = maintenance.getBoardingHouseName();
        if (bhName == null || bhName.isEmpty()) {
            bhName = "";
        }
        holder.tvBoardingHouseName.setText(bhName);
    }

    private void setStatusBackground(TextView statusView, String status) {
        if (status == null) {
            statusView.setBackgroundResource(R.drawable.bg_status_pending);
            return;
        }

        String statusLower = status.toLowerCase();
        if (statusLower.equals("resolved") || statusLower.contains("resolved") || statusLower.contains("completed")) {
            statusView.setBackgroundResource(R.drawable.bg_status_approved); // Green
        } else if (statusLower.equals("in progress") || statusLower.contains("in progress")) {
            statusView.setBackgroundResource(R.drawable.bg_status_in_progress); // Blue
        } else if (statusLower.equals("declined") || statusLower.contains("declined")) {
            statusView.setBackgroundResource(R.drawable.bg_status_declined); // Red
        } else if (statusLower.equals("pending") || statusLower.contains("pending")) {
            statusView.setBackgroundResource(R.drawable.bg_status_pending); // Orange
        } else {
            statusView.setBackgroundResource(R.drawable.bg_status_pending); // Default Orange
        }
    }

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty() || dateString.equals("null")) {
            return "";
        }

        try {
            // Try to parse with time
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            
            Date date = inputFormat.parse(dateString);
            if (date != null) {
                return dateFormat.format(date) + " at " + timeFormat.format(date);
            }
        } catch (ParseException e) {
            try {
                // Try without time
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
                
                Date date = inputFormat.parse(dateString);
                if (date != null) {
                    return outputFormat.format(date);
                }
            } catch (ParseException e2) {
                // Return original if parsing fails
            }
        }

        return dateString;
    }

    private String[] splitDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isEmpty()) {
            return new String[]{"", ""};
        }

        // Check if it contains "at" (formatted date with time)
        if (dateTimeString.contains(" at ")) {
            String[] parts = dateTimeString.split(" at ", 2);
            return new String[]{parts[0], parts.length > 1 ? parts[1] : ""};
        }

        // If no time separator, return date only
        return new String[]{dateTimeString, ""};
    }

    @Override
    public int getItemCount() {
        return maintenanceList != null ? maintenanceList.size() : 0;
    }
    
    // Method to update the list
    public void updateList(List<MaintenanceHistoryActivity.MaintenanceHistoryItem> newList) {
        this.maintenanceList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubject, tvArea, tvDescription, tvStatus;
        TextView tvCreatedDate, tvCreatedTime;
        TextView tvApprovedDate, tvApprovedTime;
        TextView tvCompletedDate, tvCompletedTime;
        TextView tvRoomInfo, tvBoardingHouseName;
        LinearLayout layoutRoomInfo, layoutApprovedDate, layoutCompletedDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvArea = itemView.findViewById(R.id.tvArea);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvCreatedDate = itemView.findViewById(R.id.tvCreatedDate);
            tvCreatedTime = itemView.findViewById(R.id.tvCreatedTime);
            tvApprovedDate = itemView.findViewById(R.id.tvApprovedDate);
            tvApprovedTime = itemView.findViewById(R.id.tvApprovedTime);
            tvCompletedDate = itemView.findViewById(R.id.tvCompletedDate);
            tvCompletedTime = itemView.findViewById(R.id.tvCompletedTime);
            tvRoomInfo = itemView.findViewById(R.id.tvRoomInfo);
            tvBoardingHouseName = itemView.findViewById(R.id.tvBoardingHouseName);
            layoutRoomInfo = itemView.findViewById(R.id.layoutRoomInfo);
            layoutApprovedDate = itemView.findViewById(R.id.layoutApprovedDate);
            layoutCompletedDate = itemView.findViewById(R.id.layoutCompletedDate);
        }
    }
}

