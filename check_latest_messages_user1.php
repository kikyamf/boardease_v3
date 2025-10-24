<?php
// Check the latest messages for User 1 to see what's missing
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $db = getDB();
    
    echo "=== CHECKING LATEST MESSAGES FOR USER 1 ===\n\n";
    
    // 1. Get the most recent messages for user 1
    echo "1. MOST RECENT MESSAGES FOR USER 1:\n";
    $stmt = $db->prepare("
        SELECT 
            m.message_id,
            m.sender_id,
            m.receiver_id,
            m.msg_text,
            m.msg_timestamp,
            m.msg_status,
            sender_reg.f_name as sender_fname,
            sender_reg.l_name as sender_lname,
            receiver_reg.f_name as receiver_fname,
            receiver_reg.l_name as receiver_lname
        FROM messages m
        JOIN users sender ON m.sender_id = sender.user_id
        JOIN registrations sender_reg ON sender.reg_id = sender_reg.reg_id
        JOIN users receiver ON m.receiver_id = receiver.user_id
        JOIN registrations receiver_reg ON receiver.reg_id = receiver_reg.reg_id
        WHERE (m.sender_id = 1 OR m.receiver_id = 1)
        ORDER BY m.msg_timestamp DESC
        LIMIT 20
    ");
    $stmt->execute();
    $recent_messages = $stmt->fetchAll();
    
    echo "Last 20 messages involving User 1:\n";
    foreach ($recent_messages as $msg) {
        $time = date('g:i A', strtotime($msg['msg_timestamp']));
        $direction = $msg['sender_id'] == 1 ? "→" : "←";
        echo "- ID {$msg['message_id']}: {$msg['sender_fname']} {$msg['sender_lname']} $direction {$msg['receiver_fname']} {$msg['receiver_lname']} | Time: $time | Text: " . substr($msg['msg_text'], 0, 30) . "...\n";
    }
    
    // 2. Check for messages around the times your app shows
    echo "\n2. CHECKING FOR MESSAGES AROUND YOUR APP'S TIMES:\n";
    
    // Check for messages around 10:13 PM
    echo "\n2.1 MESSAGES AROUND 10:13 PM:\n";
    $stmt = $db->prepare("
        SELECT 
            m.message_id,
            m.sender_id,
            m.receiver_id,
            m.msg_text,
            m.msg_timestamp,
            sender_reg.f_name as sender_fname,
            sender_reg.l_name as sender_lname,
            receiver_reg.f_name as receiver_fname,
            receiver_reg.l_name as receiver_lname
        FROM messages m
        JOIN users sender ON m.sender_id = sender.user_id
        JOIN registrations sender_reg ON sender.reg_id = sender_reg.reg_id
        JOIN users receiver ON m.receiver_id = receiver.user_id
        JOIN registrations receiver_reg ON receiver.reg_id = receiver_reg.reg_id
        WHERE (m.sender_id = 1 OR m.receiver_id = 1)
        AND TIME(m.msg_timestamp) BETWEEN '22:00:00' AND '22:30:00'
        ORDER BY m.msg_timestamp DESC
    ");
    $stmt->execute();
    $evening_messages = $stmt->fetchAll();
    
    if (count($evening_messages) > 0) {
        echo "Found " . count($evening_messages) . " messages around 10:13 PM:\n";
        foreach ($evening_messages as $msg) {
            $time = date('g:i A', strtotime($msg['msg_timestamp']));
            echo "- ID {$msg['message_id']}: {$msg['sender_fname']} {$msg['sender_lname']} -> {$msg['receiver_fname']} {$msg['receiver_lname']} | Time: $time | Text: " . substr($msg['msg_text'], 0, 30) . "...\n";
        }
    } else {
        echo "No messages found around 10:13 PM\n";
    }
    
    // Check for messages around 8:27 PM
    echo "\n2.2 MESSAGES AROUND 8:27 PM:\n";
    $stmt = $db->prepare("
        SELECT 
            m.message_id,
            m.sender_id,
            m.receiver_id,
            m.msg_text,
            m.msg_timestamp,
            sender_reg.f_name as sender_fname,
            sender_reg.l_name as sender_lname,
            receiver_reg.f_name as receiver_fname,
            receiver_reg.l_name as receiver_lname
        FROM messages m
        JOIN users sender ON m.sender_id = sender.user_id
        JOIN registrations sender_reg ON sender.reg_id = sender_reg.reg_id
        JOIN users receiver ON m.receiver_id = receiver.user_id
        JOIN registrations receiver_reg ON receiver.reg_id = receiver_reg.reg_id
        WHERE (m.sender_id = 1 OR m.receiver_id = 1)
        AND TIME(m.msg_timestamp) BETWEEN '20:00:00' AND '20:30:00'
        ORDER BY m.msg_timestamp DESC
    ");
    $stmt->execute();
    $evening_messages = $stmt->fetchAll();
    
    if (count($evening_messages) > 0) {
        echo "Found " . count($evening_messages) . " messages around 8:27 PM:\n";
        foreach ($evening_messages as $msg) {
            $time = date('g:i A', strtotime($msg['msg_timestamp']));
            echo "- ID {$msg['message_id']}: {$msg['sender_fname']} {$msg['sender_lname']} -> {$msg['receiver_fname']} {$msg['receiver_lname']} | Time: $time | Text: " . substr($msg['msg_text'], 0, 30) . "...\n";
        }
    } else {
        echo "No messages found around 8:27 PM\n";
    }
    
    // 3. Test the original get_chat_list.php API
    echo "\n3. TESTING ORIGINAL API:\n";
    echo "Testing: http://localhost/get_chat_list.php?user_id=1\n";
    
    // Simulate the API call
    $stmt = $db->prepare("
        SELECT 
            other_user_id,
            first_name,
            last_name,
            user_type,
            user_status,
            last_message,
            last_message_time,
            last_message_status,
            last_sender_id,
            unread_count
        FROM (
            SELECT 
                CASE 
                    WHEN m.sender_id = 1 THEN m.receiver_id
                    ELSE m.sender_id
                END as other_user_id,
                r.f_name as first_name,
                r.l_name as last_name,
                r.role as user_type,
                u.status as user_status,
                m.msg_text as last_message,
                m.msg_timestamp as last_message_time,
                m.msg_status as last_message_status,
                m.sender_id as last_sender_id,
                (SELECT COUNT(*) FROM messages WHERE receiver_id = 1 AND sender_id = other_user_id AND msg_status NOT IN ('Read', 'Deleted')) as unread_count
            FROM messages m
            JOIN users u ON (
                CASE 
                    WHEN m.sender_id = 1 THEN m.receiver_id
                    ELSE m.sender_id
                END = u.user_id
            )
            JOIN registrations r ON u.reg_id = r.reg_id
            WHERE (m.sender_id = 1 OR m.receiver_id = 1) 
            AND m.msg_status != 'Deleted'
            ORDER BY m.msg_timestamp DESC
        ) as chat_messages
        GROUP BY other_user_id
        ORDER BY last_message_time DESC
    ");
    $stmt->execute();
    $api_chats = $stmt->fetchAll();
    
    echo "Original API results:\n";
    foreach ($api_chats as $chat) {
        $time = date('g:i A', strtotime($chat['last_message_time']));
        echo "- {$chat['first_name']} {$chat['last_name']} | Last: '{$chat['last_message']}' | Time: $time | Unread: {$chat['unread_count']}\n";
    }
    
    echo "\n=== ANALYSIS ===\n";
    echo "If the times still don't match, your app might be:\n";
    echo "1. Using cached data\n";
    echo "2. Using a different API endpoint\n";
    echo "3. Showing different timezone\n";
    echo "4. Using mock/test data\n";
    
    echo "\n=== TEST COMPLETE ===\n";
    
} catch (Exception $e) {
    echo "ERROR: " . $e->getMessage() . "\n";
}
?>








