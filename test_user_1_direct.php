<?php
// Direct test for User 1 (Namz Baer) - most likely your app's user
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $db = getDB();
    
    echo "=== TESTING USER 1 (NAMZ BAER) - LIKELY YOUR APP'S USER ===\n\n";
    
    // Test chat list for user 1 directly
    echo "1. CHAT LIST FOR USER 1 (NAMZ BAER):\n";
    
    // Simple approach: Get all messages for this user first
    $stmt = $db->prepare("
        SELECT 
            m.message_id,
            m.sender_id,
            m.receiver_id,
            m.msg_text,
            m.msg_timestamp,
            m.msg_status,
            CASE 
                WHEN m.sender_id = 1 THEN m.receiver_id
                ELSE m.sender_id
            END as other_user_id
        FROM messages m
        WHERE (m.sender_id = 1 OR m.receiver_id = 1) 
        AND m.msg_status != 'Deleted'
        ORDER BY m.msg_timestamp DESC
    ");
    
    $stmt->execute();
    $all_messages = $stmt->fetchAll();
    
    echo "Found " . count($all_messages) . " total messages for user 1\n\n";
    
    // Group messages by other_user_id and get the latest message for each
    $chats = [];
    $processed_users = [];
    
    foreach ($all_messages as $message) {
        $other_user_id = $message['other_user_id'];
        
        // Skip if we already processed this user
        if (in_array($other_user_id, $processed_users)) {
            continue;
        }
        
        $processed_users[] = $other_user_id;
        
        // Get user info for this other user
        $user_stmt = $db->prepare("
            SELECT 
                u.user_id,
                u.status as user_status,
                r.f_name,
                r.l_name,
                r.role as user_type,
                r.status as reg_status
            FROM users u
            JOIN registrations r ON u.reg_id = r.reg_id
            WHERE u.user_id = ?
        ");
        $user_stmt->execute([$other_user_id]);
        $user_info = $user_stmt->fetch();
        
        if (!$user_info) {
            continue; // Skip if user not found
        }
        
        // Get unread count for this conversation
        $unread_stmt = $db->prepare("
            SELECT COUNT(*) as unread_count
            FROM messages
            WHERE sender_id = ? AND receiver_id = 1 AND msg_status NOT IN ('Read', 'Deleted')
        ");
        $unread_stmt->execute([$other_user_id]);
        $unread_count = $unread_stmt->fetch()['unread_count'];
        
        // Format the last message
        $last_message = $message['msg_text'];
        if ($message['sender_id'] == 1) {
            $last_message = "You: " . $last_message;
        }
        
        // Truncate long messages
        if (strlen($last_message) > 50) {
            $last_message = substr($last_message, 0, 47) . "...";
        }
        
        $chats[] = [
            'chat_id' => 'individual_' . $other_user_id,
            'chat_type' => 'individual',
            'other_user_id' => $other_user_id,
            'other_user_name' => $user_info['f_name'] . ' ' . $user_info['l_name'],
            'other_user_type' => $user_info['user_type'],
            'user_status' => $user_info['user_status'],
            'reg_status' => $user_info['reg_status'],
            'last_message' => $last_message,
            'last_message_time' => date('g:i A', strtotime($message['msg_timestamp'])),
            'last_message_status' => $message['msg_status'],
            'unread_count' => (int)$unread_count
        ];
    }
    
    echo "Found " . count($chats) . " conversations for user 1:\n\n";
    
    foreach ($chats as $chat) {
        echo "Chat with {$chat['other_user_name']}:\n";
        echo "- User ID: {$chat['other_user_id']}\n";
        echo "- Last message: '{$chat['last_message']}'\n";
        echo "- Time: {$chat['last_message_time']}\n";
        echo "- Unread count: {$chat['unread_count']}\n";
        echo "- Status: {$chat['user_status']} / {$chat['reg_status']}\n\n";
    }
    
    // Check if this matches your app
    echo "2. COMPARING WITH YOUR APP:\n";
    echo "Your app shows:\n";
    echo "- David Brown (10:13 PM)\n";
    echo "- Namz Baer (8:27 PM)\n";
    echo "- John Doe (6:57 PM)\n";
    echo "- BH CUAS Chat (6:47 PM)\n\n";
    
    echo "User 1's chats:\n";
    foreach ($chats as $chat) {
        echo "- {$chat['other_user_name']} ({$chat['last_message_time']})\n";
    }
    
    // Check for matches
    $app_names = ['David Brown', 'Namz Baer', 'John Doe'];
    $found_names = array_column($chats, 'other_user_name');
    $matches = array_intersect($found_names, $app_names);
    
    if (count($matches) > 0) {
        echo "\n🎯 MATCH FOUND! User 1 has conversations with: " . implode(', ', $matches) . "\n";
        echo "This confirms that your Android app is using User ID 1 (Namz Baer)!\n";
    } else {
        echo "\n❌ No direct matches found. Let's check other users.\n";
    }
    
    echo "\n=== TEST COMPLETE ===\n";
    
} catch (Exception $e) {
    echo "ERROR: " . $e->getMessage() . "\n";
}
?>








