<?php
// Send test message between existing users
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $db = getDB();
    
    echo "=== SENDING TEST MESSAGE BETWEEN EXISTING USERS ===\n\n";
    
    // 1. Get first two users from database
    $stmt = $db->prepare("
        SELECT 
            u.user_id,
            r.f_name,
            r.l_name
        FROM users u
        JOIN registrations r ON u.reg_id = r.reg_id
        ORDER BY u.user_id
        LIMIT 2
    ");
    $stmt->execute();
    $users = $stmt->fetchAll();
    
    if (count($users) < 2) {
        echo "❌ Need at least 2 users in database to test messaging\n";
        exit;
    }
    
    $user1 = $users[0];
    $user2 = $users[1];
    
    echo "Using users for test:\n";
    echo "- User {$user1['user_id']}: {$user1['f_name']} {$user1['l_name']}\n";
    echo "- User {$user2['user_id']}: {$user2['f_name']} {$user2['l_name']}\n\n";
    
    // 2. Send message from user1 to user2
    $sender_id = $user1['user_id'];
    $receiver_id = $user2['user_id'];
    $message_text = "Hello! This is a test message from " . $user1['f_name'] . " to " . $user2['f_name'] . ".";
    
    $stmt = $db->prepare("
        INSERT INTO messages (sender_id, receiver_id, msg_text, msg_timestamp, msg_status) 
        VALUES (?, ?, ?, NOW(), 'Sent')
    ");
    
    $result = $stmt->execute([$sender_id, $receiver_id, $message_text]);
    
    if ($result) {
        $message_id = $db->lastInsertId();
        echo "✅ Message sent successfully!\n";
        echo "Message ID: $message_id\n";
        echo "From: User $sender_id ({$user1['f_name']} {$user1['l_name']})\n";
        echo "To: User $receiver_id ({$user2['f_name']} {$user2['l_name']})\n";
        echo "Text: $message_text\n";
        echo "Status: Sent\n";
        echo "Timestamp: " . date('Y-m-d H:i:s') . "\n";
        
        // 3. Test chat list for both users
        echo "\n=== TESTING CHAT LISTS ===\n";
        
        // Test for sender
        echo "\n3.1 CHAT LIST FOR USER $sender_id (Sender):\n";
        $url = "http://localhost/get_chat_list_simple.php?user_id=$sender_id";
        echo "URL: $url\n";
        $response = file_get_contents($url);
        $data = json_decode($response, true);
        
        if ($data && $data['success']) {
            echo "✅ Chat list found " . count($data['data']['chats']) . " chats\n";
            foreach ($data['data']['chats'] as $chat) {
                echo "- Chat with user {$chat['other_user_id']}: {$chat['other_user_name']} | Last: '{$chat['last_message']}' | Unread: {$chat['unread_count']}\n";
            }
        } else {
            echo "❌ Failed to get chat list: " . ($data['message'] ?? 'Unknown error') . "\n";
        }
        
        // Test for receiver
        echo "\n3.2 CHAT LIST FOR USER $receiver_id (Receiver):\n";
        $url = "http://localhost/get_chat_list_simple.php?user_id=$receiver_id";
        echo "URL: $url\n";
        $response = file_get_contents($url);
        $data = json_decode($response, true);
        
        if ($data && $data['success']) {
            echo "✅ Chat list found " . count($data['data']['chats']) . " chats\n";
            foreach ($data['data']['chats'] as $chat) {
                echo "- Chat with user {$chat['other_user_id']}: {$chat['other_user_name']} | Last: '{$chat['last_message']}' | Unread: {$chat['unread_count']}\n";
            }
        } else {
            echo "❌ Failed to get chat list: " . ($data['message'] ?? 'Unknown error') . "\n";
        }
        
        // 4. Test unread count
        echo "\n4. TESTING UNREAD COUNTS:\n";
        
        // Unread count for receiver
        $stmt = $db->prepare("
            SELECT COUNT(*) as unread_count
            FROM messages
            WHERE receiver_id = ? AND msg_status NOT IN ('Read', 'Deleted')
        ");
        $stmt->execute([$receiver_id]);
        $unread_count = $stmt->fetch()['unread_count'];
        echo "Unread messages for user $receiver_id: $unread_count\n";
        
    } else {
        echo "❌ Failed to send message\n";
        echo "Error: " . $stmt->error . "\n";
    }
    
    echo "\n=== TEST COMPLETE ===\n";
    
} catch (Exception $e) {
    echo "ERROR: " . $e->getMessage() . "\n";
}
?>








