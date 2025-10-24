<?php
// Test the complete badge flow as described by user
require_once 'dbConfig.php';

echo "=== TESTING NOTIFICATION BADGE FLOW ===\n\n";

try {
    $user_id = 1;
    
    // Step 1: Create some notifications
    echo "Step 1: Creating notifications...\n";
    $notifications = [
        ['New Booking', 'Someone wants to book your room', 'booking'],
        ['Payment Alert', 'Payment received from tenant', 'payment'],
        ['Maintenance', 'Elevator needs repair', 'maintenance']
    ];
    
    $insert_sql = "INSERT INTO notifications (user_id, notif_title, notif_message, notif_type, notif_status) VALUES (?, ?, ?, ?, 'unread')";
    $stmt = $conn->prepare($insert_sql);
    
    foreach ($notifications as $notif) {
        $stmt->bind_param("isss", $user_id, $notif[0], $notif[1], $notif[2]);
        $stmt->execute();
    }
    
    // Check unread count
    $stmt = $conn->prepare("SELECT COUNT(*) as unread_count FROM notifications WHERE user_id = ? AND notif_status = 'unread'");
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $unread_count = $result->fetch_assoc()['unread_count'];
    
    echo "✅ Created notifications. Unread count: {$unread_count}\n";
    echo "📱 BADGE SHOULD SHOW: {$unread_count}\n\n";
    
    // Step 2: Simulate clicking notification icon (mark all as read)
    echo "Step 2: Simulating clicking notification icon...\n";
    $stmt = $conn->prepare("UPDATE notifications SET notif_status = 'read' WHERE user_id = ? AND notif_status = 'unread'");
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $updated = $stmt->affected_rows;
    
    echo "✅ Marked {$updated} notifications as read\n";
    
    // Check new count
    $stmt = $conn->prepare("SELECT COUNT(*) as unread_count FROM notifications WHERE user_id = ? AND notif_status = 'unread'");
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $new_count = $result->fetch_assoc()['unread_count'];
    
    echo "📊 New unread count: {$new_count}\n";
    echo "📱 BADGE SHOULD BE GONE (count = {$new_count})\n\n";
    
    // Step 3: Verify badge stays gone
    echo "Step 3: Verifying badge stays gone...\n";
    if ($new_count == 0) {
        echo "✅ SUCCESS: Badge should stay gone!\n";
        echo "📱 Even after:\n";
        echo "   - Going back to home\n";
        echo "   - Closing and reopening app\n";
        echo "   - Restarting app\n";
        echo "   Badge should NOT come back!\n\n";
    } else {
        echo "❌ ERROR: Still have unread notifications\n";
    }
    
    // Step 4: Test API
    echo "Step 4: Testing API endpoint...\n";
    $url = "http://192.168.101.6/BoardEase2/get_unread_notif_count.php?user_id={$user_id}";
    $response = file_get_contents($url);
    $data = json_decode($response, true);
    
    if ($data && $data['success']) {
        $api_count = $data['data']['total_unread'];
        echo "✅ API returns: {$api_count} unread notifications\n";
        
        if ($api_count == 0) {
            echo "🎉 PERFECT! API is working correctly!\n";
        } else {
            echo "⚠️  API shows {$api_count} unread notifications\n";
        }
    }
    
    echo "\n=== SUMMARY ===\n";
    echo "The badge should work like this:\n";
    echo "1. 🔔 Show badge when notifications arrive (real-time)\n";
    echo "2. 👆 Click notification icon → Open activity\n";
    echo "3. ✅ All notifications marked as read automatically\n";
    echo "4. 🏠 Go back to home → Badge disappears\n";
    echo "5. 🔒 Badge stays gone until new notifications arrive\n";
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}

$conn->close();
?>





















