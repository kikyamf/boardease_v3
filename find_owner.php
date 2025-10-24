<?php
require_once 'db_helper.php';

try {
    $db = getDB();
    
    // Find users with BH Owner role
    $stmt = $db->prepare("
        SELECT 
            u.user_id,
            r.role,
            r.first_name,
            r.last_name
        FROM users u
        JOIN registrations r ON u.reg_id = r.id
        WHERE r.role = 'BH Owner'
        LIMIT 5
    ");
    $stmt->execute();
    $owners = $stmt->fetchAll();
    
    echo "BH Owners found:\n";
    print_r($owners);
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
?>




