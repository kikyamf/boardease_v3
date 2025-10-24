<?php
// Check what user ID your Android app is using
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $db = getDB();
    
    echo "=== FINDING YOUR APP'S USER ID ===\n\n";
    
    // 1. Check recent messages to see which user is most active
    echo "1. RECENT MESSAGES (last 20):\n";
    $stmt = $db->prepare("
        SELECT 
            m.message_id,
            m.sender_id,
            m.receiver_id,
            m.msg_text,
            m.msg_timestamp,
            sender.f_name as sender_fname,
            sender.l_name as sender_lname,
            receiver.f_name as receiver_fname,
            receiver.l_name as receiver_lname
        FROM messages m
        JOIN users sender ON m.sender_id = sender.user_id
        JOIN registrations sender_reg ON sender.reg_id = sender_reg.reg_id
        JOIN users receiver ON m.receiver_id = receiver.user_id
        JOIN registrations receiver_reg ON receiver.reg_id = receiver_reg.reg_id
        ORDER BY m.msg_timestamp DESC
        LIMIT 20
    ");
    $stmt->execute();
    $recent_messages = $stmt->fetchAll();
    
    foreach ($recent_messages as $msg) {
        $time = date('g:i A', strtotime($msg['msg_timestamp']));
        echo "- {$msg['sender_fname']} {$msg['sender_lname']} -> {$msg['receiver_fname']} {$msg['receiver_lname']} | Time: $time | Text: " . substr($msg['msg_text'], 0, 40) . "...\n";
    }
    
    // 2. Find which user has the most conversations
    echo "\n2. USERS WITH MOST CONVERSATIONS:\n";
    $stmt = $db->prepare("
        SELECT 
            user_id,
            COUNT(DISTINCT other_user_id) as conversation_count,
            MAX(last_message_time) as last_activity
        FROM (
            SELECT 
                sender_id as user_id,
                receiver_id as other_user_id,
                MAX(msg_timestamp) as last_message_time
            FROM messages
            GROUP BY sender_id, receiver_id
            
            UNION ALL
            
            SELECT 
                receiver_id as user_id,
                sender_id as other_user_id,
                MAX(msg_timestamp) as last_message_time
            FROM messages
            GROUP BY receiver_id, sender_id
        ) as conversations
        GROUP BY user_id
        ORDER BY conversation_count DESC, last_activity DESC
    ");
    $stmt->execute();
    $active_users = $stmt->fetchAll();
    
    foreach ($active_users as $user) {
        echo "- User {$user['user_id']}: {$user['conversation_count']} conversations | Last activity: {$user['last_activity']}\n";
    }
    
    // 3. Test the most likely user IDs
    echo "\n3. TESTING MOST LIKELY USER IDs:\n";
    
    $likely_users = array_slice($active_users, 0, 5); // Top 5 most active users
    
    foreach ($likely_users as $user_data) {
        $user_id = $user_data['user_id'];
        echo "\nTesting User ID $user_id:\n";
        
        $url = "http://localhost/get_chat_list_simple.php?user_id=$user_id";
        $response = file_get_contents($url);
        $data = json_decode($response, true);
        
        if ($data && $data['success']) {
            $chats = $data['data']['chats'];
            echo "Found " . count($chats) . " chats:\n";
            
            $found_names = [];
            foreach ($chats as $chat) {
                $found_names[] = $chat['other_user_name'];
                echo "  - {$chat['other_user_name']} | Last: '{$chat['last_message']}' | Time: {$chat['last_message_time']}\n";
            }
            
            // Check if this matches your app
            $app_names = ['David Brown', 'Namz Baer', 'John Doe'];
            $matches = array_intersect($found_names, $app_names);
            
            if (count($matches) > 0) {
                echo "🎯 MATCH FOUND! User ID $user_id has conversations with: " . implode(', ', $matches) . "\n";
                echo "This might be the user ID your app is using!\n";
            }
        }
    }
    
    // 4. Check what user is currently logged in (if stored in shared preferences)
    echo "\n4. CHECKING FOR CURRENT USER INDICATORS:\n";
    echo "Your Android app might be using a specific user ID stored in SharedPreferences.\n";
    echo "Common user IDs to check: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11\n";
    echo "Or check your app's login/logout functionality to see which user ID is being used.\n";
    
    echo "\n=== DEBUG COMPLETE ===\n";
    echo "Look for the user ID that shows the same names as in your app!\n";
    
} catch (Exception $e) {
    echo "ERROR: " . $e->getMessage() . "\n";
}
?>








