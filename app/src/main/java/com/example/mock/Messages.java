package com.example.mock;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class Messages extends AppCompatActivity {

    private ImageView backButton, searchButton, addGroupButton;
    private RecyclerView profileRecyclerView, chatListRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_messages);

        // Adjust for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        backButton = findViewById(R.id.backButton);
        searchButton = findViewById(R.id.searchButton);
        addGroupButton = findViewById(R.id.addGroupButton);
        profileRecyclerView = findViewById(R.id.profileRecyclerView);
        chatListRecyclerView = findViewById(R.id.chatListRecyclerView);

        // Back button action
        backButton.setOnClickListener(v -> finish());

        // Search button action
        searchButton.setOnClickListener(v -> {
            // TODO: Open search activity or show search bar
        });

        // Add group action
        addGroupButton.setOnClickListener(v -> {
            // TODO: Open create group activity
        });

        // Setup RecyclerViews with dummy data
        setupRecyclerViews();
    }

    private void setupRecyclerViews() {
        // Dummy profile data
        List<ProfileModel> profiles = new ArrayList<>();
        profiles.add(new ProfileModel("Alice", R.drawable.ic_profile));
        profiles.add(new ProfileModel("Bob", R.drawable.ic_profile));
        profiles.add(new ProfileModel("Group Chat", R.drawable.ic_profile));

        // Dummy chat data
        List<ChatModel> chats = new ArrayList<>();
        chats.add(new ChatModel("Alice", "See you soon!", "10:30 AM", R.drawable.ic_profile));
        chats.add(new ChatModel("Bob", "Thanks!", "Yesterday", R.drawable.ic_profile));
        chats.add(new ChatModel("Group Chat", "Meeting at 5 PM", "Mon", R.drawable.ic_profile));

        // Horizontal RecyclerView for profiles
        LinearLayoutManager horizontalLayoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        profileRecyclerView.setLayoutManager(horizontalLayoutManager);
        ProfileAdapter profileAdapter = new ProfileAdapter(this, profiles);
        profileRecyclerView.setAdapter(profileAdapter);

        // Vertical RecyclerView for chat list
        LinearLayoutManager verticalLayoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        chatListRecyclerView.setLayoutManager(verticalLayoutManager);

        ChatListAdapter chatListAdapter = new ChatListAdapter(this, chats);

        // Click handler for chat list items
        chatListAdapter.setOnItemClickListener(chat -> {
            Intent intent = new Intent(Messages.this, Conversation.class);
            intent.putExtra("chatName", chat.getName());
            intent.putExtra("chatImage", chat.getImageResId());
            startActivity(intent);
        });

        chatListRecyclerView.setAdapter(chatListAdapter);
    }
}
