<?php
// Check active_boarders table data
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

echo "Checking active_boarders table...\n\n";

try {
    $db = getDB();
    
    // Check active_boarders table
    $stmt = $db->prepare("SELECT * FROM active_boarders ORDER BY user_id");
    $stmt->execute();
    $active_boarders = $stmt->fetchAll();
    
    echo "=== Active Boarders Table ===\n";
    if (empty($active_boarders)) {
        echo "❌ No data in active_boarders table\n";
    } else {
        echo "Found " . count($active_boarders) . " records:\n";
        foreach ($active_boarders as $ab) {
            echo "- User ID: " . $ab['user_id'] . ", Boarding House ID: " . $ab['boarding_house_id'] . ", Status: " . $ab['status'] . "\n";
        }
    }
    
    echo "\n=== Boarding Houses Table ===\n";
    $stmt = $db->prepare("SELECT * FROM boarding_houses ORDER BY bh_id");
    $stmt->execute();
    $boarding_houses = $stmt->fetchAll();
    
    if (empty($boarding_houses)) {
        echo "❌ No data in boarding_houses table\n";
    } else {
        echo "Found " . count($boarding_houses) . " boarding houses:\n";
        foreach ($boarding_houses as $bh) {
            echo "- BH ID: " . $bh['bh_id'] . ", Name: " . $bh['bh_name'] . ", Owner: " . $bh['user_id'] . "\n";
        }
    }
    
    echo "\n=== Users Table ===\n";
    $stmt = $db->prepare("SELECT u.user_id, r.first_name, r.last_name, r.role FROM users u JOIN registrations r ON u.reg_id = r.id ORDER BY u.user_id");
    $stmt->execute();
    $users = $stmt->fetchAll();
    
    if (empty($users)) {
        echo "❌ No data in users table\n";
    } else {
        echo "Found " . count($users) . " users:\n";
        foreach ($users as $user) {
            echo "- User ID: " . $user['user_id'] . ", Name: " . $user['first_name'] . " " . $user['last_name'] . ", Role: " . $user['role'] . "\n";
        }
    }
    
} catch (Exception $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}
?>




