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
            // BED SPACER: Vertical Layout (Long text)
            holder.llContentContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
            
            // Reset layout params for Room Number (wrap_content, no weight)
            android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) holder.tvRoomNumber.getLayoutParams();
            params.width = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            params.weight = 0;
            params.bottomMargin = 8; // Add some spacing bottom
            holder.tvRoomNumber.setLayoutParams(params);

            // Show status with capacity: "Available - 0/2 person(s)", "Partially Occupied - 1/2 person(s)", or "Occupied - 2/2 person(s)"
            String statusText = unit.status + " - " + unit.capacityDisplay;
            holder.tvStatus.setText(statusText);
        } else {
            // PRIVATE ROOM: Horizontal Layout (Short text)
            holder.llContentContainer.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            
            // Set layout params for Room Number (0dp width, weight 1 to push status to right)
            android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) holder.tvRoomNumber.getLayoutParams();
            params.width = 0;
            params.weight = 1;
            params.bottomMargin = 0; // Remove bottom spacing
            holder.tvRoomNumber.setLayoutParams(params);

            // Just show status
            holder.tvStatus.setText(unit.status);
        }

        // Append (Your Application) if applicable
        if (unit.userApplicationStatus != null && !unit.userApplicationStatus.isEmpty()) {
            holder.tvStatus.append(" - (" + unit.userApplicationStatus + " Application)");
        }
        
        // Set status background color
        if ("Available".equals(unit.status)) {
            // Green for strictly "Available"
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_available);
        } else if (unit.status != null && (unit.status.contains("Occupied") || unit.status.contains("occupied"))) {
            // Red for "Occupied" or "Available(Partially Occupied)"
            // CHECK THIS FIRST before "Reserved" because "Available(Partially Occupied) (1 reserved)" contains "Reserved" too
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_occupied);
        } else if (unit.status != null && (unit.status.contains("Reserved") || unit.status.contains("reserved"))) {
            // Blue for "Reserved" -> Only if NOT Occupied/Partially Occupied
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_reserved);
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
        android.widget.LinearLayout llContentContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            llContentContainer = itemView.findViewById(R.id.llContentContainer);
        }
    }

    public static class RoomUnit {
        public String roomNumber;
        public String status;
        public String capacityDisplay; // For Bed Spacer: "2/4 person(s)"
        public int totalCapacity; // Total capacity
        public int occupiedCapacity; // Occupied capacity
        public String userApplicationStatus; // "Pending" or "Approved"
    }
}

































