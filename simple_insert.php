<?php
// Simple insert for active_boarders
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "Simple insert for active_boarders...\n";

try {
    require_once 'db_helper.php';
    $db = getDB();
    
    // Try to insert directly
    $sql = "INSERT INTO active_boarders (user_id, boarding_house_id, room_id, status, created_at, updated_at) VALUES (28, 85, 81, 'Active', NOW(), NOW())";
    
    echo "SQL: " . $sql . "\n";
    
    $result = $db->exec($sql);
    
    if ($result) {
        echo "✅ Insert successful\n";
    } else {
        echo "❌ Insert failed\n";
    }
    
    // Check if it was inserted
    $stmt = $db->prepare("SELECT * FROM active_boarders WHERE user_id = 28");
    $stmt->execute();
    $data = $stmt->fetch();
    
    if ($data) {
        echo "✅ Data found: User " . $data['user_id'] . " in BH " . $data['boarding_house_id'] . "\n";
    } else {
        echo "❌ Data not found\n";
    }
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
?>




