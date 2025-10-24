<?php
// Mark messages as read for a user
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
    $sender_id = $data['sender_id'] ?? null; // Optional: mark messages from specific sender as read
    
    if (!$user_id) {
        throw new Exception('Missing required parameter: user_id');
    }
    
    $db = getDB();
    
    if ($sender_id) {
        // Mark messages from specific sender as read
        $stmt = $db->prepare("
            UPDATE messages 
            SET msg_status = 'Read' 
            WHERE receiver_id = ? AND sender_id = ? AND msg_status NOT IN ('Read', 'Deleted')
        ");
        $stmt->execute([$user_id, $sender_id]);
        $affected_rows = $stmt->rowCount();
        
        $message = "Marked $affected_rows messages from sender $sender_id as read";
    } else {
        // Mark all messages for the user as read
        $stmt = $db->prepare("
            UPDATE messages 
            SET msg_status = 'Read' 
            WHERE receiver_id = ? AND msg_status NOT IN ('Read', 'Deleted')
        ");
        $stmt->execute([$user_id]);
        $affected_rows = $stmt->rowCount();
        
        $message = "Marked $affected_rows individual messages as read";
    }
    
    // Also mark group messages as read if sender_id is provided
    $group_affected = 0;
    if ($sender_id) {
        // Mark group messages from specific sender as read
        $stmt = $db->prepare("
            UPDATE group_messages gm
            JOIN group_members gm_members ON gm.gc_id = gm_members.gc_id
            SET gm.groupmessage_status = 'Read'
            WHERE gm_members.user_id = ? AND gm.sender_id = ? AND gm.groupmessage_status NOT IN ('Read', 'Deleted')
        ");
        $stmt->execute([$user_id, $sender_id]);
        $group_affected = $stmt->rowCount();
    } else {
        // Mark all group messages for the user as read
        $stmt = $db->prepare("
            UPDATE group_messages gm
            JOIN group_members gm_members ON gm.gc_id = gm_members.gc_id
            SET gm.groupmessage_status = 'Read'
            WHERE gm_members.user_id = ? AND gm.groupmessage_status NOT IN ('Read', 'Deleted') AND gm.sender_id != ?
        ");
        $stmt->execute([$user_id, $user_id]);
        $group_affected = $stmt->rowCount();
    }
    
    $total_affected = $affected_rows + $group_affected;
    $message = "Marked $affected_rows individual messages and $group_affected group messages as read";
    
    $response = [
        'success' => true,
        'message' => $message,
        'data' => [
            'total_affected' => $total_affected,
            'individual_affected' => $affected_rows,
            'group_affected' => $group_affected,
            'user_id' => $user_id,
            'sender_id' => $sender_id
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














