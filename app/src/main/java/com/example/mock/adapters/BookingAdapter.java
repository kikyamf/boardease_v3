package com.example.mock.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.mock.BoarderBookingFragment;
import com.example.mock.R;
import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {
    
    private Context context;
    private List<BoarderBookingFragment.Booking> bookingList;
    private OnBookingClickListener bookingClickListener;
    
    public interface OnBookingClickListener {
        void onBookingClick(BoarderBookingFragment.Booking booking);
    }
    
    public BookingAdapter(Context context, List<BoarderBookingFragment.Booking> bookingList, OnBookingClickListener bookingClickListener) {
        this.context = context;
        this.bookingList = bookingList;
        this.bookingClickListener = bookingClickListener;
    }
    
    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        BoarderBookingFragment.Booking booking = bookingList.get(position);
        
        // Set boarding house name
        holder.tvBoardingHouseName.setText(booking.getBoardingHouseName());
        
        // Set room details
        String roomDetails = booking.getRoomCategory();
        if (booking.getRoomNumber() != null && !booking.getRoomNumber().isEmpty()) {
            roomDetails += " | " + booking.getRoomNumber();
        }
        holder.tvRoomDetails.setText(roomDetails);
        
        // Set location
        holder.tvLocation.setText(booking.getLocation());
        
        // Set booking dates with formatting
        String startDate = booking.getStartDate();
        String endDate = booking.getEndDate();
        String formattedStartDate = startDate != null ? formatDate(startDate) : "";
        String formattedEndDate = endDate != null ? formatDate(endDate) : "";
        holder.tvBookingDates.setText(formattedStartDate + " - " + formattedEndDate);
        
        // Set monthly due
        holder.tvMonthlyDue.setText(booking.getMonthlyDue());
        
        // Set status
        String status = booking.getStatus();
        String displayStatus = booking.getDisplayStatus();
        holder.tvStatus.setText(displayStatus);
        
        // Set status background based on status (use original status or display status for fine-tuning)
        // Pending - orange, Confirmed - blue, Completed - green
        if ("Confirmed".equals(status)) {
            // Both Active and Upcoming bookings are "Confirmed"
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_approved); // Green for Confirmed (Active/Upcoming)
        } else if ("Completed".equals(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed); // Blue for Completed/History
        } else if ("Pending".equals(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending); // Orange for Pending
        } else if ("Approved".equals(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed); // Blue for Approved (Action Required)
        } else if ("Cancelled".equals(status) || "Declined".equals(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_cancelled_gray); // Gray for Cancelled/Declined
        } else if ("Expired".equals(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_expired); // Red for Expired
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending); // Orange for Default
        }
        
        // Load image with Glide
        if (booking.getImagePath() != null && !booking.getImagePath().isEmpty()) {
            Glide.with(context)
                    .load(booking.getImagePath())
                    .placeholder(R.drawable.sample_listing)
                    .error(R.drawable.sample_listing)
                    .into(holder.imgBoardingHouse);
        } else {
            holder.imgBoardingHouse.setImageResource(R.drawable.sample_listing);
        }
        
        // Set click listener for the entire card (only if listener is provided)
        if (bookingClickListener != null) {
            holder.itemView.setOnClickListener(v -> {
                bookingClickListener.onBookingClick(booking);
            });
        } else {
            // Remove click listener if no listener provided
            holder.itemView.setOnClickListener(null);
        }
    }
    
    @Override
    public int getItemCount() {
        return bookingList != null ? bookingList.size() : 0;
    }
    
    public void updateList(List<BoarderBookingFragment.Booking> newList) {
        this.bookingList = newList;
        notifyDataSetChanged();
    }
    
    private String formatDate(String dateString) {
        // Format from "YYYY-MM-DD" or "YYYY-MM-DD HH:MM:SS" to "MMM DD, YYYY" or "MMM DD, YYYY hh:mm a"
        if (dateString == null || dateString.isEmpty()) {
            return "";
        }
        try {
            // Try date-time format first
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault());
            java.util.Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (Exception e) {
            // If parsing fails, try date-only format
            try {
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
                java.util.Date date = inputFormat.parse(dateString);
                return outputFormat.format(date);
            } catch (Exception e2) {
                return dateString; // Return original if parsing fails
            }
        }
    }
    
    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBoardingHouse;
        TextView tvBoardingHouseName, tvRoomDetails, tvLocation, tvBookingDates, tvMonthlyDue, tvStatus;
        
        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBoardingHouse = itemView.findViewById(R.id.imgBoardingHouse);
            tvBoardingHouseName = itemView.findViewById(R.id.tvBoardingHouseName);
            tvRoomDetails = itemView.findViewById(R.id.tvRoomDetails);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvBookingDates = itemView.findViewById(R.id.tvBookingDates);
            tvMonthlyDue = itemView.findViewById(R.id.tvMonthlyDue);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
