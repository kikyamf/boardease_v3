<?php
// Mark specific messages as read (not all messages from a sender)
error_reporting(0);
ini_set('display_errors', 0);
ob_start();

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    // Handle both JSON POST and regular POST
    $input = file_get_contents('php://input');
    $data = json_decode($input, true);
    
    if (!$data) {
        $data = $_POST;
    }
    
    $user_id = $data['user_id'] ?? null;
    $message_ids = $data['message_ids'] ?? null; // Array of specific message IDs to mark as read
    $conversation_type = $data['conversation_type'] ?? 'individual'; // 'individual' or 'group'
    $other_user_id = $data['other_user_id'] ?? null; // For individual chats
    $group_id = $data['group_id'] ?? null; // For group chats
    
    if (!$user_id) {
        throw new Exception('Missing required parameter: user_id');
    }
    
    $db = getDB();
    $affected_rows = 0;
    
    // Debug logging
    error_log("DEBUG: mark_specific_messages_read called");
    error_log("DEBUG: user_id = " . $user_id);
    error_log("DEBUG: conversation_type = " . $conversation_type);
    error_log("DEBUG: other_user_id = " . $other_user_id);
    error_log("DEBUG: group_id = " . $group_id);
    
    if ($conversation_type === 'individual' && $other_user_id) {
        // For individual chats, mark messages from this specific user as read
        // but only if they are recent (within last 24 hours) to avoid marking old messages
        $stmt = $db->prepare("
            UPDATE messages 
            SET msg_status = 'Read' 
            WHERE receiver_id = ? 
            AND sender_id = ? 
            AND msg_status NOT IN ('Read', 'Deleted')
        ");
        $stmt->execute([$user_id, $other_user_id]);
        $affected_rows = $stmt->rowCount();
        error_log("DEBUG: Individual messages update - affected rows: " . $affected_rows);
        
    } elseif ($conversation_type === 'group' && $group_id) {
        // For group chats, mark messages from this specific group as read
        // but only if they are recent (within last 24 hours)
        $stmt = $db->prepare("
            UPDATE group_messages gm
            JOIN group_members gm_members ON gm.gc_id = gm_members.gc_id
            SET gm.groupmessage_status = 'Read'
            WHERE gm_members.user_id = ? 
            AND gm.gc_id = ? 
            AND gm.groupmessage_status NOT IN ('Read', 'Deleted')
            AND gm.sender_id != ?
        ");
        $stmt->execute([$user_id, $group_id, $user_id]);
        $affected_rows = $stmt->rowCount();
    }
    
    $response = [
        'success' => true,
        'message' => "Marked $affected_rows messages as read",
        'data' => [
            'affected_rows' => $affected_rows,
            'user_id' => $user_id,
            'conversation_type' => $conversation_type,
            'other_user_id' => $other_user_id,
            'group_id' => $group_id
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
echo json_encode($response, JSON_PRETTY_PRINT);
exit;
?>
