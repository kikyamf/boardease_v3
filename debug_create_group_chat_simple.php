<?php
// Simple debug version of create_group_chat.php
error_reporting(E_ALL);
ini_set('display_errors', 1);

header('Content-Type: application/json');

echo "=== DEBUG CREATE GROUP CHAT ===\n";

try {
    echo "1. Loading db_helper.php...\n";
    require_once 'db_helper.php';
    echo "✅ db_helper.php loaded\n";
    
    echo "2. Getting database connection...\n";
    $db = getDB();
    echo "✅ Database connection successful\n";
    
    echo "3. Getting request data...\n";
    $input = file_get_contents('php://input');
    echo "Raw input: " . $input . "\n";
    
    $group_name = null;
    $created_by = null;
    $members = [];
    
    if (!empty($input)) {
        $data = json_decode($input, true);
        echo "JSON decode result: " . print_r($data, true) . "\n";
        if ($data) {
            $group_name = $data['group_name'] ?? null;
            $created_by = $data['created_by'] ?? null;
            $members = $data['member_ids'] ?? [];
        }
    }
    
    echo "4. Parsed values:\n";
    echo "- group_name: " . $group_name . "\n";
    echo "- created_by: " . $created_by . "\n";
    echo "- members: " . print_r($members, true) . "\n";
    
    // Validate input
    if (!$group_name || !$created_by || empty($members)) {
        throw new Exception('Missing required parameters');
    }
    
    echo "5. Starting transaction...\n";
    $db->query("START TRANSACTION");
    echo "✅ Transaction started\n";
    
    try {
        echo "6. Getting boarding house ID...\n";
        $stmt = $db->prepare("SELECT bh_id FROM boarding_houses LIMIT 1");
        $stmt->execute();
        $bh_result = $stmt->fetch();
        $bh_id = $bh_result ? $bh_result['bh_id'] : null;
        echo "Found bh_id: " . $bh_id . "\n";
        
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
            throw new Exception('Failed to create group chat');
        }
        
        echo "9. Adding members...\n";
        $stmt = $db->prepare("
            INSERT INTO group_members (gc_id, user_id, gm_role, gm_joined_at) 
            VALUES (?, ?, 'member', NOW())
        ");
        
        foreach ($members as $member_id) {
            echo "Adding member: " . $member_id . "\n";
            $result = $stmt->execute([$group_id, $member_id]);
            echo "Member " . $member_id . " result: " . ($result ? 'success' : 'failed') . "\n";
        }
        
        echo "10. Committing transaction...\n";
        $db->query("COMMIT");
        echo "✅ Transaction committed\n";
        
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
        echo "❌ Error in transaction: " . $e->getMessage() . "\n";
        $db->query("ROLLBACK");
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