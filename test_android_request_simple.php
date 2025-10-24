<?php
// Test the exact Android request
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "<h2>Test Android Request</h2>";

// Simulate the exact JSON request from Android
$json_data = json_encode([
    'group_name' => 'Group 1',
    'created_by' => 29,
    'member_ids' => [28, 1]
]);

echo "<p>Simulating Android request with JSON:</p>";
echo "<pre>" . htmlspecialchars($json_data) . "</pre>";

// Test the debug version
echo "<h3>Testing debug_create_group_chat_simple.php:</h3>";

// Set up the request
$_SERVER['REQUEST_METHOD'] = 'POST';
$_SERVER['CONTENT_TYPE'] = 'application/json';

// Capture the output
ob_start();

// Simulate the input
$GLOBALS['input_data'] = $json_data;

// Set up the input data for testing
$GLOBALS['test_input_data'] = $json_data;

// Include the debug version
try {
    include 'debug_create_group_chat_simple.php';
} catch (Exception $e) {
    echo "<p>❌ Error including debug_create_group_chat_simple.php: " . $e->getMessage() . "</p>";
}

$output = ob_get_clean();
echo "<h3>Response from debug_create_group_chat_simple.php:</h3>";
echo "<pre>" . htmlspecialchars($output) . "</pre>";

// Also test with a direct call
echo "<h3>Direct Test:</h3>";
echo "<p>Testing direct database operations...</p>";

try {
    require_once 'db_helper.php';
    $db = getDB();
    
    echo "<p>✅ Database connection successful</p>";
    
    // Test the exact same operations
    $group_name = 'Group 1';
    $created_by = 29;
    $members = [28, 1];
    
    // Check if tables exist
    $stmt = $db->prepare("SHOW TABLES LIKE 'chat_groups'");
    $stmt->execute();
    $chat_groups_exists = $stmt->fetch();
    
    $stmt = $db->prepare("SHOW TABLES LIKE 'group_members'");
    $stmt->execute();
    $group_members_exists = $stmt->fetch();
    
    echo "<p>" . ($chat_groups_exists ? "✅" : "❌") . " chat_groups table exists</p>";
    echo "<p>" . ($group_members_exists ? "✅" : "❌") . " group_members table exists</p>";
    
    if (!$chat_groups_exists || !$group_members_exists) {
        echo "<p>❌ Required tables are missing</p>";
    } else {
        // Try to create a group
        $db->query("START TRANSACTION");
        
        try {
            // Get bh_id
            $stmt = $db->prepare("SELECT bh_id FROM boarding_houses LIMIT 1");
            $stmt->execute();
            $bh_result = $stmt->fetch();
            $bh_id = $bh_result ? $bh_result['bh_id'] : null;
            
            if (!$bh_id) {
                echo "<p>❌ No boarding houses found</p>";
                $db->query("ROLLBACK");
            } else {
                echo "<p>✅ Found boarding house ID: " . $bh_id . "</p>";
                
                // Create group
                $stmt = $db->prepare("
                    INSERT INTO chat_groups (bh_id, gc_name, gc_created_by, gc_created_at) 
                    VALUES (?, ?, ?, NOW())
                ");
                $result = $stmt->execute([$bh_id, $group_name, $created_by]);
                
                if ($result) {
                    $group_id = $db->lastInsertId();
                    echo "<p>✅ Group created with ID: " . $group_id . "</p>";
                    
                    // Add members
                    $stmt = $db->prepare("
                        INSERT INTO group_members (gc_id, user_id, gm_role, gm_joined_at) 
                        VALUES (?, ?, 'member', NOW())
                    ");
                    
                    foreach ($members as $member_id) {
                        $result = $stmt->execute([$group_id, $member_id]);
                        echo "<p>" . ($result ? "✅" : "❌") . " Added member $member_id</p>";
                    }
                    
                    $db->query("COMMIT");
                    echo "<p>✅ Transaction committed</p>";
                } else {
                    echo "<p>❌ Failed to create group</p>";
                    $db->query("ROLLBACK");
                }
            }
        } catch (Exception $e) {
            $db->query("ROLLBACK");
            echo "<p>❌ Transaction error: " . $e->getMessage() . "</p>";
        }
    }
    
} catch (Exception $e) {
    echo "<p>❌ Fatal error: " . $e->getMessage() . "</p>";
}
?>
