<?php
// Check database state directly
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

try {
    $db = getDB();
    
    echo "=== CHECKING DATABASE STATE ===\n\n";
    
    // Check all messages between user 1 and 6
    $stmt = $db->prepare("
        SELECT 
            message_id,
            sender_id,
            receiver_id,
            msg_status,
            msg_text,
            msg_timestamp
        FROM messages 
        WHERE (sender_id = 1 AND receiver_id = 6) OR (sender_id = 6 AND receiver_id = 1)
        ORDER BY msg_timestamp DESC
    ");
    $stmt->execute();
    $messages = $stmt->fetchAll();
    
    echo "=== MESSAGES BETWEEN USER 1 AND 6 ===\n";
    echo "Total messages: " . count($messages) . "\n\n";
    
    $status_counts = [];
    foreach ($messages as $msg) {
        $status = $msg['msg_status'] ?: 'NULL';
        $status_counts[$status] = ($status_counts[$status] ?? 0) + 1;
        
        echo "ID {$msg['message_id']}: {$msg['sender_id']} -> {$msg['receiver_id']} | Status: '$status' | Text: " . substr($msg['msg_text'], 0, 40) . "...\n";
    }
    
    echo "\n=== STATUS SUMMARY ===\n";
    foreach ($status_counts as $status => $count) {
        echo "$status: $count messages\n";
    }
    
    echo "\n=== TESTING DELETE QUERY ===\n";
    
    // Test the delete query
    $stmt = $db->prepare("
        UPDATE messages 
        SET msg_status = 'Deleted' 
        WHERE ((sender_id = 1 AND receiver_id = 6) 
           OR (sender_id = 6 AND receiver_id = 1))
        AND msg_status != 'Deleted'
    ");
    
    $result = $stmt->execute();
    $affected = $stmt->rowCount();
    
    echo "Delete query result: " . ($result ? "SUCCESS" : "FAILED") . "\n";
    echo "Affected rows: $affected\n";
    
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
        WHERE (sender_id = 1 AND receiver_id = 6) OR (sender_id = 6 AND receiver_id = 1)
        ORDER BY msg_timestamp DESC
    ");
    $stmt->execute();
    $messages_after = $stmt->fetchAll();
    
    echo "Total messages after delete: " . count($messages_after) . "\n";
    
    $status_counts_after = [];
    foreach ($messages_after as $msg) {
        $status = $msg['msg_status'] ?: 'NULL';
        $status_counts_after[$status] = ($status_counts_after[$status] ?? 0) + 1;
    }
    
    echo "\n=== STATUS SUMMARY AFTER DELETE ===\n";
    foreach ($status_counts_after as $status => $count) {
        echo "$status: $count messages\n";
    }
    
    echo "\n=== TESTING CHAT LIST QUERY ===\n";
    
    // Test the chat list query
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
                    WHEN m.sender_id = 1 THEN m.receiver_id
                    ELSE m.sender_id
                END as other_user_id,
                r.f_name as first_name,
                r.l_name as last_name,
                m.msg_text as last_message,
                m.msg_timestamp as last_message_time,
                m.msg_status as last_message_status,
                m.sender_id as last_sender_id,
                (SELECT COUNT(*) FROM messages WHERE receiver_id = 1 AND sender_id = other_user_id AND msg_status != 'Read' AND msg_status != 'Deleted') as unread_count
            FROM messages m
            JOIN users u ON (
                CASE 
                    WHEN m.sender_id = 1 THEN m.receiver_id
                    ELSE m.sender_id
                END = u.user_id
            )
            JOIN registration r ON u.reg_id = r.reg_id
            WHERE (m.sender_id = 1 OR m.receiver_id = 1) 
            AND m.msg_status != 'Deleted'
            ORDER BY m.msg_timestamp DESC
        ) as chat_messages
        GROUP BY other_user_id
        ORDER BY last_message_time DESC
    ");
    
    $stmt->execute();
    $chat_list = $stmt->fetchAll();
    
    echo "Chat list shows " . count($chat_list) . " chats:\n";
    
    $found_user_6 = false;
    foreach ($chat_list as $chat) {
        if ($chat['other_user_id'] == 6) {
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




















