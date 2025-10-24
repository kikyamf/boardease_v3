<?php
// Simple test for chat list duplicates
require_once 'db_connection.php';

$user_id = 29;

echo "=== SIMPLE CHAT LIST TEST ===\n";

try {
    $db = getDB();
    
    // Test basic query
    $stmt = $db->prepare("
        SELECT 
            CASE 
                WHEN m.sender_id = ? THEN m.receiver_id
                ELSE m.sender_id
            END as other_user_id,
            r.first_name,
            r.last_name,
            MAX(m.msg_timestamp) as last_message_time
        FROM messages m
        JOIN users u ON (
            CASE 
                WHEN m.sender_id = ? THEN m.receiver_id
                ELSE m.sender_id
            END = u.user_id
        )
        JOIN registrations r ON u.reg_id = r.id
        WHERE (m.sender_id = ? OR m.receiver_id = ?)
        GROUP BY other_user_id, r.first_name, r.last_name
        ORDER BY last_message_time DESC
    ");
    
    $stmt->execute([$user_id, $user_id, $user_id, $user_id]);
    $results = $stmt->fetchAll();
    
    echo "Found " . count($results) . " conversations:\n";
    foreach ($results as $result) {
        echo "- User ID: {$result['other_user_id']} | Name: {$result['first_name']} {$result['last_name']} | Time: {$result['last_message_time']}\n";
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
    echo "❌ Error: " . $e->getMessage() . "\n";
}
?>




