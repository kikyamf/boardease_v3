<?php
// Create a new group chat - Fixed version with foreign key constraint handling
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
    error_log("DEBUG: Raw input: " . $input);
    
    if (!empty($input)) {
        $data = json_decode($input, true);
        error_log("DEBUG: JSON decode result: " . print_r($data, true));
        if ($data) {
            $group_name = $data['group_name'] ?? null;
            $created_by = $data['created_by'] ?? null;
            $members = $data['member_ids'] ?? [];
        }
    }
    
    // Fallback to POST data
    if (!$group_name || !$created_by || empty($members)) {
        error_log("DEBUG: Trying POST data");
        $group_name = $_POST['group_name'] ?? null;
        $created_by = $_POST['created_by'] ?? null;
        $members = $_POST['member_ids'] ?? [];
        
        // If members is a JSON string, decode it
        if (is_string($members)) {
            $members = json_decode($members, true);
        }
    }
    
    error_log("DEBUG: Final values - group_name: " . $group_name . ", created_by: " . $created_by . ", members: " . print_r($members, true));
    
    // Validate input
    if (!$group_name || !$created_by || empty($members)) {
        throw new Exception('Missing required parameters: group_name, created_by, member_ids');
    }
    
    // Add creator to members if not already included
    if (!in_array($created_by, $members)) {
        $members[] = $created_by;
    }
    
    // Start transaction
    error_log("DEBUG: Starting transaction");
    $db->query("START TRANSACTION");
    
    try {
        // Get a valid bh_id from boarding_houses table owned by the creator
        error_log("DEBUG: Getting boarding house ID for user: " . $created_by);
        $stmt = $db->prepare("SELECT bh_id FROM boarding_houses WHERE user_id = ? LIMIT 1");
        $stmt->execute([$created_by]);
        $bh_result = $stmt->fetch();
        $bh_id = $bh_result ? $bh_result['bh_id'] : null;
        error_log("DEBUG: Found bh_id: " . $bh_id);
        
        // If no boarding house exists for this user, create a default one
        if (!$bh_id) {
            error_log("DEBUG: Creating default boarding house for user: " . $created_by);
            // Create a default boarding house for group chats
            $stmt = $db->prepare("
                INSERT INTO boarding_houses (user_id, bh_name, bh_address, bh_description, bh_rules, number_of_bathroom, area, build_year, status, bh_created_at) 
                VALUES (?, 'Group Chat Default', 'N/A', 'Default boarding house for group chats', 'N/A', 0, 0, 2024, 'active', NOW())
            ");
            $stmt->execute([$created_by]);
            $bh_id = $db->lastInsertId();
            error_log("DEBUG: Created default bh_id: " . $bh_id);
        }
        
        // Create group with valid bh_id
        error_log("DEBUG: Creating group chat");
        $stmt = $db->prepare("
            INSERT INTO chat_groups (bh_id, gc_name, gc_created_by, gc_created_at) 
            VALUES (?, ?, ?, NOW())
        ");
        $result = $stmt->execute([$bh_id, $group_name, $created_by]);
        error_log("DEBUG: Group creation result: " . ($result ? 'success' : 'failed'));
        $group_id = $db->lastInsertId();
        error_log("DEBUG: Group ID: " . $group_id);
        
        if (!$group_id) {
            throw new Exception('Failed to create group chat');
        }
        
        // Add members to group
        error_log("DEBUG: Adding members to group");
        $stmt = $db->prepare("
            INSERT INTO group_members (gc_id, user_id, gm_role, gm_joined_at) 
            VALUES (?, ?, 'member', NOW())
        ");
        
        foreach ($members as $member_id) {
            error_log("DEBUG: Adding member: " . $member_id);
            $result = $stmt->execute([$group_id, $member_id]);
            error_log("DEBUG: Member " . $member_id . " result: " . ($result ? 'success' : 'failed'));
        }
        
        // Commit transaction
        error_log("DEBUG: Committing transaction");
        $db->query("COMMIT");
        error_log("DEBUG: Transaction committed successfully");
        
        $response = [
            'success' => true,
            'message' => 'Group chat created successfully',
            'data' => [
                'group_id' => (int)$group_id,
                'group_name' => $group_name,
                'created_by' => (int)$created_by,
                'member_count' => count($members),
                'members' => $members,
                'bh_id_used' => (int)$bh_id
            ]
        ];
        
    } catch (Exception $e) {
        // Rollback transaction on error
        error_log("DEBUG: Error in transaction: " . $e->getMessage());
        $db->query("ROLLBACK");
        error_log("DEBUG: Transaction rolled back");
        throw $e;
    }
    
} catch (Exception $e) {
    error_log("DEBUG: Fatal error: " . $e->getMessage());
    $response = [
        'success' => false,
        'message' => 'Error: ' . $e->getMessage(),
        'data' => null
    ];
}

error_log("DEBUG: Final response: " . json_encode($response));
ob_clean();
echo json_encode($response, JSON_PRETTY_PRINT);
exit;
?>