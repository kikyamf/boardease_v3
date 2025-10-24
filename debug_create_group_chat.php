<?php
// Debug version of create_group_chat.php
error_reporting(E_ALL);
ini_set('display_errors', 1);

header('Content-Type: application/json');

echo "=== DEBUG CREATE GROUP CHAT ===\n";

try {
    require_once 'db_helper.php';
    $db = getDB();
    echo "✅ Database connection successful\n";
    
    // Get request data - try both JSON and POST
    $group_name = null;
    $created_by = null;
    $members = [];
    
    echo "1. Getting request data...\n";
    
    // Try JSON first
    $input = file_get_contents('php://input');
    echo "Raw input: " . $input . "\n";
    
    if (!empty($input)) {
        $data = json_decode($input, true);
        echo "JSON decode result: " . print_r($data, true) . "\n";
        if ($data) {
            $group_name = $data['group_name'] ?? null;
            $created_by = $data['created_by'] ?? null;
            $members = $data['member_ids'] ?? [];
        }
    }
    
    // Fallback to POST data
    if (!$group_name || !$created_by || empty($members)) {
        echo "2. Trying POST data...\n";
        $group_name = $_POST['group_name'] ?? null;
        $created_by = $_POST['created_by'] ?? null;
        $members = $_POST['member_ids'] ?? [];
        
        echo "POST group_name: " . $group_name . "\n";
        echo "POST created_by: " . $created_by . "\n";
        echo "POST members: " . print_r($members, true) . "\n";
        
        // If members is a JSON string, decode it
        if (is_string($members)) {
            $members = json_decode($members, true);
            echo "Decoded members: " . print_r($members, true) . "\n";
        }
    }
    
    echo "3. Final values:\n";
    echo "- group_name: " . $group_name . "\n";
    echo "- created_by: " . $created_by . "\n";
    echo "- members: " . print_r($members, true) . "\n";
    
    // Validate input
    if (!$group_name || !$created_by || empty($members)) {
        throw new Exception('Missing required parameters: group_name=' . $group_name . ', created_by=' . $created_by . ', members=' . print_r($members, true));
    }
    
    // Add creator to members if not already included
    if (!in_array($created_by, $members)) {
        $members[] = $created_by;
        echo "4. Added creator to members: " . print_r($members, true) . "\n";
    }
    
    // Start transaction
    echo "5. Starting transaction...\n";
    $db->beginTransaction();
    
    try {
        // Get a valid bh_id from boarding_houses table
        echo "6. Getting boarding house ID...\n";
        $stmt = $db->prepare("SELECT bh_id FROM boarding_houses LIMIT 1");
        $stmt->execute();
        $bh_result = $stmt->fetch();
        $bh_id = $bh_result ? $bh_result['bh_id'] : null;
        
        echo "Found bh_id: " . $bh_id . "\n";
        
        // If no boarding house exists, create a default one
        if (!$bh_id) {
            echo "7. Creating default boarding house...\n";
            $stmt = $db->prepare("
                INSERT INTO boarding_houses (user_id, bh_name, bh_address, bh_description, bh_rules, number_of_bathroom, area, build_year, status, bh_created_at) 
                VALUES (?, 'Group Chat Default', 'N/A', 'Default boarding house for group chats', 'N/A', 0, 0, 2024, 'active', NOW())
            ");
            $stmt->execute([$created_by]);
            $bh_id = $db->lastInsertId();
            echo "Created default bh_id: " . $bh_id . "\n";
        }
        
        // Create group with valid bh_id
        echo "8. Creating group chat...\n";
        $stmt = $db->prepare("
            INSERT INTO chat_groups (bh_id, gc_name, gc_created_by, gc_created_at) 
            VALUES (?, ?, ?, NOW())
        ");
        $result = $stmt->execute([$bh_id, $group_name, $created_by]);
        echo "Group creation result: " . ($result ? 'success' : 'failed') . "\n";
        
        $group_id = $db->lastInsertId();
        echo "Group ID: " . $group_id . "\n";
        
        if (!$group_id) {
            throw new Exception('Failed to create group chat - lastInsertId returned: ' . $group_id);
        }
        
        // Add members to group
        echo "9. Adding members to group...\n";
        $stmt = $db->prepare("
            INSERT INTO group_members (gc_id, user_id, gm_role, gm_joined_at) 
            VALUES (?, ?, 'member', NOW())
        ");
        
        foreach ($members as $member_id) {
            echo "Adding member: " . $member_id . "\n";
            $result = $stmt->execute([$group_id, $member_id]);
            echo "Member " . $member_id . " result: " . ($result ? 'success' : 'failed') . "\n";
        }
        
        // Commit transaction
        echo "10. Committing transaction...\n";
        $db->commit();
        
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
        
        echo "✅ SUCCESS: " . json_encode($response, JSON_PRETTY_PRINT) . "\n";
        
    } catch (Exception $e) {
        // Rollback transaction on error
        echo "❌ Error in transaction: " . $e->getMessage() . "\n";
        $db->rollback();
        throw $e;
    }
    
} catch (Exception $e) {
    echo "❌ FATAL ERROR: " . $e->getMessage() . "\n";
    echo "Stack trace: " . $e->getTraceAsString() . "\n";
    
    $response = [
        'success' => false,
        'message' => 'Error: ' . $e->getMessage(),
        'data' => null
    ];
}

echo "\n=== FINAL RESPONSE ===\n";
echo json_encode($response, JSON_PRETTY_PRINT);
exit;
?>