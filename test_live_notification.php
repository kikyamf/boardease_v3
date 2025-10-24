<?php
// Live Firebase Cloud Messaging Test with Real Device Token
require_once 'fcm_config.php';

echo "=== Live Firebase Cloud Messaging Test ===\n\n";

// Your actual device token
$deviceToken = 'dYaKt6VuSEeCzU-WccgreV:APA91bFMDucCjbd8QRQBGThd_oXC7SA9vTl5_K1bTe49pV_PdgGDHnQiTfKBkSEJCZerEqaUl3jfsYnrNviojYuFvPrkiwYqTMgCqxh62xkcSYTLhDzYn9U';

echo "Device Token: " . substr($deviceToken, 0, 20) . "...\n\n";

// Test 1: Send a notification message
echo "1. Testing Notification Message:\n";
echo "   Sending test notification to your device...\n";

try {
    $result = FCMConfig::sendToDevice(
        $deviceToken,
        '🎉 Test Notification',
        'Hello! This is a test message from your PHP server. If you see this, Firebase Cloud Messaging is working perfectly!'
    );
    
    if ($result['success']) {
        echo "✅ Notification sent successfully!\n";
        echo "   Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n\n";
    } else {
        echo "❌ Failed to send notification\n";
        echo "   HTTP Code: " . $result['http_code'] . "\n";
        echo "   Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n\n";
    }
} catch (Exception $e) {
    echo "❌ Error sending notification: " . $e->getMessage() . "\n\n";
}

// Test 2: Send a data message (for badge updates)
echo "2. Testing Data Message (Badge Update):\n";
echo "   Sending data message to update message badge...\n";

try {
    $result = FCMConfig::sendDataMessage($deviceToken, [
        'unread_count' => '3',
        'type' => 'badge_update',
        'message' => 'You have 3 unread messages'
    ]);
    
    if ($result['success']) {
        echo "✅ Data message sent successfully!\n";
        echo "   Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n\n";
    } else {
        echo "❌ Failed to send data message\n";
        echo "   HTTP Code: " . $result['http_code'] . "\n";
        echo "   Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n\n";
    }
} catch (Exception $e) {
    echo "❌ Error sending data message: " . $e->getMessage() . "\n\n";
}

// Test 3: Send a message with both notification and data
echo "3. Testing Combined Message (Notification + Data):\n";
echo "   Sending message with both notification and data payload...\n";

try {
    $result = FCMConfig::sendToDevice(
        $deviceToken,
        '📱 New Message',
        'You received a new message in your boarding house app!',
        [
            'unread_count' => '5',
            'type' => 'new_message',
            'sender' => 'John Doe',
            'chat_id' => '123'
        ]
    );
    
    if ($result['success']) {
        echo "✅ Combined message sent successfully!\n";
        echo "   Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n\n";
    } else {
        echo "❌ Failed to send combined message\n";
        echo "   HTTP Code: " . $result['http_code'] . "\n";
        echo "   Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n\n";
    }
} catch (Exception $e) {
    echo "❌ Error sending combined message: " . $e->getMessage() . "\n\n";
}

echo "=== Test Complete ===\n";
echo "Check your Android device for:\n";
echo "1. Push notification (should appear in notification bar)\n";
echo "2. Badge update (if your app handles data messages)\n";
echo "3. Combined notification with data\n\n";
echo "If you don't see notifications, check:\n";
echo "- Your app is running in the background\n";
echo "- Notification permissions are enabled\n";
echo "- Firebase project configuration is correct\n";
?>
























