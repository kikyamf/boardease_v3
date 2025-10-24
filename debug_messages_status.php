<?php
// Debug messages status
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $user_id = $_GET['user_id'] ?? 1;
    
    $db = getDB();
    
    // Get all messages for this user
    $stmt = $db->prepare("
        SELECT 
            message_id,
            sender_id,
            receiver_id,
            msg_text,
            msg_status,
            msg_timestamp
        FROM messages 
        WHERE sender_id = ? OR receiver_id = ?
        ORDER BY msg_timestamp DESC
        LIMIT 20
    ");
    $stmt->execute([$user_id, $user_id]);
    $messages = $stmt->fetchAll();
    
    // Get unread count
    $stmt = $db->prepare("
        SELECT COUNT(*) as unread_count 
        FROM messages 
        WHERE receiver_id = ? AND msg_status != 'Read' AND msg_status != 'Deleted'
    ");
    $stmt->execute([$user_id]);
    $unread_count = $stmt->fetch()['unread_count'];
    
    $response = [
        'success' => true,
        'data' => [
            'user_id' => $user_id,
            'unread_count' => $unread_count,
            'recent_messages' => $messages
        ]
    ];
    
} catch (Exception $e) {
    $response = [
        'success' => false,
        'message' => 'Error: ' . $e->getMessage(),
        'data' => null
    ];
}

echo json_encode($response, JSON_PRETTY_PRINT);
exit;
?>




















