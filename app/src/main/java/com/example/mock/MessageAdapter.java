package com.example.mock;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private Context context;
    private List<MessageModel> messages;
    private String chatType; // "individual" or "group"

    public MessageAdapter(Context context, List<MessageModel> messages) {
        this.context = context;
        this.messages = messages;
        this.chatType = "individual"; // Default to individual
    }

    public MessageAdapter(Context context, List<MessageModel> messages, String chatType) {
        this.context = context;
        this.messages = messages;
        this.chatType = chatType;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isReceiver() ? 1 : 0;
        // 0 = received (gray), 1 = sent (brown)
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0) {
            // Received bubble (gray)
            view = LayoutInflater.from(context).inflate(R.layout.item_message_receiver, parent, false);
        } else {
            // Sent bubble (brown)
            view = LayoutInflater.from(context).inflate(R.layout.item_message_sender, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        MessageModel message = messages.get(position);
        holder.messageText.setText(message.getMessage());
        holder.messageTime.setText(message.getTimestamp());
        
        // Show sender name for group chats, but only for sent messages (your own)
        if ("group".equals(chatType) && !message.isReceiver() && message.getSenderName() != null && !message.getSenderName().isEmpty()) {
            holder.senderName.setText(message.getSenderName());
            holder.senderName.setVisibility(View.VISIBLE);
        } else {
            holder.senderName.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView messageTime;
        TextView senderName;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.textMessage);
            messageTime = itemView.findViewById(R.id.textTime);
            senderName = itemView.findViewById(R.id.textSenderName);
        }
    }
}

