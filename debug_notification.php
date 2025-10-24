<?php
// Debug notification test
require_once 'fcm_config.php';

echo "=== Debug Notification Test ===\n\n";

$deviceToken = 'dYaKt6VuSEeCzU-WccgreV:APA91bFMDucCjbd8QRQBGThd_oXC7SA9vTl5_K1bTe49pV_PdgGDHnQiTfKBkSEJCZerEqaUl3jfsYnrNviojYuFvPrkiwYqTMgCqxh62xkcSYTLhDzYn9U';

echo "1. Testing with notification payload only (should show notification):\n";
try {
    $result = FCMConfig::sendToDevice(
        $deviceToken,
        '🔔 TEST NOTIFICATION',
        'If you see this, notifications are working!'
    );
    
    if ($result['success']) {
        echo "✅ Sent successfully!\n";
        echo "Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n\n";
    } else {
        echo "❌ Failed\n";
        echo "Error: " . json_encode($result, JSON_PRETTY_PRINT) . "\n\n";
    }
} catch (Exception $e) {
    echo "❌ Exception: " . $e->getMessage() . "\n\n";
}

echo "2. Testing with data payload only (no visible notification):\n";
try {
    $result = FCMConfig::sendDataMessage($deviceToken, [
        'test' => 'data_only',
        'message' => 'This is data only - no notification should appear'
    ]);
    
    if ($result['success']) {
        echo "✅ Data message sent successfully!\n";
        echo "Response: " . json_encode($result['response'], JSON_PRETTY_PRINT) . "\n\n";
    } else {
        echo "❌ Failed\n";
        echo "Error: " . json_encode($result, JSON_PRETTY_PRINT) . "\n\n";
    }
} catch (Exception $e) {
    echo "❌ Exception: " . $e->getMessage() . "\n\n";
}

echo "=== Debug Complete ===\n";
echo "Check your Android device and Android Studio logs!\n";
echo "You should see a notification for test #1, but not for test #2.\n";
?>
























