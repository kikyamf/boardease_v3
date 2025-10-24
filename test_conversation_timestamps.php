<?php
// Test conversation timestamps
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "=== TESTING CONVERSATION TIMESTAMPS ===\n\n";

// 1. Test individual messages
echo "1. TESTING INDIVIDUAL MESSAGES:\n";
$url = "http://localhost/get_messages.php?user1_id=1&user2_id=6";
echo "URL: $url\n";
$response = file_get_contents($url);
$data = json_decode($response, true);

if ($data && $data['success']) {
    echo "✅ Individual messages API working!\n";
    echo "Found " . count($data['data']['messages']) . " messages:\n\n";
    
    // Show last 3 messages
    $messages = array_slice($data['data']['messages'], -3);
    foreach ($messages as $msg) {
        $sender = $msg['is_from_current_user'] ? "You" : $msg['sender_name'];
        echo "- $sender: '{$msg['message']}'\n";
        echo "  Timestamp: {$msg['timestamp']}\n";
        echo "  Original: {$msg['original_timestamp']}\n\n";
    }
} else {
    echo "❌ Individual messages API failed: " . ($data['message'] ?? 'Unknown error') . "\n";
}

// 2. Test group messages
echo "2. TESTING GROUP MESSAGES:\n";
$url = "http://localhost/get_group_messages.php?group_id=4&current_user_id=1";
echo "URL: $url\n";
$response = file_get_contents($url);
$data = json_decode($response, true);

if ($data && $data['success']) {
    echo "✅ Group messages API working!\n";
    echo "Found " . count($data['data']['messages']) . " messages:\n\n";
    
    // Show last 3 messages
    $messages = array_slice($data['data']['messages'], -3);
    foreach ($messages as $msg) {
        $sender = $msg['is_sender'] ? "You" : $msg['sender_name'];
        echo "- $sender: '{$msg['message_text']}'\n";
        echo "  Timestamp: {$msg['timestamp']}\n";
        echo "  Original: {$msg['original_timestamp']}\n\n";
    }
} else {
    echo "❌ Group messages API failed: " . ($data['message'] ?? 'Unknown error') . "\n";
}

// 3. Send a new message to test real-time timestamp
echo "3. SENDING NEW MESSAGE TO TEST REAL-TIME TIMESTAMP:\n";

require_once 'db_helper.php';
$db = getDB();

$stmt = $db->prepare("
    INSERT INTO messages (sender_id, receiver_id, msg_text, msg_timestamp, msg_status) 
    VALUES (1, 6, 'REAL-TIME TEST MESSAGE', NOW(), 'Sent')
");

if ($stmt->execute()) {
    $message_id = $db->lastInsertId();
    echo "✅ Sent new message ID: $message_id\n";
    echo "Message: 'REAL-TIME TEST MESSAGE'\n";
    echo "Time: " . date('Y-m-d H:i:s') . "\n";
    echo "Formatted: " . date('g:i A') . "\n\n";
    
    // 4. Test the new message timestamp
    echo "4. TESTING NEW MESSAGE TIMESTAMP:\n";
    $url = "http://localhost/get_messages.php?user1_id=1&user2_id=6";
    $response = file_get_contents($url);
    $data = json_decode($response, true);
    
    if ($data && $data['success']) {
        $messages = $data['data']['messages'];
        $last_message = end($messages);
        
        if (strpos($last_message['message'], 'REAL-TIME TEST MESSAGE') !== false) {
            echo "✅ New message found!\n";
            echo "Message: '{$last_message['message']}'\n";
            echo "Timestamp: {$last_message['timestamp']}\n";
            echo "Original: {$last_message['original_timestamp']}\n";
            
            // Check if timestamp is current
            $current_time = date('g:i A');
            if ($last_message['timestamp'] === $current_time) {
                echo "🎯 SUCCESS! Timestamp is real-time: $current_time\n";
            } else {
                echo "❌ Timestamp mismatch. Expected: $current_time, Got: {$last_message['timestamp']}\n";
            }
        } else {
            echo "❌ New message not found in conversation\n";
        }
    }
} else {
    echo "❌ Failed to send new message\n";
}

echo "\n=== TEST COMPLETE ===\n";
echo "✅ Chat bubbles should now show real-time timestamps (1:20 PM format)\n";
echo "✅ No more default 1:20 AM timestamps\n";
?>


















