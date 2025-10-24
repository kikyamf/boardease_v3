<?php
// Send group message with FCM notification (optimized for speed)
error_reporting(0);
ini_set('display_errors', 0);
ob_start();

require_once 'db_helper.php';
require_once 'fcm_config.php';

header('Content-Type: application/json');

try {
    // Handle both JSON and regular POST data
    $sender_id = null;
    $group_id = null;
    $message_text = null;
    
    // Check if JSON data is sent
    $input = file_get_contents('php://input');
    if (!empty($input)) {
        $data = json_decode($input, true);
        if ($data) {
            $sender_id = $data['sender_id'] ?? null;
            $group_id = $data['group_id'] ?? null;
            $message_text = $data['message'] ?? null;
        }
    }
    
    // Fallback to regular POST data
    if (!$sender_id || !$group_id || !$message_text) {
        $sender_id = $_POST['sender_id'] ?? null;
        $group_id = $_POST['group_id'] ?? null;
        $message_text = $_POST['message'] ?? null;
    }
    
    // Validate input
    if (!$sender_id || !$group_id || !$message_text) {
        throw new Exception('Missing required parameters: sender_id, group_id, message');
    }
    
    $db = getDB();
    
    // Get group information
    $stmt = $db->prepare("
        SELECT gc_name, gc_created_by 
        FROM chat_groups 
        WHERE gc_id = ?
    ");
    $stmt->execute([$group_id]);
    $group = $stmt->fetch();
    
    if (!$group) {
        throw new Exception('Group not found');
    }
    
    // Check for duplicate group message in last 5 seconds
    $stmt = $db->prepare("
        SELECT groupmessage_id FROM group_messages 
        WHERE gc_id = ? AND sender_id = ? AND groupmessage_text = ? 
        AND groupmessage_timestamp > DATE_SUB(NOW(), INTERVAL 5 SECOND)
        LIMIT 1
    ");
    $stmt->execute([$group_id, $sender_id, $message_text]);
    $duplicate = $stmt->fetch();
    
    if ($duplicate) {
        // Return existing message ID instead of creating duplicate
        $message_id = $duplicate['groupmessage_id'];
        error_log("Duplicate group message prevented, returning existing ID: $message_id");
    } else {
        // Insert group message
        $stmt = $db->prepare("
            INSERT INTO group_messages (gc_id, sender_id, groupmessage_text, groupmessage_timestamp, groupmessage_status) 
            VALUES (?, ?, ?, NOW(), 'Sent')
        ");
        $stmt->execute([$group_id, $sender_id, $message_text]);
        $message_id = $db->lastInsertId();
    }
    
    // Get sender name
    $stmt = $db->prepare("
        SELECT r.f_name, r.l_name 
        FROM users u 
        JOIN registration r ON u.reg_id = r.reg_id 
        WHERE u.user_id = ?
    ");
    $stmt->execute([$sender_id]);
    $sender = $stmt->fetch();
    $sender_name = $sender ? $sender['f_name'] . ' ' . $sender['l_name'] : 'Unknown User';
    
    // Get group members for notifications (simplified query)
    $stmt = $db->prepare("
        SELECT u.user_id, dt.device_token
        FROM group_members gm
        JOIN users u ON gm.user_id = u.user_id
        LEFT JOIN device_tokens dt ON u.user_id = dt.user_id AND dt.is_active = 1
        WHERE gm.gc_id = ? AND u.status = 'Active'
    ");
    $stmt->execute([$group_id]);
    $members = $stmt->fetchAll();
    
    // Send notifications quickly (don't wait for all responses)
    $notification_count = 0;
    foreach ($members as $member) {
        if ($member['user_id'] != $sender_id && $member['device_token']) {
            try {
                // Send notification without waiting for detailed response
                FCMConfig::sendToDevice(
                    $member['device_token'],
                    $group['gc_name'],
                    $sender_name . ': ' . $message_text,
                    [
                        'type' => 'new_message',
                        'sender_id' => (string)$sender_id,
                        'sender_name' => $sender_name,
                        'receiver_id' => (string)$member['user_id'],
                        'group_id' => (string)$group_id,
                        'group_name' => $group['gc_name'],
                        'message_id' => (string)$message_id,
                        'message_text' => $message_text,
                        'chat_type' => 'group',
                        'timestamp' => date('Y-m-d H:i:s')
                    ]
                );
                $notification_count++;
            } catch (Exception $e) {
                // Continue even if one notification fails
                error_log("FCM notification failed: " . $e->getMessage());
            }
        }
    }
    
    // Quick response
    $response = [
        'success' => true,
        'message' => 'Group message sent successfully',
        'data' => [
            'message_id' => $message_id,
            'group_id' => $group_id,
            'group_name' => $group['gc_name'],
            'sender_id' => $sender_id,
            'sender_name' => $sender_name,
            'message' => $message_text,
            'timestamp' => date('Y-m-d H:i:s'),
            'notifications_sent' => $notification_count,
            'total_members' => count($members) - 1
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












