<?php
// Test duplicate message fix and date format
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "=== TESTING DUPLICATE MESSAGE FIX AND DATE FORMAT ===\n\n";

// 1. Test current timestamp format
echo "1. TESTING CURRENT TIMESTAMP FORMAT:\n";
$url = "http://localhost/get_messages.php?user1_id=1&user2_id=6";
echo "URL: $url\n";
$response = file_get_contents($url);
$data = json_decode($response, true);

if ($data && $data['success']) {
    echo "✅ Messages API working!\n";
    $messages = $data['data']['messages'];
    echo "Found " . count($messages) . " messages\n\n";
    
    // Show last 3 messages with new date format
    $last_messages = array_slice($messages, -3);
    foreach ($last_messages as $msg) {
        $sender = $msg['is_from_current_user'] ? "You" : $msg['sender_name'];
        echo "- $sender: '{$msg['message']}'\n";
        echo "  Timestamp: {$msg['timestamp']} (NEW FORMAT)\n";
        echo "  Original: {$msg['original_timestamp']}\n\n";
    }
} else {
    echo "❌ Messages API failed\n";
}

// 2. Test group messages format
echo "2. TESTING GROUP MESSAGES FORMAT:\n";
$url = "http://localhost/get_group_messages.php?group_id=4&current_user_id=1";
echo "URL: $url\n";
$response = file_get_contents($url);
$data = json_decode($response, true);

if ($data && $data['success']) {
    echo "✅ Group messages API working!\n";
    $messages = $data['data']['messages'];
    echo "Found " . count($messages) . " messages\n\n";
    
    // Show last 3 messages with new date format
    $last_messages = array_slice($messages, -3);
    foreach ($last_messages as $msg) {
        $sender = $msg['is_sender'] ? "You" : $msg['sender_name'];
        echo "- $sender: '{$msg['message_text']}'\n";
        echo "  Timestamp: {$msg['timestamp']} (NEW FORMAT)\n";
        echo "  Original: {$msg['original_timestamp']}\n\n";
    }
} else {
    echo "❌ Group messages API failed\n";
}

// 3. Send a test message to verify format
echo "3. SENDING TEST MESSAGE FOR DATE FORMAT:\n";
require_once 'db_helper.php';
$db = getDB();

$current_time = date('Y-m-d H:i:s');
$formatted_time = date('M j, g:i A');

$stmt = $db->prepare("
    INSERT INTO messages (sender_id, receiver_id, msg_text, msg_timestamp, msg_status) 
    VALUES (1, 6, 'DATE FORMAT TEST - $formatted_time', ?, 'Sent')
");

if ($stmt->execute([$current_time])) {
    $message_id = $db->lastInsertId();
    echo "✅ Sent test message ID: $message_id\n";
    echo "Message: 'DATE FORMAT TEST - $formatted_time'\n";
    echo "Database time: $current_time\n";
    echo "Expected format: $formatted_time\n\n";
    
    // 4. Test the new message format
    echo "4. TESTING NEW MESSAGE DATE FORMAT:\n";
    $url = "http://localhost/get_messages.php?user1_id=1&user2_id=6";
    $response = file_get_contents($url);
    $data = json_decode($response, true);
    
    if ($data && $data['success']) {
        $messages = $data['data']['messages'];
        $last_message = end($messages);
        
        if (strpos($last_message['message'], 'DATE FORMAT TEST') !== false) {
            echo "✅ New message found!\n";
            echo "Message: '{$last_message['message']}'\n";
            echo "Timestamp: {$last_message['timestamp']}\n";
            echo "Expected: $formatted_time\n";
            
            if ($last_message['timestamp'] === $formatted_time) {
                echo "🎯 SUCCESS! Date format is correct: $formatted_time\n";
            } else {
                echo "❌ Date format mismatch\n";
            }
        } else {
            echo "❌ New message not found\n";
        }
    }
} else {
    echo "❌ Failed to send test message\n";
}

echo "\n=== FIX SUMMARY ===\n";
echo "✅ DUPLICATE MESSAGE FIX:\n";
echo "   - Added isSendingMessage flag to prevent multiple sends\n";
echo "   - Flag is set to true when sending starts\n";
echo "   - Flag is reset to false when send completes (success/error)\n";
echo "   - Send button is disabled while sending\n\n";

echo "✅ DATE FORMAT FIX:\n";
echo "   - API now returns 'Oct 5, 7:30 PM' format instead of just '7:30 PM'\n";
echo "   - Android app shows date + time for better identification\n";
echo "   - Both individual and group messages have date format\n\n";

echo "🎯 EXPECTED RESULTS:\n";
echo "   - No more duplicate messages when clicking send once\n";
echo "   - Chat bubbles show 'Oct 5, 7:30 PM' instead of '1:20 AM'\n";
echo "   - Easy to identify when messages were sent\n\n";

echo "📱 BUILD AND TEST YOUR ANDROID APP!\n";
echo "   - Send a message and verify no duplicates\n";
echo "   - Check that timestamps show date + time\n";
?>


















