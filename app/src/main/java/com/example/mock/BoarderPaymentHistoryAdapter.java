package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BoarderPaymentHistoryAdapter extends RecyclerView.Adapter<BoarderPaymentHistoryAdapter.ViewHolder> {

    private List<PaymentHistoryActivity.PaymentHistoryItem> paymentList;

    public BoarderPaymentHistoryAdapter(List<PaymentHistoryActivity.PaymentHistoryItem> paymentList) {
        this.paymentList = paymentList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_boarder_payment_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PaymentHistoryActivity.PaymentHistoryItem payment = paymentList.get(position);

        // Set payment date and time
        String formattedDate = payment.getFormattedDate();
        if (formattedDate == null || formattedDate.isEmpty()) {
            formattedDate = formatDate(payment.getPaymentDate());
        }
        
        String[] dateTimeParts = splitDateTime(formattedDate);
        holder.tvPaymentDate.setText(dateTimeParts[0]);
        if (dateTimeParts.length > 1 && !dateTimeParts[1].isEmpty()) {
            holder.tvPaymentTime.setText(dateTimeParts[1]);
            holder.tvPaymentTime.setVisibility(View.VISIBLE);
        } else {
            holder.tvPaymentTime.setVisibility(View.GONE);
        }

        // Set payment status - convert to simple status (Successful, Pending, Failed)
        String paymentStatus = payment.getPaymentStatus();
        if (paymentStatus == null || paymentStatus.isEmpty()) {
            paymentStatus = payment.getStatus();
        }
        
        // Convert to simple status labels
        String simpleStatus = convertToSimpleStatus(paymentStatus);
        holder.tvPaymentStatus.setText(simpleStatus);
        setStatusBackground(holder.tvPaymentStatus, simpleStatus);

        // Set amount
        holder.tvAmount.setText(payment.getAmount());

        // Set payment method
        String paymentMethod = payment.getPaymentMethod();
        if (paymentMethod == null || paymentMethod.isEmpty()) {
            paymentMethod = "N/A";
        }
        holder.tvPaymentMethod.setText(paymentMethod);

        // Set room information (optional - only show if available)
        String roomInfo = payment.getRoomInfo();
        if (roomInfo == null || roomInfo.isEmpty()) {
            String roomName = payment.getRoomName();
            String roomNumber = payment.getRoomNumber();
            if (roomName != null && !roomName.isEmpty()) {
                roomInfo = roomName;
                if (roomNumber != null && !roomNumber.isEmpty()) {
                    roomInfo += " - " + roomNumber;
                }
            } else if (roomNumber != null && !roomNumber.isEmpty()) {
                roomInfo = roomNumber;
            }
        }
        
        if (roomInfo != null && !roomInfo.isEmpty() && !roomInfo.equals("N/A")) {
            holder.layoutRoomInfo.setVisibility(View.VISIBLE);
            holder.tvRoomInfo.setText(roomInfo);
        } else {
            holder.layoutRoomInfo.setVisibility(View.GONE);
        }

        // Set boarding house name
        String bhName = payment.getBoardingHouseName();
        if (bhName == null || bhName.isEmpty()) {
            bhName = "";
        }
        holder.tvBoardingHouseName.setText(bhName);
    }


    private String convertToSimpleStatus(String status) {
        if (status == null || status.isEmpty()) {
            return "Pending for Approval";
        }
        
        String statusLower = status.toLowerCase();
        // Check for "Successful" first (from PHP enhanced status)
        if (statusLower.contains("successful")) {
            return "Successful";
        }
        // Fully Paid, Partially Paid, Paid, or Completed = Successful
        else if (statusLower.contains("fully paid") || 
            statusLower.contains("partially paid") || 
            statusLower.contains("completed") || 
            (statusLower.contains("paid") && !statusLower.contains("pending"))) {
            return "Successful";
        } 
        // Check for "Pending for Approval" (from PHP enhanced status)
        else if (statusLower.contains("pending for approval") || 
                 (statusLower.contains("pending") && statusLower.contains("approval"))) {
            return "Pending for Approval";
        } 
        // Check for just "pending" (fallback)
        else if (statusLower.contains("pending")) {
            return "Pending for Approval";
        } 
        else if (statusLower.contains("failed") || statusLower.contains("cancelled")) {
            return "Failed";
        } else {
            // Default to Pending for Approval
            return "Pending for Approval";
        }
    }
    
    private void setStatusBackground(TextView statusView, String status) {
        if (status == null) {
            statusView.setBackgroundResource(R.drawable.bg_status_pending);
            return;
        }

        String statusLower = status.toLowerCase();
        // Check for "Successful" first (exact match from PHP)
        if (statusLower.equals("successful") || statusLower.contains("successful")) {
            statusView.setBackgroundResource(R.drawable.bg_status_approved); // Green
        }
        // Successful includes: Fully Paid, Partially Paid, Paid, Completed
        else if (statusLower.contains("fully paid") || 
            statusLower.contains("partially paid") || 
            statusLower.contains("completed") || 
            (statusLower.contains("paid") && !statusLower.contains("pending"))) {
            statusView.setBackgroundResource(R.drawable.bg_status_approved); // Green
        } 
        // Check for "Pending for Approval" (exact match from PHP)
        else if (statusLower.equals("pending for approval") || 
                 (statusLower.contains("pending") && statusLower.contains("approval"))) {
            statusView.setBackgroundResource(R.drawable.bg_status_pending); // Orange
        } 
        // Check for just "pending" (fallback)
        else if (statusLower.contains("pending")) {
            statusView.setBackgroundResource(R.drawable.bg_status_pending); // Orange
        } 
        else if (statusLower.contains("failed")) {
            statusView.setBackgroundResource(R.drawable.bg_status_declined); // Red
        } else {
            statusView.setBackgroundResource(R.drawable.bg_status_pending); // Default Orange
        }
    }

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty() || dateString.equals("null")) {
            return "";
        }

        try {
            // Try to parse with time
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            
            Date date = inputFormat.parse(dateString);
            if (date != null) {
                return dateFormat.format(date) + " at " + timeFormat.format(date);
            }
        } catch (ParseException e) {
            try {
                // Try without time
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
                
                Date date = inputFormat.parse(dateString);
                if (date != null) {
                    return outputFormat.format(date);
                }
            } catch (ParseException e2) {
                // Return original if parsing fails
            }
        }

        return dateString;
    }

    private String[] splitDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isEmpty()) {
            return new String[]{"", ""};
        }

        // Check if it contains "at" (formatted date with time)
        if (dateTimeString.contains(" at ")) {
            String[] parts = dateTimeString.split(" at ", 2);
            return new String[]{parts[0], parts.length > 1 ? parts[1] : ""};
        }

        // If no time separator, return date only
        return new String[]{dateTimeString, ""};
    }

    @Override
    public int getItemCount() {
        return paymentList != null ? paymentList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPaymentDate, tvPaymentTime, tvPaymentStatus;
        TextView tvAmount, tvPaymentMethod, tvRoomInfo, tvBoardingHouseName;
        LinearLayout layoutRoomInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPaymentDate = itemView.findViewById(R.id.tvPaymentDate);
            tvPaymentTime = itemView.findViewById(R.id.tvPaymentTime);
            tvPaymentStatus = itemView.findViewById(R.id.tvPaymentStatus);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvPaymentMethod = itemView.findViewById(R.id.tvPaymentMethod);
            tvRoomInfo = itemView.findViewById(R.id.tvRoomInfo);
            tvBoardingHouseName = itemView.findViewById(R.id.tvBoardingHouseName);
            layoutRoomInfo = itemView.findViewById(R.id.layoutRoomInfo);
        }
    }

}
