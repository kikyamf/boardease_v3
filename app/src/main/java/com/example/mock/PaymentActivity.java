package com.example.mock;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class PaymentActivity extends AppCompatActivity {
    
    private static final int PICK_IMAGE_REQUEST = 1;
    
    private ImageButton btnBack;
    private ImageView imgQRCode, imgProofPreview;
    private MaterialButton btnUploadProof, btnDone;
    private TextView tvFileName;
    
    // Data from previous activity
    private int boardingHouseId;
    private String boardingHouseName;
    private String accommodationType;
    private String accommodationPrice;
    private long checkInDate;
    private long checkOutDate;
    
    private Uri proofImageUri;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        
        // Get data from intent
        getIntentData();
        
        // Initialize views
        initializeViews();
        
        // Setup click listeners
        setupClickListeners();
        
        // Load data
        loadData();
    }
    
    private void getIntentData() {
        Intent intent = getIntent();
        boardingHouseId = intent.getIntExtra("boarding_house_id", 0);
        boardingHouseName = intent.getStringExtra("boarding_house_name");
        accommodationType = intent.getStringExtra("accommodation_type");
        accommodationPrice = intent.getStringExtra("accommodation_price");
        checkInDate = intent.getLongExtra("check_in_date", 0);
        checkOutDate = intent.getLongExtra("check_out_date", 0);
    }
    
    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        imgQRCode = findViewById(R.id.imgQRCode);
        imgProofPreview = findViewById(R.id.imgProofPreview);
        btnUploadProof = findViewById(R.id.btnUploadProof);
        btnDone = findViewById(R.id.btnDone);
        tvFileName = findViewById(R.id.tvFileName);
    }
    
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnUploadProof.setOnClickListener(v -> {
            // Open image picker
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Payment Screenshot"), PICK_IMAGE_REQUEST);
        });
        
        btnDone.setOnClickListener(v -> {
            if (proofImageUri != null) {
                showPaymentConfirmationDialog();
            } else {
                // Show error - proof of payment required
                showAlertDialog("Proof Required", "Please upload a screenshot of your payment before proceeding.");
            }
        });
    }
    
    private void loadData() {
        // TODO: Load actual QR code from boarding house owner
        imgQRCode.setImageResource(R.drawable.sample_qr);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            proofImageUri = data.getData();
            
            // Show preview
            imgProofPreview.setImageURI(proofImageUri);
            imgProofPreview.setVisibility(View.VISIBLE);
            
            // Show file name
            String fileName = getFileName(proofImageUri);
            tvFileName.setText(fileName);
            tvFileName.setVisibility(View.VISIBLE);
            
            // Update upload button
            btnUploadProof.setText("Change Screenshot");
        }
    }
    
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result != null ? result : "payment_screenshot.jpg";
    }
    
    private void showPaymentConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Payment Submitted")
                .setMessage("A notification will be sent to you once payment is approved.")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Navigate back to main screen or booking list
                    finish();
                })
                .setCancelable(false)
                .show();
    }
    
    private void showAlertDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}

