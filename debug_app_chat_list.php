<?php
// Debug what your Android app is actually calling
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $db = getDB();
    
    echo "=== DEBUGGING YOUR ANDROID APP'S CHAT LIST ===\n\n";
    
    // 1. Check what user ID your app is using
    echo "1. CHECKING RECENT API CALLS:\n";
    echo "Your app is probably calling: http://your-server/get_chat_list.php?user_id=[SOME_USER_ID]\n";
    echo "We need to find out which user ID your app is using.\n\n";
    
    // 2. Check all users and their recent messages
    echo "2. ALL USERS AND THEIR RECENT MESSAGES:\n";
    $stmt = $db->prepare("
        SELECT 
            u.user_id,
            r.f_name,
            r.l_name,
            r.role,
            (SELECT COUNT(*) FROM messages WHERE sender_id = u.user_id OR receiver_id = u.user_id) as message_count,
            (SELECT MAX(msg_timestamp) FROM messages WHERE sender_id = u.user_id OR receiver_id = u.user_id) as last_message_time
        FROM users u
        JOIN registrations r ON u.reg_id = r.reg_id
        ORDER BY last_message_time DESC
    ");
    $stmt->execute();
    $users = $stmt->fetchAll();
    
    foreach ($users as $user) {
        echo "- User {$user['user_id']}: {$user['f_name']} {$user['l_name']} ({$user['role']}) | Messages: {$user['message_count']} | Last: {$user['last_message_time']}\n";
    }
    
    // 3. Check messages that match the names in your app
    echo "\n3. CHECKING MESSAGES FOR NAMES IN YOUR APP:\n";
    
    // Check for David Brown
    echo "\n3.1 DAVID BROWN:\n";
    $stmt = $db->prepare("
        SELECT 
            m.message_id,
            m.sender_id,
            m.receiver_id,
            m.msg_text,
            m.msg_timestamp,
            m.msg_status,
            sender.f_name as sender_fname,
            sender.l_name as sender_lname,
            receiver.f_name as receiver_fname,
            receiver.l_name as receiver_lname
        FROM messages m
        JOIN users sender ON m.sender_id = sender.user_id
        JOIN registrations sender_reg ON sender.reg_id = sender_reg.reg_id
        JOIN users receiver ON m.receiver_id = receiver.user_id
        JOIN registrations receiver_reg ON receiver.reg_id = receiver_reg.reg_id
        WHERE (sender_reg.f_name = 'David' AND sender_reg.l_name = 'Brown')
           OR (receiver_reg.f_name = 'David' AND receiver_reg.l_name = 'Brown')
        ORDER BY m.msg_timestamp DESC
        LIMIT 5
    ");
    $stmt->execute();
    $david_messages = $stmt->fetchAll();
    
    if (count($david_messages) > 0) {
        echo "Found " . count($david_messages) . " messages involving David Brown:\n";
        foreach ($david_messages as $msg) {
            echo "- ID {$msg['message_id']}: {$msg['sender_fname']} {$msg['sender_lname']} -> {$msg['receiver_fname']} {$msg['receiver_lname']} | Time: {$msg['msg_timestamp']} | Text: " . substr($msg['msg_text'], 0, 50) . "...\n";
        }
    } else {
        echo "No messages found for David Brown\n";
    }
    
    // Check for Namz Baer
    echo "\n3.2 NAMZ BAER:\n";
    $stmt = $db->prepare("
        SELECT 
            m.message_id,
            m.sender_id,
            m.receiver_id,
            m.msg_text,
            m.msg_timestamp,
            m.msg_status,
            sender.f_name as sender_fname,
            sender.l_name as sender_lname,
            receiver.f_name as receiver_fname,
            receiver.l_name as receiver_lname
        FROM messages m
        JOIN users sender ON m.sender_id = sender.user_id
        JOIN registrations sender_reg ON sender.reg_id = sender_reg.reg_id
        JOIN users receiver ON m.receiver_id = receiver.user_id
        JOIN registrations receiver_reg ON receiver.reg_id = receiver_reg.reg_id
        WHERE (sender_reg.f_name = 'Namz' AND sender_reg.l_name = 'Baer')
           OR (receiver_reg.f_name = 'Namz' AND receiver_reg.l_name = 'Baer')
        ORDER BY m.msg_timestamp DESC
        LIMIT 5
    ");
    $stmt->execute();
    $namz_messages = $stmt->fetchAll();
    
    if (count($namz_messages) > 0) {
        echo "Found " . count($namz_messages) . " messages involving Namz Baer:\n";
        foreach ($namz_messages as $msg) {
            echo "- ID {$msg['message_id']}: {$msg['sender_fname']} {$msg['sender_lname']} -> {$msg['receiver_fname']} {$msg['receiver_lname']} | Time: {$msg['msg_timestamp']} | Text: " . substr($msg['msg_text'], 0, 50) . "...\n";
        }
    } else {
        echo "No messages found for Namz Baer\n";
    }
    
    // 4. Check for John Doe
    echo "\n3.3 JOHN DOE:\n";
    $stmt = $db->prepare("
        SELECT 
            m.message_id,
            m.sender_id,
            m.receiver_id,
            m.msg_text,
            m.msg_timestamp,
            m.msg_status,
            sender.f_name as sender_fname,
            sender.l_name as sender_lname,
            receiver.f_name as receiver_fname,
            receiver.l_name as receiver_lname
        FROM messages m
        JOIN users sender ON m.sender_id = sender.user_id
        JOIN registrations sender_reg ON sender.reg_id = sender_reg.reg_id
        JOIN users receiver ON m.receiver_id = receiver.user_id
        JOIN registrations receiver_reg ON receiver.reg_id = receiver_reg.reg_id
        WHERE (sender_reg.f_name = 'John' AND sender_reg.l_name = 'Doe')
           OR (receiver_reg.f_name = 'John' AND receiver_reg.l_name = 'Doe')
        ORDER BY m.msg_timestamp DESC
        LIMIT 5
    ");
    $stmt->execute();
    $john_messages = $stmt->fetchAll();
    
    if (count($john_messages) > 0) {
        echo "Found " . count($john_messages) . " messages involving John Doe:\n";
        foreach ($john_messages as $msg) {
            echo "- ID {$msg['message_id']}: {$msg['sender_fname']} {$msg['sender_lname']} -> {$msg['receiver_fname']} {$msg['receiver_lname']} | Time: {$msg['msg_timestamp']} | Text: " . substr($msg['msg_text'], 0, 50) . "...\n";
        }
    } else {
        echo "No messages found for John Doe\n";
    }
    
    // 5. Check for group chats
    echo "\n4. CHECKING GROUP CHATS:\n";
    $stmt = $db->prepare("
        SELECT 
            cg.gc_id,
            cg.gc_name,
            cg.gc_created_by,
            MAX(gm.groupmessage_timestamp) as last_message_time,
            (SELECT groupmessage_text FROM group_messages WHERE gc_id = cg.gc_id ORDER BY groupmessage_timestamp DESC LIMIT 1) as last_message
        FROM chat_groups cg
        LEFT JOIN group_messages gm ON cg.gc_id = gm.gc_id
        GROUP BY cg.gc_id, cg.gc_name, cg.gc_created_by
        ORDER BY last_message_time DESC
    ");
    $stmt->execute();
    $groups = $stmt->fetchAll();
    
    echo "Found " . count($groups) . " group chats:\n";
    foreach ($groups as $group) {
        echo "- Group ID {$group['gc_id']}: {$group['gc_name']} | Last: '{$group['last_message']}' | Time: {$group['last_message_time']}\n";
    }
    
    // 6. Test different user IDs to see which one matches your app
    echo "\n5. TESTING DIFFERENT USER IDs TO MATCH YOUR APP:\n";
    echo "Let's test a few user IDs to see which one gives us the same results as your app:\n\n";
    
    $test_user_ids = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11];
    
    foreach ($test_user_ids as $user_id) {
        echo "Testing user ID $user_id:\n";
        $url = "http://localhost/get_chat_list_simple.php?user_id=$user_id";
        $response = file_get_contents($url);
        $data = json_decode($response, true);
        
        if ($data && $data['success'] && count($data['data']['chats']) > 0) {
            echo "✅ User $user_id has " . count($data['data']['chats']) . " chats:\n";
            foreach ($data['data']['chats'] as $chat) {
                echo "  - {$chat['other_user_name']} | Last: '{$chat['last_message']}' | Time: {$chat['last_message_time']}\n";
            }
            echo "\n";
        } else {
            echo "❌ User $user_id has no chats or error\n\n";
        }
    }
    
    echo "\n=== DEBUG COMPLETE ===\n";
    echo "Look for the user ID that shows the same names as in your app!\n";
    
} catch (Exception $e) {
    echo "ERROR: " . $e->getMessage() . "\n";
}
?>








