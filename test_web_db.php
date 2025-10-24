<?php
// Test database through web server
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "Testing database through web server...\n";

try {
    require_once 'db_helper.php';
    $db = getDB();
    
    echo "Database connection successful\n";
    
    // Check if active_boarders table exists
    $stmt = $db->prepare("SHOW TABLES LIKE 'active_boarders'");
    $stmt->execute();
    $table = $stmt->fetch();
    
    if ($table) {
        echo "active_boarders table exists\n";
        
        // Check current data
        $stmt = $db->prepare("SELECT * FROM active_boarders");
        $stmt->execute();
        $data = $stmt->fetchAll();
        
        echo "Current records: " . count($data) . "\n";
        foreach ($data as $row) {
            echo "- User: " . $row['user_id'] . ", BH: " . $row['boarding_house_id'] . ", Status: " . $row['status'] . "\n";
        }
        
        // Try to insert user 28
        $stmt = $db->prepare("INSERT INTO active_boarders (user_id, boarding_house_id, room_id, status, created_at, updated_at) VALUES (28, 85, 81, 'Active', NOW(), NOW())");
        $result = $stmt->execute();
        
        if ($result) {
            echo "Successfully inserted user 28\n";
        } else {
            echo "Failed to insert user 28\n";
        }
        
    } else {
        echo "active_boarders table does not exist\n";
    }
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
?>




