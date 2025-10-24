<?php
// Test the fixed group creation
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "<h2>Test Group Creation - Fixed Version</h2>";

try {
    require_once 'db_helper.php';
    $db = getDB();
    echo "<p>✅ Database connection successful</p>";
    
    // Test data
    $group_name = 'Test Group 1';
    $created_by = 29;
    $members = [28, 1];
    
    echo "<p>Testing with:</p>";
    echo "<ul>";
    echo "<li>Group Name: " . $group_name . "</li>";
    echo "<li>Created By: " . $created_by . "</li>";
    echo "<li>Members: " . implode(', ', $members) . "</li>";
    echo "</ul>";
    
    // Check if user 29 has a boarding house
    echo "<p>1. Checking if user 29 has a boarding house...</p>";
    $stmt = $db->prepare("SELECT bh_id, bh_name FROM boarding_houses WHERE user_id = ?");
    $stmt->execute([$created_by]);
    $bh_result = $stmt->fetch();
    
    if ($bh_result) {
        echo "<p>✅ Found boarding house: " . $bh_result['bh_name'] . " (ID: " . $bh_result['bh_id'] . ")</p>";
        $bh_id = $bh_result['bh_id'];
    } else {
        echo "<p>❌ No boarding house found for user 29</p>";
        echo "<p>2. Creating default boarding house...</p>";
        
        $stmt = $db->prepare("
            INSERT INTO boarding_houses (user_id, bh_name, bh_address, bh_description, bh_rules, number_of_bathroom, area, build_year, status, bh_created_at) 
            VALUES (?, 'Group Chat Default', 'N/A', 'Default boarding house for group chats', 'N/A', 0, 0, 2024, 'active', NOW())
        ");
        $stmt->execute([$created_by]);
        $bh_id = $db->lastInsertId();
        echo "<p>✅ Created default boarding house with ID: " . $bh_id . "</p>";
    }
    
    echo "<p>3. Testing group creation...</p>";
    $db->query("START TRANSACTION");
    
    try {
        // Create group
        $stmt = $db->prepare("
            INSERT INTO chat_groups (bh_id, gc_name, gc_created_by, gc_created_at) 
            VALUES (?, ?, ?, NOW())
        ");
        $result = $stmt->execute([$bh_id, $group_name, $created_by]);
        
        if ($result) {
            $group_id = $db->lastInsertId();
            echo "<p>✅ Group created successfully with ID: " . $group_id . "</p>";
            
            echo "<p>4. Adding members...</p>";
            $stmt = $db->prepare("
                INSERT INTO group_members (gc_id, user_id, gm_role, gm_joined_at) 
                VALUES (?, ?, 'member', NOW())
            ");
            
            foreach ($members as $member_id) {
                $result = $stmt->execute([$group_id, $member_id]);
                echo "<p>Member " . $member_id . ": " . ($result ? '✅ Added' : '❌ Failed') . "</p>";
            }
            
            echo "<p>5. Committing transaction...</p>";
            $db->query("COMMIT");
            echo "<p>✅ Transaction committed successfully</p>";
            
            echo "<p>6. Cleaning up test data...</p>";
            $stmt = $db->prepare("DELETE FROM group_members WHERE gc_id = ?");
            $stmt->execute([$group_id]);
            
            $stmt = $db->prepare("DELETE FROM chat_groups WHERE gc_id = ?");
            $stmt->execute([$group_id]);
            echo "<p>✅ Test data cleaned up</p>";
            
        } else {
            echo "<p>❌ Group creation failed</p>";
            $db->query("ROLLBACK");
        }
        
    } catch (Exception $e) {
        echo "<p>❌ Error in transaction: " . $e->getMessage() . "</p>";
        $db->query("ROLLBACK");
    }
    
} catch (Exception $e) {
    echo "<p>❌ Error: " . $e->getMessage() . "</p>";
}
?>




