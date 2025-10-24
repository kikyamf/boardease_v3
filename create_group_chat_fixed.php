<?php
// Fixed create group chat - handles both JSON and POST data
error_reporting(0);
ini_set('display_errors', 0);
ob_start();

header('Content-Type: application/json');

try {
    require_once 'db_helper.php';
    $db = getDB();
    
    // Get request data - try both JSON and POST
    $group_name = null;
    $created_by = null;
    $members = [];
    
    // Try JSON first
    $input = file_get_contents('php://input');
    if (!empty($input)) {
        $data = json_decode($input, true);
        if ($data) {
            $group_name = $data['group_name'] ?? null;
            $created_by = $data['created_by'] ?? null;
            $members = $data['member_ids'] ?? [];
        }
    }
    
    // Fallback to POST data
    if (!$group_name || !$created_by || empty($members)) {
        $group_name = $_POST['group_name'] ?? null;
        $created_by = $_POST['created_by'] ?? null;
        $members = $_POST['member_ids'] ?? [];
        
        // If members is a JSON string, decode it
        if (is_string($members)) {
            $members = json_decode($members, true);
        }
    }
    
    // Debug output
    error_log("DEBUG: group_name = " . ($group_name ?? 'null'));
    error_log("DEBUG: created_by = " . ($created_by ?? 'null'));
    error_log("DEBUG: members = " . json_encode($members));
    error_log("DEBUG: raw input = " . $input);
    error_log("DEBUG: POST data = " . json_encode($_POST));
    
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

ob_clean();
echo json_encode($response, JSON_PRETTY_PRINT);
exit;
?>
