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

public class AllPaymentsFragment extends Fragment {

    private RecyclerView recyclerView;
    private PaymentAdapter adapter;
    private List<PaymentData> allPayments;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_payments, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize with sample data
        allPayments = new ArrayList<>();
        loadAllPayments();

        return view;
    }

    private void loadAllPayments() {
        // Sample data - replace with actual API call
        allPayments.clear();
        allPayments.add(new PaymentData(
            "Hanna Cuas",
            "Room 2",
            "long-term",
            "₱6,000.00",
            "₱6,000.00",
            "3/3 months paid",
            "Completed",
            "2025-05-18 19:40"
        ));
        allPayments.add(new PaymentData(
            "Liz",
            "Room 1",
            "short-term",
            "₱2,000.00",
            "₱1,000.00",
            "2/1 months paid",
            "Completed",
            "2025-05-18 19:36"
        ));
        allPayments.add(new PaymentData(
            "Christe Hanna Mae Cuas",
            "Single",
            "daily",
            "₱2,800.00",
            "₱400.00",
            "14/2 days paid",
            "Completed",
            "2025-05-18 18:21"
        ));

        adapter = new PaymentAdapter(allPayments);
        recyclerView.setAdapter(adapter);
    }
}
