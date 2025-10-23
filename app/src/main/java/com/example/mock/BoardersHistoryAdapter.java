package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class BoardersHistoryAdapter extends RecyclerView.Adapter<BoardersHistoryAdapter.ViewHolder> {
    
    private List<BoarderHistoryData> boarders;
    
    public BoardersHistoryAdapter(List<BoarderHistoryData> boarders) {
        this.boarders = boarders;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_boarder_history, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BoarderHistoryData boarder = boarders.get(position);
        
        // Set boarder name
        holder.tvBoarderName.setText(boarder.getBoarderName());
        
        // Set room info with boarding house name if available
        String roomInfo = boarder.getRoomName();
        if (boarder.getBoardingHouseName() != null && !boarder.getBoardingHouseName().isEmpty()) {
            roomInfo = boarder.getBoardingHouseName() + " - " + boarder.getRoomName();
        }
        if (boarder.getRentType() != null && !boarder.getRentType().isEmpty()) {
            roomInfo += " (" + boarder.getRentType() + ")";
        }
        holder.tvRoomName.setText(roomInfo);
        
        // Set dates
        holder.tvStartDate.setText(boarder.getStartDate());
        holder.tvEndDate.setText(boarder.getEndDate());
        
        // Set status with appropriate styling
        String status = boarder.getStatus();
        holder.tvStatus.setText(status);
        
        // Set status background color based on status
        if ("Completed".equals(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
        } else if ("Confirmed".equals(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_available);
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_rounded_orange);
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
        }
    }
    
    @Override
    public int getItemCount() {
        return boarders.size();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBoarderName, tvRoomName, tvStartDate, tvEndDate, tvStatus;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBoarderName = itemView.findViewById(R.id.tvBoarderName);
            tvRoomName = itemView.findViewById(R.id.tvRoomName);
            tvStartDate = itemView.findViewById(R.id.tvStartDate);
            tvEndDate = itemView.findViewById(R.id.tvEndDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}































