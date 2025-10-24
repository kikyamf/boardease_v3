<?php
// Test specific users for chat issues
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "=== TESTING SPECIFIC USERS (76, 4, 11) ===\n\n";

// Test get_chat_list.php for user 76
echo "1. Testing get_chat_list.php for user 76:\n";
$url = "http://localhost/get_chat_list.php?user_id=76";
echo "URL: $url\n";
$response = file_get_contents($url);
echo "Response: " . substr($response, 0, 500) . "...\n\n";

// Test get_chat_list.php for user 4
echo "2. Testing get_chat_list.php for user 4:\n";
$url = "http://localhost/get_chat_list.php?user_id=4";
echo "URL: $url\n";
$response = file_get_contents($url);
echo "Response: " . substr($response, 0, 500) . "...\n\n";

// Test get_chat_list.php for user 11
echo "3. Testing get_chat_list.php for user 11:\n";
$url = "http://localhost/get_chat_list.php?user_id=11";
echo "URL: $url\n";
$response = file_get_contents($url);
echo "Response: " . substr($response, 0, 500) . "...\n\n";

// Test get_unread_count.php for user 76
echo "4. Testing get_unread_count.php for user 76:\n";
$url = "http://localhost/get_unread_count.php?user_id=76";
echo "URL: $url\n";
$response = file_get_contents($url);
echo "Response: $response\n\n";

echo "=== TEST COMPLETE ===\n";
?>


















