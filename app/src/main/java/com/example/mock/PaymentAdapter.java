package com.example.mock;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.ViewHolder> {
    
    public static final int VIEW_TYPE_ALL = 0;
    public static final int VIEW_TYPE_FULLY_PAID = 1;
    public static final int VIEW_TYPE_REMAINING = 2;
    
    private List<PaymentData> payments;
    private Context context;
    private PaymentActionListener actionListener;
    private int viewType = VIEW_TYPE_ALL; // Default to all payments view
    
    public interface PaymentActionListener {
        void onMarkAsPaid(PaymentData payment);
        void onMarkAsOverdue(PaymentData payment);
        void onViewDetails(PaymentData payment);
    }
    
    public PaymentAdapter(List<PaymentData> payments) {
        this.payments = payments;
    }
    
    public PaymentAdapter(List<PaymentData> payments, PaymentActionListener actionListener) {
        this.payments = payments;
        this.actionListener = actionListener;
    }
    
    public PaymentAdapter(List<PaymentData> payments, PaymentActionListener actionListener, int viewType) {
        this.payments = payments;
        this.actionListener = actionListener;
        this.viewType = viewType;
    }
    
    public void setViewType(int viewType) {
        this.viewType = viewType;
        notifyDataSetChanged();
    }
    
    public void setActionListener(PaymentActionListener actionListener) {
        this.actionListener = actionListener;
    }
    
    @Override
    public int getItemViewType(int position) {
        return viewType;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view;
        
        switch (viewType) {
            case VIEW_TYPE_FULLY_PAID:
                view = LayoutInflater.from(context)
                        .inflate(R.layout.item_payment_fully_paid, parent, false);
                return new ViewHolder(view, VIEW_TYPE_FULLY_PAID);
            case VIEW_TYPE_REMAINING:
                view = LayoutInflater.from(context)
                        .inflate(R.layout.item_payment_remaining, parent, false);
                return new ViewHolder(view, VIEW_TYPE_REMAINING);
            case VIEW_TYPE_ALL:
            default:
                view = LayoutInflater.from(context)
                .inflate(R.layout.item_payment, parent, false);
                return new ViewHolder(view, VIEW_TYPE_ALL);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PaymentData payment = payments.get(position);
        
        holder.tvBoarderName.setText(payment.getBoarderName());
        holder.tvRoom.setText(payment.getRoom());
        holder.tvRentType.setText(payment.getRentType());
        
        // Bind based on view type
        switch (holder.viewType) {
            case VIEW_TYPE_FULLY_PAID:
                bindFullyPaidViewHolder(holder, payment);
                break;
            case VIEW_TYPE_REMAINING:
                bindRemainingViewHolder(holder, payment);
                break;
            case VIEW_TYPE_ALL:
            default:
                bindAllPaymentsViewHolder(holder, payment);
                break;
        }
        
        // Set click listener
        if (actionListener != null) {
            holder.itemView.setOnClickListener(v -> actionListener.onViewDetails(payment));
        }
    }
    
    private void bindFullyPaidViewHolder(ViewHolder holder, PaymentData payment) {
        holder.tvTotalAmount.setText("₱" + payment.getTotalAmountForBooking());
        holder.tvPaymentStatus.setText("FULLY PAID");
        holder.tvPaymentDate.setText("Completed: " + payment.getPaymentDate());
        
        // Hide elements not used in fully paid view
        if (holder.tvAmountPaid != null) holder.tvAmountPaid.setVisibility(View.GONE);
        if (holder.tvRentalStatus != null) holder.tvRentalStatus.setVisibility(View.GONE);
        if (holder.progressBarPayment != null) holder.progressBarPayment.setVisibility(View.GONE);
        if (holder.tvPaymentProgress != null) holder.tvPaymentProgress.setVisibility(View.GONE);
        if (holder.tvProgressPercent != null) holder.tvProgressPercent.setVisibility(View.GONE);
    }
    
    private void bindRemainingViewHolder(ViewHolder holder, PaymentData payment) {
        // Use accurate amounts from payment_breakdowns if available and valid (> 0)
        // Otherwise fall back to regular payment amounts
        String paidAmount = payment.getPaidAmountForBooking();
        boolean useBookingPaid = false;
        if (paidAmount != null && !paidAmount.isEmpty()) {
            try {
                double paidValue = Double.parseDouble(paidAmount);
                if (paidValue > 0) {
                    holder.tvAmountPaid.setText("₱" + formatAmount(paidAmount));
                    useBookingPaid = true;
                }
            } catch (NumberFormatException e) {
                // Invalid number, will fall back
            }
        }
        
        if (!useBookingPaid) {
            // Fall back to regular amount_paid field
            String fallbackPaid = payment.getAmountPaid();
            if (fallbackPaid != null && !fallbackPaid.isEmpty() && !fallbackPaid.equals("₱0.00")) {
                holder.tvAmountPaid.setText(fallbackPaid);
            } else {
                holder.tvAmountPaid.setText("₱0.00");
            }
        }
        
        if (holder.tvRemainingAmount != null) {
            String remainingAmount = payment.getRemainingAmountToPay();
            boolean useBookingRemaining = false;
            if (remainingAmount != null && !remainingAmount.isEmpty()) {
                try {
                    double remainingValue = Double.parseDouble(remainingAmount);
                    if (remainingValue > 0) {
                        holder.tvRemainingAmount.setText("₱" + formatAmount(remainingAmount));
                        useBookingRemaining = true;
                    }
                } catch (NumberFormatException e) {
                    // Invalid number, will calculate
                }
            }
            
            if (!useBookingRemaining) {
                // Calculate remaining from total and paid if breakdown amounts are not available
                String totalAmt = payment.getTotalAmountForBooking();
                String paidAmt = payment.getPaidAmountForBooking();
                boolean calculated = false;
                
                if (totalAmt != null && !totalAmt.isEmpty() && paidAmt != null && !paidAmt.isEmpty()) {
                    try {
                        double total = Double.parseDouble(totalAmt);
                        double paid = Double.parseDouble(paidAmt);
                        if (total > 0) {
                            double remaining = total - paid;
                            if (remaining > 0) {
                                holder.tvRemainingAmount.setText("₱" + formatAmount(String.valueOf(remaining)));
                                calculated = true;
                            } else {
                                holder.tvRemainingAmount.setText("₱0.00");
                                calculated = true;
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Will try fallback
                    }
                }
                
                if (!calculated) {
                    // Fall back to calculating from regular amounts
                    String fallbackTotal = payment.getTotalAmount();
                    String fallbackPaid = payment.getAmountPaid();
                    if (fallbackTotal != null && fallbackPaid != null) {
                        try {
                            // Remove ₱ and commas, then parse
                            String totalStr = fallbackTotal.replace("₱", "").replace(",", "").trim();
                            String paidStr = fallbackPaid.replace("₱", "").replace(",", "").trim();
                            if (!totalStr.isEmpty() && !paidStr.isEmpty()) {
                                double total = Double.parseDouble(totalStr);
                                double paid = Double.parseDouble(paidStr);
                                double remaining = total - paid;
                                if (remaining > 0) {
                                    holder.tvRemainingAmount.setText("₱" + formatAmount(String.valueOf(remaining)));
                                } else {
                                    holder.tvRemainingAmount.setText("₱0.00");
                                }
                            } else {
                                holder.tvRemainingAmount.setText("₱0.00");
                            }
                        } catch (NumberFormatException e) {
                            holder.tvRemainingAmount.setText("₱0.00");
                        }
                    } else {
                        holder.tvRemainingAmount.setText("₱0.00");
                    }
                }
            }
        }
        holder.tvPaymentStatus.setText("REMAINING");
        
        // Set payment progress
        int totalPeriods = payment.getTotalPeriods();
        int paidPeriods = payment.getPaidPeriods();
        int totalMonths = payment.getTotalMonthsForBooking();
        int paidMonths = payment.getPaidMonthsForBooking();
        double progressPercent = payment.getPaymentProgressPercent();
        
        if (totalPeriods > 0 && holder.tvPaymentProgress != null) {
            // Show "months" only if all periods are exactly monthly (total_periods == total_months)
            // Otherwise show "periods" (use singular "period" if only 1)
            if (totalMonths > 0 && totalPeriods == totalMonths) {
                // All periods are monthly, show as months (use singular "month" if only 1)
                if (totalMonths == 1) {
                    holder.tvPaymentProgress.setText(
                        String.format("%d/%d month paid", paidMonths, totalMonths)
                    );
                } else {
                    holder.tvPaymentProgress.setText(
                        String.format("%d/%d months paid", paidMonths, totalMonths)
                    );
                }
            } else {
                // Mixed periods or not all monthly, show as periods (use singular "period" if only 1)
                if (totalPeriods == 1) {
                    holder.tvPaymentProgress.setText(
                        String.format("%d/%d period paid", paidPeriods, totalPeriods)
                    );
                } else {
                    holder.tvPaymentProgress.setText(
                        String.format("%d/%d periods paid", paidPeriods, totalPeriods)
                    );
                }
            }
            
            if (holder.progressBarPayment != null) {
                holder.progressBarPayment.setProgress((int) progressPercent);
            }
            
            if (holder.tvProgressPercent != null) {
                holder.tvProgressPercent.setText(String.format("%.0f%%", progressPercent));
            }
        }
        
        // Set button click listener
        if (holder.btnViewDetails != null && actionListener != null) {
            holder.btnViewDetails.setOnClickListener(v -> actionListener.onViewDetails(payment));
        }
        
        // Hide elements not used in remaining view
        if (holder.tvRentalStatus != null) holder.tvRentalStatus.setVisibility(View.GONE);
        if (holder.tvPaymentDate != null) holder.tvPaymentDate.setVisibility(View.GONE);
    }
    
    private void bindAllPaymentsViewHolder(ViewHolder holder, PaymentData payment) {
        // Use accurate amounts from payment_breakdowns if available and valid (> 0)
        // Otherwise fall back to regular payment amounts
        String paidAmount = payment.getPaidAmountForBooking();
        boolean useBookingPaid = false;
        if (paidAmount != null && !paidAmount.isEmpty()) {
            try {
                double paidValue = Double.parseDouble(paidAmount);
                if (paidValue > 0) {
                    holder.tvAmountPaid.setText("₱" + formatAmount(paidAmount));
                    useBookingPaid = true;
                }
            } catch (NumberFormatException e) {
                // Invalid number, will fall back
            }
        }
        
        if (!useBookingPaid) {
            // Fall back to regular amount_paid field
            String fallbackPaid = payment.getAmountPaid();
            if (fallbackPaid != null && !fallbackPaid.isEmpty() && !fallbackPaid.equals("₱0.00")) {
                holder.tvAmountPaid.setText(fallbackPaid);
            } else {
                holder.tvAmountPaid.setText("₱0.00");
            }
        }
        
        String totalAmount = payment.getTotalAmountForBooking();
        boolean useBookingTotal = false;
        if (totalAmount != null && !totalAmount.isEmpty()) {
            try {
                double totalValue = Double.parseDouble(totalAmount);
                if (totalValue > 0) {
                    holder.tvTotalAmount.setText("₱" + formatAmount(totalAmount));
                    useBookingTotal = true;
                }
            } catch (NumberFormatException e) {
                // Invalid number, will fall back
            }
        }
        
        if (!useBookingTotal) {
            // Fall back to regular total_amount field
            String fallbackTotal = payment.getTotalAmount();
            if (fallbackTotal != null && !fallbackTotal.isEmpty() && !fallbackTotal.equals("₱0.00")) {
                holder.tvTotalAmount.setText(fallbackTotal);
            } else {
                holder.tvTotalAmount.setText("₱0.00");
            }
        }
        
        holder.tvPaymentStatus.setText(payment.getPaymentStatus());
        holder.tvRentalStatus.setText(payment.getRentalStatus());
        holder.tvPaymentDate.setText(payment.getPaymentDate());
        
        // Set card background color based on rental status
        String rentalStatus = payment.getRentalStatus();
        View cardView = holder.itemView;
        if (rentalStatus != null && rentalStatus.equalsIgnoreCase("Cancelled")) {
            // Make card background red for cancelled rentals
            if (cardView instanceof androidx.cardview.widget.CardView) {
                ((androidx.cardview.widget.CardView) cardView).setCardBackgroundColor(Color.parseColor("#FFEBEE"));
            } else {
                // If not CardView, try to set background color on the root view
                cardView.setBackgroundColor(Color.parseColor("#FFEBEE"));
            }
        } else {
            // Reset to white for non-cancelled
            if (cardView instanceof androidx.cardview.widget.CardView) {
                ((androidx.cardview.widget.CardView) cardView).setCardBackgroundColor(Color.parseColor("#FFFFFF"));
            } else {
                cardView.setBackgroundColor(Color.parseColor("#FFFFFF"));
            }
        }
        
        // Show "Fully Paid" badge at top right if payment is fully paid
        boolean isFullyPaid = payment.isFullyPaid();
        if (holder.tvStatusBadge != null) {
            if (isFullyPaid) {
                holder.tvStatusBadge.setText("FULLY PAID");
                holder.tvStatusBadge.setVisibility(View.VISIBLE);
                holder.tvStatusBadge.setTextColor(Color.parseColor("#FFFFFF"));
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_approved);
            } else {
                holder.tvStatusBadge.setVisibility(View.GONE);
            }
        }
        
        // Set payment progress for all payments view
        int totalPeriods = payment.getTotalPeriods();
        int paidPeriods = payment.getPaidPeriods();
        int totalMonths = payment.getTotalMonthsForBooking();
        int paidMonths = payment.getPaidMonthsForBooking();
        double progressPercent = payment.getPaymentProgressPercent();
        
        if (totalPeriods > 0 && holder.progressBarPayment != null) {
            // Show "months" only if all periods are exactly monthly (total_periods == total_months)
            // Otherwise show "periods" (use singular "period" if only 1)
            if (totalMonths > 0 && totalPeriods == totalMonths) {
                // All periods are monthly, show as months (use singular "month" if only 1)
                if (totalMonths == 1) {
                    holder.tvPaymentProgress.setText(
                        String.format("%d/%d month paid", paidMonths, totalMonths)
                    );
                } else {
                    holder.tvPaymentProgress.setText(
                        String.format("%d/%d months paid", paidMonths, totalMonths)
                    );
                }
            } else {
                // Mixed periods or not all monthly, show as periods (use singular "period" if only 1)
                if (totalPeriods == 1) {
                    holder.tvPaymentProgress.setText(
                        String.format("%d/%d period paid", paidPeriods, totalPeriods)
                    );
                } else {
                    holder.tvPaymentProgress.setText(
                        String.format("%d/%d periods paid", paidPeriods, totalPeriods)
                    );
                }
            }
            
            // Set progress bar
            holder.progressBarPayment.setProgress((int) progressPercent);
            holder.tvProgressPercent.setText(String.format("%.0f%%", progressPercent));
            
            // Set progress bar color based on completion
            if (isFullyPaid) {
                holder.progressBarPayment.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                );
                holder.tvPaymentProgress.setTextColor(Color.parseColor("#4CAF50"));
            } else if (progressPercent >= 50) {
                holder.progressBarPayment.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#FF9800"))
                );
                holder.tvPaymentProgress.setTextColor(Color.parseColor("#FF9800"));
            } else {
                holder.progressBarPayment.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336"))
                );
                holder.tvPaymentProgress.setTextColor(Color.parseColor("#F44336"));
            }
            
            // Show progress section
            holder.tvPaymentProgress.setVisibility(View.VISIBLE);
            holder.progressBarPayment.setVisibility(View.VISIBLE);
            holder.tvProgressPercent.setVisibility(View.VISIBLE);
        } else if (holder.progressBarPayment != null) {
            // Hide progress if no data
            holder.tvPaymentProgress.setVisibility(View.GONE);
            holder.progressBarPayment.setVisibility(View.GONE);
            holder.tvProgressPercent.setVisibility(View.GONE);
        }
        
        // Set status color based on payment status
        setStatusColor(holder.tvPaymentStatus, payment.getPaymentStatus());
    }
    
    private String formatAmount(String amount) {
        // Format amount with commas (e.g., "1000.00" -> "1,000.00")
        try {
            double amountValue = Double.parseDouble(amount);
            java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,##0.00");
            return formatter.format(amountValue);
        } catch (NumberFormatException e) {
            return amount;
        }
    }
    
    private void setStatusColor(TextView statusView, String status) {
        switch (status.toLowerCase()) {
            case "paid":
            case "completed":
                statusView.setTextColor(Color.parseColor("#4CAF50"));
                statusView.setBackgroundColor(Color.parseColor("#E8F5E8"));
                break;
            case "pending":
                statusView.setTextColor(Color.parseColor("#FF9800"));
                statusView.setBackgroundColor(Color.parseColor("#FFF3E0"));
                break;
            case "overdue":
                statusView.setTextColor(Color.parseColor("#F44336"));
                statusView.setBackgroundColor(Color.parseColor("#FFEBEE"));
                break;
            default:
                statusView.setTextColor(Color.parseColor("#666666"));
                statusView.setBackgroundColor(Color.parseColor("#F5F5F5"));
                break;
        }
    }
    
    @Override
    public int getItemCount() {
        return payments.size();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        int viewType;
        TextView tvBoarderName, tvRoom, tvRentType, tvAmountPaid, tvTotalAmount;
        TextView tvPaymentStatus, tvRentalStatus, tvPaymentDate;
        TextView tvPaymentProgress, tvProgressPercent, tvRemainingAmount;
        TextView tvStatusBadge; // Badge for "Fully Paid" status at top right
        android.widget.ProgressBar progressBarPayment;
        com.google.android.material.button.MaterialButton btnViewDetails;
        
        public ViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            this.viewType = viewType;
            
            // Common views
            tvBoarderName = itemView.findViewById(R.id.tvBoarderName);
            tvRoom = itemView.findViewById(R.id.tvRoom);
            tvRentType = itemView.findViewById(R.id.tvRentType);
            tvPaymentStatus = itemView.findViewById(R.id.tvPaymentStatus);
            
            // Views that may not exist in all layouts
            tvAmountPaid = itemView.findViewById(R.id.tvAmountPaid);
            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
            tvRentalStatus = itemView.findViewById(R.id.tvRentalStatus);
            tvPaymentDate = itemView.findViewById(R.id.tvPaymentDate);
            tvPaymentProgress = itemView.findViewById(R.id.tvPaymentProgress);
            tvProgressPercent = itemView.findViewById(R.id.tvProgressPercent);
            progressBarPayment = itemView.findViewById(R.id.progressBarPayment);
            tvRemainingAmount = itemView.findViewById(R.id.tvRemainingAmount);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge); // Status badge for "Fully Paid"
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }
    }
}

































