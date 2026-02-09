package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChangeRoomRequestsAdapter extends RecyclerView.Adapter<ChangeRoomRequestsAdapter.ViewHolder> {

    private List<ChangeRoomRequestsFragment.ChangeRoomRequestData> requests;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ChangeRoomRequestsFragment.ChangeRoomRequestData request);
    }

    public ChangeRoomRequestsAdapter(List<ChangeRoomRequestsFragment.ChangeRoomRequestData> requests, OnItemClickListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_change_room_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChangeRoomRequestsFragment.ChangeRoomRequestData request = requests.get(position);
        holder.tvBoarderName.setText(request.getBoarderName());
        holder.tvDate.setText(request.getCreatedAt());
        holder.tvBhName.setText(request.getBhName());
        holder.tvOldRoom.setText(request.getOldRoomName());
        holder.tvNewRoom.setText(request.getNewRoomName());
        holder.tvReason.setText("Reason: " + request.getReason());

        holder.cardView.setOnClickListener(v -> listener.onItemClick(request));
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBoarderName, tvDate, tvBhName, tvOldRoom, tvNewRoom, tvReason;
        View cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvBoarderName = itemView.findViewById(R.id.tvBoarderName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvBhName = itemView.findViewById(R.id.tvBhName);
            tvOldRoom = itemView.findViewById(R.id.tvOldRoom);
            tvNewRoom = itemView.findViewById(R.id.tvNewRoom);
            tvReason = itemView.findViewById(R.id.tvReason);
        }
    }
}
