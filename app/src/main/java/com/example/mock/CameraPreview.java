package com.example.mock;

import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "CameraPreview";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private boolean isPreviewRunning = false;

    public CameraPreview(Context context) {
        super(context);
        init();
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        // Disable camera shutter sound at initialization
        try {
            System.setProperty("camera.disable_shutter_sound", "true");
            System.setProperty("ro.camera.sound.forced", "0");
            System.setProperty("camera.sound.forced", "0");
            Log.d(TAG, "Camera shutter sound disabled in CameraPreview");
        } catch (Exception e) {
            Log.d(TAG, "Could not disable camera sound in CameraPreview: " + e.getMessage());
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera = Camera.open();
            mCamera.setDisplayOrientation(90);
            
            // Enable autofocus
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            
                // Set flash to OFF by default (user can toggle manually)
                if (parameters.getSupportedFlashModes() != null &&
                    parameters.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_OFF)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                }
            
            
            // Set picture size for better quality
            Camera.Size bestSize = getBestPictureSize(parameters);
            if (bestSize != null) {
                parameters.setPictureSize(bestSize.width, bestSize.height);
            }
            
            // Try to disable shutter sound at camera level
            try {
                // Some devices support this parameter
                parameters.set("shutter-sound", "false");
            } catch (Exception e) {
                Log.d(TAG, "Shutter sound parameter not supported on this device");
            }
            
            mCamera.setParameters(parameters);
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening camera: " + e.getMessage());
        }
    }
    
    private Camera.Size getBestPictureSize(Camera.Parameters parameters) {
        Camera.Size bestSize = null;
        int bestArea = 0;
        
        for (Camera.Size size : parameters.getSupportedPictureSizes()) {
            int area = size.width * size.height;
            // Prefer sizes around 1920x1080 or similar
            if (area > bestArea && area <= 2073600) { // 1920x1080 = 2073600
                bestSize = size;
                bestArea = area;
            }
        }
        
        // If no good size found, use the largest available
        if (bestSize == null) {
            for (Camera.Size size : parameters.getSupportedPictureSizes()) {
                int area = size.width * size.height;
                if (area > bestArea) {
                    bestSize = size;
                    bestArea = area;
                }
            }
        }
        
        return bestSize;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mHolder.getSurface() == null) {
            return;
        }

        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping preview: " + e.getMessage());
        }

        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            isPreviewRunning = true;
        } catch (Exception e) {
            Log.e(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            isPreviewRunning = false;
        }
    }

    public Camera getCamera() {
        return mCamera;
    }

    public boolean isPreviewRunning() {
        return isPreviewRunning && mCamera != null;
    }
    
    public void startPreview() {
        if (mCamera != null && !isPreviewRunning) {
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
                isPreviewRunning = true;
                Log.d(TAG, "Camera preview started");
            } catch (IOException e) {
                Log.e(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }
    }
    
    public void stopPreview() {
        if (mCamera != null && isPreviewRunning) {
            try {
                mCamera.stopPreview();
                isPreviewRunning = false;
                Log.d(TAG, "Camera preview stopped");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping camera preview: " + e.getMessage());
            }
        }
    }

    public void takePicture(Camera.PictureCallback callback) {
        if (mCamera != null && isPreviewRunning) {
            try {
                // Completely disable shutter sound using multiple approaches
                
                // Approach 1: Disable camera shutter sound at parameter level
                Camera.Parameters params = mCamera.getParameters();
                try {
                    params.set("shutter-sound", "false");
                    params.set("camera-sound", "false");
                    params.set("sound", "false");
                    mCamera.setParameters(params);
                    Log.d(TAG, "Camera shutter sound disabled at parameter level");
                } catch (Exception e) {
                    Log.d(TAG, "Could not disable camera parameters: " + e.getMessage());
                }
                
                // Approach 2: Mute all system sounds temporarily
                android.media.AudioManager audioManager = (android.media.AudioManager) getContext().getSystemService(android.content.Context.AUDIO_SERVICE);
                int originalRingerMode = audioManager.getRingerMode();
                int originalStreamVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_SYSTEM);
                
                // Mute all audio streams
                audioManager.setRingerMode(android.media.AudioManager.RINGER_MODE_SILENT);
                audioManager.setStreamMute(android.media.AudioManager.STREAM_SYSTEM, true);
                audioManager.setStreamMute(android.media.AudioManager.STREAM_NOTIFICATION, true);
                audioManager.setStreamMute(android.media.AudioManager.STREAM_ALARM, true);
                audioManager.setStreamMute(android.media.AudioManager.STREAM_MUSIC, true);
                
                // Approach 3: Use completely silent shutter callback
                Camera.ShutterCallback silentShutter = new Camera.ShutterCallback() {
                    @Override
                    public void onShutter() {
                        // Absolutely silent - no sound at all
                        Log.d(TAG, "Completely silent shutter triggered");
                    }
                };
                
                // Take picture with silent shutter
                mCamera.takePicture(silentShutter, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        // Restore audio settings after a short delay
                        new Handler().postDelayed(() -> {
                            try {
                                audioManager.setRingerMode(originalRingerMode);
                                audioManager.setStreamMute(android.media.AudioManager.STREAM_SYSTEM, false);
                                audioManager.setStreamMute(android.media.AudioManager.STREAM_NOTIFICATION, false);
                                audioManager.setStreamMute(android.media.AudioManager.STREAM_ALARM, false);
                                audioManager.setStreamMute(android.media.AudioManager.STREAM_MUSIC, false);
                                audioManager.setStreamVolume(android.media.AudioManager.STREAM_SYSTEM, originalStreamVolume, 0);
                                Log.d(TAG, "All audio settings restored");
                            } catch (Exception e) {
                                Log.e(TAG, "Error restoring audio settings: " + e.getMessage());
                            }
                        }, 100); // Small delay to ensure picture is processed
                        
                        // Call the original callback
                        callback.onPictureTaken(data, camera);
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error in silent picture taking: " + e.getMessage());
                // Fallback - try to take picture with minimal sound
                try {
                    mCamera.takePicture(null, null, callback);
                } catch (Exception e2) {
                    Log.e(TAG, "Fallback picture taking failed: " + e2.getMessage());
                    // If both attempts fail, call the callback with null data to prevent hanging
                    if (callback != null) {
                        callback.onPictureTaken(null, mCamera);
                    }
                }
            }
        }
    }

    public void setFlashMode(String mode) {
        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            params.setFlashMode(mode);
            mCamera.setParameters(params);
        }
    }
    
    public void focusOnTouch(float x, float y) {
        if (mCamera != null) {
            try {
                Camera.Parameters parameters = mCamera.getParameters();
                if (parameters.getMaxNumFocusAreas() > 0) {
                    // Convert screen coordinates to camera coordinates
                    int focusX = (int) (x * 2000 / getWidth() - 1000);
                    int focusY = (int) (y * 2000 / getHeight() - 1000);
                    
                    // Clamp values to valid range
                    focusX = Math.max(-1000, Math.min(1000, focusX));
                    focusY = Math.max(-1000, Math.min(1000, focusY));
                    
                    Camera.Area focusArea = new Camera.Area(new android.graphics.Rect(focusX - 100, focusY - 100, focusX + 100, focusY + 100), 1000);
                    parameters.setFocusAreas(java.util.Arrays.asList(focusArea));
                    parameters.setMeteringAreas(java.util.Arrays.asList(focusArea));
                    
                    if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    }
                    
                    mCamera.setParameters(parameters);
                    mCamera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            Log.d(TAG, "Autofocus " + (success ? "succeeded" : "failed"));
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting focus: " + e.getMessage());
            }
        }
    }
}
