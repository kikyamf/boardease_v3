<?php
// Send individual message with FCM notification
error_reporting(0);
ini_set('display_errors', 0);
ob_start();

require_once '../db_helper.php';
require_once '../fcm_config.php';

header('Content-Type: application/json');

try {
    // Get POST data
    $sender_id = $_POST['sender_id'] ?? null;
    $receiver_id = $_POST['receiver_id'] ?? null;
    $message_text = $_POST['message'] ?? null;
    
    // Validate input
    if (!$sender_id || !$receiver_id || !$message_text) {
        throw new Exception('Missing required parameters: sender_id, receiver_id, message');
    }
    
    $db = getDB();
    
    // Insert message into database
    $stmt = $db->prepare("
        INSERT INTO messages (sender_id, receiver_id, msg_text, msg_timestamp, msg_status) 
        VALUES (?, ?, ?, NOW(), 'Sent')
    ");
    
    $stmt->execute([$sender_id, $receiver_id, $message_text]);
    $message_id = $db->lastInsertId();
    
    // Get sender and receiver information
    $stmt = $db->prepare("
        SELECT u.user_id, r.first_name, r.last_name, dt.device_token 
        FROM users u 
        JOIN registrations r ON u.reg_id = r.id
        LEFT JOIN device_tokens dt ON u.user_id = dt.user_id AND dt.is_active = 1
        WHERE u.user_id = ? AND u.status = 'Active' AND r.status = 'approved'
    ");
    
    $stmt->execute([$sender_id]);
    $sender = $stmt->fetch();
    
    $stmt->execute([$receiver_id]);
    $receiver = $stmt->fetch();
    
    if (!$sender || !$receiver) {
        throw new Exception('Sender or receiver not found');
    }
    
    $sender_name = $sender['first_name'] . ' ' . $sender['last_name'];
    $receiver_name = $receiver['first_name'] . ' ' . $receiver['last_name'];
    
    // Only send FCM notification to receiver (not to sender)
    $notification_sent = false;
    $notification_result = null;
    
    if ($sender_id != $receiver_id && $receiver['device_token']) {
        try {
            $notification_result = FCMConfig::sendToDevice(
                $receiver['device_token'],
                $sender_name,
                $message_text,
                [
                    'type' => 'new_message',
                    'sender_id' => (string)$sender_id,
                    'sender_name' => $sender_name,
                    'receiver_id' => (string)$receiver_id,
                    'message_id' => (string)$message_id,
                    'chat_type' => 'individual',
                    'timestamp' => date('Y-m-d H:i:s')
                ]
            );
            
            $notification_sent = $notification_result['success'];
        } catch (Exception $e) {
            $notification_result = [
                'success' => false,
                'error' => $e->getMessage()
            ];
        }
    }
    
    // Log the message
    $log_entry = [
        'message_id' => $message_id,
        'sender_id' => $sender_id,
        'receiver_id' => $receiver_id,
        'message' => $message_text,
        'timestamp' => date('Y-m-d H:i:s'),
        'status' => 'sent',
        'notification_sent' => $notification_sent
    ];
    
    file_put_contents('messages.log', json_encode($log_entry) . "\n", FILE_APPEND);
    
    // Prepare response
    $response = [
        'success' => true,
        'message' => $sender_id == $receiver_id ? 
            'Message sent (no notification - same sender/receiver)' : 
            ($notification_sent ? 'Message sent successfully with notification' : 'Message sent but notification failed'),
        'data' => [
            'message_id' => $message_id,
            'sender_id' => $sender_id,
            'receiver_id' => $receiver_id,
            'message' => $message_text,
            'timestamp' => date('Y-m-d H:i:s'),
            'status' => 'Sent',
            'notification_sent' => $notification_sent,
            'notification_reason' => $sender_id == $receiver_id ? 'Same sender and receiver' : 'Notification sent to receiver',
            'sender_name' => $sender_name,
            'receiver_name' => $receiver_name
        ]
    ];
    
    if ($notification_result) {
        $response['data']['notification_result'] = $notification_result;
    }
    
} catch (Exception $e) {
    $response = [
        'success' => false,
        'message' => 'Error: ' . $e->getMessage(),
        'data' => null
    ];
}

// Clean output buffer and send response
ob_clean();
echo json_encode($response);
exit;
?>










