package com.example.mock;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnSendReset;
    private TextView tvBackToLogin;
    private ImageView backButton;
    private ProgressDialog progressDialog;
    private RequestQueue requestQueue;
    
    // Server URL for password reset
    private static final String FORGOT_PASSWORD_URL = "http://192.168.1.3/boardease_v3/forgot_password.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        // Initialize views
        initializeViews();
        
        // Initialize Volley RequestQueue
        requestQueue = Volley.newRequestQueue(this);
        
        // Set click listeners
        setClickListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    
    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        btnSendReset = findViewById(R.id.btnSendReset);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        backButton = findViewById(R.id.backButton);
        
        // Set up back button click listener
        backButton.setOnClickListener(v -> {
            // Navigate back to Login
            Intent intent = new Intent(ForgotPasswordActivity.this, Login.class);
            startActivity(intent);
            finish();
        });
        
        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sending reset email...");
        progressDialog.setCancelable(false);
    }
    
    private void setClickListeners() {
        btnSendReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendPasswordReset();
            }
        });
        
        tvBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to Login
                Intent intent = new Intent(ForgotPasswordActivity.this, Login.class);
                startActivity(intent);
                finish();
            }
        });
    }
    
    private void sendPasswordReset() {
        // Get input values
        String email = etEmail.getText().toString().trim();
        
        // Validate input
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return;
        }
        
        // Show progress dialog
        progressDialog.show();
        
        // Log the reset attempt
        Log.d("ForgotPassword", "Attempting password reset for email: " + email);
        Log.d("ForgotPassword", "Reset URL: " + FORGOT_PASSWORD_URL);
        
        // Create password reset request
        StringRequest stringRequest = new StringRequest(Request.Method.POST, FORGOT_PASSWORD_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressDialog.dismiss();
                        Log.d("ForgotPasswordResponse", "Server response: " + response);
                        handleResetResponse(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.dismiss();
                        Log.e("ForgotPasswordError", "Volley error: " + error.getMessage());
                        Log.e("ForgotPasswordError", "Error details: " + error.toString());
                        
                        String errorMessage = "Network error: ";
                        if (error.getMessage() != null) {
                            errorMessage += error.getMessage();
                        } else if (error.networkResponse != null) {
                            errorMessage += "HTTP " + error.networkResponse.statusCode;
                            if (error.networkResponse.data != null) {
                                String responseBody = new String(error.networkResponse.data);
                                Log.e("ForgotPasswordError", "Response body: " + responseBody);
                            }
                        } else {
                            errorMessage += "Unknown network error";
                        }
                        
                        Toast.makeText(ForgotPasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("email", email);
                return params;
            }
        };
        
        // Add request to queue
        requestQueue.add(stringRequest);
    }
    
    private void handleResetResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            boolean success = jsonObject.getBoolean("success");
            String message = jsonObject.getString("message");
            
            if (success) {
                // Reset email sent successfully
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                
                // Navigate back to login
                Intent intent = new Intent(ForgotPasswordActivity.this, Login.class);
                startActivity(intent);
                finish();
                
            } else {
                // Reset failed
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
            
        } catch (JSONException e) {
            Log.e("ForgotPasswordError", "JSON parsing error: " + e.getMessage());
            Toast.makeText(this, "Server response error. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}
