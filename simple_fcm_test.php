<?php
// Simple Firebase Cloud Messaging Test
echo "=== Firebase Cloud Messaging Setup Test ===\n\n";

// Check if fcm_config.php exists
if (!file_exists('fcm_config.php')) {
    echo "❌ fcm_config.php not found!\n";
    echo "Please make sure fcm_config.php is in the same directory.\n";
    exit;
}

echo "✅ fcm_config.php found\n";

// Try to include the config
try {
    require_once 'fcm_config.php';
    echo "✅ fcm_config.php loaded successfully\n";
} catch (Exception $e) {
    echo "❌ Error loading fcm_config.php: " . $e->getMessage() . "\n";
    exit;
}

// Check configuration
echo "\n=== Configuration Check ===\n";

// Check Project ID
if (FCMConfig::FIREBASE_PROJECT_ID === 'your-firebase-project-id') {
    echo "❌ FIREBASE_PROJECT_ID not set\n";
    echo "   Please update FIREBASE_PROJECT_ID in fcm_config.php\n";
    echo "   Get it from: Firebase Console > Project Settings > General\n\n";
} else {
    echo "✅ FIREBASE_PROJECT_ID set: " . FCMConfig::FIREBASE_PROJECT_ID . "\n";
}

// Check Service Account Path
if (FCMConfig::SERVICE_ACCOUNT_PATH === 'path/to/your/service-account-key.json') {
    echo "❌ SERVICE_ACCOUNT_PATH not set\n";
    echo "   Please update SERVICE_ACCOUNT_PATH in fcm_config.php\n";
    echo "   Download service account JSON from: Firebase Console > Project Settings > Service Accounts\n\n";
} else {
    echo "✅ SERVICE_ACCOUNT_PATH set: " . FCMConfig::SERVICE_ACCOUNT_PATH . "\n";
    
    // Check if file exists
    if (file_exists(FCMConfig::SERVICE_ACCOUNT_PATH)) {
        echo "✅ Service account file exists\n";
    } else {
        echo "❌ Service account file not found at: " . FCMConfig::SERVICE_ACCOUNT_PATH . "\n";
        echo "   Please download the service account JSON file and place it in the correct location\n";
    }
}

echo "\n=== Next Steps ===\n";
echo "1. Go to Firebase Console (https://console.firebase.google.com)\n";
echo "2. Select your project\n";
echo "3. Go to Project Settings > General\n";
echo "4. Copy your Project ID\n";
echo "5. Go to Project Settings > Service Accounts\n";
echo "6. Click 'Generate new private key'\n";
echo "7. Download the JSON file\n";
echo "8. Update fcm_config.php with your actual values\n";
echo "9. Run this test again\n\n";

echo "=== Example Configuration ===\n";
echo "const FIREBASE_PROJECT_ID = 'your-actual-project-id';\n";
echo "const SERVICE_ACCOUNT_PATH = 'firebase-service-account.json';\n\n";

echo "=== Test Complete ===\n";
?>

























