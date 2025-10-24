<?php
// Test script to verify that chat list no longer shows duplicates
require_once 'db_connection.php';

$user_id = 29; // Test with user 29 (owner)

echo "=== TESTING CHAT LIST - NO DUPLICATES ===\n";
echo "Testing with user_id: $user_id\n\n";

try {
    $db = getDB();
    
    // Get current user info
    $stmt = $db->prepare("SELECT u.user_id, r.role as user_role FROM users u JOIN registrations r ON u.reg_id = r.id WHERE u.user_id = ?");
    $stmt->execute([$user_id]);
    $current_user = $stmt->fetch();
    
    if (!$current_user) {
        echo "❌ User not found\n";
        exit;
    }
    
    echo "Current user: {$current_user['user_role']}\n\n";
    
    // Test the individual chats query
    if ($current_user['user_role'] === 'BH Owner') {
        echo "=== OWNER SIDE QUERY ===\n";
        $stmt = $db->prepare("
            SELECT 
                CASE 
                    WHEN m.sender_id = ? THEN m.receiver_id
                    ELSE m.sender_id
                END as other_user_id,
                r.first_name,
                r.last_name,
                r.role as user_type,
                u.status as user_status,
                MAX(m.msg_text) as last_message,
                MAX(m.msg_timestamp) as last_message_time,
                MAX(m.msg_status) as last_message_status
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
            GROUP BY other_user_id, r.first_name, r.last_name, r.role, u.status
            ORDER BY last_message_time DESC
        ");
        $stmt->execute([$user_id, $user_id, $user_id, $user_id, $user_id, $user_id]);
        $individual_chats = $stmt->fetchAll();
        
        echo "Found " . count($individual_chats) . " individual chats:\n";
        foreach ($individual_chats as $chat) {
            echo "- User ID: {$chat['other_user_id']} | Name: {$chat['first_name']} {$chat['last_name']} | Last Message: {$chat['last_message']} | Time: {$chat['last_message_time']}\n";
        }
        
        // Check for duplicates
        $user_ids = array_column($individual_chats, 'other_user_id');
        $unique_user_ids = array_unique($user_ids);
        
        if (count($user_ids) === count($unique_user_ids)) {
            echo "\n✅ NO DUPLICATES FOUND - Each user appears only once\n";
        } else {
            echo "\n❌ DUPLICATES FOUND:\n";
            $duplicates = array_diff_assoc($user_ids, $unique_user_ids);
            foreach ($duplicates as $duplicate) {
                echo "- User ID $duplicate appears multiple times\n";
            }
        }
    }
    
    echo "\n=== TEST COMPLETE ===\n";
    
} catch (Exception $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}
?>




