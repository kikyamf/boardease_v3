<?php
// Test the mark_all_notifications_read.php API endpoint
$_POST['user_id'] = 1; // Simulate POST data

// Capture output
ob_start();
include 'mark_all_notifications_read.php';
$output = ob_get_clean();

echo "Mark All Notifications Read API Test:\n";
echo $output . "\n";

// Now check the unread count
echo "\nChecking unread count after marking all as read:\n";
$_GET['user_id'] = 1;
ob_start();
include 'get_unread_notif_count.php';
$count_output = ob_get_clean();
echo $count_output . "\n";
?>





















