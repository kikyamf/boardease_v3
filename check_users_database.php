<?php
// Check what users exist in the database
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $db = getDB();
    
    echo "=== CHECKING USERS IN DATABASE ===\n\n";
    
    // 1. Check all users in the database
    echo "1. ALL USERS IN DATABASE:\n";
    $stmt = $db->prepare("
        SELECT 
            u.user_id,
            u.status as user_status,
            r.f_name,
            r.l_name,
            r.status as reg_status,
            r.role
        FROM users u
        JOIN registrations r ON u.reg_id = r.reg_id
        ORDER BY u.user_id
    ");
    $stmt->execute();
    $users = $stmt->fetchAll();
    
    echo "Found " . count($users) . " users:\n";
    foreach ($users as $user) {
        echo "- User {$user['user_id']}: {$user['f_name']} {$user['l_name']} | Role: {$user['role']} | User Status: {$user['user_status']} | Reg Status: {$user['reg_status']}\n";
    }
    
    // 2. Check if specific users exist
    echo "\n2. CHECKING SPECIFIC USERS (4, 11, 76):\n";
    $user_ids = [4, 11, 76];
    
    foreach ($user_ids as $user_id) {
        $stmt = $db->prepare("
            SELECT 
                u.user_id,
                u.status as user_status,
                r.f_name,
                r.l_name,
                r.status as reg_status,
                r.role
            FROM users u
            JOIN registrations r ON u.reg_id = r.reg_id
            WHERE u.user_id = ?
        ");
        $stmt->execute([$user_id]);
        $user = $stmt->fetch();
        
        if ($user) {
            echo "✅ User $user_id exists: {$user['f_name']} {$user['l_name']} | Role: {$user['role']} | User Status: {$user['user_status']} | Reg Status: {$user['reg_status']}\n";
        } else {
            echo "❌ User $user_id does NOT exist in database\n";
        }
    }
    
    // 3. Check messages and see which users are actually involved
    echo "\n3. CHECKING MESSAGES AND INVOLVED USERS:\n";
    $stmt = $db->prepare("
        SELECT DISTINCT sender_id, receiver_id
        FROM messages
        ORDER BY sender_id, receiver_id
    ");
    $stmt->execute();
    $message_users = $stmt->fetchAll();
    
    $involved_users = [];
    foreach ($message_users as $row) {
        $involved_users[] = $row['sender_id'];
        $involved_users[] = $row['receiver_id'];
    }
    $involved_users = array_unique($involved_users);
    
    echo "Users involved in messages: " . implode(', ', $involved_users) . "\n";
    
    // 4. Show recent messages
    echo "\n4. RECENT MESSAGES:\n";
    $stmt = $db->prepare("
        SELECT 
            message_id,
            sender_id,
            receiver_id,
            msg_text,
            msg_status,
            msg_timestamp
        FROM messages 
        ORDER BY msg_timestamp DESC
        LIMIT 10
    ");
    $stmt->execute();
    $recent_messages = $stmt->fetchAll();
    
    echo "Recent " . count($recent_messages) . " messages:\n";
    foreach ($recent_messages as $msg) {
        echo "- ID {$msg['message_id']}: {$msg['sender_id']} -> {$msg['receiver_id']} | Status: '{$msg['msg_status']}' | Time: {$msg['msg_timestamp']} | Text: " . substr($msg['msg_text'], 0, 30) . "...\n";
    }
    
    // 5. Suggest which user to use for testing
    echo "\n5. SUGGESTION FOR TESTING:\n";
    if (count($users) > 0) {
        $first_user = $users[0];
        $second_user = count($users) > 1 ? $users[1] : $users[0];
        
        echo "✅ Use these existing users for testing:\n";
        echo "- User {$first_user['user_id']}: {$first_user['f_name']} {$first_user['l_name']}\n";
        echo "- User {$second_user['user_id']}: {$second_user['f_name']} {$second_user['l_name']}\n";
        echo "\nTest URL: http://your-server/get_chat_list_simple.php?user_id={$first_user['user_id']}\n";
    }
    
    echo "\n=== CHECK COMPLETE ===\n";
    
} catch (Exception $e) {
    echo "ERROR: " . $e->getMessage() . "\n";
}
?>








