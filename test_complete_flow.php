<?php
// Test the complete notification badge flow
require_once 'dbConfig.php';

echo "=== NOTIFICATION BADGE COMPLETE FLOW TEST ===\n\n";

try {
    $user_id = 1;
    
    // Step 1: Create test notifications
    echo "Step 1: Creating test notifications...\n";
    $test_notifications = [
        ['Test Notification 1', 'This is a test notification', 'general'],
        ['Test Notification 2', 'Another test notification', 'booking'],
        ['Test Notification 3', 'Third test notification', 'payment']
    ];
    
    $insert_sql = "INSERT INTO notifications (user_id, notif_title, notif_message, notif_type, notif_status) VALUES (?, ?, ?, ?, 'unread')";
    $stmt = $conn->prepare($insert_sql);
    
    foreach ($test_notifications as $notif) {
        $stmt->bind_param("isss", $user_id, $notif[0], $notif[1], $notif[2]);
        $stmt->execute();
    }
    
    // Check unread count
    $stmt = $conn->prepare("SELECT COUNT(*) as unread_count FROM notifications WHERE user_id = ? AND notif_status = 'unread'");
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $unread_count = $result->fetch_assoc()['unread_count'];
    
    echo "✓ Created notifications. Unread count: {$unread_count}\n";
    echo "📱 Android app should show badge with count: {$unread_count}\n\n";
    
    // Step 2: Simulate opening notification activity (mark all as read)
    echo "Step 2: Simulating opening notification activity...\n";
    $stmt = $conn->prepare("UPDATE notifications SET notif_status = 'read' WHERE user_id = ? AND notif_status = 'unread'");
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $updated_count = $stmt->affected_rows;
    
    echo "✓ Marked {$updated_count} notifications as read\n";
    
    // Check new unread count
    $stmt = $conn->prepare("SELECT COUNT(*) as unread_count FROM notifications WHERE user_id = ? AND notif_status = 'unread'");
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $new_unread_count = $result->fetch_assoc()['unread_count'];
    
    echo "✓ New unread count: {$new_unread_count}\n";
    echo "📱 Android app badge should now be GONE\n\n";
    
    // Step 3: Verify badge stays gone
    echo "Step 3: Verifying badge stays gone...\n";
    if ($new_unread_count == 0) {
        echo "✅ SUCCESS: Badge should stay gone!\n";
        echo "📱 Even if you:\n";
        echo "   - Go back to home screen\n";
        echo "   - Close and reopen the app\n";
        echo "   - Restart the app\n";
        echo "   The badge should NOT come back because notifications are read.\n\n";
    } else {
        echo "❌ ERROR: Still have unread notifications\n";
    }
    
    // Step 4: Test API endpoint
    echo "Step 4: Testing API endpoint...\n";
    $url = "http://192.168.101.6/BoardEase2/get_unread_notif_count.php?user_id={$user_id}";
    $response = file_get_contents($url);
    $data = json_decode($response, true);
    
    if ($data && $data['success']) {
        $api_count = $data['data']['total_unread'];
        echo "✓ API returns unread count: {$api_count}\n";
        
        if ($api_count == 0) {
            echo "✅ API is working correctly!\n";
        } else {
            echo "❌ API mismatch!\n";
        }
    } else {
        echo "❌ API error\n";
    }
    
    echo "\n=== TEST COMPLETE ===\n";
    echo "The notification badge system should work as follows:\n";
    echo "1. Badge shows when there are unread notifications\n";
    echo "2. Badge disappears when opening notification activity\n";
    echo "3. Badge stays gone until new notifications arrive\n";
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}

$conn->close();
?>





















