<?php
// Test delete endpoint directly
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $user_id = 1;
    $chat_id = 2;
    $chat_type = 'individual';
    
    echo "=== TESTING DELETE ENDPOINT ===\n";
    echo "User ID: $user_id\n";
    echo "Chat ID: $chat_id\n";
    echo "Chat Type: $chat_type\n\n";
    
    // Simulate the delete_chat.php logic
    $db = getDB();
    
    if ($chat_type === 'individual') {
        $other_user_id = $chat_id;
        
        echo "=== BEFORE DELETE ===\n";
        
        // Check messages before delete
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
            LIMIT 5
        ");
        $stmt->execute([$user_id, $other_user_id, $other_user_id, $user_id]);
        $messages_before = $stmt->fetchAll();
        
        echo "Found " . count($messages_before) . " messages before delete:\n";
        foreach ($messages_before as $msg) {
            echo "  ID {$msg['message_id']}: {$msg['sender_id']} -> {$msg['receiver_id']} | Status: '{$msg['msg_status']}'\n";
        }
        
        echo "\n=== DELETING MESSAGES ===\n";
        
        // Delete the messages
        $stmt = $db->prepare("
            UPDATE messages 
            SET msg_status = 'Deleted' 
            WHERE ((sender_id = ? AND receiver_id = ?) 
               OR (sender_id = ? AND receiver_id = ?))
            AND msg_status != 'Deleted'
        ");
        
        $result = $stmt->execute([$user_id, $other_user_id, $other_user_id, $user_id]);
        $deleted_count = $stmt->rowCount();
        
        echo "Delete result: " . ($result ? "SUCCESS" : "FAILED") . "\n";
        echo "Deleted count: $deleted_count\n";
        
        if (!$result) {
            echo "Error: " . $stmt->error . "\n";
        }
        
        echo "\n=== AFTER DELETE ===\n";
        
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
            LIMIT 5
        ");
        $stmt->execute([$user_id, $other_user_id, $other_user_id, $user_id]);
        $messages_after = $stmt->fetchAll();
        
        echo "Found " . count($messages_after) . " messages after delete:\n";
        foreach ($messages_after as $msg) {
            echo "  ID {$msg['message_id']}: {$msg['sender_id']} -> {$msg['receiver_id']} | Status: '{$msg['msg_status']}'\n";
        }
        
        echo "\n=== TESTING CHAT LIST ===\n";
        
        // Test if chat list would show this chat
        $stmt = $db->prepare("
            SELECT COUNT(*) as chat_count
            FROM (
                SELECT DISTINCT
                    CASE 
                        WHEN m.sender_id = ? THEN m.receiver_id
                        ELSE m.sender_id
                    END as other_user_id
                FROM messages m
                WHERE (m.sender_id = ? OR m.receiver_id = ?) 
                AND m.msg_status != 'Deleted'
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
        ");
        
        $stmt->execute([$user_id, $user_id, $user_id, $user_id, $user_id, $user_id]);
        $chat_count = $stmt->fetch()['chat_count'];
        
        echo "Chat list would show $chat_count chats with user $other_user_id\n";
        
        if ($chat_count == 0) {
            echo "✅ SUCCESS: Chat should be hidden from chat list\n";
        } else {
            echo "❌ PROBLEM: Chat is still showing in chat list\n";
        }
        
    }
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
?>




















