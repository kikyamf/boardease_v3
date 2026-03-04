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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
//testubg
import java.util.Map;

public class Login extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private Button btnGuest;
    private Button btnGoogleSignIn;
    private TextView tvSignUp, tvForgotPassword;
    private ImageButton btnTogglePassword;
    private CheckBox chkRemember;
    private ProgressDialog progressDialog;
    private RequestQueue requestQueue;
    private boolean isPasswordVisible = false;

    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    // SharedPreferences for storing user session
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "UserSession";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    
    // SharedPreferences keys for Remember Me
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_SAVED_EMAIL = "saved_email";

    // Server URL - Update this path if login.php is in a different location
    private static final String LOGIN_URL = "https://boardease.calapebohol.com/login.php";
    private static final String GOOGLE_AUTH_URL = "https://boardease.calapebohol.com/google_auth.php";

    // TODO: Replace with your actual Web Client ID from Firebase Console
    private static final String WEB_CLIENT_ID = "663702982127-8dqon7abvp2udgt5j9em59kmdduq37oo.apps.googleusercontent.com";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Initialize views
        initializeViews();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(WEB_CLIENT_ID)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize Google Sign-In Launcher
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        handleGoogleSignInResult(task);
                    }
                }
        );

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Initialize Volley RequestQueue
        requestQueue = Volley.newRequestQueue(this);

        // Check if user is already logged in
        checkExistingSession();
        
        // Load saved email if Remember Me was checked
        loadSavedEmail();

        // Set click listeners
        setClickListeners();
        
        // Start animations
        startLogoAnimation();
        startEntryAnimations();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void addButtonClickAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }

    private void startEntryAnimations() {
        View loginCard = findViewById(R.id.loginCard);
        View btnGuest = findViewById(R.id.btnGuest);

        android.view.animation.Animation slideUp = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.anim_slide_up_fade_in);

        loginCard.startAnimation(slideUp);
        btnGuest.startAnimation(slideUp);
    }
    
    private void startLogoAnimation() {
        ImageView logoImage = findViewById(R.id.logoImage);
        android.view.animation.Animation bounceAnimation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.logo_bounce);
        logoImage.startAnimation(bounceAnimation);
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGuest = findViewById(R.id.btnGuest);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvSignUp = findViewById(R.id.tvSignUp);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        chkRemember = findViewById(R.id.chkRemember);

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in...");
        progressDialog.setCancelable(false);

        // Add button click animations
        addButtonClickAnimation(btnLogin);
        addButtonClickAnimation(btnGoogleSignIn);
        addButtonClickAnimation(btnGuest);
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
    
    private void loadSavedEmail() {
        // Check if Remember Me was previously checked
        boolean rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
        String savedEmail = sharedPreferences.getString(KEY_SAVED_EMAIL, "");
        
        if (rememberMe && !savedEmail.isEmpty()) {
            // Set checkbox as checked
            chkRemember.setChecked(true);
            // Load saved email
            etEmail.setText(savedEmail);
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

        btnGoogleSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Login.this, ForgotPasswordActivity.class);
                startActivity(intent);
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

    private void signInWithGoogle() {
        // Sign out first to ensure the account picker always appears
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            // Signed in successfully, show authenticated UI.
            String idToken = account.getIdToken();
            String email = account.getEmail();
            
            Log.d("GoogleSignIn", "ID Token: " + idToken);
            Log.d("GoogleSignIn", "Email: " + email);
            
            authenticateWithBackend(idToken, email);
            
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("GoogleSignIn", "signInResult:failed code=" + e.getStatusCode());
            showGoogleSignInErrorDialog("Google sign-in failed. Please try again.");
        }
    }
    
    private void showGoogleSignInErrorDialog(String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Sign-In Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }

    private void authenticateWithBackend(String idToken, String email) {
        progressDialog.setMessage("Authenticating with Google...");
        progressDialog.show();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, GOOGLE_AUTH_URL,
                response -> {
                    progressDialog.dismiss();
                    Log.d("GoogleAuth", "Response: " + response);
                    handleLoginResponse(response);
                },
                error -> {
                    progressDialog.dismiss();
                    Log.e("GoogleAuth", "Error: " + error.getMessage());
                    showAuthenticationErrorDialog("Authentication failed. Please try again.");
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("idToken", idToken);
                params.put("email", email);
                return params;
            }
        };

        requestQueue.add(stringRequest);
    }
    
    private void showAuthenticationErrorDialog(String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Authentication Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
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
                String userStatus = userObject.optString("status", "approved");

                // Get additional user details (if available)
                String suffix = userObject.optString("suffix", "");
                String middleName = userObject.optString("middleName", "");
                String phone = userObject.optString("phone", "");
                String birthDate = userObject.optString("birthDate", "");
                String address = userObject.optString("address", "");
                String gcashNumber = userObject.optString("gcashNumber", "");

                // Build full name with suffix
                String fullName = firstName + " " + lastName;
                if (suffix != null && !suffix.isEmpty() && !suffix.equals("None")) {
                    fullName += " " + suffix;
                }

                // Store user session in SharedPreferences
                saveUserSession(userId, userRole, fullName, userEmail,
                        middleName, phone, birthDate, address, gcashNumber, suffix, userStatus);

                // Also store first and last name separately
                saveUserNameParts(firstName, lastName);
                
                // Handle Remember Me checkbox - use email from EditText
                String email = etEmail.getText().toString().trim();
                handleRememberMe(email);

                // Navigate directly to appropriate dashboard based on role
                String status = userObject.optString("status", "");
    
                if ("email_unverified".equals(status)) {
                    // Force verification
                    Intent intent = new Intent(Login.this, EmailVerificationActivity.class);
                    intent.putExtra("email", userEmail); // Use userEmail from the backend response
                    startActivity(intent);
                } else if ("rejected".equals(status)) {
                    showAuthenticationErrorDialog("Your account has been rejected. Please contact support.");
                } else {
                    // Allow login for approved, profile_incomplete, and pending_admin_review
                    // User session is already saved above, just navigate
                    navigateToDashboard(userObject.optString("role", ""));
                }

            } else {
                // Check if verification is required
                boolean requiresVerification = jsonObject.optBoolean("requires_verification", false);
                boolean needsRegistration = jsonObject.optBoolean("needs_registration", false);

                if (needsRegistration) {
                    // Navigate to registration - show modal first
                    new AlertDialog.Builder(Login.this)
                            .setTitle("Account Not Found")
                            .setMessage("this Google account is not yet registered. Would you like to create a new account?")
                            .setPositiveButton("Sign Up", (dialog, which) -> {
                                Intent intent = new Intent(Login.this, RegistrationActivity.class);
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else if (requiresVerification) {
                    // Navigate to email verification
                    Intent intent = new Intent(Login.this, EmailVerificationActivity.class);
                    intent.putExtra("email", etEmail.getText().toString().trim());
                    startActivity(intent);
                } else {
                    // Show error message in modal
                    new AlertDialog.Builder(Login.this)
                            .setTitle("Login Failed")
                            .setMessage(message)
                            .setPositiveButton("OK", null)
                            .show();
                }
            }

        } catch (JSONException e) {
            Log.e("LoginError", "JSON parsing error: " + e.getMessage());
            Toast.makeText(this, "Server response error. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserSession(String userId, String userRole, String userName, String userEmail,
                                 String middleName, String phone, String birthDate, String address, String gcashNumber, String suffix, String status) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_ROLE, userRole);
        editor.putString(KEY_USER_NAME, userName);
        editor.putString(KEY_USER_EMAIL, userEmail);
        editor.putString("user_status", status);
        editor.putString("user_middle_name", middleName);
        editor.putString("user_phone", phone);
        editor.putString("user_birth_date", birthDate);
        editor.putString("user_address", address);
        editor.putString("user_gcash_number", gcashNumber);
        editor.putString("user_suffix", suffix);
        editor.apply();
    }
    
    private void handleRememberMe(String email) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        
        if (chkRemember.isChecked()) {
            // Save email and remember me preference
            editor.putBoolean(KEY_REMEMBER_ME, true);
            editor.putString(KEY_SAVED_EMAIL, email);
        } else {
            // Clear saved email and remember me preference
            editor.putBoolean(KEY_REMEMBER_ME, false);
            editor.remove(KEY_SAVED_EMAIL);
        }
        editor.apply();
    }

    // Helper method to save first and last name separately (called during login)
    private void saveUserNameParts(String firstName, String lastName) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_first_name", firstName);
        editor.putString("user_last_name", lastName);
        editor.apply();
    }

    private void navigateToDashboard(String userRole) {
        // Check user status first for hybrid flow
        String status = sharedPreferences.getString("user_status", "approved");
        
        // Soft restriction: Allow entry but dashboards will handle the "Welcome/Complete Profile" prompt
        // if ("profile_incomplete".equals(status)) { ... } - REMOVED

        Intent intent;

        if ("Boarder".equals(userRole)) {
            // Navigate to BoarderDashboard
            intent = new Intent(Login.this, BoarderDashboard.class);
            intent.putExtra("IS_NEW_LOGIN", true);
        } else if ("BH Owner".equals(userRole)) {
            // Navigate to MainActivity (Owner Dashboard)
            intent = new Intent(Login.this, MainActivity.class);
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
        // Get user ID before clearing preferences
        String userId = getCurrentUserId(context);
        
        if (userId != null) {
            // Call server to mark user as offline (fire and forget)
            String logoutUrl = "https://boardease.calapebohol.com/logout.php?user_id=" + userId;
            StringRequest request = new StringRequest(Request.Method.GET, logoutUrl,
                response -> Log.d("Logout", "Logged out from server: " + response),
                error -> Log.e("Logout", "Error logging out from server", error)
            );
            Volley.newRequestQueue(context).add(request);
        }

        // Google Sign Out - Fired to ensure Google session is cleared locally
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(context, gso);
            googleSignInClient.signOut();
            googleSignInClient.revokeAccess(); // Forces account picker next time
        } catch (Exception e) {
            Log.e("Logout", "Error during Google sign out", e);
        }

        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        
        // Save remember me preference and email before clearing
        boolean rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
        String savedEmail = sharedPreferences.getString(KEY_SAVED_EMAIL, "");
        
        // Clear all preferences
        editor.clear();
        
        // Restore remember me preference and email if they were set
        if (rememberMe && !savedEmail.isEmpty()) {
            editor.putBoolean(KEY_REMEMBER_ME, rememberMe);
            editor.putString(KEY_SAVED_EMAIL, savedEmail);
        }
        
        editor.apply();

        Intent intent = new Intent(context, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    /**
     * Checks if the user has full access to features.
     * If status is 'profile_incomplete' or 'pending_admin_review', it shows a restriction modal.
     * 
     * @param context The context to show the dialog in
     * @return true if approved, false otherwise
     */
    public static boolean checkFeatureAccess(android.content.Context context) {
        String status = getCurrentUserStatus(context);
        if ("approved".equals(status)) {
            return true;
        }

        String title = "Access Restricted";
        String message;
        String positiveButtonText = "Complete Profile";
        
        if ("profile_incomplete".equals(status)) {
            message = "This feature is restricted. Please complete your profile to gain full access.";
        } else if ("pending_admin_review".equals(status)) {
            message = "Your profile is currently under admin review. This feature will be available once your account is approved.";
            positiveButtonText = "Check Status";
        } else {
            message = "This feature is restricted. Please verify your account to gain full access.";
        }

        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButtonText, (dialog, which) -> {
                    if ("pending_admin_review".equals(status)) {
                        // Just a placeholder for refresh or status check if needed
                        android.widget.Toast.makeText(context, "Refresh dashboard to see updates", android.widget.Toast.LENGTH_SHORT).show();
                    } else {
                        Intent intent = new Intent(context, UpdateProfileActivity.class);
                        context.startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        
        return false;
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

    // Additional user info methods
    public static String getCurrentUserMiddleName(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString("user_middle_name", "");
    }

    public static String getCurrentUserPhone(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString("user_phone", "");
    }

    public static String getCurrentUserBirthDate(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString("user_birth_date", "");
    }

    public static String getCurrentUserAddress(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString("user_address", "");
    }

    public static String getCurrentUserGcashNumber(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString("user_gcash_number", "");
    }

    public static String getCurrentUserSuffix(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString("user_suffix", "");
    }

    public static String getCurrentUserStatus(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString("user_status", "approved");
    }

    public static String getCurrentUserFirstName(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString("user_first_name", "");
    }

    public static String getCurrentUserLastName(android.content.Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString("user_last_name", "");
    }
}