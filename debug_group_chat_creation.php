<?php
// Debug group chat creation to find HTML output issue
error_reporting(0);
ini_set('display_errors', 0);
ob_start();

header('Content-Type: application/json');

try {
    // Check if db_helper.php exists and loads
    if (!file_exists('db_helper.php')) {
        throw new Exception('db_helper.php not found');
    }
    
    require_once 'db_helper.php';
    $db = getDB();
    
    if (!$db) {
        throw new Exception('Database connection failed');
    }
    
    // Test data
    $group_name = "Debug Test " . date('Y-m-d H:i:s');
    $created_by = 29;
    $members = [35, 5];
    
    // Add creator to members if not already included
    if (!in_array($created_by, $members)) {
        $members[] = $created_by;
    }
    
    // Start transaction
    $db->beginTransaction();
    
    try {
        // Create group
        $stmt = $db->prepare("
            INSERT INTO chat_groups (bh_id, gc_name, gc_created_by, gc_created_at) 
            VALUES (1, ?, ?, NOW())
        ");
        $stmt->execute([$group_name, $created_by]);
        $group_id = $db->lastInsertId();
        
        if (!$group_id) {
            throw new Exception('Failed to create group chat');
        }
        
        // Add members to group
        $stmt = $db->prepare("
            INSERT INTO group_members (gc_id, user_id, gm_role, gm_joined_at) 
            VALUES (?, ?, 'member', NOW())
        ");
        
        foreach ($members as $member_id) {
            $stmt->execute([$group_id, $member_id]);
        }
        
        // Commit transaction
        $db->commit();
        
        $response = [
            'success' => true,
            'message' => 'Debug group chat created successfully',
            'data' => [
                'group_id' => (int)$group_id,
                'group_name' => $group_name,
                'created_by' => (int)$created_by,
                'member_count' => count($members),
                'members' => $members
            ]
        ];
        
    } catch (Exception $e) {
        // Rollback transaction on error
        $db->rollback();
        throw $e;
    }
    
} catch (Exception $e) {
    $response = [
        'success' => false,
        'message' => 'Error: ' . $e->getMessage(),
        'data' => null
    ];
}

// Clean any output buffer and send response
ob_clean();
echo json_encode($response, JSON_PRETTY_PRINT);
exit;
?>




