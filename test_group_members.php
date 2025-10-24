<?php
// Test group members functionality
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "<h2>Test Group Members</h2>";

try {
    require_once 'db_helper.php';
    $db = getDB();
    echo "<p>✅ Database connection successful</p>";
    
    // Check if there are any group chats
    echo "<p>1. Checking for group chats...</p>";
    $stmt = $db->prepare("SELECT gc_id, gc_name, gc_created_by FROM chat_groups ORDER BY gc_created_at DESC LIMIT 5");
    $stmt->execute();
    $groups = $stmt->fetchAll();
    
    if (count($groups) > 0) {
        echo "<p>✅ Found " . count($groups) . " group chat(s):</p>";
        echo "<ul>";
        foreach ($groups as $group) {
            echo "<li>Group ID: " . $group['gc_id'] . ", Name: " . $group['gc_name'] . ", Created by: " . $group['gc_created_by'] . "</li>";
        }
        echo "</ul>";
        
        // Test the latest group
        $latest_group = $groups[0];
        echo "<p>2. Testing group members for group: " . $latest_group['gc_name'] . " (ID: " . $latest_group['gc_id'] . ")</p>";
        
        $stmt = $db->prepare("
            SELECT 
                u.user_id,
                r.first_name,
                r.last_name,
                r.role as user_type,
                gm.gm_role,
                gm.gm_joined_at as joined_at
            FROM group_members gm
            JOIN users u ON gm.user_id = u.user_id
            JOIN registrations r ON u.reg_id = r.id
            WHERE gm.gc_id = ?
            ORDER BY gm.gm_joined_at ASC
        ");
        $stmt->execute([$latest_group['gc_id']]);
        $members = $stmt->fetchAll();
        
        if (count($members) > 0) {
            echo "<p>✅ Found " . count($members) . " member(s) in group:</p>";
            echo "<ul>";
            foreach ($members as $member) {
                $is_creator = $member['user_id'] == $latest_group['gc_created_by'] ? " (CREATOR)" : "";
                echo "<li>User ID: " . $member['user_id'] . ", Name: " . $member['first_name'] . " " . $member['last_name'] . ", Role: " . $member['gm_role'] . ", Type: " . $member['user_type'] . $is_creator . "</li>";
            }
            echo "</ul>";
        } else {
            echo "<p>❌ No members found in group</p>";
        }
        
    } else {
        echo "<p>❌ No group chats found</p>";
    }
    
    // Test the API endpoint
    echo "<p>3. Testing get_group_members.php API...</p>";
    $test_group_id = $groups[0]['gc_id'] ?? null;
    if ($test_group_id) {
        $api_url = "http://192.168.101.6/BoardEase2/get_group_members.php?group_id=" . $test_group_id;
        echo "<p>API URL: " . $api_url . "</p>";
        
        $response = file_get_contents($api_url);
        if ($response) {
            $data = json_decode($response, true);
            if ($data && $data['success']) {
                echo "<p>✅ API working - Found " . $data['data']['member_count'] . " members</p>";
                echo "<pre>" . json_encode($data, JSON_PRETTY_PRINT) . "</pre>";
            } else {
                echo "<p>❌ API error: " . ($data['message'] ?? 'Unknown error') . "</p>";
            }
        } else {
            echo "<p>❌ Failed to call API</p>";
        }
    }
    
} catch (Exception $e) {
    echo "<p>❌ Error: " . $e->getMessage() . "</p>";
    echo "<p>Stack trace: " . $e->getTraceAsString() . "</p>";
}
?>




