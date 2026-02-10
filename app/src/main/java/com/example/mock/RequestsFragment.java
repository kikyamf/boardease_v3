package com.example.mock;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class RequestsFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private int userId;

    public static RequestsFragment newInstance(int userId) {
        RequestsFragment fragment = new RequestsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_requests, container, false);

        if (getArguments() != null) {
            userId = getArguments().getInt(ARG_USER_ID);
        }

        LinearLayout layoutTermination = view.findViewById(R.id.layoutTerminationRequests);
        LinearLayout layoutChangeRoom = view.findViewById(R.id.layoutChangeRoomRequests);

        layoutTermination.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), TerminationRequestsActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
        });

        layoutChangeRoom.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ChangeRoomRequestsActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
        });

        return view;
    }
}
