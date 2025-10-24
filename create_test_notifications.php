<?php
// Create multiple test notifications for badge testing
require_once 'dbConfig.php';

try {
    $user_id = 1;
    
    $test_notifications = [
        ['New Booking Request', 'John Doe wants to book Room 101 for next month', 'booking'],
        ['Payment Received', 'Payment of ₱3,500 received from Jane Smith', 'payment'],
        ['Maintenance Alert', 'Elevator maintenance scheduled for tomorrow', 'maintenance'],
        ['Important Announcement', 'Fire drill will be conducted next week', 'announcement'],
        ['Welcome Message', 'Welcome to BoardEase! Your account is ready', 'general']
    ];
    
    $insert_sql = "INSERT INTO notifications (user_id, notif_title, notif_message, notif_type, notif_status) VALUES (?, ?, ?, ?, 'unread')";
    $stmt = $conn->prepare($insert_sql);
    
    foreach ($test_notifications as $notif) {
        $stmt->bind_param("isss", $user_id, $notif[0], $notif[1], $notif[2]);
        if ($stmt->execute()) {
            echo "✓ Created notification: {$notif[0]}\n";
        } else {
            echo "✗ Error creating notification: " . $stmt->error . "\n";
        }
    }
    
    // Check total unread count
    $stmt = $conn->prepare("SELECT COUNT(*) as unread_count FROM notifications WHERE user_id = ? AND notif_status = 'unread'");
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $unread_count = $result->fetch_assoc()['unread_count'];
    
    echo "\n✓ Total unread notifications: {$unread_count}\n";
    echo "The notification badge should now show: {$unread_count}\n";
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}

$conn->close();
?>








