<?php
// Test the mark_specific_messages_read.php endpoint
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "=== Testing mark_specific_messages_read.php ===\n\n";

// Test data
$test_data = [
    'user_id' => 29, // Test with user 29
    'conversation_type' => 'individual',
    'other_user_id' => 28 // Test with user 28
];

echo "Test Data:\n";
print_r($test_data);
echo "\n";

// Simulate the request
$url = "http://192.168.101.6/BoardEase2/mark_specific_messages_read.php";

$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, $url);
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($test_data));
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Content-Type: application/json',
    'Content-Length: ' . strlen(json_encode($test_data))
]);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);

$response = curl_exec($ch);
$http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

echo "HTTP Code: $http_code\n";
echo "Response: $response\n";

// Also test with direct database query
echo "\n=== Direct Database Test ===\n";
require_once 'db_helper.php';

try {
    $db = getDB();
    
    // Check current message status
    $stmt = $db->prepare("
        SELECT message_id, sender_id, receiver_id, message, msg_status, created_at 
        FROM messages 
        WHERE receiver_id = ? AND sender_id = ? 
        ORDER BY created_at DESC 
        LIMIT 5
    ");
    $stmt->execute([29, 28]);
    $messages = $stmt->fetchAll();
    
    echo "Current messages from user 28 to user 29:\n";
    foreach ($messages as $msg) {
        echo "ID: {$msg['message_id']}, Status: {$msg['msg_status']}, Message: {$msg['message']}, Time: {$msg['created_at']}\n";
    }
    
    // Test the update query
    echo "\nTesting update query...\n";
    $stmt = $db->prepare("
        UPDATE messages 
        SET msg_status = 'Read' 
        WHERE receiver_id = ? 
        AND sender_id = ? 
        AND msg_status NOT IN ('Read', 'Deleted')
        AND created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
    ");
    $result = $stmt->execute([29, 28]);
    $affected = $stmt->rowCount();
    
    echo "Update result: " . ($result ? "Success" : "Failed") . "\n";
    echo "Affected rows: $affected\n";
    
    // Check messages again
    $stmt = $db->prepare("
        SELECT message_id, sender_id, receiver_id, message, msg_status, created_at 
        FROM messages 
        WHERE receiver_id = ? AND sender_id = ? 
        ORDER BY created_at DESC 
        LIMIT 5
    ");
    $stmt->execute([29, 28]);
    $messages = $stmt->fetchAll();
    
    echo "\nMessages after update:\n";
    foreach ($messages as $msg) {
        echo "ID: {$msg['message_id']}, Status: {$msg['msg_status']}, Message: {$msg['message']}, Time: {$msg['created_at']}\n";
    }
    
} catch (Exception $e) {
    echo "Database error: " . $e->getMessage() . "\n";
}

echo "\n=== Test Complete ===\n";
?>




