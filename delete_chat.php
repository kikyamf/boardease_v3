<?php
// Delete a chat (individual or group)
error_reporting(0);
ini_set('display_errors', 0);
ob_start();

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $user_id = $_POST['user_id'] ?? null;
    $chat_type = $_POST['chat_type'] ?? null;
    $chat_id = $_POST['chat_id'] ?? null;
    
    // Handle JSON data
    $input = file_get_contents('php://input');
    if (!empty($input)) {
        $data = json_decode($input, true);
        if ($data) {
            $user_id = $data['user_id'] ?? null;
            $chat_type = $data['chat_type'] ?? null;
            $chat_id = $data['chat_id'] ?? null;
        }
    }
    
    if (!$user_id || !$chat_type || !$chat_id) {
        throw new Exception('Missing required parameters: user_id, chat_type, chat_id');
    }
    
    $db = getDB();
    
    if ($chat_type === 'individual') {
        // For individual chats, we need to mark messages as deleted
        $other_user_id = $chat_id;
        
        // First, let's see what messages exist before delete
        $stmt = $db->prepare("
            SELECT COUNT(*) as total_count,
                   SUM(CASE WHEN msg_status = 'Deleted' THEN 1 ELSE 0 END) as deleted_count,
                   SUM(CASE WHEN msg_status != 'Deleted' THEN 1 ELSE 0 END) as non_deleted_count
            FROM messages 
            WHERE ((sender_id = ? AND receiver_id = ?) 
               OR (sender_id = ? AND receiver_id = ?))
        ");
        $stmt->execute([$user_id, $other_user_id, $other_user_id, $user_id]);
        $before_stats = $stmt->fetch();
        
        // Mark ALL messages as deleted for this user (soft delete)
        $stmt = $db->prepare("
            UPDATE messages 
            SET msg_status = 'Deleted' 
            WHERE ((sender_id = ? AND receiver_id = ?) 
               OR (sender_id = ? AND receiver_id = ?))
        ");
        $stmt->execute([$user_id, $other_user_id, $other_user_id, $user_id]);
        $deleted_count = $stmt->rowCount();
        
        // Check what messages exist after delete
        $stmt = $db->prepare("
            SELECT COUNT(*) as total_count,
                   SUM(CASE WHEN msg_status = 'Deleted' THEN 1 ELSE 0 END) as deleted_count,
                   SUM(CASE WHEN msg_status != 'Deleted' THEN 1 ELSE 0 END) as non_deleted_count
            FROM messages 
            WHERE ((sender_id = ? AND receiver_id = ?) 
               OR (sender_id = ? AND receiver_id = ?))
        ");
        $stmt->execute([$user_id, $other_user_id, $other_user_id, $user_id]);
        $after_stats = $stmt->fetch();
        
        $response = [
            'success' => true,
            'message' => "Individual chat deleted successfully. Deleted $deleted_count messages.",
            'data' => [
                'chat_type' => 'individual',
                'chat_id' => $chat_id,
                'deleted_messages' => $deleted_count,
                'debug' => [
                    'before' => $before_stats,
                    'after' => $after_stats
                ]
            ]
        ];
        
    } elseif ($chat_type === 'group') {
        // For group chats, remove user from group
        $group_id = $chat_id;
        
        // Check if user is a member of the group
        $stmt = $db->prepare("
            SELECT gm_id FROM group_members 
            WHERE gc_id = ? AND user_id = ?
        ");
        $stmt->execute([$group_id, $user_id]);
        $membership = $stmt->fetch();
        
        if (!$membership) {
            throw new Exception('User is not a member of this group');
        }
        
        // Remove user from group
        $stmt = $db->prepare("
            DELETE FROM group_members 
            WHERE gc_id = ? AND user_id = ?
        ");
        $stmt->execute([$group_id, $user_id]);
        
        // Check if group is now empty and delete it if so
        $stmt = $db->prepare("
            SELECT COUNT(*) as member_count 
            FROM group_members 
            WHERE gc_id = ?
        ");
        $stmt->execute([$group_id]);
        $member_count = $stmt->fetch()['member_count'];
        
        if ($member_count == 0) {
            // Delete the group and all its messages
            $stmt = $db->prepare("DELETE FROM group_messages WHERE gc_id = ?");
            $stmt->execute([$group_id]);
            
            $stmt = $db->prepare("DELETE FROM chat_groups WHERE gc_id = ?");
            $stmt->execute([$group_id]);
            
            $response = [
                'success' => true,
                'message' => "Group chat deleted successfully (group was empty)",
                'data' => [
                    'chat_type' => 'group',
                    'chat_id' => $chat_id,
                    'group_deleted' => true
                ]
            ];
        } else {
            $response = [
                'success' => true,
                'message' => "Left group chat successfully",
                'data' => [
                    'chat_type' => 'group',
                    'chat_id' => $chat_id,
                    'group_deleted' => false
                ]
            ];
        }
        
    } else {
        throw new Exception('Invalid chat type');
    }
    
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
