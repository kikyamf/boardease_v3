<?php
// Test to verify user 76 chat issue
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $db = getDB();
    
    echo "=== TESTING USER 76 CHAT ISSUE ===\n\n";
    
    // 1. Check if user 76 exists
    echo "1. CHECKING IF USER 76 EXISTS:\n";
    $stmt = $db->prepare("
        SELECT 
            u.user_id,
            u.status as user_status,
            r.f_name,
            r.l_name,
            r.status as reg_status
        FROM users u
        JOIN registration r ON u.reg_id = r.reg_id
        WHERE u.user_id = 76
    ");
    $stmt->execute();
    $user_76 = $stmt->fetch();
    
    if ($user_76) {
        echo "✅ User 76 exists: {$user_76['f_name']} {$user_76['l_name']} | Status: {$user_76['user_status']} | Reg Status: {$user_76['reg_status']}\n";
    } else {
        echo "❌ User 76 does NOT exist in database\n";
    }
    
    // 2. Check all messages involving user 76
    echo "\n2. CHECKING ALL MESSAGES INVOLVING USER 76:\n";
    $stmt = $db->prepare("
        SELECT 
            message_id,
            sender_id,
            receiver_id,
            msg_text,
            msg_status,
            msg_timestamp
        FROM messages 
        WHERE sender_id = 76 OR receiver_id = 76
        ORDER BY msg_timestamp DESC
    ");
    $stmt->execute();
    $messages_76 = $stmt->fetchAll();
    
    echo "Found " . count($messages_76) . " messages involving user 76:\n";
    foreach ($messages_76 as $msg) {
        echo "- ID {$msg['message_id']}: {$msg['sender_id']} -> {$msg['receiver_id']} | Status: '{$msg['msg_status']}' | Time: {$msg['msg_timestamp']} | Text: " . substr($msg['msg_text'], 0, 50) . "...\n";
    }
    
    // 3. Create a test message involving user 76
    echo "\n3. CREATING TEST MESSAGE INVOLVING USER 76:\n";
    
    // Send message from user 76 to user 4
    $stmt = $db->prepare("
        INSERT INTO messages (sender_id, receiver_id, msg_text, msg_timestamp, msg_status) 
        VALUES (76, 4, 'Test message from user 76', NOW(), 'Sent')
    ");
    
    if ($stmt->execute()) {
        $message_id = $db->lastInsertId();
        echo "✅ Created test message ID: $message_id (76 -> 4)\n";
    } else {
        echo "❌ Failed to create test message\n";
    }
    
    // 4. Test chat list for user 76 again
    echo "\n4. TESTING CHAT LIST FOR USER 76 AFTER CREATING MESSAGE:\n";
    $stmt = $db->prepare("
        SELECT 
            other_user_id,
            first_name,
            last_name,
            user_type,
            user_status,
            last_message,
            last_message_time,
            last_message_status,
            last_sender_id,
            unread_count
        FROM (
            SELECT 
                CASE 
                    WHEN m.sender_id = 76 THEN m.receiver_id
                    ELSE m.sender_id
                END as other_user_id,
                r.f_name as first_name,
                r.l_name as last_name,
                r.role as user_type,
                u.status as user_status,
                m.msg_text as last_message,
                m.msg_timestamp as last_message_time,
                m.msg_status as last_message_status,
                m.sender_id as last_sender_id,
                (SELECT COUNT(*) FROM messages WHERE receiver_id = 76 AND sender_id = other_user_id AND msg_status NOT IN ('Read', 'Deleted')) as unread_count
            FROM messages m
            JOIN users u ON (
                CASE 
                    WHEN m.sender_id = 76 THEN m.receiver_id
                    ELSE m.sender_id
                END = u.user_id
            )
            JOIN registration r ON u.reg_id = r.reg_id
            WHERE (m.sender_id = 76 OR m.receiver_id = 76) 
            AND m.msg_status != 'Deleted'
            ORDER BY m.msg_timestamp DESC
        ) as chat_messages
        GROUP BY other_user_id
        ORDER BY last_message_time DESC
    ");
    $stmt->execute();
    $chats = $stmt->fetchAll();
    
    echo "Found " . count($chats) . " chats for user 76:\n";
    foreach ($chats as $chat) {
        echo "- User {$chat['other_user_id']}: {$chat['first_name']} {$chat['last_name']} | Last: '{$chat['last_message']}' | Time: {$chat['last_message_time']} | Unread: {$chat['unread_count']}\n";
    }
    
    // 5. Test the simplified chat list API
    echo "\n5. TESTING SIMPLIFIED CHAT LIST API:\n";
    $url = "http://localhost/get_chat_list_simple.php?user_id=76";
    echo "URL: $url\n";
    $response = file_get_contents($url);
    echo "Response: " . substr($response, 0, 500) . "...\n";
    
    echo "\n=== TEST COMPLETE ===\n";
    
} catch (Exception $e) {
    echo "ERROR: " . $e->getMessage() . "\n";
}
?>


















