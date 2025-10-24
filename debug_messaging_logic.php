<?php
// Debug the messaging logic
error_reporting(0);
ini_set('display_errors', 0);
ob_start();
require_once 'db_helper.php';
header('Content-Type: application/json');

try {
    $current_user_id = 1; // Test with user 1
    
    $db = getDB();
    
    // Get current user's role and boarding house info
    $stmt = $db->prepare("
        SELECT 
            r.role as user_role,
            r.first_name,
            r.last_name,
            bh.bh_id as owner_bh_id,
            bh.bh_name as owner_bh_name,
            bh.bh_address as owner_bh_address
        FROM users u
        JOIN registrations r ON u.reg_id = r.id
        LEFT JOIN boarding_houses bh ON u.user_id = bh.user_id
        WHERE u.user_id = ?
    ");
    $stmt->execute([$current_user_id]);
    $current_user = $stmt->fetch();
    
    echo "Current user info:\n";
    print_r($current_user);
    echo "\n";
    
    if ($current_user['user_role'] === 'BH Owner') {
        echo "User is BH Owner, looking for boarders from their boarding houses...\n";
        
        // Check what boarding houses this owner has
        $stmt = $db->prepare("SELECT bh_id, bh_name, bh_address FROM boarding_houses WHERE user_id = ?");
        $stmt->execute([$current_user_id]);
        $owner_bhs = $stmt->fetchAll();
        
        echo "Owner's boarding houses:\n";
        print_r($owner_bhs);
        echo "\n";
        
        // Get boarders from the owner's boarding houses
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
                bh.bh_id as boarding_house_id
            FROM users u
            JOIN registrations r ON u.reg_id = r.id
            JOIN boarding_houses bh ON u.user_id = bh.user_id
            WHERE u.user_id != ? 
            AND r.role = 'Boarder' 
            AND r.status = 'approved'
            AND bh.user_id = ?
        ");
        
        $stmt->execute([$current_user_id, $current_user_id]);
        $boarders = $stmt->fetchAll();
        
        echo "Boarders found:\n";
        print_r($boarders);
        echo "\n";
        
    } else {
        echo "User is Boarder, looking for owner and other boarders from same boarding house...\n";
    }
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}

ob_clean();
exit;
?>




