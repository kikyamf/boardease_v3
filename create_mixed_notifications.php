<?php
// Create a mix of read and unread notifications to test uniform appearance
require_once 'dbConfig.php';

echo "=== CREATING MIXED NOTIFICATIONS FOR UNIFORM APPEARANCE TEST ===\n\n";

try {
    $user_id = 1;
    
    // First, create some unread notifications
    echo "Step 1: Creating unread notifications...\n";
    $unread_notifications = [
        ['🔔 New Message', 'You have a new message from tenant', 'general'],
        ['💰 Payment Alert', 'Rent payment received', 'payment'],
        ['📅 Booking Request', 'New booking request received', 'booking']
    ];
    
    $insert_sql = "INSERT INTO notifications (user_id, notif_title, notif_message, notif_type, notif_status) VALUES (?, ?, ?, ?, 'unread')";
    $stmt = $conn->prepare($insert_sql);
    
    foreach ($unread_notifications as $notif) {
        $stmt->bind_param("isss", $user_id, $notif[0], $notif[1], $notif[2]);
        $stmt->execute();
        echo "✅ Created unread: {$notif[0]}\n";
    }
    
    // Then create some read notifications
    echo "\nStep 2: Creating read notifications...\n";
    $read_notifications = [
        ['✅ Maintenance Complete', 'Elevator maintenance completed', 'maintenance'],
        ['📢 Announcement', 'Monthly meeting scheduled', 'announcement'],
        ['💳 Payment Processed', 'Utility bill payment processed', 'payment']
    ];
    
    foreach ($read_notifications as $notif) {
        $stmt->bind_param("isss", $user_id, $notif[0], $notif[1], $notif[2]);
        $stmt->execute();
        echo "✅ Created read: {$notif[0]}\n";
    }
    
    // Check counts
    $stmt = $conn->prepare("SELECT COUNT(*) as unread_count FROM notifications WHERE user_id = ? AND notif_status = 'unread'");
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $unread_count = $result->fetch_assoc()['unread_count'];
    
    $stmt = $conn->prepare("SELECT COUNT(*) as read_count FROM notifications WHERE user_id = ? AND notif_status = 'read'");
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $read_count = $result->fetch_assoc()['read_count'];
    
    echo "\n📊 Notification Summary:\n";
    echo "   - Unread notifications: {$unread_count}\n";
    echo "   - Read notifications: {$read_count}\n";
    echo "   - Total notifications: " . ($unread_count + $read_count) . "\n\n";
    
    echo "📱 Android App Test:\n";
    echo "1. Open the notification activity\n";
    echo "2. You should see ALL notifications with the SAME appearance\n";
    echo "3. Read and unread notifications should look identical\n";
    echo "4. No gray text or reduced opacity for read notifications\n";
    echo "5. Badge should show: {$unread_count} (only unread count)\n\n";
    
    echo "✅ Mixed notifications created successfully!\n";
    echo "🎨 All notifications now have uniform appearance!\n";
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}

$conn->close();
?>





















