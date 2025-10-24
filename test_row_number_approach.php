<?php
// Test ROW_NUMBER() approach for get_chat_list.php
require_once 'dbConfig.php';

$user_id = 29;

echo "=== TESTING ROW_NUMBER() APPROACH ===\n";

try {
    $db = getDB();
    
    // Test the owner side query with ROW_NUMBER()
    echo "Testing owner side query with ROW_NUMBER()...\n";
    $stmt = $db->prepare("
        SELECT 
            other_user_id,
            first_name,
            last_name,
            last_message,
            last_message_time
        FROM (
            SELECT 
                CASE 
                    WHEN m.sender_id = ? THEN m.receiver_id
                    ELSE m.sender_id
                END as other_user_id,
                r.first_name,
                r.last_name,
                m.msg_text as last_message,
                m.msg_timestamp as last_message_time,
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
    $results = $stmt->fetchAll();
    
    echo "✅ ROW_NUMBER() query executed successfully!\n";
    echo "Found " . count($results) . " conversations\n";
    
    foreach ($results as $result) {
        echo "- User ID: {$result['other_user_id']} | Name: {$result['first_name']} {$result['last_name']} | Message: {$result['last_message']} | Time: {$result['last_message_time']}\n";
    }
    
    // Check for duplicates
    $user_ids = array_column($results, 'other_user_id');
    $unique_user_ids = array_unique($user_ids);
    
    if (count($user_ids) === count($unique_user_ids)) {
        echo "\n✅ NO DUPLICATES FOUND\n";
    } else {
        echo "\n❌ DUPLICATES FOUND\n";
        $duplicates = array_diff_assoc($user_ids, $unique_user_ids);
        foreach ($duplicates as $duplicate) {
            echo "- User ID $duplicate appears multiple times\n";
        }
    }
    
} catch (Exception $e) {
    echo "❌ SQL Error: " . $e->getMessage() . "\n";
}
?>




