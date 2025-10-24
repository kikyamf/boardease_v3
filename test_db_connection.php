<?php
// Test database connection
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "Testing database connection...\n";

try {
    require_once 'db_helper.php';
    $db = getDB();
    
    echo "✅ Database connection successful\n";
    
    // Test simple query
    $stmt = $db->prepare("SELECT 1 as test");
    $stmt->execute();
    $result = $stmt->fetch();
    echo "✅ Simple query works: " . $result['test'] . "\n";
    
    // Check if active_boarders table exists
    $stmt = $db->prepare("SHOW TABLES LIKE 'active_boarders'");
    $stmt->execute();
    $table = $stmt->fetch();
    
    if ($table) {
        echo "✅ active_boarders table exists\n";
        
        // Check table structure
        $stmt = $db->prepare("DESCRIBE active_boarders");
        $stmt->execute();
        $columns = $stmt->fetchAll();
        
        echo "Table structure:\n";
        foreach ($columns as $col) {
            echo "- " . $col['Field'] . " (" . $col['Type'] . ")\n";
        }
        
        // Check current data
        $stmt = $db->prepare("SELECT COUNT(*) as count FROM active_boarders");
        $stmt->execute();
        $count = $stmt->fetch();
        echo "Current records: " . $count['count'] . "\n";
        
    } else {
        echo "❌ active_boarders table does not exist\n";
    }
    
} catch (Exception $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}
?>