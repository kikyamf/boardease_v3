<?php
// Get group messages
error_reporting(0);
ini_set('display_errors', 0);
ob_start();

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $group_id = $_GET['group_id'] ?? null;
    $current_user_id = $_GET['current_user_id'] ?? null;
    
    if (!$group_id) {
        throw new Exception('Missing required parameter: group_id');
    }
    
    $db = getDB();
    
    // Get group messages with sender information in one query
    $stmt = $db->prepare("
        SELECT 
            gm.groupmessage_id as message_id,
            gm.sender_id,
            gm.groupmessage_text as message,
            gm.groupmessage_timestamp as timestamp,
            gm.groupmessage_status as status,
            r.first_name,
            r.last_name,
            r.role as user_type
        FROM group_messages gm
        JOIN users u ON gm.sender_id = u.user_id
        JOIN registrations r ON u.reg_id = r.id
        WHERE gm.gc_id = ?
        ORDER BY gm.groupmessage_timestamp ASC
    ");
    $stmt->execute([$group_id]);
    $messages = $stmt->fetchAll();
    
    // Format messages for response
    $formatted_messages = [];
    foreach ($messages as $message) {
        $sender_name = $message['first_name'] . ' ' . $message['last_name'];
        $sender_type = $message['user_type'];
        
        // Format timestamp to include date and time (e.g., Oct 5, 1:20 PM)
        $formatted_timestamp = date('M j, g:i A', strtotime($message['timestamp']));
        
        $formatted_messages[] = [
            'message_id' => $message['message_id'],
            'sender_id' => $message['sender_id'],
            'message_text' => $message['message'],
            'timestamp' => $formatted_timestamp, // Use formatted timestamp for Android app
            'original_timestamp' => $message['timestamp'], // Keep original for reference
            'status' => $message['status'],
            'sender_name' => $sender_name,
            'sender_type' => $sender_type, // Add sender type (Owner/Boarder)
            'is_sender' => $message['sender_id'] == $current_user_id
        ];
    }
    
    $response = [
        'success' => true,
        'message' => 'Group messages retrieved successfully',
        'data' => [
            'group_id' => $group_id,
            'total_count' => count($formatted_messages),
            'messages' => $formatted_messages
        ]
    ];
    
} catch (Exception $e) {
    $response = [
        'success' => false,
        'message' => 'Error: ' . $e->getMessage(),
        'data' => null
    ];
}

// Clean output buffer and send response
ob_clean();
echo json_encode($response);
exit;
?>
























