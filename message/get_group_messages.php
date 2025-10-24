<?php
// Get group messages
error_reporting(0);
ini_set('display_errors', 0);
ob_start();

require_once '../db_helper.php';

header('Content-Type: application/json');

try {
    $group_id = $_GET['group_id'] ?? null;
    $limit = (int)($_GET['limit'] ?? 50);
    $offset = (int)($_GET['offset'] ?? 0);
    
    if (!$group_id) {
        throw new Exception('Missing required parameter: group_id');
    }
    
    $db = getDB();
    
    // Get group messages (simplified query)
    $stmt = $db->prepare("
        SELECT 
            gm.groupmessage_id as message_id,
            gm.gc_id as group_id,
            gm.sender_id,
            gm.groupmessage_text as message,
            gm.groupmessage_timestamp as timestamp,
            gm.groupmessage_status as status
        FROM group_messages gm
        WHERE gm.gc_id = ?
        ORDER BY gm.groupmessage_timestamp DESC
        LIMIT " . $limit . " OFFSET " . $offset
    );
    
    $stmt->execute([$group_id]);
    $messages = $stmt->fetchAll();
    
    // Get total count
    $stmt = $db->prepare("
        SELECT COUNT(*) as total_count
        FROM group_messages gm
        WHERE gm.gc_id = ?
    ");
    
    $stmt->execute([$group_id]);
    $total_count = $stmt->fetch()['total_count'];
    
    // Format messages for response
    $formatted_messages = [];
    foreach ($messages as $message) {
        // Get sender name
        $stmt = $db->prepare("
            SELECT r.f_name, r.l_name 
            FROM users u 
            JOIN registration r ON u.reg_id = r.reg_id 
            WHERE u.user_id = ?
        ");
        $stmt->execute([$message['sender_id']]);
        $sender = $stmt->fetch();
        $sender_name = $sender ? $sender['f_name'] . ' ' . $sender['l_name'] : 'Unknown';
        
        $formatted_messages[] = [
            'message_id' => $message['message_id'],
            'group_id' => $message['group_id'],
            'sender_id' => $message['sender_id'],
            'message' => $message['message'],
            'timestamp' => $message['timestamp'],
            'status' => $message['status'],
            'sender_name' => $sender_name
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






















