<?php
try {
    $db = new PDO('mysql:host=localhost;dbname=boardease2', 'root', '');
    $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    echo "=== CHECKING ROOM PRICE COLUMN ===\n\n";
    
    // Check boarding_house_rooms table structure
    echo "📋 boarding_house_rooms table structure:\n";
    $stmt = $db->prepare("DESCRIBE boarding_house_rooms");
    $stmt->execute();
    $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);
    foreach ($columns as $column) {
        echo "   - {$column['Field']} ({$column['Type']})\n";
    }
    
    echo "\n📋 Sample data:\n";
    $stmt = $db->prepare("SELECT * FROM boarding_house_rooms LIMIT 3");
    $stmt->execute();
    $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
    foreach ($rows as $row) {
        echo "   - ID: {$row['bhr_id']}, Category: {$row['room_category']}\n";
        foreach ($row as $key => $value) {
            if (strpos($key, 'price') !== false || strpos($key, 'Price') !== false) {
                echo "     Price field: $key = $value\n";
            }
        }
    }
    
} catch (Exception $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}
?>














