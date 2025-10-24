<?php
// Check current messages state
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

try {
    $db = getDB();
    
    echo "=== CHECKING ALL MESSAGES ===\n";
    
    // Get all messages
    $stmt = $db->prepare("
        SELECT 
            message_id,
            sender_id,
            receiver_id,
            msg_status,
            msg_text,
            msg_timestamp
        FROM messages 
        ORDER BY message_id DESC
        LIMIT 20
    ");
    $stmt->execute();
    $messages = $stmt->fetchAll();
    
    echo "Found " . count($messages) . " recent messages:\n";
    foreach ($messages as $msg) {
        echo "  ID {$msg['message_id']}: {$msg['sender_id']} -> {$msg['receiver_id']} | Status: '{$msg['msg_status']}' | Text: " . substr($msg['msg_text'], 0, 30) . "...\n";
    }
    
    echo "\n=== CHECKING MESSAGES BETWEEN USER 1 AND 2 ===\n";
    
    // Get messages between user 1 and 2
    $stmt = $db->prepare("
        SELECT 
            message_id,
            sender_id,
            receiver_id,
            msg_status,
            msg_text
        FROM messages 
        WHERE (sender_id = 1 AND receiver_id = 2) OR (sender_id = 2 AND receiver_id = 1)
        ORDER BY message_id DESC
    ");
    $stmt->execute();
    $messages_1_2 = $stmt->fetchAll();
    
    echo "Found " . count($messages_1_2) . " messages between user 1 and 2:\n";
    foreach ($messages_1_2 as $msg) {
        echo "  ID {$msg['message_id']}: {$msg['sender_id']} -> {$msg['receiver_id']} | Status: '{$msg['msg_status']}' | Text: " . substr($msg['msg_text'], 0, 30) . "...\n";
    }
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
?>




















