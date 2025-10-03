package com.example.mock;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class Notification extends AppCompatActivity {

    private RecyclerView recyclerNotifications;
    private NotificationsAdapter notificationsAdapter;
    private ImageButton btnBack;
    private List<NotificationItemModel> notifList;
    private LinearLayout emptyLayout; // 🔹 Added for empty state

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        btnBack = findViewById(R.id.btnBack);

        recyclerNotifications = findViewById(R.id.rvNotifications);
        emptyLayout = findViewById(R.id.emptyLayout); // 🔹 Reference the empty view

        recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));

        notifList = new ArrayList<>();

        // Back button action
        btnBack.setOnClickListener(v -> finish());

//        // ✅ Dummy Boarding House Notifications
//        // (Comment this block if you want to test empty state)
//        notifList.add(new NotificationItemModel("Today", true));
//
//        notifList.add(new NotificationItemModel(
//                "Payment Due",
//                "Your monthly rent is due tomorrow.",
//                "5 min ago",
//                "payment"
//        ));
//
//        notifList.add(new NotificationItemModel(
//                "Maintenance Notice",
//                "Water system check scheduled at 3PM.",
//                "2 hrs ago",
//                "maintenance"
//        ));
//
//        notifList.add(new NotificationItemModel("Earlier", true));
//
//        notifList.add(new NotificationItemModel(
//                "Welcome to BoardEase 🎉",
//                "Your profile has been activated successfully.",
//                "Yesterday",
//                "announcement"
//        ));
//
//        notifList.add(new NotificationItemModel(
//                "House Rule Reminder",
//                "Quiet hours are from 10PM - 6AM.",
//                "2 days ago",
//                "reminder"
//        ));

        // ✅ Setup Adapter
        notificationsAdapter = new NotificationsAdapter(notifList);
        recyclerNotifications.setAdapter(notificationsAdapter);

        // ✅ Toggle empty state
        if (notifList.isEmpty()) {
            recyclerNotifications.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerNotifications.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);
        }
    }
}
