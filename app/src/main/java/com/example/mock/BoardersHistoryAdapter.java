package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BoardersHistoryAdapter extends RecyclerView.Adapter<BoardersHistoryAdapter.ViewHolder> {
    
    private List<BoarderHistoryData> boarders;
    private OnBoarderHistoryClickListener listener;

    public interface OnBoarderHistoryClickListener {
        void onBoarderHistoryClick(BoarderHistoryData boarder);
    }
    
    public BoardersHistoryAdapter(List<BoarderHistoryData> boarders) {
        this.boarders = boarders;
    }

    public void setOnBoarderHistoryClickListener(OnBoarderHistoryClickListener listener) {
        this.listener = listener;
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
        
        // Set dates with formatting
        if (boarder.getStartDate() != null && !boarder.getStartDate().isEmpty()) {
            holder.tvStartDate.setText(formatDate(boarder.getStartDate()));
        } else {
            holder.tvStartDate.setText("Not specified");
        }
        if (boarder.getEndDate() != null && !boarder.getEndDate().isEmpty()) {
            holder.tvEndDate.setText(formatDate(boarder.getEndDate()));
        } else {
            holder.tvEndDate.setText("Not specified");
        }
        
        // Set status with appropriate styling
        String status = boarder.getStatus();
        holder.tvStatus.setText(status);
        
        // Set status background color based on status
        // Completed - blue
        if ("Completed".equals(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed); // Blue for Completed
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
        } else if ("Confirmed".equals(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_available);
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_rounded_orange);
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBoarderHistoryClick(boarder);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return boarders.size();
    }

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "";
        }
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (ParseException e) {
            // If parsing fails, return original string
        }
        return dateString;
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



































