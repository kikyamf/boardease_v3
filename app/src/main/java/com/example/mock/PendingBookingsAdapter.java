package com.example.mock;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
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
        
        // Set boarder name
        if (booking.getBoarderName() != null && !booking.getBoarderName().isEmpty()) {
            holder.tvBoarderName.setText(booking.getBoarderName());
        } else {
            holder.tvBoarderName.setText("Unknown");
        }
        
        // Set status
        String status = booking.getStatus() != null ? booking.getStatus() : "Pending";
        holder.tvStatus.setText(status);
        
        // Update status color based on status
        // Pending - orange, Confirmed - blue, Completed - green
        if ("Confirmed".equals(status) || "Approved".equals(status)) {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed); // Blue for Confirmed/Approved
        } else if ("Cancelled".equals(status)) {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_rounded_red);
        } else if ("Completed".equals(status)) {
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_approved); // Green for Completed
        } else {
            // Pending - orange
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
        
        // Debug logging
        android.util.Log.d("PendingBookingsAdapter", "Booking ID: " + booking.getBookingId() + 
                          ", Total Amount For Booking: [" + totalBookingAmount + "]" + 
                          ", Room Amount: [" + roomAmount + "]" +
                          ", Total Periods: " + totalPeriods);
        
        // Try to use total_amount_for_booking if payment breakdown exists (total_periods > 0)
        // Even if the amount is currently 0, if breakdown exists, we should show it
        boolean useTotalAmount = false;
        if (totalPeriods > 0 && totalBookingAmount != null && !totalBookingAmount.isEmpty()) {
            try {
                double totalValue = Double.parseDouble(totalBookingAmount);
                // Use total amount if breakdown exists, even if it's 0 (they might not have paid yet)
                holder.tvTotalBookingAmount.setText("₱" + formatAmount(totalBookingAmount));
                useTotalAmount = true;
                android.util.Log.d("PendingBookingsAdapter", "Set total amount from breakdown (periods exist): " + holder.tvTotalBookingAmount.getText());
            } catch (NumberFormatException e) {
                android.util.Log.e("PendingBookingsAdapter", "Error parsing total amount: " + totalBookingAmount, e);
            }
        } else if (totalBookingAmount != null && !totalBookingAmount.isEmpty() && !totalBookingAmount.equals("0.00")) {
            // If no breakdown but total amount is provided and > 0, use it
            try {
                double totalValue = Double.parseDouble(totalBookingAmount);
                if (totalValue > 0) {
                    holder.tvTotalBookingAmount.setText("₱" + formatAmount(totalBookingAmount));
                    useTotalAmount = true;
                    android.util.Log.d("PendingBookingsAdapter", "Set total amount from breakdown (no periods but value > 0): " + holder.tvTotalBookingAmount.getText());
                }
            } catch (NumberFormatException e) {
                android.util.Log.e("PendingBookingsAdapter", "Error parsing total amount: " + totalBookingAmount, e);
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
                android.util.Log.d("PendingBookingsAdapter", "Using room amount as total (no breakdown): " + holder.tvTotalBookingAmount.getText());
            } else {
                holder.tvTotalBookingAmount.setText("₱0.00");
                android.util.Log.w("PendingBookingsAdapter", "No amount data available, showing 0.00");
            }
        }
        
        holder.tvRentType.setText(booking.getRentType() != null ? booking.getRentType() : "");
        
        // Set payment information
        int paidPeriods = booking.getPaidPeriods();
        String paidAmount = booking.getPaidAmountForBooking();
        String totalAmount = booking.getTotalAmountForBooking();
        double progressPercent = booking.getPaymentProgressPercent();
        boolean isFullyPaid = booking.isFullyPaid();
        
        // Show payment info if there's payment data
        if (totalPeriods > 0 || (paidAmount != null && !paidAmount.isEmpty() && Double.parseDouble(paidAmount) > 0)) {
            holder.layoutPaymentInfo.setVisibility(View.VISIBLE);
            
            // Set amounts with fallback
            if (paidAmount != null && !paidAmount.isEmpty() && Double.parseDouble(paidAmount) > 0) {
                holder.tvAmountPaid.setText("₱" + formatAmount(paidAmount));
            } else {
                holder.tvAmountPaid.setText("₱0.00");
            }
            
            if (totalAmount != null && !totalAmount.isEmpty() && Double.parseDouble(totalAmount) > 0) {
                holder.tvTotalAmount.setText("₱" + formatAmount(totalAmount));
            } else {
                holder.tvTotalAmount.setText(booking.getAmount() != null ? booking.getAmount() : "₱0.00");
            }
            
            // Show payment progress if available
            if (totalPeriods > 0) {
                holder.layoutPaymentProgress.setVisibility(View.VISIBLE);
                
                int totalMonths = booking.getTotalMonthsForBooking();
                int paidMonths = booking.getPaidMonthsForBooking();
                
                // Show "months" only if all periods are exactly monthly
                if (totalMonths > 0 && totalPeriods == totalMonths) {
                    if (totalMonths == 1) {
                        holder.tvPaymentProgress.setText(String.format("%d/%d month paid", paidMonths, totalMonths));
                    } else {
                        holder.tvPaymentProgress.setText(String.format("%d/%d months paid", paidMonths, totalMonths));
                    }
                } else {
                    if (totalPeriods == 1) {
                        holder.tvPaymentProgress.setText(String.format("%d/%d period paid", paidPeriods, totalPeriods));
                    } else {
                        holder.tvPaymentProgress.setText(String.format("%d/%d periods paid", paidPeriods, totalPeriods));
                    }
                }
                
                holder.progressBarPayment.setProgress((int) progressPercent);
                holder.tvProgressPercent.setText(String.format("%.0f%%", progressPercent));
                
                // Set progress bar color
                if (isFullyPaid) {
                    holder.progressBarPayment.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                    holder.tvPaymentProgress.setTextColor(Color.parseColor("#4CAF50"));
                } else if (progressPercent >= 50) {
                    holder.progressBarPayment.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF9800")));
                    holder.tvPaymentProgress.setTextColor(Color.parseColor("#FF9800"));
                } else {
                    holder.progressBarPayment.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336")));
                    holder.tvPaymentProgress.setTextColor(Color.parseColor("#F44336"));
                }
            } else {
                holder.layoutPaymentProgress.setVisibility(View.GONE);
            }
        } else {
            holder.layoutPaymentInfo.setVisibility(View.GONE);
        }

        // Hide Approve and Decline buttons in the card layout
        if (holder.btnApprove != null) {
            holder.btnApprove.setVisibility(View.GONE);
        }
        if (holder.btnDecline != null) {
            holder.btnDecline.setVisibility(View.GONE);
        }
        
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
            DecimalFormat formatter = new DecimalFormat("#,##0.00");
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
        TextView tvBoarderName, tvStatus, tvEmail, tvPhone, tvRoomName, tvStartDate, tvEndDate, tvAmount, tvRentType;
        TextView tvTotalBookingAmount, tvAmountPaid, tvTotalAmount, tvPaymentProgress, tvProgressPercent;
        LinearLayout layoutPaymentInfo, layoutPaymentProgress;
        ProgressBar progressBarPayment;
        Button btnApprove, btnDecline, btnViewDetails;

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
            
            // Payment info views
            layoutPaymentInfo = itemView.findViewById(R.id.layoutPaymentInfo);
            layoutPaymentProgress = itemView.findViewById(R.id.layoutPaymentProgress);
            tvAmountPaid = itemView.findViewById(R.id.tvAmountPaid);
            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
            tvPaymentProgress = itemView.findViewById(R.id.tvPaymentProgress);
            tvProgressPercent = itemView.findViewById(R.id.tvProgressPercent);
            progressBarPayment = itemView.findViewById(R.id.progressBarPayment);
            
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnDecline = itemView.findViewById(R.id.btnDecline);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }
    }
}
































