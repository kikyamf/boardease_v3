<?php
// Test delete chat directly
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $user_id = 1;
    $chat_id = 2; // User 2
    $chat_type = 'individual';
    
    $db = getDB();
    
    echo "=== TESTING DELETE CHAT DIRECTLY ===\n";
    echo "User ID: $user_id\n";
    echo "Chat ID: $chat_id\n";
    echo "Chat Type: $chat_type\n\n";
    
    // First, let's see what messages exist before delete
    $stmt = $db->prepare("
        SELECT 
            message_id,
            sender_id,
            receiver_id,
            msg_status,
            msg_text
        FROM messages 
        WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)
        ORDER BY message_id DESC
        LIMIT 10
    ");
    $stmt->execute([$user_id, $chat_id, $chat_id, $user_id]);
    $messages_before = $stmt->fetchAll();
    
    echo "=== MESSAGES BEFORE DELETE ===\n";
    echo "Found " . count($messages_before) . " messages:\n";
    foreach ($messages_before as $msg) {
        echo "  ID {$msg['message_id']}: {$msg['sender_id']} -> {$msg['receiver_id']} | Status: '{$msg['msg_status']}' | Text: " . substr($msg['msg_text'], 0, 30) . "...\n";
    }
    
    echo "\n=== DELETING CHAT ===\n";
    
    // Now try to delete the chat
    $stmt = $db->prepare("
        UPDATE messages 
        SET msg_status = 'Deleted' 
        WHERE ((sender_id = ? AND receiver_id = ?) 
           OR (sender_id = ? AND receiver_id = ?))
        AND msg_status != 'Deleted'
    ");
    
    $result = $stmt->execute([$user_id, $chat_id, $chat_id, $user_id]);
    $deleted_count = $stmt->rowCount();
    
    echo "Delete result: " . ($result ? "SUCCESS" : "FAILED") . "\n";
    echo "Deleted count: $deleted_count\n";
    
    if (!$result) {
        echo "Error: " . $stmt->error . "\n";
    }
    
    echo "\n=== MESSAGES AFTER DELETE ===\n";
    
    // Check messages after delete
    $stmt = $db->prepare("
        SELECT 
            message_id,
            sender_id,
            receiver_id,
            msg_status,
            msg_text
        FROM messages 
        WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)
        ORDER BY message_id DESC
        LIMIT 10
    ");
    $stmt->execute([$user_id, $chat_id, $chat_id, $user_id]);
    $messages_after = $stmt->fetchAll();
    
    echo "Found " . count($messages_after) . " messages after delete:\n";
    foreach ($messages_after as $msg) {
        echo "  ID {$msg['message_id']}: {$msg['sender_id']} -> {$msg['receiver_id']} | Status: '{$msg['msg_status']}' | Text: " . substr($msg['msg_text'], 0, 30) . "...\n";
    }
    
    echo "\n=== TESTING CHAT LIST QUERY ===\n";
    
    // Test if the chat list query would show this chat
    $stmt = $db->prepare("
        SELECT 
            other_user_id,
            first_name,
            last_name,
            last_message,
            last_message_time,
            last_message_status,
            unread_count
        FROM (
            SELECT DISTINCT
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
        ) as chat_summary
        WHERE other_user_id IN (
            SELECT DISTINCT
                CASE 
                    WHEN sender_id = ? THEN receiver_id
                    ELSE sender_id
                END
            FROM messages 
            WHERE (sender_id = ? OR receiver_id = ?) 
            AND msg_status != 'Deleted'
        )
        ORDER BY last_message_time DESC
    ");
    
    $stmt->execute([$user_id, $user_id, $user_id, $user_id, $user_id, $user_id, $user_id, $user_id]);
    $chat_list = $stmt->fetchAll();
    
    echo "Chat list shows " . count($chat_list) . " chats:\n";
    foreach ($chat_list as $chat) {
        echo "  Chat with user {$chat['other_user_id']}: {$chat['last_message']} | Unread: {$chat['unread_count']} | Status: {$chat['last_message_status']}\n";
    }
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
?>




















