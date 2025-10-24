<?php
// Step by step debug to find the exact problem
error_reporting(E_ALL);
ini_set('display_errors', 1);
ob_start();

header('Content-Type: application/json');

$debug_steps = [];
$error_occurred = false;

try {
    $debug_steps[] = "Step 1: Starting debug...";
    
    // Step 2: Test basic PHP
    $debug_steps[] = "Step 2: Testing basic PHP";
    $debug_steps[] = "✅ Basic PHP working";
    
    // Step 3: Test file existence
    $debug_steps[] = "Step 3: Testing file existence";
    if (!file_exists('db_helper.php')) {
        throw new Exception('db_helper.php not found');
    }
    $debug_steps[] = "✅ db_helper.php exists";
    
    // Step 4: Test require_once
    $debug_steps[] = "Step 4: Testing require_once";
    require_once 'db_helper.php';
    $debug_steps[] = "✅ db_helper.php loaded";
    
    // Step 5: Test database connection
    $debug_steps[] = "Step 5: Testing database connection";
    $db = getDB();
    if (!$db) {
        throw new Exception('Database connection failed');
    }
    $debug_steps[] = "✅ Database connection successful";
    
    // Step 6: Test JSON input
    $debug_steps[] = "Step 6: Testing JSON input";
    $input = file_get_contents('php://input');
    $debug_steps[] = "Raw input: " . $input;
    
    $data = json_decode($input, true);
    if (!$data) {
        throw new Exception('Invalid JSON data');
    }
    $debug_steps[] = "✅ JSON data parsed successfully";
    
    // Step 7: Test data extraction
    $debug_steps[] = "Step 7: Testing data extraction";
    $group_name = $data['group_name'] ?? null;
    $created_by = $data['created_by'] ?? null;
    $members = $data['member_ids'] ?? [];
    $debug_steps[] = "Group name: " . ($group_name ?? 'null');
    $debug_steps[] = "Created by: " . ($created_by ?? 'null');
    $debug_steps[] = "Members: " . json_encode($members);
    
    // Step 8: Test validation
    $debug_steps[] = "Step 8: Testing validation";
    if (!$group_name || !$created_by || empty($members)) {
        throw new Exception('Missing required parameters');
    }
    $debug_steps[] = "✅ Validation passed";
    
    // Step 9: Test transaction start
    $debug_steps[] = "Step 9: Testing transaction start";
    $db->beginTransaction();
    $debug_steps[] = "✅ Transaction started";
    
    // Step 10: Test bh_id selection
    $debug_steps[] = "Step 10: Testing bh_id selection";
    $stmt = $db->prepare("SELECT bh_id FROM boarding_houses LIMIT 1");
    $stmt->execute();
    $bh_result = $stmt->fetch();
    $bh_id = $bh_result ? $bh_result['bh_id'] : 11;
    $debug_steps[] = "✅ bh_id selected: " . $bh_id;
    
    // Step 11: Test group creation
    $debug_steps[] = "Step 11: Testing group creation";
    $stmt = $db->prepare("INSERT INTO chat_groups (bh_id, gc_name, gc_created_by, gc_created_at) VALUES (?, ?, ?, NOW())");
    $stmt->execute([$bh_id, $group_name, $created_by]);
    $group_id = $db->lastInsertId();
    $debug_steps[] = "✅ Group created with ID: " . $group_id;
    
    // Step 12: Test member addition
    $debug_steps[] = "Step 12: Testing member addition";
    $stmt = $db->prepare("INSERT INTO group_members (gc_id, user_id, gm_role, gm_joined_at) VALUES (?, ?, 'member', NOW())");
    foreach ($members as $member_id) {
        $stmt->execute([$group_id, $member_id]);
        $debug_steps[] = "✅ Member added: " . $member_id;
    }
    
    // Step 13: Test transaction commit
    $debug_steps[] = "Step 13: Testing transaction commit";
    $db->commit();
    $debug_steps[] = "✅ Transaction committed";
    
    $response = [
        'success' => true,
        'message' => 'All steps completed successfully',
        'data' => [
            'group_id' => (int)$group_id,
            'group_name' => $group_name,
            'created_by' => (int)$created_by,
            'member_count' => count($members),
            'members' => $members,
            'bh_id_used' => (int)$bh_id,
            'debug_steps' => $debug_steps
        ]
    ];
    
} catch (Exception $e) {
    $error_occurred = true;
    $debug_steps[] = "❌ Error at step: " . $e->getMessage();
    
    // Rollback transaction if it was started
    if (isset($db) && $db->inTransaction()) {
        $db->rollback();
        $debug_steps[] = "✅ Transaction rolled back";
    }
    
    $response = [
        'success' => false,
        'message' => 'Error: ' . $e->getMessage(),
        'data' => [
            'debug_steps' => $debug_steps,
            'error_occurred' => true
        ]
    ];
}

ob_clean();
echo json_encode($response, JSON_PRETTY_PRINT);
exit;
?>




