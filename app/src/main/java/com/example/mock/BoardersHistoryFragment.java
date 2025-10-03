package com.example.mock;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class BoardersHistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private BoardersHistoryAdapter adapter;
    private List<BoarderHistoryData> boardersHistory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_boarders_history, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize with sample data
        boardersHistory = new ArrayList<>();
        loadBoardersHistory();

        return view;
    }

    private void loadBoardersHistory() {
        // Sample data - replace with actual API call
        boardersHistory.clear();
        boardersHistory.add(new BoarderHistoryData(
            "Hanna Cuas",
            "Room 2",
            "2025-05-15",
            "2025-08-15",
            "Completed"
        ));
        boardersHistory.add(new BoarderHistoryData(
            "Christe Hanna Mae Cuas",
            "Single",
            "2025-05-18",
            "2025-06-18",
            "Completed"
        ));
        boardersHistory.add(new BoarderHistoryData(
            "Liz",
            "Room 1",
            "2025-05-18",
            "2025-05-22",
            "Completed"
        ));

        adapter = new BoardersHistoryAdapter(boardersHistory);
        recyclerView.setAdapter(adapter);
    }
}





