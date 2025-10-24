<?php
// Create group messaging tables
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

echo "<h1>Creating Group Messaging Tables</h1>";

try {
    $db = getDB();
    
    // Create chat_groups table
    echo "<h2>Creating chat_groups table:</h2>";
    $sql = "CREATE TABLE IF NOT EXISTS chat_groups (
        gc_id INT AUTO_INCREMENT PRIMARY KEY,
        bh_id INT NOT NULL,
        gc_name VARCHAR(255) NOT NULL,
        gc_created_by INT NOT NULL,
        gc_created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        gc_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (bh_id) REFERENCES boarding_houses(bh_id),
        FOREIGN KEY (gc_created_by) REFERENCES users(user_id)
    )";
    
    if ($db->exec($sql) !== false) {
        echo "✅ chat_groups table created successfully<br>";
    } else {
        echo "❌ Failed to create chat_groups table<br>";
    }
    
    // Create group_members table
    echo "<h2>Creating group_members table:</h2>";
    $sql = "CREATE TABLE IF NOT EXISTS group_members (
        gm_id INT AUTO_INCREMENT PRIMARY KEY,
        gc_id INT NOT NULL,
        user_id INT NOT NULL,
        gm_role ENUM('Admin', 'Boarder') DEFAULT 'Boarder',
        gm_joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (gc_id) REFERENCES chat_groups(gc_id) ON DELETE CASCADE,
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
        UNIQUE KEY unique_group_member (gc_id, user_id)
    )";
    
    if ($db->exec($sql) !== false) {
        echo "✅ group_members table created successfully<br>";
    } else {
        echo "❌ Failed to create group_members table<br>";
    }
    
    // Create group_messages table
    echo "<h2>Creating group_messages table:</h2>";
    $sql = "CREATE TABLE IF NOT EXISTS group_messages (
        groupmessage_id INT AUTO_INCREMENT PRIMARY KEY,
        gc_id INT NOT NULL,
        sender_id INT NOT NULL,
        groupmessage_text TEXT NOT NULL,
        groupmessage_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        groupmessage_status ENUM('Sent', 'Delivered', 'Read') DEFAULT 'Sent',
        FOREIGN KEY (gc_id) REFERENCES chat_groups(gc_id) ON DELETE CASCADE,
        FOREIGN KEY (sender_id) REFERENCES users(user_id) ON DELETE CASCADE
    )";
    
    if ($db->exec($sql) !== false) {
        echo "✅ group_messages table created successfully<br>";
    } else {
        echo "❌ Failed to create group_messages table<br>";
    }
    
    echo "<h2>✅ All group messaging tables created successfully!</h2>";
    
} catch (Exception $e) {
    echo "❌ Error: " . $e->getMessage();
}
?>




















