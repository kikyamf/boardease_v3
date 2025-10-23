package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class BoardersListAdapter extends RecyclerView.Adapter<BoardersListAdapter.ViewHolder> {

    private List<BoarderData> boardersList;

    public BoardersListAdapter(List<BoarderData> boardersList) {
        this.boardersList = boardersList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_boarder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BoarderData boarder = boardersList.get(position);

        // Set boarder name
        holder.tvBoarderName.setText(boarder.getBoarderName());

        // Set boarding house and room info
        String boardingInfo = boarder.getBoardingHouseName() + " - Room " + boarder.getRoomNumber();
        holder.tvBoardingInfo.setText(boardingInfo);

        // Set rent type
        holder.tvRentType.setText(boarder.getRentType());

        // Set rental period
        String rentalPeriod = boarder.getStartDate() + " to " + boarder.getEndDate();
        holder.tvRentalPeriod.setText(rentalPeriod);

        // Set status with appropriate background
        holder.tvStatus.setText(boarder.getStatus());
        if ("Active".equals(boarder.getStatus())) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_available);
        } else if ("Completed".equals(boarder.getStatus())) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_occupied);
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_rounded_orange);
        }

        // Set profile picture
        if (boarder.getProfilePicture() != null && !boarder.getProfilePicture().isEmpty()) {
            String fullImageUrl = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/" + boarder.getProfilePicture();
            Glide.with(holder.itemView.getContext())
                    .load(fullImageUrl)
                    .placeholder(R.drawable.btn_profile)
                    .error(R.drawable.btn_profile)
                    .circleCrop()
                    .into(holder.ivProfilePicture);
        } else {
            holder.ivProfilePicture.setImageResource(R.drawable.btn_profile);
        }
    }

    @Override
    public int getItemCount() {
        return boardersList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfilePicture;
        TextView tvBoarderName, tvBoardingInfo, tvRentType, tvRentalPeriod, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfilePicture = itemView.findViewById(R.id.ivProfilePicture);
            tvBoarderName = itemView.findViewById(R.id.tvBoarderName);
            tvBoardingInfo = itemView.findViewById(R.id.tvBoardingInfo);
            tvRentType = itemView.findViewById(R.id.tvRentType);
            tvRentalPeriod = itemView.findViewById(R.id.tvRentalPeriod);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}














