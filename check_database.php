<?php
// Check database connection and tables
require_once 'dbConfig.php';

echo "Database Configuration:\n";
echo "Host: " . DB_HOST . "\n";
echo "Database: " . DB_NAME . "\n";
echo "User: " . DB_USER . "\n";
echo "\n";

try {
    // Check current database
    $result = $conn->query("SELECT DATABASE() as current_db");
    $current_db = $result->fetch_assoc()['current_db'];
    echo "Currently connected to database: $current_db\n\n";
    
    // Show all tables
    $result = $conn->query("SHOW TABLES");
    $tables = [];
    while ($row = $result->fetch_array()) {
        $tables[] = $row[0];
    }
    
    echo "Available tables in $current_db:\n";
    foreach ($tables as $table) {
        echo "- $table\n";
    }
    
    // Check if notifications table exists
    if (in_array('notifications', $tables)) {
        echo "\n✓ Notifications table exists!\n";
        
        // Count notifications
        $result = $conn->query("SELECT COUNT(*) as total FROM notifications");
        $total = $result->fetch_assoc()['total'];
        echo "Total notifications: $total\n";
        
        // Count unread notifications for user 1
        $result = $conn->query("SELECT COUNT(*) as unread FROM notifications WHERE user_id = 1 AND notif_status = 'unread'");
        $unread = $result->fetch_assoc()['unread'];
        echo "Unread notifications for user 1: $unread\n";
    } else {
        echo "\n✗ Notifications table does NOT exist!\n";
    }
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}

$conn->close();
?>







