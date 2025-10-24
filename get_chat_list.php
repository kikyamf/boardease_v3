<?php
// Get chat list for a user (individual and group chats)
error_reporting(0);
ini_set('display_errors', 0);
ob_start();

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $user_id = $_GET['user_id'] ?? null;
    
    if (!$user_id) {
        throw new Exception('Missing required parameter: user_id');
    }
    
    $db = getDB();
    
    // Get current user's role for role-based filtering
    $stmt = $db->prepare("
        SELECT
            r.role as user_role
        FROM users u
        JOIN registrations r ON u.reg_id = r.id
        WHERE u.user_id = ?
    ");
    $stmt->execute([$user_id]);
    $current_user = $stmt->fetch();
    
    if (!$current_user) {
        throw new Exception('Current user not found');
    }
    
    // Get individual chats with role-based filtering
    if ($current_user['user_role'] === 'BH Owner') {
        // OWNER SIDE: Only show conversations with boarders from their own boarding houses
        $stmt = $db->prepare("
            SELECT 
                other_user_id,
                first_name,
                last_name,
                user_type,
                user_status,
                last_message,
                last_message_time,
                last_message_status
            FROM (
                SELECT 
                    CASE 
                        WHEN m.sender_id = ? THEN m.receiver_id
                        ELSE m.sender_id
                    END as other_user_id,
                    r.first_name,
                    r.last_name,
                    r.role as user_type,
                    u.status as user_status,
                    m.msg_text as last_message,
                    m.msg_timestamp as last_message_time,
                    m.msg_status as last_message_status,
                    ROW_NUMBER() OVER (PARTITION BY CASE WHEN m.sender_id = ? THEN m.receiver_id ELSE m.sender_id END ORDER BY m.msg_timestamp DESC) as rn
                FROM messages m
                JOIN users u ON (
                    CASE 
                        WHEN m.sender_id = ? THEN m.receiver_id
                        ELSE m.sender_id
                    END = u.user_id
                )
                JOIN registrations r ON u.reg_id = r.id
                JOIN active_boarders ab ON u.user_id = ab.user_id
                WHERE (m.sender_id = ? OR m.receiver_id = ?)
                AND ab.boarding_house_id IN (
                    SELECT bh_id FROM boarding_houses WHERE user_id = ?
                )
                AND ab.user_id != ?
                AND ab.status = 'Active'
                AND r.role = 'Boarder'
                AND r.status = 'approved'
            ) ranked_messages
            WHERE rn = 1
            ORDER BY last_message_time DESC
        ");
        $stmt->execute([$user_id, $user_id, $user_id, $user_id, $user_id, $user_id, $user_id]);
        $individual_chats = $stmt->fetchAll();
        
    } else if ($current_user['user_role'] === 'Boarder') {
        // BOARDER SIDE: Only show conversations with owner and other boarders from same boarding house
        $stmt = $db->prepare("
            SELECT
                ab.boarding_house_id,
                bh.user_id as owner_id
            FROM active_boarders ab
            JOIN boarding_houses bh ON ab.boarding_house_id = bh.bh_id
            WHERE ab.user_id = ? AND ab.status = 'Active'
        ");
        $stmt->execute([$user_id]);
        $boarder_bh = $stmt->fetch();
        
        if ($boarder_bh) {
            $stmt = $db->prepare("
                SELECT 
                    other_user_id,
                    first_name,
                    last_name,
                    user_type,
                    user_status,
                    last_message,
                    last_message_time,
                    last_message_status
                FROM (
                    SELECT 
                        CASE 
                            WHEN m.sender_id = ? THEN m.receiver_id
                            ELSE m.sender_id
                        END as other_user_id,
                        r.first_name,
                        r.last_name,
                        r.role as user_type,
                        u.status as user_status,
                        m.msg_text as last_message,
                        m.msg_timestamp as last_message_time,
                        m.msg_status as last_message_status,
                        ROW_NUMBER() OVER (PARTITION BY CASE WHEN m.sender_id = ? THEN m.receiver_id ELSE m.sender_id END ORDER BY m.msg_timestamp DESC) as rn
                    FROM messages m
                    JOIN users u ON (
                        CASE 
                            WHEN m.sender_id = ? THEN m.receiver_id
                            ELSE m.sender_id
                        END = u.user_id
                    )
                    JOIN registrations r ON u.reg_id = r.id
                    WHERE (m.sender_id = ? OR m.receiver_id = ?)
                    AND (
                        u.user_id = ? OR  -- Owner
                        (u.user_id IN (
                            SELECT ab2.user_id 
                            FROM active_boarders ab2 
                            WHERE ab2.boarding_house_id = ? 
                            AND ab2.user_id != ? 
                            AND ab2.status = 'Active'
                        ) AND r.role = 'Boarder' AND r.status = 'approved')
                    )
                ) ranked_messages
                WHERE rn = 1
                ORDER BY last_message_time DESC
            ");
            $stmt->execute([$user_id, $user_id, $user_id, $user_id, $user_id, $boarder_bh['owner_id'], $boarder_bh['boarding_house_id'], $user_id]);
            $individual_chats = $stmt->fetchAll();
        } else {
            // Boarder has no active boarding house - no individual chats
            $individual_chats = [];
        }
    } else {
        // Unknown role - no individual chats
        $individual_chats = [];
    }
    
    // Get unread count for individual chats with role-based filtering
    $formatted_individual_chats = [];
    foreach ($individual_chats as $chat) {
        $unread_count = 0;
        
        if ($current_user['user_role'] === 'BH Owner') {
            // OWNER SIDE: Count unread messages from boarders in their boarding houses only
            $stmt = $db->prepare("
                SELECT COUNT(*) as unread_count 
                FROM messages m
                JOIN users u ON m.sender_id = u.user_id
                JOIN registrations r ON u.reg_id = r.id
                JOIN active_boarders ab ON u.user_id = ab.user_id
                WHERE m.receiver_id = ? 
                AND m.sender_id = ?
                AND m.msg_status NOT IN ('Read', 'Deleted')
                AND ab.boarding_house_id IN (
                    SELECT bh_id FROM boarding_houses WHERE user_id = ?
                )
                AND ab.user_id != ?
                AND ab.status = 'Active'
                AND r.role = 'Boarder'
                AND r.status = 'approved'
            ");
            $stmt->execute([$user_id, $chat['other_user_id'], $user_id, $user_id]);
            $unread_result = $stmt->fetch();
            $unread_count = $unread_result['unread_count'];
            
        } else if ($current_user['user_role'] === 'Boarder') {
            // BOARDER SIDE: Count unread messages from owner and other boarders from same boarding house
            $stmt = $db->prepare("
                SELECT
                    ab.boarding_house_id,
                    bh.user_id as owner_id
                FROM active_boarders ab
                JOIN boarding_houses bh ON ab.boarding_house_id = bh.bh_id
                WHERE ab.user_id = ? AND ab.status = 'Active'
            ");
            $stmt->execute([$user_id]);
            $boarder_bh = $stmt->fetch();
            
            if ($boarder_bh) {
                $stmt = $db->prepare("
                    SELECT COUNT(*) as unread_count 
                    FROM messages m
                    JOIN users u ON m.sender_id = u.user_id
                    JOIN registrations r ON u.reg_id = r.id
                    WHERE m.receiver_id = ? 
                    AND m.sender_id = ?
                    AND m.msg_status NOT IN ('Read', 'Deleted')
                    AND (
                        u.user_id = ? OR  -- Owner
                        (u.user_id IN (
                            SELECT ab2.user_id 
                            FROM active_boarders ab2 
                            WHERE ab2.boarding_house_id = ? 
                            AND ab2.user_id != ? 
                            AND ab2.status = 'Active'
                        ) AND r.role = 'Boarder' AND r.status = 'approved')
                    )
                ");
                $stmt->execute([$user_id, $chat['other_user_id'], $boarder_bh['owner_id'], $boarder_bh['boarding_house_id'], $user_id]);
                $unread_result = $stmt->fetch();
                $unread_count = $unread_result['unread_count'];
            }
        }
        
        // Check if current user sent the last message in this individual chat
        $is_current_user_sender = false;
        if ($chat['last_message']) {
            $stmt = $db->prepare("
                SELECT sender_id 
                FROM messages 
                WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)
                ORDER BY msg_timestamp DESC 
                LIMIT 1
            ");
            $stmt->execute([$user_id, $chat['other_user_id'], $chat['other_user_id'], $user_id]);
            $last_sender_result = $stmt->fetch();
            $is_current_user_sender = ($last_sender_result && $last_sender_result['sender_id'] == $user_id);
        }
        
        // Format sender name for individual chats
        // Only show "You" when current user sent the message
        // Don't show other person's name since it's obvious in 1-on-1 conversation
        $display_sender_name = '';
        if ($chat['last_message'] && $is_current_user_sender) {
            $display_sender_name = 'You';
        }
        
        $formatted_individual_chats[] = [
            'chat_id' => 'individual_' . $chat['other_user_id'],
            'chat_type' => 'individual',
            'other_user_id' => $chat['other_user_id'],
            'other_user_name' => $chat['first_name'] . ' ' . $chat['last_name'],
            'other_user_type' => $chat['user_type'],
            'user_status' => $chat['user_status'],
            'last_message' => $chat['last_message'],
            'last_message_time' => $chat['last_message_time'],
            'last_message_status' => $chat['last_message_status'],
            'last_sender_name' => $display_sender_name,
            'unread_count' => (int)$unread_count
        ];
    }
    
    // Try to get group chats (check if tables exist first)
    $formatted_group_chats = [];
    try {
        // Check if group_messages table exists
        $stmt = $db->prepare("SHOW TABLES LIKE 'group_messages'");
        $stmt->execute();
        $group_messages_exists = $stmt->fetch();
        
        if ($group_messages_exists) {
            // Get group chats (simplified query)
            $stmt = $db->prepare("
                SELECT 
                    cg.gc_id as group_id,
                    cg.gc_name as group_name,
                    cg.gc_created_by
                FROM chat_groups cg
                JOIN group_members gm_members ON cg.gc_id = gm_members.gc_id
                WHERE gm_members.user_id = ?
                ORDER BY cg.gc_id
            ");
            
            $stmt->execute([$user_id]);
            $group_chats = $stmt->fetchAll();
            
            // Format group chats
            foreach ($group_chats as $chat) {
                // Get last message for this group
                $last_message = null;
                $last_sender_name = '';
                $last_message_time = null;
                
                try {
                    $stmt = $db->prepare("
                        SELECT 
                            gm.groupmessage_text as last_message,
                            gm.groupmessage_timestamp as last_message_time,
                            r.first_name as last_sender_first_name,
                            r.last_name as last_sender_last_name
                        FROM group_messages gm
                        JOIN users u ON gm.sender_id = u.user_id
                        JOIN registrations r ON u.reg_id = r.id
                        WHERE gm.gc_id = ?
                        ORDER BY gm.groupmessage_timestamp DESC
                        LIMIT 1
                    ");
                    $stmt->execute([$chat['group_id']]);
                    $last_message = $stmt->fetch();
                    
                    if ($last_message && $last_message['last_sender_first_name'] && $last_message['last_sender_last_name']) {
                        $last_sender_name = $last_message['last_sender_first_name'] . ' ' . $last_message['last_sender_last_name'];
                        $last_message_time = $last_message['last_message_time'];
                    }
                } catch (Exception $e) {
                    // No messages in this group yet
                    error_log("No messages in group " . $chat['group_id'] . ": " . $e->getMessage());
                }
                
                // Get unread count for this group
                $unread_count = 0;
                try {
                    $stmt = $db->prepare("
                        SELECT COUNT(*) as unread_count 
                        FROM group_messages 
                        WHERE gc_id = ? AND sender_id != ? AND groupmessage_status != 'Read'
                    ");
                    $stmt->execute([$chat['group_id'], $user_id]);
                    $unread_result = $stmt->fetch();
                    $unread_count = $unread_result['unread_count'];
                } catch (Exception $e) {
                    // No unread messages
                    error_log("No unread messages in group " . $chat['group_id'] . ": " . $e->getMessage());
                }
                
                // Determine if current user sent the last message
                $is_current_user_sender = false;
                if ($last_message && $last_message['last_sender_first_name'] && $last_message['last_sender_last_name']) {
                    // Check if the last message was sent by current user
                    $stmt = $db->prepare("
                        SELECT sender_id 
                        FROM group_messages 
                        WHERE gc_id = ? 
                        ORDER BY groupmessage_timestamp DESC 
                        LIMIT 1
                    ");
                    $stmt->execute([$chat['group_id']]);
                    $last_sender_result = $stmt->fetch();
                    $is_current_user_sender = ($last_sender_result && $last_sender_result['sender_id'] == $user_id);
                }
                
                // Format sender name for display
                $display_sender_name = '';
                if ($last_message && $last_message['last_sender_first_name'] && $last_message['last_sender_last_name']) {
                    if ($is_current_user_sender) {
                        $display_sender_name = 'You';
                    } else {
                        $display_sender_name = $last_sender_name;
                    }
                }
                
                // Include all groups, even if they don't have messages yet
                $formatted_group_chats[] = [
                    'chat_id' => 'group_' . $chat['group_id'],
                    'chat_type' => 'group',
                    'group_id' => $chat['group_id'],
                    'group_name' => $chat['group_name'],
                    'created_by' => $chat['gc_created_by'],
                    'last_message' => $last_message ? $last_message['last_message'] : 'No messages yet',
                    'last_message_time' => $last_message_time ? $last_message_time : '',
                    'last_sender_name' => $display_sender_name,
                    'unread_count' => (int)$unread_count
                ];
            }
        }
    } catch (Exception $e) {
        // Group chats not available, continue with individual chats only
        error_log("Group chats not available: " . $e->getMessage());
    }
    
    // Combine all chats and sort by last message time
    $all_chats = array_merge($formatted_individual_chats, $formatted_group_chats);
    usort($all_chats, function($a, $b) {
        $time_a = $a['last_message_time'] ? strtotime($a['last_message_time']) : 0;
        $time_b = $b['last_message_time'] ? strtotime($b['last_message_time']) : 0;
        return $time_b - $time_a;
    });
    
    $response = [
        'success' => true,
        'data' => [
            'chats' => $all_chats,
            'individual_chats' => $formatted_individual_chats,
            'group_chats' => $formatted_group_chats,
            'total_count' => count($all_chats)
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