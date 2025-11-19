package com.example.mock;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private Context context;
    private List<ChatModel> chatList;
    private OnItemClickListener listener; // 🔹 Add listener

    // Interface for click handling
    public interface OnItemClickListener {
        void onItemClick(ChatModel chat);
        void onItemLongClick(ChatModel chat);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public ChatListAdapter(Context context, List<ChatModel> chatList) {
        this.context = context;
        this.chatList = chatList;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_list_item, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatModel chat = chatList.get(position);
        // Format name to ensure suffixes are preserved
        String displayName = NameFormatter.formatFullName(chat.getName());
        holder.userName.setText(displayName);
        holder.lastMessage.setText(chat.getLastMessage());
        holder.messageTime.setText(chat.getTime());
        
        // Load profile picture from URL if available, otherwise use default
        String profilePictureUrl = chat.getProfilePictureUrl();
        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
            String fullImageUrl = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/" + profilePictureUrl;
            Glide.with(context)
                    .load(fullImageUrl)
                    .placeholder(chat.getImageResId())
                    .error(chat.getImageResId())
                    .centerCrop()
                    .circleCrop()
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(chat.getImageResId());
        }

        // 🔹 Handle unread message indicators (like Messenger)
        boolean hasUnreadMessages = chat.getUnreadCount() > 0;
        
        if (hasUnreadMessages) {
            // Bold text for unread messages
            holder.userName.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.lastMessage.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.messageTime.setTypeface(null, android.graphics.Typeface.BOLD);
            
            // Darker colors for unread messages
            holder.userName.setTextColor(context.getResources().getColor(android.R.color.black));
            holder.lastMessage.setTextColor(context.getResources().getColor(android.R.color.black));
            holder.messageTime.setTextColor(context.getResources().getColor(android.R.color.black));
            
            // Show unread badge with count
            holder.unreadBadge.setVisibility(View.VISIBLE);
            if (chat.getUnreadCount() > 99) {
                holder.unreadBadge.setText("99+");
            } else {
                holder.unreadBadge.setText(String.valueOf(chat.getUnreadCount()));
            }
        } else {
            // Normal text for read messages
            holder.userName.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.lastMessage.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.messageTime.setTypeface(null, android.graphics.Typeface.NORMAL);
            
            // Lighter colors for read messages
            holder.userName.setTextColor(context.getResources().getColor(android.R.color.black));
            holder.lastMessage.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
            holder.messageTime.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
            
            // Hide unread badge
            holder.unreadBadge.setVisibility(View.GONE);
        }

            // 🔹 Handle item clicks
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(chat);
                }
            });
            
            // 🔹 Handle long press for delete
            holder.itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onItemLongClick(chat);
                }
                return true; // Consume the long press event
            });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView userName, lastMessage, messageTime, unreadBadge;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            userName = itemView.findViewById(R.id.userName);
            lastMessage = itemView.findViewById(R.id.lastMessage);
            messageTime = itemView.findViewById(R.id.messageTime);
            unreadBadge = itemView.findViewById(R.id.unreadBadge);
        }
    }
}
