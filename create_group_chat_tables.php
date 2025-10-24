<?php
// Create group chat tables if they don't exist
error_reporting(0);
ini_set('display_errors', 0);
ob_start();
require_once 'db_helper.php';
header('Content-Type: application/json');

try {
    $db = getDB();
    
    // Create chat_groups table
    $create_chat_groups = "
        CREATE TABLE IF NOT EXISTS chat_groups (
            gc_id INT AUTO_INCREMENT PRIMARY KEY,
            bh_id INT DEFAULT 1,
            gc_name VARCHAR(255) NOT NULL,
            gc_created_by INT NOT NULL,
            gc_created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            gc_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            gc_status ENUM('active', 'inactive', 'deleted') DEFAULT 'active',
            INDEX idx_bh_id (bh_id),
            INDEX idx_created_by (gc_created_by),
            INDEX idx_status (gc_status)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    ";
    
    $db->exec($create_chat_groups);
    
    // Create group_members table
    $create_group_members = "
        CREATE TABLE IF NOT EXISTS group_members (
            gm_id INT AUTO_INCREMENT PRIMARY KEY,
            gc_id INT NOT NULL,
            user_id INT NOT NULL,
            gm_role ENUM('admin', 'member') DEFAULT 'member',
            gm_joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            gm_status ENUM('active', 'left', 'removed') DEFAULT 'active',
            UNIQUE KEY unique_group_user (gc_id, user_id),
            INDEX idx_gc_id (gc_id),
            INDEX idx_user_id (user_id),
            INDEX idx_status (gm_status),
            FOREIGN KEY (gc_id) REFERENCES chat_groups(gc_id) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    ";
    
    $db->exec($create_group_members);
    
    // Create group_messages table
    $create_group_messages = "
        CREATE TABLE IF NOT EXISTS group_messages (
            gm_id INT AUTO_INCREMENT PRIMARY KEY,
            gc_id INT NOT NULL,
            sender_id INT NOT NULL,
            groupmessage_text TEXT NOT NULL,
            groupmessage_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            groupmessage_status ENUM('sent', 'delivered', 'read', 'deleted') DEFAULT 'sent',
            message_type ENUM('text', 'image', 'file', 'system') DEFAULT 'text',
            INDEX idx_gc_id (gc_id),
            INDEX idx_sender_id (sender_id),
            INDEX idx_timestamp (groupmessage_timestamp),
            INDEX idx_status (groupmessage_status),
            FOREIGN KEY (gc_id) REFERENCES chat_groups(gc_id) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    ";
    
    $db->exec($create_group_messages);
    
    $response = [
        'success' => true,
        'message' => 'Group chat tables created successfully',
        'data' => [
            'tables_created' => [
                'chat_groups',
                'group_members', 
                'group_messages'
            ]
        ]
    ];
    
} catch (Exception $e) {
    $response = [
        'success' => false,
        'message' => 'Error: ' . $e->getMessage(),
        'data' => null
    ];
}

ob_clean();
echo json_encode($response, JSON_PRETTY_PRINT);
exit;
?>




