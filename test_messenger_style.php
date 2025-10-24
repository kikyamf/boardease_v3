<?php
// Test for Messenger-style popup notifications
require_once 'fcm_config.php';

echo "=== Messenger-Style Popup Test ===\n\n";

$deviceToken = 'dYaKt6VuSEeCzU-WccgreV:APA91bFMDucCjbd8QRQBGThd_oXC7SA9vTl5_K1bTe49pV_PdgGDHnQiTfKBkSEJCZerEqaUl3jfsYnrNviojYuFvPrkiwYqTMgCqxh62xkcSYTLhDzYn9U';

echo "Sending Messenger-style popup notification...\n\n";

try {
    $result = FCMConfig::sendToDevice(
        $deviceToken,
        'John Doe',
        'Hey! How are you doing? Are you free to chat?'
    );
    
    if ($result['success']) {
        echo "✅ Messenger-style notification sent!\n";
        echo "Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n\n";
        echo "📱 Check your Android device now!\n";
        echo "You should see a popup notification that looks like Messenger with:\n";
        echo "- Profile picture\n";
        echo "- Sender name: John Doe\n";
        echo "- Message: Hey! How are you doing? Are you free to chat?\n";
        echo "- Time: now\n";
        echo "- Sound and vibration\n";
        echo "- Full screen popup (like Messenger)\n\n";
    } else {
        echo "❌ Failed to send notification\n";
        echo "HTTP Code: " . $result['http_code'] . "\n";
        echo "Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n";
    }
} catch (Exception $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}

echo "=== Test Complete ===\n";
echo "If you don't see a Messenger-style popup:\n";
echo "1. Make sure your app is running (even in background)\n";
echo "2. Check if notification permissions are enabled\n";
echo "3. Try minimizing your app and sending another test\n";
echo "4. Check Android Studio logs for any errors\n";
?>
























