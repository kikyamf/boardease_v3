package com.example.mock;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.ViewHolder> {
    
    public static final int VIEW_TYPE_ALL = 0;
    public static final int VIEW_TYPE_FULLY_PAID = 1;
    public static final int VIEW_TYPE_REMAINING = 2;
    public static final int VIEW_TYPE_PENDING = 3;
    public static final int VIEW_TYPE_OVERDUE = 4;
    
    private List<PaymentData> payments;
    private Context context;
    private PaymentActionListener actionListener;
    private int viewType = VIEW_TYPE_ALL; // Default to all payments view
    
    public interface PaymentActionListener {
        void onMarkAsPaid(PaymentData payment);
        void onMarkAsOverdue(PaymentData payment);
        void onViewDetails(PaymentData payment);
        void onSendReminder(PaymentData payment);
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
            case VIEW_TYPE_OVERDUE:
                view = LayoutInflater.from(context)
                        .inflate(R.layout.item_payment, parent, false);
                return new ViewHolder(view, VIEW_TYPE_OVERDUE);
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
            case VIEW_TYPE_OVERDUE:
                bindOverdueViewHolder(holder, payment);
                break;
            case VIEW_TYPE_ALL:
            default:
                bindAllPaymentsViewHolder(holder, payment);
                break;
        }
        
        // Set click listener - for overdue, show reminder modal instead of payment details
        if (actionListener != null) {
            if (holder.viewType == VIEW_TYPE_OVERDUE) {
                holder.itemView.setOnClickListener(v -> {
                    actionListener.onSendReminder(payment);
                });
            } else {
                holder.itemView.setOnClickListener(v -> actionListener.onViewDetails(payment));
            }
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
        
        // Apply Green Palette for Fully Paid - white progress section
        holder.applyPalette("#F1F8E9", "#4CAF50", "#2E7D32", "#4CAF50", "#757575", "✓", "#FFFFFF");

        // Hide elements not used in fully paid view
        if (holder.tvAmountPaid != null && holder.tvAmountPaidLabel != null) {
            holder.tvAmountPaid.setVisibility(View.GONE);
            holder.tvAmountPaidLabel.setVisibility(View.GONE);
        }
        if (holder.tvRentalStatus != null && holder.tvRentalStatusLabel != null) {
            holder.tvRentalStatus.setVisibility(View.GONE);
            holder.tvRentalStatusLabel.setVisibility(View.GONE);
        }
        if (holder.getLayoutPaymentProgress() != null) {
            holder.getLayoutPaymentProgress().setVisibility(View.GONE);
        }
    }
    
    private void bindRemainingViewHolder(ViewHolder holder, PaymentData payment) {
        // Restore declarations for progress bar and calculation logic
        int totalPeriods = payment.getTotalPeriods();
        int paidPeriods = payment.getPaidPeriods();
        boolean isFullyPaid = payment.isFullyPaid();
        
        // Set amount paid and total amount using overall totals from backend
        if (holder.tvAmountPaid != null) {
            holder.tvAmountPaid.setText(payment.getAmountPaid());
        }
        if (holder.tvTotalAmount != null) {
            holder.tvTotalAmount.setText(payment.getTotalAmount());
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
        
        // Change status badge from "REMAINING" to "PARTIALLY PAID"
        holder.tvPaymentStatus.setText("PARTIALLY PAID");
        
        // Apply Blue Palette for Remaining / Partially Paid - white progress section
        holder.applyPalette("#E3F2FD", "#2196F3", "#1565C0", "#2196F3", "#757575", "◷", "#FFFFFF");
        
        // Ensure remaining amount is highlighted in red as requested
        if (holder.tvRemainingAmount != null) {
            holder.tvRemainingAmount.setTextColor(Color.RED);
        }
        if (holder.tvRemainingAmountLabel != null) {
            holder.tvRemainingAmountLabel.setTextColor(Color.RED);
        }
        
        // Set payment progress
        totalPeriods = payment.getTotalPeriods();
        paidPeriods = payment.getPaidPeriods();
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
    
    private void bindOverdueViewHolder(ViewHolder holder, PaymentData payment) {
        // For overdue payments: show due amount (what needs to be paid on due date), due date, and days overdue
        
        // Change label from "Amount Paid" to "Amount to be Paid" since nothing has been paid yet
        if (holder.tvAmountPaidLabel != null) {
            holder.tvAmountPaidLabel.setText("Amount to be Paid");
        }
        
        // Show DUE AMOUNT (what needs to be paid on due date) - this is the original amount due, not remaining
        // Example: If 5000 was due, show 5000 (even if they paid some, show the original due amount)
        if (holder.tvAmountPaid != null) {
            String dueAmount = null;
            
            // First try: use totalAmount (this is the payment amount that was due for this period)
            String totalAmount = payment.getTotalAmount();
            if (totalAmount != null && !totalAmount.isEmpty()) {
                // Remove currency symbol and format
                String cleanAmount = totalAmount.replace("₱", "").replace(",", "").trim();
                if (!cleanAmount.isEmpty() && !cleanAmount.equals("0") && !cleanAmount.equals("0.00")) {
                    try {
                        double amount = Double.parseDouble(cleanAmount);
                        if (amount > 0) {
                            dueAmount = cleanAmount;
                        }
                    } catch (NumberFormatException e) {
                        // Invalid number
                    }
                }
            }
            
            // Fallback: use totalAmountForBooking if totalAmount is not available
            if (dueAmount == null || dueAmount.isEmpty() || dueAmount.equals("0") || dueAmount.equals("0.00")) {
                String bookingTotal = payment.getTotalAmountForBooking();
                if (bookingTotal != null && !bookingTotal.isEmpty()) {
                    try {
                        double amount = Double.parseDouble(bookingTotal);
                        if (amount > 0) {
                            dueAmount = bookingTotal;
                        }
                    } catch (NumberFormatException e) {
                        // Invalid number
                    }
                }
            }
            
            // Final fallback
            if (dueAmount == null || dueAmount.isEmpty() || dueAmount.equals("0") || dueAmount.equals("0.00")) {
                dueAmount = "0.00";
            }
            
            holder.tvAmountPaid.setText("₱" + formatAmount(dueAmount));
        }
        
        // Show total amount (pila tanan ang bayran all in all - total amount for entire booking)
        // This should be the total amount for all periods in the booking, regardless of payment status
        if (holder.tvTotalAmount != null) {
            holder.tvTotalAmount.setVisibility(View.VISIBLE);
            // Use totalAmountForBooking (total for all periods in the booking, maski naka paid pa)
            String bookingTotal = payment.getTotalAmountForBooking();
            if (bookingTotal != null && !bookingTotal.isEmpty()) {
                try {
                    double amount = Double.parseDouble(bookingTotal);
                    if (amount > 0) {
                        holder.tvTotalAmount.setText("₱" + formatAmount(bookingTotal));
                    } else {
                        // Fallback to regular totalAmount
                        String totalAmount = payment.getTotalAmount();
                        if (totalAmount != null && !totalAmount.isEmpty() && !totalAmount.equals("₱0.00")) {
                            holder.tvTotalAmount.setText(totalAmount);
                        } else {
                            holder.tvTotalAmount.setText("₱0.00");
                        }
                    }
                } catch (NumberFormatException e) {
                    // Fallback to regular totalAmount
                    String totalAmount = payment.getTotalAmount();
                    if (totalAmount != null && !totalAmount.isEmpty() && !totalAmount.equals("₱0.00")) {
                        holder.tvTotalAmount.setText(totalAmount);
                    } else {
                        holder.tvTotalAmount.setText("₱0.00");
                    }
                }
            } else {
                // Fallback to regular totalAmount
                String totalAmount = payment.getTotalAmount();
                if (totalAmount != null && !totalAmount.isEmpty() && !totalAmount.equals("₱0.00")) {
                    holder.tvTotalAmount.setText(totalAmount);
                } else {
                    holder.tvTotalAmount.setText("₱0.00");
                }
            }
        }
        
        // Hide remaining amount
        if (holder.tvRemainingAmount != null) {
            holder.tvRemainingAmount.setVisibility(View.GONE);
        }
        
        // Set DUE DATE and DAYS OVERDUE (use the overdue payment's due date, not next due date)
        // The due_date should be from the payment breakdown that is overdue
        if (holder.tvPaymentDate != null) {
            String dueDateStr = payment.getDueDate();
            // Make sure we're using the overdue payment's due date, not the next due date
            // If due_date is empty or seems to be a future date, we should still use it if it's the overdue one
            if (dueDateStr == null || dueDateStr.isEmpty()) {
                // Fallback to payment_date if due_date not available (but this shouldn't happen for overdue)
                dueDateStr = payment.getPaymentDate();
            }
            
            if (dueDateStr != null && !dueDateStr.isEmpty()) {
                try {
                    // Parse due date
                    java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String datePart = dueDateStr.split(" ")[0]; // Take only date part
                    java.util.Date dueDate = inputFormat.parse(datePart);
                    
                    // Calculate days overdue
                    java.util.Date today = new java.util.Date();
                    long diffInMillis = today.getTime() - dueDate.getTime();
                    long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);
                    
                    // Format due date for display
                    java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    String formattedDueDate = outputFormat.format(dueDate);
                    
                    if (diffInDays > 0) {
                        holder.tvPaymentDate.setText("Due: " + formattedDueDate + " (" + diffInDays + " day" + (diffInDays > 1 ? "s" : "") + " overdue)");
                    } else {
                        holder.tvPaymentDate.setText("Due: " + formattedDueDate);
                    }
                } catch (Exception e) {
                    // If parsing fails, just show the date as is
                    holder.tvPaymentDate.setText("Due: " + dueDateStr);
                }
            } else {
                holder.tvPaymentDate.setText("Due: N/A");
            }
        }
        
        // Apply Red Palette for Overdue - Softer background, neutral gray labels, white progress section
        holder.applyPalette("#FFFFFF", "#F44336", "#B71C1C", "#F44336", "#757575", "!", "#FFEBEE");
        
        // Ensure status badge says OVERDUE
        if (holder.tvPaymentStatus != null) {
            holder.tvPaymentStatus.setText("OVERDUE");
            holder.tvPaymentStatus.setVisibility(View.VISIBLE);
        }
        
        // Set rental status (if view exists)
        if (holder.tvRentalStatus != null) {
            holder.tvRentalStatus.setText(payment.getRentalStatus());
        }
        
        // Hide progress container for overdue
        if (holder.getLayoutPaymentProgress() != null) {
            holder.getLayoutPaymentProgress().setVisibility(View.GONE);
        }
    }
    
    private void bindPendingViewHolder(ViewHolder holder, PaymentData payment) {
        // Restore declarations for progress bar logic
        int totalPeriods = payment.getTotalPeriods();
        int paidPeriods = payment.getPaidPeriods();
        boolean isFullyPaid = payment.isFullyPaid();
        
        // Similar to bindAllPaymentsViewHolder but with orange border and PENDING badge only
        
        // Use accurate amounts from backend (overall totals)
        holder.tvAmountPaid.setText(payment.getAmountPaid());
        holder.tvTotalAmount.setText(payment.getTotalAmount());
        
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
        
        // Apply Orange Palette for Pending - Softer background, orange-tinted progress section
        holder.applyPalette("#FFFFFF", "#FF9800", "#E65100", "#FF9800", "#757575", "●", "#FFF3E0");
        
        if (holder.tvPaymentStatus != null) {
            holder.tvPaymentStatus.setText("PENDING");
            holder.tvPaymentStatus.setVisibility(View.VISIBLE);
        }
        
        // Set rental status
        if (holder.tvRentalStatus != null) {
            holder.tvRentalStatus.setText(payment.getRentalStatus());
        }

        // Set payment progress
        int totalMonths = payment.getTotalMonthsForBooking();
        int paidMonths = payment.getPaidMonthsForBooking();
        double progressPercent = payment.getPaymentProgressPercent();
        
        if (progressPercent == 0 && totalPeriods > 0) {
            progressPercent = (paidPeriods * 100.0) / totalPeriods;
        }
        
        if ((totalPeriods > 0 || isFullyPaid) && holder.getLayoutPaymentProgress() != null) {
            holder.getLayoutPaymentProgress().setVisibility(View.VISIBLE);
            
            if (totalMonths > 0 && totalPeriods == totalMonths) {
                holder.tvPaymentProgress.setText(String.format("%d/%d month%s paid", paidMonths, totalMonths, totalMonths == 1 ? "" : "s"));
            } else {
                holder.tvPaymentProgress.setText(String.format("%d/%d period%s paid", paidPeriods, totalPeriods, totalPeriods == 1 ? "" : "s"));
            }
            
            if (holder.progressBarPayment != null) {
                holder.progressBarPayment.setProgress((int) progressPercent);
            }
            if (holder.tvProgressPercent != null) {
                holder.tvProgressPercent.setText(String.format("%.0f%%", progressPercent));
            }
        } else if (holder.getLayoutPaymentProgress() != null) {
            holder.getLayoutPaymentProgress().setVisibility(View.GONE);
        }
    }
    
    private void bindAllPaymentsViewHolder(ViewHolder holder, PaymentData payment) {
        // Restore declarations for progress bar logic
        int totalPeriods = payment.getTotalPeriods();
        int paidPeriods = payment.getPaidPeriods();
        boolean isFullyPaid = payment.isFullyPaid();
        
        // Use accurate amounts from backend (overall totals)
        holder.tvAmountPaid.setText(payment.getAmountPaid());
        holder.tvTotalAmount.setText(payment.getTotalAmount());
        
        // Set dynamic palette based on status
        String paymentStatus = payment.getPaymentStatus();
        String statusLower = paymentStatus != null ? paymentStatus.toLowerCase() : "";
        
        // Determine exact status for palette
        boolean isFullyPaidStatus = isFullyPaid || (totalPeriods > 0 && paidPeriods >= totalPeriods) || "fully paid".equals(statusLower);
        boolean isOverdueStatus = "overdue".equals(statusLower);
        boolean isPartiallyPaidStatus = !isFullyPaidStatus && !isOverdueStatus && ((totalPeriods > 0 && paidPeriods > 0) || statusLower.contains("partially"));
        boolean isPendingStatus = !isFullyPaidStatus && !isOverdueStatus && !isPartiallyPaidStatus;

        if (isFullyPaidStatus) {
            holder.applyPalette("#F1F8E9", "#4CAF50", "#2E7D32", "#4CAF50", "#757575", "✓", "#FFFFFF");
            holder.tvPaymentStatus.setText("FULLY PAID");
        } else if (isOverdueStatus) {
            holder.applyPalette("#FFFFFF", "#F44336", "#B71C1C", "#F44336", "#757575", "!", "#FFEBEE");
            holder.tvPaymentStatus.setText("OVERDUE");
        } else if (isPartiallyPaidStatus) {
            holder.applyPalette("#E3F2FD", "#2196F3", "#1565C0", "#2196F3", "#757575", "◷", "#FFFFFF");
            holder.tvPaymentStatus.setText("PARTIALLY PAID");
            
            // Highlight remaining in red
            if (holder.tvRemainingAmount != null) holder.tvRemainingAmount.setTextColor(Color.RED);
            if (holder.tvRemainingAmountLabel != null) holder.tvRemainingAmountLabel.setTextColor(Color.RED);
        } else {
            // Default Pending
            holder.applyPalette("#FFFFFF", "#FF9800", "#E65100", "#FF9800", "#757575", "●", "#FFF3E0");
            holder.tvPaymentStatus.setText("PENDING");
        }
        
        if (holder.tvPaymentStatus != null) holder.tvPaymentStatus.setVisibility(View.VISIBLE);

        // Set payment progress
        int totalMonths = payment.getTotalMonthsForBooking();
        int paidMonths = payment.getPaidMonthsForBooking();
        double progressPercent = payment.getPaymentProgressPercent();
        
        if (progressPercent == 0 && totalPeriods > 0) {
            progressPercent = (paidPeriods * 100.0) / totalPeriods;
        }
        
        if (isFullyPaid && totalPeriods == 0) {
            progressPercent = 100.0;
        }
        
        if ((totalPeriods > 0 || isFullyPaid) && holder.getLayoutPaymentProgress() != null) {
            holder.getLayoutPaymentProgress().setVisibility(View.VISIBLE);
            
            if (totalMonths > 0 && totalPeriods == totalMonths && totalPeriods > 0) {
                holder.tvPaymentProgress.setText(String.format("%d/%d month%s paid", paidMonths, totalMonths, totalMonths == 1 ? "" : "s"));
            } else if (totalPeriods > 0) {
                holder.tvPaymentProgress.setText(String.format("%d/%d period%s paid", paidPeriods, totalPeriods, totalPeriods == 1 ? "" : "s"));
            } else if (isFullyPaid) {
                holder.tvPaymentProgress.setText("Fully Paid");
            }
            
            if (holder.progressBarPayment != null) {
                holder.progressBarPayment.setProgress((int) progressPercent);
            }
            if (holder.tvProgressPercent != null) {
                holder.tvProgressPercent.setText(String.format("%.0f%%", progressPercent));
            }
        } else if (holder.getLayoutPaymentProgress() != null) {
            holder.getLayoutPaymentProgress().setVisibility(View.GONE);
        }
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
        // Format from "YYYY-MM-DD HH:MM:SS" (UTC) to "MMM DD, YYYY hh:mm a" (Local Time)
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            inputFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC")); // Server sends UTC
            
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault());
            outputFormat.setTimeZone(java.util.TimeZone.getDefault()); // Display in Local Time
            
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
        com.google.android.material.card.MaterialCardView cardView;
        LinearLayout layoutStatusBadge, layoutPaymentProgress;
        TextView tvStatusIcon;
        TextView tvBoarderName, tvRoom, tvRentType, tvPaymentStatus;
        TextView tvAmountPaid, tvTotalAmount, tvRentalStatus, tvPaymentDate;
        TextView tvPaymentProgress, tvProgressPercent, tvRemainingAmount;
        android.widget.ProgressBar progressBarPayment;
        TextView tvRoomLabel, tvRentTypeLabel, tvAmountPaidLabel, tvTotalAmountLabel;
        TextView tvProgressLabel, tvRentalStatusLabel, tvPaymentDateLabel, tvRemainingAmountLabel;
        com.google.android.material.button.MaterialButton btnViewDetails;
        int viewType;
        
        public ViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            this.viewType = viewType;
            
            // Common views
            cardView = itemView.findViewById(R.id.cardView);
            layoutStatusBadge = itemView.findViewById(R.id.layoutStatusBadge);
            tvStatusIcon = itemView.findViewById(R.id.tvStatusIcon);
            tvBoarderName = itemView.findViewById(R.id.tvBoarderName);
            tvRoom = itemView.findViewById(R.id.tvRoom);
            tvRentType = itemView.findViewById(R.id.tvRentType);
            tvPaymentStatus = itemView.findViewById(R.id.tvPaymentStatus);
            layoutPaymentProgress = itemView.findViewById(R.id.layoutPaymentProgress);
            
            // Views that may not exist in all layouts
            tvAmountPaid = itemView.findViewById(R.id.tvAmountPaid);
            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
            tvRentalStatus = itemView.findViewById(R.id.tvRentalStatus);
            tvPaymentDate = itemView.findViewById(R.id.tvPaymentDate);
            tvPaymentProgress = itemView.findViewById(R.id.tvPaymentProgress);
            tvProgressPercent = itemView.findViewById(R.id.tvProgressPercent);
            progressBarPayment = itemView.findViewById(R.id.progressBarPayment);
            tvRemainingAmount = itemView.findViewById(R.id.tvRemainingAmount);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);

            // Labels for dynamic coloring
            tvRoomLabel = itemView.findViewById(R.id.tvRoomLabel);
            tvRentTypeLabel = itemView.findViewById(R.id.tvRentTypeLabel);
            tvAmountPaidLabel = itemView.findViewById(R.id.tvAmountPaidLabel);
            tvTotalAmountLabel = itemView.findViewById(R.id.tvTotalAmountLabel);
            tvProgressLabel = itemView.findViewById(R.id.tvProgressLabel);
            tvRentalStatusLabel = itemView.findViewById(R.id.tvRentalStatusLabel);
            tvPaymentDateLabel = itemView.findViewById(R.id.tvPaymentDateLabel);
            tvRemainingAmountLabel = itemView.findViewById(R.id.tvRemainingAmountLabel);
        }
        
        public LinearLayout getLayoutPaymentProgress() {
            return layoutPaymentProgress;
        }
        
        // Helper to apply color palette
        // progressBgColor: background of the progress section rectangle
        public void applyPalette(String bgColor, String strokeColor, String headerTextColor, String badgeBgColor, String subLabelColor, String icon, String progressBgColor) {
            if (cardView != null) {
                cardView.setCardBackgroundColor(Color.parseColor(bgColor));
                cardView.setStrokeColor(Color.parseColor(strokeColor));
            }
            if (layoutStatusBadge != null) {
                layoutStatusBadge.setBackgroundColor(Color.parseColor(badgeBgColor));
            }
            if (layoutPaymentProgress != null) {
                layoutPaymentProgress.setBackgroundColor(Color.parseColor(progressBgColor));
            }
            if (tvStatusIcon != null) {
                tvStatusIcon.setText(icon);
            }
            if (tvBoarderName != null) tvBoarderName.setTextColor(Color.parseColor(headerTextColor));
            if (tvPaymentStatus != null) tvPaymentStatus.setTextColor(Color.WHITE);
            
            // Set sub-labels and secondary text
            int subColor = Color.parseColor(subLabelColor);
            if (tvRoomLabel != null) tvRoomLabel.setTextColor(subColor);
            if (tvRentTypeLabel != null) tvRentTypeLabel.setTextColor(subColor);
            if (tvAmountPaidLabel != null) tvAmountPaidLabel.setTextColor(subColor);
            if (tvTotalAmountLabel != null) tvTotalAmountLabel.setTextColor(subColor);
            if (tvProgressLabel != null) tvProgressLabel.setTextColor(subColor);
            if (tvRentalStatusLabel != null) tvRentalStatusLabel.setTextColor(subColor);
            if (tvPaymentDateLabel != null) tvPaymentDateLabel.setTextColor(subColor);
            if (tvRemainingAmountLabel != null) tvRemainingAmountLabel.setTextColor(subColor);
            
            // Also update some values to be dark version of the color for better readability
            int darkColor = Color.parseColor(headerTextColor);
            if (tvRoom != null) tvRoom.setTextColor(darkColor);
            if (tvRentType != null) tvRentType.setTextColor(darkColor);
            if (tvTotalAmount != null) tvTotalAmount.setTextColor(darkColor);
            if (tvRentalStatus != null) tvRentalStatus.setTextColor(darkColor);
            if (tvPaymentDate != null) tvPaymentDate.setTextColor(darkColor);
            if (tvRemainingAmount != null) tvRemainingAmount.setTextColor(darkColor);
            if (tvProgressPercent != null) tvProgressPercent.setTextColor(subColor);
            // Payment progress text (e.g. "1/1 period paid") uses the status color
            if (tvPaymentProgress != null) tvPaymentProgress.setTextColor(Color.parseColor(strokeColor));
            
            // Progress bar tint
            if (progressBarPayment != null) {
                progressBarPayment.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor(strokeColor)));
            }
        }
        
        // Overload for backward-compatible calls without progressBgColor
        public void applyPalette(String bgColor, String strokeColor, String headerTextColor, String badgeBgColor, String subLabelColor, String icon) {
            applyPalette(bgColor, strokeColor, headerTextColor, badgeBgColor, subLabelColor, icon, "#F8F9FA");
        }
    }
}

































