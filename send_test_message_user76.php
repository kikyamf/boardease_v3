<?php
// Send test message from user 76 to test chat system
error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $db = getDB();
    
    echo "=== SENDING TEST MESSAGE FROM USER 76 ===\n\n";
    
    // Send message from user 76 to user 4
    $sender_id = 76;
    $receiver_id = 4;
    $message_text = "Hello from user 76! This is a test message.";
    
    $stmt = $db->prepare("
        INSERT INTO messages (sender_id, receiver_id, msg_text, msg_timestamp, msg_status) 
        VALUES (?, ?, ?, NOW(), 'Sent')
    ");
    
    $result = $stmt->execute([$sender_id, $receiver_id, $message_text]);
    
    if ($result) {
        $message_id = $db->lastInsertId();
        echo "✅ Message sent successfully!\n";
        echo "Message ID: $message_id\n";
        echo "From: User $sender_id\n";
        echo "To: User $receiver_id\n";
        echo "Text: $message_text\n";
        echo "Status: Sent\n";
        echo "Timestamp: " . date('Y-m-d H:i:s') . "\n";
        
        // Now test if it appears in chat list
        echo "\n=== TESTING CHAT LIST AFTER SENDING MESSAGE ===\n";
        
        // Test for user 76
        $stmt = $db->prepare("
            SELECT 
                CASE 
                    WHEN m.sender_id = 76 THEN m.receiver_id
                    ELSE m.sender_id
                END as other_user_id,
                r.f_name,
                r.l_name,
                m.msg_text as last_message,
                m.msg_timestamp as last_message_time
            FROM messages m
            JOIN users u ON (
                CASE 
                    WHEN m.sender_id = 76 THEN m.receiver_id
                    ELSE m.sender_id
                END = u.user_id
            )
            JOIN registrations r ON u.reg_id = r.reg_id
            WHERE (m.sender_id = 76 OR m.receiver_id = 76) 
            AND m.msg_status != 'Deleted'
            ORDER BY m.msg_timestamp DESC
            LIMIT 1
        ");
        $stmt->execute();
        $chat = $stmt->fetch();
        
        if ($chat) {
            echo "✅ Chat found for user 76:\n";
            echo "- Other user: {$chat['other_user_id']} ({$chat['f_name']} {$chat['l_name']})\n";
            echo "- Last message: {$chat['last_message']}\n";
            echo "- Time: {$chat['last_message_time']}\n";
        } else {
            echo "❌ No chat found for user 76\n";
        }
        
    } else {
        echo "❌ Failed to send message\n";
        echo "Error: " . $stmt->error . "\n";
    }
    
    echo "\n=== TEST COMPLETE ===\n";
    
} catch (Exception $e) {
    echo "ERROR: " . $e->getMessage() . "\n";
}
?>








