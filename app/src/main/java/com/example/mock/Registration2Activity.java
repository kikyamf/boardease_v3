package com.example.mock;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Registration2Activity extends AppCompatActivity {

    private Spinner spinnerVId;
    private ImageView ivUploadF, ivUploadB;
    private EditText etIdNumber;
    private CheckBox cbAgree;
    private Button btnReg;

    // Get data from first registration screen
    String role = getIntent().getStringExtra("role");
    String firstName = getIntent().getStringExtra("firstName");
    String middleName = getIntent().getStringExtra("middleName");
    String lastName = getIntent().getStringExtra("lastName");
    String birthDate = getIntent().getStringExtra("birthDate");
    String phone = getIntent().getStringExtra("phone");
    String address = getIntent().getStringExtra("address");
    String email = getIntent().getStringExtra("email");
    String password = getIntent().getStringExtra("password");
    String gcashNum = getIntent().getStringExtra("gcashNum");
    String qrUri = getIntent().getStringExtra("qrUri"); // optional


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration2);

        // Initialize Views
        spinnerVId = findViewById(R.id.spinnerVId);
        ivUploadF = findViewById(R.id.ivUploadF);
        ivUploadB = findViewById(R.id.ivUploadB);
        etIdNumber = findViewById(R.id.etidNumber);
        cbAgree = findViewById(R.id.cbAgree);
        btnReg = findViewById(R.id.btnReg);

        // Create a list of choices
        String[] roles = {
                "Select --",
                "Philippine Passport",
                "Driver's License",
                "PhilID (National ID)",
                "UMID",
                "SSS ID",
                "GSIS e-card",
                "PhilHealth ID",
                "TIN ID (BIR)",
                "Voter's ID",
                "Postal ID"
        };

        // Set adapter for Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                roles
        );
        spinnerVId.setAdapter(adapter);


        btnReg.setOnClickListener(v -> {
            // Get values
            String selectedIdType = spinnerVId.getSelectedItem().toString();
            String idNumber = etIdNumber.getText().toString().trim();
            boolean isAgreed = cbAgree.isChecked();

            // Validation example
            if (selectedIdType.equals("Select --")) {
                Toast.makeText(this, "Please select a valid ID type", Toast.LENGTH_SHORT).show();
                return;
            }

            if (idNumber.isEmpty()) {
                Toast.makeText(this, "Please enter your ID number", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isAgreed) {
                Toast.makeText(this, "You must agree to continue", Toast.LENGTH_SHORT).show();
                return;
            }

            // Now you can send these values to your database
        });


        // Insets handling
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
