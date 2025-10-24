# 🔥 Firebase Cloud Messaging Setup Guide (HTTP v1 API)

## 📋 Overview
Google has deprecated the legacy Cloud Messaging API. You now need to use the **Firebase Cloud Messaging API (HTTP v1)** which requires a **Service Account** instead of a server key.

## 🚀 Step 1: Get Your Firebase Project ID

1. **Go to [Firebase Console](https://console.firebase.google.com)**
2. **Select your project**
3. **Click the gear icon** ⚙️ → **Project Settings**
4. **Go to "General" tab**
5. **Copy your "Project ID"** (not the project name)

## 🔑 Step 2: Create Service Account

1. **In Firebase Console** → **Project Settings** → **Service Accounts**
2. **Click "Generate new private key"**
3. **Click "Generate key"** in the popup
4. **Download the JSON file** (keep it secure!)
5. **Place the JSON file** in your PHP project directory

## 📁 Step 3: Update PHP Configuration

### Update `fcm_config.php`:

```php
// Replace these values with your actual data
const FIREBASE_PROJECT_ID = 'your-actual-project-id';
const SERVICE_ACCOUNT_PATH = 'path/to/your/downloaded-service-account-key.json';
```

### Example:
```php
const FIREBASE_PROJECT_ID = 'mock-boarding-app-12345';
const SERVICE_ACCOUNT_PATH = 'firebase-service-account.json';
```

## 🔧 Step 4: Enable Required APIs

1. **Go to [Google Cloud Console](https://console.cloud.google.com)**
2. **Select your Firebase project**
3. **Go to "APIs & Services"** → **"Library"**
4. **Search for "Firebase Cloud Messaging API"**
5. **Click "Enable"**

## 📱 Step 5: Android Integration

### Update `build.gradle` (Project level):
```gradle
buildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.3.15'
    }
}
```

### Update `build.gradle` (App level):
```gradle
plugins {
    id 'com.google.gms.google-services'
}

dependencies {
    implementation 'com.google.firebase:firebase-messaging:23.1.2'
    implementation 'com.google.firebase:firebase-analytics:21.2.0'
}
```

### Download `google-services.json`:
1. **Firebase Console** → **Project Settings** → **General**
2. **Scroll down to "Your apps"**
3. **Click on your Android app**
4. **Download `google-services.json`**
5. **Place it in `app/` directory**

## 🧪 Step 6: Test Your Setup

### Create a test file `test_fcm_v1.php`:

```php
<?php
require_once 'fcm_config.php';

try {
    // Test sending a notification
    $result = FCMConfig::sendToDevice(
        'YOUR_DEVICE_TOKEN_HERE', // Replace with actual device token
        'Test Notification',
        'This is a test message from HTTP v1 API'
    );
    
    if ($result['success']) {
        echo "✅ Notification sent successfully!\n";
        echo "Response: " . json_encode($result['response']) . "\n";
    } else {
        echo "❌ Failed to send notification\n";
        echo "HTTP Code: " . $result['http_code'] . "\n";
        echo "Response: " . json_encode($result['response']) . "\n";
    }
} catch (Exception $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}
?>
```

## 🔍 Step 7: Get Device Token (Android)

### In your Android app, get the FCM token:

```java
FirebaseMessaging.getInstance().getToken()
    .addOnCompleteListener(new OnCompleteListener<String>() {
        @Override
        public void onComplete(@NonNull Task<String> task) {
            if (!task.isSuccessful()) {
                Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                return;
            }

            // Get new FCM registration token
            String token = task.getResult();
            Log.d(TAG, "FCM Registration Token: " + token);
            
            // Send this token to your server
            sendTokenToServer(token);
        }
    });
```

## 📊 Step 8: Update Your PHP Files

All your existing PHP files (`send_message_with_notification.php`, `send_group_message_with_notification.php`, etc.) will work with the updated `FCMConfig` class. No changes needed!

## 🚨 Important Notes

1. **Service Account Security**: Keep your service account JSON file secure and never commit it to version control
2. **Token Expiration**: Access tokens expire after 1 hour, but the code automatically refreshes them
3. **Rate Limits**: The new API has different rate limits than the legacy API
4. **Error Handling**: The new API returns more detailed error messages

## 🎯 Quick Checklist

- [ ] Get Firebase Project ID
- [ ] Create and download Service Account JSON
- [ ] Update `fcm_config.php` with Project ID and JSON path
- [ ] Enable Firebase Cloud Messaging API in Google Cloud Console
- [ ] Update Android `build.gradle` files
- [ ] Download and place `google-services.json`
- [ ] Test with a device token
- [ ] Update your existing PHP files (if needed)

## 🔧 Troubleshooting

### Common Issues:

1. **"Service account file not found"**
   - Check the path to your JSON file
   - Make sure the file exists and is readable

2. **"Failed to get access token"**
   - Verify your service account JSON is valid
   - Check that Firebase Cloud Messaging API is enabled

3. **"Invalid project ID"**
   - Double-check your Project ID in Firebase Console
   - Make sure there are no extra spaces or characters

4. **Android app not receiving notifications**
   - Verify `google-services.json` is in the correct location
   - Check that Firebase is properly initialized in your app
   - Ensure device token is valid and up-to-date

## 🎉 You're All Set!

Once you complete these steps, your push notifications will work with the modern Firebase Cloud Messaging API (HTTP v1)! 🚀

























