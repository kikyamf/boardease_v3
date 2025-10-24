<?php
// Get messages for a conversation
error_reporting(0);
ini_set('display_errors', 0);
ob_start();

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $user1_id = $_GET['user1_id'] ?? null;
    $user2_id = $_GET['user2_id'] ?? null;
    $limit = (int)($_GET['limit'] ?? 50);
    $offset = (int)($_GET['offset'] ?? 0);
    
    if (!$user1_id || !$user2_id) {
        throw new Exception('Missing required parameters: user1_id, user2_id');
    }
    
    $db = getDB();
    
    // Get messages between two users (simplified query)
    $stmt = $db->prepare("
        SELECT 
            m.message_id,
            m.sender_id,
            m.receiver_id,
            m.msg_text as message,
            m.msg_timestamp as timestamp,
            m.msg_status as status
        FROM messages m
        WHERE (m.sender_id = ? AND m.receiver_id = ?) 
           OR (m.sender_id = ? AND m.receiver_id = ?)
        ORDER BY m.msg_timestamp ASC
        LIMIT " . $limit . " OFFSET " . $offset
    );
    
    $stmt->execute([$user1_id, $user2_id, $user2_id, $user1_id]);
    $messages = $stmt->fetchAll();
    
    // Get total count
    $stmt = $db->prepare("
        SELECT COUNT(*) as total_count
        FROM messages m
        WHERE (m.sender_id = ? AND m.receiver_id = ?)
           OR (m.sender_id = ? AND m.receiver_id = ?)
    ");
    
    $stmt->execute([$user1_id, $user2_id, $user2_id, $user1_id]);
    $total_count = $stmt->fetch()['total_count'];
    
    // Format messages for response
    $formatted_messages = [];
    foreach ($messages as $message) {
        // Get sender name
        $stmt = $db->prepare("
            SELECT r.first_name, r.last_name 
            FROM users u 
            JOIN registrations r ON u.reg_id = r.id 
            WHERE u.user_id = ?
        ");
        $stmt->execute([$message['sender_id']]);
        $sender = $stmt->fetch();
        $sender_name = $sender ? $sender['first_name'] . ' ' . $sender['last_name'] : 'Unknown';
        
        // Get receiver name
        $stmt->execute([$message['receiver_id']]);
        $receiver = $stmt->fetch();
        $receiver_name = $receiver ? $receiver['first_name'] . ' ' . $receiver['last_name'] : 'Unknown';
        
        // Format timestamp to include date and time (e.g., Oct 5, 1:20 PM)
        $formatted_timestamp = date('M j, g:i A', strtotime($message['timestamp']));
        
        $formatted_messages[] = [
            'message_id' => $message['message_id'],
            'sender_id' => $message['sender_id'],
            'receiver_id' => $message['receiver_id'],
            'message' => $message['message'],
            'timestamp' => $formatted_timestamp, // Use formatted timestamp for Android app
            'original_timestamp' => $message['timestamp'], // Keep original for reference
            'status' => $message['status'],
            'sender_name' => $sender_name,
            'receiver_name' => $receiver_name,
            'is_from_current_user' => $message['sender_id'] == $user1_id
        ];
    }
    
    $response = [
        'success' => true,
        'data' => [
            'messages' => $formatted_messages,
            'total_count' => $total_count,
            'limit' => $limit,
            'offset' => $offset
        ]
    ];
    
} catch (Exception $e) {
    $response = [
        'success' => false,
        'message' => 'Error: ' . $e->getMessage(),
        'data' => null
    ];
}

ob_clean();
echo json_encode($response);
exit;
?>


















