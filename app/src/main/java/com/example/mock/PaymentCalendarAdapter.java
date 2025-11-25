package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class PaymentCalendarAdapter extends RecyclerView.Adapter<PaymentCalendarAdapter.ViewHolder> {

    private List<CalendarPaymentData.PaymentItem> payments;

    public PaymentCalendarAdapter(List<CalendarPaymentData.PaymentItem> payments) {
        this.payments = payments != null ? payments : new java.util.ArrayList<>();
    }
    
    public void updatePayments(List<CalendarPaymentData.PaymentItem> newPayments) {
        this.payments = newPayments != null ? newPayments : new java.util.ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment_calendar, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CalendarPaymentData.PaymentItem payment = payments.get(position);
        
        holder.tvBoarderName.setText(payment.getBoarderName());
        holder.tvRoomNumber.setText("Room: " + payment.getRoomNumber());
        holder.tvBhName.setText(payment.getBhName());
        holder.tvAmount.setText("₱" + String.format(Locale.getDefault(), "%.2f", payment.getAmount()));
        
        // Show "Overdue" status if payment is overdue
        String status = payment.getStatus();
        if (status != null && status.equals("Overdue")) {
            holder.tvStatus.setVisibility(View.VISIBLE);
            holder.tvStatus.setText("Overdue");
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#F44336")); // Red
        } else {
            holder.tvStatus.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return payments != null ? payments.size() : 0;
    }

    private int getStatusColor(String status) {
        switch (status) {
            case "Pending":
                return android.graphics.Color.parseColor("#FF9800"); // Orange
            case "Partially Paid":
                return android.graphics.Color.parseColor("#2196F3"); // Blue
            case "Overdue":
                return android.graphics.Color.parseColor("#F44336"); // Red
            case "Paid":
                return android.graphics.Color.parseColor("#4CAF50"); // Green
            default:
                return android.graphics.Color.parseColor("#666666"); // Gray
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBoarderName, tvRoomNumber, tvBhName, tvAmount, tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBoarderName = itemView.findViewById(R.id.tvBoarderName);
            tvRoomNumber = itemView.findViewById(R.id.tvRoomNumber);
            tvBhName = itemView.findViewById(R.id.tvBhName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}

