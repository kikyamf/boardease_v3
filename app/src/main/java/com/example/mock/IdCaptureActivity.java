package com.example.mock;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import android.util.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import java.util.HashMap;
import java.util.Map;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class IdCaptureActivity extends AppCompatActivity {
    private static final String TAG = "IdCaptureActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    
    private CameraPreview cameraPreview;
    private ImageButton btnCapture, btnBack, btnFlash;
    private TextView tvIdType, tvInstructions, tvStatus;
    private LinearLayout processingOverlay;
    
    private String idType; // "front" or "back"
    private String selectedIdType; // Selected ID type from spinner (e.g., "Driver's License", "PhilID (National ID)")
    private boolean isFlashOn = false;
    private boolean isProcessing = false;
    private boolean isCapturing = true; // Track if we're in capture mode
    private boolean isGreenFrameVisible = false; // Track green frame visibility
    private long startTime; // Track when camera started
    private String capturedImagePath; // Store captured image path
    private String capturedIdNumber; // Store captured ID number
    private Handler autoCaptureHandler = new Handler();
    private Runnable autoCaptureRunnable;
    private boolean isVerificationPassed = false; // Track if current scan passed verification
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_id_capture);
        
        // Disable camera shutter sound globally
        try {
            System.setProperty("camera.disable_shutter_sound", "true");
            System.setProperty("ro.camera.sound.forced", "0");
            System.setProperty("camera.sound.forced", "0");
            Log.d(TAG, "Camera shutter sound disabled via system properties");
        } catch (Exception e) {
            Log.d(TAG, "Could not set system properties: " + e.getMessage());
        }
        
        // Get ID type from intent
        idType = getIntent().getStringExtra("id_type");
        if (idType == null) {
            idType = "front";
        }
        
        // Get selected ID type from intent
        selectedIdType = getIntent().getStringExtra("selected_id_type");
        if (selectedIdType == null || selectedIdType.equals("Select --")) {
            selectedIdType = "Driver's License"; // Default fallback
        }
        Log.d(TAG, "Selected ID type for verification: " + selectedIdType);
        
        initializeViews();
        setupClickListeners();
        checkCameraPermission();
    }
    
    private void initializeViews() {
        cameraPreview = findViewById(R.id.camera_preview);
        btnCapture = findViewById(R.id.btn_capture);
        btnBack = findViewById(R.id.btn_back);
        btnFlash = findViewById(R.id.btn_flash);
        tvIdType = findViewById(R.id.tv_id_type);
        tvInstructions = findViewById(R.id.tv_instructions);
        tvStatus = findViewById(R.id.tv_status);
        processingOverlay = findViewById(R.id.processing_overlay);
        
        // Initially disable capture button until verification passes
        if (btnCapture != null) {
            btnCapture.setEnabled(false);
            btnCapture.setAlpha(0.5f); // Make it semi-transparent
        }
        
        // Set initial flash button icon (flash off)
        btnFlash.setImageResource(R.drawable.ic_flashlight_modern_off);
        
        // Update UI based on ID type
        if ("front".equals(idType)) {
            tvIdType.setText("Position the FRONT of your ID within the frame");
            tvInstructions.setText("Hold ID horizontally and make sure all text is clear and readable");
        } else {
            tvIdType.setText("Position the BACK of your ID within the frame");
            tvInstructions.setText("Hold ID horizontally and make sure all text is clear and readable");
        }
    }
    
    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            // Return captured result if available, otherwise cancel
            if (capturedImagePath != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("image_path", capturedImagePath);
                resultIntent.putExtra("id_number", capturedIdNumber != null ? capturedIdNumber : "");
                resultIntent.putExtra("id_type", idType);
                setResult(Activity.RESULT_OK, resultIntent);
                Log.d(TAG, "Back button - returning captured result: " + capturedImagePath);
            } else {
                setResult(Activity.RESULT_CANCELED);
                Log.d(TAG, "Back button - no capture made, returning cancel");
            }
            finish();
        });
        
        btnFlash.setOnClickListener(v -> toggleFlash());
        
        btnCapture.setOnClickListener(v -> {
            if (!isProcessing) {
                captureId();
            }
        });
        
        // Add touch-to-focus functionality
        cameraPreview.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                cameraPreview.focusOnTouch(event.getX(), event.getY());
                return true;
            }
            return false;
        });
    }
    
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA}, 
                CAMERA_PERMISSION_REQUEST);
        } else {
            // Initialize frame to white
            hideGreenFrame();
            startTime = System.currentTimeMillis();
            startAutoCapture();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Initialize frame to white
                hideGreenFrame();
                startTime = System.currentTimeMillis();
                startAutoCapture();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    
    private void startAutoCapture() {
        // Manual capture only - no auto-capture
        tvStatus.setText("Position your ID in the frame. Green frame will appear when ID is detected");
        hideGreenFrame();
        
        // Start real ID detection for visual feedback
        startVisualIdDetection();
    }
    
    private void startVisualIdDetection() {
        // Real ID detection for visual feedback
        // Continue scanning even after verification passes to detect when ID is removed
        autoCaptureRunnable = () -> {
            if (!isProcessing && cameraPreview.isPreviewRunning()) {
                Log.d(TAG, "Checking for real ID detection...");
                // Always check, even if verification passed (to detect when ID is removed)
                checkForVisualIdIndicators();
            }
            
            // Schedule next check for continuous monitoring
            // Always continue scanning to detect when ID is removed from view
            if (isCapturing && !isProcessing) {
                Log.d(TAG, "Scheduling next detection check in 1.5 seconds");
                autoCaptureHandler.postDelayed(autoCaptureRunnable, 1500); // Faster detection
            } else {
                Log.d(TAG, "Not scheduling next check - Capturing: " + isCapturing + ", Processing: " + isProcessing);
            }
        };
        autoCaptureHandler.postDelayed(autoCaptureRunnable, 1500); // Check every 1.5 seconds for faster response
    }
    
    private void startIdDetection() {
        // Check for ID every 5 seconds to reduce sound frequency
        autoCaptureRunnable = () -> {
            if (!isProcessing && cameraPreview.isPreviewRunning()) {
                Log.d(TAG, "Checking for valid ID...");
                checkForValidId();
            }
        };
        autoCaptureHandler.postDelayed(autoCaptureRunnable, 5000); // Start after 5 seconds
    }
    
    private void checkForValidId() {
        if (cameraPreview.getCamera() == null) return;
        
        try {
            // Use a simpler approach - just check if we can take a picture
            Camera camera = cameraPreview.getCamera();
            if (camera != null) {
                // Try to take a low-quality picture for analysis
                camera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        // Process the image in background
                        new Thread(() -> {
                            try {
                                // Convert to bitmap
                                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                                if (bitmap != null) {
                                    // Analyze the image for ID content
                                    analyzeImageForId(bitmap);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error analyzing image for ID: " + e.getMessage());
                            }
                        }).start();
                        
                        // Restart preview
                        camera.startPreview();
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error taking picture for ID detection: " + e.getMessage());
            // If camera is busy, just continue scanning
            runOnUiThread(() -> {
                if (isCapturing && !isProcessing) {
                    autoCaptureHandler.postDelayed(autoCaptureRunnable, 2000); // Check again in 2 seconds
                }
            });
        }
    }
    
    private void checkForVisualIdIndicators() {
        // Real-time ID detection with continuous monitoring
        // This provides feedback by taking silent pictures for analysis
        
        Log.d(TAG, "checkForVisualIdIndicators called - Preview running: " + cameraPreview.isPreviewRunning() + ", Processing: " + isProcessing);
        
        if (cameraPreview.isPreviewRunning() && !isProcessing) {
            // Take a silent picture for ID detection
            try {
                Camera camera = cameraPreview.getCamera();
                if (camera != null) {
                    Log.d(TAG, "Taking silent picture for ID detection...");
                    // Take picture for analysis (completely silent)
                    cameraPreview.takePicture(new Camera.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            Log.d(TAG, "Silent picture taken, analyzing for ID detection...");
                            // Process the image in background thread
                            new Thread(() -> {
                                try {
                                    // Convert to bitmap
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                                    if (bitmap != null) {
                                        Log.d(TAG, "Bitmap created, analyzing image for ID detection...");
                                        // Analyze the image for ID content
                                        analyzeImageForIdDetection(bitmap);
                                    } else {
                                        Log.e(TAG, "Failed to create bitmap from camera data");
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error analyzing image for ID detection: " + e.getMessage());
                                }
                            }).start();
                            
                            // Restart preview immediately
                            camera.startPreview();
                        }
                    });
                } else {
                    Log.e(TAG, "Camera is null, cannot take picture for detection");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error taking picture for ID detection: " + e.getMessage());
                // If camera is busy, just continue scanning
                runOnUiThread(() -> {
                    if (isCapturing && !isProcessing) {
                        Log.d(TAG, "Scheduling next detection check in 2 seconds...");
                        autoCaptureHandler.postDelayed(autoCaptureRunnable, 2000); // Check again in 2 seconds
                    }
                });
            }
        } else {
            Log.d(TAG, "Skipping detection - Preview running: " + cameraPreview.isPreviewRunning() + ", Processing: " + isProcessing);
            // Camera not ready - hide green frame
            if (isGreenFrameVisible) {
                hideGreenFrame();
                tvStatus.setText("Camera not ready...");
            }
        }
    }
    
    private void analyzeImageForIdDetection(Bitmap bitmap) {
        // Use Philippine ID Detection API for more accurate detection
        callPhilippineIdDetectionAPI(bitmap);
    }
    
    private void callPhilippineIdDetectionAPI(Bitmap bitmap) {
        // Convert bitmap to base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageBytes = baos.toByteArray();
        String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        String dataUrl = "data:image/jpeg;base64," + base64Image;

        // Prepare JSON payload
        JSONObject jsonPayload = new JSONObject();
        try {
            jsonPayload.put("image", dataUrl);
            jsonPayload.put("side", idType); // "front" or "back"
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON payload: " + e.getMessage());
            runOnUiThread(() -> {
                hideGreenFrame();
                tvStatus.setText("Error preparing ID detection");
            });
            return;
        }

        // Make API call
        StringRequest request = new StringRequest(
            Request.Method.POST,
            "https://boardease.calapebohol.com/philippine_id_detection_api.php",
            response -> {
                Log.d(TAG, "Philippine ID API Response: " + response);
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    boolean success = jsonResponse.getBoolean("success");
                    
                    if (success) {
                        JSONObject data = jsonResponse.getJSONObject("data");
                        boolean isValid = data.getBoolean("isValid");
                        String detectedIdType = data.getString("idType");
                        String extractedText = data.getString("extractedText");
                        int confidence = data.getInt("confidence");
                        
                        Log.d(TAG, "ID Detection - Valid: " + isValid + 
                                   ", Type: " + detectedIdType + 
                                   ", Confidence: " + confidence + "%" +
                                   ", Text: " + (extractedText.isEmpty() ? "EMPTY" : "HAS_TEXT"));
                        
                        // If API returns empty text, fall back to local detection
                        if (extractedText.isEmpty() || extractedText.trim().isEmpty()) {
                            Log.d(TAG, "API returned empty text, falling back to local detection");
                            performLocalIdDetection(bitmap);
                            return;
                        }
                        
                        runOnUiThread(() -> {
                            if (isValid && confidence >= 50) {
                                if (!isGreenFrameVisible) {
                                    showGreenFrame();
                                    tvStatus.setText("✅ " + detectedIdType + " detected! Ready to capture");
                                }
                            } else {
                                if (isGreenFrameVisible) {
                                    hideGreenFrame();
                                    tvStatus.setText("Position your " + idType + " ID in the frame");
                                }
                            }
                            
                            // Note: Next check is scheduled in startVisualIdDetection()
                        });
                    } else {
                        String error = jsonResponse.getString("error");
                        Log.e(TAG, "Philippine ID API Error: " + error);
                        runOnUiThread(() -> {
                            hideGreenFrame();
                            tvStatus.setText("Position your " + idType + " ID in the frame");
                            // Note: Next check is scheduled in startVisualIdDetection()
                        });
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing Philippine ID API response: " + e.getMessage());
                    runOnUiThread(() -> {
                        hideGreenFrame();
                        tvStatus.setText("Position your " + idType + " ID in the frame");
                        // Note: Next check is scheduled in startVisualIdDetection()
                    });
                }
            },
            error -> {
                Log.e(TAG, "Philippine ID API request failed: " + error.getMessage());
                // Fallback to local detection
                performLocalIdDetection(bitmap);
            }
        ) {
            @Override
            public byte[] getBody() {
                try {
                    return jsonPayload.toString().getBytes("utf-8");
                } catch (Exception e) {
                    Log.e(TAG, "Error creating request body: " + e.getMessage());
                    return null;
                }
            }
            
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
            
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        // Add request to queue
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
    
    private void performLocalIdDetection(Bitmap bitmap) {
        // Fallback to local OCR detection
        try {
            TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
            
            if (!textRecognizer.isOperational()) {
                Log.d(TAG, "TextRecognizer not operational for detection");
                runOnUiThread(() -> {
                    if (isGreenFrameVisible) {
                        hideGreenFrame();
                        tvStatus.setText("OCR not available - position ID manually");
                    }
                });
                return;
            }
            
            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            android.util.SparseArray<TextBlock> textBlocks = textRecognizer.detect(frame);
            
            StringBuilder extractedText = new StringBuilder();
            for (int i = 0; i < textBlocks.size(); i++) {
                TextBlock textBlock = textBlocks.valueAt(i);
                extractedText.append(textBlock.getValue()).append(" ");
            }
            
            String fullText = extractedText.toString();
            Log.d(TAG, "Local OCR text: " + fullText);
            
            // Use local validation as fallback
            boolean isValidId = isStrictValidIdDocument(fullText, bitmap);
            Log.d(TAG, "Local ID validation result: " + (isValidId ? "VALID" : "INVALID"));
            
            runOnUiThread(() -> {
                if (isValidId) {
                    if (!isGreenFrameVisible) {
                        Log.d(TAG, "Showing green frame - ID detected locally");
                        showGreenFrame();
                        tvStatus.setText("✅ ID detected! Ready to capture");
                    } else {
                        Log.d(TAG, "Green frame already visible");
                    }
                } else {
                    if (isGreenFrameVisible) {
                        Log.d(TAG, "Hiding green frame - no valid ID detected");
                        hideGreenFrame();
                        tvStatus.setText("Position your " + idType + " ID in the frame");
                    } else {
                        Log.d(TAG, "White frame already visible");
                    }
                }
                
                // Note: Next check is scheduled in startVisualIdDetection()
            });
            
            textRecognizer.release();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in local ID detection: " + e.getMessage());
            runOnUiThread(() -> {
                if (isGreenFrameVisible) {
                    hideGreenFrame();
                    tvStatus.setText("Detection error - position ID manually");
                }
                // Note: Next check is scheduled in startVisualIdDetection()
            });
        }
    }
    
    private boolean isStrictValidIdDocument(String text, Bitmap bitmap) {
        if (text == null || text.trim().isEmpty()) {
            Log.d(TAG, "No text extracted - not a valid ID");
            return false;
        }

        String textLower = text.toLowerCase();
        Log.d(TAG, "Analyzing text for ID validation (Type: " + idType + "): " + textLower);

        // Different validation criteria for front vs back ID
        if ("front".equals(idType)) {
            Log.d(TAG, "Using FRONT ID validation criteria");
            return validateFrontId(textLower, bitmap);
        } else {
            Log.d(TAG, "Using BACK ID validation criteria");
            return validateBackId(textLower, bitmap);
        }
    }
    
    private boolean validateFrontId(String textLower, Bitmap bitmap) {
        Log.d(TAG, "Validating FRONT ID");
        
        // Stricter ID keyword requirements for front ID
        String[] requiredIdKeywords = {
            "republic of the philippines",
            "philippine passport",
            "driver's license",
            "philid",
            "national id",
            "philippine national id",
            "phil national id",
            "umid",
            "sss",
            "gsis",
            "philhealth",
            "tin",
            "voter's",
            "postal",
            "license",
            "passport",
            "identification"
        };

        // More lenient criteria - only need 1 ID keyword for detection
        int keywordCount = 0;
        for (String keyword : requiredIdKeywords) {
            if (textLower.contains(keyword)) {
                keywordCount++;
                Log.d(TAG, "Found required ID keyword: " + keyword);
            }
        }

        boolean hasRequiredKeywords = keywordCount >= 1; // Reduced from 3 to 1
        Log.d(TAG, "Required keywords found: " + keywordCount + "/1");

        // Check for ID number patterns (optional for detection)
        boolean hasIdNumber = !extractIdNumberFromText(textLower).isEmpty();
        Log.d(TAG, "Has ID number: " + hasIdNumber);

        // Check for personal information patterns (optional for detection)
        String[] personalInfoKeywords = {
            "name", "address", "birth", "sex", "citizenship", 
            "civil status", "height", "weight", "blood type",
            "valid until", "expiry", "expires", "date of birth",
            "place of birth", "control no", "control number"
        };
        
        int personalInfoCount = 0;
        for (String keyword : personalInfoKeywords) {
            if (textLower.contains(keyword)) {
                personalInfoCount++;
            }
        }
        
        boolean hasPersonalInfo = personalInfoCount >= 1; // Reduced from 2 to 1
        Log.d(TAG, "Personal info found: " + personalInfoCount + "/1");

        // Check image quality - must have reasonable size
        boolean hasGoodQuality = bitmap.getWidth() >= 200 && bitmap.getHeight() >= 200;
        Log.d(TAG, "Image quality good: " + hasGoodQuality + " (" + bitmap.getWidth() + "x" + bitmap.getHeight() + ")");

        // More lenient validation - only need ID keyword and good quality
        boolean isValid = hasRequiredKeywords && hasGoodQuality;
        
        Log.d(TAG, "FRONT ID Validation Result - Keywords: " + hasRequiredKeywords + 
                   ", IDNumber: " + hasIdNumber + 
                   ", PersonalInfo: " + hasPersonalInfo + 
                   ", Quality: " + hasGoodQuality + 
                   ", Final: " + isValid);

        return isValid;
    }
    
    private boolean validateBackId(String textLower, Bitmap bitmap) {
        Log.d(TAG, "Validating BACK ID");
        
        // More specific criteria for back ID (usually has less text but specific content)
        String[] backIdKeywords = {
            "republic of the philippines",
            "philippine passport",
            "driver's license",
            "philid",
            "national id",
            "license",
            "passport",
            "identification",
            "valid until",
            "expiry",
            "expires",
            "signature",
            "authorized",
            "official",
            "government",
            "qr code",
            "qr",
            "barcode",
            "machine readable",
            "mrz",
            "sex",
            "blood type",
            "marital status",
            "place of birth",
            "date of issue",
            "issued",
            "valid",
            "card",
            "document",
            "philippines",
            "phil",
            "id",
            "number",
            "code",
            "control number",
            "control no",
            "serial number",
            "serial no",
            "reference",
            "ref",
            "transaction",
            "trans",
            "verification",
            "verify",
            "authentic",
            "genuine",
            "security",
            "hologram",
            "watermark"
        };

        // Must have at least 1 ID-related keyword for back ID
        int keywordCount = 0;
        for (String keyword : backIdKeywords) {
            if (textLower.contains(keyword)) {
                keywordCount++;
                Log.d(TAG, "Found back ID keyword: " + keyword);
            }
        }

        boolean hasRequiredKeywords = keywordCount >= 1;
        Log.d(TAG, "Back ID keywords found: " + keywordCount + "/1");

        // Check for meaningful text content (back ID should have substantial text)
        boolean hasTextContent = textLower.length() >= 20; // Increased from 5 to 20
        Log.d(TAG, "Has text content: " + hasTextContent + " (length: " + textLower.length() + ")");

        // Check image quality - must have reasonable size
        boolean hasGoodQuality = bitmap.getWidth() >= 200 && bitmap.getHeight() >= 200;
        Log.d(TAG, "Image quality good: " + hasGoodQuality + " (" + bitmap.getWidth() + "x" + bitmap.getHeight() + ")");

        // More strict criteria for back ID - need BOTH keyword AND substantial text content
        boolean isValid = hasRequiredKeywords && hasTextContent && hasGoodQuality;
        
        Log.d(TAG, "BACK ID Validation Result - Keywords: " + hasRequiredKeywords + 
                   ", TextContent: " + hasTextContent + 
                   ", Quality: " + hasGoodQuality + 
                   ", Final: " + isValid);

        return isValid;
    }
    
    private boolean isCameraStable() {
        // Simple stability check - in a real implementation, you could use
        // accelerometer or gyroscope data to detect if the phone is being held steady
        // For now, we'll use a simple time-based approach
        long currentTime = System.currentTimeMillis();
        long timeSinceStart = currentTime - startTime;
        
        // Consider camera stable after 5 seconds of positioning
        return timeSinceStart > 5000;
    }
    
    
    private void analyzeImageForId(Bitmap bitmap) {
        try {
            // Save bitmap to temporary file for verification
            String tempFileName = "temp_scan_" + System.currentTimeMillis() + ".jpg";
            File tempFile = new File(getCacheDir(), tempFileName);
            FileOutputStream fos = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            
            android.net.Uri imageUri = android.net.Uri.fromFile(tempFile);
            
            // Use IdVerificationHelper for real-time verification
            if (selectedIdType != null && !selectedIdType.isEmpty()) {
                Log.d(TAG, "Performing real-time ID verification with selected type: " + selectedIdType);
                
                IdVerificationHelper.verifyIdDocument(this, imageUri, selectedIdType, new IdVerificationHelper.VerificationCallback() {
                    @Override
                    public void onVerificationComplete(IdVerificationHelper.VerificationResult result) {
                        if (result.isValid) {
                            Log.d(TAG, "✅ ID verification passed! Detected: " + result.detectedIdType);
                            
                            // Set verification passed flag BEFORE showing UI
                            isVerificationPassed = true;
                            
                            // Show green frame to indicate ready for capture
                            runOnUiThread(() -> {
                                showGreenFrame();
                                String statusMsg = "✅ " + result.detectedIdType + " verified! Tap capture to take photo";
                                if (result.extractedIdNumber != null && !result.extractedIdNumber.isEmpty()) {
                                    statusMsg += "\nID Number: " + result.extractedIdNumber;
                                    capturedIdNumber = result.extractedIdNumber;
                                }
                                tvStatus.setText(statusMsg);
                                
                                // Log for debugging
                                Log.d(TAG, "Green frame shown, isVerificationPassed: " + isVerificationPassed);
                                Log.d(TAG, "Capture button enabled: " + (btnCapture != null && btnCapture.isEnabled()));
                            });
                        } else {
                            Log.d(TAG, "❌ ID verification failed: " + result.reason);
                            
                            // Set verification failed flag
                            isVerificationPassed = false;
                            
                            // Check if we had a green frame before (ID was detected but now invalid)
                            boolean hadGreenFrame = isGreenFrameVisible;
                            
                            // Hide green frame and show error, then continue scanning
                            runOnUiThread(() -> {
                                hideGreenFrame();
                                
                                // If ID was previously detected but now invalid (removed from view)
                                if (hadGreenFrame) {
                                    tvStatus.setText("ID removed from view. Please position your " + idType + " ID in the frame");
                                } else {
                                    // First time detection failed
                                    String errorMsg = result.reason;
                                    if (errorMsg.length() > 80) {
                                        errorMsg = errorMsg.substring(0, 80) + "...";
                                    }
                                    tvStatus.setText("❌ " + errorMsg + "\nPlease adjust and try again");
                                    
                                    // Auto-reset after 3 seconds only for first-time failures
                                    new Handler().postDelayed(() -> {
                                        if (isCapturing && !isProcessing) {
                                            tvStatus.setText("Position your " + idType + " ID in the frame");
                                        }
                                    }, 3000);
                                }
                            });
                        }
                        
                        // Clean up temp file
                        if (tempFile.exists()) {
                            tempFile.delete();
                        }
                    }
                    
                    @Override
                    public void onVerificationError(String error) {
                        Log.e(TAG, "Verification error: " + error);
                        
                        // Check if we had a green frame before (ID was detected but now error)
                        boolean hadGreenFrame = isGreenFrameVisible;
                        isVerificationPassed = false;
                        
                        // Hide green frame and continue scanning
                        runOnUiThread(() -> {
                            hideGreenFrame();
                            
                            // If ID was previously detected but now error (removed from view)
                            if (hadGreenFrame) {
                                tvStatus.setText("ID removed from view. Please position your " + idType + " ID in the frame");
                            } else {
                                tvStatus.setText("Scanning... Please ensure ID is clear and well-lit");
                            }
                        });
                        
                        // Clean up temp file
                        if (tempFile.exists()) {
                            tempFile.delete();
                        }
                    }
                });
            } else {
                // Fallback to old method if no ID type selected
                Log.w(TAG, "No selected ID type, using fallback detection");
                performFallbackDetection(bitmap);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error analyzing image for ID: " + e.getMessage());
            // Continue scanning
            runOnUiThread(() -> {
                if (isCapturing && !isProcessing) {
                    autoCaptureHandler.postDelayed(autoCaptureRunnable, 2000);
                }
            });
        }
    }
    
    /**
     * Fallback detection method (old implementation) when ID type is not available
     */
    private void performFallbackDetection(Bitmap bitmap) {
        try {
            TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
            
            if (textRecognizer.isOperational()) {
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                SparseArray<TextBlock> textBlocks = textRecognizer.detect(frame);
                
                StringBuilder fullText = new StringBuilder();
                for (int i = 0; i < textBlocks.size(); i++) {
                    TextBlock textBlock = textBlocks.valueAt(i);
                    if (textBlock != null && textBlock.getValue() != null) {
                        fullText.append(textBlock.getValue()).append(" ");
                    }
                }
                
                String extractedText = fullText.toString().trim();
                Log.d(TAG, "Fallback: Analyzing text for ID detection: " + extractedText);
                
                if (isValidIdDocument(extractedText)) {
                    runOnUiThread(() -> {
                        showGreenFrame();
                        tvStatus.setText("✅ ID detected! Tap capture button to take photo");
                    });
                } else {
                    runOnUiThread(() -> {
                        hideGreenFrame();
                        tvStatus.setText("Position your ID in the frame");
                        if (isCapturing && !isProcessing) {
                            autoCaptureHandler.postDelayed(autoCaptureRunnable, 2000);
                        }
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in fallback detection: " + e.getMessage());
        }
    }
    
    
    
    private void stopIdDetection() {
        if (autoCaptureHandler != null && autoCaptureRunnable != null) {
            autoCaptureHandler.removeCallbacks(autoCaptureRunnable);
        }
    }
    
    private void showGreenFrame() {
        isGreenFrameVisible = true;
        
        // Ensure verification passed flag is set when showing green frame
        if (!isVerificationPassed) {
            Log.w(TAG, "Warning: Showing green frame but isVerificationPassed is false - setting to true");
            isVerificationPassed = true;
        }
        
        // Change the ID frame to green to indicate ready for capture
        FrameLayout idFrame = findViewById(R.id.id_frame);
        if (idFrame != null) {
            idFrame.setBackgroundResource(R.drawable.id_frame_green);
            
            // Change corner indicators to green
            updateCornerIndicators(true);
            
            // Enable capture button when verification passes
            if (btnCapture != null) {
                btnCapture.setEnabled(true);
                btnCapture.setAlpha(1.0f); // Make it fully visible
                Log.d(TAG, "Capture button enabled in showGreenFrame()");
            } else {
                Log.e(TAG, "btnCapture is null in showGreenFrame()");
            }
            
            // Add a subtle pulse animation to draw attention
            idFrame.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(200)
                .withEndAction(() -> {
                    idFrame.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(200)
                        .start();
                })
                .start();
        }
        
        // Add a subtle glow effect to the status text
        TextView statusText = findViewById(R.id.tv_status);
        if (statusText != null) {
            statusText.setTextColor(0xFF4CAF50); // Green color
            statusText.setShadowLayer(10, 0, 0, 0x804CAF50); // Green glow
        }
        
        Log.d(TAG, "Green frame shown - isVerificationPassed: " + isVerificationPassed);
    }
    
    private void hideGreenFrame() {
        isGreenFrameVisible = false;
        
        // Change the ID frame back to white
        FrameLayout idFrame = findViewById(R.id.id_frame);
        if (idFrame != null) {
            idFrame.setBackgroundResource(R.drawable.id_frame_background);
        }
        
        // Change corner indicators back to white
        updateCornerIndicators(false);
        
        // Disable capture button when verification fails or resets
        if (btnCapture != null && isCapturing) {
            btnCapture.setEnabled(false);
            btnCapture.setAlpha(0.5f); // Make it semi-transparent
        }
        
        // Reset status text color
        TextView statusText = findViewById(R.id.tv_status);
        if (statusText != null) {
            statusText.setTextColor(0xFFFFFFFF); // White color
            statusText.setShadowLayer(0, 0, 0, 0); // Remove glow
        }
        
        // Mark green frame as not visible
        isGreenFrameVisible = false;
    }
    
    private void updateCornerIndicators(boolean isGreen) {
        // Update all corner indicators
        View[] corners = {
            findViewById(R.id.corner_top_left),
            findViewById(R.id.corner_top_right),
            findViewById(R.id.corner_bottom_left),
            findViewById(R.id.corner_bottom_right)
        };
        
        for (View corner : corners) {
            if (corner != null) {
                corner.setBackgroundResource(isGreen ? R.drawable.corner_indicator_green : R.drawable.corner_indicator);
            }
        }
    }
    
    private void restartCameraPreview() {
        try {
            if (cameraPreview != null) {
                // Stop current preview
                cameraPreview.stopPreview();
                
                // Start preview again
                cameraPreview.startPreview();
                Log.d(TAG, "Camera preview restarted");
                
                // Restart ID detection after camera preview is ready
                new Handler().postDelayed(() -> {
                    // Ensure we're in capturing mode when restarting detection
                    if (!isCapturing) {
                        isCapturing = true;
                        Log.d(TAG, "Re-enabled capturing mode for detection restart");
                    }
                    
                    if (isCapturing && !isProcessing) {
                        Log.d(TAG, "Restarting ID detection after camera preview restart");
                        startVisualIdDetection();
                    } else {
                        Log.d(TAG, "Cannot restart detection - Capturing: " + isCapturing + ", Processing: " + isProcessing);
                    }
                }, 500); // Wait 500ms for camera to stabilize
            }
        } catch (Exception e) {
            Log.e(TAG, "Error restarting camera preview: " + e.getMessage());
        }
    }
    
    private void startCountdown() {
        new Handler().postDelayed(() -> {
            if (!isProcessing) {
                tvStatus.setText("Auto-capture in 4 seconds...");
                new Handler().postDelayed(() -> {
                    if (!isProcessing) {
                        tvStatus.setText("Auto-capture in 3 seconds...");
                        new Handler().postDelayed(() -> {
                            if (!isProcessing) {
                                tvStatus.setText("Auto-capture in 2 seconds...");
                                new Handler().postDelayed(() -> {
                                    if (!isProcessing) {
                                        tvStatus.setText("Auto-capture in 1 second...");
                                        new Handler().postDelayed(() -> {
                                            if (!isProcessing) {
                                                tvStatus.setText("Tap to capture or wait for auto-capture");
                                            }
                                        }, 1000);
                                    }
                                }, 1000);
                            }
                        }, 1000);
                    }
                }, 1000);
            }
        }, 1000);
    }
    
    private void toggleFlash() {
        if (cameraPreview.getCamera() != null) {
            isFlashOn = !isFlashOn;
            String flashMode = isFlashOn ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF;
            cameraPreview.setFlashMode(flashMode);
            
            // Use custom modern flashlight icons for flash on/off
            btnFlash.setImageResource(isFlashOn ? 
                R.drawable.ic_flashlight_modern_on : R.drawable.ic_flashlight_modern_off);
        }
    }
    
    private void captureId() {
        if (isProcessing) {
            Log.d(TAG, "Capture blocked: Already processing");
            return;
        }
        
        // Check verification status
        Log.d(TAG, "Capture button clicked - isVerificationPassed: " + isVerificationPassed);
        Log.d(TAG, "Capture button enabled: " + (btnCapture != null && btnCapture.isEnabled()));
        
        // Only allow capture if verification passed
        if (!isVerificationPassed) {
            Log.w(TAG, "Capture blocked: Verification not passed yet");
            Toast.makeText(this, "Please wait for ID verification to complete", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "✅ Starting capture - verification passed");
        isProcessing = true;
        isCapturing = false; // Stop capturing mode
        processingOverlay.setVisibility(View.VISIBLE);
        tvStatus.setText("Capturing ID...");
        
        // Stop ID detection
        stopIdDetection();
        
        cameraPreview.takePicture(new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                if (data == null) {
                    Log.e(TAG, "Picture taking failed - data is null");
                    runOnUiThread(() -> {
                        hideGreenFrame();
                        showError("Failed to capture image. Please try again.");
                        
                        // Reset processing state and return to camera preview
                        isProcessing = false;
                        isCapturing = true; // Re-enable capturing mode
                        processingOverlay.setVisibility(View.GONE);
                        tvStatus.setText("Position your " + idType + " ID in the frame");
                        
                        // Restart camera preview
                        restartCameraPreview();
                    });
                    return;
                }
                processCapturedImage(data);
            }
        });
    }
    
    private void processCapturedImage(byte[] data) {
        try {
            // Show green frame to indicate processing
            runOnUiThread(() -> {
                showGreenFrame();
                tvStatus.setText("Processing ID...");
            });
            
            // Convert byte array to bitmap
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            
            // Rotate bitmap if needed (camera captures in landscape)
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, 
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            
            // Crop the image to only include the ID frame area
            Bitmap croppedBitmap = cropToIdFrame(rotatedBitmap);
            
            // Save cropped image to temporary file
            String fileName = idType + "_id_" + System.currentTimeMillis() + ".jpg";
            File tempFile = new File(getCacheDir(), fileName);
            FileOutputStream fos = new FileOutputStream(tempFile);
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            
            Log.d(TAG, "Cropped ID image saved: " + tempFile.getAbsolutePath());
            
            // Extract ID number using OCR
            extractIdNumber(croppedBitmap, tempFile.getAbsolutePath());
            
        } catch (IOException e) {
            Log.e(TAG, "Error processing captured image: " + e.getMessage());
            runOnUiThread(() -> {
                hideGreenFrame();
                showError("Failed to process captured image");
            });
        }
    }
    
    private Bitmap cropToIdFrame(Bitmap originalBitmap) {
        try {
            // Get the ID frame view
            FrameLayout idFrame = findViewById(R.id.id_frame);
            if (idFrame == null) {
                Log.w(TAG, "ID frame not found, returning original bitmap");
                return originalBitmap;
            }
            
            // Get the camera preview view
            CameraPreview cameraPreview = findViewById(R.id.camera_preview);
            if (cameraPreview == null) {
                Log.w(TAG, "Camera preview not found, returning original bitmap");
                return originalBitmap;
            }
            
            // Get the dimensions of the ID frame relative to the camera preview
            int[] frameLocation = new int[2];
            int[] previewLocation = new int[2];
            idFrame.getLocationOnScreen(frameLocation);
            cameraPreview.getLocationOnScreen(previewLocation);
            
            // Calculate the frame position relative to the camera preview
            int frameX = frameLocation[0] - previewLocation[0];
            int frameY = frameLocation[1] - previewLocation[1];
            int frameWidth = idFrame.getWidth();
            int frameHeight = idFrame.getHeight();
            
            // Get the camera preview dimensions
            int previewWidth = cameraPreview.getWidth();
            int previewHeight = cameraPreview.getHeight();
            
            // Calculate the crop area in the original bitmap
            // Note: Camera preview might be rotated, so we need to account for that
            int cropX = (int) ((float) frameX / previewWidth * originalBitmap.getWidth());
            int cropY = (int) ((float) frameY / previewHeight * originalBitmap.getHeight());
            int cropWidth = (int) ((float) frameWidth / previewWidth * originalBitmap.getWidth());
            int cropHeight = (int) ((float) frameHeight / previewHeight * originalBitmap.getHeight());
            
            // Ensure crop area is within bitmap bounds
            cropX = Math.max(0, Math.min(cropX, originalBitmap.getWidth() - cropWidth));
            cropY = Math.max(0, Math.min(cropY, originalBitmap.getHeight() - cropHeight));
            cropWidth = Math.min(cropWidth, originalBitmap.getWidth() - cropX);
            cropHeight = Math.min(cropHeight, originalBitmap.getHeight() - cropY);
            
            Log.d(TAG, "Cropping bitmap: " + cropX + "," + cropY + " " + cropWidth + "x" + cropHeight);
            
            // Crop the bitmap
            Bitmap croppedBitmap = Bitmap.createBitmap(originalBitmap, cropX, cropY, cropWidth, cropHeight);
            
            // Clean up original bitmap if it's different from cropped
            if (croppedBitmap != originalBitmap) {
                originalBitmap.recycle();
            }
            
            return croppedBitmap;
            
        } catch (Exception e) {
            Log.e(TAG, "Error cropping bitmap: " + e.getMessage());
            return originalBitmap;
        }
    }
    
    private void extractIdNumber(Bitmap bitmap, String imagePath) {
        // Use Philippine ID API for better field extraction
        callPhilippineIdFieldExtractionAPI(bitmap, imagePath);
    }
    
    private void callPhilippineIdFieldExtractionAPI(Bitmap bitmap, String imagePath) {
        // Convert bitmap to base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] imageBytes = baos.toByteArray();
        String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        String dataUrl = "data:image/jpeg;base64," + base64Image;

        // Prepare JSON payload
        JSONObject jsonPayload = new JSONObject();
        try {
            jsonPayload.put("image", dataUrl);
            jsonPayload.put("side", idType); // "front" or "back"
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON payload for field extraction: " + e.getMessage());
            // Fallback to local extraction
            performLocalIdNumberExtraction(bitmap, imagePath);
            return;
        }

        // Make API call
        StringRequest request = new StringRequest(
            Request.Method.POST,
            "https://boardease.calapebohol.com/philippine_id_detection_api.php",
            response -> {
                Log.d(TAG, "Philippine ID Field Extraction Response: " + response);
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    boolean success = jsonResponse.getBoolean("success");
                    
                    if (success) {
                        JSONObject data = jsonResponse.getJSONObject("data");
                        boolean isValid = data.getBoolean("isValid");
                        String detectedIdType = data.getString("idType");
                        JSONObject extractedFields = data.getJSONObject("extractedFields");
                        
                        Log.d(TAG, "API Detection - Valid: " + isValid + ", Type: " + detectedIdType);
                        
                        if (isValid) {
                            // Extract ID number from API response
                            final String idNumber;
                            if (extractedFields.has("id_number")) {
                                idNumber = extractedFields.getString("id_number");
                                Log.d(TAG, "ID number extracted from API: " + idNumber);
                            } else {
                                idNumber = "";
                            }
                            
                            // Log other extracted fields for debugging
                            Log.d(TAG, "All extracted fields: " + extractedFields.toString());
                            
                            // Show success and return to form
                            runOnUiThread(() -> {
                                tvStatus.setText("✅ " + detectedIdType + " captured successfully! Returning to form...");
                                hideGreenFrame();
                                
                                // Store the captured data
                                capturedImagePath = imagePath;
                                capturedIdNumber = idNumber;
                                
                                // Wait a moment then return to form
                                new Handler().postDelayed(() -> {
                                    Intent resultIntent = new Intent();
                                    resultIntent.putExtra("image_path", capturedImagePath);
                                    resultIntent.putExtra("id_number", capturedIdNumber);
                                    resultIntent.putExtra("id_type", idType);
                                    setResult(Activity.RESULT_OK, resultIntent);
                                    Log.d(TAG, "Returning to form with captured result: " + capturedImagePath);
                                    finish();
                                }, 2000);
                            });
                        } else {
                            runOnUiThread(() -> {
                                hideGreenFrame();
                                showError("Please capture a valid ID document. Make sure the ID is clear and readable.");
                                
                                // Reset processing state and return to camera preview
                                isProcessing = false;
                                isCapturing = true; // Re-enable capturing mode
                                processingOverlay.setVisibility(View.GONE);
                                tvStatus.setText("Position your " + idType + " ID in the frame");
                                
                                // Restart camera preview
                                restartCameraPreview();
                            });
                        }
                    } else {
                        String error = jsonResponse.getString("error");
                        Log.e(TAG, "Philippine ID Field Extraction Error: " + error);
                        // Fallback to local extraction
                        performLocalIdNumberExtraction(bitmap, imagePath);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing Philippine ID field extraction response: " + e.getMessage());
                    // Fallback to local extraction
                    performLocalIdNumberExtraction(bitmap, imagePath);
                }
            },
            error -> {
                Log.e(TAG, "Philippine ID field extraction request failed: " + error.getMessage());
                // Fallback to local extraction
                performLocalIdNumberExtraction(bitmap, imagePath);
            }
        ) {
            @Override
            public byte[] getBody() {
                try {
                    return jsonPayload.toString().getBytes("utf-8");
                } catch (Exception e) {
                    Log.e(TAG, "Error creating field extraction request body: " + e.getMessage());
                    return null;
                }
            }
            
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
            
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        // Add request to queue
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
    
    private void performLocalIdNumberExtraction(Bitmap bitmap, String imagePath) {
        try {
            TextRecognizer textRecognizer = new TextRecognizer.Builder(this).build();
            
            if (!textRecognizer.isOperational()) {
                Log.e(TAG, "TextRecognizer is not operational");
                runOnUiThread(() -> {
                    hideGreenFrame();
                    showError("OCR not available");
                });
                return;
            }
            
            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            android.util.SparseArray<TextBlock> textBlocks = textRecognizer.detect(frame);
            
            StringBuilder extractedText = new StringBuilder();
            for (int i = 0; i < textBlocks.size(); i++) {
                TextBlock textBlock = textBlocks.valueAt(i);
                extractedText.append(textBlock.getValue()).append(" ");
            }
            
            String fullText = extractedText.toString();
            Log.d(TAG, "Local OCR extracted text: " + fullText);
            
            // Check if this is actually an ID document
            if (!isValidIdDocument(fullText)) {
                Log.d(TAG, "Not a valid ID document detected");
                runOnUiThread(() -> {
                    hideGreenFrame();
                    showError("Please capture a valid ID document. Make sure the ID is clear and readable.");
                    
                    // Reset processing state and return to camera preview
                    isProcessing = false;
                    isCapturing = true; // Re-enable capturing mode
                    processingOverlay.setVisibility(View.GONE);
                    tvStatus.setText("Position your " + idType + " ID in the frame");
                    
                    // Restart camera preview
                    restartCameraPreview();
                });
                return;
            }
            
            // Only extract ID number for front ID, skip for back ID
            final String idNumber;
            if ("front".equals(idType)) {
                idNumber = extractIdNumberFromText(fullText);
                Log.d(TAG, "Local extracted ID number from front ID: '" + idNumber + "'");
            } else {
                idNumber = "";
                Log.d(TAG, "Skipping ID number extraction for back ID");
            }
            
            // Show success message and return to form
            runOnUiThread(() -> {
                tvStatus.setText("✅ ID captured successfully! Returning to form...");
                hideGreenFrame();
                
                // Store the captured data
                capturedImagePath = imagePath;
                capturedIdNumber = idNumber != null ? idNumber : "";
                
                // Wait a moment then return to form
                new Handler().postDelayed(() -> {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("image_path", capturedImagePath);
                    resultIntent.putExtra("id_number", capturedIdNumber);
                    resultIntent.putExtra("id_type", idType);
                    setResult(Activity.RESULT_OK, resultIntent);
                    Log.d(TAG, "Returning to form with captured result: " + capturedImagePath);
                    finish();
                }, 2000); // Wait 2 seconds to show success message
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error in local ID number extraction: " + e.getMessage());
            runOnUiThread(() -> {
                hideGreenFrame();
                showError("Failed to extract ID number");
                
                // Reset processing state and return to camera preview
                isProcessing = false;
                isCapturing = true; // Re-enable capturing mode
                processingOverlay.setVisibility(View.GONE);
                tvStatus.setText("Position your " + idType + " ID in the frame");
                
                // Restart camera preview
                restartCameraPreview();
            });
        }
    }
    
    private String extractIdNumberFromText(String text) {
        Log.d(TAG, "Extracting ID number from text: " + text);
        
        // Clean the text - remove extra spaces, normalize, and remove common OCR artifacts
        String cleanText = text.replaceAll("\\s+", " ").trim();
        // Remove common OCR artifacts that might break digit sequences, but keep hyphens
        cleanText = cleanText.replaceAll("[^\\w\\d\\s-]", " "); // Keep only word chars, digits, spaces, and hyphens
        cleanText = cleanText.replaceAll("\\s+", " ").trim(); // Clean up spaces again
        
        // Special handling for hyphenated ID numbers - ensure proper spacing around hyphens
        cleanText = cleanText.replaceAll("\\s*-\\s*", "-"); // Remove spaces around hyphens
        
        // Common patterns for ID numbers in the Philippines
        String[] patterns = {
            // Philippine Passport: 2 letters + 7 digits
            "\\b[A-Z]{2}\\d{7}\\b",
            // Driver's License: Various formats with letter prefixes
            "\\b[A-Z]\\d{2}-\\d{2}-\\d{6}\\b",    // Format: G03-20-000299 (1 letter + 2 digits + 2 digits + 6 digits)
            "\\b[A-Z]{2}\\d{2}-\\d{2}-\\d{6}\\b", // Format: AB03-20-000299 (2 letters + 2 digits + 2 digits + 6 digits)
            "\\b[A-Z]\\d{2}-\\d{6}\\b",           // Format: G03-20000299 (1 letter + 2 digits + 6 digits)
            "\\b[A-Z]{2}\\d{2}-\\d{6}\\b",        // Format: AB03-20000299 (2 letters + 2 digits + 6 digits)
            "\\b[A-Z]\\d{2}\\d{2}\\d{6}\\b",      // Format: G0320000299 (1 letter + 2 digits + 2 digits + 6 digits)
            "\\b[A-Z]{2}\\d{2}\\d{2}\\d{6}\\b",   // Format: AB0320000299 (2 letters + 2 digits + 2 digits + 6 digits)
            // Driver's License: Numeric-only formats
            "\\b\\d{4}-\\d{4}-\\d{4}-\\d{4}\\b",  // Format: 1234-5678-9012-3456 (16 digits)
            "\\b\\d{4}-\\d{4}-\\d{4}\\b",  // Format: 1234-5678-9012 (12 digits)
            "\\b\\d{2}-\\d{6}\\b",          // Format: 12-345678
            "\\b\\d{3}-\\d{6}\\b",          // Format: 123-456789
            "\\b\\d{4}\\s\\d{4}\\s\\d{4}\\s\\d{4}\\b", // Format: 1234 5678 9012 3456 (16 digits)
            "\\b\\d{4}\\s\\d{4}\\s\\d{4}\\b", // Format: 1234 5678 9012 (12 digits)
            // PhilID (National ID): 12 digits
            "\\b\\d{12}\\b",
            // National ID (New): 16 digits
            "\\b\\d{16}\\b",
            // National ID (Extended): 17-20 digits
            "\\b\\d{17,20}\\b",
            // UMID: 12 digits
            "\\b\\d{12}\\b",
            // SSS ID: 10 digits
            "\\b\\d{10}\\b",
            // GSIS e-card: 12 digits
            "\\b\\d{12}\\b",
            // PhilHealth ID: 12 digits
            "\\b\\d{12}\\b",
            // TIN ID: 9-12 digits
            "\\b\\d{9,12}\\b",
            // Voter's ID: Various formats
            "\\b\\d{8}\\b",                 // 8 digits
            "\\b\\d{10}\\b",                // 10 digits
            // Postal ID: Various formats
            "\\b[A-Z]{2}\\d{6}\\b",        // Format: AB123456
            "\\b\\d{6}\\b",                 // 6 digits
            // Generic patterns - more flexible (updated to include longer IDs)
            "\\b\\d{4,20}\\b",              // 4-20 digits
            // Flexible hyphenated patterns
            "\\b(\\d{4}-)+\\d{4}\\b"        // Any number of 4-digit groups with hyphens
        };
        
        // Try each pattern and return the first match
        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(cleanText);
            if (m.find()) {
                String idNumber = m.group().trim();
                Log.d(TAG, "Found ID number with pattern " + pattern + ": " + idNumber);
                return idNumber;
            }
        }
        
        // Try case-insensitive patterns
        String[] caseInsensitivePatterns = {
            // Driver's License: Case-insensitive letter prefixes
            "\\b[a-zA-Z]\\d{2}-\\d{2}-\\d{6}\\b",    // Format: G03-20-000299 (case-insensitive)
            "\\b[a-zA-Z]{2}\\d{2}-\\d{2}-\\d{6}\\b", // Format: AB03-20-000299 (case-insensitive)
            "\\b[a-zA-Z]\\d{2}-\\d{6}\\b",           // Format: G03-20000299 (case-insensitive)
            "\\b[a-zA-Z]{2}\\d{2}-\\d{6}\\b",        // Format: AB03-20000299 (case-insensitive)
            "\\b[a-zA-Z]\\d{2}\\d{2}\\d{6}\\b",      // Format: G0320000299 (case-insensitive)
            "\\b[a-zA-Z]{2}\\d{2}\\d{2}\\d{6}\\b",   // Format: AB0320000299 (case-insensitive)
            "\\b\\d{4,20}\\b"  // 4-20 digits anywhere
        };
        
        for (String pattern : caseInsensitivePatterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(cleanText);
            if (m.find()) {
                String idNumber = m.group().trim();
                Log.d(TAG, "Found ID number with case-insensitive pattern: " + idNumber);
                return idNumber;
            }
        }
        
        // If no pattern matches, look for any sequence of 4+ digits anywhere
        java.util.regex.Pattern fallbackPattern = java.util.regex.Pattern.compile("\\d{4,20}");
        java.util.regex.Matcher fallbackMatcher = fallbackPattern.matcher(cleanText);
        if (fallbackMatcher.find()) {
            String idNumber = fallbackMatcher.group().trim();
            Log.d(TAG, "Found ID number with fallback pattern: " + idNumber);
            return idNumber;
        }
        
        // Try to find multiple digit sequences and combine them
        java.util.regex.Pattern multiDigitPattern = java.util.regex.Pattern.compile("\\d{3,}");
        java.util.regex.Matcher multiDigitMatcher = multiDigitPattern.matcher(cleanText);
        StringBuilder combinedDigits = new StringBuilder();
        while (multiDigitMatcher.find()) {
            combinedDigits.append(multiDigitMatcher.group());
        }
        
        if (combinedDigits.length() >= 4) {
            String combinedIdNumber = combinedDigits.toString();
            Log.d(TAG, "Found combined ID number: " + combinedIdNumber);
            return combinedIdNumber;
        }
        
        // Last resort: find the longest continuous digit sequence
        java.util.regex.Pattern longestDigitPattern = java.util.regex.Pattern.compile("\\d+");
        java.util.regex.Matcher longestDigitMatcher = longestDigitPattern.matcher(cleanText);
        String longestSequence = "";
        while (longestDigitMatcher.find()) {
            String currentSequence = longestDigitMatcher.group();
            if (currentSequence.length() > longestSequence.length()) {
                longestSequence = currentSequence;
            }
        }
        
        if (longestSequence.length() >= 4) {
            Log.d(TAG, "Found longest digit sequence: " + longestSequence);
            return longestSequence;
        }
        
        Log.d(TAG, "No ID number pattern found in text: " + cleanText);
        return "";
    }
    
    private boolean isValidIdDocument(String text) {
        if (text == null || text.trim().isEmpty()) {
            Log.d(TAG, "No text extracted - not a valid ID");
            return false;
        }
        
        String textLower = text.toLowerCase();
        
            // Check for ID-related keywords
            String[] idKeywords = {
                "republic of the philippines",
                "philippine passport",
                "driver's license",
                "philid",
                "national id",
                "philippine national id",
                "phil national id",
                "umid",
                "sss",
                "gsis",
                "philhealth",
                "tin",
                "voter's",
                "postal",
                "license",
                "passport",
                "identification",
                "id no",
                "id number",
                "control no",
                "control number",
                "date of birth",
                "place of birth",
                "address",
                "citizenship",
                "sex",
                "civil status",
                "height",
                "weight",
                "blood type",
                "valid until",
                "expiry",
                "expires",
                "philid number",
                "national id number"
            };
        
        int keywordCount = 0;
        for (String keyword : idKeywords) {
            if (textLower.contains(keyword)) {
                keywordCount++;
                Log.d(TAG, "Found ID keyword: " + keyword);
            }
        }
        
        // Must have at least 2 ID-related keywords
        boolean hasIdKeywords = keywordCount >= 2;
        
        // Check for ID number patterns
        boolean hasIdNumber = !extractIdNumberFromText(text).isEmpty();
        
        // Check for personal information patterns
        boolean hasPersonalInfo = textLower.contains("name") || 
                                 textLower.contains("address") || 
                                 textLower.contains("birth") ||
                                 textLower.contains("sex") ||
                                 textLower.contains("citizenship");
        
        boolean isValid = hasIdKeywords || hasIdNumber || hasPersonalInfo;
        
        Log.d(TAG, "ID Validation - Keywords: " + keywordCount + ", HasIDNumber: " + hasIdNumber + 
                   ", HasPersonalInfo: " + hasPersonalInfo + ", IsValid: " + isValid);
        
        return isValid;
    }
    
    private void showError(String message) {
        isProcessing = false;
        processingOverlay.setVisibility(View.GONE);
        tvStatus.setText("Error: " + message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isCapturing = false;
        stopIdDetection();
    }
}
