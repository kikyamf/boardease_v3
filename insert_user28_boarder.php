<?php
// Insert user_id = 28 as boarder in room_id = 81 at bh_id = 85
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

echo "Inserting user_id = 28 as boarder...\n\n";

try {
    $db = getDB();
    
    // Check if boarding house exists
    $stmt = $db->prepare("SELECT bh_id, bh_name, user_id FROM boarding_houses WHERE bh_id = ?");
    $stmt->execute([85]);
    $bh = $stmt->fetch();
    
    if (!$bh) {
        echo "❌ Boarding house ID 85 not found. Creating it...\n";
        // Create the boarding house
        $stmt = $db->prepare("
            INSERT INTO boarding_houses (bh_id, user_id, bh_name, bh_address, bh_description, bh_rules, number_of_bathroom, area, build_year, status, bh_created_at) 
            VALUES (85, 1, 'Test Boarding House 85', '123 Test St', 'Test description', 'Test rules', 2, 100, 2024, 'active', NOW())
        ");
        $stmt->execute();
        echo "✅ Created boarding house with ID: 85\n";
    } else {
        echo "✅ Boarding house ID 85 exists: " . $bh['bh_name'] . " (Owner: " . $bh['user_id'] . ")\n";
    }
    
    // Check if user exists
    $stmt = $db->prepare("SELECT u.user_id, r.first_name, r.last_name, r.role FROM users u JOIN registrations r ON u.reg_id = r.id WHERE u.user_id = ?");
    $stmt->execute([28]);
    $user = $stmt->fetch();
    
    if (!$user) {
        echo "❌ User ID 28 not found. Creating user...\n";
        // First create registration
        $stmt = $db->prepare("
            INSERT INTO registrations (role, first_name, last_name, email, password, status, created_at) 
            VALUES ('Boarder', 'Test', 'User28', 'user28@test.com', 'password123', 'approved', NOW())
        ");
        $stmt->execute();
        $reg_id = $db->lastInsertId();
        
        // Then create user
        $stmt = $db->prepare("
            INSERT INTO users (reg_id, status, created_at) 
            VALUES (?, 'Active', NOW())
        ");
        $stmt->execute([$reg_id]);
        $user_id = $db->lastInsertId();
        echo "✅ Created user with ID: " . $user_id . "\n";
    } else {
        echo "✅ User ID 28 exists: " . $user['first_name'] . " " . $user['last_name'] . " (" . $user['role'] . ")\n";
    }
    
    // Clear any existing active_boarders entry for this user
    $stmt = $db->prepare("DELETE FROM active_boarders WHERE user_id = ?");
    $stmt->execute([28]);
    echo "✅ Cleared existing active_boarders entry for user 28\n";
    
    // Insert the new active_boarders entry
    $stmt = $db->prepare("
        INSERT INTO active_boarders (user_id, boarding_house_id, room_id, status, created_at, updated_at) 
        VALUES (?, ?, ?, 'Active', NOW(), NOW())
    ");
    $stmt->execute([28, 85, 81]);
    echo "✅ Inserted user 28 as boarder in room 81 at boarding house 85\n";
    
    // Verify the insertion
    $stmt = $db->prepare("
        SELECT ab.*, bh.bh_name, bh.user_id as owner_id 
        FROM active_boarders ab 
        JOIN boarding_houses bh ON ab.boarding_house_id = bh.bh_id 
        WHERE ab.user_id = ?
    ");
    $stmt->execute([28]);
    $result = $stmt->fetch();
    
    if ($result) {
        echo "\n✅ Verification successful:\n";
        echo "- User ID: " . $result['user_id'] . "\n";
        echo "- Boarding House ID: " . $result['boarding_house_id'] . " (" . $result['bh_name'] . ")\n";
        echo "- Room ID: " . $result['room_id'] . "\n";
        echo "- Status: " . $result['status'] . "\n";
        echo "- Owner ID: " . $result['owner_id'] . "\n";
    } else {
        echo "❌ Verification failed - no data found\n";
    }
    
} catch (Exception $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}
?>




