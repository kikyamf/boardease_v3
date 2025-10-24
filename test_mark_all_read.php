<?php
// Test marking all notifications as read
require_once 'dbConfig.php';

try {
    $user_id = 1;
    
    // Check current unread count
    $stmt = $conn->prepare("SELECT COUNT(*) as unread_count FROM notifications WHERE user_id = ? AND notif_status = 'unread'");
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $before_count = $result->fetch_assoc()['unread_count'];
    
    echo "Before: {$before_count} unread notifications\n";
    
    // Mark all as read
    $stmt = $conn->prepare("UPDATE notifications SET notif_status = 'read' WHERE user_id = ? AND notif_status = 'unread'");
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $updated_count = $stmt->affected_rows;
    
    echo "Updated: {$updated_count} notifications to read status\n";
    
    // Check new unread count
    $stmt = $conn->prepare("SELECT COUNT(*) as unread_count FROM notifications WHERE user_id = ? AND notif_status = 'unread'");
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $after_count = $result->fetch_assoc()['unread_count'];
    
    echo "After: {$after_count} unread notifications\n";
    
    // Insert a new test notification to test badge again
    $stmt = $conn->prepare("INSERT INTO notifications (user_id, notif_title, notif_message, notif_type, notif_status) VALUES (?, ?, ?, ?, ?)");
    $test_title = "New Test Notification";
    $test_message = "This is a new test notification to verify the badge system";
    $test_type = "general";
    $test_status = "unread";
    $stmt->bind_param("issss", $user_id, $test_title, $test_message, $test_type, $test_status);
    $stmt->execute();
    
    echo "Inserted 1 new test notification\n";
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}

$conn->close();
?>





















