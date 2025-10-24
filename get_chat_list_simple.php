<?php
// Simplified chat list - more reliable approach
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $user_id = $_GET['user_id'] ?? null;
    
    if (!$user_id) {
        throw new Exception('Missing required parameter: user_id');
    }
    
    $db = getDB();
    
    // Simple approach: Get all messages for this user first
    $stmt = $db->prepare("
        SELECT 
            m.message_id,
            m.sender_id,
            m.receiver_id,
            m.msg_text,
            m.msg_timestamp,
            m.msg_status,
            CASE 
                WHEN m.sender_id = ? THEN m.receiver_id
                ELSE m.sender_id
            END as other_user_id
        FROM messages m
        WHERE (m.sender_id = ? OR m.receiver_id = ?) 
        AND m.msg_status != 'Deleted'
        ORDER BY m.msg_timestamp DESC
    ");
    
    $stmt->execute([$user_id, $user_id, $user_id]);
    $all_messages = $stmt->fetchAll();
    
    // Group messages by other_user_id and get the latest message for each
    $chats = [];
    $processed_users = [];
    
    foreach ($all_messages as $message) {
        $other_user_id = $message['other_user_id'];
        
        // Skip if we already processed this user
        if (in_array($other_user_id, $processed_users)) {
            continue;
        }
        
        $processed_users[] = $other_user_id;
        
        // Get user info for this other user
        $user_stmt = $db->prepare("
            SELECT 
                u.user_id,
                u.status as user_status,
                r.f_name,
                r.l_name,
                r.role as user_type,
                r.status as reg_status
            FROM users u
            JOIN registrations r ON u.reg_id = r.reg_id
            WHERE u.user_id = ?
        ");
        $user_stmt->execute([$other_user_id]);
        $user_info = $user_stmt->fetch();
        
        if (!$user_info) {
            continue; // Skip if user not found
        }
        
        // Get unread count for this conversation
        $unread_stmt = $db->prepare("
            SELECT COUNT(*) as unread_count
            FROM messages
            WHERE sender_id = ? AND receiver_id = ? AND msg_status NOT IN ('Read', 'Deleted')
        ");
        $unread_stmt->execute([$other_user_id, $user_id]);
        $unread_count = $unread_stmt->fetch()['unread_count'];
        
        // Format the last message
        $last_message = $message['msg_text'];
        if ($message['sender_id'] == $user_id) {
            $last_message = "You: " . $last_message;
        }
        
        // Truncate long messages
        if (strlen($last_message) > 50) {
            $last_message = substr($last_message, 0, 47) . "...";
        }
        
        $chats[] = [
            'chat_id' => 'individual_' . $other_user_id,
            'chat_type' => 'individual',
            'other_user_id' => $other_user_id,
            'other_user_name' => $user_info['f_name'] . ' ' . $user_info['l_name'],
            'other_user_type' => $user_info['user_type'],
            'user_status' => $user_info['user_status'],
            'reg_status' => $user_info['reg_status'],
            'last_message' => $last_message,
            'last_message_time' => date('g:i A', strtotime($message['msg_timestamp'])),
            'last_message_status' => $message['msg_status'],
            'unread_count' => (int)$unread_count
        ];
    }
    
    $response = [
        'success' => true,
        'data' => [
            'chats' => $chats,
            'total_count' => count($chats),
            'debug_info' => [
                'user_id' => $user_id,
                'total_messages_found' => count($all_messages),
                'unique_conversations' => count($chats)
            ]
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








