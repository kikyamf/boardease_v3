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
            params.bottomMargin = 8;
            holder.tvRoomNumber.setLayoutParams(params);

            // Show status with capacity: e.g. "Available (Partially Occupied) (1 reserved) - 2/6 person(s)"
            String statusText = unit.status + " - " + unit.capacityDisplay;
            holder.tvStatus.setText(statusText);
        } else {
            // PRIVATE ROOM: Horizontal Layout (Short text)
            holder.llContentContainer.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            
            android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) holder.tvRoomNumber.getLayoutParams();
            params.width = 0;
            params.weight = 1;
            params.bottomMargin = 0;
            holder.tvRoomNumber.setLayoutParams(params);

            holder.tvStatus.setText(unit.status);
        }

        // Append (Your Application) if applicable
        if (unit.userApplicationStatus != null && !unit.userApplicationStatus.isEmpty()) {
            holder.tvStatus.append(" - (" + unit.userApplicationStatus + " Application)");
        }

        // Set status badge color — order matters: most specific check first
        String s = unit.status != null ? unit.status : "";
        if (s.equals("Occupied")) {
            // Full — red
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_occupied);
        } else if (s.contains("Partially Occupied")) {
            // Bed Spacer partially filled — orange
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_partially_occupied);
        } else if (s.equals("Reserved")) {
            // Reserved (Approved, not yet paid) — blue
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_reserved);
        } else if (s.equals("Available")) {
            // Fully available — green
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_available);
        } else {
            // Fallback — gray
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

































