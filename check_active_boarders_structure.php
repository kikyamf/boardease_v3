<?php
// Check active_boarders table structure
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

echo "Checking active_boarders table structure...\n\n";

try {
    $db = getDB();
    
    // Check table structure
    $stmt = $db->prepare("DESCRIBE active_boarders");
    $stmt->execute();
    $columns = $stmt->fetchAll();
    
    echo "=== active_boarders table structure ===\n";
    foreach ($columns as $column) {
        echo "- " . $column['Field'] . " (" . $column['Type'] . ")\n";
    }
    
    // Check if room_id column exists
    $has_room_id = false;
    foreach ($columns as $column) {
        if ($column['Field'] === 'room_id') {
            $has_room_id = true;
            break;
        }
    }
    
    if ($has_room_id) {
        echo "\n✅ room_id column exists\n";
    } else {
        echo "\n❌ room_id column does not exist\n";
        echo "Adding room_id column...\n";
        
        // Add room_id column
        $stmt = $db->prepare("ALTER TABLE active_boarders ADD COLUMN room_id INT NULL");
        $stmt->execute();
        echo "✅ Added room_id column\n";
    }
    
    // Check current data
    $stmt = $db->prepare("SELECT * FROM active_boarders ORDER BY user_id");
    $stmt->execute();
    $data = $stmt->fetchAll();
    
    echo "\n=== Current active_boarders data ===\n";
    if (empty($data)) {
        echo "No data found\n";
    } else {
        foreach ($data as $row) {
            echo "- User ID: " . $row['user_id'] . ", BH ID: " . $row['boarding_house_id'] . ", Room ID: " . ($row['room_id'] ?? 'NULL') . ", Status: " . $row['status'] . "\n";
        }
    }
    
} catch (Exception $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}
?>




