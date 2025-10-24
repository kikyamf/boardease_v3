<?php
// Test for banner notifications at top of screen
require_once 'fcm_config.php';

echo "=== Banner Notification Test ===\n\n";

$deviceToken = 'dYaKt6VuSEeCzU-WccgreV:APA91bFMDucCjbd8QRQBGThd_oXC7SA9vTl5_K1bTe49pV_PdgGDHnQiTfKBkSEJCZerEqaUl3jfsYnrNviojYuFvPrkiwYqTMgCqxh62xkcSYTLhDzYn9U';

echo "Sending a banner notification that should appear at the top of your screen...\n\n";

try {
    $result = FCMConfig::sendToDevice(
        $deviceToken,
        '📱 Banner Notification',
        'This should appear as a banner at the top of your screen! Like Messenger or WhatsApp!'
    );
    
    if ($result['success']) {
        echo "✅ Banner notification sent!\n";
        echo "Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n\n";
        echo "📱 Check your Android device now!\n";
        echo "You should see a banner notification at the top of your screen with:\n";
        echo "- Title: 📱 Banner Notification\n";
        echo "- Message: This should appear as a banner at the top of your screen!\n";
        echo "- Sound and vibration\n";
        echo "- Banner style (like Messenger/WhatsApp)\n\n";
        echo "If you don't see a banner:\n";
        echo "1. Check your Android notification settings\n";
        echo "2. Make sure 'Heads-up notifications' are enabled\n";
        echo "3. Check if your app has notification permissions\n";
        echo "4. Try minimizing your app and sending another test\n";
    } else {
        echo "❌ Failed to send notification\n";
        echo "HTTP Code: " . $result['http_code'] . "\n";
        echo "Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n";
    }
} catch (Exception $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}

echo "=== Test Complete ===\n";
?>
























