<?php
// Get unread message count for a user
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
    
    // Get current user's role first
    $stmt = $db->prepare("
        SELECT
            r.role as user_role,
            r.first_name,
            r.last_name
        FROM users u
        JOIN registrations r ON u.reg_id = r.id
        WHERE u.user_id = ?
    ");
    $stmt->execute([$user_id]);
    $current_user = $stmt->fetch();
    
    if (!$current_user) {
        throw new Exception('Current user not found');
    }
    
    // Get unread individual messages count based on role and boarding house relationships
    if ($current_user['user_role'] === 'BH Owner') {
        // OWNER SIDE: Count unread messages from boarders in their boarding houses only
        $stmt = $db->prepare("
            SELECT COUNT(*) as unread_count 
            FROM messages m
            JOIN users u ON m.sender_id = u.user_id
            JOIN registrations r ON u.reg_id = r.id
            JOIN active_boarders ab ON u.user_id = ab.user_id
            WHERE m.receiver_id = ? 
            AND m.msg_status NOT IN ('Read', 'Deleted')
            AND ab.boarding_house_id IN (
                SELECT bh_id FROM boarding_houses WHERE user_id = ?
            )
            AND ab.user_id != ?
            AND ab.status = 'Active'
            AND r.role = 'Boarder'
            AND r.status = 'approved'
        ");
        $stmt->execute([$user_id, $user_id, $user_id]);
        $individual_unread = $stmt->fetch()['unread_count'];
        
    } else if ($current_user['user_role'] === 'Boarder') {
        // BOARDER SIDE: Count unread messages from owner and other boarders from same boarding house
        
        // First, find which boarding house this boarder is staying in
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
            $stmt->execute([$user_id, $boarder_bh['owner_id'], $boarder_bh['boarding_house_id'], $user_id]);
            $individual_unread = $stmt->fetch()['unread_count'];
        } else {
            // Boarder has no active boarding house - no unread messages
            $individual_unread = 0;
        }
    } else {
        // Unknown role - no unread messages
        $individual_unread = 0;
    }
    
    // Get unread group messages count
    $stmt = $db->prepare("
        SELECT COUNT(*) as unread_count 
        FROM group_messages gm
        JOIN group_members gm_members ON gm.gc_id = gm_members.gc_id
        WHERE gm_members.user_id = ? AND gm.groupmessage_status NOT IN ('Read', 'Deleted') AND gm.sender_id != ?
    ");
    $stmt->execute([$user_id, $user_id]);
    $group_unread = $stmt->fetch()['unread_count'];
    
    $total_unread = $individual_unread + $group_unread;
    
    $response = [
        'success' => true,
        'data' => [
            'user_id' => $user_id,
            'individual_unread' => (int)$individual_unread,
            'group_unread' => (int)$group_unread,
            'total_unread' => (int)$total_unread,
            'timestamp' => date('Y-m-d H:i:s')
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














