package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ReservationsAdapter extends RecyclerView.Adapter<ReservationsAdapter.ViewHolder> {

    private List<ReservationsFragment.ReservationLog> reservationLogs;
    private OnReservationClickListener clickListener;

    public interface OnReservationClickListener {
        void onReservationClick(ReservationsFragment.ReservationLog reservation);
    }

    public ReservationsAdapter(List<ReservationsFragment.ReservationLog> reservationLogs, OnReservationClickListener clickListener) {
        this.reservationLogs = reservationLogs;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reservation_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReservationsFragment.ReservationLog reservation = reservationLogs.get(position);
        
        holder.tvTenantName.setText(reservation.getTenantName());
        holder.tvPropertyName.setText(reservation.getPropertyName());
        holder.tvTimestamp.setText(reservation.getTimestamp());
        holder.tvDescription.setText(reservation.getDescription());
        
        // Set status with appropriate color
        holder.tvStatus.setText(reservation.getStatus());
        switch (reservation.getStatus().toLowerCase()) {
            case "pending":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                break;
            case "approved":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_approved);
                break;
            case "declined":
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_declined);
                break;
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onReservationClick(reservation);
            }
        });
    }

    @Override
    public int getItemCount() {
        return reservationLogs.size();
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

































