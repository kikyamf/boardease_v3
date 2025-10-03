package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PendingBookingsAdapter extends RecyclerView.Adapter<PendingBookingsAdapter.ViewHolder> {

    private List<BookingData> bookings;
    private OnBookingActionListener listener;

    public interface OnBookingActionListener {
        void onApprove(BookingData booking);
        void onDecline(BookingData booking);
        void onViewDetails(BookingData booking);
    }

    public PendingBookingsAdapter(List<BookingData> bookings, OnBookingActionListener listener) {
        this.bookings = bookings;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BookingData booking = bookings.get(position);
        
        holder.tvBoarderName.setText(booking.getBoarderName());
        holder.tvEmail.setText(booking.getEmail());
        holder.tvPhone.setText(booking.getPhoneNumber());
        holder.tvRoomName.setText(booking.getRoomName());
        holder.tvStartDate.setText(booking.getStartDate());
        holder.tvEndDate.setText(booking.getEndDate());
        holder.tvAmount.setText(booking.getAmount());
        holder.tvRentType.setText(booking.getRentType());

        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onApprove(booking);
            }
        });

        holder.btnDecline.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDecline(booking);
            }
        });

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
        TextView tvBoarderName, tvEmail, tvPhone, tvRoomName, tvStartDate, tvEndDate, tvAmount, tvRentType;
        Button btnApprove, btnDecline, btnViewDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBoarderName = itemView.findViewById(R.id.tvBoarderName);
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





