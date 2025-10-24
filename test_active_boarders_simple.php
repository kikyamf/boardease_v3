<?php
require_once 'db_helper.php';

try {
    $db = getDB();
    
    // Simple test
    $stmt = $db->prepare("SHOW TABLES LIKE 'active_boarders'");
    $stmt->execute();
    $result = $stmt->fetch();
    
    if ($result) {
        echo "active_boarders table exists\n";
        
        $stmt = $db->prepare("SELECT COUNT(*) as count FROM active_boarders");
        $stmt->execute();
        $count = $stmt->fetch();
        echo "Records in active_boarders: " . $count['count'] . "\n";
        
        if ($count['count'] > 0) {
            $stmt = $db->prepare("SELECT * FROM active_boarders LIMIT 3");
            $stmt->execute();
            $records = $stmt->fetchAll();
            echo "Sample records:\n";
            print_r($records);
        }
    } else {
        echo "active_boarders table does not exist\n";
    }
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
?>




