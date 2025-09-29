package com.example.mock;

import android.app.AlertDialog;
import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class BookingConfirmationActivity extends AppCompatActivity {

    private ImageView ivProof, ivProofCash;
    private ActivityResultLauncher<String> imagePickerLauncher;

    private RadioGroup rgPayment;
    private RadioButton rbCash, rbGcash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirmation);

        Button btnBookNow = findViewById(R.id.btnBookNow);
        rgPayment = findViewById(R.id.rgPayment);
        rbCash = findViewById(R.id.rbCash);
        rbGcash = findViewById(R.id.rbGcash);

        // Setup image picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::onImageSelected
        );

        btnBookNow.setOnClickListener(v -> {
            int selectedId = rgPayment.getCheckedRadioButtonId();
            if (selectedId == rbCash.getId()) {
                showCashDialog();
            } else if (selectedId == rbGcash.getId()) {
                showGcashDialog();
            }
        });
    }

    // For Gcash option
    private void showGcashDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(R.layout.dialog_payment);
        Dialog dialog = builder.create();
        dialog.show();

        ivProof = dialog.findViewById(R.id.ivProof);
        Button btnDone = dialog.findViewById(R.id.btnDone);

        if (ivProof != null) {
            ivProof.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        }

        if (btnDone != null) {
            btnDone.setOnClickListener(v -> {
                dialog.dismiss();
                showConfirmationDialog();
            });
        }
    }

    // For Cash option
    private void showCashDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(R.layout.dialog_cash_payment);
        Dialog dialog = builder.create();
        dialog.show();

        ivProofCash = dialog.findViewById(R.id.ivProofCash);
        Button btnOkCash = dialog.findViewById(R.id.btnOkCash);

        if (ivProofCash != null) {
            ivProofCash.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        }

        if (btnOkCash != null) {
            btnOkCash.setOnClickListener(v -> {
                dialog.dismiss();
                showConfirmationDialog();
            });
        }
    }

    // Handle uploaded image
    private void onImageSelected(Uri uri) {
        if (ivProof != null && uri != null) {
            ivProof.setImageURI(uri);
        }
        if (ivProofCash != null && uri != null) {
            ivProofCash.setImageURI(uri);
        }
    }

    // Confirmation dialog
    private void showConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(R.layout.dialog_confirmation);
        builder.setPositiveButton("OK", (d, which) -> d.dismiss());
        builder.create().show();
    }
}
