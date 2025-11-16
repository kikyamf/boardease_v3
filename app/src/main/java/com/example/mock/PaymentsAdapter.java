package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

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
        
        // Load boarding house image
        String bhImageUrl = payment.getBhImageUrl();
        if (bhImageUrl != null && !bhImageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(bhImageUrl)
                    .placeholder(R.drawable.ic_home)
                    .error(R.drawable.ic_home)
                    .into(holder.imgBoardingHouse);
        } else {
            holder.imgBoardingHouse.setImageResource(R.drawable.ic_home);
        }
        
        // Set status with appropriate color
        String status = payment.getStatus();
        holder.tvStatus.setText(status);
        
        // Handle payment statuses: Pending (orange), Partially Paid (blue), Fully Paid (green)
        switch (status.toLowerCase()) {
            case "pending":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending); // Orange
                break;
            case "partially paid":
            case "partially_paid":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed); // Blue
                break;
            case "fully paid":
            case "fully_paid":
            case "paid":
            case "completed":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_approved); // Green
                break;
            case "failed":
            case "cancelled":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_declined);
                break;
            default:
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending); // Orange
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
        ImageView imgBoardingHouse;
        TextView tvTenantName, tvPropertyName, tvAmount, tvTimestamp, tvStatus, tvDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBoardingHouse = itemView.findViewById(R.id.imgBoardingHouse);
            tvTenantName = itemView.findViewById(R.id.tvTenantName);
            tvPropertyName = itemView.findViewById(R.id.tvPropertyName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }
    }
}


































