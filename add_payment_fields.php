<?php
// Add missing fields to active_boarders table for payment system
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "=== ADDING PAYMENT SYSTEM FIELDS ===\n\n";

try {
    $db = new PDO('mysql:host=localhost;dbname=boardease2', 'root', '');
    $db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    echo "✅ Database connection successful\n\n";
} catch (Exception $e) {
    echo "❌ Database connection failed: " . $e->getMessage() . "\n";
    exit;
}

try {
    // Start transaction
    $db->beginTransaction();
    
    echo "📋 Adding fields to active_boarders table...\n";
    
    // Check if room_id field already exists
    $stmt = $db->prepare("SHOW COLUMNS FROM active_boarders LIKE 'room_id'");
    $stmt->execute();
    if ($stmt->rowCount() == 0) {
        $stmt = $db->prepare("ALTER TABLE active_boarders ADD COLUMN room_id INT(11)");
        $stmt->execute();
        echo "✅ Added room_id field\n";
    } else {
        echo "⚠️  room_id field already exists\n";
    }
    
    // Check if boarding_house_id field already exists
    $stmt = $db->prepare("SHOW COLUMNS FROM active_boarders LIKE 'boarding_house_id'");
    $stmt->execute();
    if ($stmt->rowCount() == 0) {
        $stmt = $db->prepare("ALTER TABLE active_boarders ADD COLUMN boarding_house_id INT(11)");
        $stmt->execute();
        echo "✅ Added boarding_house_id field\n";
    } else {
        echo "⚠️  boarding_house_id field already exists\n";
    }
    
    // Add foreign key constraints
    try {
        $stmt = $db->prepare("ALTER TABLE active_boarders ADD FOREIGN KEY (room_id) REFERENCES room_units(room_id)");
        $stmt->execute();
        echo "✅ Added foreign key constraint for room_id\n";
    } catch (Exception $e) {
        echo "⚠️  Foreign key for room_id already exists or error: " . $e->getMessage() . "\n";
    }
    
    try {
        $stmt = $db->prepare("ALTER TABLE active_boarders ADD FOREIGN KEY (boarding_house_id) REFERENCES boarding_houses(bh_id)");
        $stmt->execute();
        echo "✅ Added foreign key constraint for boarding_house_id\n";
    } catch (Exception $e) {
        echo "⚠️  Foreign key for boarding_house_id already exists or error: " . $e->getMessage() . "\n";
    }
    
    // Commit transaction
    $db->commit();
    
    echo "\n🎉 Database fields added successfully!\n\n";
    
    // Show updated table structure
    echo "📋 Updated active_boarders table structure:\n";
    $stmt = $db->prepare("DESCRIBE active_boarders");
    $stmt->execute();
    $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);
    foreach ($columns as $column) {
        echo "   - {$column['Field']} ({$column['Type']})\n";
    }
    
} catch (Exception $e) {
    // Rollback transaction on error
    $db->rollback();
    echo "❌ Error adding fields: " . $e->getMessage() . "\n";
}
?>














