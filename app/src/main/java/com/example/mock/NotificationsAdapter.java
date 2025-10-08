package com.example.mock;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotificationsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_CARD = 1;

    private List<NotificationItemModel> notifList;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationItemModel notification);
    }

    public NotificationsAdapter(List<NotificationItemModel> notifList) {
        this.notifList = notifList;
    }

    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return notifList.get(position).isHeader() ? TYPE_HEADER : TYPE_CARD;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notif_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notif_card, parent, false);
            return new CardViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        NotificationItemModel item = notifList.get(position);

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).txtHeader.setText(item.getTitle());
        } else if (holder instanceof CardViewHolder) {
            CardViewHolder cardHolder = (CardViewHolder) holder;

            // Set text details
            cardHolder.txtTitle.setText(item.getTitle());
            cardHolder.txtMessage.setText(item.getMessage());
            cardHolder.txtTime.setText(item.getTime());

            // Set consistent appearance for all notifications (read and unread look the same)
            cardHolder.itemView.setAlpha(1.0f);
            cardHolder.txtTitle.setTextColor(cardHolder.itemView.getContext().getResources().getColor(android.R.color.black));

            // Change icon depending on type
            switch (item.getType()) {
                case "payment":
                    cardHolder.imgIcon.setImageResource(R.drawable.ic_payment2);
                    break;
                case "maintenance":
                    cardHolder.imgIcon.setImageResource(R.drawable.ic_maintenance);
                    break;
                case "announcement":
                    cardHolder.imgIcon.setImageResource(R.drawable.ic_announcement);
                    break;
                case "booking":
                    cardHolder.imgIcon.setImageResource(R.drawable.ic_announcement);
                    break;
                case "general":
                    cardHolder.imgIcon.setImageResource(R.drawable.ic_notification1);
                    break;
                default:
                    cardHolder.imgIcon.setImageResource(R.drawable.ic_notification1); // fallback
                    break;
            }

            // Set click listener
            cardHolder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(item);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return notifList.size();
    }

    // ViewHolder for header
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView txtHeader;
        HeaderViewHolder(View itemView) {
            super(itemView);
            txtHeader = itemView.findViewById(R.id.tvNotifHeader);
        }
    }

    // ViewHolder for card
    static class CardViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView txtTitle, txtMessage, txtTime;

        CardViewHolder(View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.imgNotifIcon);
            txtTitle = itemView.findViewById(R.id.tvNotifTitle);
            txtMessage = itemView.findViewById(R.id.tvNotifMessage);
            txtTime = itemView.findViewById(R.id.tvNotifTime);
        }
    }
}
