package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RoomUnitsAdapter extends RecyclerView.Adapter<RoomUnitsAdapter.ViewHolder> {

    private List<RoomUnit> unitList;

    public RoomUnitsAdapter(List<RoomUnit> unitList) {
        this.unitList = unitList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_room_unit, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RoomUnit unit = unitList.get(position);
        
        // Set room number
        holder.tvRoomNumber.setText(unit.roomNumber);
        
        // For Bed Spacer, show capacity display if available
        if (unit.capacityDisplay != null && !unit.capacityDisplay.isEmpty()) {
            // Show status with capacity: "Available - 0/2 person(s)", "Partially Occupied - 1/2 person(s)", or "Occupied - 2/2 person(s)"
            String statusText = unit.status;
            if (unit.status.equals("Available") || unit.status.equals("Partially Occupied") || unit.status.equals("Occupied")) {
                statusText = unit.status + " - " + unit.capacityDisplay;
            }
            holder.tvStatus.setText(statusText);
        } else {
            // For Private Room, just show status
            holder.tvStatus.setText(unit.status);
        }
        
        // Set status background color
        if ("Available".equals(unit.status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_available);
        } else if ("Occupied".equals(unit.status) || "Partially Occupied".equals(unit.status)) {
            // Partially Occupied uses the same red color as Occupied
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_occupied);
        } else {
            // Default to gray for other statuses
            holder.tvStatus.setBackgroundResource(R.drawable.bg_rounded_gray);
        }
    }

    @Override
    public int getItemCount() {
        return unitList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomNumber, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }

    public static class RoomUnit {
        public String roomNumber;
        public String status;
        public String capacityDisplay; // For Bed Spacer: "2/4 person(s)"
        public int totalCapacity; // Total capacity
        public int occupiedCapacity; // Occupied capacity
    }
}

































