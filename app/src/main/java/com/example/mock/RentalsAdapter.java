package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RentalsAdapter extends RecyclerView.Adapter<RentalsAdapter.ViewHolder> {

    private List<RentalsFragment.RentalLog> rentalLogs;
    private OnRentalClickListener clickListener;

    public interface OnRentalClickListener {
        void onRentalClick(RentalsFragment.RentalLog rental);
    }

    public RentalsAdapter(List<RentalsFragment.RentalLog> rentalLogs, OnRentalClickListener clickListener) {
        this.rentalLogs = rentalLogs;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rental_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RentalsFragment.RentalLog rental = rentalLogs.get(position);
        
        holder.tvTenantName.setText(rental.getTenantName());
        holder.tvPropertyName.setText(rental.getPropertyName());
        holder.tvTimestamp.setText(rental.getTimestamp());
        holder.tvDescription.setText(rental.getDescription());
        
        // Set status with appropriate color
        holder.tvStatus.setText(rental.getStatus());
        switch (rental.getStatus().toLowerCase()) {
            case "new":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_approved);
                break;
            case "extended":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                break;
            case "terminated":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_declined);
                break;
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onRentalClick(rental);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rentalLogs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTenantName, tvPropertyName, tvTimestamp, tvStatus, tvDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTenantName = itemView.findViewById(R.id.tvTenantName);
            tvPropertyName = itemView.findViewById(R.id.tvPropertyName);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }
    }
}

































