<?php
// Test Firebase Cloud Messaging HTTP v1 API
require_once 'fcm_config.php';

echo "=== Firebase Cloud Messaging HTTP v1 Test ===\n\n";

// Check if service account file exists
if (!file_exists(FCMConfig::SERVICE_ACCOUNT_PATH)) {
    echo "❌ Service account file not found: " . FCMConfig::SERVICE_ACCOUNT_PATH . "\n";
    echo "Please download your service account JSON from Firebase Console and update the path.\n\n";
    exit;
}

echo "✅ Service account file found: " . FCMConfig::SERVICE_ACCOUNT_PATH . "\n";

// Check if project ID is set
if (FCMConfig::FIREBASE_PROJECT_ID === 'your-firebase-project-id') {
    echo "❌ Please update FIREBASE_PROJECT_ID in fcm_config.php with your actual project ID.\n\n";
    exit;
}

echo "✅ Project ID set: " . FCMConfig::FIREBASE_PROJECT_ID . "\n\n";

// Test access token generation
try {
    echo "Testing access token generation...\n";
    $reflection = new ReflectionClass('FCMConfig');
    $method = $reflection->getMethod('getAccessToken');
    $method->setAccessible(true);
    $accessToken = $method->invoke(null);
    
    if ($accessToken) {
        echo "✅ Access token generated successfully!\n";
        echo "Token (first 20 chars): " . substr($accessToken, 0, 20) . "...\n\n";
    } else {
        echo "❌ Failed to generate access token\n\n";
        exit;
    }
} catch (Exception $e) {
    echo "❌ Error generating access token: " . $e->getMessage() . "\n\n";
    exit;
}

// Test sending notification (you need to provide a real device token)
echo "To test sending a notification:\n";
echo "1. Get a device token from your Android app\n";
echo "2. Replace 'YOUR_DEVICE_TOKEN_HERE' below with the actual token\n";
echo "3. Uncomment the test code below\n\n";

/*
// Uncomment and modify this section to test actual notification sending
$deviceToken = 'YOUR_DEVICE_TOKEN_HERE'; // Replace with actual device token

try {
    echo "Sending test notification...\n";
    $result = FCMConfig::sendToDevice(
        $deviceToken,
        'Test Notification',
        'This is a test message from HTTP v1 API'
    );
    
    if ($result['success']) {
        echo "✅ Notification sent successfully!\n";
        echo "Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n";
    } else {
        echo "❌ Failed to send notification\n";
        echo "HTTP Code: " . $result['http_code'] . "\n";
        echo "Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n";
    }
} catch (Exception $e) {
    echo "❌ Error sending notification: " . $e->getMessage() . "\n";
}
*/

echo "=== Test Complete ===\n";
echo "Your Firebase Cloud Messaging HTTP v1 setup is ready!\n";
echo "Next steps:\n";
echo "1. Update your Android app with the new Firebase configuration\n";
echo "2. Get a device token from your app\n";
echo "3. Test sending notifications\n";
?>

























