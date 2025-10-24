<?php
// Test the mark_all_notifications_read.php API endpoint
echo "=== TESTING MARK ALL NOTIFICATIONS AS READ API ===\n\n";

// Simulate POST request
$_POST['user_id'] = 1;

// Capture output
ob_start();
include 'mark_all_notifications_read.php';
$output = ob_get_clean();

echo "Mark All Notifications Read API Response:\n";
echo $output . "\n\n";

// Now check the unread count after marking all as read
echo "Checking unread count after marking all as read:\n";
$url = "http://192.168.101.6/BoardEase2/get_unread_notif_count.php?user_id=1";
$response = file_get_contents($url);
$data = json_decode($response, true);

if ($data && $data['success']) {
    $unread_count = $data['data']['total_unread'];
    echo "Unread count: {$unread_count}\n";
    
    if ($unread_count == 0) {
        echo "✅ SUCCESS: All notifications marked as read!\n";
        echo "📱 Badge should now be gone!\n";
    } else {
        echo "❌ ERROR: Still have {$unread_count} unread notifications\n";
    }
} else {
    echo "❌ ERROR: Failed to get unread count\n";
}
?>





















