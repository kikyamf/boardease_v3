package com.example.mock.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mock.ChatItem;
import com.example.mock.MessagesActivity;
import com.example.mock.R;

import java.util.List;

/**
 * ChatListAdapter - Adapter for main chat list RecyclerView in MessagesActivity
 * Displays all chat conversations with last message and timestamp
 */
public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private List<ChatItem> chatList;
    private OnChatClickListener onChatClickListener;

    public interface OnChatClickListener {
        void onChatClick(ChatItem chat);
    }

    public ChatListAdapter(List<ChatItem> chatList, OnChatClickListener listener) {
        this.chatList = chatList;
        this.onChatClickListener = listener;
    }
    
    // Constructor for MessagesActivity
    public ChatListAdapter(List<ChatItem> chatList, MessagesActivity activity) {
        this.chatList = chatList;
        this.onChatClickListener = activity::onChatClick;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        try {
            ChatItem chat = chatList.get(position);
            holder.bind(chat);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return chatList != null ? chatList.size() : 0;
    }

    public class ChatViewHolder extends RecyclerView.ViewHolder {
        private ImageView chatImage;
        private TextView chatName;
        private TextView lastMessage;
        private TextView timestamp;
        private TextView unreadBadge;
        private ImageView chatTypeIcon;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            chatImage = itemView.findViewById(R.id.chatImage);
            chatName = itemView.findViewById(R.id.chatName);
            lastMessage = itemView.findViewById(R.id.lastMessage);
            timestamp = itemView.findViewById(R.id.timestamp);
            unreadBadge = itemView.findViewById(R.id.unreadBadge);
            chatTypeIcon = itemView.findViewById(R.id.chatTypeIcon);

            // Set click listener
            itemView.setOnClickListener(v -> {
                try {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && onChatClickListener != null) {
                        onChatClickListener.onChatClick(chatList.get(position));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        public void bind(ChatItem chat) {
            try {
                // Set chat name
                if (chatName != null) {
                    if ("DM".equals(chat.getChatType())) {
                        chatName.setText(chat.getOtherParticipantName());
                    } else {
                        chatName.setText(chat.getGroupName());
                    }
                }

                // Set last message
                if (lastMessage != null) {
                    lastMessage.setText(chat.getLastMessage());
                }

                // Set timestamp
                if (timestamp != null) {
                    timestamp.setText(getRelativeTime(chat.getTimestamp()));
                }

                // Set chat image
                if (chatImage != null) {
                    // For now, set a default image
                    chatImage.setImageResource(R.drawable.ic_person1);
                    
                    // TODO: Load image from URL using Glide or Picasso
                    // if ("DM".equals(chat.getChatType())) {
                    //     Glide.with(itemView.getContext())
                    //         .load(chat.getOtherParticipantImageUrl())
                    //         .placeholder(R.drawable.ic_person1)
                    //         .into(chatImage);
                    // } else {
                    //     Glide.with(itemView.getContext())
                    //         .load(chat.getGroupImageUrl())
                    //         .placeholder(R.drawable.ic_person1)
                    //         .into(chatImage);
                    // }
                }

                // Set chat type icon
                if (chatTypeIcon != null) {
                    if ("DM".equals(chat.getChatType())) {
                        chatTypeIcon.setImageResource(R.drawable.ic_person1);
                        chatTypeIcon.setVisibility(View.GONE); // Hide for DMs
                    } else {
                        chatTypeIcon.setImageResource(R.drawable.ic_group);
                        chatTypeIcon.setVisibility(View.VISIBLE);
                    }
                }

                // Show unread count badge
                if (unreadBadge != null) {
                    if (chat.getUnreadCount() > 0) {
                        unreadBadge.setVisibility(View.VISIBLE);
                        unreadBadge.setText(String.valueOf(chat.getUnreadCount()));
                    } else {
                        unreadBadge.setVisibility(View.GONE);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private String getRelativeTime(long timestamp) {
            try {
                return DateUtils.getRelativeTimeSpanString(
                        timestamp,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE
                ).toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "Just now";
            }
        }
    }
}
