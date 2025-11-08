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
        
        // Set location
        holder.tvLocation.setText(booking.getLocation());
        
        // Set booking dates
        holder.tvBookingDates.setText(booking.getStartDate() + " - " + booking.getEndDate());
        
        // Set monthly due
        holder.tvMonthlyDue.setText(booking.getMonthlyDue());
        
        // Set status - Display "Active" instead of "Confirmed" for Current Boarding House Booked section
        String status = booking.getStatus();
        String displayStatus = status;
        if ("Confirmed".equals(status)) {
            displayStatus = "Active";
        }
        holder.tvStatus.setText(displayStatus);
        
        // Set status background based on status (use original status, not display status)
        if ("Confirmed".equals(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_approved);
        } else if ("Completed".equals(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
        } else if ("Pending".equals(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
        } else if ("Cancelled".equals(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_cancelled); // Red background for cancelled bookings
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
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
            
            // Set "See Details" button click listener
            holder.btnSeeDetails.setOnClickListener(v -> {
                bookingClickListener.onBookingClick(booking);
            });
        } else {
            // Remove click listeners if no listener provided
            holder.itemView.setOnClickListener(null);
            holder.btnSeeDetails.setOnClickListener(null);
            holder.btnSeeDetails.setVisibility(View.GONE); // Hide button if no click listener
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
    
    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBoardingHouse;
        TextView tvBoardingHouseName, tvLocation, tvBookingDates, tvMonthlyDue, tvStatus;
        com.google.android.material.button.MaterialButton btnSeeDetails;
        
        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBoardingHouse = itemView.findViewById(R.id.imgBoardingHouse);
            tvBoardingHouseName = itemView.findViewById(R.id.tvBoardingHouseName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvBookingDates = itemView.findViewById(R.id.tvBookingDates);
            tvMonthlyDue = itemView.findViewById(R.id.tvMonthlyDue);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnSeeDetails = itemView.findViewById(R.id.btnSeeDetails);
        }
    }
}
