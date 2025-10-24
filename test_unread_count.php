<?php
// Simple test for unread count
error_reporting(0);
ini_set('display_errors', 0);

require_once 'db_helper.php';

try {
    $user_id = 1;
    
    $db = getDB();
    
    // Get unread count
    $stmt = $db->prepare("
        SELECT COUNT(*) as unread_count 
        FROM messages 
        WHERE receiver_id = ? AND msg_status != 'Read' AND msg_status != 'Deleted'
    ");
    $stmt->execute([$user_id]);
    $unread_count = $stmt->fetch()['unread_count'];
    
    echo "Unread count for user $user_id: $unread_count\n";
    
    // Get total messages
    $stmt = $db->prepare("
        SELECT COUNT(*) as total_count 
        FROM messages 
        WHERE receiver_id = ? OR sender_id = ?
    ");
    $stmt->execute([$user_id, $user_id]);
    $total_count = $stmt->fetch()['total_count'];
    
    echo "Total messages for user $user_id: $total_count\n";
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
?>




















