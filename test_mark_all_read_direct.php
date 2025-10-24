<?php
// Test the mark_all_notifications_read.php API directly
echo "=== TESTING MARK ALL NOTIFICATIONS READ API DIRECTLY ===\n\n";

// Simulate the exact POST request that Android is making
$_POST['user_id'] = 1;

echo "Simulating POST request with user_id = 1\n";

// Capture output
ob_start();
include 'mark_all_notifications_read.php';
$output = ob_get_clean();

echo "API Response:\n";
echo $output . "\n\n";

// Parse the response
$response = json_decode($output, true);
if ($response) {
    if ($response['success']) {
        echo "✅ SUCCESS: API is working correctly!\n";
        echo "Updated count: " . $response['data']['updated_count'] . "\n";
    } else {
        echo "❌ ERROR: " . $response['message'] . "\n";
    }
} else {
    echo "❌ ERROR: Invalid JSON response\n";
}

// Check unread count after
echo "\nChecking unread count after API call:\n";
$url = "http://192.168.101.6/BoardEase2/get_unread_notif_count.php?user_id=1";
$response = file_get_contents($url);
$data = json_decode($response, true);

if ($data && $data['success']) {
    $unread_count = $data['data']['total_unread'];
    echo "Unread count: {$unread_count}\n";
    
    if ($unread_count == 0) {
        echo "✅ All notifications marked as read!\n";
    } else {
        echo "⚠️  Still have {$unread_count} unread notifications\n";
    }
}
?>





















