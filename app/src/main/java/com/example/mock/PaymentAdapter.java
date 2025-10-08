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
    
    private List<PaymentData> payments;
    private Context context;
    private PaymentActionListener actionListener;
    
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
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_payment, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PaymentData payment = payments.get(position);
        
        holder.tvBoarderName.setText(payment.getBoarderName());
        holder.tvRoom.setText(payment.getRoom());
        holder.tvRentType.setText(payment.getRentType());
        holder.tvAmountPaid.setText(payment.getAmountPaid());
        holder.tvTotalAmount.setText(payment.getTotalAmount());
        holder.tvPaymentStatus.setText(payment.getPaymentStatus());
        holder.tvRentalStatus.setText(payment.getRentalStatus());
        holder.tvPaymentDate.setText(payment.getPaymentDate());
        
        // Set status color based on payment status
        setStatusColor(holder.tvPaymentStatus, payment.getPaymentStatus());
        
        // Set click listeners for action buttons if available
        if (actionListener != null) {
            holder.itemView.setOnClickListener(v -> actionListener.onViewDetails(payment));
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
        TextView tvBoarderName, tvRoom, tvRentType, tvAmountPaid, tvTotalAmount;
        TextView tvPaymentStatus, tvRentalStatus, tvPaymentDate;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBoarderName = itemView.findViewById(R.id.tvBoarderName);
            tvRoom = itemView.findViewById(R.id.tvRoom);
            tvRentType = itemView.findViewById(R.id.tvRentType);
            tvAmountPaid = itemView.findViewById(R.id.tvAmountPaid);
            tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
            tvPaymentStatus = itemView.findViewById(R.id.tvPaymentStatus);
            tvRentalStatus = itemView.findViewById(R.id.tvRentalStatus);
            tvPaymentDate = itemView.findViewById(R.id.tvPaymentDate);
        }
    }
}





















