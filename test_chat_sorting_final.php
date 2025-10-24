<?php
// Test final chat sorting and timestamps
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

echo "=== TESTING FINAL CHAT SORTING AND TIMESTAMPS ===\n\n";

try {
    $db = getDB();
    
    // 1. Send a new message to test sorting
    echo "1. SENDING NEW MESSAGE:\n";
    $stmt = $db->prepare("
        INSERT INTO messages (sender_id, receiver_id, msg_text, msg_timestamp, msg_status) 
        VALUES (1, 6, 'NEWEST MESSAGE - Should be at top!', NOW(), 'Sent')
    ");
    
    if ($stmt->execute()) {
        $message_id = $db->lastInsertId();
        echo "✅ Sent new message ID: $message_id\n";
        echo "Message: 'NEWEST MESSAGE - Should be at top!'\n";
        echo "Time: " . date('Y-m-d H:i:s') . "\n\n";
    }
    
    // 2. Test get_chat_list.php
    echo "2. TESTING GET_CHAT_LIST.PHP:\n";
    $url = "http://localhost/get_chat_list.php?user_id=1";
    echo "URL: $url\n";
    $response = file_get_contents($url);
    $data = json_decode($response, true);
    
    if ($data && $data['success']) {
        echo "✅ Chat list API working!\n";
        echo "Found " . count($data['data']['chats']) . " chats:\n\n";
        
        foreach ($data['data']['chats'] as $index => $chat) {
            echo ($index + 1) . ". {$chat['other_user_name']}\n";
            echo "   Last message: '{$chat['last_message']}'\n";
            echo "   Time: {$chat['last_message_time']}\n";
            echo "   Unread: {$chat['unread_count']}\n\n";
        }
        
        // Check if new message is at the top
        $first_chat = $data['data']['chats'][0];
        if (strpos($first_chat['last_message'], 'NEWEST MESSAGE') !== false) {
            echo "🎯 SUCCESS! New message appears at the top!\n";
        } else {
            echo "❌ New message is not at the top. Sorting issue.\n";
        }
    } else {
        echo "❌ Chat list API failed: " . ($data['message'] ?? 'Unknown error') . "\n";
    }
    
    // 3. Test get_messages.php for real-time timestamps
    echo "\n3. TESTING GET_MESSAGES.PHP (Real-time timestamps):\n";
    $url = "http://localhost/get_messages.php?user1_id=1&user2_id=6";
    echo "URL: $url\n";
    $response = file_get_contents($url);
    $data = json_decode($response, true);
    
    if ($data && $data['success']) {
        echo "✅ Messages API working!\n";
        echo "Found " . count($data['data']['messages']) . " messages:\n\n";
        
        // Show last 5 messages
        $messages = array_slice($data['data']['messages'], -5);
        foreach ($messages as $msg) {
            $sender = $msg['is_from_current_user'] ? "You" : $msg['sender_name'];
            echo "- $sender: '{$msg['message']}'\n";
            echo "  Time: {$msg['formatted_timestamp']} (Original: {$msg['timestamp']})\n\n";
        }
    } else {
        echo "❌ Messages API failed: " . ($data['message'] ?? 'Unknown error') . "\n";
    }
    
    // 4. Check current server time
    echo "4. CURRENT SERVER TIME:\n";
    echo "Server time: " . date('Y-m-d H:i:s') . "\n";
    echo "Formatted: " . date('g:i A') . "\n";
    
    echo "\n=== TEST COMPLETE ===\n";
    echo "✅ Chat list should show actual times (1:20 PM format)\n";
    echo "✅ New messages should appear at the top\n";
    echo "✅ Conversation timestamps should be real-time\n";
    
} catch (Exception $e) {
    echo "ERROR: " . $e->getMessage() . "\n";
}
?>


















