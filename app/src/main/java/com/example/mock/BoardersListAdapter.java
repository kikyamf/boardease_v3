package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BoardersListAdapter extends RecyclerView.Adapter<BoardersListAdapter.ViewHolder> {

    private List<BoarderData> boardersList;
    private OnBoarderClickListener listener;

    public interface OnBoarderClickListener {
        void onBoarderClick(BoarderData boarder);
    }

    public BoardersListAdapter(List<BoarderData> boardersList) {
        this.boardersList = boardersList;
    }

    public void setOnBoarderClickListener(OnBoarderClickListener listener) {
        this.listener = listener;
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

        // Set rental period with formatted dates
        String rentalPeriod = "";
        if (boarder.getStartDate() != null && !boarder.getStartDate().isEmpty() 
            && boarder.getEndDate() != null && !boarder.getEndDate().isEmpty()) {
            // Format dates (assuming format is YYYY-MM-DD)
            rentalPeriod = formatDate(boarder.getStartDate()) + " - " + formatDate(boarder.getEndDate());
        } else {
            rentalPeriod = "Dates not specified";
        }
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

        // Set profile picture with rounded corners (matching booking card style)
        if (boarder.getProfilePicture() != null && !boarder.getProfilePicture().isEmpty()) {
            String fullImageUrl = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/" + boarder.getProfilePicture();
            Glide.with(holder.itemView.getContext())
                    .load(fullImageUrl)
                    .placeholder(R.drawable.btn_profile)
                    .error(R.drawable.btn_profile)
                    .centerCrop()
                    .into(holder.ivProfilePicture);
        } else {
            holder.ivProfilePicture.setImageResource(R.drawable.btn_profile);
        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBoarderClick(boarder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return boardersList.size();
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















