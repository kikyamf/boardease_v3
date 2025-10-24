<?php
// Create notifications to test real-time badge updates
require_once 'dbConfig.php';

echo "=== REAL-TIME NOTIFICATION BADGE TEST ===\n\n";

try {
    $user_id = 1;
    
    // Create multiple notifications with delays to test real-time updates
    $notifications = [
        ['🚨 URGENT: Fire Drill', 'Fire drill scheduled for tomorrow at 2:00 PM', 'announcement'],
        ['💰 Payment Received', 'Payment of ₱5,000 received from John Doe', 'payment'],
        ['🔧 Maintenance Alert', 'Elevator maintenance completed', 'maintenance'],
        ['📅 New Booking', 'Jane Smith wants to book Room 201', 'booking'],
        ['📢 General Notice', 'Water supply will be interrupted tomorrow', 'general']
    ];
    
    $insert_sql = "INSERT INTO notifications (user_id, notif_title, notif_message, notif_type, notif_status) VALUES (?, ?, ?, ?, 'unread')";
    $stmt = $conn->prepare($insert_sql);
    
    foreach ($notifications as $i => $notif) {
        $stmt->bind_param("isss", $user_id, $notif[0], $notif[1], $notif[2]);
        if ($stmt->execute()) {
            echo "✅ Created notification " . ($i + 1) . ": {$notif[0]}\n";
        }
        
        // Small delay between notifications
        usleep(500000); // 0.5 seconds
    }
    
    // Check final unread count
    $stmt = $conn->prepare("SELECT COUNT(*) as unread_count FROM notifications WHERE user_id = ? AND notif_status = 'unread'");
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $unread_count = $result->fetch_assoc()['unread_count'];
    
    echo "\n📊 Total unread notifications: {$unread_count}\n";
    echo "📱 Android app badge should show: {$unread_count}\n";
    echo "🔄 Badge should update in real-time (every 10 seconds)\n\n";
    
    echo "=== TEST INSTRUCTIONS ===\n";
    echo "1. Open your Android app\n";
    echo "2. Look at the notification icon - you should see badge with count: {$unread_count}\n";
    echo "3. Click the notification icon to open notifications\n";
    echo "4. All notifications will be marked as read automatically\n";
    echo "5. Go back to home - badge should disappear\n";
    echo "6. Badge should stay gone even if you close/reopen the app\n\n";
    
    echo "✅ Test notifications created successfully!\n";
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}

$conn->close();
?>





















