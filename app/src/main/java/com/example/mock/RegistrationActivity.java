package com.example.mock;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import android.app.DatePickerDialog;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class RegistrationActivity extends AppCompatActivity {

    Spinner spinnerRole;

    EditText etFirstName, etLastName, etMiddleName, etBirthDate, etPhone, etAddress, etEmail, etPassword, etGcashNum;
    TextView tvLogin;
    ImageView UploadQr, ivTogglePassword;
    Button btnNext;
    boolean isPasswordVisible = false;

    private Uri selectedQrUri; // store the selected image URI

    // Launcher to pick image from gallery
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            selectedQrUri = uri;
                            UploadQr.setImageURI(uri); // show the image in ImageView
                        }
                    });

    @SuppressLint({"WrongViewCast", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        btnNext = findViewById(R.id.btnNext);

        spinnerRole = findViewById(R.id.spinnerRole);

        etBirthDate = findViewById(R.id.etBirthDate);
        etFirstName = findViewById(R.id.etFirstName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etLastName = findViewById(R.id.etLastName);
        etBirthDate = findViewById(R.id.etBirthDate);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etGcashNum = findViewById(R.id.etGcashNum);

        tvLogin = findViewById(R.id.tvLogin);

        UploadQr = findViewById(R.id.UploadQr);

        // Create a list of choices
        String[] roles = {"Select --", "Boarder", "BH Owner"};

        // Set adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                roles
        );
        spinnerRole.setAdapter(adapter);

        //Set the calendar for the birthdate
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

        String role = spinnerRole.getSelectedItem().toString();

        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String middleName = etMiddleName.getText().toString().trim();
        String birthDate = etBirthDate.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String gcashNum = etGcashNum.getText().toString().trim();

        // When ImageView is clicked → open gallery
        UploadQr.setOnClickListener(v -> {
            pickImageLauncher.launch("image/*"); // open only image files
        });

        //Button next for proceeding to the next Activity
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(etFirstName.getText().toString().isEmpty() || etLastName.getText().toString().isEmpty() ||
                        etBirthDate.getText().toString().isEmpty() || etPhone.getText().toString().isEmpty() ||
                        etAddress.getText().toString().isEmpty() || etEmail.getText().toString().isEmpty() ||
                        etPassword.getText().toString().isEmpty() || etGcashNum.getText().toString().isEmpty()) {

                    Toast.makeText(RegistrationActivity.this, "Please input all fields.", Toast.LENGTH_SHORT).show();

                } else if (UploadQr.getDrawable() == null) {
                    Toast.makeText(RegistrationActivity.this, "No image chosen for QR", Toast.LENGTH_SHORT).show();

                } else {
                    // Pass all values to Registration2Activity
                    Intent a = new Intent(RegistrationActivity.this, Registration2Activity.class);

                    a.putExtra("role", spinnerRole.getSelectedItem().toString());
                    a.putExtra("firstName", etFirstName.getText().toString().trim());
                    a.putExtra("middleName", etMiddleName.getText().toString().trim());
                    a.putExtra("lastName", etLastName.getText().toString().trim());
                    a.putExtra("birthDate", etBirthDate.getText().toString().trim());
                    a.putExtra("phone", etPhone.getText().toString().trim());
                    a.putExtra("address", etAddress.getText().toString().trim());
                    a.putExtra("email", etEmail.getText().toString().trim());
                    a.putExtra("password", etPassword.getText().toString().trim());
                    a.putExtra("gcashNum", etGcashNum.getText().toString().trim());

                    // if you want to send QR URI
                    if (selectedQrUri != null) {
                        a.putExtra("qrUri", selectedQrUri.toString());
                    }

                    startActivity(a);
                }
            }
        });

        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent a = new Intent(RegistrationActivity.this, Login.class);
                startActivity(a);
            }
        });
    }
}
