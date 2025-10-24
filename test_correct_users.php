<?php
// Test chat system with correct user IDs (4 and 11)
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $db = getDB();
    
    echo "=== TESTING CHAT SYSTEM WITH CORRECT USER IDs ===\n\n";
    
    // 1. Test chat list for user 4 (sender of message 76)
    echo "1. CHAT LIST FOR USER 4 (Sender of message 76):\n";
    $url = "http://localhost/get_chat_list_simple.php?user_id=4";
    echo "URL: $url\n";
    $response = file_get_contents($url);
    $data = json_decode($response, true);
    
    if ($data && $data['success']) {
        echo "✅ Chat list found " . count($data['data']['chats']) . " chats\n";
        foreach ($data['data']['chats'] as $chat) {
            echo "- Chat with user {$chat['other_user_id']}: {$chat['other_user_name']} | Last: '{$chat['last_message']}' | Time: {$chat['last_message_time']} | Unread: {$chat['unread_count']}\n";
        }
    } else {
        echo "❌ Failed to get chat list: " . ($data['message'] ?? 'Unknown error') . "\n";
    }
    
    // 2. Test chat list for user 11 (receiver of message 76)
    echo "\n2. CHAT LIST FOR USER 11 (Receiver of message 76):\n";
    $url = "http://localhost/get_chat_list_simple.php?user_id=11";
    echo "URL: $url\n";
    $response = file_get_contents($url);
    $data = json_decode($response, true);
    
    if ($data && $data['success']) {
        echo "✅ Chat list found " . count($data['data']['chats']) . " chats\n";
        foreach ($data['data']['chats'] as $chat) {
            echo "- Chat with user {$chat['other_user_id']}: {$chat['other_user_name']} | Last: '{$chat['last_message']}' | Time: {$chat['last_message_time']} | Unread: {$chat['unread_count']}\n";
        }
    } else {
        echo "❌ Failed to get chat list: " . ($data['message'] ?? 'Unknown error') . "\n";
    }
    
    // 3. Test unread counts
    echo "\n3. UNREAD COUNTS:\n";
    
    // Unread count for user 4
    $stmt = $db->prepare("
        SELECT COUNT(*) as unread_count
        FROM messages
        WHERE receiver_id = 4 AND msg_status NOT IN ('Read', 'Deleted')
    ");
    $stmt->execute();
    $unread_4 = $stmt->fetch()['unread_count'];
    echo "Unread messages for user 4: $unread_4\n";
    
    // Unread count for user 11
    $stmt = $db->prepare("
        SELECT COUNT(*) as unread_count
        FROM messages
        WHERE receiver_id = 11 AND msg_status NOT IN ('Read', 'Deleted')
    ");
    $stmt->execute();
    $unread_11 = $stmt->fetch()['unread_count'];
    echo "Unread messages for user 11: $unread_11\n";
    
    // 4. Test the original get_chat_list.php API
    echo "\n4. TESTING ORIGINAL API:\n";
    
    // Test for user 4
    echo "\n4.1 Original API for user 4:\n";
    $url = "http://localhost/get_chat_list.php?user_id=4";
    echo "URL: $url\n";
    $response = file_get_contents($url);
    $data = json_decode($response, true);
    
    if ($data && $data['success']) {
        echo "✅ Original API found " . count($data['data']['chats']) . " chats\n";
        foreach ($data['data']['chats'] as $chat) {
            echo "- Chat with user {$chat['other_user_id']}: {$chat['other_user_name']} | Last: '{$chat['last_message']}' | Time: {$chat['last_message_time']} | Unread: {$chat['unread_count']}\n";
        }
    } else {
        echo "❌ Original API failed: " . ($data['message'] ?? 'Unknown error') . "\n";
    }
    
    // Test for user 11
    echo "\n4.2 Original API for user 11:\n";
    $url = "http://localhost/get_chat_list.php?user_id=11";
    echo "URL: $url\n";
    $response = file_get_contents($url);
    $data = json_decode($response, true);
    
    if ($data && $data['success']) {
        echo "✅ Original API found " . count($data['data']['chats']) . " chats\n";
        foreach ($data['data']['chats'] as $chat) {
            echo "- Chat with user {$chat['other_user_id']}: {$chat['other_user_name']} | Last: '{$chat['last_message']}' | Time: {$chat['last_message_time']} | Unread: {$chat['unread_count']}\n";
        }
    } else {
        echo "❌ Original API failed: " . ($data['message'] ?? 'Unknown error') . "\n";
    }
    
    // 5. Send a new message to test the system
    echo "\n5. SENDING NEW TEST MESSAGE:\n";
    
    // Send message from user 11 to user 4
    $stmt = $db->prepare("
        INSERT INTO messages (sender_id, receiver_id, msg_text, msg_timestamp, msg_status) 
        VALUES (11, 4, 'Reply from user 11 to user 4', NOW(), 'Sent')
    ");
    
    if ($stmt->execute()) {
        $message_id = $db->lastInsertId();
        echo "✅ Sent new message ID: $message_id (11 -> 4)\n";
        
        // Test chat list again
        echo "\n6. TESTING CHAT LIST AFTER NEW MESSAGE:\n";
        $url = "http://localhost/get_chat_list_simple.php?user_id=4";
        $response = file_get_contents($url);
        $data = json_decode($response, true);
        
        if ($data && $data['success']) {
            echo "✅ Updated chat list for user 4:\n";
            foreach ($data['data']['chats'] as $chat) {
                echo "- Chat with user {$chat['other_user_id']}: {$chat['other_user_name']} | Last: '{$chat['last_message']}' | Time: {$chat['last_message_time']} | Unread: {$chat['unread_count']}\n";
            }
        }
    } else {
        echo "❌ Failed to send new message\n";
    }
    
    echo "\n=== TEST COMPLETE ===\n";
    
} catch (Exception $e) {
    echo "ERROR: " . $e->getMessage() . "\n";
}
?>


















