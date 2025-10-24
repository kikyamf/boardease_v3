<?php
// Execute SQL file to insert active_boarders data
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "Executing SQL to insert active_boarders data...\n\n";

try {
    require_once 'db_helper.php';
    $db = getDB();
    
    // Read SQL file
    $sql = file_get_contents('insert_active_boarder.sql');
    
    // Split by semicolon and execute each statement
    $statements = explode(';', $sql);
    
    foreach ($statements as $statement) {
        $statement = trim($statement);
        if (!empty($statement)) {
            echo "Executing: " . substr($statement, 0, 50) . "...\n";
            $db->exec($statement);
        }
    }
    
    echo "\n✅ SQL execution completed\n";
    
    // Verify the data
    $stmt = $db->prepare("SELECT * FROM active_boarders WHERE user_id = 28");
    $stmt->execute();
    $result = $stmt->fetch();
    
    if ($result) {
        echo "✅ User 28 found in active_boarders:\n";
        echo "- User ID: " . $result['user_id'] . "\n";
        echo "- Boarding House ID: " . $result['boarding_house_id'] . "\n";
        echo "- Room ID: " . $result['room_id'] . "\n";
        echo "- Status: " . $result['status'] . "\n";
    } else {
        echo "❌ User 28 not found in active_boarders\n";
    }
    
} catch (Exception $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}
?>




