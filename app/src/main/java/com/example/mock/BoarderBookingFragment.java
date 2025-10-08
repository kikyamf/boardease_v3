package com.example.mock;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mock.adapters.BookingAdapter;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * BoarderBookingFragment - Displays current bookings and booking history
 * Allows users to view their booking details and history
 */
public class BoarderBookingFragment extends Fragment {

    // Views
    private RecyclerView rvCurrentBookings;
    private RecyclerView rvBookingHistory;
    private LinearLayout layoutCurrentBookingsEmpty;
    private LinearLayout layoutBookingHistoryEmpty;
    private ProgressBar progressBar;

    // Adapters
    private BookingAdapter currentBookingsAdapter;
    private BookingAdapter bookingHistoryAdapter;

    // Data
    private List<Booking> currentBookings;
    private List<Booking> bookingHistory;

    public BoarderBookingFragment() {
        // Required empty public constructor
    }

    public static BoarderBookingFragment newInstance() {
        return new BoarderBookingFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_boarder_booking, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupRecyclerViews();
        loadBookingData();
    }

    private void initializeViews(View view) {
        try {
            rvCurrentBookings = view.findViewById(R.id.rvCurrentBookings);
            rvBookingHistory = view.findViewById(R.id.rvBookingHistory);
            layoutCurrentBookingsEmpty = view.findViewById(R.id.layoutCurrentBookingsEmpty);
            layoutBookingHistoryEmpty = view.findViewById(R.id.layoutBookingHistoryEmpty);
            progressBar = view.findViewById(R.id.progressBar);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupRecyclerViews() {
        try {
            // Initialize data lists
            currentBookings = new ArrayList<>();
            bookingHistory = new ArrayList<>();

            // Setup Current Bookings RecyclerView
            currentBookingsAdapter = new BookingAdapter(getContext(), currentBookings, this::showBookingDetailsDialog);
            LinearLayoutManager currentLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
            rvCurrentBookings.setLayoutManager(currentLayoutManager);
            rvCurrentBookings.setAdapter(currentBookingsAdapter);

            // Setup Booking History RecyclerView
            bookingHistoryAdapter = new BookingAdapter(getContext(), bookingHistory, this::showBookingDetailsDialog);
            LinearLayoutManager historyLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
            rvBookingHistory.setLayoutManager(historyLayoutManager);
            rvBookingHistory.setAdapter(bookingHistoryAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadBookingData() {
        try {
            // Show loading indicator
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }

            // Create mock data
            createMockBookingData();

            // Hide loading indicator and update UI
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            updateUI();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createMockBookingData() {
        // Create mock current bookings
        currentBookings.clear();
        currentBookings.add(new Booking(1, "Sunshine Boarding House", "sample_listing", 
            "Quezon City, Metro Manila", "January 1, 2024", "December 31, 2024", 
            "₱3,500", "₱0", "Active"));

        // Create mock booking history
        bookingHistory.clear();
        bookingHistory.add(new Booking(2, "Green Valley Dormitory", "sample_listing", 
            "Makati City, Metro Manila", "June 1, 2023", "December 31, 2023", 
            "₱4,000", "₱0", "Completed"));
        bookingHistory.add(new Booking(3, "Metro Student Housing", "sample_listing", 
            "Taguig City, Metro Manila", "January 1, 2023", "May 31, 2023", 
            "₱3,200", "₱0", "Completed"));
    }

    private void updateUI() {
        try {
            // Update current bookings
            if (currentBookingsAdapter != null) {
                currentBookingsAdapter.notifyDataSetChanged();
            }
            
            // Update booking history
            if (bookingHistoryAdapter != null) {
                bookingHistoryAdapter.notifyDataSetChanged();
            }

            // Show/hide empty states
            if (currentBookings.isEmpty()) {
                if (rvCurrentBookings != null) {
                    rvCurrentBookings.setVisibility(View.GONE);
                }
                if (layoutCurrentBookingsEmpty != null) {
                    layoutCurrentBookingsEmpty.setVisibility(View.VISIBLE);
                }
            } else {
                if (rvCurrentBookings != null) {
                    rvCurrentBookings.setVisibility(View.VISIBLE);
                }
                if (layoutCurrentBookingsEmpty != null) {
                    layoutCurrentBookingsEmpty.setVisibility(View.GONE);
                }
            }

            if (bookingHistory.isEmpty()) {
                if (rvBookingHistory != null) {
                    rvBookingHistory.setVisibility(View.GONE);
                }
                if (layoutBookingHistoryEmpty != null) {
                    layoutBookingHistoryEmpty.setVisibility(View.VISIBLE);
                }
            } else {
                if (rvBookingHistory != null) {
                    rvBookingHistory.setVisibility(View.VISIBLE);
                }
                if (layoutBookingHistoryEmpty != null) {
                    layoutBookingHistoryEmpty.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showBookingDetailsDialog(Booking booking) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_booking_details, null);
            builder.setView(dialogView);

            // Initialize dialog views
            ImageView imgBoardingHouse = dialogView.findViewById(R.id.imgBoardingHouse);
            TextView tvBoardingHouseName = dialogView.findViewById(R.id.tvBoardingHouseName);
            TextView tvLocation = dialogView.findViewById(R.id.tvLocation);
            TextView tvStartDate = dialogView.findViewById(R.id.tvStartDate);
            TextView tvEndDate = dialogView.findViewById(R.id.tvEndDate);
            TextView tvMonthlyDue = dialogView.findViewById(R.id.tvMonthlyDue);
            TextView tvBalanceDue = dialogView.findViewById(R.id.tvBalanceDue);
            TextView tvStatus = dialogView.findViewById(R.id.tvStatus);
            MaterialButton btnCloseDialog = dialogView.findViewById(R.id.btnCloseDialog);
            
            // Initialize new buttons
            MaterialButton btnSupportTicket = dialogView.findViewById(R.id.btnSupportTicket);
            MaterialButton btnMakePayment = dialogView.findViewById(R.id.btnMakePayment);
            MaterialButton btnSubmitReview = dialogView.findViewById(R.id.btnSubmitReview);
            RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
            EditText etReviewMessage = dialogView.findViewById(R.id.etReviewMessage);

            // Set booking data
            if (booking.getImagePath() != null && !booking.getImagePath().isEmpty()) {
                Glide.with(getContext())
                        .load(booking.getImagePath())
                        .placeholder(R.drawable.sample_listing)
                        .error(R.drawable.sample_listing)
                        .into(imgBoardingHouse);
            } else {
                imgBoardingHouse.setImageResource(R.drawable.sample_listing);
            }

            tvBoardingHouseName.setText(booking.getBoardingHouseName());
            tvLocation.setText(booking.getLocation());
            tvStartDate.setText(booking.getStartDate());
            tvEndDate.setText(booking.getEndDate());
            tvMonthlyDue.setText(booking.getMonthlyDue());
            tvBalanceDue.setText(booking.getBalanceDue());
            tvStatus.setText(booking.getStatus());

            // Set status background based on status
            if ("Active".equals(booking.getStatus())) {
                tvStatus.setBackgroundResource(R.drawable.bg_status_approved);
            } else if ("Completed".equals(booking.getStatus())) {
                tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
            } else {
                tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
            }

            AlertDialog dialog = builder.create();
            dialog.show();

            // Close button click listener
            btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
            
            // Support Ticket button click listener
            btnSupportTicket.setOnClickListener(v -> {
                dialog.dismiss();
                showSupportTicketDialog(booking);
            });
            
            // Make Payment button click listener
            btnMakePayment.setOnClickListener(v -> {
                dialog.dismiss();
                showPaymentDialog(booking);
            });
            
            // Submit Review button click listener
            btnSubmitReview.setOnClickListener(v -> {
                submitReview(booking, ratingBar.getRating(), etReviewMessage.getText().toString());
                Toast.makeText(getContext(), "Review submitted successfully!", Toast.LENGTH_SHORT).show();
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error showing booking details", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSupportTicketDialog(Booking booking) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_support_ticket, null);
            builder.setView(dialogView);

            // Initialize views
            ImageButton btnCloseSupport = dialogView.findViewById(R.id.btnCloseSupport);
            MaterialButton btnCancelSupport = dialogView.findViewById(R.id.btnCancelSupport);
            MaterialButton btnSendRequest = dialogView.findViewById(R.id.btnSendRequest);
            EditText etIssueDescription = dialogView.findViewById(R.id.etIssueDescription);

            AlertDialog dialog = builder.create();
            dialog.show();

            // Close button click listener
            btnCloseSupport.setOnClickListener(v -> dialog.dismiss());
            btnCancelSupport.setOnClickListener(v -> dialog.dismiss());

            // Send Request button click listener
            btnSendRequest.setOnClickListener(v -> {
                String issueDescription = etIssueDescription.getText().toString().trim();
                if (issueDescription.isEmpty()) {
                    Toast.makeText(getContext(), "Please describe your issue", Toast.LENGTH_SHORT).show();
                    return;
                }

                // TODO: Send support request to server
                // For now, just show success message
                dialog.dismiss();
                Toast.makeText(getContext(), "Support request sent successfully!", Toast.LENGTH_SHORT).show();
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error showing support ticket dialog", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPaymentDialog(Booking booking) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_payment, null);
            builder.setView(dialogView);

            // Initialize views
            MaterialButton btnDone = dialogView.findViewById(R.id.btnDone);

            AlertDialog dialog = builder.create();
            dialog.show();

            // Done button click listener
            btnDone.setOnClickListener(v -> {
                // TODO: Process payment
                // For now, just show success message
                dialog.dismiss();
                Toast.makeText(getContext(), "Payment processed successfully!", Toast.LENGTH_SHORT).show();
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error showing payment dialog", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitReview(Booking booking, float rating, String reviewMessage) {
        try {
            // TODO: Submit review to server
            // For now, just log the review data
            System.out.println("Review submitted for booking: " + booking.getBoardingHouseName());
            System.out.println("Rating: " + rating + " stars");
            System.out.println("Review: " + reviewMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Booking data class
    public static class Booking {
        private int bookingId;
        private String boardingHouseName;
        private String imagePath;
        private String location;
        private String startDate;
        private String endDate;
        private String monthlyDue;
        private String balanceDue;
        private String status;

        public Booking(int bookingId, String boardingHouseName, String imagePath, String location,
                      String startDate, String endDate, String monthlyDue, String balanceDue, String status) {
            this.bookingId = bookingId;
            this.boardingHouseName = boardingHouseName;
            this.imagePath = imagePath;
            this.location = location;
            this.startDate = startDate;
            this.endDate = endDate;
            this.monthlyDue = monthlyDue;
            this.balanceDue = balanceDue;
            this.status = status;
        }

        // Getters
        public int getBookingId() { return bookingId; }
        public String getBoardingHouseName() { return boardingHouseName; }
        public String getImagePath() { return imagePath; }
        public String getLocation() { return location; }
        public String getStartDate() { return startDate; }
        public String getEndDate() { return endDate; }
        public String getMonthlyDue() { return monthlyDue; }
        public String getBalanceDue() { return balanceDue; }
        public String getStatus() { return status; }
    }
}
