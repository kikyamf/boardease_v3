<?php
require_once 'db_helper.php';

try {
    $db = getDB();
    $current_user_id = 1;
    
    echo "Testing SQL query for owner side...\n";
    
    // Test the exact query from get_users_for_messaging.php
    $stmt = $db->prepare("
        SELECT 
            u.user_id,
            CONCAT(r.first_name, ' ', r.last_name) as full_name,
            r.role as user_type,
            r.email,
            r.phone,
            r.status,
            bh.bh_name as boarding_house_name,
            bh.bh_address as boarding_house_address,
            bh.bh_id as boarding_house_id,
            CASE 
                WHEN dt.device_token IS NOT NULL AND dt.is_active = 1 THEN 1 
                ELSE 0 
            END as has_device_token,
            CASE 
                WHEN dt.device_token IS NOT NULL AND dt.is_active = 1 THEN 'Online' 
                ELSE 'Offline' 
            END as status_text
        FROM active_boarders ab
        JOIN users u ON ab.user_id = u.user_id
        JOIN registrations r ON u.reg_id = r.id
        JOIN boarding_houses bh ON ab.boarding_house_id = bh.bh_id
        LEFT JOIN device_tokens dt ON u.user_id = dt.user_id AND dt.is_active = 1
        WHERE ab.boarding_house_id IN (
            SELECT bh_id FROM boarding_houses WHERE user_id = ?
        )
        AND ab.user_id != ? 
        AND ab.status = 'active'
        AND r.role = 'Boarder' 
        AND r.status = 'approved'
        ORDER BY r.first_name ASC
    ");
    
    $stmt->execute([$current_user_id, $current_user_id]);
    $users = $stmt->fetchAll();
    
    echo "Query result count: " . count($users) . "\n";
    if (count($users) > 0) {
        echo "Sample result:\n";
        print_r($users[0]);
    } else {
        echo "No results found - this is expected since active_boarders table is empty\n";
    }
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
?>




