<?php
// Test group messages functionality
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "<h2>Test Group Messages</h2>";

try {
    require_once 'db_helper.php';
    $db = getDB();
    echo "<p>✅ Database connection successful</p>";
    
    // Check if there are any group chats
    echo "<p>1. Checking for group chats...</p>";
    $stmt = $db->prepare("SELECT gc_id, gc_name, gc_created_by FROM chat_groups ORDER BY gc_created_at DESC LIMIT 3");
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
        echo "<p>2. Testing group messages for group: " . $latest_group['gc_name'] . " (ID: " . $latest_group['gc_id'] . ")</p>";
        
        // Check if there are any messages
        $stmt = $db->prepare("
            SELECT 
                gm.groupmessage_id,
                gm.sender_id,
                gm.groupmessage_text,
                gm.groupmessage_timestamp,
                r.first_name,
                r.last_name,
                r.role as user_type
            FROM group_messages gm
            JOIN users u ON gm.sender_id = u.user_id
            JOIN registrations r ON u.reg_id = r.id
            WHERE gm.gc_id = ?
            ORDER BY gm.groupmessage_timestamp ASC
        ");
        $stmt->execute([$latest_group['gc_id']]);
        $messages = $stmt->fetchAll();
        
        if (count($messages) > 0) {
            echo "<p>✅ Found " . count($messages) . " message(s) in group:</p>";
            echo "<ul>";
            foreach ($messages as $message) {
                $sender_name = $message['first_name'] . ' ' . $message['last_name'];
                $formatted_time = date('M j, g:i A', strtotime($message['groupmessage_timestamp']));
                echo "<li><strong>" . $sender_name . " (" . $message['user_type'] . ")</strong>: " . $message['groupmessage_text'] . " <em>[" . $formatted_time . "]</em></li>";
            }
            echo "</ul>";
        } else {
            echo "<p>❌ No messages found in group</p>";
            echo "<p>3. Creating a test message...</p>";
            
            // Create a test message
            $stmt = $db->prepare("
                INSERT INTO group_messages (gc_id, sender_id, groupmessage_text, groupmessage_timestamp, groupmessage_status) 
                VALUES (?, ?, ?, NOW(), 'Sent')
            ");
            $test_message = "Test message from " . $latest_group['gc_created_by'];
            $result = $stmt->execute([$latest_group['gc_id'], $latest_group['gc_created_by'], $test_message]);
            
            if ($result) {
                echo "<p>✅ Test message created successfully</p>";
            } else {
                echo "<p>❌ Failed to create test message</p>";
            }
        }
        
        // Test the API endpoint
        echo "<p>4. Testing get_group_messages.php API...</p>";
        $api_url = "http://192.168.101.6/BoardEase2/get_group_messages.php?group_id=" . $latest_group['gc_id'] . "&current_user_id=" . $latest_group['gc_created_by'];
        echo "<p>API URL: " . $api_url . "</p>";
        
        $response = file_get_contents($api_url);
        if ($response) {
            $data = json_decode($response, true);
            if ($data && $data['success']) {
                echo "<p>✅ API working - Found " . $data['data']['total_count'] . " messages</p>";
                echo "<pre>" . json_encode($data, JSON_PRETTY_PRINT) . "</pre>";
            } else {
                echo "<p>❌ API error: " . ($data['message'] ?? 'Unknown error') . "</p>";
            }
        } else {
            echo "<p>❌ Failed to call API</p>";
        }
        
    } else {
        echo "<p>❌ No group chats found</p>";
    }
    
} catch (Exception $e) {
    echo "<p>❌ Error: " . $e->getMessage() . "</p>";
    echo "<p>Stack trace: " . $e->getTraceAsString() . "</p>";
}
?>




