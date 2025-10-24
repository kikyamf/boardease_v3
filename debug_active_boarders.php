<?php
// Debug active_boarders table
error_reporting(0);
ini_set('display_errors', 0);
ob_start();
require_once 'db_helper.php';

try {
    $db = getDB();
    
    echo "=== Active Boarders Table Debug ===\n\n";
    
    // Check if active_boarders table exists and has data
    $stmt = $db->prepare("SELECT COUNT(*) as count FROM active_boarders");
    $stmt->execute();
    $result = $stmt->fetch();
    echo "1. Active boarders count: " . $result['count'] . "\n\n";
    
    // Show sample data from active_boarders
    echo "2. Sample active_boarders data:\n";
    $stmt = $db->prepare("SELECT * FROM active_boarders LIMIT 5");
    $stmt->execute();
    $active_boarders = $stmt->fetchAll();
    print_r($active_boarders);
    echo "\n";
    
    // Check boarding houses for user_id = 1
    echo "3. Boarding houses owned by user_id = 1:\n";
    $stmt = $db->prepare("SELECT bh_id, bh_name, user_id FROM boarding_houses WHERE user_id = 1 LIMIT 5");
    $stmt->execute();
    $owner_bhs = $stmt->fetchAll();
    print_r($owner_bhs);
    echo "\n";
    
    // Check if there are any active boarders in the owner's boarding houses
    echo "4. Active boarders in owner's boarding houses:\n";
    $stmt = $db->prepare("
        SELECT 
            ab.active_id,
            ab.user_id,
            ab.boarding_house_id,
            ab.status,
            bh.bh_name,
            u.user_id as boarder_user_id,
            r.first_name,
            r.last_name
        FROM active_boarders ab
        JOIN boarding_houses bh ON ab.boarding_house_id = bh.bh_id
        JOIN users u ON ab.user_id = u.user_id
        JOIN registrations r ON u.reg_id = r.id
        WHERE bh.user_id = 1
        LIMIT 5
    ");
    $stmt->execute();
    $boarders_in_owner_bhs = $stmt->fetchAll();
    print_r($boarders_in_owner_bhs);
    echo "\n";
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}

ob_clean();
exit;
?>




