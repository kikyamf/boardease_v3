<?php
// Check the actual message statuses in the database
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

echo "=== CHECKING MESSAGE STATUSES ===\n\n";

try {
    $db = getDB();
    
    // Check all messages and their statuses
    echo "1. ALL MESSAGES AND THEIR STATUSES:\n";
    $stmt = $db->prepare("
        SELECT 
            m.message_id,
            m.sender_id,
            m.receiver_id,
            m.msg_text,
            m.msg_status,
            m.msg_timestamp,
            r1.first_name as sender_name,
            r2.first_name as receiver_name
        FROM messages m
        JOIN users u1 ON m.sender_id = u1.user_id
        JOIN registrations r1 ON u1.reg_id = r1.id
        JOIN users u2 ON m.receiver_id = u2.user_id
        JOIN registrations r2 ON u2.reg_id = r2.id
        ORDER BY m.msg_timestamp DESC
        LIMIT 20
    ");
    $stmt->execute();
    $messages = $stmt->fetchAll();
    
    foreach ($messages as $msg) {
        echo "ID: {$msg['message_id']} | {$msg['sender_name']} → {$msg['receiver_name']} | Status: {$msg['msg_status']} | Text: '{$msg['msg_text']}' | Time: {$msg['msg_timestamp']}\n";
    }
    
    echo "\n2. MESSAGE STATUS COUNTS:\n";
    $stmt = $db->prepare("
        SELECT 
            msg_status,
            COUNT(*) as count
        FROM messages
        GROUP BY msg_status
    ");
    $stmt->execute();
    $status_counts = $stmt->fetchAll();
    
    foreach ($status_counts as $status) {
        echo "Status '{$status['msg_status']}': {$status['count']} messages\n";
    }
    
    echo "\n3. UNREAD MESSAGES FOR USER 29:\n";
    $stmt = $db->prepare("
        SELECT 
            m.message_id,
            m.sender_id,
            m.receiver_id,
            m.msg_text,
            m.msg_status,
            m.msg_timestamp,
            r.first_name as sender_name
        FROM messages m
        JOIN users u ON m.sender_id = u.user_id
        JOIN registrations r ON u.reg_id = r.id
        WHERE m.receiver_id = 29 
        AND m.msg_status NOT IN ('Read', 'Deleted')
        ORDER BY m.msg_timestamp DESC
    ");
    $stmt->execute();
    $unread_messages = $stmt->fetchAll();
    
    echo "Unread messages for user 29:\n";
    foreach ($unread_messages as $msg) {
        echo "ID: {$msg['message_id']} | From: {$msg['sender_name']} | Status: {$msg['msg_status']} | Text: '{$msg['msg_text']}'\n";
    }
    
    echo "\n4. UNREAD MESSAGES FOR USER 28:\n";
    $stmt = $db->prepare("
        SELECT 
            m.message_id,
            m.sender_id,
            m.receiver_id,
            m.msg_text,
            m.msg_status,
            m.msg_timestamp,
            r.first_name as sender_name
        FROM messages m
        JOIN users u ON m.sender_id = u.user_id
        JOIN registrations r ON u.reg_id = r.id
        WHERE m.receiver_id = 28 
        AND m.msg_status NOT IN ('Read', 'Deleted')
        ORDER BY m.msg_timestamp DESC
    ");
    $stmt->execute();
    $unread_messages_28 = $stmt->fetchAll();
    
    echo "Unread messages for user 28:\n";
    foreach ($unread_messages_28 as $msg) {
        echo "ID: {$msg['message_id']} | From: {$msg['sender_name']} | Status: {$msg['msg_status']} | Text: '{$msg['msg_text']}'\n";
    }
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}

echo "\n=== CHECK COMPLETE ===\n";
?>




