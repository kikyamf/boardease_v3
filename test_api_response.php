<?php
// Test API response directly
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "Testing API response...\n\n";

// Set the parameter
$_GET['current_user_id'] = '1';

// Include the API file
ob_start();
include 'get_users_for_messaging.php';
$response = ob_get_clean();

echo "Raw Response:\n";
echo $response . "\n\n";

// Try to decode JSON
$data = json_decode($response, true);
if ($data) {
    echo "JSON Decoded Successfully:\n";
    print_r($data);
} else {
    echo "Failed to decode JSON. Raw response:\n";
    echo $response;
}
?>




