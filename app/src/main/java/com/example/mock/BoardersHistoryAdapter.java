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
        
        holder.tvBoarderName.setText(boarder.getBoarderName());
        holder.tvRoomName.setText(boarder.getRoomName());
        holder.tvStartDate.setText(boarder.getStartDate());
        holder.tvEndDate.setText(boarder.getEndDate());
        holder.tvStatus.setText(boarder.getStatus());
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





