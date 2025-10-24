<?php
// Test marking all notifications as read directly
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
    
    if ($after_count == 0) {
        echo "✓ SUCCESS: All notifications marked as read!\n";
        echo "The badge should now be gone and stay gone.\n";
    } else {
        echo "✗ ERROR: Still have unread notifications\n";
    }
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}

$conn->close();
?>





















