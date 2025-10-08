package com.example.mock;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Registration2Activity extends AppCompatActivity {

    Spinner spinnerVId;
    ImageView ivUploadF, ivUploadB;
    EditText etIdNumber;
    CheckBox cbAgree;
    Button btnReg;
    Bitmap qrBitmap;
    Bitmap frontBitmap;
    Bitmap backBitmap;
    TextView tvLogin;


    // File paths for ID images
    private String idFrontPath = null;
    private String idBackPath = null;
    private Uri idFrontUri = null;
    private Uri idBackUri = null;

    // Get data from first registration screen
    String role, firstName, middleName, lastName, birthDate, phone, address, email, password, gcashNum, qrPath;
    
    // Permission request launcher
    private ActivityResultLauncher<String> requestPermissionLauncher;

    // Launchers for picking images
    private ActivityResultLauncher<String> pickFrontImageLauncher;
    private ActivityResultLauncher<String> pickBackImageLauncher;

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

        tvLogin = findViewById(R.id.tvLogin);

        // Retrieve data passed from RegistrationActivity
        role = getIntent().getStringExtra("role");
        firstName = getIntent().getStringExtra("firstName");
        middleName = getIntent().getStringExtra("middleName");
        lastName = getIntent().getStringExtra("lastName");
        birthDate = getIntent().getStringExtra("birthDate");
        phone = getIntent().getStringExtra("phone");
        address = getIntent().getStringExtra("address");
        email = getIntent().getStringExtra("email");
        password = getIntent().getStringExtra("password");
        gcashNum = getIntent().getStringExtra("gcashNum");
        qrPath = getIntent().getStringExtra("qrUri");

        // Initialize permission request launcher
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permission granted, you can proceed with image operations
                    } else {
                        Toast.makeText(this, "Permission denied. Cannot access images.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Check and request permissions
        checkAndRequestPermissions();

        // Prepare image pickers
        pickFrontImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        ivUploadF.setImageURI(uri); // show image
                        idFrontUri = uri; // save URI
                        idFrontPath = uri.toString(); // save URI path
                    }
                }
        );

        pickBackImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        ivUploadB.setImageURI(uri); // show image
                        idBackUri = uri; // save URI
                        idBackPath = uri.toString(); // save URI path
                    }
                }
        );

        // Open gallery when clicking the ImageViews
        ivUploadF.setOnClickListener(v -> pickFrontImageLauncher.launch("image/*"));
        ivUploadB.setOnClickListener(v -> pickBackImageLauncher.launch("image/*"));

        // Spinner choices
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

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                roles
        );
        spinnerVId.setAdapter(adapter);

        // Register button click
        btnReg.setOnClickListener(v -> {
            String selectedIdType = spinnerVId.getSelectedItem().toString();
            String idNumber = etIdNumber.getText().toString().trim();
            boolean isAgreed = cbAgree.isChecked();

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

            if (idFrontUri == null || idBackUri == null) {
                Toast.makeText(this, "Please upload front and back ID images", Toast.LENGTH_SHORT).show();
                return;
            }

            // Convert URIs to Bitmaps
            try {
                frontBitmap = getBitmapFromUri(idFrontUri);
                backBitmap = getBitmapFromUri(idBackUri);
                
                // For QR code, we need to get it from the first activity
                if (qrPath != null) {
                    Uri qrUri = Uri.parse(qrPath);
                    qrBitmap = getBitmapFromUri(qrUri);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error processing images: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            // Using your WiFi IP address: 192.168.1.3
            String UPLOAD_URL = "http://192.168.1.3/boardease2/insert_registration.php";

            // Inside btnReg.setOnClickListener
            VolleyMultipartRequest request = new VolleyMultipartRequest(Request.Method.POST, UPLOAD_URL,
                    response -> {
                        try {
                            String responseString = new String(response.data);
                            Log.d("RegistrationResponse", "Server response: " + responseString);
                            
                            JSONObject obj = new JSONObject(responseString);
                            String message = obj.getString("message");
                            boolean success = obj.getBoolean("success");
                            
                            if (success) {
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                                // Navigate to login after successful registration
                                Intent intent = new Intent(this, Login.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(this, "Registration failed: " + message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e("RegistrationError", "Response parsing error: " + e.getMessage());
                            e.printStackTrace();
                            Toast.makeText(this, "Server response error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        Log.e("RegistrationError", "Volley error: " + error.getMessage());
                        Log.e("RegistrationError", "Error details: " + error.toString());
                        
                        String errorMessage = "Upload failed: ";
                        if (error.getMessage() != null) {
                            errorMessage += error.getMessage();
                        } else if (error.networkResponse != null) {
                            errorMessage += "HTTP " + error.networkResponse.statusCode;
                        } else {
                            errorMessage += "Unknown network error";
                        }
                        
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("role", role);
                    params.put("firstName", firstName);
                    params.put("middleName", middleName);
                    params.put("lastName", lastName);
                    params.put("birthDate", birthDate);
                    params.put("phone", phone);
                    params.put("address", address);
                    params.put("email", email);
                    params.put("password", password);
                    params.put("gcashNum", gcashNum);
                    params.put("idType", selectedIdType);
                    params.put("idNumber", idNumber);
                    params.put("isAgreed", String.valueOf(isAgreed));
                    return params;
                }

                @Override
                protected Map<String, DataPart> getByteData() {
                    Map<String, DataPart> params = new HashMap<>();
                    params.put("qrFile", new DataPart("qr.jpg", AppHelper.getFileDataFromDrawable(getBaseContext(), qrBitmap)));
                    params.put("idFrontFile", new DataPart("front.jpg", AppHelper.getFileDataFromDrawable(getBaseContext(), frontBitmap)));
                    params.put("idBackFile", new DataPart("back.jpg", AppHelper.getFileDataFromDrawable(getBaseContext(), backBitmap)));
                    return params;
                }
            };
            Volley.newRequestQueue(this).add(request);

        });

        tvLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
        });

        // Insets handling
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void checkAndRequestPermissions() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(permission);
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                throw new IOException("Cannot open input stream for URI: " + uri);
            }
            
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            
            if (bitmap == null) {
                throw new IOException("Failed to decode bitmap from URI: " + uri);
            }
            
            return bitmap;
        } catch (Exception e) {
            throw new IOException("Error processing image: " + e.getMessage(), e);
        }
    }
}
