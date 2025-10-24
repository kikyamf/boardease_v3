<?php
// Very simple test - just send "Hello" to your device
require_once 'fcm_config.php';

echo "=== Simple Hello Test ===\n";
echo "Sending 'Hello' notification to your device...\n\n";

$deviceToken = 'dYaKt6VuSEeCzU-WccgreV:APA91bFMDucCjbd8QRQBGThd_oXC7SA9vTl5_K1bTe49pV_PdgGDHnQiTfKBkSEJCZerEqaUl3jfsYnrNviojYuFvPrkiwYqTMgCqxh62xkcSYTLhDzYn9U';

try {
    $result = FCMConfig::sendToDevice(
        $deviceToken,
        'Hello! 👋',
        'This is a test notification from your PHP server!'
    );
    
    if ($result['success']) {
        echo "✅ SUCCESS! Notification sent!\n";
        echo "Check your Android device - you should see a notification!\n\n";
        echo "Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n";
    } else {
        echo "❌ Failed to send notification\n";
        echo "HTTP Code: " . $result['http_code'] . "\n";
        echo "Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n";
    }
} catch (Exception $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}

echo "\n=== What to Look For ===\n";
echo "On your Android device, you should see:\n";
echo "1. A notification in the notification bar (top of screen)\n";
echo "2. The notification should say 'Hello! 👋'\n";
echo "3. When you tap it, it should open your app\n\n";
echo "If you don't see anything:\n";
echo "- Make sure your app is running in the background\n";
echo "- Check if notifications are enabled for your app\n";
echo "- Try running the test again\n";
?>
























