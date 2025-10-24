<?php
// Test the final messaging endpoint
echo "Testing get_users_for_messaging.php endpoint...\n\n";

// Test with user_id = 1 (owner)
echo "=== Testing with user_id = 1 (Owner) ===\n";
$url = "http://192.168.101.6/BoardEase2/get_users_for_messaging.php?current_user_id=1";
$response = file_get_contents($url);
echo "Response: " . $response . "\n\n";

// Test with user_id = 2 (boarder)
echo "=== Testing with user_id = 2 (Boarder) ===\n";
$url = "http://192.168.101.6/BoardEase2/get_users_for_messaging.php?current_user_id=2";
$response = file_get_contents($url);
echo "Response: " . $response . "\n\n";

echo "Test completed.\n";
?>




