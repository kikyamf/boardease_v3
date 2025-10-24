<?php
// Check existing group chat tables structure
error_reporting(E_ALL);
ini_set('display_errors', 1);
ob_start();

header('Content-Type: application/json');

try {
    require_once 'db_helper.php';
    $db = getDB();
    
    // Check chat_groups table structure
    $stmt = $db->prepare("DESCRIBE chat_groups");
    $stmt->execute();
    $chat_groups_structure = $stmt->fetchAll();
    
    // Check group_members table structure
    $stmt = $db->prepare("DESCRIBE group_members");
    $stmt->execute();
    $group_members_structure = $stmt->fetchAll();
    
    // Check if group_messages table exists
    $stmt = $db->prepare("SHOW TABLES LIKE 'group_messages'");
    $stmt->execute();
    $group_messages_exists = $stmt->fetch() ? true : false;
    
    $response = [
        'success' => true,
        'data' => [
            'chat_groups_structure' => $chat_groups_structure,
            'group_members_structure' => $group_members_structure,
            'group_messages_exists' => $group_messages_exists
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




