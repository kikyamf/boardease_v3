package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PopularListingBookingsAdapter extends RecyclerView.Adapter<PopularListingBookingsAdapter.ViewHolder> {
    
    private List<PopularListingBookingsActivity.PopularListingBooking> bookingsList;
    
    public PopularListingBookingsAdapter(List<PopularListingBookingsActivity.PopularListingBooking> bookingsList) {
        this.bookingsList = bookingsList;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_popular_listing_booking, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PopularListingBookingsActivity.PopularListingBooking booking = bookingsList.get(position);
        
        holder.tvBoarderName.setText(booking.getBoarderName());
        holder.tvRoomName.setText(booking.getRoomName());
        holder.tvDates.setText(booking.getStartDate() + " - " + booking.getEndDate());
        holder.tvAmount.setText("₱" + booking.getAmount());
    }
    
    @Override
    public int getItemCount() {
        return bookingsList != null ? bookingsList.size() : 0;
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBoarderName, tvRoomName, tvDates, tvAmount;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBoarderName = itemView.findViewById(R.id.tvBoarderName);
            tvRoomName = itemView.findViewById(R.id.tvRoomName);
            tvDates = itemView.findViewById(R.id.tvDates);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}


