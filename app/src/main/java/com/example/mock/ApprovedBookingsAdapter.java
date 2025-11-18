package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ApprovedBookingsAdapter extends RecyclerView.Adapter<ApprovedBookingsAdapter.ViewHolder> {
    
    private List<BookingData> bookings;
    private OnBookingActionListener listener;
    
    public interface OnBookingActionListener {
        void onViewDetails(BookingData booking);
    }
    
    public ApprovedBookingsAdapter(List<BookingData> bookings, OnBookingActionListener listener) {
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
        
        // Set status - should be "Confirmed" for approved bookings
        String status = booking.getStatus() != null ? booking.getStatus() : "Confirmed";
        holder.tvStatus.setText(status);
        
        // Update status color based on status
        // Pending - orange, Confirmed - blue, Completed - green
        if ("Confirmed".equals(status)) {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed); // Blue for Confirmed
        } else if ("Cancelled".equals(status)) {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_rounded_red);
        } else if ("Completed".equals(status)) {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_approved); // Green for Completed
        } else {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending); // Orange for Pending
        }
        
        holder.tvEmail.setText(booking.getEmail() != null ? booking.getEmail() : "");
        holder.tvPhone.setText(booking.getPhoneNumber() != null ? booking.getPhoneNumber() : "");
        holder.tvRoomName.setText(booking.getRoomName() != null ? booking.getRoomName() : "");
        
        // Format dates with time
        String startDate = booking.getStartDate();
        String endDate = booking.getEndDate();
        holder.tvStartDate.setText(startDate != null && !startDate.isEmpty() ? formatDateTime(startDate) : "");
        holder.tvEndDate.setText(endDate != null && !endDate.isEmpty() ? formatDateTime(endDate) : "");
        
        // Set room amount (monthly price)
        String roomAmount = booking.getAmount();
        if (roomAmount != null && !roomAmount.isEmpty()) {
            // Format if not already formatted
            if (!roomAmount.startsWith("₱")) {
                holder.tvAmount.setText("₱" + formatAmount(roomAmount));
            } else {
                holder.tvAmount.setText(roomAmount);
            }
        } else {
            holder.tvAmount.setText("₱0.00");
        }
        
        // Set total booking amount (from payment_breakdowns)
        String totalBookingAmount = booking.getTotalAmountForBooking();
        int totalPeriods = booking.getTotalPeriods();
        
        // Try to use total_amount_for_booking if payment breakdown exists (total_periods > 0)
        boolean useTotalAmount = false;
        if (totalPeriods > 0 && totalBookingAmount != null && !totalBookingAmount.isEmpty()) {
            try {
                double totalValue = Double.parseDouble(totalBookingAmount);
                // Use total amount if breakdown exists, even if it's 0 (they might not have paid yet)
                holder.tvTotalBookingAmount.setText("₱" + formatAmount(totalBookingAmount));
                useTotalAmount = true;
            } catch (NumberFormatException e) {
                // Will use fallback
            }
        } else if (totalBookingAmount != null && !totalBookingAmount.isEmpty() && !totalBookingAmount.equals("0.00")) {
            // If no breakdown but total amount is provided and > 0, use it
            try {
                double totalValue = Double.parseDouble(totalBookingAmount);
                if (totalValue > 0) {
                    holder.tvTotalBookingAmount.setText("₱" + formatAmount(totalBookingAmount));
                    useTotalAmount = true;
                }
            } catch (NumberFormatException e) {
                // Will use fallback
            }
        }
        
        // Fallback to room amount if total amount is not valid or no breakdown exists
        if (!useTotalAmount) {
            if (roomAmount != null && !roomAmount.isEmpty()) {
                if (!roomAmount.startsWith("₱")) {
                    holder.tvTotalBookingAmount.setText("₱" + formatAmount(roomAmount));
                } else {
                    holder.tvTotalBookingAmount.setText(roomAmount);
                }
            } else {
                holder.tvTotalBookingAmount.setText("₱0.00");
            }
        }
        
        holder.tvRentType.setText(booking.getRentType() != null ? booking.getRentType() : "");
        
        // Hide action buttons for approved bookings
        holder.btnApprove.setVisibility(View.GONE);
        holder.btnDecline.setVisibility(View.GONE);
        
        // Make card clickable to open booking details
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewDetails(booking);
            }
        });
        
        // Hide View Details button
        if (holder.btnViewDetails != null) {
            holder.btnViewDetails.setVisibility(View.GONE);
        }
    }
    
    @Override
    public int getItemCount() {
        return bookings.size();
    }
    
    private String formatAmount(String amount) {
        try {
            double amountValue = Double.parseDouble(amount);
            java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,##0.00");
            return formatter.format(amountValue);
        } catch (NumberFormatException e) {
            return amount;
        }
    }
    
    private String formatDateTime(String dateTime) {
        // Format from "YYYY-MM-DD HH:MM:SS" to "MMM DD, YYYY hh:mm a" (e.g., "Jan 15, 2025 02:00 PM")
        if (dateTime == null || dateTime.isEmpty()) {
            return "";
        }
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault());
            java.util.Date date = inputFormat.parse(dateTime);
            return outputFormat.format(date);
        } catch (Exception e) {
            // If parsing fails, try date-only format
            try {
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
                java.util.Date date = inputFormat.parse(dateTime);
                return outputFormat.format(date);
            } catch (Exception e2) {
                return dateTime; // Return original if parsing fails
            }
        }
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBoarderName, tvStatus, tvEmail, tvPhone, tvRoomName, tvStartDate, tvEndDate, tvAmount, tvTotalBookingAmount, tvRentType;
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
            tvTotalBookingAmount = itemView.findViewById(R.id.tvTotalBookingAmount);
            tvRentType = itemView.findViewById(R.id.tvRentType);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnDecline = itemView.findViewById(R.id.btnDecline);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }
    }
}








