package com.example.mock;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PaymentCalendarActivity extends AppCompatActivity {

    private GridView gridViewCalendar;
    private TextView tvMonthYear;
    private ImageButton btnPrevMonth, btnNextMonth, btnRefresh;
    private ImageView ivBack;
    private CardView cardSelectedDate, cardCalendar;
    private TextView tvSelectedDate, tvStatusSummary;
    private RecyclerView recyclerViewPayments;
    private android.widget.Button btnSendReminders;
    
    private PaymentApiService paymentApiService;
    private ProgressDialog progressDialog;
    private int ownerId;
    private Calendar currentCalendar;
    private Map<String, CalendarPaymentData> paymentsByDate;
    private List<CalendarPaymentData.PaymentItem> selectedDatePayments;
    private PaymentCalendarAdapter adapter;
    private CalendarAdapter calendarAdapter;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_calendar);

        // Get owner ID from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        String userIdString = sharedPreferences.getString("user_id", "1");
        try {
            ownerId = Integer.parseInt(userIdString);
        } catch (NumberFormatException e) {
            ownerId = 1;
        }

        // Initialize API service and data structures first
        paymentApiService = new PaymentApiService(this);
        paymentsByDate = new HashMap<>();
        selectedDatePayments = new ArrayList<>();
        currentCalendar = Calendar.getInstance();
        
        initializeViews();
        setupClickListeners();
        
        // Setup calendar adapter
        calendarAdapter = new CalendarAdapter();
        gridViewCalendar.setAdapter(calendarAdapter);
        
        // Set GridView height to show all rows (6 weeks)
        gridViewCalendar.post(() -> {
            int cellHeight = (int) (50 * getResources().getDisplayMetrics().density);
            int totalHeight = cellHeight * 6; // 6 weeks
            ViewGroup.LayoutParams params = gridViewCalendar.getLayoutParams();
            params.height = totalHeight;
            gridViewCalendar.setLayoutParams(params);
        });
        
        // Setup swipe gesture detector
        setupSwipeGestures();
        
        // Load calendar data
        loadCalendarData();
    }

    private void initializeViews() {
        gridViewCalendar = findViewById(R.id.gridViewCalendar);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        btnRefresh = findViewById(R.id.btnRefresh);
        ivBack = findViewById(R.id.ivBack);
        cardSelectedDate = findViewById(R.id.cardSelectedDate);
        cardCalendar = findViewById(R.id.cardCalendar);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvStatusSummary = findViewById(R.id.tvStatusSummary);
        recyclerViewPayments = findViewById(R.id.recyclerViewPayments);
        btnSendReminders = findViewById(R.id.btnSendReminders);
        
        recyclerViewPayments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PaymentCalendarAdapter(selectedDatePayments);
        recyclerViewPayments.setAdapter(adapter);
        
        updateMonthYearDisplay();
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
        
        btnRefresh.setOnClickListener(v -> loadCalendarData());
        
        btnPrevMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateMonthYearDisplay();
            // Clear selected date display when changing month
            clearSelectedDateDisplay();
            loadCalendarData();
        });
        
        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateMonthYearDisplay();
            // Clear selected date display when changing month
            clearSelectedDateDisplay();
            loadCalendarData();
        });
        
        gridViewCalendar.setOnItemClickListener((parent, view, position, id) -> {
            CalendarDay day = calendarAdapter.getItem(position);
            if (day != null && day.isCurrentMonth) {
                // Check if the clicked date is in the current displayed month
                Calendar clickedMonth = (Calendar) day.calendar.clone();
                clickedMonth.set(Calendar.DAY_OF_MONTH, 1);
                Calendar currentMonth = (Calendar) currentCalendar.clone();
                currentMonth.set(Calendar.DAY_OF_MONTH, 1);
                
                // Only show if it's in the current month
                if (clickedMonth.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR) &&
                    clickedMonth.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH)) {
                    String dateKey = formatDateKey(day.calendar);
                    showPaymentsForDate(dateKey, day.calendar);
                }
            }
        });
    }

    private void updateMonthYearDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdf.format(currentCalendar.getTime()));
    }

    private void loadCalendarData() {
        showProgressDialog("Loading payment calendar...");
        
        // Check if notifications were already sent today
        SharedPreferences prefs = getSharedPreferences("PaymentCalendar", MODE_PRIVATE);
        String lastNotificationDate = prefs.getString("last_notification_date", "");
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        boolean notificationsAlreadySentToday = today.equals(lastNotificationDate);
        
        // First, auto-mark overdue payments
        paymentApiService.autoMarkOverdue(new PaymentApiService.OverdueUpdateCallback() {
            @Override
            public void onSuccess(String message, int totalUpdated, int notificationsSent, int notificationsSkipped) {
                // Debug logging
                android.util.Log.d("PaymentCalendar", "onSuccess - notificationsSent: " + notificationsSent + ", notificationsSkipped: " + notificationsSkipped + ", alreadySentToday: " + notificationsAlreadySentToday);
                
                // Show toast message based on notification status
                if (notificationsSent > 0) {
                    // Notifications were sent (first time opening today)
                    Toast.makeText(PaymentCalendarActivity.this, 
                        "Sent " + notificationsSent + " overdue payment notification(s)", 
                        Toast.LENGTH_SHORT).show();
                    
                    // Save today's date to track that notifications were sent
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("last_notification_date", today);
                    editor.apply();
                    
                    android.util.Log.d("PaymentCalendar", "Toast shown: Sent " + notificationsSent + " notifications");
                } else if (notificationsSkipped > 0) {
                    // Notifications were already sent today (opening calendar again)
                    // Check if this is the first time we detected skipped notifications today
                    if (notificationsAlreadySentToday) {
                        // Already showed toast today, don't show again
                        android.util.Log.d("PaymentCalendar", "Notifications skipped but toast already shown today");
                    } else {
                        // First time detecting skipped notifications today
                        Toast.makeText(PaymentCalendarActivity.this, 
                            "Already sent notification(s) today", 
                            Toast.LENGTH_SHORT).show();
                        
                        // Save today's date since notifications were already sent (even though skipped)
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("last_notification_date", today);
                        editor.apply();
                        
                        android.util.Log.d("PaymentCalendar", "Toast shown: Already sent (skipped)");
                    }
                }
                
                if (totalUpdated > 0) {
                    // Log silently - don't interrupt user experience
                    android.util.Log.d("PaymentCalendar", message + " - " + totalUpdated + " payments updated");
                }
                // Continue loading calendar data
                loadCalendarDataInternal();
            }

            @Override
            public void onError(String error) {
                // Continue loading even if overdue update fails
                Log.e("PaymentCalendar", "Error updating overdue: " + error);
                loadCalendarDataInternal();
            }
        });
    }

    private void loadCalendarDataInternal() {
        int month = currentCalendar.get(Calendar.MONTH) + 1; // Calendar.MONTH is 0-based
        int year = currentCalendar.get(Calendar.YEAR);
        
        paymentApiService.getPaymentsCalendar(ownerId, month, year, new PaymentApiService.PaymentCalendarCallback() {
            @Override
            public void onSuccess(List<CalendarPaymentData> calendarData) {
                hideProgressDialog();
                paymentsByDate.clear();
                
                // Store payments by date
                for (CalendarPaymentData data : calendarData) {
                    paymentsByDate.put(data.getDate(), data);
                }
                
                // Update calendar
                calendarAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                hideProgressDialog();
                Toast.makeText(PaymentCalendarActivity.this, "Error loading calendar: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private int getStatusColor(String status) {
        // All due dates are red
        return Color.parseColor("#F44336"); // Red for all due dates
    }

    private void showPaymentsForDate(String dateKey, Calendar calendar) {
        CalendarPaymentData data = paymentsByDate.get(dateKey);
        
        if (data != null && !data.getPayments().isEmpty()) {
            // Clear and update payments list
            selectedDatePayments.clear();
            selectedDatePayments.addAll(data.getPayments());
            
            // Update adapter
            if (adapter == null) {
                adapter = new PaymentCalendarAdapter(selectedDatePayments);
                recyclerViewPayments.setAdapter(adapter);
            } else {
                adapter.updatePayments(selectedDatePayments);
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            String dateStr = sdf.format(calendar.getTime());
            
            // Build boarder list summary
            Set<String> boarderNames = new HashSet<>();
            
            for (CalendarPaymentData.PaymentItem payment : data.getPayments()) {
                boarderNames.add(payment.getBoarderName());
            }
            
            // Set date title only
            tvSelectedDate.setText(dateStr);
            
            // Hide status summary - only showing due dates
            tvStatusSummary.setVisibility(View.GONE);
            
            // Make sure RecyclerView is visible
            recyclerViewPayments.setVisibility(View.VISIBLE);
            cardSelectedDate.setVisibility(View.VISIBLE);
            
            // Setup send reminders button
            if (btnSendReminders != null) {
                btnSendReminders.setOnClickListener(v -> showSendRemindersDialog(data.getPayments(), dateStr));
            }
            
            android.util.Log.d("PaymentCalendar", "Showing " + selectedDatePayments.size() + " payments for date: " + dateKey);
        } else {
            cardSelectedDate.setVisibility(View.GONE);
            Toast.makeText(this, "No payments for this date", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatDateKey(Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    private void clearSelectedDateDisplay() {
        // Clear selected date display when month changes
        cardSelectedDate.setVisibility(View.GONE);
        selectedDatePayments.clear();
        if (adapter != null) {
            adapter.updatePayments(selectedDatePayments);
        }
    }

    private void showSendRemindersDialog(List<CalendarPaymentData.PaymentItem> payments, String dateStr) {
        if (payments == null || payments.isEmpty()) {
            Toast.makeText(this, "No payments to send reminders for", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load Poppins font from resources first
        android.graphics.Typeface poppinsBold;
        android.graphics.Typeface poppinsMedium;
        android.graphics.Typeface poppinsRegular;
        
        try {
            poppinsBold = getResources().getFont(R.font.poppins_bold);
            poppinsMedium = getResources().getFont(R.font.poppins_medium);
            poppinsRegular = getResources().getFont(R.font.poppins_regular);
        } catch (Exception e) {
            // Fallback to default if font loading fails
            poppinsBold = android.graphics.Typeface.DEFAULT_BOLD;
            poppinsMedium = android.graphics.Typeface.DEFAULT;
            poppinsRegular = android.graphics.Typeface.DEFAULT;
        }

        // Use Material Design AlertDialog with custom theme
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        
        // Create custom title view with center alignment
        TextView titleView = new TextView(this);
        titleView.setText("Send Payment Reminders");
        titleView.setTextSize(18);
        titleView.setTextColor(Color.parseColor("#FFFFFF"));
        titleView.setTypeface(poppinsBold);
        titleView.setPadding(24, 24, 24, 16);
        titleView.setGravity(android.view.Gravity.CENTER);
        builder.setCustomTitle(titleView);

        // Create a scrollable view for multiple boarders with default background
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.setPadding(0, 0, 0, 0);
        
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(24, 20, 24, 20);

        // Add date info with consistent styling and Poppins font
        TextView tvDateInfo = new TextView(this);
        tvDateInfo.setText("Due Date: " + dateStr);
        tvDateInfo.setTextSize(16);
        tvDateInfo.setTextColor(Color.parseColor("#FFFFFF"));
        tvDateInfo.setTypeface(poppinsBold);
        tvDateInfo.setPadding(0, 0, 0, 20);
        layout.addView(tvDateInfo);

        // Add divider
        View divider = new View(this);
        divider.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 2));
        divider.setBackgroundColor(Color.parseColor("#E0E0E0"));
        layout.addView(divider);
        
        // Add spacing
        View spacing = new View(this);
        spacing.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 16));
        layout.addView(spacing);

        // Add boarder details with consistent styling
        for (int i = 0; i < payments.size(); i++) {
            CalendarPaymentData.PaymentItem payment = payments.get(i);
            
            // Create card container with default background
            androidx.cardview.widget.CardView cardView = new androidx.cardview.widget.CardView(this);
            cardView.setCardElevation(2);
            cardView.setRadius(8);
            cardView.setUseCompatPadding(true);
            
            android.widget.LinearLayout.LayoutParams cardParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, 16);
            cardView.setLayoutParams(cardParams);
            
            android.widget.LinearLayout boarderLayout = new android.widget.LinearLayout(this);
            boarderLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
            boarderLayout.setPadding(16, 16, 16, 16);

            // Boarder name with Poppins font
            TextView tvName = new TextView(this);
            tvName.setText(payment.getBoarderName());
            tvName.setTextSize(16);
            tvName.setTextColor(Color.parseColor("#FFFFFF"));
            tvName.setTypeface(poppinsBold);
            tvName.setPadding(0, 0, 0, 8);
            boarderLayout.addView(tvName);

            // Room number with Poppins font
            TextView tvRoom = new TextView(this);
            tvRoom.setText("Room: " + payment.getRoomNumber());
            tvRoom.setTextSize(14);
            tvRoom.setTextColor(Color.parseColor("#FFFFFF"));
            tvRoom.setTypeface(poppinsRegular);
            tvRoom.setPadding(0, 0, 0, 6);
            boarderLayout.addView(tvRoom);

            // Amount with Poppins font
            TextView tvAmount = new TextView(this);
            tvAmount.setText("Amount: ₱" + String.format(Locale.getDefault(), "%.2f", payment.getAmount()));
            tvAmount.setTextSize(14);
            tvAmount.setTextColor(Color.parseColor("#FFFFFF"));
            tvAmount.setTypeface(poppinsRegular);
            tvAmount.setPadding(0, 0, 0, 6);
            boarderLayout.addView(tvAmount);

            // Due date - distinguish between due (not overdue) and overdue
            String dueDateStr = payment.getDueDate();
            if (dueDateStr != null && !dueDateStr.isEmpty()) {
                try {
                    java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    String datePart = dueDateStr.split(" ")[0];
                    java.util.Date dueDate = inputFormat.parse(datePart);
                    
                    // Get today's date (without time)
                    java.util.Calendar todayCal = java.util.Calendar.getInstance();
                    todayCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                    todayCal.set(java.util.Calendar.MINUTE, 0);
                    todayCal.set(java.util.Calendar.SECOND, 0);
                    todayCal.set(java.util.Calendar.MILLISECOND, 0);
                    java.util.Date today = todayCal.getTime();
                    
                    // Get due date without time
                    java.util.Calendar dueDateCal = java.util.Calendar.getInstance();
                    dueDateCal.setTime(dueDate);
                    dueDateCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                    dueDateCal.set(java.util.Calendar.MINUTE, 0);
                    dueDateCal.set(java.util.Calendar.SECOND, 0);
                    dueDateCal.set(java.util.Calendar.MILLISECOND, 0);
                    java.util.Date dueDateOnly = dueDateCal.getTime();
                    
                    // Calculate days difference
                    long diffInMillis = today.getTime() - dueDateOnly.getTime();
                    int daysDiff = (int) (diffInMillis / (1000 * 60 * 60 * 24));
                    
                    TextView tvDueDate = new TextView(this);
                    String paymentStatus = payment.getStatus() != null ? payment.getStatus().toLowerCase() : "";
                    
                    // Check if payment is overdue (past due date OR status is "Overdue")
                    boolean isOverdue = daysDiff > 0 || paymentStatus.equals("overdue");
                    
                    if (isOverdue) {
                        // Overdue - show days overdue with Poppins font
                        tvDueDate.setText("Due: " + outputFormat.format(dueDate) + " (" + daysDiff + " day" + (daysDiff > 1 ? "s" : "") + " overdue)");
                        tvDueDate.setTextColor(Color.parseColor("#F44336")); // Red for overdue
                        tvDueDate.setTypeface(poppinsBold);
                    } else {
                        // Not overdue yet - just show due date with Poppins font
                        tvDueDate.setText("Due on: " + outputFormat.format(dueDate));
                        tvDueDate.setTextColor(Color.parseColor("#2196F3")); // Blue for due
                        tvDueDate.setTypeface(poppinsRegular);
                    }
                    tvDueDate.setTextSize(14);
                    boarderLayout.addView(tvDueDate);
                } catch (Exception e) {
                    // If parsing fails, just show the date as is with Poppins font
                    TextView tvDueDate = new TextView(this);
                    tvDueDate.setText("Due on: " + dueDateStr);
                    tvDueDate.setTextSize(14);
                    tvDueDate.setTextColor(Color.parseColor("#2196F3")); // Blue for due
                    tvDueDate.setTypeface(poppinsRegular);
                    boarderLayout.addView(tvDueDate);
                }
            }

            cardView.addView(boarderLayout);
            layout.addView(cardView);
        }

        scrollView.addView(layout);
        builder.setView(scrollView);

        builder.setPositiveButton("Send Reminders", (dialog, which) -> {
            sendRemindersToAll(payments);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        
        // Style the buttons with Poppins font
        try {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#2196F3"));
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextSize(14);
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTypeface(poppinsBold);
            
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#757575"));
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextSize(14);
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTypeface(poppinsMedium);
        } catch (Exception e) {
            // Fallback if font loading fails
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#2196F3"));
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextSize(14);
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTypeface(null, android.graphics.Typeface.BOLD);
            
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#757575"));
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextSize(14);
        }
    }

    private void sendRemindersToAll(List<CalendarPaymentData.PaymentItem> payments) {
        if (payments == null || payments.isEmpty()) {
            return;
        }

        showProgressDialog("Sending reminders to " + payments.size() + " boarder(s)...");

        final int[] successCount = {0};
        final int[] errorCount = {0};
        final int[] totalCount = {payments.size()};

        for (CalendarPaymentData.PaymentItem payment : payments) {
            // Get payment ID (use breakdown_id or payment_id)
            int paymentId = payment.getBreakdownId() != null ? payment.getBreakdownId() : 
                           (payment.getPaymentId() != null ? payment.getPaymentId() : 0);
            
            if (paymentId == 0) {
                // If no payment_id, use booking_id as fallback
                paymentId = payment.getBookingId();
            }

            // Get boarder user ID
            int boarderUserId = payment.getBoarderUserId();
            
            if (boarderUserId == 0) {
                errorCount[0]++;
                checkAllRemindersSent(successCount[0], errorCount[0], totalCount[0]);
                continue;
            }
            
            // Call API to send reminder
            paymentApiService.sendPaymentReminder(paymentId, boarderUserId, new PaymentApiService.PaymentReminderCallback() {
                @Override
                public void onSuccess(String message) {
                    successCount[0]++;
                    checkAllRemindersSent(successCount[0], errorCount[0], totalCount[0]);
                }

                @Override
                public void onError(String error) {
                    errorCount[0]++;
                    checkAllRemindersSent(successCount[0], errorCount[0], totalCount[0]);
                }
            });
        }
    }

    private void checkAllRemindersSent(int successCount, int errorCount, int totalCount) {
        if (successCount + errorCount >= totalCount) {
            hideProgressDialog();
            if (successCount > 0) {
                Toast.makeText(this, 
                    "Sent " + successCount + " reminder(s) successfully" + 
                    (errorCount > 0 ? " (" + errorCount + " failed)" : ""), 
                    Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to send reminders", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupSwipeGestures() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();
                
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    // Horizontal swipe
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // Swipe right - previous month
                            btnPrevMonth.performClick();
                        } else {
                            // Swipe left - next month
                            btnNextMonth.performClick();
                        }
                        return true;
                    }
                }
                return false;
            }
        });

        // Add touch listener to calendar card view
        if (cardCalendar != null) {
            cardCalendar.setOnTouchListener((v, event) -> {
                gestureDetector.onTouchEvent(event);
                return false; // Allow normal touch events to pass through
            });
        }
        
        // Also add to GridView for better coverage
        gridViewCalendar.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false; // Allow normal touch events to pass through
        });
    }

    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    // Calendar Day class
    private static class CalendarDay {
        Calendar calendar;
        boolean isCurrentMonth;
        String status;

        CalendarDay(Calendar calendar, boolean isCurrentMonth, String status) {
            this.calendar = calendar;
            this.isCurrentMonth = isCurrentMonth;
            this.status = status;
        }
    }

    // Calendar Adapter
    private class CalendarAdapter extends BaseAdapter {
        private List<CalendarDay> days;

        CalendarAdapter() {
            days = new ArrayList<>();
            updateDays();
        }

        private void updateDays() {
            days.clear();
            
            Calendar calendar = (Calendar) currentCalendar.clone();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            
            int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            
            // Add empty cells for days before the first day of the month
            for (int i = 1; i < firstDayOfWeek; i++) {
                days.add(null);
            }
            
            // Add days of the month
            for (int i = 1; i <= daysInMonth; i++) {
                Calendar dayCalendar = (Calendar) calendar.clone();
                dayCalendar.set(Calendar.DAY_OF_MONTH, i);
                
                String dateKey = formatDateKey(dayCalendar);
                CalendarPaymentData data = paymentsByDate.get(dateKey);
                String status = data != null ? data.getDominantStatus() : null;
                
                days.add(new CalendarDay(dayCalendar, true, status));
            }
            
            // Fill remaining cells to make 42 total (6 weeks * 7 days)
            while (days.size() < 42) {
                days.add(null);
            }
        }

        @Override
        public int getCount() {
            return days.size();
        }

        @Override
        public CalendarDay getItem(int position) {
            return days.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView;
            
            if (convertView == null) {
                textView = new TextView(PaymentCalendarActivity.this);
                int cellHeight = (int) (50 * getResources().getDisplayMetrics().density);
                textView.setLayoutParams(new GridView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    cellHeight));
                textView.setGravity(android.view.Gravity.CENTER);
                textView.setTextSize(14);
                textView.setPadding(8, 8, 8, 8);
                textView.setMinHeight(cellHeight);
            } else {
                textView = (TextView) convertView;
            }
            
            CalendarDay day = getItem(position);
            
            if (day == null || !day.isCurrentMonth) {
                textView.setText("");
                textView.setBackgroundColor(Color.TRANSPARENT);
                textView.setTextColor(Color.TRANSPARENT);
            } else {
                textView.setText(String.valueOf(day.calendar.get(Calendar.DAY_OF_MONTH)));
                
                // Set background color based on payment status
                if (day.status != null) {
                    int color = getStatusColor(day.status);
                    textView.setBackgroundColor(color);
                    textView.setTextColor(Color.WHITE);
                } else {
                    textView.setBackgroundColor(Color.TRANSPARENT);
                    textView.setTextColor(Color.BLACK);
                }
                
                // Highlight today
                Calendar today = Calendar.getInstance();
                if (day.calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    day.calendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    day.calendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)) {
                    textView.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
                }
            }
            
            return textView;
        }

        @Override
        public void notifyDataSetChanged() {
            updateDays();
            super.notifyDataSetChanged();
        }
    }
}
