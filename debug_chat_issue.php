<?php
// Debug script for chat visibility issues
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $db = getDB();
    
    echo "=== DEBUGGING CHAT VISIBILITY ISSUE ===\n\n";
    
    // 1. Check messages between users 76, 4, 11
    echo "1. CHECKING MESSAGES BETWEEN USERS 76, 4, 11:\n";
    $stmt = $db->prepare("
        SELECT 
            message_id,
            sender_id,
            receiver_id,
            msg_text,
            msg_status,
            msg_timestamp
        FROM messages 
        WHERE (sender_id IN (76, 4, 11) AND receiver_id IN (76, 4, 11))
        ORDER BY msg_timestamp DESC
    ");
    $stmt->execute();
    $messages = $stmt->fetchAll();
    
    echo "Found " . count($messages) . " messages:\n";
    foreach ($messages as $msg) {
        echo "- ID {$msg['message_id']}: {$msg['sender_id']} -> {$msg['receiver_id']} | Status: '{$msg['msg_status']}' | Time: {$msg['msg_timestamp']} | Text: " . substr($msg['msg_text'], 0, 50) . "...\n";
    }
    
    // 2. Check user status for these users
    echo "\n2. CHECKING USER STATUS:\n";
    $stmt = $db->prepare("
        SELECT 
            u.user_id,
            u.status as user_status,
            r.f_name,
            r.l_name,
            r.status as reg_status
        FROM users u
        JOIN registration r ON u.reg_id = r.reg_id
        WHERE u.user_id IN (76, 4, 11)
    ");
    $stmt->execute();
    $users = $stmt->fetchAll();
    
    foreach ($users as $user) {
        echo "- User {$user['user_id']}: {$user['f_name']} {$user['l_name']} | User Status: '{$user['user_status']}' | Reg Status: '{$user['reg_status']}'\n";
    }
    
    // 3. Test chat list query for user 76
    echo "\n3. TESTING CHAT LIST FOR USER 76:\n";
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
                    WHEN m.sender_id = 76 THEN m.receiver_id
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
                (SELECT COUNT(*) FROM messages WHERE receiver_id = 76 AND sender_id = other_user_id AND msg_status NOT IN ('Read', 'Deleted')) as unread_count
            FROM messages m
            JOIN users u ON (
                CASE 
                    WHEN m.sender_id = 76 THEN m.receiver_id
                    ELSE m.sender_id
                END = u.user_id
            )
            JOIN registration r ON u.reg_id = r.reg_id
            WHERE (m.sender_id = 76 OR m.receiver_id = 76) 
            AND m.msg_status != 'Deleted'
            ORDER BY m.msg_timestamp DESC
        ) as chat_messages
        GROUP BY other_user_id
        ORDER BY last_message_time DESC
    ");
    $stmt->execute();
    $chats = $stmt->fetchAll();
    
    echo "Found " . count($chats) . " chats for user 76:\n";
    foreach ($chats as $chat) {
        echo "- User {$chat['other_user_id']}: {$chat['first_name']} {$chat['last_name']} | Last: '{$chat['last_message']}' | Time: {$chat['last_message_time']} | Unread: {$chat['unread_count']}\n";
    }
    
    // 4. Test chat list query for user 4
    echo "\n4. TESTING CHAT LIST FOR USER 4:\n";
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
                    WHEN m.sender_id = 4 THEN m.receiver_id
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
                (SELECT COUNT(*) FROM messages WHERE receiver_id = 4 AND sender_id = other_user_id AND msg_status NOT IN ('Read', 'Deleted')) as unread_count
            FROM messages m
            JOIN users u ON (
                CASE 
                    WHEN m.sender_id = 4 THEN m.receiver_id
                    ELSE m.sender_id
                END = u.user_id
            )
            JOIN registration r ON u.reg_id = r.reg_id
            WHERE (m.sender_id = 4 OR m.receiver_id = 4) 
            AND m.msg_status != 'Deleted'
            ORDER BY m.msg_timestamp DESC
        ) as chat_messages
        GROUP BY other_user_id
        ORDER BY last_message_time DESC
    ");
    $stmt->execute();
    $chats = $stmt->fetchAll();
    
    echo "Found " . count($chats) . " chats for user 4:\n";
    foreach ($chats as $chat) {
        echo "- User {$chat['other_user_id']}: {$chat['first_name']} {$chat['last_name']} | Last: '{$chat['last_message']}' | Time: {$chat['last_message_time']} | Unread: {$chat['unread_count']}\n";
    }
    
    // 5. Check if there are any issues with the JOIN conditions
    echo "\n5. CHECKING JOIN CONDITIONS:\n";
    $stmt = $db->prepare("
        SELECT 
            m.message_id,
            m.sender_id,
            m.receiver_id,
            m.msg_timestamp,
            u.user_id as found_user_id,
            r.f_name,
            r.l_name,
            u.status as user_status,
            r.status as reg_status
        FROM messages m
        LEFT JOIN users u ON (
            CASE 
                WHEN m.sender_id = 76 THEN m.receiver_id
                ELSE m.sender_id
            END = u.user_id
        )
        LEFT JOIN registration r ON u.reg_id = r.reg_id
        WHERE (m.sender_id = 76 OR m.receiver_id = 76)
        ORDER BY m.msg_timestamp DESC
        LIMIT 10
    ");
    $stmt->execute();
    $join_test = $stmt->fetchAll();
    
    echo "JOIN test results:\n";
    foreach ($join_test as $row) {
        $found_user = $row['found_user_id'] ? "YES (ID: {$row['found_user_id']})" : "NO";
        echo "- Message {$row['message_id']}: {$row['sender_id']} -> {$row['receiver_id']} | User found: $found_user | Name: {$row['f_name']} {$row['l_name']} | User Status: {$row['user_status']} | Reg Status: {$row['reg_status']}\n";
    }
    
    echo "\n=== DEBUG COMPLETE ===\n";
    
} catch (Exception $e) {
    echo "ERROR: " . $e->getMessage() . "\n";
}
?>


















