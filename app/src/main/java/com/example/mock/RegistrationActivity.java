package com.example.mock;

import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import android.app.DatePickerDialog;
import android.widget.DatePicker;
import java.util.Calendar;

import androidx.appcompat.app.AppCompatActivity;

public class RegistrationActivity extends AppCompatActivity {

    Spinner spinnerRole;

    private EditText etPassword;
    private ImageView ivTogglePassword;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        Spinner spinnerRole = findViewById(R.id.spinnerRole);

        // Create a list of choices
        String[] roles = {"Select --", "Boarder", "BH Owner"};

        // Set adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                roles
        );
        spinnerRole.setAdapter(adapter);

        EditText etBirthDate = findViewById(R.id.etBirthDate);

        etBirthDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        RegistrationActivity.this,
                        (DatePicker view, int selectedYear, int selectedMonth, int selectedDay) -> {
                            // Format: MM/DD/YYYY
                            String date = (selectedMonth + 1) + "/" + selectedDay + "/" + selectedYear;
                            etBirthDate.setText(date);
                        },
                        year, month, day
                );

                // Optional: restrict future dates (no selecting birth date in future)
                datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

                datePickerDialog.show();
            }
        });
    }
}
