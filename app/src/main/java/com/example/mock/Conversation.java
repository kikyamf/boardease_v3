package com.example.mock;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class Conversation extends AppCompatActivity {

    private ImageView btnBack, chatProfileImage;
    private TextView chatUserName;
    private RecyclerView recyclerMessages;
    private EditText editMessage;
    private ImageButton btnSend;

    private MessageAdapter messageAdapter;
    private List<MessageModel> messageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensure layout resizes when keyboard appears
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        );

        setContentView(R.layout.activity_conversation);



        // Bind views
        btnBack = findViewById(R.id.btnBack);
        chatProfileImage = findViewById(R.id.chatProfileImage);
        chatUserName = findViewById(R.id.chatUserName);
        recyclerMessages = findViewById(R.id.recyclerMessages);
        editMessage = findViewById(R.id.editMessage);
        btnSend = findViewById(R.id.btnSend);

        // Get data from intent
        String name = getIntent().getStringExtra("chatName");
        int imageRes = getIntent().getIntExtra("chatImage", R.drawable.ic_profile);

        chatUserName.setText(name);
        chatProfileImage.setImageResource(imageRes);

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Setup RecyclerView
        messageList = new ArrayList<>();
        messageList.add(new MessageModel("Hello " + name + " 👋", true));
        messageList.add(new MessageModel("How are you?", true));

        messageAdapter = new MessageAdapter(this, messageList);
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerMessages.setAdapter(messageAdapter);

        // Send button
        btnSend.setOnClickListener(v -> {
            String msg = editMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                messageList.add(new MessageModel(msg, false)); // false = sent by "me"
                messageAdapter.notifyItemInserted(messageList.size() - 1);
                recyclerMessages.scrollToPosition(messageList.size() - 1);
                editMessage.setText("");
            }
        });
    }
}
