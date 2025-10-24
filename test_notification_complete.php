<?php
// Complete Firebase Cloud Messaging Test
require_once 'fcm_config.php';

echo "=== Complete Firebase Cloud Messaging Test ===\n\n";

// Configuration check
echo "1. Configuration Check:\n";
echo "   Project ID: " . FCMConfig::FIREBASE_PROJECT_ID . "\n";
echo "   Service Account: " . FCMConfig::SERVICE_ACCOUNT_PATH . "\n\n";

// Check if service account file exists
if (!file_exists(FCMConfig::SERVICE_ACCOUNT_PATH)) {
    echo "❌ Service account file not found!\n";
    echo "   Please download your service account JSON from Firebase Console\n";
    echo "   and update the SERVICE_ACCOUNT_PATH in fcm_config.php\n\n";
    exit;
}

echo "✅ Service account file found\n\n";

// Test access token generation
echo "2. Testing Access Token Generation:\n";
try {
    $reflection = new ReflectionClass('FCMConfig');
    $method = $reflection->getMethod('getAccessToken');
    $method->setAccessible(true);
    $accessToken = $method->invoke(null);
    
    if ($accessToken) {
        echo "✅ Access token generated successfully!\n";
        echo "   Token (first 20 chars): " . substr($accessToken, 0, 20) . "...\n\n";
    } else {
        echo "❌ Failed to generate access token\n\n";
        exit;
    }
} catch (Exception $e) {
    echo "❌ Error generating access token: " . $e->getMessage() . "\n\n";
    exit;
}

// Test notification sending
echo "3. Testing Notification Sending:\n";
echo "   To test with a real device:\n";
echo "   1. Run your Android app\n";
echo "   2. Click the red notification button (top-right)\n";
echo "   3. Copy the FCM token\n";
echo "   4. Replace 'YOUR_DEVICE_TOKEN_HERE' below with the actual token\n";
echo "   5. Uncomment the test code below\n\n";

/*
// Uncomment and modify this section to test actual notification sending
$deviceToken = 'YOUR_DEVICE_TOKEN_HERE'; // Replace with actual device token

echo "   Sending test notification to device...\n";
try {
    $result = FCMConfig::sendToDevice(
        $deviceToken,
        'Test Notification',
        'This is a test message from your PHP server!'
    );
    
    if ($result['success']) {
        echo "✅ Notification sent successfully!\n";
        echo "   Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n";
    } else {
        echo "❌ Failed to send notification\n";
        echo "   HTTP Code: " . $result['http_code'] . "\n";
        echo "   Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n";
    }
} catch (Exception $e) {
    echo "❌ Error sending notification: " . $e->getMessage() . "\n";
}
*/

echo "4. Testing Data Message (Badge Update):\n";
echo "   Data messages don't show notifications but can update app state\n";
echo "   Uncomment the code below to test badge updates\n\n";

/*
// Test data message for badge update
$deviceToken = 'YOUR_DEVICE_TOKEN_HERE'; // Replace with actual device token

echo "   Sending data message for badge update...\n";
try {
    $result = FCMConfig::sendDataMessage($deviceToken, [
        'unread_count' => '5',
        'type' => 'badge_update'
    ]);
    
    if ($result['success']) {
        echo "✅ Data message sent successfully!\n";
        echo "   Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n";
    } else {
        echo "❌ Failed to send data message\n";
        echo "   HTTP Code: " . $result['http_code'] . "\n";
        echo "   Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n";
    }
} catch (Exception $e) {
    echo "❌ Error sending data message: " . $e->getMessage() . "\n";
}
*/

echo "=== Test Complete ===\n";
echo "Your Firebase Cloud Messaging setup is ready!\n\n";
echo "Next steps:\n";
echo "1. Build and run your Android app\n";
echo "2. Get the FCM token from the test activity\n";
echo "3. Use the token in the test code above\n";
echo "4. Test sending notifications\n\n";
echo "For production use:\n";
echo "- Implement token registration in your app\n";
echo "- Store tokens in your database\n";
echo "- Use the messaging PHP files for real notifications\n";
?>

























