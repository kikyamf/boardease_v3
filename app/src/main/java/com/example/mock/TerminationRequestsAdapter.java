package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TerminationRequestsAdapter extends RecyclerView.Adapter<TerminationRequestsAdapter.ViewHolder> {

    private List<TerminationRequestData> requestList;
    private OnActionListener listener;

    public interface OnActionListener {
        void onApprove(TerminationRequestData request);
        void onDecline(TerminationRequestData request);
    }

    public TerminationRequestsAdapter(List<TerminationRequestData> requestList, OnActionListener listener) {
        this.requestList = requestList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_termination_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TerminationRequestData request = requestList.get(position);
        holder.tvBoarderName.setText(request.getBoarderName());
        holder.tvRoomInfo.setText(request.getBoardingHouseName() + " - " + request.getRoomType() + " (" + request.getRoomNumber() + ")");
        holder.tvReason.setText("Reason: " + request.getReason());
        holder.tvDetails.setText(request.getDetails());
        holder.tvDate.setText(request.getCreatedAt());

        holder.btnApprove.setOnClickListener(v -> listener.onApprove(request));
        holder.btnDecline.setOnClickListener(v -> listener.onDecline(request));
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBoarderName, tvRoomInfo, tvReason, tvDetails, tvDate;
        Button btnApprove, btnDecline;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBoarderName = itemView.findViewById(R.id.tvBoarderName);
            tvRoomInfo = itemView.findViewById(R.id.tvRoomInfo);
            tvReason = itemView.findViewById(R.id.tvReason);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }
}
