package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PaymentsAdapter extends RecyclerView.Adapter<PaymentsAdapter.ViewHolder> {

    private List<PaymentsFragment.PaymentLog> paymentLogs;
    private OnPaymentClickListener clickListener;

    public interface OnPaymentClickListener {
        void onPaymentClick(PaymentsFragment.PaymentLog payment);
    }

    public PaymentsAdapter(List<PaymentsFragment.PaymentLog> paymentLogs, OnPaymentClickListener clickListener) {
        this.paymentLogs = paymentLogs;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PaymentsFragment.PaymentLog payment = paymentLogs.get(position);
        
        holder.tvTenantName.setText(payment.getTenantName());
        holder.tvPropertyName.setText(payment.getPropertyName());
        holder.tvAmount.setText(payment.getAmount());
        holder.tvTimestamp.setText(payment.getTimestamp());
        holder.tvDescription.setText(payment.getDescription());
        
        // Set status with appropriate color
        holder.tvStatus.setText(payment.getStatus());
        switch (payment.getStatus().toLowerCase()) {
            case "paid":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_approved);
                break;
            case "pending":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                break;
            case "failed":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_declined);
                break;
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onPaymentClick(payment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return paymentLogs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTenantName, tvPropertyName, tvAmount, tvTimestamp, tvStatus, tvDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTenantName = itemView.findViewById(R.id.tvTenantName);
            tvPropertyName = itemView.findViewById(R.id.tvPropertyName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }
    }
}

































