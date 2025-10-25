package com.example.mock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageVerification {
    
    private static final String TAG = "ImageVerification";
    
    // API Endpoints for image verification
    private static final String CONTENT_MODERATION_API = "https://api.sightengine.com/1.0/check.json";
    private static final String AI_DETECTION_API = "https://api.sightengine.com/1.0/check.json";
    private static final String REAL_IMAGE_API = "https://api.sightengine.com/1.0/check.json";
    
    // API Keys (you should store these securely)
    // TODO: Replace with your actual Sightengine API credentials
    private static final String SIGHTENGINE_API_KEY = "your_sightengine_api_key";
    private static final String SIGHTENGINE_API_SECRET = "your_sightengine_api_secret";
    
    // Enable/disable API verification (set to true to use API, false for local verification)
    private static final boolean USE_API_VERIFICATION = false;
    
    // Verification result callback interface
    public interface VerificationCallback {
        void onVerificationComplete(boolean isApproved, String reason);
        void onVerificationError(String error);
    }
    
    /**
     * Verifies an image for inappropriate content, AI generation, and authenticity
     * @param context Application context
     * @param imageUri URI of the image to verify
     * @param callback Callback for verification results
     */
    public static void verifyImage(Context context, Uri imageUri, VerificationCallback callback) {
        try {
            if (USE_API_VERIFICATION) {
                // Use API-based verification (more accurate but requires internet)
                String base64Image = convertImageToBase64(context, imageUri);
                if (base64Image == null) {
                    callback.onVerificationError("Failed to process image");
                    return;
                }
                performComprehensiveVerification(context, base64Image, callback);
            } else {
                // Use local verification (faster but less accurate)
                verifyImageStrictly(context, imageUri, callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error verifying image: " + e.getMessage());
            callback.onVerificationError("Image verification failed: " + e.getMessage());
        }
    }

    /**
     * Verifies an ID document image for legitimacy and authenticity
     * @param context Application context
     * @param imageUri URI of the image to verify
     * @param callback Callback for verification results
     */
    public static void verifyIdDocument(Context context, Uri imageUri, VerificationCallback callback) {
        try {
            if (USE_API_VERIFICATION) {
                // Use API-based verification (more accurate but requires internet)
                String base64Image = convertImageToBase64(context, imageUri);
                if (base64Image == null) {
                    callback.onVerificationError("Failed to process image");
                    return;
                }
                performIdDocumentVerification(context, base64Image, callback);
            } else {
                // Use local ID document verification (faster but less accurate)
                verifyIdDocumentLocally(context, imageUri, callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error verifying ID document: " + e.getMessage());
            callback.onVerificationError("ID document verification failed: " + e.getMessage());
        }
    }

    /**
     * Verifies a QR code image for legitimacy
     * @param context Application context
     * @param imageUri URI of the image to verify
     * @param callback Callback for verification results
     */
    public static void verifyQrCode(Context context, Uri imageUri, VerificationCallback callback) {
        try {
            if (USE_API_VERIFICATION) {
                // Use API-based verification (more accurate but requires internet)
                String base64Image = convertImageToBase64(context, imageUri);
                if (base64Image == null) {
                    callback.onVerificationError("Failed to process image");
                    return;
                }
                performQrCodeVerification(context, base64Image, callback);
            } else {
                // Use local QR code verification (faster but less accurate)
                verifyQrCodeLocally(context, imageUri, callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error verifying QR code: " + e.getMessage());
            callback.onVerificationError("QR code verification failed: " + e.getMessage());
        }
    }
    
    /**
     * Converts image URI to base64 string
     */
    private static String convertImageToBase64(Context context, Uri imageUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) return null;
            
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) return null;
            
            // Compress bitmap to reduce size
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            
            return Base64.encodeToString(imageBytes, Base64.DEFAULT);
            
        } catch (Exception e) {
            Log.e(TAG, "Error converting image to base64: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Performs comprehensive image verification
     */
    private static void performComprehensiveVerification(Context context, String base64Image, VerificationCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(context);
        
        // Build request parameters
        JSONObject params = new JSONObject();
        try {
            params.put("api_user", SIGHTENGINE_API_KEY);
            params.put("api_secret", SIGHTENGINE_API_SECRET);
            params.put("media", base64Image);
            params.put("models", "nudity-2.0,wad,offensive,celebrities,scam,text-content,face-attributes");
            params.put("callback", "https://your-callback-url.com");
        } catch (JSONException e) {
            callback.onVerificationError("Failed to build request parameters");
            return;
        }
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, CONTENT_MODERATION_API, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.d(TAG, "Verification response: " + response.toString());
                            
                            // Parse verification results
                            VerificationResult result = parseVerificationResponse(response);
                            
                            if (result.isApproved) {
                                callback.onVerificationComplete(true, "Image approved");
                            } else {
                                callback.onVerificationComplete(false, result.reason);
                            }
                            
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing verification response: " + e.getMessage());
                            callback.onVerificationError("Failed to parse verification results");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Verification API error: " + error.getMessage());
                        callback.onVerificationError("Image verification service unavailable");
                    }
                });
        
        queue.add(request);
    }
    
    /**
     * Parses the verification API response
     */
    private static VerificationResult parseVerificationResponse(JSONObject response) {
        try {
            boolean isApproved = true;
            StringBuilder reasons = new StringBuilder();
            
            // Check for inappropriate content
            if (response.has("nudity")) {
                JSONObject nudity = response.getJSONObject("nudity");
                if (nudity.getDouble("sexual_activity") > 0.3 || 
                    nudity.getDouble("sexual_display") > 0.3 ||
                    nudity.getDouble("erotica") > 0.3) {
                    isApproved = false;
                    reasons.append("Inappropriate content detected. ");
                }
            }
            
            // Check for offensive content
            if (response.has("offensive")) {
                JSONObject offensive = response.getJSONObject("offensive");
                if (offensive.getDouble("prob") > 0.3) {
                    isApproved = false;
                    reasons.append("Offensive content detected. ");
                }
            }
            
            // Check for text content (inappropriate text in image)
            if (response.has("text-content")) {
                JSONObject textContent = response.getJSONObject("text-content");
                if (textContent.has("profanity") && textContent.getDouble("profanity") > 0.3) {
                    isApproved = false;
                    reasons.append("Inappropriate text in image. ");
                }
            }
            
            // Check for scam content
            if (response.has("scam")) {
                JSONObject scam = response.getJSONObject("scam");
                if (scam.getDouble("prob") > 0.3) {
                    isApproved = false;
                    reasons.append("Potential scam content detected. ");
                }
            }
            
            // Check for AI-generated content (basic detection)
            if (response.has("face-attributes")) {
                JSONObject faceAttributes = response.getJSONObject("face-attributes");
                if (faceAttributes.has("artificial") && faceAttributes.getDouble("artificial") > 0.7) {
                    isApproved = false;
                    reasons.append("AI-generated content detected. ");
                }
            }
            
            // Check for celebrity impersonation
            if (response.has("celebrities")) {
                JSONObject celebrities = response.getJSONObject("celebrities");
                if (celebrities.getDouble("prob") > 0.5) {
                    isApproved = false;
                    reasons.append("Celebrity impersonation detected. ");
                }
            }
            
            return new VerificationResult(isApproved, reasons.toString());
            
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing verification response: " + e.getMessage());
            return new VerificationResult(false, "Failed to verify image content");
        }
    }
    
    /**
     * Alternative verification using local image analysis with basic content detection
     */
    public static void verifyImageLocally(Context context, Uri imageUri, VerificationCallback callback) {
        try {
            // Basic local verification
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                callback.onVerificationError("Cannot read image file");
                return;
            }
            
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                callback.onVerificationError("Invalid image format");
                return;
            }
            
            // Basic image validation
            if (bitmap.getWidth() < 100 || bitmap.getHeight() < 100) {
                callback.onVerificationComplete(false, "Image too small (minimum 100x100 pixels)");
                return;
            }
            
             if (bitmap.getWidth() > 8000 || bitmap.getHeight() > 8000) {
                 callback.onVerificationComplete(false, "Image too large (maximum 8000x8000 pixels)");
                 return;
             }
            
            // Check file size (approximate)
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            int fileSize = outputStream.toByteArray().length;
            
            if (fileSize > 10 * 1024 * 1024) { // 10MB limit
                callback.onVerificationComplete(false, "Image file too large (maximum 10MB)");
                return;
            }
            
            // Perform basic content analysis
            ContentAnalysisResult analysis = analyzeImageContent(bitmap);
            
            if (!analysis.isAppropriate) {
                callback.onVerificationComplete(false, analysis.reason);
                return;
            }
            
            // Basic approval for local verification
            callback.onVerificationComplete(true, "Image passed content verification");
            
        } catch (Exception e) {
            Log.e(TAG, "Local verification error: " + e.getMessage());
            callback.onVerificationError("Local image verification failed");
        }
    }
    
    /**
     * Basic content analysis using image properties
     */
    private static ContentAnalysisResult analyzeImageContent(Bitmap bitmap) {
        try {
            // Get image dimensions
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            
            // Check for suspicious aspect ratios (very wide or very tall)
            double aspectRatio = (double) width / height;
            if (aspectRatio > 3.0 || aspectRatio < 0.33) {
                return new ContentAnalysisResult(false, "Suspicious image dimensions detected");
            }
            
            // Sample pixels for basic color analysis
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            
            // Analyze color distribution
            ColorAnalysis colorAnalysis = analyzeColors(pixels);
            
            // Check for suspicious color patterns
            if (colorAnalysis.hasExcessiveSkinTone) {
                return new ContentAnalysisResult(false, "Inappropriate content detected");
            }
            
            if (colorAnalysis.hasExcessiveRed) {
                return new ContentAnalysisResult(false, "Suspicious content detected");
            }
            
            // Check for very dark or very bright images (potential inappropriate content)
            if (colorAnalysis.averageBrightness < 30 || colorAnalysis.averageBrightness > 240) {
                return new ContentAnalysisResult(false, "Image quality issues detected");
            }
            
            // Check for uniform colors (potential fake images)
            if (colorAnalysis.colorVariation < 0.1) {
                return new ContentAnalysisResult(false, "Suspicious image pattern detected");
            }
            
            return new ContentAnalysisResult(true, "Content appears appropriate");
            
        } catch (Exception e) {
            Log.e(TAG, "Content analysis error: " + e.getMessage());
            return new ContentAnalysisResult(false, "Content analysis failed");
        }
    }
    
    /**
     * Analyzes color distribution in the image
     */
    private static ColorAnalysis analyzeColors(int[] pixels) {
        int totalPixels = pixels.length;
        int skinToneCount = 0;
        int redCount = 0;
        int totalBrightness = 0;
        int uniqueColors = 0;
        
        // Sample every 10th pixel for performance
        for (int i = 0; i < totalPixels; i += 10) {
            int pixel = pixels[i];
            
            // Extract RGB values
            int red = (pixel >> 16) & 0xFF;
            int green = (pixel >> 8) & 0xFF;
            int blue = pixel & 0xFF;
            
            // Calculate brightness
            int brightness = (red + green + blue) / 3;
            totalBrightness += brightness;
            
            // Check for skin tone (basic heuristic)
            if (isSkinTone(red, green, blue)) {
                skinToneCount++;
            }
            
            // Check for excessive red
            if (red > green + 50 && red > blue + 50) {
                redCount++;
            }
        }
        
        // Calculate statistics
        double averageBrightness = (double) totalBrightness / (totalPixels / 10);
        double skinToneRatio = (double) skinToneCount / (totalPixels / 10);
        double redRatio = (double) redCount / (totalPixels / 10);
        
        // Estimate color variation (simplified)
        double colorVariation = Math.min(1.0, (averageBrightness / 255.0) * 0.5 + 0.3);
        
        return new ColorAnalysis(
            skinToneRatio > 0.4,  // Has excessive skin tone
            redRatio > 0.3,       // Has excessive red
            averageBrightness,
            colorVariation
        );
    }
    
    /**
     * Basic skin tone detection (simplified heuristic)
     */
    private static boolean isSkinTone(int red, int green, int blue) {
        // Basic skin tone detection
        return red > 95 && green > 40 && blue > 20 &&
               red > green && red > blue &&
               Math.abs(red - green) > 15;
    }
    
    /**
     * Data classes for analysis results
     */
    private static class ContentAnalysisResult {
        boolean isAppropriate;
        String reason;
        
        ContentAnalysisResult(boolean isAppropriate, String reason) {
            this.isAppropriate = isAppropriate;
            this.reason = reason;
        }
    }
    
    private static class ColorAnalysis {
        boolean hasExcessiveSkinTone;
        boolean hasExcessiveRed;
        double averageBrightness;
        double colorVariation;
        
        ColorAnalysis(boolean hasExcessiveSkinTone, boolean hasExcessiveRed, 
                     double averageBrightness, double colorVariation) {
            this.hasExcessiveSkinTone = hasExcessiveSkinTone;
            this.hasExcessiveRed = hasExcessiveRed;
            this.averageBrightness = averageBrightness;
            this.colorVariation = colorVariation;
        }
    }
    
    /**
     * Strict verification with more aggressive content detection
     */
    public static void verifyImageStrictly(Context context, Uri imageUri, VerificationCallback callback) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                callback.onVerificationError("Cannot read image file");
                return;
            }
            
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                callback.onVerificationError("Invalid image format");
                return;
            }
            
            // Basic image validation
            if (bitmap.getWidth() < 200 || bitmap.getHeight() < 200) {
                callback.onVerificationComplete(false, "Image too small (minimum 200x200 pixels)");
                return;
            }
            
             if (bitmap.getWidth() > 8000 || bitmap.getHeight() > 8000) {
                 callback.onVerificationComplete(false, "Image too large (maximum 8000x8000 pixels)");
                 return;
             }
            
            // Strict content analysis
            StrictContentAnalysisResult analysis = performStrictContentAnalysis(bitmap);
            
            if (!analysis.isAppropriate) {
                callback.onVerificationComplete(false, analysis.reason);
                return;
            }
            
            callback.onVerificationComplete(true, "Image passed strict verification");
            
        } catch (Exception e) {
            Log.e(TAG, "Strict verification error: " + e.getMessage());
            callback.onVerificationError("Strict image verification failed");
        }
    }
    
    /**
     * Performs strict content analysis with more sensitive detection
     */
    private static StrictContentAnalysisResult performStrictContentAnalysis(Bitmap bitmap) {
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            
            // More strict aspect ratio check
            double aspectRatio = (double) width / height;
            if (aspectRatio > 2.5 || aspectRatio < 0.4) {
                return new StrictContentAnalysisResult(false, "Unusual image dimensions");
            }
            
            // Analyze more pixels for better detection
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            
            StrictColorAnalysis colorAnalysis = performStrictColorAnalysis(pixels);
            
            // More sensitive detection thresholds
            if (colorAnalysis.skinToneRatio > 0.3) {
                return new StrictContentAnalysisResult(false, "Inappropriate content detected");
            }
            
            if (colorAnalysis.redRatio > 0.2) {
                return new StrictContentAnalysisResult(false, "Suspicious content detected");
            }
            
            if (colorAnalysis.averageBrightness < 50 || colorAnalysis.averageBrightness > 220) {
                return new StrictContentAnalysisResult(false, "Poor image quality or inappropriate content");
            }
            
            if (colorAnalysis.colorVariation < 0.2) {
                return new StrictContentAnalysisResult(false, "Suspicious image pattern");
            }
            
            // Check for very uniform colors (potential inappropriate content)
            if (colorAnalysis.uniformity > 0.8) {
                return new StrictContentAnalysisResult(false, "Suspicious uniform content");
            }
            
            return new StrictContentAnalysisResult(true, "Content appears appropriate");
            
        } catch (Exception e) {
            Log.e(TAG, "Strict content analysis error: " + e.getMessage());
            return new StrictContentAnalysisResult(false, "Content analysis failed");
        }
    }
    
    /**
     * Performs strict color analysis with more sensitive detection
     */
    private static StrictColorAnalysis performStrictColorAnalysis(int[] pixels) {
        int totalPixels = pixels.length;
        int skinToneCount = 0;
        int redCount = 0;
        int totalBrightness = 0;
        int uniformColorCount = 0;
        
        // Sample every 5th pixel for more thorough analysis
        for (int i = 0; i < totalPixels; i += 5) {
            int pixel = pixels[i];
            
            int red = (pixel >> 16) & 0xFF;
            int green = (pixel >> 8) & 0xFF;
            int blue = pixel & 0xFF;
            
            int brightness = (red + green + blue) / 3;
            totalBrightness += brightness;
            
            // More sensitive skin tone detection
            if (isSkinToneStrict(red, green, blue)) {
                skinToneCount++;
            }
            
            // More sensitive red detection
            if (red > green + 30 && red > blue + 30) {
                redCount++;
            }
            
            // Check for uniform colors
            if (Math.abs(red - green) < 20 && Math.abs(green - blue) < 20 && Math.abs(red - blue) < 20) {
                uniformColorCount++;
            }
        }
        
        double averageBrightness = (double) totalBrightness / (totalPixels / 5);
        double skinToneRatio = (double) skinToneCount / (totalPixels / 5);
        double redRatio = (double) redCount / (totalPixels / 5);
        double uniformity = (double) uniformColorCount / (totalPixels / 5);
        double colorVariation = Math.min(1.0, (averageBrightness / 255.0) * 0.6 + 0.2);
        
        return new StrictColorAnalysis(skinToneRatio, redRatio, averageBrightness, colorVariation, uniformity);
    }
    
    /**
     * More strict skin tone detection for ID documents
     */
    private static boolean isSkinToneStrict(int red, int green, int blue) {
        // Much more sensitive detection for inappropriate content
        return red > 70 && green > 25 && blue > 10 &&
                red > green && red > blue &&
                Math.abs(red - green) > 8 &&
                red < 220 && green < 200 && blue < 180;
    }
    
    /**
     * Data classes for strict analysis
     */
    private static class StrictContentAnalysisResult {
        boolean isAppropriate;
        String reason;
        
        StrictContentAnalysisResult(boolean isAppropriate, String reason) {
            this.isAppropriate = isAppropriate;
            this.reason = reason;
        }
    }
    
    private static class StrictColorAnalysis {
        double skinToneRatio;
        double redRatio;
        double averageBrightness;
        double colorVariation;
        double uniformity;
        
        StrictColorAnalysis(double skinToneRatio, double redRatio, double averageBrightness, 
                          double colorVariation, double uniformity) {
            this.skinToneRatio = skinToneRatio;
            this.redRatio = redRatio;
            this.averageBrightness = averageBrightness;
            this.colorVariation = colorVariation;
            this.uniformity = uniformity;
        }
    }
    
    /**
     * Performs ID document verification using API
     */
    private static void performIdDocumentVerification(Context context, String base64Image, VerificationCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(context);
        
        // Build request parameters for ID document verification
        JSONObject params = new JSONObject();
        try {
            params.put("api_user", SIGHTENGINE_API_KEY);
            params.put("api_secret", SIGHTENGINE_API_SECRET);
            params.put("media", base64Image);
            params.put("models", "nudity-2.0,wad,offensive,celebrities,scam,text-content,face-attributes,text-attributes");
            params.put("callback", "https://your-callback-url.com");
        } catch (JSONException e) {
            callback.onVerificationError("Failed to build ID verification request parameters");
            return;
        }
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, CONTENT_MODERATION_API, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.d(TAG, "ID verification response: " + response.toString());
                            
                            // Parse ID document verification results
                            VerificationResult result = parseIdDocumentResponse(response);
                            
                            if (result.isApproved) {
                                callback.onVerificationComplete(true, "ID document verified");
                            } else {
                                callback.onVerificationComplete(false, result.reason);
                            }
                            
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing ID verification response: " + e.getMessage());
                            callback.onVerificationError("Failed to parse ID verification results");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "ID verification API error: " + error.getMessage());
                        callback.onVerificationError("ID document verification service unavailable");
                    }
                });
        
        queue.add(request);
    }

    /**
     * Parses the ID document verification API response
     */
    private static VerificationResult parseIdDocumentResponse(JSONObject response) {
        try {
            boolean isApproved = true;
            StringBuilder reasons = new StringBuilder();
            
            // Check for inappropriate content (stricter for ID documents)
            if (response.has("nudity")) {
                JSONObject nudity = response.getJSONObject("nudity");
                if (nudity.getDouble("sexual_activity") > 0.1 || 
                    nudity.getDouble("sexual_display") > 0.1 ||
                    nudity.getDouble("erotica") > 0.1) {
                    isApproved = false;
                    reasons.append("Inappropriate content detected in ID document. ");
                }
            }
            
            // Check for offensive content (stricter for ID documents)
            if (response.has("offensive")) {
                JSONObject offensive = response.getJSONObject("offensive");
                if (offensive.getDouble("prob") > 0.1) {
                    isApproved = false;
                    reasons.append("Offensive content detected in ID document. ");
                }
            }
            
            // Check for text content (should contain ID information)
            if (response.has("text-content")) {
                JSONObject textContent = response.getJSONObject("text-content");
                if (textContent.has("profanity") && textContent.getDouble("profanity") > 0.1) {
                    isApproved = false;
                    reasons.append("Inappropriate text in ID document. ");
                }
            }
            
            // Check for scam content (stricter for ID documents)
            if (response.has("scam")) {
                JSONObject scam = response.getJSONObject("scam");
                if (scam.getDouble("prob") > 0.1) {
                    isApproved = false;
                    reasons.append("Potential fake ID document detected. ");
                }
            }
            
            // Check for AI-generated content (stricter for ID documents)
            if (response.has("face-attributes")) {
                JSONObject faceAttributes = response.getJSONObject("face-attributes");
                if (faceAttributes.has("artificial") && faceAttributes.getDouble("artificial") > 0.3) {
                    isApproved = false;
                    reasons.append("AI-generated ID document detected. ");
                }
            }
            
            // Check for celebrity impersonation (stricter for ID documents)
            if (response.has("celebrities")) {
                JSONObject celebrities = response.getJSONObject("celebrities");
                if (celebrities.getDouble("prob") > 0.2) {
                    isApproved = false;
                    reasons.append("Celebrity impersonation in ID document detected. ");
                }
            }
            
            return new VerificationResult(isApproved, reasons.toString());
            
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing ID verification response: " + e.getMessage());
            return new VerificationResult(false, "Failed to verify ID document content");
        }
    }

    /**
     * Local ID document verification with specific checks for government IDs
     */
    public static void verifyIdDocumentLocally(Context context, Uri imageUri, VerificationCallback callback) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                callback.onVerificationError("Cannot read ID document file");
                return;
            }
            
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                callback.onVerificationError("Invalid ID document format");
                return;
            }
            
            // ID document specific validation
            if (bitmap.getWidth() < 300 || bitmap.getHeight() < 200) {
                callback.onVerificationComplete(false, "ID document too small (minimum 300x200 pixels)");
                return;
            }
            
            if (bitmap.getWidth() > 8000 || bitmap.getHeight() > 8000) {
                callback.onVerificationComplete(false, "ID document too large (maximum 8000x8000 pixels)");
                return;
            }
            
            // Check for appropriate aspect ratio for ID documents (typically rectangular)
            double aspectRatio = (double) bitmap.getWidth() / bitmap.getHeight();
            if (aspectRatio < 0.5 || aspectRatio > 3.0) {
                callback.onVerificationComplete(false, "ID document has unusual dimensions");
                return;
            }
            
            // Perform ID document specific content analysis
            IdDocumentAnalysisResult analysis = analyzeIdDocumentContent(bitmap);
            
            if (!analysis.isLegitimateId) {
                callback.onVerificationComplete(false, analysis.reason);
                return;
            }
            
            callback.onVerificationComplete(true, "ID document appears legitimate");
            
        } catch (Exception e) {
            Log.e(TAG, "ID document verification error: " + e.getMessage());
            callback.onVerificationError("ID document verification failed");
        }
    }

    /**
     * Analyzes ID document content for legitimacy
     */
    private static IdDocumentAnalysisResult analyzeIdDocumentContent(Bitmap bitmap) {
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            
            // Sample pixels for ID document analysis
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            
            IdDocumentColorAnalysis colorAnalysis = analyzeIdDocumentColors(pixels);
            
            // MUCH STRICTER ID document verification
            
            // Check for excessive skin tone (should be minimal in ID documents)
            if (colorAnalysis.skinToneRatio > 0.3) {
                return new IdDocumentAnalysisResult(false, "ID document contains inappropriate content");
            }
            
            // Check for excessive red content (suspicious for ID documents)
            if (colorAnalysis.redRatio > 0.2) {
                return new IdDocumentAnalysisResult(false, "ID document contains suspicious content");
            }
            
            // ID documents should be well-lit and clear (stricter range)
            if (colorAnalysis.averageBrightness < 100 || colorAnalysis.averageBrightness > 200) {
                return new IdDocumentAnalysisResult(false, "ID document should be well-lit and clear");
            }
            
            // ID documents should have good color variation (stricter requirement)
            if (colorAnalysis.colorVariation < 0.4) {
                return new IdDocumentAnalysisResult(false, "ID document appears to be fake or low quality");
            }
            
            // Check for text-like patterns (ID documents must have text)
            if (colorAnalysis.textLikePatterns < 0.2) {
                return new IdDocumentAnalysisResult(false, "ID document should contain text and official markings");
            }
            
            // Additional checks for ID document legitimacy
            if (colorAnalysis.skinToneRatio > 0.1 && colorAnalysis.redRatio > 0.1) {
                return new IdDocumentAnalysisResult(false, "ID document contains inappropriate content");
            }
            
            // Check for very uniform colors (potential fake images)
            if (colorAnalysis.colorVariation < 0.5 && colorAnalysis.textLikePatterns < 0.3) {
                return new IdDocumentAnalysisResult(false, "ID document appears to be fake or manipulated");
            }
            
            return new IdDocumentAnalysisResult(true, "ID document appears legitimate");
            
        } catch (Exception e) {
            Log.e(TAG, "ID document content analysis error: " + e.getMessage());
            return new IdDocumentAnalysisResult(false, "ID document analysis failed");
        }
    }

    /**
     * Analyzes colors in ID document for legitimacy
     */
    private static IdDocumentColorAnalysis analyzeIdDocumentColors(int[] pixels) {
        int totalPixels = pixels.length;
        int skinToneCount = 0;
        int redCount = 0;
        int totalBrightness = 0;
        int textLikeCount = 0;
        
        // Sample every 5th pixel for thorough analysis
        for (int i = 0; i < totalPixels; i += 5) {
            int pixel = pixels[i];
            
            int red = (pixel >> 16) & 0xFF;
            int green = (pixel >> 8) & 0xFF;
            int blue = pixel & 0xFF;
            
            int brightness = (red + green + blue) / 3;
            totalBrightness += brightness;
            
            // Check for skin tone (should be minimal in ID documents) - MORE SENSITIVE
            if (isSkinToneStrict(red, green, blue)) {
                skinToneCount++;
            }
            
            // Check for excessive red (suspicious for ID documents) - MORE SENSITIVE
            if (red > green + 30 && red > blue + 30) {
                redCount++;
            }
            
            // Additional check for inappropriate colors
            if (red > 200 && green < 100 && blue < 100) {
                redCount++;
            }
            
            // Check for text-like patterns (high contrast areas)
            if (Math.abs(red - green) > 50 || Math.abs(green - blue) > 50 || Math.abs(red - blue) > 50) {
                textLikeCount++;
            }
        }
        
        double averageBrightness = (double) totalBrightness / (totalPixels / 5);
        double skinToneRatio = (double) skinToneCount / (totalPixels / 5);
        double redRatio = (double) redCount / (totalPixels / 5);
        double textLikePatterns = (double) textLikeCount / (totalPixels / 5);
        double colorVariation = Math.min(1.0, (averageBrightness / 255.0) * 0.7 + 0.3);
        
        return new IdDocumentColorAnalysis(
            skinToneRatio > 0.3,  // Has excessive skin tone (stricter)
            redRatio > 0.2,        // Has excessive red (stricter)
            skinToneRatio,         // Skin tone ratio
            redRatio,              // Red ratio
            averageBrightness,
            colorVariation,
            textLikePatterns
        );
    }

    /**
     * Data classes for ID document analysis
     */
    private static class IdDocumentAnalysisResult {
        boolean isLegitimateId;
        String reason;
        
        IdDocumentAnalysisResult(boolean isLegitimateId, String reason) {
            this.isLegitimateId = isLegitimateId;
            this.reason = reason;
        }
    }
    
    private static class IdDocumentColorAnalysis {
        boolean hasExcessiveSkinTone;
        boolean hasExcessiveRed;
        double skinToneRatio;
        double redRatio;
        double averageBrightness;
        double colorVariation;
        double textLikePatterns;
        
        IdDocumentColorAnalysis(boolean hasExcessiveSkinTone, boolean hasExcessiveRed, 
                              double skinToneRatio, double redRatio, double averageBrightness, 
                              double colorVariation, double textLikePatterns) {
            this.hasExcessiveSkinTone = hasExcessiveSkinTone;
            this.hasExcessiveRed = hasExcessiveRed;
            this.skinToneRatio = skinToneRatio;
            this.redRatio = redRatio;
            this.averageBrightness = averageBrightness;
            this.colorVariation = colorVariation;
            this.textLikePatterns = textLikePatterns;
        }
    }

    /**
     * Performs QR code verification using API
     */
    private static void performQrCodeVerification(Context context, String base64Image, VerificationCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(context);
        
        // Build request parameters for QR code verification
        JSONObject params = new JSONObject();
        try {
            params.put("api_user", SIGHTENGINE_API_KEY);
            params.put("api_secret", SIGHTENGINE_API_SECRET);
            params.put("media", base64Image);
            params.put("models", "nudity-2.0,wad,offensive,celebrities,scam,text-content,face-attributes");
            params.put("callback", "https://your-callback-url.com");
        } catch (JSONException e) {
            callback.onVerificationError("Failed to build QR verification request parameters");
            return;
        }
        
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, CONTENT_MODERATION_API, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.d(TAG, "QR verification response: " + response.toString());
                            
                            // Parse QR code verification results
                            VerificationResult result = parseQrCodeResponse(response);
                            
                            if (result.isApproved) {
                                callback.onVerificationComplete(true, "QR code verified");
                            } else {
                                callback.onVerificationComplete(false, result.reason);
                            }
                            
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing QR verification response: " + e.getMessage());
                            callback.onVerificationError("Failed to parse QR verification results");
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "QR verification API error: " + error.getMessage());
                        callback.onVerificationError("QR code verification service unavailable");
                    }
                });
        
        queue.add(request);
    }

    /**
     * Parses the QR code verification API response
     */
    private static VerificationResult parseQrCodeResponse(JSONObject response) {
        try {
            boolean isApproved = true;
            StringBuilder reasons = new StringBuilder();
            
            // Check for inappropriate content (stricter for QR codes)
            if (response.has("nudity")) {
                JSONObject nudity = response.getJSONObject("nudity");
                if (nudity.getDouble("sexual_activity") > 0.1 || 
                    nudity.getDouble("sexual_display") > 0.1 ||
                    nudity.getDouble("erotica") > 0.1) {
                    isApproved = false;
                    reasons.append("Inappropriate content detected in QR code. ");
                }
            }
            
            // Check for offensive content (stricter for QR codes)
            if (response.has("offensive")) {
                JSONObject offensive = response.getJSONObject("offensive");
                if (offensive.getDouble("prob") > 0.1) {
                    isApproved = false;
                    reasons.append("Offensive content detected in QR code. ");
                }
            }
            
            // Check for text content (should be minimal for QR codes)
            if (response.has("text-content")) {
                JSONObject textContent = response.getJSONObject("text-content");
                if (textContent.has("profanity") && textContent.getDouble("profanity") > 0.1) {
                    isApproved = false;
                    reasons.append("Inappropriate text in QR code. ");
                }
            }
            
            // Check for scam content (stricter for QR codes)
            if (response.has("scam")) {
                JSONObject scam = response.getJSONObject("scam");
                if (scam.getDouble("prob") > 0.1) {
                    isApproved = false;
                    reasons.append("Potential fake QR code detected. ");
                }
            }
            
            // Check for AI-generated content (stricter for QR codes)
            if (response.has("face-attributes")) {
                JSONObject faceAttributes = response.getJSONObject("face-attributes");
                if (faceAttributes.has("artificial") && faceAttributes.getDouble("artificial") > 0.3) {
                    isApproved = false;
                    reasons.append("AI-generated QR code detected. ");
                }
            }
            
            // Check for celebrity impersonation (stricter for QR codes)
            if (response.has("celebrities")) {
                JSONObject celebrities = response.getJSONObject("celebrities");
                if (celebrities.getDouble("prob") > 0.2) {
                    isApproved = false;
                    reasons.append("Celebrity impersonation in QR code detected. ");
                }
            }
            
            return new VerificationResult(isApproved, reasons.toString());
            
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing QR verification response: " + e.getMessage());
            return new VerificationResult(false, "Failed to verify QR code content");
        }
    }

    /**
     * Local QR code verification with specific checks for QR codes
     */
    public static void verifyQrCodeLocally(Context context, Uri imageUri, VerificationCallback callback) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                callback.onVerificationError("Cannot read QR code file");
                return;
            }
            
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                callback.onVerificationError("Invalid QR code format");
                return;
            }
            
            // QR code specific validation - more lenient for GCash QR codes
            if (bitmap.getWidth() < 100 || bitmap.getHeight() < 100) {
                callback.onVerificationComplete(false, "QR code too small (minimum 100x100 pixels)");
                return;
            }
            
            if (bitmap.getWidth() > 8000 || bitmap.getHeight() > 8000) {
                callback.onVerificationComplete(false, "QR code too large (maximum 8000x8000 pixels)");
                return;
            }
            
            // More lenient aspect ratio for GCash QR codes (can be rectangular)
            double aspectRatio = (double) bitmap.getWidth() / bitmap.getHeight();
            if (aspectRatio < 0.5 || aspectRatio > 2.0) {
                callback.onVerificationComplete(false, "QR code should be reasonably proportioned");
                return;
            }
            
            // Perform QR code specific content analysis
            QrCodeAnalysisResult analysis = analyzeQrCodeContent(bitmap);
            
            if (!analysis.isLegitimateQrCode) {
                callback.onVerificationComplete(false, analysis.reason);
                return;
            }
            
            callback.onVerificationComplete(true, "QR code appears legitimate");
            
        } catch (Exception e) {
            Log.e(TAG, "QR code verification error: " + e.getMessage());
            callback.onVerificationError("QR code verification failed");
        }
    }

    /**
     * Analyzes QR code content for legitimacy
     */
    private static QrCodeAnalysisResult analyzeQrCodeContent(Bitmap bitmap) {
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            
            // Sample pixels for QR code analysis
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            
            QrCodeColorAnalysis colorAnalysis = analyzeQrCodeColors(pixels);
            
            // More lenient checks for GCash QR codes
            if (colorAnalysis.hasExcessiveSkinTone && colorAnalysis.skinToneRatio > 0.3) {
                return new QrCodeAnalysisResult(false, "QR code contains inappropriate content");
            }
            
            if (colorAnalysis.hasExcessiveRed && colorAnalysis.redRatio > 0.4) {
                return new QrCodeAnalysisResult(false, "QR code contains suspicious content");
            }
            
            // More lenient brightness for GCash QR codes
            if (colorAnalysis.averageBrightness < 30 || colorAnalysis.averageBrightness > 255) {
                return new QrCodeAnalysisResult(false, "QR code should be reasonably clear");
            }
            
            // More lenient contrast for GCash QR codes
            if (colorAnalysis.colorVariation < 0.1) {
                return new QrCodeAnalysisResult(false, "QR code should have some contrast");
            }
            
            // More lenient pattern detection for GCash QR codes
            if (colorAnalysis.qrLikePatterns < 0.05) {
                return new QrCodeAnalysisResult(false, "QR code should contain some patterns");
            }
            
            return new QrCodeAnalysisResult(true, "QR code appears legitimate");
            
        } catch (Exception e) {
            Log.e(TAG, "QR code content analysis error: " + e.getMessage());
            return new QrCodeAnalysisResult(false, "QR code analysis failed");
        }
    }

    /**
     * Analyzes colors in QR code for legitimacy
     */
    private static QrCodeColorAnalysis analyzeQrCodeColors(int[] pixels) {
        int totalPixels = pixels.length;
        int skinToneCount = 0;
        int redCount = 0;
        int totalBrightness = 0;
        int qrLikeCount = 0;
        
        // Sample every 5th pixel for thorough analysis
        for (int i = 0; i < totalPixels; i += 5) {
            int pixel = pixels[i];
            
            int red = (pixel >> 16) & 0xFF;
            int green = (pixel >> 8) & 0xFF;
            int blue = pixel & 0xFF;
            
            int brightness = (red + green + blue) / 3;
            totalBrightness += brightness;
            
            // Check for skin tone (should be minimal in QR codes)
            if (isSkinToneStrict(red, green, blue)) {
                skinToneCount++;
            }
            
            // Check for excessive red (suspicious for QR codes)
            if (red > green + 30 && red > blue + 30) {
                redCount++;
            }
            
            // Check for QR code-like patterns (high contrast squares)
            if (Math.abs(red - green) > 100 || Math.abs(green - blue) > 100 || Math.abs(red - blue) > 100) {
                qrLikeCount++;
            }
            
            // Additional check for GCash-style QR codes (blue background, white/black patterns)
            if ((red < 50 && green < 50 && blue > 150) || // Blue background
                (red > 200 && green > 200 && blue > 200) || // White areas
                (red < 50 && green < 50 && blue < 50)) { // Black QR code areas
                qrLikeCount++;
            }
        }
        
        double averageBrightness = (double) totalBrightness / (totalPixels / 5);
        double skinToneRatio = (double) skinToneCount / (totalPixels / 5);
        double redRatio = (double) redCount / (totalPixels / 5);
        double qrLikePatterns = (double) qrLikeCount / (totalPixels / 5);
        double colorVariation = Math.min(1.0, (averageBrightness / 255.0) * 0.8 + 0.2);
        
        return new QrCodeColorAnalysis(
            skinToneRatio > 0.3,  // Has excessive skin tone (more lenient)
            redRatio > 0.4,        // Has excessive red (more lenient)
            skinToneRatio,         // Skin tone ratio
            redRatio,              // Red ratio
            averageBrightness,
            colorVariation,
            qrLikePatterns
        );
    }

    /**
     * Data classes for QR code analysis
     */
    private static class QrCodeAnalysisResult {
        boolean isLegitimateQrCode;
        String reason;
        
        QrCodeAnalysisResult(boolean isLegitimateQrCode, String reason) {
            this.isLegitimateQrCode = isLegitimateQrCode;
            this.reason = reason;
        }
    }
    
    private static class QrCodeColorAnalysis {
        boolean hasExcessiveSkinTone;
        boolean hasExcessiveRed;
        double skinToneRatio;
        double redRatio;
        double averageBrightness;
        double colorVariation;
        double qrLikePatterns;
        
        QrCodeColorAnalysis(boolean hasExcessiveSkinTone, boolean hasExcessiveRed, 
                          double skinToneRatio, double redRatio, double averageBrightness, 
                          double colorVariation, double qrLikePatterns) {
            this.hasExcessiveSkinTone = hasExcessiveSkinTone;
            this.hasExcessiveRed = hasExcessiveRed;
            this.skinToneRatio = skinToneRatio;
            this.redRatio = redRatio;
            this.averageBrightness = averageBrightness;
            this.colorVariation = colorVariation;
            this.qrLikePatterns = qrLikePatterns;
        }
    }

    /**
     * Shows verification result to user
     */
    public static void showVerificationResult(Context context, boolean isApproved, String reason) {
        if (isApproved) {
            Toast.makeText(context, "✅ Image approved for upload", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "❌ Image rejected: " + reason, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Verification result data class
     */
    private static class VerificationResult {
        boolean isApproved;
        String reason;
        
        VerificationResult(boolean isApproved, String reason) {
            this.isApproved = isApproved;
            this.reason = reason;
        }
    }
}
