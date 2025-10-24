<?php
// Simple test to identify the exact issue
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "<h2>Simple Group Creation Test</h2>";

try {
    echo "<p>1. Testing database connection...</p>";
    require_once 'db_helper.php';
    $db = getDB();
    echo "<p>✅ Database connection successful</p>";
    
    echo "<p>2. Testing basic query...</p>";
    $stmt = $db->prepare("SELECT COUNT(*) as count FROM boarding_houses");
    $stmt->execute();
    $result = $stmt->fetch();
    echo "<p>✅ Boarding houses count: " . $result['count'] . "</p>";
    
    echo "<p>3. Testing chat_groups table...</p>";
    $stmt = $db->prepare("SELECT COUNT(*) as count FROM chat_groups");
    $stmt->execute();
    $result = $stmt->fetch();
    echo "<p>✅ Chat groups count: " . $result['count'] . "</p>";
    
    echo "<p>4. Testing group_members table...</p>";
    $stmt = $db->prepare("SELECT COUNT(*) as count FROM group_members");
    $stmt->execute();
    $result = $stmt->fetch();
    echo "<p>✅ Group members count: " . $result['count'] . "</p>";
    
    echo "<p>5. Testing transaction...</p>";
    $db->query("START TRANSACTION");
    echo "<p>✅ Transaction started</p>";
    
    echo "<p>6. Testing group creation...</p>";
    $stmt = $db->prepare("
        INSERT INTO chat_groups (bh_id, gc_name, gc_created_by, gc_created_at) 
        VALUES (?, ?, ?, NOW())
    ");
    $result = $stmt->execute([11, 'Test Group', 29]);
    echo "<p>Group creation result: " . ($result ? 'success' : 'failed') . "</p>";
    
    if ($result) {
        $group_id = $db->lastInsertId();
        echo "<p>✅ Group created with ID: " . $group_id . "</p>";
        
        echo "<p>7. Testing member addition...</p>";
        $stmt = $db->prepare("
            INSERT INTO group_members (gc_id, user_id, gm_role, gm_joined_at) 
            VALUES (?, ?, 'member', NOW())
        ");
        $result = $stmt->execute([$group_id, 28]);
        echo "<p>Member addition result: " . ($result ? 'success' : 'failed') . "</p>";
        
        if ($result) {
            echo "<p>8. Committing transaction...</p>";
            $db->query("COMMIT");
            echo "<p>✅ Transaction committed</p>";
            
            echo "<p>9. Cleaning up test data...</p>";
            $stmt = $db->prepare("DELETE FROM group_members WHERE gc_id = ?");
            $stmt->execute([$group_id]);
            
            $stmt = $db->prepare("DELETE FROM chat_groups WHERE gc_id = ?");
            $stmt->execute([$group_id]);
            echo "<p>✅ Test data cleaned up</p>";
        } else {
            $db->query("ROLLBACK");
            echo "<p>❌ Member addition failed, rolled back</p>";
        }
    } else {
        $db->query("ROLLBACK");
        echo "<p>❌ Group creation failed, rolled back</p>";
    }
    
} catch (Exception $e) {
    echo "<p>❌ Error: " . $e->getMessage() . "</p>";
    echo "<p>Stack trace: " . $e->getTraceAsString() . "</p>";
}
?>