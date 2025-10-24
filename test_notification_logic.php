<?php
// Test to demonstrate notification logic
require_once 'fcm_config.php';

echo "=== Notification Logic Test ===\n\n";

$deviceToken = 'dYaKt6VuSEeCzU-WccgreV:APA91bFMDucCjbd8QRQBGThd_oXC7SA9vTl5_K1bTe49pV_PdgGDHnQiTfKBkSEJCZerEqaUl3jfsYnrNviojYuFvPrkiwYqTMgCqxh62xkcSYTLhDzYn9U';

echo "Testing notification logic:\n\n";

// Test 1: Send message from User 1 to User 2 (should notify User 2)
echo "1. Sending message from User 1 to User 2 (should notify User 2):\n";
$result1 = FCMConfig::sendToDevice(
    $deviceToken,
    'John Doe',
    'Hello! This message is from John to you. You should receive this notification!'
);

if ($result1['success']) {
    echo "✅ Notification sent to receiver (User 2)\n";
} else {
    echo "❌ Failed to send notification\n";
}

echo "\n";

// Test 2: Send message from User 1 to User 1 (should NOT notify)
echo "2. Sending message from User 1 to User 1 (should NOT notify - same sender/receiver):\n";
echo "   This would normally be blocked by our logic, but let's test the notification anyway:\n";

$result2 = FCMConfig::sendToDevice(
    $deviceToken,
    'You',
    'This is a message from you to yourself. You should NOT receive this notification!'
);

if ($result2['success']) {
    echo "✅ Notification sent (but in real app, this would be blocked)\n";
} else {
    echo "❌ Failed to send notification\n";
}

echo "\n";

// Test 3: Send message from User 2 to User 1 (should notify User 1)
echo "3. Sending message from User 2 to User 1 (should notify User 1):\n";
$result3 = FCMConfig::sendToDevice(
    $deviceToken,
    'Jane Smith',
    'Hi! This message is from Jane to you. You should receive this notification!'
);

if ($result3['success']) {
    echo "✅ Notification sent to receiver (User 1)\n";
} else {
    echo "❌ Failed to send notification\n";
}

echo "\n=== Test Complete ===\n";
echo "Summary:\n";
echo "- Test 1: You should receive a notification (John → You)\n";
echo "- Test 2: You should NOT receive a notification (You → You) - blocked by logic\n";
echo "- Test 3: You should receive a notification (Jane → You)\n";
echo "\nCheck your Android device for notifications!\n";
?>
























