<?php
// Simple test for delete logic
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

try {
    $user_id = 1;
    $chat_id = 2;
    
    $db = getDB();
    
    echo "=== TESTING DELETE LOGIC ===\n";
    
    // First, let's see what messages exist
    $stmt = $db->prepare("
        SELECT 
            message_id,
            sender_id,
            receiver_id,
            msg_status,
            msg_text
        FROM messages 
        WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)
        ORDER BY message_id
    ");
    $stmt->execute([$user_id, $chat_id, $chat_id, $user_id]);
    $messages = $stmt->fetchAll();
    
    echo "Found " . count($messages) . " messages:\n";
    foreach ($messages as $msg) {
        echo "  ID {$msg['message_id']}: {$msg['sender_id']} -> {$msg['receiver_id']} | Status: '{$msg['msg_status']}' | Text: {$msg['msg_text']}\n";
    }
    
    echo "\n=== TESTING DELETE QUERY ===\n";
    
    // Test the delete query
    $stmt = $db->prepare("
        UPDATE messages 
        SET msg_status = 'Deleted' 
        WHERE (sender_id = ? AND receiver_id = ?) 
           OR (sender_id = ? AND receiver_id = ?)
    ");
    
    $result = $stmt->execute([$user_id, $chat_id, $chat_id, $user_id]);
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
        WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)
        ORDER BY message_id
    ");
    $stmt->execute([$user_id, $chat_id, $chat_id, $user_id]);
    $messages_after = $stmt->fetchAll();
    
    echo "Found " . count($messages_after) . " messages after delete:\n";
    foreach ($messages_after as $msg) {
        echo "  ID {$msg['message_id']}: {$msg['sender_id']} -> {$msg['receiver_id']} | Status: '{$msg['msg_status']}' | Text: {$msg['msg_text']}\n";
    }
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
?>




















