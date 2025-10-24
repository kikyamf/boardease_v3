<?php
// Check and insert active_boarders data
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

echo "Checking and inserting active_boarders data...\n\n";

try {
    $db = getDB();
    
    // First, check if active_boarders table exists
    $stmt = $db->prepare("SHOW TABLES LIKE 'active_boarders'");
    $stmt->execute();
    $table_exists = $stmt->fetch();
    
    if (!$table_exists) {
        echo "❌ active_boarders table does not exist. Creating it...\n";
        $stmt = $db->prepare("
            CREATE TABLE active_boarders (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL,
                boarding_house_id INT NOT NULL,
                room_id INT NULL,
                status ENUM('Active', 'Inactive', 'Moved') DEFAULT 'Active',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                FOREIGN KEY (boarding_house_id) REFERENCES boarding_houses(bh_id) ON DELETE CASCADE
            )
        ");
        $stmt->execute();
        echo "✅ Created active_boarders table\n";
    } else {
        echo "✅ active_boarders table exists\n";
    }
    
    // Check current data
    $stmt = $db->prepare("SELECT * FROM active_boarders ORDER BY user_id");
    $stmt->execute();
    $current_data = $stmt->fetchAll();
    
    echo "\n=== Current active_boarders data ===\n";
    if (empty($current_data)) {
        echo "No data found\n";
    } else {
        foreach ($current_data as $row) {
            echo "- ID: " . $row['id'] . ", User ID: " . $row['user_id'] . ", BH ID: " . $row['boarding_house_id'] . ", Room ID: " . ($row['room_id'] ?? 'NULL') . ", Status: " . $row['status'] . "\n";
        }
    }
    
    // Check if user 28 exists
    $stmt = $db->prepare("SELECT u.user_id, r.first_name, r.last_name, r.role FROM users u JOIN registrations r ON u.reg_id = r.id WHERE u.user_id = ?");
    $stmt->execute([28]);
    $user = $stmt->fetch();
    
    if (!$user) {
        echo "\n❌ User 28 does not exist. Creating user...\n";
        // Create registration
        $stmt = $db->prepare("
            INSERT INTO registrations (role, first_name, last_name, email, password, status, created_at) 
            VALUES ('Boarder', 'Test', 'User28', 'user28@test.com', 'password123', 'approved', NOW())
        ");
        $stmt->execute();
        $reg_id = $db->lastInsertId();
        echo "✅ Created registration with ID: " . $reg_id . "\n";
        
        // Create user
        $stmt = $db->prepare("
            INSERT INTO users (reg_id, status, created_at) 
            VALUES (?, 'Active', NOW())
        ");
        $stmt->execute([$reg_id]);
        $user_id = $db->lastInsertId();
        echo "✅ Created user with ID: " . $user_id . "\n";
    } else {
        echo "\n✅ User 28 exists: " . $user['first_name'] . " " . $user['last_name'] . " (" . $user['role'] . ")\n";
    }
    
    // Check if boarding house 85 exists
    $stmt = $db->prepare("SELECT bh_id, bh_name, user_id FROM boarding_houses WHERE bh_id = ?");
    $stmt->execute([85]);
    $bh = $stmt->fetch();
    
    if (!$bh) {
        echo "\n❌ Boarding house 85 does not exist. Creating it...\n";
        $stmt = $db->prepare("
            INSERT INTO boarding_houses (bh_id, user_id, bh_name, bh_address, bh_description, bh_rules, number_of_bathroom, area, build_year, status, bh_created_at) 
            VALUES (85, 1, 'Test Boarding House 85', '123 Test St', 'Test description', 'Test rules', 2, 100, 2024, 'active', NOW())
        ");
        $stmt->execute();
        echo "✅ Created boarding house with ID: 85\n";
    } else {
        echo "\n✅ Boarding house 85 exists: " . $bh['bh_name'] . " (Owner: " . $bh['user_id'] . ")\n";
    }
    
    // Clear any existing entry for user 28
    $stmt = $db->prepare("DELETE FROM active_boarders WHERE user_id = ?");
    $stmt->execute([28]);
    echo "\n✅ Cleared existing entries for user 28\n";
    
    // Insert new entry
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
        echo "\n❌ Verification failed - no data found\n";
    }
    
    // Show all active_boarders data
    $stmt = $db->prepare("SELECT * FROM active_boarders ORDER BY user_id");
    $stmt->execute();
    $all_data = $stmt->fetchAll();
    
    echo "\n=== All active_boarders data ===\n";
    if (empty($all_data)) {
        echo "No data found\n";
    } else {
        foreach ($all_data as $row) {
            echo "- User ID: " . $row['user_id'] . ", BH ID: " . $row['boarding_house_id'] . ", Room ID: " . ($row['room_id'] ?? 'NULL') . ", Status: " . $row['status'] . "\n";
        }
    }
    
} catch (Exception $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
    echo "Stack trace: " . $e->getTraceAsString() . "\n";
}
?>




