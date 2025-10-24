<?php
try {
    $db = new PDO('mysql:host=localhost;dbname=boardease2', 'root', '');
    $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    echo "=== CHECKING TABLES ===\n\n";
    
    // List all tables
    $stmt = $db->prepare("SHOW TABLES");
    $stmt->execute();
    $tables = $stmt->fetchAll(PDO::FETCH_COLUMN);
    
    echo "📋 Available tables:\n";
    foreach ($tables as $table) {
        echo "   - $table\n";
    }
    echo "\n";
    
    // Check if boarding_house_rooms exists
    if (in_array('boarding_house_rooms', $tables)) {
        echo "✅ boarding_house_rooms table exists\n";
        
        // Get structure
        $stmt = $db->prepare("DESCRIBE boarding_house_rooms");
        $stmt->execute();
        $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        echo "📋 Structure:\n";
        foreach ($columns as $column) {
            echo "   - {$column['Field']} ({$column['Type']})\n";
        }
    } else {
        echo "❌ boarding_house_rooms table does not exist\n";
    }
    
} catch (Exception $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}
?>














