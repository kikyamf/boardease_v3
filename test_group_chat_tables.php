<?php
// Test if group chat tables exist and have correct structure
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "<h2>Group Chat Tables Test</h2>";

try {
    require_once 'db_helper.php';
    $db = getDB();
    echo "<p>✅ Database connection successful</p>";
    
    // Check if chat_groups table exists
    echo "<h3>1. Checking chat_groups table:</h3>";
    $stmt = $db->prepare("SHOW TABLES LIKE 'chat_groups'");
    $stmt->execute();
    $table_exists = $stmt->fetch();
    
    if ($table_exists) {
        echo "<p>✅ chat_groups table exists</p>";
        
        // Show table structure
        $stmt = $db->prepare("DESCRIBE chat_groups");
        $stmt->execute();
        $columns = $stmt->fetchAll();
        
        echo "<h4>Table structure:</h4><ul>";
        foreach ($columns as $col) {
            echo "<li>" . $col['Field'] . " (" . $col['Type'] . ")</li>";
        }
        echo "</ul>";
        
        // Check current data
        $stmt = $db->prepare("SELECT * FROM chat_groups ORDER BY gc_id DESC LIMIT 5");
        $stmt->execute();
        $data = $stmt->fetchAll();
        
        echo "<h4>Recent data:</h4>";
        if (empty($data)) {
            echo "<p>No data found</p>";
        } else {
            echo "<ul>";
            foreach ($data as $row) {
                echo "<li>ID: " . $row['gc_id'] . ", Name: " . $row['gc_name'] . ", Created by: " . $row['gc_created_by'] . ", BH ID: " . $row['bh_id'] . "</li>";
            }
            echo "</ul>";
        }
    } else {
        echo "<p>❌ chat_groups table does not exist</p>";
    }
    
    // Check if group_members table exists
    echo "<h3>2. Checking group_members table:</h3>";
    $stmt = $db->prepare("SHOW TABLES LIKE 'group_members'");
    $stmt->execute();
    $table_exists = $stmt->fetch();
    
    if ($table_exists) {
        echo "<p>✅ group_members table exists</p>";
        
        // Show table structure
        $stmt = $db->prepare("DESCRIBE group_members");
        $stmt->execute();
        $columns = $stmt->fetchAll();
        
        echo "<h4>Table structure:</h4><ul>";
        foreach ($columns as $col) {
            echo "<li>" . $col['Field'] . " (" . $col['Type'] . ")</li>";
        }
        echo "</ul>";
        
        // Check current data
        $stmt = $db->prepare("SELECT * FROM group_members ORDER BY gm_id DESC LIMIT 5");
        $stmt->execute();
        $data = $stmt->fetchAll();
        
        echo "<h4>Recent data:</h4>";
        if (empty($data)) {
            echo "<p>No data found</p>";
        } else {
            echo "<ul>";
            foreach ($data as $row) {
                echo "<li>ID: " . $row['gm_id'] . ", Group ID: " . $row['gc_id'] . ", User ID: " . $row['user_id'] . ", Role: " . $row['gm_role'] . "</li>";
            }
            echo "</ul>";
        }
    } else {
        echo "<p>❌ group_members table does not exist</p>";
    }
    
    // Check if boarding_houses table exists
    echo "<h3>3. Checking boarding_houses table:</h3>";
    $stmt = $db->prepare("SHOW TABLES LIKE 'boarding_houses'");
    $stmt->execute();
    $table_exists = $stmt->fetch();
    
    if ($table_exists) {
        echo "<p>✅ boarding_houses table exists</p>";
        
        // Check current data
        $stmt = $db->prepare("SELECT bh_id, user_id, bh_name FROM boarding_houses ORDER BY bh_id LIMIT 5");
        $stmt->execute();
        $data = $stmt->fetchAll();
        
        echo "<h4>Recent data:</h4>";
        if (empty($data)) {
            echo "<p>No data found</p>";
        } else {
            echo "<ul>";
            foreach ($data as $row) {
                echo "<li>BH ID: " . $row['bh_id'] . ", Owner: " . $row['user_id'] . ", Name: " . $row['bh_name'] . "</li>";
            }
            echo "</ul>";
        }
    } else {
        echo "<p>❌ boarding_houses table does not exist</p>";
    }
    
    // Test creating a simple group
    echo "<h3>4. Testing group creation:</h3>";
    try {
        $db->beginTransaction();
        
        // Get a valid bh_id
        $stmt = $db->prepare("SELECT bh_id FROM boarding_houses LIMIT 1");
        $stmt->execute();
        $bh_result = $stmt->fetch();
        $bh_id = $bh_result ? $bh_result['bh_id'] : null;
        
        if (!$bh_id) {
            echo "<p>❌ No boarding houses found - cannot test group creation</p>";
        } else {
            echo "<p>✅ Found boarding house ID: " . $bh_id . "</p>";
            
            // Try to create a test group
            $stmt = $db->prepare("
                INSERT INTO chat_groups (bh_id, gc_name, gc_created_by, gc_created_at) 
                VALUES (?, 'Test Group', ?, NOW())
            ");
            $result = $stmt->execute([$bh_id, 29]);
            
            if ($result) {
                $group_id = $db->lastInsertId();
                echo "<p>✅ Test group created successfully with ID: " . $group_id . "</p>";
                
                // Clean up test group
                $stmt = $db->prepare("DELETE FROM chat_groups WHERE gc_id = ?");
                $stmt->execute([$group_id]);
                echo "<p>✅ Test group cleaned up</p>";
            } else {
                echo "<p>❌ Failed to create test group</p>";
            }
        }
        
        $db->rollback();
        
    } catch (Exception $e) {
        $db->rollback();
        echo "<p>❌ Test group creation failed: " . $e->getMessage() . "</p>";
    }
    
} catch (Exception $e) {
    echo "<p>❌ Error: " . $e->getMessage() . "</p>";
}
?>