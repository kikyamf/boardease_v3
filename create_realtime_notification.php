<?php
// Create a new notification to test real-time badge updates
require_once 'dbConfig.php';

try {
    $user_id = 1;
    
    // Create a new notification
    $stmt = $conn->prepare("INSERT INTO notifications (user_id, notif_title, notif_message, notif_type, notif_status) VALUES (?, ?, ?, ?, 'unread')");
    $title = "🔔 REAL-TIME TEST";
    $message = "This notification was just created to test real-time badge updates!";
    $type = "general";
    $stmt->bind_param("isss", $user_id, $title, $message, $type);
    
    if ($stmt->execute()) {
        echo "✅ Created new notification: {$title}\n";
        
        // Check new unread count
        $stmt = $conn->prepare("SELECT COUNT(*) as unread_count FROM notifications WHERE user_id = ? AND notif_status = 'unread'");
        $stmt->bind_param("i", $user_id);
        $stmt->execute();
        $result = $stmt->get_result();
        $unread_count = $result->fetch_assoc()['unread_count'];
        
        echo "📊 New unread count: {$unread_count}\n";
        echo "📱 Android app badge should now show: {$unread_count}\n";
        echo "🔄 This should update in real-time!\n";
        
    } else {
        echo "❌ Error creating notification: " . $stmt->error . "\n";
    }
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}

$conn->close();
?>





















