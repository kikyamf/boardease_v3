package com.example.mock;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class FCMTestActivity extends AppCompatActivity {

    private TextView tokenTextView;
    private Button getTokenButton;
    private Button copyTokenButton;
    private String currentToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fcm_test);

        tokenTextView = findViewById(R.id.tokenTextView);
        getTokenButton = findViewById(R.id.getTokenButton);
        copyTokenButton = findViewById(R.id.copyTokenButton);

        // Check if we have a cached token
        String cachedToken = FCMTokenManager.getCachedToken(this);
        if (cachedToken != null) {
            displayToken(cachedToken);
        }

        getTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFCMToken();
            }
        });

        copyTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyTokenToClipboard();
            }
        });
    }

    private void getFCMToken() {
        getTokenButton.setEnabled(false);
        getTokenButton.setText("Getting Token...");

        FCMTokenManager.getCurrentToken(this, new FCMTokenManager.TokenCallback() {
            @Override
            public void onTokenReceived(String token) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayToken(token);
                        getTokenButton.setEnabled(true);
                        getTokenButton.setText("Get Token");
                        Toast.makeText(FCMTestActivity.this, "Token received!", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onTokenError(Exception error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tokenTextView.setText("Error getting token: " + error.getMessage());
                        getTokenButton.setEnabled(true);
                        getTokenButton.setText("Get Token");
                        Toast.makeText(FCMTestActivity.this, "Error getting token", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void displayToken(String token) {
        currentToken = token;
        tokenTextView.setText(token);
        copyTokenButton.setEnabled(true);
        Log.d("FCMTestActivity", "FCM Token: " + token);
    }

    private void copyTokenToClipboard() {
        if (currentToken != null) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("FCM Token", currentToken);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Token copied to clipboard!", Toast.LENGTH_SHORT).show();
        }
    }
}























