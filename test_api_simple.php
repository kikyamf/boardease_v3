<?php
// Simple API test
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "Testing API endpoint...\n";

// Include the API file directly
$_GET['current_user_id'] = '1';

// Capture output
ob_start();
include 'get_users_for_messaging.php';
$output = ob_get_clean();

echo "API Output:\n";
echo $output . "\n";

// Try to parse as JSON
$data = json_decode($output, true);
if ($data) {
    echo "\nParsed JSON successfully:\n";
    if (isset($data['success'])) {
        echo "Success: " . ($data['success'] ? 'true' : 'false') . "\n";
        if (isset($data['data']['users'])) {
            echo "Users found: " . count($data['data']['users']) . "\n";
        }
    }
} else {
    echo "\nFailed to parse JSON\n";
}
?>




