<?php
// Test user role from database
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

echo "Testing User Role from Database...\n\n";

try {
    $db = getDB();
    
    // Get user with ID 1 (assuming this is the logged-in user)
    $stmt = $db->prepare("
        SELECT 
            u.user_id,
            r.role,
            r.first_name,
            r.last_name,
            r.status
        FROM users u
        JOIN registrations r ON u.reg_id = r.id
        WHERE u.user_id = 1
    ");
    $stmt->execute();
    $user = $stmt->fetch();
    
    if ($user) {
        echo "✅ User found:\n";
        echo "User ID: " . $user['user_id'] . "\n";
        echo "Role: '" . $user['role'] . "'\n";
        echo "Name: " . $user['first_name'] . " " . $user['last_name'] . "\n";
        echo "Status: " . $user['status'] . "\n";
        
        // Test role comparison
        echo "\n=== Role Comparison Tests ===\n";
        echo "Role equals 'BH Owner': " . ($user['role'] === 'BH Owner' ? 'true' : 'false') . "\n";
        echo "Role equals 'Boarder': " . ($user['role'] === 'Boarder' ? 'true' : 'false') . "\n";
        echo "Role length: " . strlen($user['role']) . "\n";
        echo "Role bytes: " . bin2hex($user['role']) . "\n";
        
    } else {
        echo "❌ User not found\n";
    }
    
    // Also check all users
    echo "\n=== All Users ===\n";
    $stmt = $db->prepare("
        SELECT 
            u.user_id,
            r.role,
            r.first_name,
            r.last_name,
            r.status
        FROM users u
        JOIN registrations r ON u.reg_id = r.id
        ORDER BY u.user_id
    ");
    $stmt->execute();
    $users = $stmt->fetchAll();
    
    foreach ($users as $u) {
        echo "ID: " . $u['user_id'] . " - Role: '" . $u['role'] . "' - Name: " . $u['first_name'] . " " . $u['last_name'] . " - Status: " . $u['status'] . "\n";
    }
    
} catch (Exception $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}
?>




