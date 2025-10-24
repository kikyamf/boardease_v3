<?php
// Test chat sorting and real-time timestamps
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $db = getDB();
    
    echo "=== TESTING CHAT SORTING AND REAL-TIME TIMESTAMPS ===\n\n";
    
    // 1. Check current chat list sorting
    echo "1. CURRENT CHAT LIST SORTING:\n";
    $stmt = $db->prepare("
        SELECT 
            other_user_id,
            first_name,
            last_name,
            last_message,
            last_message_time,
            last_sender_id
        FROM (
            SELECT 
                CASE 
                    WHEN m.sender_id = 1 THEN m.receiver_id
                    ELSE m.sender_id
                END as other_user_id,
                r.f_name as first_name,
                r.l_name as last_name,
                m.msg_text as last_message,
                m.msg_timestamp as last_message_time,
                m.sender_id as last_sender_id
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
    $chats = $stmt->fetchAll();
    
    echo "Chats sorted by last message time:\n";
    foreach ($chats as $chat) {
        $time = date('g:i A', strtotime($chat['last_message_time']));
        $sender = $chat['last_sender_id'] == 1 ? "You" : $chat['first_name'];
        echo "- {$chat['first_name']} {$chat['last_name']} | Last: '$sender: {$chat['last_message']}' | Time: $time\n";
    }
    
    // 2. Send a new test message to see if it appears at the top
    echo "\n2. SENDING NEW TEST MESSAGE:\n";
    
    // Send message from user 1 to user 6 (David Brown)
    $stmt = $db->prepare("
        INSERT INTO messages (sender_id, receiver_id, msg_text, msg_timestamp, msg_status) 
        VALUES (1, 6, 'NEW TEST MESSAGE - This should appear at the top!', NOW(), 'Sent')
    ");
    
    if ($stmt->execute()) {
        $message_id = $db->lastInsertId();
        echo "✅ Sent new message ID: $message_id (User 1 -> User 6)\n";
        echo "Message: 'NEW TEST MESSAGE - This should appear at the top!'\n";
        echo "Time: " . date('Y-m-d H:i:s') . "\n";
        
        // 3. Check chat list again
        echo "\n3. CHAT LIST AFTER NEW MESSAGE:\n";
        $stmt = $db->prepare("
            SELECT 
                other_user_id,
                first_name,
                last_name,
                last_message,
                last_message_time,
                last_sender_id
            FROM (
                SELECT 
                    CASE 
                        WHEN m.sender_id = 1 THEN m.receiver_id
                        ELSE m.sender_id
                    END as other_user_id,
                    r.f_name as first_name,
                    r.l_name as last_name,
                    m.msg_text as last_message,
                    m.msg_timestamp as last_message_time,
                    m.sender_id as last_sender_id
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
        $updated_chats = $stmt->fetchAll();
        
        echo "Updated chat list:\n";
        foreach ($updated_chats as $chat) {
            $time = date('g:i A', strtotime($chat['last_message_time']));
            $sender = $chat['last_sender_id'] == 1 ? "You" : $chat['first_name'];
            echo "- {$chat['first_name']} {$chat['last_name']} | Last: '$sender: {$chat['last_message']}' | Time: $time\n";
        }
        
        // 4. Test the API response
        echo "\n4. TESTING API RESPONSE:\n";
        $url = "http://localhost/get_chat_list.php?user_id=1";
        echo "URL: $url\n";
        $response = file_get_contents($url);
        $data = json_decode($response, true);
        
        if ($data && $data['success']) {
            echo "API Response:\n";
            foreach ($data['data']['chats'] as $chat) {
                echo "- {$chat['other_user_name']} | Last: '{$chat['last_message']}' | Time: {$chat['last_message_time']}\n";
            }
        }
        
    } else {
        echo "❌ Failed to send new message\n";
    }
    
    // 5. Check real-time timestamp formatting
    echo "\n5. REAL-TIME TIMESTAMP TESTING:\n";
    $current_time = date('Y-m-d H:i:s');
    echo "Current server time: $current_time\n";
    echo "Formatted time (12-hour): " . date('g:i A', strtotime($current_time)) . "\n";
    echo "Formatted time (24-hour): " . date('H:i', strtotime($current_time)) . "\n";
    
    echo "\n=== TEST COMPLETE ===\n";
    
} catch (Exception $e) {
    echo "ERROR: " . $e->getMessage() . "\n";
}
?>








