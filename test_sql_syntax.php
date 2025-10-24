<?php
// Test SQL syntax for get_chat_list.php
require_once 'dbConfig.php';

$user_id = 29;

echo "=== TESTING SQL SYNTAX ===\n";

try {
    $db = getDB();
    
    // Test the owner side query
    echo "Testing owner side query...\n";
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
        JOIN active_boarders ab ON u.user_id = ab.user_id
        LEFT JOIN messages m2 ON (
            (m2.sender_id = ? AND m2.receiver_id = m.receiver_id) OR 
            (m2.receiver_id = ? AND m2.sender_id = m.sender_id)
        ) AND m2.msg_timestamp > m.msg_timestamp
        WHERE (m.sender_id = ? OR m.receiver_id = ?)
        AND ab.boarding_house_id IN (
            SELECT bh_id FROM boarding_houses WHERE user_id = ?
        )
        AND ab.user_id != ?
        AND ab.status = 'Active'
        AND r.role = 'Boarder'
        AND r.status = 'approved'
        AND m2.msg_id IS NULL
        ORDER BY m.msg_timestamp DESC
    ");
    
    $stmt->execute([$user_id, $user_id, $user_id, $user_id, $user_id, $user_id, $user_id]);
    $results = $stmt->fetchAll();
    
    echo "✅ SQL syntax is correct!\n";
    echo "Found " . count($results) . " conversations\n";
    
    foreach ($results as $result) {
        echo "- User ID: {$result['other_user_id']} | Name: {$result['first_name']} {$result['last_name']} | Message: {$result['last_message']}\n";
    }
    
} catch (Exception $e) {
    echo "❌ SQL Error: " . $e->getMessage() . "\n";
}
?>




