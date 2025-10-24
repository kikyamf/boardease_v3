<?php
// Test delete verification
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $user_id = 1;
    $chat_id = 6; // User 6 (David Brown)
    
    $db = getDB();
    
    echo "=== TESTING DELETE VERIFICATION ===\n";
    echo "User ID: $user_id\n";
    echo "Chat ID: $chat_id\n\n";
    
    // Check if there are any non-deleted messages between user 1 and 6
    $stmt = $db->prepare("
        SELECT 
            message_id,
            sender_id,
            receiver_id,
            msg_status,
            msg_text,
            msg_timestamp
        FROM messages 
        WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)
        ORDER BY msg_timestamp DESC
    ");
    $stmt->execute([$user_id, $chat_id, $chat_id, $user_id]);
    $all_messages = $stmt->fetchAll();
    
    echo "=== ALL MESSAGES BETWEEN USER 1 AND 6 ===\n";
    echo "Total messages: " . count($all_messages) . "\n";
    
    $deleted_count = 0;
    $non_deleted_count = 0;
    
    foreach ($all_messages as $msg) {
        if ($msg['msg_status'] == 'Deleted') {
            $deleted_count++;
        } else {
            $non_deleted_count++;
            echo "  NON-DELETED: ID {$msg['message_id']} | Status: '{$msg['msg_status']}' | Text: " . substr($msg['msg_text'], 0, 30) . "...\n";
        }
    }
    
    echo "\nDeleted messages: $deleted_count\n";
    echo "Non-deleted messages: $non_deleted_count\n";
    
    if ($non_deleted_count == 0) {
        echo "✅ SUCCESS: All messages are deleted - chat should not appear in list\n";
    } else {
        echo "❌ PROBLEM: There are still $non_deleted_count non-deleted messages\n";
    }
    
    echo "\n=== TESTING CHAT LIST QUERY ===\n";
    
    // Test the actual chat list query
    $stmt = $db->prepare("
        SELECT 
            other_user_id,
            first_name,
            last_name,
            last_message,
            last_message_time,
            unread_count
        FROM (
            SELECT 
                CASE 
                    WHEN m.sender_id = ? THEN m.receiver_id
                    ELSE m.sender_id
                END as other_user_id,
                r.f_name as first_name,
                r.l_name as last_name,
                m.msg_text as last_message,
                m.msg_timestamp as last_message_time,
                m.msg_status as last_message_status,
                m.sender_id as last_sender_id,
                (SELECT COUNT(*) FROM messages WHERE receiver_id = ? AND sender_id = other_user_id AND msg_status != 'Read' AND msg_status != 'Deleted') as unread_count
            FROM messages m
            JOIN users u ON (
                CASE 
                    WHEN m.sender_id = ? THEN m.receiver_id
                    ELSE m.sender_id
                END = u.user_id
            )
            JOIN registration r ON u.reg_id = r.reg_id
            WHERE (m.sender_id = ? OR m.receiver_id = ?) 
            AND m.msg_status != 'Deleted'
            ORDER BY m.msg_timestamp DESC
        ) as chat_messages
        GROUP BY other_user_id
        ORDER BY last_message_time DESC
    ");
    
    $stmt->execute([$user_id, $user_id, $user_id, $user_id, $user_id]);
    $chat_list = $stmt->fetchAll();
    
    echo "Chat list shows " . count($chat_list) . " chats:\n";
    
    $found_user_6 = false;
    foreach ($chat_list as $chat) {
        if ($chat['other_user_id'] == $chat_id) {
            $found_user_6 = true;
            echo "  ❌ FOUND USER 6: {$chat['last_message']} | Unread: {$chat['unread_count']}\n";
        } else {
            echo "  User {$chat['other_user_id']}: {$chat['last_message']} | Unread: {$chat['unread_count']}\n";
        }
    }
    
    if ($found_user_6) {
        echo "\n❌ PROBLEM: User 6 chat is still showing in chat list\n";
    } else {
        echo "\n✅ SUCCESS: User 6 chat is not showing in chat list\n";
    }
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
?>




















