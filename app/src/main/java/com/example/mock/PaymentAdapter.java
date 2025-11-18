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
    public static final int VIEW_TYPE_PENDING = 3;
    
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
            case VIEW_TYPE_PENDING:
                view = LayoutInflater.from(context)
                        .inflate(R.layout.item_payment, parent, false);
                return new ViewHolder(view, VIEW_TYPE_PENDING);
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
            case VIEW_TYPE_PENDING:
                bindPendingViewHolder(holder, payment);
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
        // Check if fully paid
        int totalPeriods = payment.getTotalPeriods();
        int paidPeriods = payment.getPaidPeriods();
        boolean isFullyPaid = payment.isFullyPaid();
        
        // If we have period data, verify fully paid status
        if (totalPeriods > 0) {
            isFullyPaid = isFullyPaid && paidPeriods >= totalPeriods;
        }
        
        // Also check payment status string as fallback
        String paymentStatusStr = payment.getPaymentStatus();
        if (paymentStatusStr != null && paymentStatusStr.equalsIgnoreCase("Fully Paid")) {
            isFullyPaid = true;
        }
        
        // Total Amount logic (same as All Payments view):
        // - If fully paid AND only 1 period exists, total = amount paid (166.67)
        // - If fully paid AND amount paid equals total amount, total = amount paid (166.67)
        // - Otherwise, total = sum of all periods from payment breakdown
        String totalAmount = payment.getTotalAmountForBooking();
        String paidAmount = payment.getPaidAmountForBooking();
        boolean useBookingTotal = false;
        
        // Check if fully paid with only 1 period - then total should equal amount paid
        if (isFullyPaid && (totalPeriods == 1 || totalPeriods == 0)) {
            // Only 1 period (or no period data) and fully paid - use amount paid as total
            String amountToUse = null;
            if (paidAmount != null && !paidAmount.isEmpty()) {
                try {
                    double paidValue = Double.parseDouble(paidAmount);
                    if (paidValue > 0) {
                        amountToUse = paidAmount;
                    }
                } catch (NumberFormatException e) {
                    // Will fall through
                }
            }
            
            // Try regular amount_paid field if breakdown data not available
            if (amountToUse == null) {
                String fallbackPaid = payment.getAmountPaid();
                if (fallbackPaid != null && !fallbackPaid.isEmpty() && !fallbackPaid.equals("₱0.00")) {
                    // Remove ₱ and commas, then use it
                    amountToUse = fallbackPaid.replace("₱", "").replace(",", "");
                }
            }
            
            if (amountToUse != null) {
                try {
                    double paidValue = Double.parseDouble(amountToUse);
                    if (paidValue > 0) {
                        holder.tvTotalAmount.setText("₱" + formatAmount(amountToUse));
                        useBookingTotal = true;
                    }
                } catch (NumberFormatException e) {
                    // Will fall through to use total amount
                }
            }
        }
        
        // Also check if amount paid equals total amount (fully paid single period scenario)
        if (!useBookingTotal && isFullyPaid && paidAmount != null && totalAmount != null) {
            try {
                double paidValue = Double.parseDouble(paidAmount);
                double totalValue = Double.parseDouble(totalAmount);
                // If they're equal or very close, it's likely a single period payment
                if (paidValue > 0 && Math.abs(paidValue - totalValue) < 0.01) {
                    holder.tvTotalAmount.setText("₱" + formatAmount(paidAmount));
                    useBookingTotal = true;
                }
            } catch (NumberFormatException e) {
                // Will fall through to use total amount
            }
        }
        
        // If not handled above, use total amount from booking breakdown
        if (!useBookingTotal && totalAmount != null && !totalAmount.isEmpty()) {
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
                // Last resort: use paid amount if available
                String lastResortPaid = payment.getAmountPaid();
                if (lastResortPaid != null && !lastResortPaid.isEmpty() && !lastResortPaid.equals("₱0.00")) {
                    holder.tvTotalAmount.setText(lastResortPaid);
                } else {
                    holder.tvTotalAmount.setText("₱0.00");
                }
            }
        }
        
        holder.tvPaymentStatus.setText("FULLY PAID");
        String paymentDate = payment.getPaymentDate();
        if (paymentDate != null && !paymentDate.isEmpty()) {
            holder.tvPaymentDate.setText("Completed: " + formatDateTime(paymentDate));
        } else {
            holder.tvPaymentDate.setText("Completed: N/A");
        }
        
        // Hide elements not used in fully paid view
        if (holder.tvAmountPaid != null) holder.tvAmountPaid.setVisibility(View.GONE);
        if (holder.tvRentalStatus != null) holder.tvRentalStatus.setVisibility(View.GONE);
        if (holder.progressBarPayment != null) holder.progressBarPayment.setVisibility(View.GONE);
        if (holder.tvPaymentProgress != null) holder.tvPaymentProgress.setVisibility(View.GONE);
        if (holder.tvProgressPercent != null) holder.tvProgressPercent.setVisibility(View.GONE);
    }
    
    private void bindRemainingViewHolder(ViewHolder holder, PaymentData payment) {
        // Set total amount
        if (holder.tvTotalAmount != null) {
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
        }
        
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
        
        // Change status badge from "REMAINING" to "PARTIALLY PAID" with blue color
        holder.tvPaymentStatus.setText("PARTIALLY PAID");
        holder.tvPaymentStatus.setBackgroundResource(R.drawable.bg_status_completed);
        holder.tvPaymentStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
        
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
        
        // Hide the "View Details & Update" button
        if (holder.btnViewDetails != null) {
            holder.btnViewDetails.setVisibility(View.GONE);
        }
        
        // Hide elements not used in remaining view
        if (holder.tvRentalStatus != null) holder.tvRentalStatus.setVisibility(View.GONE);
        if (holder.tvPaymentDate != null) holder.tvPaymentDate.setVisibility(View.GONE);
    }
    
    private void bindPendingViewHolder(ViewHolder holder, PaymentData payment) {
        // Similar to bindAllPaymentsViewHolder but with orange border and PENDING badge only
        
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
        
        // Set total amount
        int totalPeriods = payment.getTotalPeriods();
        int paidPeriods = payment.getPaidPeriods();
        boolean isFullyPaid = payment.isFullyPaid();
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
            String fallbackTotal = payment.getTotalAmount();
            if (fallbackTotal != null && !fallbackTotal.isEmpty() && !fallbackTotal.equals("₱0.00")) {
                holder.tvTotalAmount.setText(fallbackTotal);
            } else {
                holder.tvTotalAmount.setText("₱0.00");
            }
        }
        
        // Hide duplicate status text - we'll show badge instead
        if (holder.tvPaymentStatus != null) {
            holder.tvPaymentStatus.setVisibility(View.GONE);
        }
        
        holder.tvRentalStatus.setText(payment.getRentalStatus());
        String paymentDate = payment.getPaymentDate();
        if (paymentDate != null && !paymentDate.isEmpty()) {
            holder.tvPaymentDate.setText(formatDateTime(paymentDate));
        } else {
            holder.tvPaymentDate.setText("N/A");
        }
        
        // Set card background color based on rental status
        String rentalStatus = payment.getRentalStatus();
        View cardView = holder.itemView;
        if (rentalStatus != null && rentalStatus.equalsIgnoreCase("Cancelled")) {
            if (cardView instanceof androidx.cardview.widget.CardView) {
                ((androidx.cardview.widget.CardView) cardView).setCardBackgroundColor(Color.parseColor("#FFEBEE"));
            } else {
                cardView.setBackgroundColor(Color.parseColor("#FFEBEE"));
            }
        } else {
            if (cardView instanceof androidx.cardview.widget.CardView) {
                ((androidx.cardview.widget.CardView) cardView).setCardBackgroundColor(Color.parseColor("#FFFFFF"));
            } else {
                cardView.setBackgroundColor(Color.parseColor("#FFFFFF"));
            }
        }
        
        // Show orange PENDING badge
        if (holder.tvStatusBadge != null) {
            holder.tvStatusBadge.setText("PENDING");
            holder.tvStatusBadge.setVisibility(View.VISIBLE);
            holder.tvStatusBadge.setTextColor(Color.parseColor("#FFFFFF"));
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pending);
        }
        
        // Set orange card border for pending payments
        if (cardView instanceof androidx.cardview.widget.CardView) {
            android.graphics.drawable.GradientDrawable borderDrawable = new android.graphics.drawable.GradientDrawable();
            borderDrawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            borderDrawable.setColor(Color.parseColor("#FFFFFF"));
            borderDrawable.setStroke((int)(2 * context.getResources().getDisplayMetrics().density), Color.parseColor("#FF9800")); // Orange border, 2dp width
            borderDrawable.setCornerRadius(8 * context.getResources().getDisplayMetrics().density); // 8dp corner radius
            cardView.setBackground(borderDrawable);
        }
        
        // Set payment progress (similar to bindAllPaymentsViewHolder but with orange color)
        int totalMonths = payment.getTotalMonthsForBooking();
        int paidMonths = payment.getPaidMonthsForBooking();
        double progressPercent = payment.getPaymentProgressPercent();
        
        if (progressPercent == 0 && totalPeriods > 0) {
            progressPercent = (paidPeriods * 100.0) / totalPeriods;
        }
        
        if ((totalPeriods > 0 || isFullyPaid) && holder.progressBarPayment != null) {
            if (totalMonths > 0 && totalPeriods == totalMonths) {
                if (totalMonths == 1) {
                    holder.tvPaymentProgress.setText(String.format("%d/%d month paid", paidMonths, totalMonths));
                } else {
                    holder.tvPaymentProgress.setText(String.format("%d/%d months paid", paidMonths, totalMonths));
                }
            } else {
                if (totalPeriods == 1) {
                    holder.tvPaymentProgress.setText(String.format("%d/%d period paid", paidPeriods, totalPeriods));
                } else if (totalPeriods > 1) {
                    holder.tvPaymentProgress.setText(String.format("%d/%d periods paid", paidPeriods, totalPeriods));
                } else {
                    holder.tvPaymentProgress.setText("No period data");
                }
            }
            
            holder.progressBarPayment.setProgress((int) progressPercent);
            holder.tvProgressPercent.setText(String.format("%.0f%%", progressPercent));
            
            // Set progress bar to orange for pending payments
            holder.progressBarPayment.setProgressTintList(
                android.content.res.ColorStateList.valueOf(Color.parseColor("#FF9800"))
            );
            holder.tvPaymentProgress.setTextColor(Color.parseColor("#FF9800"));
            
            holder.tvPaymentProgress.setVisibility(View.VISIBLE);
            holder.progressBarPayment.setVisibility(View.VISIBLE);
            holder.tvProgressPercent.setVisibility(View.VISIBLE);
        } else if (holder.progressBarPayment != null) {
            if (paidPeriods > 0 || totalPeriods > 0) {
                holder.tvPaymentProgress.setVisibility(View.VISIBLE);
                holder.progressBarPayment.setVisibility(View.VISIBLE);
                holder.tvProgressPercent.setVisibility(View.VISIBLE);
                
                if (totalPeriods > 0) {
                    int defaultProgress = (int) ((paidPeriods * 100.0) / totalPeriods);
                    holder.progressBarPayment.setProgress(defaultProgress);
                    holder.tvProgressPercent.setText(defaultProgress + "%");
                    holder.tvPaymentProgress.setText(String.format("%d/%d periods paid", paidPeriods, totalPeriods));
                }
                
                // Set progress bar to orange
                holder.progressBarPayment.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#FF9800"))
                );
                holder.tvPaymentProgress.setTextColor(Color.parseColor("#FF9800"));
            } else {
                holder.tvPaymentProgress.setVisibility(View.GONE);
                holder.progressBarPayment.setVisibility(View.GONE);
                holder.tvProgressPercent.setVisibility(View.GONE);
            }
        }
        
        // Don't show status badge logic here - we already set PENDING badge above
        // Remove any duplicate status badge logic from bindAllPaymentsViewHolder
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
        
        // Check if fully paid
        // First check the isFullyPaid flag from database
        // Then verify with period data if available
        int totalPeriods = payment.getTotalPeriods();
        int paidPeriods = payment.getPaidPeriods();
        boolean isFullyPaid = payment.isFullyPaid();
        
        // If we have period data, verify fully paid status
        if (totalPeriods > 0) {
            isFullyPaid = isFullyPaid && paidPeriods >= totalPeriods;
        }
        
        // Also check payment status string as fallback
        String paymentStatusStr = payment.getPaymentStatus();
        if (paymentStatusStr != null && paymentStatusStr.equalsIgnoreCase("Fully Paid")) {
            isFullyPaid = true;
        }
        
        // Total Amount logic:
        // - If fully paid AND only 1 period exists, total = amount paid (166.67)
        // - If fully paid AND amount paid equals total amount, total = amount paid (166.67)
        // - If fully paid AND multiple periods, total = sum of all periods (5000)
        // - If not fully paid, total = sum of all periods
        // Note: paidAmount is already declared above (line 281)
        String totalAmount = payment.getTotalAmountForBooking();
        boolean useBookingTotal = false;
        
        // Check if fully paid with only 1 period - then total should equal amount paid
        if (isFullyPaid && (totalPeriods == 1 || totalPeriods == 0)) {
            // Only 1 period (or no period data) and fully paid - use amount paid as total
            String amountToUse = null;
            if (useBookingPaid && paidAmount != null && !paidAmount.isEmpty()) {
                amountToUse = paidAmount;
            } else {
                // Try regular amount_paid field
                String fallbackPaid = payment.getAmountPaid();
                if (fallbackPaid != null && !fallbackPaid.isEmpty() && !fallbackPaid.equals("₱0.00")) {
                    // Remove ₱ and commas, then use it
                    amountToUse = fallbackPaid.replace("₱", "").replace(",", "");
                }
            }
            
            if (amountToUse != null) {
                try {
                    double paidValue = Double.parseDouble(amountToUse);
                    if (paidValue > 0) {
                        holder.tvTotalAmount.setText("₱" + formatAmount(amountToUse));
                        useBookingTotal = true;
                    }
                } catch (NumberFormatException e) {
                    // Will fall through to use total amount
                }
            }
        }
        
        // Also check if amount paid equals total amount (fully paid single period scenario)
        if (!useBookingTotal && isFullyPaid) {
            String paidToCompare = null;
            String totalToCompare = null;
            
            // Get paid amount to compare
            if (useBookingPaid && paidAmount != null && !paidAmount.isEmpty()) {
                paidToCompare = paidAmount;
            } else {
                String fallbackPaid = payment.getAmountPaid();
                if (fallbackPaid != null && !fallbackPaid.isEmpty() && !fallbackPaid.equals("₱0.00")) {
                    paidToCompare = fallbackPaid.replace("₱", "").replace(",", "");
                }
            }
            
            // Get total amount to compare
            if (totalAmount != null && !totalAmount.isEmpty()) {
                totalToCompare = totalAmount;
            } else {
                String fallbackTotal = payment.getTotalAmount();
                if (fallbackTotal != null && !fallbackTotal.isEmpty() && !fallbackTotal.equals("₱0.00")) {
                    totalToCompare = fallbackTotal.replace("₱", "").replace(",", "");
                }
            }
            
            // Compare if both are available
            if (paidToCompare != null && totalToCompare != null) {
                try {
                    double paidValue = Double.parseDouble(paidToCompare);
                    double totalValue = Double.parseDouble(totalToCompare);
                    // If they're equal or very close, it's likely a single period payment
                    if (paidValue > 0 && Math.abs(paidValue - totalValue) < 0.01) {
                        holder.tvTotalAmount.setText("₱" + formatAmount(paidToCompare));
                        useBookingTotal = true;
                    }
                } catch (NumberFormatException e) {
                    // Will fall through to use total amount
                }
            }
        }
        
        // If not handled above, use total amount from booking breakdown
        if (!useBookingTotal && totalAmount != null && !totalAmount.isEmpty()) {
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
        
        // Always hide the rectangle status badge (tvPaymentStatus) - we only show oblong badge (tvStatusBadge)
        if (holder.tvPaymentStatus != null) {
            holder.tvPaymentStatus.setVisibility(View.GONE);
        }
        
        holder.tvRentalStatus.setText(payment.getRentalStatus());
        String paymentDate = payment.getPaymentDate();
        if (paymentDate != null && !paymentDate.isEmpty()) {
            holder.tvPaymentDate.setText(formatDateTime(paymentDate));
        } else {
            holder.tvPaymentDate.setText("N/A");
        }
        
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
        
        // Show status badge at top right based on payment status
        // Fully Paid = all periods paid OR isFullyPaid flag is true
        // Partially Paid = some periods paid but not all
        // Pending = no periods paid or status is "Pending"
        if (holder.tvStatusBadge != null) {
            String paymentStatus = payment.getPaymentStatus();
            String statusLower = paymentStatus != null ? paymentStatus.toLowerCase() : "";
            
            // Check if fully paid (from flag or period data or status string)
            boolean showFullyPaid = isFullyPaid || 
                                   (totalPeriods > 0 && paidPeriods >= totalPeriods) ||
                                   "fully paid".equals(statusLower);
            
            // Check if partially paid (some periods paid but not all)
            boolean showCompletedPartially = (totalPeriods > 0 && paidPeriods > 0 && paidPeriods < totalPeriods) ||
                                           "partially paid".equals(statusLower) ||
                                           "partially_paid".equals(statusLower) ||
                                           (statusLower.contains("partially") && statusLower.contains("paid")) ||
                                           // Legacy support for old "Completed/Partially" status
                                           "completed".equals(statusLower) || 
                                           "completed/partially".equals(statusLower) ||
                                           "completed_partially".equals(statusLower) ||
                                           (statusLower.contains("completed") && !showFullyPaid);
            
            // Check if pending (same logic as Pending tab)
            boolean showPending = "pending".equals(statusLower) ||
                                (totalPeriods > 0 && paidPeriods == 0) ||
                                (totalPeriods == 0 && !showFullyPaid && !showCompletedPartially);
            
            // Show badges for Fully Paid, Partially Paid, and Pending in All Payments tab
            // Same badges as shown in their respective tabs
            if (showFullyPaid) {
                // Green badge for Fully Paid
                holder.tvStatusBadge.setText("FULLY PAID");
                holder.tvStatusBadge.setVisibility(View.VISIBLE);
                holder.tvStatusBadge.setTextColor(Color.parseColor("#FFFFFF"));
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_approved);
            } else if (showCompletedPartially) {
                // Blue badge for Partially Paid
                holder.tvStatusBadge.setText("PARTIALLY PAID");
                holder.tvStatusBadge.setVisibility(View.VISIBLE);
                holder.tvStatusBadge.setTextColor(Color.parseColor("#FFFFFF"));
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_completed);
            } else if (showPending) {
                // Orange badge for Pending (same as Pending tab)
                holder.tvStatusBadge.setText("PENDING");
                holder.tvStatusBadge.setVisibility(View.VISIBLE);
                holder.tvStatusBadge.setTextColor(Color.parseColor("#FFFFFF"));
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pending);
            } else {
                holder.tvStatusBadge.setVisibility(View.GONE);
            }
        }
        
        
        // Set payment progress for all payments view
        // Always show progress if we have period data OR if payment is fully paid
        int totalMonths = payment.getTotalMonthsForBooking();
        int paidMonths = payment.getPaidMonthsForBooking();
        double progressPercent = payment.getPaymentProgressPercent();
        
        // Calculate progress percent if not set but we have period data
        if (progressPercent == 0 && totalPeriods > 0) {
            progressPercent = (paidPeriods * 100.0) / totalPeriods;
        }
        
        // For fully paid payments without period data, show 100%
        boolean hasNoPeriodData = totalPeriods == 0;
        if (isFullyPaid && hasNoPeriodData) {
            progressPercent = 100.0;
            totalPeriods = 1; // Set to 1 so progress bar shows
            paidPeriods = 1;
        }
        
        // Show progress bar if we have period data OR if payment is fully paid
        if ((totalPeriods > 0 || isFullyPaid) && holder.progressBarPayment != null) {
            // Show "months" only if all periods are exactly monthly (total_periods == total_months)
            // Otherwise show "periods" (use singular "period" if only 1)
            if (totalMonths > 0 && totalPeriods == totalMonths && !hasNoPeriodData) {
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
                } else if (totalPeriods > 1) {
                    holder.tvPaymentProgress.setText(
                        String.format("%d/%d periods paid", paidPeriods, totalPeriods)
                    );
                } else if (isFullyPaid && hasNoPeriodData) {
                    // Fully paid but no period data - show as fully paid
                    holder.tvPaymentProgress.setText("Fully Paid");
                } else {
                    holder.tvPaymentProgress.setText("No period data");
                }
            }
            
            // Set progress bar
            holder.progressBarPayment.setProgress((int) progressPercent);
            holder.tvProgressPercent.setText(String.format("%.0f%%", progressPercent));
            
            // Check if partially paid (same logic as status badge)
            String paymentStatus = payment.getPaymentStatus();
            String statusLower = paymentStatus != null ? paymentStatus.toLowerCase() : "";
            boolean isPartiallyPaid = (totalPeriods > 0 && paidPeriods > 0 && paidPeriods < totalPeriods) ||
                                     "partially paid".equals(statusLower) ||
                                     "partially_paid".equals(statusLower) ||
                                     (statusLower.contains("partially") && statusLower.contains("paid")) ||
                                     // Legacy support for old "Completed/Partially" status
                                     "completed/partially".equals(statusLower) ||
                                     "completed_partially".equals(statusLower) ||
                                     (statusLower.contains("completed") && !isFullyPaid);
            
            // Set progress bar color based on payment status
            if (isFullyPaid) {
                // Green for Fully Paid
                holder.progressBarPayment.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                );
                holder.tvPaymentProgress.setTextColor(Color.parseColor("#4CAF50"));
            } else if (isPartiallyPaid) {
                // Blue for Partially Paid
                holder.progressBarPayment.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#2196F3"))
                );
                holder.tvPaymentProgress.setTextColor(Color.parseColor("#2196F3"));
            } else {
                // Orange for pending/low progress (changed from red to orange)
                holder.progressBarPayment.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#FF9800"))
                );
                holder.tvPaymentProgress.setTextColor(Color.parseColor("#FF9800"));
            }
            
            // Show progress section
            holder.tvPaymentProgress.setVisibility(View.VISIBLE);
            holder.progressBarPayment.setVisibility(View.VISIBLE);
            holder.tvProgressPercent.setVisibility(View.VISIBLE);
        } else if (holder.progressBarPayment != null) {
            // If no period data, try to show progress based on payment status
            // For payments without breakdown, still try to show something
            if (paidPeriods > 0 || totalPeriods > 0) {
                // We have some period data, show it
                holder.tvPaymentProgress.setVisibility(View.VISIBLE);
                holder.progressBarPayment.setVisibility(View.VISIBLE);
                holder.tvProgressPercent.setVisibility(View.VISIBLE);
                
                // Set default progress
                if (totalPeriods > 0) {
                    int defaultProgress = (int) ((paidPeriods * 100.0) / totalPeriods);
                    holder.progressBarPayment.setProgress(defaultProgress);
                    holder.tvProgressPercent.setText(defaultProgress + "%");
                    holder.tvPaymentProgress.setText(String.format("%d/%d periods paid", paidPeriods, totalPeriods));
                }
            } else {
                // Hide progress if no data at all
                holder.tvPaymentProgress.setVisibility(View.GONE);
                holder.progressBarPayment.setVisibility(View.GONE);
                holder.tvProgressPercent.setVisibility(View.GONE);
            }
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
    
    private String formatDateTime(String dateTime) {
        // Format from "YYYY-MM-DD HH:MM:SS" to "MMM DD, YYYY hh:mm a" (e.g., "Jan 15, 2025 02:00 PM")
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

































