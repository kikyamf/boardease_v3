<?php
// Test a single notification that appears in the app's notification section
require_once 'dbConfig.php';
require_once 'fcm_config.php';

header('Content-Type: application/json');

echo "<h2>Test Single Notification</h2>";

try {
    // Get the latest device token for user_id = 1
    $stmt = $conn->prepare("
        SELECT device_token, user_id 
        FROM device_tokens 
        WHERE user_id = 1 AND is_active = 1 
        ORDER BY created_at DESC 
        LIMIT 1
    ");
    $stmt->execute();
    $result = $stmt->get_result();
    $token_data = $result->fetch_assoc();
    $stmt->close();
    
    if (!$token_data) {
        echo "<h3 style='color: red;'>❌ No active device token found for user_id = 1</h3>";
        exit;
    }
    
    $device_token = $token_data['device_token'];
    echo "<h3>✅ Found device token for user_id = 1</h3>";
    
    // Create a test notification
    $title = "Test Notification - " . date('H:i:s');
    $message = "This is a test notification created at " . date('Y-m-d H:i:s') . ". It should appear in your app's notification section.";
    $type = 'general';
    
    echo "<h3>Creating notification...</h3>";
    echo "<p><strong>Title:</strong> $title</p>";
    echo "<p><strong>Message:</strong> $message</p>";
    echo "<p><strong>Type:</strong> $type</p>";
    
    // Insert notification into database
    $stmt = $conn->prepare("
        INSERT INTO notifications (user_id, notif_title, notif_message, notif_type, notif_status, notif_created_at) 
        VALUES (?, ?, ?, ?, 'unread', NOW())
    ");
    $stmt->bind_param("isss", $user_id, $title, $message, $type);
    $user_id = 1;
    $stmt->execute();
    $notif_id = $conn->insert_id;
    $stmt->close();
    
    echo "<h4 style='color: green;'>✅ Database notification created (ID: $notif_id)</h4>";
    
    // Send FCM notification
    $fcm_result = FCMConfig::sendToDevice(
        $device_token,
        "New Notification",
        "You have a new notification in BoardEase",
        [
            'type' => $type,
            'notif_id' => (string)$notif_id,
            'title' => $title,
            'message' => $message,
            'timestamp' => date('Y-m-d H:i:s')
        ]
    );
    
    if ($fcm_result['success']) {
        echo "<h4 style='color: green;'>✅ FCM notification sent successfully</h4>";
    } else {
        echo "<h4 style='color: red;'>❌ FCM notification failed</h4>";
        echo "<p>Error: " . ($fcm_result['response']['error']['message'] ?? 'Unknown error') . "</p>";
    }
    
    echo "<h3>Result</h3>";
    echo "<p>✅ Notification created in database</p>";
    echo "<p>✅ Push notification sent to device</p>";
    echo "<p><strong>Check your app's notification section - you should see this notification!</strong></p>";
    
} catch (Exception $e) {
    echo "<h3 style='color: red;'>❌ Error: " . $e->getMessage() . "</h3>";
}
?>








