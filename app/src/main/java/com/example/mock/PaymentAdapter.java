package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.ViewHolder> {
    
    private List<PaymentData> payments;
    
    public PaymentAdapter(List<PaymentData> payments) {
        this.payments = payments;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
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





