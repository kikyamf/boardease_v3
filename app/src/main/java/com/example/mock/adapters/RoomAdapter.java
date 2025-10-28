package com.example.mock.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mock.R;
import com.example.mock.RoomData;
import java.util.List;

public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {
    
    private Context context;
    private List<RoomData> roomList;
    
    public RoomAdapter(Context context, List<RoomData> roomList) {
        this.context = context;
        this.roomList = roomList;
    }
    
    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_room_card, parent, false);
        return new RoomViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        RoomData room = roomList.get(position);
        
        holder.tvRoomName.setText(room.getRoomName());
        holder.tvRoomCategory.setText(room.getRoomCategory());
        holder.tvPrice.setText(room.getFormattedPrice());
        holder.tvCapacity.setText(room.getCapacityText());
        holder.tvRoomDescription.setText(room.getRoomDescription());
        holder.tvTotalRooms.setText(room.getTotalRooms() + " available");
    }
    
    @Override
    public int getItemCount() {
        return roomList != null ? roomList.size() : 0;
    }
    
    public static class RoomViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomName, tvRoomCategory, tvPrice, tvCapacity, tvRoomDescription, tvTotalRooms;
        
        public RoomViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomName = itemView.findViewById(R.id.tvRoomName);
            tvRoomCategory = itemView.findViewById(R.id.tvRoomCategory);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvCapacity = itemView.findViewById(R.id.tvCapacity);
            tvRoomDescription = itemView.findViewById(R.id.tvRoomDescription);
            tvTotalRooms = itemView.findViewById(R.id.tvTotalRooms);
        }
    }
}
