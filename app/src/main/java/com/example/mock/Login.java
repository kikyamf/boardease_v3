package com.example.mock;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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

public class Login extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private Button btnGuest;
    private TextView tvSignUp;
    private ImageButton btnTogglePassword;
    private ImageView backButton;
    private ProgressDialog progressDialog;
    private RequestQueue requestQueue;
    private boolean isPasswordVisible = false;
    
    // SharedPreferences for storing user session
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "UserSession";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_MIDDLE_NAME = "user_middle_name";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_ADDRESS = "user_address";
    private static final String KEY_USER_BIRTH_DATE = "user_birth_date";
    private static final String KEY_USER_GCASH_NUMBER = "user_gcash_number";
    private static final String KEY_USER_QR_CODE_PATH = "user_qr_code_path";
    private static final String KEY_USER_VALID_ID_TYPE = "user_valid_id_type";
    private static final String KEY_USER_ID_NUMBER = "user_id_number";
    private static final String KEY_USER_ID_FRONT_FILE = "user_id_front_file";
    private static final String KEY_USER_ID_BACK_FILE = "user_id_back_file";
    private static final String KEY_USER_STATUS = "user_status";
    
    // Server URL - Update this path if login.php is in a different location
    private static final String LOGIN_URL = "http://192.168.1.9/boardease_v3/login.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Initialize views
        initializeViews();
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Initialize Volley RequestQueue
        requestQueue = Volley.newRequestQueue(this);
        
        // Check if user is already logged in
        checkExistingSession();

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
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGuest = findViewById(R.id.btnGuest);
        tvSignUp = findViewById(R.id.tvSignUp);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        backButton = findViewById(R.id.backButton);
        
        // Set up back button click listener
        backButton.setOnClickListener(v -> {
            // Navigate back to WelcomeActivity
            Intent intent = new Intent(Login.this, WelcomeActivity.class);
            startActivity(intent);
            finish();
        });
        
        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in...");
        progressDialog.setCancelable(false);
    }
    
    private void checkExistingSession() {
        // Check if user is already logged in
        String userId = sharedPreferences.getString(KEY_USER_ID, null);
        String userRole = sharedPreferences.getString(KEY_USER_ROLE, null);
        
        if (userId != null && userRole != null) {
            // User is already logged in, navigate to appropriate dashboard
            navigateToDashboard(userRole);
        }
    }
    
    private void setClickListeners() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        tvSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Login.this, RegistrationActivity.class);
                startActivity(intent);
            }
        });

        btnTogglePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility();
            }
        });

        btnGuest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToGuestMode();
            }
        });
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide password
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            btnTogglePassword.setImageResource(R.drawable.ic_password_hidden);
            isPasswordVisible = false;
        } else {
            // Show password
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            btnTogglePassword.setImageResource(R.drawable.ic_password_visible);
            isPasswordVisible = true;
        }
        
        // Move cursor to end of text
        etPassword.setSelection(etPassword.getText().length());
    }

    private void navigateToGuestMode() {
        Intent intent = new Intent(Login.this, GuestHomeActivity.class);
        startActivity(intent);
    }
    
    private void performLogin() {
        // Get input values
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        // Validate input
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return;
        }
        
        // Show progress dialog
        progressDialog.show();
        
        // Log the login attempt
        Log.d("LoginAttempt", "Attempting login for email: " + email);
        Log.d("LoginAttempt", "Login URL: " + LOGIN_URL);
        
        // Create login request
        StringRequest stringRequest = new StringRequest(Request.Method.POST, LOGIN_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressDialog.dismiss();
                        Log.d("LoginResponse", "Server response: " + response);
                        handleLoginResponse(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.dismiss();
                        Log.e("LoginError", "Volley error: " + error.getMessage());
                        Log.e("LoginError", "Error details: " + error.toString());
                        
                        String errorMessage = "Network error: ";
                        if (error.getMessage() != null) {
                            errorMessage += error.getMessage();
                        } else if (error.networkResponse != null) {
                            errorMessage += "HTTP " + error.networkResponse.statusCode;
                            if (error.networkResponse.data != null) {
                                String responseBody = new String(error.networkResponse.data);
                                Log.e("LoginError", "Response body: " + responseBody);
                            }
                        } else {
                            errorMessage += "Unknown network error";
                        }
                        
                        Toast.makeText(Login.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("email", email);
                params.put("password", password);
                return params;
            }
        };
        
        // Add request to queue
        requestQueue.add(stringRequest);
    }
    
    private void handleLoginResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            boolean success = jsonObject.getBoolean("success");
            String message = jsonObject.getString("message");
            
            if (success) {
                // Login successful
                JSONObject userObject = jsonObject.getJSONObject("user");
                String userId = userObject.getString("id");
                String userRole = userObject.getString("role");
                String firstName = userObject.getString("firstName");
                String lastName = userObject.getString("lastName");
                String userEmail = userObject.getString("email");
                
                // Store additional user data
                String middleName = userObject.optString("middleName", "");
                String phone = userObject.optString("phone", "");
                String address = userObject.optString("address", "");
                String birthDate = userObject.optString("birthDate", "");
                String gcashNumber = userObject.optString("gcashNumber", "");
                String qrCodePath = userObject.optString("qrCodePath", "");
                String validIdType = userObject.optString("validIdType", "");
                String idNumber = userObject.optString("idNumber", "");
                String idFrontFile = userObject.optString("idFrontFile", "");
                String idBackFile = userObject.optString("idBackFile", "");
                String status = userObject.optString("status", "");
                
                // Store user session in SharedPreferences
                saveUserSession(userId, userRole, firstName + " " + lastName, userEmail, 
                              middleName, phone, address, birthDate, gcashNumber, qrCodePath,
                              validIdType, idNumber, idFrontFile, idBackFile, status);
                
                // Show success message
                Toast.makeText(this, "Welcome, " + firstName + "!", Toast.LENGTH_SHORT).show();
                
                // Navigate to appropriate dashboard based on role
                navigateToDashboard(userRole);
                
            } else {
                // Login failed
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
            
        } catch (JSONException e) {
            Log.e("LoginError", "JSON parsing error: " + e.getMessage());
            Toast.makeText(this, "Server response error. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveUserSession(String userId, String userRole, String userName, String userEmail, 
                               String middleName, String phone, String address, String birthDate, 
                               String gcashNumber, String qrCodePath, String validIdType, String idNumber,
                               String idFrontFile, String idBackFile, String status) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_ROLE, userRole);
        editor.putString(KEY_USER_NAME, userName);
        editor.putString(KEY_USER_EMAIL, userEmail);
        editor.putString(KEY_USER_MIDDLE_NAME, middleName);
        editor.putString(KEY_USER_PHONE, phone);
        editor.putString(KEY_USER_ADDRESS, address);
        editor.putString(KEY_USER_BIRTH_DATE, birthDate);
        editor.putString(KEY_USER_GCASH_NUMBER, gcashNumber);
        editor.putString(KEY_USER_QR_CODE_PATH, qrCodePath);
        editor.putString(KEY_USER_VALID_ID_TYPE, validIdType);
        editor.putString(KEY_USER_ID_NUMBER, idNumber);
        editor.putString(KEY_USER_ID_FRONT_FILE, idFrontFile);
        editor.putString(KEY_USER_ID_BACK_FILE, idBackFile);
        editor.putString(KEY_USER_STATUS, status);
        editor.apply();
    }
    
    private void navigateToDashboard(String userRole) {
        Intent intent;
        
        if ("Boarder".equals(userRole)) {
            // Navigate to BoarderDashboard
            intent = new Intent(Login.this, BoarderDashboard.class);
        } else if ("BH Owner".equals(userRole)) {
            // Navigate to BHOwnerDashboard
            intent = new Intent(Login.this, BHOwnerDashboard.class);
        } else {
            // Unknown role, show error
            Toast.makeText(this, "Unknown user role. Please contact support.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Clear the login activity from the stack
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    // Method to logout (can be called from other activities)
    public static void logout(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        
        Intent intent = new Intent(context, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
    
    // Method to get current user info (can be called from other activities)
    public static String getCurrentUserId(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString(KEY_USER_ID, null);
    }
    
    public static String getCurrentUserRole(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString(KEY_USER_ROLE, null);
    }
    
    public static String getCurrentUserName(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString(KEY_USER_NAME, null);
    }
    
    public static String getCurrentUserEmail(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString(KEY_USER_EMAIL, null);
    }
    
    public static String getCurrentUserMiddleName(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString(KEY_USER_MIDDLE_NAME, null);
    }
    
    public static String getCurrentUserPhone(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString(KEY_USER_PHONE, null);
    }
    
    public static String getCurrentUserAddress(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString(KEY_USER_ADDRESS, null);
    }
    
    public static String getCurrentUserBirthDate(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString(KEY_USER_BIRTH_DATE, null);
    }
    
    public static String getCurrentUserGcashNumber(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString(KEY_USER_GCASH_NUMBER, null);
    }
    
    public static String getCurrentUserQrCodePath(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString(KEY_USER_QR_CODE_PATH, null);
    }
    
    public static String getCurrentUserValidIdType(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString(KEY_USER_VALID_ID_TYPE, null);
    }
    
    public static String getCurrentUserIdNumber(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString(KEY_USER_ID_NUMBER, null);
    }
    
    public static String getCurrentUserIdFrontFile(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString(KEY_USER_ID_FRONT_FILE, null);
    }
    
    public static String getCurrentUserIdBackFile(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString(KEY_USER_ID_BACK_FILE, null);
    }
    
    public static String getCurrentUserStatus(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString(KEY_USER_STATUS, null);
    }
}