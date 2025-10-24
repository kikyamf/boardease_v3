<?php
// Step by step debug for group chat creation
error_reporting(0);
ini_set('display_errors', 0);
ob_start();

header('Content-Type: application/json');

$debug_steps = [];

try {
    // Step 1: Check if db_helper.php exists
    $debug_steps[] = "Step 1: Checking db_helper.php";
    if (!file_exists('db_helper.php')) {
        throw new Exception('db_helper.php not found');
    }
    $debug_steps[] = "✅ db_helper.php exists";
    
    // Step 2: Load db_helper.php
    $debug_steps[] = "Step 2: Loading db_helper.php";
    require_once 'db_helper.php';
    $debug_steps[] = "✅ db_helper.php loaded";
    
    // Step 3: Test database connection
    $debug_steps[] = "Step 3: Testing database connection";
    $db = getDB();
    if (!$db) {
        throw new Exception('Database connection failed');
    }
    $debug_steps[] = "✅ Database connection successful";
    
    // Step 4: Check if required tables exist
    $debug_steps[] = "Step 4: Checking required tables";
    $tables = ['chat_groups', 'group_members'];
    foreach ($tables as $table) {
        $stmt = $db->prepare("SHOW TABLES LIKE ?");
        $stmt->execute([$table]);
        if (!$stmt->fetch()) {
            throw new Exception("Table $table does not exist");
        }
        $debug_steps[] = "✅ Table $table exists";
    }
    
    // Step 5: Test simple insert
    $debug_steps[] = "Step 5: Testing simple insert";
    $test_group_name = "Test Group " . time();
    $stmt = $db->prepare("INSERT INTO chat_groups (bh_id, gc_name, gc_created_by, gc_created_at) VALUES (1, ?, 29, NOW())");
    $stmt->execute([$test_group_name]);
    $group_id = $db->lastInsertId();
    
    if (!$group_id) {
        throw new Exception('Failed to create test group');
    }
    $debug_steps[] = "✅ Test group created with ID: $group_id";
    
    // Step 6: Test member insertion
    $debug_steps[] = "Step 6: Testing member insertion";
    $stmt = $db->prepare("INSERT INTO group_members (gc_id, user_id, gm_role, gm_joined_at) VALUES (?, 29, 'member', NOW())");
    $stmt->execute([$group_id]);
    $debug_steps[] = "✅ Test member added";
    
    // Step 7: Clean up test data
    $debug_steps[] = "Step 7: Cleaning up test data";
    $stmt = $db->prepare("DELETE FROM group_members WHERE gc_id = ?");
    $stmt->execute([$group_id]);
    $stmt = $db->prepare("DELETE FROM chat_groups WHERE gc_id = ?");
    $stmt->execute([$group_id]);
    $debug_steps[] = "✅ Test data cleaned up";
    
    $response = [
        'success' => true,
        'message' => 'All steps completed successfully',
        'data' => [
            'debug_steps' => $debug_steps,
            'timestamp' => date('Y-m-d H:i:s')
        ]
    ];
    
} catch (Exception $e) {
    $debug_steps[] = "❌ Error: " . $e->getMessage();
    $response = [
        'success' => false,
        'message' => 'Error: ' . $e->getMessage(),
        'data' => [
            'debug_steps' => $debug_steps
        ]
    ];
}

ob_clean();
echo json_encode($response, JSON_PRETTY_PRINT);
exit;
?>




