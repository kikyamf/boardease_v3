package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class BookingHistoryAdapter extends RecyclerView.Adapter<BookingHistoryAdapter.ViewHolder> {
    
    private List<BookingData> bookings;
    private OnBookingActionListener listener;
    
    public interface OnBookingActionListener {
        void onViewDetails(BookingData booking);
    }
    
    public BookingHistoryAdapter(List<BookingData> bookings, OnBookingActionListener listener) {
        this.bookings = bookings;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_booking, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingData booking = bookings.get(position);
        
        // Set boarder name
        if (booking.getBoarderName() != null && !booking.getBoarderName().isEmpty()) {
            holder.tvBoarderName.setText(booking.getBoarderName());
        } else {
            holder.tvBoarderName.setText("Unknown");
        }
        
        // Set status
        String status = booking.getStatus() != null ? booking.getStatus() : "";
        holder.tvStatus.setText(status);
        
        // Update status color based on status
        if ("Confirmed".equals(status)) {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_approved);
        } else if ("Cancelled".equals(status)) {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_rounded_red);
        } else if ("Completed".equals(status)) {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
        } else {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
        }
        
        holder.tvEmail.setText(booking.getEmail() != null ? booking.getEmail() : "");
        holder.tvPhone.setText(booking.getPhoneNumber() != null ? booking.getPhoneNumber() : "");
        holder.tvRoomName.setText(booking.getRoomName() != null ? booking.getRoomName() : "");
        holder.tvStartDate.setText(booking.getStartDate() != null ? booking.getStartDate() : "");
        holder.tvEndDate.setText(booking.getEndDate() != null ? booking.getEndDate() : "");
        holder.tvAmount.setText(booking.getAmount() != null ? booking.getAmount() : "");
        holder.tvRentType.setText(booking.getRentType() != null ? booking.getRentType() : "");
        
        // Hide action buttons for history
        holder.btnApprove.setVisibility(View.GONE);
        holder.btnDecline.setVisibility(View.GONE);
        
        // Set click listener for view details
        holder.btnViewDetails.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewDetails(booking);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return bookings.size();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBoarderName, tvStatus, tvEmail, tvPhone, tvRoomName, tvStartDate, tvEndDate, tvAmount, tvRentType;
        View btnApprove, btnDecline, btnViewDetails;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBoarderName = itemView.findViewById(R.id.tvBoarderName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvRoomName = itemView.findViewById(R.id.tvRoomName);
            tvStartDate = itemView.findViewById(R.id.tvStartDate);
            tvEndDate = itemView.findViewById(R.id.tvEndDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvRentType = itemView.findViewById(R.id.tvRentType);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnDecline = itemView.findViewById(R.id.btnDecline);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }
    }
}








