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
    private List<BoarderBookingFragment.Booking> bookings;
    private OnBookingClickListener onBookingClickListener;
    
    public interface OnBookingClickListener {
        void onBookingClick(BoarderBookingFragment.Booking booking);
    }
    
    public BookingAdapter(Context context, List<BoarderBookingFragment.Booking> bookings, OnBookingClickListener listener) {
        this.context = context;
        this.bookings = bookings;
        this.onBookingClickListener = listener;
    }
    
    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        BoarderBookingFragment.Booking booking = bookings.get(position);
        
        // Set booking data
        holder.tvBoardingHouseName.setText(booking.getBoardingHouseName());
        holder.tvLocation.setText(booking.getLocation());
        holder.tvStartDate.setText(booking.getStartDate());
        holder.tvEndDate.setText(booking.getEndDate());
        holder.tvMonthlyDue.setText(booking.getMonthlyDue());
        holder.tvStatus.setText(booking.getStatus());
        
        // Set image
        if (booking.getImagePath() != null && !booking.getImagePath().isEmpty()) {
            Glide.with(context)
                    .load(booking.getImagePath())
                    .placeholder(R.drawable.sample_listing)
                    .error(R.drawable.sample_listing)
                    .into(holder.imgBoardingHouse);
        } else {
            holder.imgBoardingHouse.setImageResource(R.drawable.sample_listing);
        }
        
        // Set status background
        if ("Active".equals(booking.getStatus())) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_approved);
        } else if ("Completed".equals(booking.getStatus())) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
        }
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (onBookingClickListener != null) {
                onBookingClickListener.onBookingClick(booking);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return bookings.size();
    }
    
    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBoardingHouse;
        TextView tvBoardingHouseName;
        TextView tvLocation;
        TextView tvStartDate;
        TextView tvEndDate;
        TextView tvMonthlyDue;
        TextView tvStatus;
        
        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBoardingHouse = itemView.findViewById(R.id.imgBoardingHouse);
            tvBoardingHouseName = itemView.findViewById(R.id.tvBoardingHouseName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvStartDate = itemView.findViewById(R.id.tvStartDate);
            tvEndDate = itemView.findViewById(R.id.tvEndDate);
            tvMonthlyDue = itemView.findViewById(R.id.tvMonthlyDue);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
