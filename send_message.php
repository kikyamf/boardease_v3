<?php

// Send individual message with FCM notification
error_reporting(0);
ini_set('display_errors', 0);
ob_start();

require_once 'db_helper.php';
require_once 'fcm_config.php';

header('Content-Type: application/json');

try {
    // Debug logging
    error_log("=== SEND MESSAGE DEBUG ===");
    error_log("POST data: " . print_r($_POST, true));
    error_log("Raw input: " . file_get_contents('php://input'));
    
    // Handle both JSON and regular POST data
    $sender_id = null;
    $receiver_id = null;
    $message_text = null;
    
    // Check if JSON data is sent
    $input = file_get_contents('php://input');
    if (!empty($input)) {
        $data = json_decode($input, true);
        error_log("JSON data: " . print_r($data, true));
        if ($data) {
            $sender_id = $data['sender_id'] ?? null;
            $receiver_id = $data['receiver_id'] ?? null;
            $message_text = $data['message'] ?? null;
        }
    }
    
    // Fallback to regular POST data
    if (!$sender_id || !$receiver_id || !$message_text) {
        $sender_id = $_POST['sender_id'] ?? null;
        $receiver_id = $_POST['receiver_id'] ?? null;
        $message_text = $_POST['message'] ?? null;
    }
    
    error_log("Final values - sender_id: $sender_id, receiver_id: $receiver_id, message: $message_text");
    
    // Validate input
    if (!$sender_id || !$receiver_id || !$message_text) {
        throw new Exception('Missing required parameters: sender_id, receiver_id, message');
    }
    
    $db = getDB();
    
    // Check for duplicate message in last 5 seconds
    $stmt = $db->prepare("
        SELECT message_id FROM messages 
        WHERE sender_id = ? AND receiver_id = ? AND msg_text = ? 
        AND msg_timestamp > DATE_SUB(NOW(), INTERVAL 5 SECOND)
        LIMIT 1
    ");
    $stmt->execute([$sender_id, $receiver_id, $message_text]);
    $duplicate = $stmt->fetch();
    
    if ($duplicate) {
        // Return existing message ID instead of creating duplicate
        $message_id = $duplicate['message_id'];
        error_log("Duplicate message prevented, returning existing ID: $message_id");
    } else {
        // Insert message into database
        $stmt = $db->prepare("
            INSERT INTO messages (sender_id, receiver_id, msg_text, msg_timestamp, msg_status) 
            VALUES (?, ?, ?, NOW(), 'Sent')
        ");
        
        $stmt->execute([$sender_id, $receiver_id, $message_text]);
        $message_id = $db->lastInsertId();
    }
    
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
    $notification_reason = '';
    
    if ($sender_id == $receiver_id) {
        $notification_reason = 'Same sender and receiver';
    } elseif (!$receiver['device_token']) {
        $notification_reason = 'No device token for receiver';
    } else {
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
            $notification_reason = $notification_sent ? 'Notification sent to receiver' : 'Notification failed to send';
        } catch (Exception $e) {
            $notification_result = [
                'success' => false,
                'error' => $e->getMessage()
            ];
            $notification_reason = 'Notification error: ' . $e->getMessage();
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
            'notification_reason' => $notification_reason,
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












