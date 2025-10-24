package com.example.mock;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BookingHistoryFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private RecyclerView recyclerView;
    private BookingHistoryAdapter adapter;
    private List<BookingData> bookingHistory;
    private ProgressDialog progressDialog;
    private RequestQueue requestQueue;
    private int userId;

    public static BookingHistoryFragment newInstance(int userId) {
        BookingHistoryFragment fragment = new BookingHistoryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_booking_history, container, false);

        // Get userId from arguments
        if (getArguments() != null) {
            userId = getArguments().getInt(ARG_USER_ID);
        }

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize request queue
        requestQueue = Volley.newRequestQueue(getContext());

        // Initialize with empty list
        bookingHistory = new ArrayList<>();
        loadBookingHistory();

        return view;
    }

    private void loadBookingHistory() {
        showProgressDialog("Loading booking history...");
        
        String url = "https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/get_booking_history.php?user_id=" + userId + "&user_type=owner";
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject data = response.getJSONObject("data");
                            JSONArray bookingsArray = data.getJSONArray("booking_history");
                            
                            bookingHistory.clear();
                            
                            for (int i = 0; i < bookingsArray.length(); i++) {
                                JSONObject bookingObj = bookingsArray.getJSONObject(i);
                                
                                BookingData booking = new BookingData(
                                    bookingObj.getInt("booking_id"),
                                    bookingObj.getString("boarder_name"),
                                    bookingObj.getString("boarder_email"),
                                    bookingObj.getString("boarder_phone"),
                                    bookingObj.getString("room_name"),
                                    bookingObj.getString("start_date"),
                                    bookingObj.getString("end_date"),
                                    bookingObj.getString("amount"),
                                    bookingObj.getString("rent_type"),
                                    bookingObj.getString("status"),
                                    bookingObj.getString("boarding_house_name"),
                                    bookingObj.getString("boarding_house_address"),
                                    bookingObj.getString("booking_date"),
                                    bookingObj.getString("payment_status"),
                                    bookingObj.getString("notes"),
                                    bookingObj.getString("profile_image"),
                                    bookingObj.getInt("boarder_id"),
                                    bookingObj.getInt("room_id"),
                                    bookingObj.getInt("boarding_house_id")
                                );
                                
                                bookingHistory.add(booking);
                            }
                            
                            adapter = new BookingHistoryAdapter(bookingHistory, new BookingHistoryAdapter.OnBookingActionListener() {
                                @Override
                                public void onViewDetails(BookingData booking) {
                                    // Navigate to booking details
                                    Intent intent = new Intent(getContext(), BookingDetailsActivity.class);
                                    // TODO: Pass booking data through intent
                                    startActivity(intent);
                                }
                            });
                            
                            recyclerView.setAdapter(adapter);
                        } else {
                            Toast.makeText(getContext(), "Error loading booking history: " + response.getString("error"), Toast.LENGTH_SHORT).show();
                        }
                        hideProgressDialog();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error parsing booking history data", Toast.LENGTH_SHORT).show();
                        hideProgressDialog();
                    }
                },
                error -> {
                    Toast.makeText(getContext(), "Error loading booking history: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    hideProgressDialog();
                });

        requestQueue.add(request);
    }
    
    private void showProgressDialog(String message) {
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage(message);
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideProgressDialog() {
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            progressDialog = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        hideProgressDialog();
    }
}

















