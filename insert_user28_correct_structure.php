<?php
// Insert user 28 with correct active_boarders table structure
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "Inserting user 28 with correct table structure...\n\n";

try {
    require_once 'db_helper.php';
    $db = getDB();
    
    // Check current table structure
    $stmt = $db->prepare("DESCRIBE active_boarders");
    $stmt->execute();
    $columns = $stmt->fetchAll();
    
    echo "=== active_boarders table structure ===\n";
    foreach ($columns as $col) {
        echo "- " . $col['Field'] . " (" . $col['Type'] . ")\n";
    }
    
    // Check current data
    $stmt = $db->prepare("SELECT * FROM active_boarders ORDER BY active_id");
    $stmt->execute();
    $current_data = $stmt->fetchAll();
    
    echo "\n=== Current active_boarders data ===\n";
    if (empty($current_data)) {
        echo "No data found\n";
    } else {
        foreach ($current_data as $row) {
            echo "- Active ID: " . $row['active_id'] . ", User ID: " . $row['user_id'] . ", BH ID: " . $row['boarding_house_id'] . ", Room ID: " . ($row['room_id'] ?? 'NULL') . ", Status: " . $row['status'] . "\n";
        }
    }
    
    // Clear any existing entry for user 28
    $stmt = $db->prepare("DELETE FROM active_boarders WHERE user_id = ?");
    $stmt->execute([28]);
    echo "\n✅ Cleared existing entries for user 28\n";
    
    // Insert new entry with correct structure
    $stmt = $db->prepare("
        INSERT INTO active_boarders (user_id, status, room_id, boarding_house_id) 
        VALUES (?, 'Active', ?, ?)
    ");
    $result = $stmt->execute([28, 81, 85]);
    
    if ($result) {
        echo "✅ Successfully inserted user 28 as boarder in room 81 at boarding house 85\n";
    } else {
        echo "❌ Failed to insert user 28\n";
    }
    
    // Verify the insertion
    $stmt = $db->prepare("SELECT * FROM active_boarders WHERE user_id = 28");
    $stmt->execute();
    $result = $stmt->fetch();
    
    if ($result) {
        echo "\n✅ Verification successful:\n";
        echo "- Active ID: " . $result['active_id'] . "\n";
        echo "- User ID: " . $result['user_id'] . "\n";
        echo "- Boarding House ID: " . $result['boarding_house_id'] . "\n";
        echo "- Room ID: " . $result['room_id'] . "\n";
        echo "- Status: " . $result['status'] . "\n";
    } else {
        echo "\n❌ Verification failed - no data found\n";
    }
    
    // Show all active_boarders data
    $stmt = $db->prepare("SELECT * FROM active_boarders ORDER BY active_id");
    $stmt->execute();
    $all_data = $stmt->fetchAll();
    
    echo "\n=== All active_boarders data ===\n";
    if (empty($all_data)) {
        echo "No data found\n";
    } else {
        foreach ($all_data as $row) {
            echo "- Active ID: " . $row['active_id'] . ", User ID: " . $row['user_id'] . ", BH ID: " . $row['boarding_house_id'] . ", Room ID: " . ($row['room_id'] ?? 'NULL') . ", Status: " . $row['status'] . "\n";
        }
    }
    
} catch (Exception $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
    echo "Stack trace: " . $e->getTraceAsString() . "\n";
}
?>




