<?php
// Simple test for duplicate fix using LEFT JOIN approach
require_once 'dbConfig.php';

$user_id = 29;

echo "=== TESTING LEFT JOIN DUPLICATE FIX ===\n";

try {
    $db = getDB();
    
    // Test the LEFT JOIN approach
    $stmt = $db->prepare("
        SELECT DISTINCT
            CASE 
                WHEN m.sender_id = ? THEN m.receiver_id
                ELSE m.sender_id
            END as other_user_id,
            r.first_name,
            r.last_name,
            m.msg_text as last_message,
            m.msg_timestamp as last_message_time
        FROM messages m
        JOIN users u ON (
            CASE 
                WHEN m.sender_id = ? THEN m.receiver_id
                ELSE m.sender_id
            END = u.user_id
        )
        JOIN registrations r ON u.reg_id = r.id
        LEFT JOIN messages m2 ON (
            (m2.sender_id = ? AND m2.receiver_id = m.receiver_id) OR 
            (m2.receiver_id = ? AND m2.sender_id = m.sender_id)
        ) AND m2.msg_timestamp > m.msg_timestamp
        WHERE (m.sender_id = ? OR m.receiver_id = ?)
        AND m2.msg_id IS NULL
        ORDER BY m.msg_timestamp DESC
    ");
    
    $stmt->execute([$user_id, $user_id, $user_id, $user_id, $user_id, $user_id]);
    $results = $stmt->fetchAll();
    
    echo "Found " . count($results) . " conversations:\n";
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
    echo "❌ Error: " . $e->getMessage() . "\n";
}
?>




