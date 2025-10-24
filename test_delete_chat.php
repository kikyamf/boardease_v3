<?php
// Test delete chat functionality
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $user_id = 1;
    $chat_id = 2; // User 2
    $chat_type = 'individual';
    
    $db = getDB();
    
    echo "=== BEFORE DELETE ===\n";
    
    // Check messages before delete
    $stmt = $db->prepare("
        SELECT 
            message_id,
            sender_id,
            receiver_id,
            msg_text,
            msg_status,
            msg_timestamp
        FROM messages 
        WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)
        ORDER BY msg_timestamp DESC
    ");
    $stmt->execute([$user_id, $chat_id, $chat_id, $user_id]);
    $messages_before = $stmt->fetchAll();
    
    echo "Messages before delete: " . count($messages_before) . "\n";
    foreach ($messages_before as $msg) {
        echo "  Message {$msg['message_id']}: {$msg['msg_status']} - {$msg['msg_text']}\n";
    }
    
    echo "\n=== DELETING CHAT ===\n";
    
    // Delete the chat
    $stmt = $db->prepare("
        UPDATE messages 
        SET msg_status = 'Deleted' 
        WHERE (sender_id = ? AND receiver_id = ?) 
           OR (sender_id = ? AND receiver_id = ?)
    ");
    $stmt->execute([$user_id, $chat_id, $chat_id, $user_id]);
    $deleted_count = $stmt->rowCount();
    
    echo "Deleted $deleted_count messages\n";
    
    echo "\n=== AFTER DELETE ===\n";
    
    // Check messages after delete
    $stmt->execute([$user_id, $chat_id, $chat_id, $user_id]);
    $messages_after = $stmt->fetchAll();
    
    echo "Messages after delete: " . count($messages_after) . "\n";
    foreach ($messages_after as $msg) {
        echo "  Message {$msg['message_id']}: {$msg['msg_status']} - {$msg['msg_text']}\n";
    }
    
    echo "\n=== TESTING CHAT LIST QUERY ===\n";
    
    // Test the chat list query
    $stmt = $db->prepare("
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
            m.sender_id as last_sender_id
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
        ORDER BY last_message_time DESC
    ");
    
    $stmt->execute([$user_id, $user_id, $user_id, $user_id, $user_id]);
    $chat_list = $stmt->fetchAll();
    
    echo "Chat list shows: " . count($chat_list) . " chats\n";
    foreach ($chat_list as $chat) {
        echo "  Chat with user {$chat['other_user_id']}: {$chat['last_message']} ({$chat['last_message_status']})\n";
    }
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
?>




















