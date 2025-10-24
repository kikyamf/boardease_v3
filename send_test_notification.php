<?php
// Simple test to send a notification
require_once 'dbConfig.php';

try {
    $user_id = 1;
    
    // Create a test notification
    $stmt = $conn->prepare("INSERT INTO notifications (user_id, notif_title, notif_message, notif_type, notif_status) VALUES (?, ?, ?, ?, 'unread')");
    $title = "🔔 Test Badge Display";
    $message = "This notification is to test if the badge displays correctly in real-time.";
    $type = "general";
    $stmt->bind_param("isss", $user_id, $title, $message, $type);
    
    if ($stmt->execute()) {
        echo "✅ Created test notification: {$title}\n";
        
        // Check unread count
        $stmt = $conn->prepare("SELECT COUNT(*) as unread_count FROM notifications WHERE user_id = ? AND notif_status = 'unread'");
        $stmt->bind_param("i", $user_id);
        $stmt->execute();
        $result = $stmt->get_result();
        $unread_count = $result->fetch_assoc()['unread_count'];
        
        echo "📊 Total unread notifications: {$unread_count}\n";
        echo "📱 Android app badge should show: {$unread_count}\n";
        echo "🔍 Check the debug logs to see if badge displays correctly!\n";
        
    } else {
        echo "❌ Error creating notification: " . $stmt->error . "\n";
    }
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}

$conn->close();
?>







