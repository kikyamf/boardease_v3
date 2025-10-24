<?php
// Get chat list with real-time timestamps and proper sorting
error_reporting(0);
ini_set('display_errors', 0);
ob_start();

// Ensure clean output
if (ob_get_level()) {
    ob_clean();
}

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $user_id = $_GET['user_id'] ?? null;
    
    if (!$user_id) {
        throw new Exception('Missing required parameter: user_id');
    }
    
    $db = getDB();
    
    // Get individual chats with proper sorting by latest message
    $stmt = $db->prepare("
        SELECT 
            other_user_id,
            first_name,
            last_name,
            user_type,
            user_status,
            last_message,
            last_message_time,
            last_message_status,
            last_sender_id,
            unread_count
        FROM (
            SELECT 
                CASE 
                    WHEN m.sender_id = ? THEN m.receiver_id
                    ELSE m.sender_id
                END as other_user_id,
                r.f_name as first_name,
                r.l_name as last_name,
                r.role as user_type,
                u.status as user_status,
                m.msg_text as last_message,
                m.msg_timestamp as last_message_time,
                m.msg_status as last_message_status,
                m.sender_id as last_sender_id,
                (SELECT COUNT(*) FROM messages WHERE receiver_id = ? AND sender_id = other_user_id AND msg_status NOT IN ('Read', 'Deleted')) as unread_count
            FROM messages m
            JOIN users u ON (
                CASE 
                    WHEN m.sender_id = ? THEN m.receiver_id
                    ELSE m.sender_id
                END = u.user_id
            )
            JOIN registrations r ON u.reg_id = r.reg_id
            WHERE (m.sender_id = ? OR m.receiver_id = ?) 
            AND m.msg_status != 'Deleted'
            ORDER BY m.msg_timestamp DESC
        ) as chat_messages
        GROUP BY other_user_id
        ORDER BY last_message_time DESC
    ");
    
    $stmt->execute([$user_id, $user_id, $user_id, $user_id, $user_id]);
    $individual_chats = $stmt->fetchAll();
    
    // Get group chats
    $stmt = $db->prepare("
        SELECT 
            cg.gc_id as group_id,
            cg.gc_name as group_name,
            cg.gc_created_by,
            MAX(gm.groupmessage_timestamp) as last_message_time,
            (SELECT groupmessage_text FROM group_messages WHERE gc_id = cg.gc_id ORDER BY groupmessage_timestamp DESC LIMIT 1) as last_message,
            (SELECT sender_id FROM group_messages WHERE gc_id = cg.gc_id ORDER BY groupmessage_timestamp DESC LIMIT 1) as last_sender_id,
            (SELECT r.f_name FROM group_messages gm_inner JOIN users u_inner ON gm_inner.sender_id = u_inner.user_id JOIN registrations r ON u_inner.reg_id = r.reg_id WHERE gm_inner.gc_id = cg.gc_id ORDER BY gm_inner.groupmessage_timestamp DESC LIMIT 1) as last_sender_first_name,
            (SELECT r.l_name FROM group_messages gm_inner JOIN users u_inner ON gm_inner.sender_id = u_inner.user_id JOIN registrations r ON u_inner.reg_id = r.reg_id WHERE gm_inner.gc_id = cg.gc_id ORDER BY gm_inner.groupmessage_timestamp DESC LIMIT 1) as last_sender_last_name,
            COUNT(CASE WHEN gm_unread.groupmessage_status NOT IN ('Read', 'Deleted') AND gm_unread.sender_id != ? THEN 1 END) as unread_count
        FROM chat_groups cg
        JOIN group_members gm_members ON cg.gc_id = gm_members.gc_id
        LEFT JOIN group_messages gm ON cg.gc_id = gm.gc_id
        LEFT JOIN group_messages gm_unread ON cg.gc_id = gm_unread.gc_id
        WHERE gm_members.user_id = ?
        GROUP BY cg.gc_id, cg.gc_name, cg.gc_created_by
        ORDER BY last_message_time DESC
    ");
    
    $stmt->execute([$user_id, $user_id]);
    $group_chats = $stmt->fetchAll();
    
    // Format individual chats with real-time timestamps
    $formatted_individual_chats = [];
    foreach ($individual_chats as $chat) {
        // Format time to 12-hour format with real-time calculation
        $message_time = strtotime($chat['last_message_time']);
        $current_time = time();
        $time_diff = $current_time - $message_time;
        
        // Format based on how recent the message is
        if ($time_diff < 60) { // Less than 1 minute
            $formatted_time = "Just now";
        } elseif ($time_diff < 3600) { // Less than 1 hour
            $minutes = floor($time_diff / 60);
            $formatted_time = $minutes . "m ago";
        } elseif ($time_diff < 86400) { // Less than 24 hours
            $formatted_time = date('g:i A', $message_time);
        } else { // More than 24 hours
            $formatted_time = date('M j', $message_time);
        }
        
        // Format last message with sender info
        $last_message = $chat['last_message'];
        if ($chat['last_sender_id'] == $user_id) {
            $last_message = "You: " . $last_message;
        }
        
        // Truncate long messages
        if (strlen($last_message) > 50) {
            $last_message = substr($last_message, 0, 47) . "...";
        }
        
        $formatted_individual_chats[] = [
            'chat_id' => 'individual_' . $chat['other_user_id'],
            'chat_type' => 'individual',
            'other_user_id' => $chat['other_user_id'],
            'other_user_name' => $chat['first_name'] . ' ' . $chat['last_name'],
            'other_user_type' => $chat['user_type'],
            'user_status' => $chat['user_status'],
            'last_message' => $last_message,
            'last_message_time' => $formatted_time,
            'last_message_timestamp' => $chat['last_message_time'], // Keep original timestamp
            'last_message_status' => $chat['last_message_status'],
            'unread_count' => (int)$chat['unread_count']
        ];
    }
    
    // Format group chats with real-time timestamps
    $formatted_group_chats = [];
    foreach ($group_chats as $chat) {
        $last_sender_name = '';
        if ($chat['last_sender_first_name'] && $chat['last_sender_last_name']) {
            $last_sender_name = $chat['last_sender_first_name'] . ' ' . $chat['last_sender_last_name'];
        }
        
        // Format time with real-time calculation
        $message_time = strtotime($chat['last_message_time']);
        $current_time = time();
        $time_diff = $current_time - $message_time;
        
        if ($time_diff < 60) {
            $formatted_time = "Just now";
        } elseif ($time_diff < 3600) {
            $minutes = floor($time_diff / 60);
            $formatted_time = $minutes . "m ago";
        } elseif ($time_diff < 86400) {
            $formatted_time = date('g:i A', $message_time);
        } else {
            $formatted_time = date('M j', $message_time);
        }
        
        // Format last message with sender info
        $last_message = $chat['last_message'];
        if ($last_message && $last_sender_name) {
            if ($chat['last_sender_id'] == $user_id) {
                $last_message = "You: " . $last_message;
            } else {
                $last_message = $last_sender_name . ": " . $last_message;
            }
        }
        
        // Truncate long messages
        if (strlen($last_message) > 50) {
            $last_message = substr($last_message, 0, 47) . "...";
        }
        
        $formatted_group_chats[] = [
            'chat_id' => 'group_' . $chat['group_id'],
            'chat_type' => 'group',
            'group_id' => $chat['group_id'],
            'group_name' => $chat['group_name'],
            'created_by' => $chat['gc_created_by'],
            'last_message' => $last_message,
            'last_message_time' => $formatted_time,
            'last_message_timestamp' => $chat['last_message_time'], // Keep original timestamp
            'last_sender_name' => $last_sender_name,
            'last_sender_id' => $chat['last_sender_id'],
            'unread_count' => (int)$chat['unread_count']
        ];
    }
    
    // Combine all chats and sort by actual timestamp (not formatted time)
    $all_chats = array_merge($formatted_individual_chats, $formatted_group_chats);
    usort($all_chats, function($a, $b) {
        $timeA = strtotime($a['last_message_timestamp']);
        $timeB = strtotime($b['last_message_timestamp']);
        return $timeB - $timeA; // Most recent first
    });
    
    $response = [
        'success' => true,
        'data' => [
            'chats' => $all_chats,
            'individual_chats' => $formatted_individual_chats,
            'group_chats' => $formatted_group_chats,
            'total_count' => count($all_chats),
            'debug_info' => [
                'user_id' => $user_id,
                'individual_count' => count($formatted_individual_chats),
                'group_count' => count($formatted_group_chats),
                'total_chats' => count($all_chats),
                'current_time' => date('Y-m-d H:i:s'),
                'server_timezone' => date_default_timezone_get()
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

ob_clean();
echo json_encode($response);
exit;
?>








