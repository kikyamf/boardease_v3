<?php
// Test database connection from message directory
require_once 'message/../db_helper.php';

try {
    $db = getDB();
    echo "Database connection successful\n";
    
    // Test a simple query
    $stmt = $db->prepare("SELECT COUNT(*) as count FROM users");
    $stmt->execute();
    $result = $stmt->fetch();
    echo "Users count: " . $result['count'] . "\n";
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
?>




