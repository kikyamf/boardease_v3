<?php
// Test the actual API endpoint
$_GET['user_id'] = 1; // Simulate the GET parameter

// Capture output
ob_start();
include 'get_unread_notif_count.php';
$output = ob_get_clean();

echo "API Endpoint Test Result:\n";
echo $output . "\n";
?>





















