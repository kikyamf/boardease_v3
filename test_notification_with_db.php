<?php
// Test notifications that appear in the app's notification section
require_once 'dbConfig.php';
require_once 'fcm_config.php';

header('Content-Type: application/json');

echo "<h2>Test Notifications for App Notification Section</h2>";

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
        echo "<p>Make sure the Android app has been opened to register the device token.</p>";
        exit;
    }
    
    $device_token = $token_data['device_token'];
    echo "<h3>✅ Found device token for user_id = 1</h3>";
    echo "<p>Token: " . substr($device_token, 0, 50) . "...</p>";
    
    // Test notifications data
    $test_notifications = [
        [
            'title' => 'Welcome to BoardEase!',
            'message' => 'Your account has been successfully set up. Start exploring our features!',
            'type' => 'general',
            'fcm_title' => 'Welcome!',
            'fcm_body' => 'Your BoardEase account is ready!'
        ],
        [
            'title' => 'New Booking Request',
            'message' => 'You have received a new booking request for "Cozy Studio Apartment" from Mike Johnson.',
            'type' => 'booking',
            'fcm_title' => 'New Booking',
            'fcm_body' => 'New booking request from Mike Johnson'
        ],
        [
            'title' => 'Payment Reminder',
            'message' => 'Your monthly payment of ₱3,500 is due in 3 days. Please make your payment to avoid late fees.',
            'type' => 'payment',
            'fcm_title' => 'Payment Due Soon',
            'fcm_body' => 'Monthly payment due in 3 days'
        ],
        [
            'title' => 'Maintenance Update',
            'message' => 'Your maintenance request for "Broken faucet in bathroom" has been completed. Please check and confirm.',
            'type' => 'maintenance',
            'fcm_title' => 'Maintenance Complete',
            'fcm_body' => 'Your maintenance request has been completed'
        ],
        [
            'title' => 'System Announcement',
            'message' => 'BoardEase will be undergoing scheduled maintenance on Sunday, 10:00 PM - 11:00 PM. Some features may be temporarily unavailable.',
            'type' => 'announcement',
            'fcm_title' => 'System Maintenance',
            'fcm_body' => 'Scheduled maintenance on Sunday 10-11 PM'
        ]
    ];
    
    $success_count = 0;
    $total_count = count($test_notifications);
    
    echo "<h3>Creating $total_count test notifications...</h3>";
    
    foreach ($test_notifications as $index => $notification) {
        echo "<h4>Test " . ($index + 1) . ": " . $notification['title'] . "</h4>";
        
        try {
            // Insert notification into database
            $stmt = $conn->prepare("
                INSERT INTO notifications (user_id, notif_title, notif_message, notif_type, notif_status, notif_created_at) 
                VALUES (?, ?, ?, ?, 'unread', NOW())
            ");
            $stmt->bind_param("isss", $user_id, $notification['title'], $notification['message'], $notification['type']);
            $user_id = 1;
            $stmt->execute();
            $notif_id = $conn->insert_id;
            $stmt->close();
            
            echo "✅ Database notification created (ID: $notif_id)<br>";
            
            // Send FCM notification
            $fcm_result = FCMConfig::sendToDevice(
                $device_token,
                $notification['fcm_title'],
                $notification['fcm_body'],
                [
                    'type' => $notification['type'],
                    'notif_id' => (string)$notif_id,
                    'title' => $notification['title'],
                    'message' => $notification['message'],
                    'timestamp' => date('Y-m-d H:i:s')
                ]
            );
            
            if ($fcm_result['success']) {
                echo "✅ FCM notification sent successfully<br>";
                $success_count++;
            } else {
                echo "❌ FCM notification failed<br>";
                echo "Error: " . ($fcm_result['response']['error']['message'] ?? 'Unknown error') . "<br>";
            }
            
        } catch (Exception $e) {
            echo "❌ Error: " . $e->getMessage() . "<br>";
        }
        
        echo "<br>";
        
        // Wait 1 second between notifications
        sleep(1);
    }
    
    // Summary
    echo "<h3>Summary</h3>";
    echo "<p><strong>Notifications created:</strong> $total_count</p>";
    echo "<p><strong>FCM notifications sent:</strong> $success_count</p>";
    
    if ($success_count == $total_count) {
        echo "<h3 style='color: green;'>🎉 ALL NOTIFICATIONS SUCCESSFUL!</h3>";
        echo "<p>Check your app's notification section - you should see $total_count new notifications!</p>";
    } else {
        echo "<h3 style='color: orange;'>⚠️ Some notifications failed. Check the errors above.</h3>";
    }
    
    // Show current notification count
    $stmt = $conn->prepare("
        SELECT COUNT(*) as total, 
               SUM(CASE WHEN notif_status = 'unread' THEN 1 ELSE 0 END) as unread
        FROM notifications 
        WHERE user_id = 1
    ");
    $stmt->execute();
    $result = $stmt->get_result();
    $counts = $result->fetch_assoc();
    $stmt->close();
    
    echo "<h3>Current Notification Status</h3>";
    echo "<p><strong>Total notifications:</strong> " . $counts['total'] . "</p>";
    echo "<p><strong>Unread notifications:</strong> " . $counts['unread'] . "</p>";
    
} catch (Exception $e) {
    echo "<h3 style='color: red;'>❌ Error: " . $e->getMessage() . "</h3>";
}
?>








