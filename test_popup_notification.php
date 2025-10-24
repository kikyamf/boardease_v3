<?php
// Test specifically for popup notifications
require_once 'fcm_config.php';

echo "=== Popup Notification Test ===\n\n";

$deviceToken = 'dYaKt6VuSEeCzU-WccgreV:APA91bFMDucCjbd8QRQBGThd_oXC7SA9vTl5_K1bTe49pV_PdgGDHnQiTfKBkSEJCZerEqaUl3jfsYnrNviojYuFvPrkiwYqTMgCqxh62xkcSYTLhDzYn9U';

echo "Sending a notification that should pop up on your device...\n\n";

try {
    $result = FCMConfig::sendToDevice(
        $deviceToken,
        '🔔 POPUP TEST',
        'This notification should pop up on your screen! If you see this, the popup is working!'
    );
    
    if ($result['success']) {
        echo "✅ Notification sent successfully!\n";
        echo "Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n\n";
        echo "📱 Check your Android device now!\n";
        echo "You should see a popup notification with:\n";
        echo "- Title: 🔔 POPUP TEST\n";
        echo "- Message: This notification should pop up on your screen!\n";
        echo "- Sound and vibration\n";
        echo "- Blue notification light (if your device supports it)\n\n";
    } else {
        echo "❌ Failed to send notification\n";
        echo "HTTP Code: " . $result['http_code'] . "\n";
        echo "Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n";
    }
} catch (Exception $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}

echo "=== Test Complete ===\n";
echo "If you don't see a popup notification:\n";
echo "1. Make sure your app is running (even in background)\n";
echo "2. Check Android Studio logs for any errors\n";
echo "3. Try minimizing your app and sending another test\n";
?>















