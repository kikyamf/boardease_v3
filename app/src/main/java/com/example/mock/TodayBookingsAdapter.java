package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TodayBookingsAdapter extends RecyclerView.Adapter<TodayBookingsAdapter.ViewHolder> {
    
    private List<TodayBookingsActivity.TodayBooking> bookingsList;
    
    public TodayBookingsAdapter(List<TodayBookingsActivity.TodayBooking> bookingsList) {
        this.bookingsList = bookingsList;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_today_booking, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TodayBookingsActivity.TodayBooking booking = bookingsList.get(position);
        
        holder.tvBoarderName.setText(booking.getBoarderName());
        holder.tvBoardingHouseName.setText(booking.getBoardingHouseName());
        holder.tvRoomName.setText(booking.getRoomName());
        holder.tvDates.setText(booking.getStartDate() + " - " + booking.getEndDate());
        holder.tvAmount.setText("₱" + booking.getAmount());
    }
    
    @Override
    public int getItemCount() {
        return bookingsList != null ? bookingsList.size() : 0;
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBoarderName, tvBoardingHouseName, tvRoomName, tvDates, tvAmount;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBoarderName = itemView.findViewById(R.id.tvBoarderName);
            tvBoardingHouseName = itemView.findViewById(R.id.tvBoardingHouseName);
            tvRoomName = itemView.findViewById(R.id.tvRoomName);
            tvDates = itemView.findViewById(R.id.tvDates);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}


