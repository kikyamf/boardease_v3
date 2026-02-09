package com.example.mock;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PaymentBreakdownAdapter extends RecyclerView.Adapter<PaymentBreakdownAdapter.ViewHolder> {
    
    private List<PaymentBreakdownItem> breakdownItems;
    private OnPaymentItemClickListener listener;
    
    public interface OnPaymentItemClickListener {
        void onPaymentItemClick(PaymentBreakdownItem item);
    }
    
    public PaymentBreakdownAdapter(List<PaymentBreakdownItem> breakdownItems) {
        this.breakdownItems = breakdownItems;
    }
    
    public void setOnPaymentItemClickListener(OnPaymentItemClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment_breakdown, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PaymentBreakdownItem item = breakdownItems.get(position);
        
        holder.tvPeriodLabel.setText(item.getPeriodLabel());
        holder.tvPeriodStartDate.setText(formatDate(item.getPeriodStartDate()));
        holder.tvPeriodEndDate.setText(formatDate(item.getPeriodEndDate()));
        holder.tvPeriodAmount.setText("₱" + item.getAmount());
        
        // Hide elements not used by this adapter
        holder.checkboxPeriod.setVisibility(View.GONE);
        holder.tvPeriodDates.setVisibility(View.GONE);
        holder.tvAmount.setVisibility(View.GONE);
        holder.tvDueDate.setVisibility(View.GONE);
        holder.tvStatus.setVisibility(View.GONE);
        
        // Show elements used by this adapter
        holder.tvPeriodStartDate.setVisibility(View.VISIBLE);
        holder.tvPeriodEndDate.setVisibility(View.VISIBLE);
        holder.tvPeriodAmount.setVisibility(View.VISIBLE);
        holder.tvPeriodStatus.setVisibility(View.VISIBLE);
        
        // Set status - only show PAID if actually paid (is_paid = 1)
        // Otherwise show UNPAID (not PENDING, since PENDING is for payment status, not breakdown status)
        if (item.isPaid()) {
            holder.tvPeriodStatus.setText("PAID");
            holder.tvPeriodStatus.setTextColor(Color.parseColor("#4CAF50"));
            holder.tvPeriodStatus.setBackgroundColor(Color.parseColor("#E8F5E8"));
        } else {
            holder.tvPeriodStatus.setText("UNPAID");
            holder.tvPeriodStatus.setTextColor(Color.parseColor("#F44336"));
            holder.tvPeriodStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPaymentItemClick(item);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return breakdownItems.size();
    }
    
    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "";
        }
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (ParseException e) {
            // If parsing fails, return original string
        }
        return dateString;
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPeriodLabel, tvPeriodStatus, tvPeriodStartDate, tvPeriodEndDate, tvPeriodAmount;
        TextView tvPeriodDates, tvAmount, tvDueDate, tvStatus;
        android.widget.CheckBox checkboxPeriod;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPeriodLabel = itemView.findViewById(R.id.tvPeriodLabel);
            tvPeriodStatus = itemView.findViewById(R.id.tvPeriodStatus);
            tvPeriodStartDate = itemView.findViewById(R.id.tvPeriodStartDate);
            tvPeriodEndDate = itemView.findViewById(R.id.tvPeriodEndDate);
            tvPeriodAmount = itemView.findViewById(R.id.tvPeriodAmount);
            checkboxPeriod = itemView.findViewById(R.id.checkboxPeriod);
            tvPeriodDates = itemView.findViewById(R.id.tvPeriodDates);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDueDate = itemView.findViewById(R.id.tvDueDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}

