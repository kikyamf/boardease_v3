<?php
// Web-accessible script to insert user 28
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "<h2>Inserting User 28 as Boarder</h2>";

try {
    require_once 'db_helper.php';
    $db = getDB();
    
    echo "<p>✅ Database connection successful</p>";
    
    // Check current data
    $stmt = $db->prepare("SELECT * FROM active_boarders ORDER BY active_id");
    $stmt->execute();
    $current_data = $stmt->fetchAll();
    
    echo "<h3>Current active_boarders data:</h3>";
    if (empty($current_data)) {
        echo "<p>No data found</p>";
    } else {
        echo "<ul>";
        foreach ($current_data as $row) {
            echo "<li>Active ID: " . $row['active_id'] . ", User ID: " . $row['user_id'] . ", BH ID: " . $row['boarding_house_id'] . ", Room ID: " . ($row['room_id'] ?? 'NULL') . ", Status: " . $row['status'] . "</li>";
        }
        echo "</ul>";
    }
    
    // Clear any existing entry for user 28
    $stmt = $db->prepare("DELETE FROM active_boarders WHERE user_id = ?");
    $stmt->execute([28]);
    echo "<p>✅ Cleared existing entries for user 28</p>";
    
    // Insert new entry
    $stmt = $db->prepare("
        INSERT INTO active_boarders (user_id, status, room_id, boarding_house_id) 
        VALUES (?, 'Active', ?, ?)
    ");
    $result = $stmt->execute([28, 81, 85]);
    
    if ($result) {
        echo "<p>✅ Successfully inserted user 28 as boarder in room 81 at boarding house 85</p>";
    } else {
        echo "<p>❌ Failed to insert user 28</p>";
    }
    
    // Verify the insertion
    $stmt = $db->prepare("SELECT * FROM active_boarders WHERE user_id = 28");
    $stmt->execute();
    $result = $stmt->fetch();
    
    if ($result) {
        echo "<h3>✅ Verification successful:</h3>";
        echo "<ul>";
        echo "<li>Active ID: " . $result['active_id'] . "</li>";
        echo "<li>User ID: " . $result['user_id'] . "</li>";
        echo "<li>Boarding House ID: " . $result['boarding_house_id'] . "</li>";
        echo "<li>Room ID: " . $result['room_id'] . "</li>";
        echo "<li>Status: " . $result['status'] . "</li>";
        echo "</ul>";
    } else {
        echo "<p>❌ Verification failed - no data found</p>";
    }
    
    // Show all data
    $stmt = $db->prepare("SELECT * FROM active_boarders ORDER BY active_id");
    $stmt->execute();
    $all_data = $stmt->fetchAll();
    
    echo "<h3>All active_boarders data:</h3>";
    if (empty($all_data)) {
        echo "<p>No data found</p>";
    } else {
        echo "<ul>";
        foreach ($all_data as $row) {
            echo "<li>Active ID: " . $row['active_id'] . ", User ID: " . $row['user_id'] . ", BH ID: " . $row['boarding_house_id'] . ", Room ID: " . ($row['room_id'] ?? 'NULL') . ", Status: " . $row['status'] . "</li>";
        }
        echo "</ul>";
    }
    
} catch (Exception $e) {
    echo "<p>❌ Error: " . $e->getMessage() . "</p>";
}
?>




