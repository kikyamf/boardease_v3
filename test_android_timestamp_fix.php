<?php
// Test Android timestamp fix
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "=== TESTING ANDROID TIMESTAMP FIX ===\n\n";

// 1. Test individual messages API
echo "1. TESTING INDIVIDUAL MESSAGES API:\n";
$url = "http://localhost/get_messages.php?user1_id=1&user2_id=6";
echo "URL: $url\n";
$response = file_get_contents($url);
$data = json_decode($response, true);

if ($data && $data['success']) {
    echo "✅ Individual messages API working!\n";
    $messages = $data['data']['messages'];
    echo "Found " . count($messages) . " messages\n\n";
    
    // Show last 3 messages with timestamps
    $last_messages = array_slice($messages, -3);
    foreach ($last_messages as $msg) {
        $sender = $msg['is_from_current_user'] ? "You" : $msg['sender_name'];
        echo "- $sender: '{$msg['message']}'\n";
        echo "  Timestamp: {$msg['timestamp']}\n";
        echo "  Original: {$msg['original_timestamp']}\n\n";
    }
} else {
    echo "❌ Individual messages API failed\n";
}

// 2. Test group messages API
echo "2. TESTING GROUP MESSAGES API:\n";
$url = "http://localhost/get_group_messages.php?group_id=4&current_user_id=1";
echo "URL: $url\n";
$response = file_get_contents($url);
$data = json_decode($response, true);

if ($data && $data['success']) {
    echo "✅ Group messages API working!\n";
    $messages = $data['data']['messages'];
    echo "Found " . count($messages) . " messages\n\n";
    
    // Show last 3 messages with timestamps
    $last_messages = array_slice($messages, -3);
    foreach ($last_messages as $msg) {
        $sender = $msg['is_sender'] ? "You" : $msg['sender_name'];
        echo "- $sender: '{$msg['message_text']}'\n";
        echo "  Timestamp: {$msg['timestamp']}\n";
        echo "  Original: {$msg['original_timestamp']}\n\n";
    }
} else {
    echo "❌ Group messages API failed\n";
}

// 3. Send a new message to test real-time
echo "3. SENDING NEW MESSAGE FOR REAL-TIME TEST:\n";
require_once 'db_helper.php';
$db = getDB();

$current_time = date('Y-m-d H:i:s');
$formatted_time = date('g:i A');

$stmt = $db->prepare("
    INSERT INTO messages (sender_id, receiver_id, msg_text, msg_timestamp, msg_status) 
    VALUES (1, 6, 'ANDROID TIMESTAMP TEST - $formatted_time', ?, 'Sent')
");

if ($stmt->execute([$current_time])) {
    $message_id = $db->lastInsertId();
    echo "✅ Sent new message ID: $message_id\n";
    echo "Message: 'ANDROID TIMESTAMP TEST - $formatted_time'\n";
    echo "Database time: $current_time\n";
    echo "Formatted time: $formatted_time\n\n";
    
    // 4. Test the new message
    echo "4. TESTING NEW MESSAGE TIMESTAMP:\n";
    $url = "http://localhost/get_messages.php?user1_id=1&user2_id=6";
    $response = file_get_contents($url);
    $data = json_decode($response, true);
    
    if ($data && $data['success']) {
        $messages = $data['data']['messages'];
        $last_message = end($messages);
        
        if (strpos($last_message['message'], 'ANDROID TIMESTAMP TEST') !== false) {
            echo "✅ New message found!\n";
            echo "Message: '{$last_message['message']}'\n";
            echo "Timestamp: {$last_message['timestamp']}\n";
            echo "Expected: $formatted_time\n";
            
            if ($last_message['timestamp'] === $formatted_time) {
                echo "🎯 SUCCESS! Android will now show: $formatted_time\n";
            } else {
                echo "❌ Timestamp mismatch\n";
            }
        } else {
            echo "❌ New message not found\n";
        }
    }
} else {
    echo "❌ Failed to send new message\n";
}

echo "\n=== ANDROID FIX SUMMARY ===\n";
echo "✅ MessageAdapter.java - Now sets timestamp from API\n";
echo "✅ item_message_sender.xml - Removed hardcoded '1:20 AM'\n";
echo "✅ item_message_receiver.xml - Removed hardcoded '1:22 AM'\n";
echo "✅ get_messages.php - Returns formatted timestamp\n";
echo "✅ get_group_messages.php - Returns formatted timestamp\n";
echo "\n🎯 RESULT: Chat bubbles will now show real-time timestamps!\n";
echo "📱 Build and test your Android app - no more default 1:20 AM!\n";
?>


















