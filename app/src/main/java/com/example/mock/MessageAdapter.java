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

    public MessageAdapter(Context context, List<MessageModel> messages) {
        this.context = context;
        this.messages = messages;
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
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView messageTime;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.textMessage);
            messageTime = itemView.findViewById(R.id.textTime);
        }
    }
}

