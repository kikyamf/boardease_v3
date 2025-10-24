<?php
// Check database structure and relationships
error_reporting(0);
ini_set('display_errors', 0);
ob_start();
require_once 'db_helper.php';

try {
    $db = getDB();
    
    echo "=== Database Structure Check ===\n\n";
    
    // Check users table
    echo "1. Users table:\n";
    $stmt = $db->prepare("SELECT user_id, reg_id FROM users LIMIT 5");
    $stmt->execute();
    $users = $stmt->fetchAll();
    print_r($users);
    echo "\n";
    
    // Check registrations table
    echo "2. Registrations table:\n";
    $stmt = $db->prepare("SELECT id, role, first_name, last_name FROM registrations LIMIT 5");
    $stmt->execute();
    $registrations = $stmt->fetchAll();
    print_r($registrations);
    echo "\n";
    
    // Check boarding_houses table
    echo "3. Boarding houses table:\n";
    $stmt = $db->prepare("SELECT bh_id, user_id, bh_name FROM boarding_houses LIMIT 5");
    $stmt->execute();
    $boarding_houses = $stmt->fetchAll();
    print_r($boarding_houses);
    echo "\n";
    
    // Check relationships
    echo "4. User-Role relationships:\n";
    $stmt = $db->prepare("
        SELECT 
            u.user_id,
            r.role,
            r.first_name,
            r.last_name
        FROM users u
        JOIN registrations r ON u.reg_id = r.id
        LIMIT 10
    ");
    $stmt->execute();
    $relationships = $stmt->fetchAll();
    print_r($relationships);
    echo "\n";
    
    // Check boarding house ownership
    echo "5. Boarding house ownership:\n";
    $stmt = $db->prepare("
        SELECT 
            bh.bh_id,
            bh.bh_name,
            u.user_id as owner_user_id,
            r.role as owner_role,
            r.first_name,
            r.last_name
        FROM boarding_houses bh
        JOIN users u ON bh.user_id = u.user_id
        JOIN registrations r ON u.reg_id = r.id
        LIMIT 5
    ");
    $stmt->execute();
    $ownership = $stmt->fetchAll();
    print_r($ownership);
    echo "\n";
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}

ob_clean();
exit;
?>




