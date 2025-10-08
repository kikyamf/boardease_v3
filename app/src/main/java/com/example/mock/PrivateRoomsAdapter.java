package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class PrivateRoomsAdapter extends RecyclerView.Adapter<PrivateRoomsAdapter.ViewHolder> {

    private List<PrivateRoomsFragment.RoomData> roomList;
    private OnRoomClickListener clickListener;
    private OnEditClickListener editListener;
    private OnDeleteClickListener deleteListener;

    public interface OnRoomClickListener {
        void onRoomClick(PrivateRoomsFragment.RoomData room);
    }

    public interface OnEditClickListener {
        void onEditClick(PrivateRoomsFragment.RoomData room);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(PrivateRoomsFragment.RoomData room);
    }

    public PrivateRoomsAdapter(List<PrivateRoomsFragment.RoomData> roomList, 
                              OnRoomClickListener clickListener,
                              OnEditClickListener editListener,
                              OnDeleteClickListener deleteListener) {
        this.roomList = roomList;
        this.clickListener = clickListener;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_room_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PrivateRoomsFragment.RoomData room = roomList.get(position);
        
        holder.tvTitle.setText(room.title);
        holder.tvDescription.setText(room.description);
        holder.tvPrice.setText("₱" + room.price);
        holder.tvCapacity.setText(room.capacity + " person(s)");
        holder.tvTotalRooms.setText(room.totalRooms + " units");
        
        // Load first image if available
        if (room.imagePaths != null && !room.imagePaths.isEmpty()) {
            String imageUrl = "http://192.168.101.6/BoardEase2/" + room.imagePaths.get(0);
            System.out.println("DEBUG: Loading room image: " + imageUrl);
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .override(200, 150)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.error)
                    .into(holder.ivRoomImage);
        } else {
            System.out.println("DEBUG: No images for room: " + room.title);
            holder.ivRoomImage.setImageResource(R.drawable.placeholder);
        }
        
        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onRoomClick(room);
            }
        });
        
        holder.btnEdit.setOnClickListener(v -> {
            if (editListener != null) {
                editListener.onEditClick(room);
            }
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(room);
            }
        });
    }

    @Override
    public int getItemCount() {
        return roomList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRoomImage;
        TextView tvTitle, tvDescription, tvPrice, tvCapacity, tvTotalRooms;
        ImageButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRoomImage = itemView.findViewById(R.id.ivRoomImage);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvCapacity = itemView.findViewById(R.id.tvCapacity);
            tvTotalRooms = itemView.findViewById(R.id.tvTotalRooms);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}








