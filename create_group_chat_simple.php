<?php
// Simplified create group chat without FCM
error_reporting(E_ALL);
ini_set('display_errors', 1);
ob_start();

header('Content-Type: application/json');

try {
    require_once 'db_helper.php';
    $db = getDB();
    
    // Get request data
    $input = file_get_contents('php://input');
    $data = json_decode($input, true);
    
    if (!$data) {
        throw new Exception('Invalid JSON data');
    }
    
    $group_name = $data['group_name'] ?? null;
    $created_by = $data['created_by'] ?? null;
    $members = $data['member_ids'] ?? [];
    
    // Validate input
    if (!$group_name || !$created_by || empty($members)) {
        throw new Exception('Missing required parameters: group_name, created_by, member_ids');
    }
    
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
            'message' => 'Group chat created successfully',
            'data' => [
                'group_id' => $group_id,
                'group_name' => $group_name,
                'created_by' => $created_by,
                'member_count' => count($members)
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

ob_clean();
echo json_encode($response, JSON_PRETTY_PRINT);
exit;
?>




