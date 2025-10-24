<?php
// Check if group chat tables exist
error_reporting(E_ALL);
ini_set('display_errors', 1);
ob_start();

header('Content-Type: application/json');

try {
    require_once 'db_helper.php';
    $db = getDB();
    
    // Check if tables exist
    $tables = ['chat_groups', 'group_members', 'group_messages'];
    $table_status = [];
    
    foreach ($tables as $table) {
        $stmt = $db->prepare("SHOW TABLES LIKE ?");
        $stmt->execute([$table]);
        $exists = $stmt->fetch() ? true : false;
        $table_status[$table] = $exists;
        
        if ($exists) {
            // Get table structure
            $stmt = $db->prepare("DESCRIBE $table");
            $stmt->execute();
            $structure = $stmt->fetchAll();
            $table_status[$table . '_structure'] = $structure;
        }
    }
    
    $response = [
        'success' => true,
        'data' => $table_status
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




