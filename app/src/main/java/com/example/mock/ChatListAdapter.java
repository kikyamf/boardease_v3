package com.example.mock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
        boolean isGroupChat = "group".equals(chat.getChatType());
        
        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
            // Reset background and scaleType for actual profile pictures
            holder.profileImage.setBackgroundResource(R.drawable.circle_bg);
            holder.profileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.profileImage.setPadding(0, 0, 0, 0);
            String fullImageUrl = "https://reflective-perkily-jakobe.ngrok-free.dev/BoardEase2/" + profilePictureUrl;
            Glide.with(context)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.btn_profile)
                    .error(R.drawable.btn_profile)
                    .centerCrop()
                    .circleCrop()
                    .into(holder.profileImage);
        } else {
            if (isGroupChat) {
                // For group chats, show first letter of group name
                String groupName = chat.getName();
                String firstLetter = getFirstLetter(groupName);
                Drawable letterAvatar = createLetterAvatar(context, firstLetter, 56);
                holder.profileImage.setImageDrawable(letterAvatar);
                holder.profileImage.setBackground(null);
                holder.profileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                holder.profileImage.setPadding(0, 0, 0, 0);
            } else {
                // For individual chats, use light gray background and fit icon inside circle
                holder.profileImage.setBackgroundResource(R.drawable.circle_bg_dark_gray);
                holder.profileImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
                holder.profileImage.setPadding(12, 12, 12, 12);
                holder.profileImage.setImageResource(R.drawable.btn_profile);
            }
        }

        // 🔹 Handle online status indicator (only for individual chats, not group chats)
        if (!isGroupChat) {
            // For now, show online status - you can add actual online status check from API later
            // For individual chats, show online status if available
            boolean isOnline = chat.isOnline(); // This will be false by default until we add the field
            holder.onlineStatus.setVisibility(isOnline ? View.VISIBLE : View.GONE);
        } else {
            // Hide online status for group chats
            holder.onlineStatus.setVisibility(View.GONE);
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
    
    /**
     * Get the first letter of a string, handling empty/null strings
     */
    private String getFirstLetter(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "?";
        }
        String trimmed = text.trim();
        return trimmed.substring(0, 1).toUpperCase();
    }
    
    /**
     * Create a circular avatar with a letter (like Gmail)
     * @param context The context
     * @param letter The letter to display
     * @param size The size of the avatar in dp (will be converted to pixels)
     * @return A Drawable containing the circular letter avatar
     */
    private Drawable createLetterAvatar(Context context, String letter, int size) {
        // Convert dp to pixels
        float density = context.getResources().getDisplayMetrics().density;
        int sizePx = (int) (size * density);
        
        // Create bitmap
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // Generate a color based on the letter (for consistency)
        int color = getColorForLetter(letter);
        
        // Draw circle background
        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(color);
        float radius = sizePx / 2.0f;
        canvas.drawCircle(radius, radius, radius, circlePaint);
        
        // Draw letter
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(sizePx * 0.5f); // Letter size is 50% of circle
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
        
        // Center the text vertically
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float textHeight = fontMetrics.bottom - fontMetrics.top;
        float textOffset = textHeight / 2 - fontMetrics.bottom;
        canvas.drawText(letter, radius, radius + textOffset, textPaint);
        
        return new BitmapDrawable(context.getResources(), bitmap);
    }
    
    /**
     * Generate a consistent color for a letter (like Gmail does)
     * Uses a simple hash function to map letters to colors
     */
    private int getColorForLetter(String letter) {
        // Array of nice colors (similar to Gmail's palette)
        int[] colors = {
            0xFF4285F4, // Blue
            0xFF34A853, // Green
            0xFFEA4335, // Red
            0xFFFBBC04, // Yellow
            0xFF9C27B0, // Purple
            0xFF00BCD4, // Cyan
            0xFFFF9800, // Orange
            0xFF795548, // Brown
            0xFF607D8B, // Blue Grey
            0xFFE91E63, // Pink
            0xFF3F51B5, // Indigo
            0xFF009688, // Teal
        };
        
        // Use the letter's character code to pick a color
        char ch = letter.charAt(0);
        int index = Math.abs(ch) % colors.length;
        return colors[index];
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView userName, lastMessage, messageTime, unreadBadge;
        View onlineStatus;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            userName = itemView.findViewById(R.id.userName);
            lastMessage = itemView.findViewById(R.id.lastMessage);
            messageTime = itemView.findViewById(R.id.messageTime);
            unreadBadge = itemView.findViewById(R.id.unreadBadge);
            onlineStatus = itemView.findViewById(R.id.onlineStatus);
        }
    }
}
