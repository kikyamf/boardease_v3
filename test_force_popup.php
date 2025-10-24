<?php
// Force popup notification test
require_once 'fcm_config.php';

echo "=== Force Popup Notification Test ===\n\n";

$deviceToken = 'dYaKt6VuSEeCzU-WccgreV:APA91bFMDucCjbd8QRQBGThd_oXC7SA9vTl5_K1bTe49pV_PdgGDHnQiTfKBkSEJCZerEqaUl3jfsYnrNviojYuFvPrkiwYqTMgCqxh62xkcSYTLhDzYn9U';

echo "Sending a notification that should FORCE a popup on your screen...\n\n";

try {
    $result = FCMConfig::sendToDevice(
        $deviceToken,
        '🚨 URGENT MESSAGE',
        'This notification should pop up on your screen! If you see this, the popup is working!'
    );
    
    if ($result['success']) {
        echo "✅ Force popup notification sent!\n";
        echo "Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n\n";
        echo "📱 Check your Android device now!\n";
        echo "You should see a popup notification with:\n";
        echo "- Title: 🚨 URGENT MESSAGE\n";
        echo "- Message: This notification should pop up on your screen!\n";
        echo "- Sound and vibration\n";
        echo "- Full screen popup (like Messenger)\n\n";
        echo "If you still don't see a popup:\n";
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
























