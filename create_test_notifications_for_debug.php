<?php
// Create test notifications for debugging the mark as read functionality
require_once 'dbConfig.php';

echo "=== CREATING TEST NOTIFICATIONS FOR DEBUG ===\n\n";

try {
    $user_id = 1;
    
    // Create test notifications
    $notifications = [
        ['🔔 Test Notification 1', 'This is a test notification for debugging', 'general'],
        ['💰 Test Payment', 'Test payment notification', 'payment'],
        ['📅 Test Booking', 'Test booking notification', 'booking'],
        ['🔧 Test Maintenance', 'Test maintenance notification', 'maintenance'],
        ['📢 Test Announcement', 'Test announcement notification', 'announcement']
    ];
    
    $insert_sql = "INSERT INTO notifications (user_id, notif_title, notif_message, notif_type, notif_status) VALUES (?, ?, ?, ?, 'unread')";
    $stmt = $conn->prepare($insert_sql);
    
    foreach ($notifications as $notif) {
        $stmt->bind_param("isss", $user_id, $notif[0], $notif[1], $notif[2]);
        $stmt->execute();
        echo "✅ Created: {$notif[0]}\n";
    }
    
    // Check unread count
    $stmt = $conn->prepare("SELECT COUNT(*) as unread_count FROM notifications WHERE user_id = ? AND notif_status = 'unread'");
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $unread_count = $result->fetch_assoc()['unread_count'];
    
    echo "\n📊 Unread notifications: {$unread_count}\n";
    echo "📱 Badge should show: {$unread_count}\n\n";
    
    echo "=== DEBUG TEST INSTRUCTIONS ===\n";
    echo "1. Install the updated Android app with debug logging\n";
    echo "2. Open the app - you should see badge with count: {$unread_count}\n";
    echo "3. Click the notification icon to open notification activity\n";
    echo "4. Check the Android logs for these debug messages:\n";
    echo "   - 'Notification activity onResume() called'\n";
    echo "   - 'markAllNotificationsAsRead() called for user: 1'\n";
    echo "   - 'markAllNotificationsAsRead API Response: ...'\n";
    echo "   - 'All notifications marked as read successfully'\n";
    echo "   - 'Sent badge update broadcast'\n";
    echo "5. Go back to home - badge should disappear\n";
    echo "6. Close and reopen app - badge should stay gone\n\n";
    
    echo "✅ Test notifications created!\n";
    echo "🔍 Check Android logs to see if markAllNotificationsAsRead is being called!\n";
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}

$conn->close();
?>





















