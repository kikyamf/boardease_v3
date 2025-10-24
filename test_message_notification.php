<?php
// Test message notification (simulates the real messaging system)
require_once 'send_message.php';

echo "<h2>Test Message Notification</h2>";

// Test data
$testData = [
    'sender_id' => '2',  // John Doe
    'receiver_id' => '1', // Namz Baer (you)
    'message' => 'Hello! This is a test message notification from the server.'
];

echo "<h3>Sending test message...</h3>";
echo "<p><strong>From:</strong> User ID " . $testData['sender_id'] . " (John Doe)</p>";
echo "<p><strong>To:</strong> User ID " . $testData['receiver_id'] . " (Namz Baer)</p>";
echo "<p><strong>Message:</strong> " . $testData['message'] . "</p>";

// Make POST request to send_message.php
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, 'http://192.168.101.6/BoardEase2/send_message.php');
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query($testData));
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Content-Type: application/x-www-form-urlencoded'
]);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

echo "<h3>Response:</h3>";
echo "<p><strong>HTTP Code:</strong> $httpCode</p>";
echo "<p><strong>Response:</strong></p>";
echo "<pre>" . htmlspecialchars($response) . "</pre>";

if ($httpCode == 200) {
    $data = json_decode($response, true);
    if ($data && $data['success']) {
        echo "<h3 style='color: green;'>✅ SUCCESS! Message sent successfully!</h3>";
        
        if (isset($data['data']['notification_sent']) && $data['data']['notification_sent']) {
            echo "<h4 style='color: green;'>✅ Notification sent successfully!</h4>";
            echo "<p>You should receive a push notification on your device.</p>";
        } else {
            echo "<h4 style='color: red;'>❌ Notification failed to send</h4>";
            echo "<p>Reason: " . ($data['data']['notification_reason'] ?? 'Unknown') . "</p>";
        }
    } else {
        echo "<h3 style='color: red;'>❌ FAILED: " . ($data['message'] ?? 'Unknown error') . "</h3>";
    }
} else {
    echo "<h3 style='color: red;'>❌ HTTP Error: $httpCode</h3>";
}

echo "<h3>Instructions:</h3>";
echo "<ol>";
echo "<li>Make sure your Android app is open or running in the background</li>";
echo "<li>Check your device for push notifications</li>";
echo "<li>If you don't see notifications, check the app's notification settings</li>";
echo "</ol>";
?>








