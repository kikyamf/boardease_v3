package com.example.mock;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

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
        holder.tvRequestDate.setText(request.getRequestDate());
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

        // Set status color for pending
        holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_blue_dark));

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

    @Override
    public int getItemCount() {
        return maintenanceRequests.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBoarderName, tvBoardingHouse, tvRoomNumber, tvMaintenanceType, 
                tvDescription, tvRequestDate, tvStatus, tvPriority;
        Button btnApprove, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
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





























