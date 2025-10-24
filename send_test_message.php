<?php
// Send a test message for badge testing
error_reporting(0);
ini_set('display_errors', 0);
ob_start();

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $sender_id = 2; // Test sender (user ID 2)
    $receiver_id = 1; // Test receiver (user ID 1)
    $message_text = "🔔 Test Message Badge - " . date('H:i:s');
    
    $db = getDB();
    
    // Insert message into database
    $stmt = $db->prepare("
        INSERT INTO messages (sender_id, receiver_id, msg_text, msg_timestamp, msg_status) 
        VALUES (?, ?, ?, NOW(), 'Sent')
    ");
    
    $stmt->execute([$sender_id, $receiver_id, $message_text]);
    $message_id = $db->lastInsertId();
    
    // Get total unread messages for receiver
    $stmt = $db->prepare("
        SELECT COUNT(*) as total_unread
        FROM messages 
        WHERE receiver_id = ? AND msg_status = 'Sent'
    ");
    
    $stmt->execute([$receiver_id]);
    $total_unread = $stmt->fetch()['total_unread'];
    
    $response = [
        'success' => true,
        'message' => 'Test message sent successfully',
        'data' => [
            'message_id' => $message_id,
            'sender_id' => $sender_id,
            'receiver_id' => $receiver_id,
            'message_text' => $message_text,
            'total_unread' => $total_unread
        ]
    ];
    
    echo "✅ Created test message: $message_text\n";
    echo "📊 Total unread messages: $total_unread\n";
    echo "📱 Android app message badge should show: $total_unread\n";
    echo "🔍 Check the debug logs to see if message badge displays correctly!\n";
    
} catch (Exception $e) {
    $response = [
        'success' => false,
        'message' => 'Error: ' . $e->getMessage(),
        'data' => null
    ];
    
    echo "❌ Error: " . $e->getMessage() . "\n";
}

ob_clean();
echo json_encode($response, JSON_PRETTY_PRINT);
exit;
?>





















