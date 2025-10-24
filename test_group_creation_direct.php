<?php
// Direct test of group creation without function overrides
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "<h2>Direct Group Creation Test</h2>";

try {
    require_once 'db_helper.php';
    $db = getDB();
    echo "<p>✅ Database connection successful</p>";
    
    // Test data (same as Android app sends)
    $group_name = "Test Group 1";
    $created_by = 29;
    $members = [28, 1];
    
    echo "<p>Testing with:</p>";
    echo "<ul>";
    echo "<li>Group Name: " . $group_name . "</li>";
    echo "<li>Created By: " . $created_by . "</li>";
    echo "<li>Members: " . implode(', ', $members) . "</li>";
    echo "</ul>";
    
    // Check if required tables exist
    echo "<h3>1. Checking tables:</h3>";
    
    $tables = ['chat_groups', 'group_members', 'boarding_houses'];
    foreach ($tables as $table) {
        $stmt = $db->prepare("SHOW TABLES LIKE ?");
        $stmt->execute([$table]);
        $exists = $stmt->fetch();
        echo "<p>" . ($exists ? "✅" : "❌") . " Table '$table' " . ($exists ? "exists" : "does not exist") . "</p>";
    }
    
    // Check if users exist
    echo "<h3>2. Checking users:</h3>";
    $user_ids = array_merge([$created_by], $members);
    foreach ($user_ids as $user_id) {
        $stmt = $db->prepare("SELECT user_id FROM users WHERE user_id = ?");
        $stmt->execute([$user_id]);
        $exists = $stmt->fetch();
        echo "<p>" . ($exists ? "✅" : "❌") . " User $user_id " . ($exists ? "exists" : "does not exist") . "</p>";
    }
    
    // Check boarding houses
    echo "<h3>3. Checking boarding houses:</h3>";
    $stmt = $db->prepare("SELECT bh_id, user_id, bh_name FROM boarding_houses LIMIT 5");
    $stmt->execute();
    $bhs = $stmt->fetchAll();
    
    if (empty($bhs)) {
        echo "<p>❌ No boarding houses found</p>";
    } else {
        echo "<p>✅ Found boarding houses:</p><ul>";
        foreach ($bhs as $bh) {
            echo "<li>BH ID: " . $bh['bh_id'] . ", Owner: " . $bh['user_id'] . ", Name: " . $bh['bh_name'] . "</li>";
        }
        echo "</ul>";
    }
    
    // Try to create a group
    echo "<h3>4. Testing group creation:</h3>";
    
    try {
        $db->query("START TRANSACTION");
        echo "<p>✅ Transaction started</p>";
        
        // Get a valid bh_id
        $stmt = $db->prepare("SELECT bh_id FROM boarding_houses LIMIT 1");
        $stmt->execute();
        $bh_result = $stmt->fetch();
        $bh_id = $bh_result ? $bh_result['bh_id'] : null;
        
        if (!$bh_id) {
            echo "<p>❌ No boarding houses found - cannot create group</p>";
            $db->query("ROLLBACK");
        } else {
            echo "<p>✅ Using boarding house ID: " . $bh_id . "</p>";
            
            // Create group
            $stmt = $db->prepare("
                INSERT INTO chat_groups (bh_id, gc_name, gc_created_by, gc_created_at) 
                VALUES (?, ?, ?, NOW())
            ");
            $result = $stmt->execute([$bh_id, $group_name, $created_by]);
            
            if ($result) {
                $group_id = $db->lastInsertId();
                echo "<p>✅ Group created successfully with ID: " . $group_id . "</p>";
                
                // Add members
                $stmt = $db->prepare("
                    INSERT INTO group_members (gc_id, user_id, gm_role, gm_joined_at) 
                    VALUES (?, ?, 'member', NOW())
                ");
                
                foreach ($members as $member_id) {
                    $result = $stmt->execute([$group_id, $member_id]);
                    echo "<p>" . ($result ? "✅" : "❌") . " Added member $member_id</p>";
                }
                
                // Commit
                $db->query("COMMIT");
                echo "<p>✅ Transaction committed successfully</p>";
                
                // Show final result
                $stmt = $db->prepare("SELECT * FROM chat_groups WHERE gc_id = ?");
                $stmt->execute([$group_id]);
                $group = $stmt->fetch();
                
                echo "<h4>Created Group:</h4>";
                echo "<ul>";
                echo "<li>ID: " . $group['gc_id'] . "</li>";
                echo "<li>Name: " . $group['gc_name'] . "</li>";
                echo "<li>Created By: " . $group['gc_created_by'] . "</li>";
                echo "<li>BH ID: " . $group['bh_id'] . "</li>";
                echo "</ul>";
                
                // Show members
                $stmt = $db->prepare("SELECT * FROM group_members WHERE gc_id = ?");
                $stmt->execute([$group_id]);
                $group_members = $stmt->fetchAll();
                
                echo "<h4>Group Members:</h4><ul>";
                foreach ($group_members as $member) {
                    echo "<li>User ID: " . $member['user_id'] . ", Role: " . $member['gm_role'] . "</li>";
                }
                echo "</ul>";
                
            } else {
                echo "<p>❌ Failed to create group</p>";
                $db->query("ROLLBACK");
            }
        }
        
    } catch (Exception $e) {
        $db->query("ROLLBACK");
        echo "<p>❌ Error during group creation: " . $e->getMessage() . "</p>";
    }
    
} catch (Exception $e) {
    echo "<p>❌ Fatal error: " . $e->getMessage() . "</p>";
}
?>




