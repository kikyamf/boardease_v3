<?php
// Test script to debug chat system issues
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $user_id = $_GET['user_id'] ?? 1;
    
    $db = getDB();
    
    echo "=== TESTING CHAT SYSTEM FOR USER $user_id ===\n\n";
    
    // 1. Check recent messages
    echo "1. RECENT MESSAGES:\n";
    $stmt = $db->prepare("
        SELECT 
            message_id,
            sender_id,
            receiver_id,
            msg_text,
            msg_status,
            msg_timestamp
        FROM messages 
        WHERE sender_id = ? OR receiver_id = ?
        ORDER BY msg_timestamp DESC
        LIMIT 10
    ");
    $stmt->execute([$user_id, $user_id]);
    $messages = $stmt->fetchAll();
    
    foreach ($messages as $msg) {
        echo "ID {$msg['message_id']}: {$msg['sender_id']} -> {$msg['receiver_id']} | Status: '{$msg['msg_status']}' | Time: {$msg['msg_timestamp']} | Text: " . substr($msg['msg_text'], 0, 30) . "...\n";
    }
    
    // 2. Check unread count
    echo "\n2. UNREAD COUNT:\n";
    $stmt = $db->prepare("
        SELECT COUNT(*) as unread_count 
        FROM messages 
        WHERE receiver_id = ? AND msg_status NOT IN ('Read', 'Deleted')
    ");
    $stmt->execute([$user_id]);
    $unread_count = $stmt->fetch()['unread_count'];
    echo "Unread messages: $unread_count\n";
    
    // 3. Test chat list query
    echo "\n3. CHAT LIST QUERY TEST:\n";
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
                    WHEN m.sender_id = ? THEN m.receiver_id
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
                (SELECT COUNT(*) FROM messages WHERE receiver_id = ? AND sender_id = other_user_id AND msg_status NOT IN ('Read', 'Deleted')) as unread_count
            FROM messages m
            JOIN users u ON (
                CASE 
                    WHEN m.sender_id = ? THEN m.receiver_id
                    ELSE m.sender_id
                END = u.user_id
            )
            JOIN registration r ON u.reg_id = r.reg_id
            WHERE (m.sender_id = ? OR m.receiver_id = ?) 
            AND m.msg_status != 'Deleted'
            ORDER BY m.msg_timestamp DESC
        ) as chat_messages
        GROUP BY other_user_id
        ORDER BY last_message_time DESC
    ");
    
    $stmt->execute([$user_id, $user_id, $user_id, $user_id, $user_id]);
    $chats = $stmt->fetchAll();
    
    echo "Found " . count($chats) . " chats:\n";
    foreach ($chats as $chat) {
        echo "- User {$chat['other_user_id']}: {$chat['first_name']} {$chat['last_name']} | Last: '{$chat['last_message']}' | Time: {$chat['last_message_time']} | Unread: {$chat['unread_count']}\n";
    }
    
    // 4. Test API endpoints
    echo "\n4. TESTING API ENDPOINTS:\n";
    
    // Test get_chat_list.php
    $chat_list_url = "http://localhost/get_chat_list.php?user_id=$user_id";
    echo "Chat list URL: $chat_list_url\n";
    
    // Test get_unread_count.php
    $unread_url = "http://localhost/get_unread_count.php?user_id=$user_id";
    echo "Unread count URL: $unread_url\n";
    
    echo "\n=== TEST COMPLETE ===\n";
    
} catch (Exception $e) {
    echo "ERROR: " . $e->getMessage() . "\n";
}
?>


















