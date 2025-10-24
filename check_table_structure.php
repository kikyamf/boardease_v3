<?php
// Check Table Structure
error_reporting(E_ALL);
ini_set('display_errors', 1);

try {
    $db = new PDO('mysql:host=localhost;dbname=boardease2', 'root', '');
    $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    echo "=== TABLE STRUCTURE CHECK ===\n\n";
    
    // Check boarding_house_rooms table
    echo "📋 boarding_house_rooms table structure:\n";
    $stmt = $db->prepare("DESCRIBE boarding_house_rooms");
    $stmt->execute();
    $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);
    foreach ($columns as $column) {
        echo "   - {$column['Field']} ({$column['Type']})\n";
    }
    echo "\n";
    
    // Check if there are any existing records
    $stmt = $db->prepare("SELECT COUNT(*) as count FROM boarding_house_rooms");
    $stmt->execute();
    $count = $stmt->fetch(PDO::FETCH_ASSOC)['count'];
    echo "📊 boarding_house_rooms records: $count\n\n";
    
    if ($count > 0) {
        echo "📋 Sample boarding_house_rooms data:\n";
        $stmt = $db->prepare("SELECT * FROM boarding_house_rooms LIMIT 3");
        $stmt->execute();
        $rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
        foreach ($rows as $row) {
            echo "   - ID: {$row['bhr_id']}, Category: {$row['room_category']}, Price: {$row['room_price']}\n";
        }
    }
    
} catch (Exception $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}
?>














