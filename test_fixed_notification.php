<?php
// Fixed Firebase Cloud Messaging Test
require_once 'fcm_config.php';

echo "=== Fixed Firebase Cloud Messaging Test ===\n\n";

// Your actual device token
$deviceToken = 'dYaKt6VuSEeCzU-WccgreV:APA91bFMDucCjbd8QRQBGThd_oXC7SA9vTl5_K1bTe49pV_PdgGDHnQiTfKBkSEJCZerEqaUl3jfsYnrNviojYuFvPrkiwYqTMgCqxh62xkcSYTLhDzYn9U';

echo "Device Token: " . substr($deviceToken, 0, 20) . "...\n\n";

// Test 1: Send a simple notification (FIXED)
echo "1. Testing Simple Notification (Fixed):\n";
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

// Test 2: Send a message notification (like a real chat message)
echo "2. Testing Chat Message Notification:\n";
echo "   Sending a chat message notification...\n";

try {
    $result = FCMConfig::sendToDevice(
        $deviceToken,
        '💬 New Message',
        'John Doe: Hey, how are you doing?',
        [
            'type' => 'new_message',
            'sender_id' => '2',
            'sender_name' => 'John Doe',
            'chat_id' => '123',
            'unread_count' => '1'
        ]
    );
    
    if ($result['success']) {
        echo "✅ Chat message notification sent successfully!\n";
        echo "   Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n\n";
    } else {
        echo "❌ Failed to send chat message notification\n";
        echo "   HTTP Code: " . $result['http_code'] . "\n";
        echo "   Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n\n";
    }
} catch (Exception $e) {
    echo "❌ Error sending chat message notification: " . $e->getMessage() . "\n\n";
}

// Test 3: Send a boarding house notification
echo "3. Testing Boarding House Notification:\n";
echo "   Sending a boarding house related notification...\n";

try {
    $result = FCMConfig::sendToDevice(
        $deviceToken,
        '🏠 Boarding House Update',
        'Your room booking has been approved! Check your app for details.',
        [
            'type' => 'booking_update',
            'booking_id' => '456',
            'status' => 'approved',
            'action' => 'view_booking'
        ]
    );
    
    if ($result['success']) {
        echo "✅ Boarding house notification sent successfully!\n";
        echo "   Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n\n";
    } else {
        echo "❌ Failed to send boarding house notification\n";
        echo "   HTTP Code: " . $result['http_code'] . "\n";
        echo "   Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n\n";
    }
} catch (Exception $e) {
    echo "❌ Error sending boarding house notification: " . $e->getMessage() . "\n\n";
}

echo "=== Test Complete ===\n";
echo "Check your Android device for notifications!\n";
echo "You should see:\n";
echo "1. A simple test notification\n";
echo "2. A chat message notification\n";
echo "3. A boarding house update notification\n\n";
echo "If notifications don't appear:\n";
echo "- Make sure your app is running in the background\n";
echo "- Check notification permissions in Android settings\n";
echo "- Try opening your app and then sending notifications\n";
?>
























