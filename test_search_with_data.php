<?php
// Test search users with actual data
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "Testing search users with actual data...\n";

// First, let's check what users exist
require_once 'db_helper.php';
$db = getDB();

echo "=== Checking existing users ===\n";
$stmt = $db->prepare("
    SELECT u.user_id, r.first_name, r.last_name, r.role 
    FROM users u 
    JOIN registrations r ON u.reg_id = r.id 
    ORDER BY u.user_id
");
$stmt->execute();
$users = $stmt->fetchAll();

foreach ($users as $user) {
    echo "User ID: " . $user['user_id'] . " - " . $user['first_name'] . " " . $user['last_name'] . " (" . $user['role'] . ")\n";
}

echo "\n=== Checking active_boarders table ===\n";
$stmt = $db->prepare("SELECT * FROM active_boarders");
$stmt->execute();
$active_boarders = $stmt->fetchAll();

if (empty($active_boarders)) {
    echo "❌ No data in active_boarders table\n";
} else {
    foreach ($active_boarders as $ab) {
        echo "User ID: " . $ab['user_id'] . " - Boarding House ID: " . $ab['boarding_house_id'] . " - Status: " . $ab['status'] . "\n";
    }
}

echo "\n=== Testing search with existing user names ===\n";

// Test with first user's name
if (!empty($users)) {
    $first_user = $users[0];
    $search_term = $first_user['first_name'];
    echo "Searching for: " . $search_term . "\n";
    
    $url = "http://192.168.101.6/BoardEase2/search_users.php?current_user_id=1&search_term=" . urlencode($search_term);
    echo "URL: " . $url . "\n";
    
    $response = file_get_contents($url);
    if ($response !== false) {
        $data = json_decode($response, true);
        if ($data && isset($data['success'])) {
            echo "Success: " . ($data['success'] ? 'true' : 'false') . "\n";
            if (isset($data['data']['users'])) {
                echo "Users found: " . count($data['data']['users']) . "\n";
                foreach ($data['data']['users'] as $user) {
                    echo "- " . $user['full_name'] . " (" . $user['user_type'] . ")\n";
                }
            }
        } else {
            echo "❌ Failed to parse response\n";
        }
    } else {
        echo "❌ Failed to get response\n";
    }
}
?>




