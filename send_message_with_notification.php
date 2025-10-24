<?php
require_once 'db_connection.php';
require_once 'fcm_config.php';

// Get parameters from POST request
$senderId = $_POST['sender_id'] ?? null;
$receiverId = $_POST['receiver_id'] ?? null;
$messageText = $_POST['message_text'] ?? null;

if (!$senderId || !$receiverId || !$messageText) {
    sendResponse(false, 'Sender ID, Receiver ID, and Message Text are required');
}

// Validate users
$sender = validateUser($pdo, $senderId);
$receiver = validateUser($pdo, $receiverId);

if (!$sender || !$receiver) {
    sendResponse(false, 'Invalid sender or receiver');
}

// Validate message text
$messageText = trim($messageText);
if (empty($messageText)) {
    sendResponse(false, 'Message text cannot be empty');
}

if (strlen($messageText) > 1000) {
    sendResponse(false, 'Message text is too long (max 1000 characters)');
}

try {
    // Insert message into database
    $insertQuery = "
        INSERT INTO messages (sender_id, receiver_id, msg_text, msg_timestamp, msg_status) 
        VALUES (:sender_id, :receiver_id, :message_text, NOW(), 'Sent')
    ";

    $stmt = $pdo->prepare($insertQuery);
    $stmt->bindParam(':sender_id', $senderId, PDO::PARAM_INT);
    $stmt->bindParam(':receiver_id', $receiverId, PDO::PARAM_INT);
    $stmt->bindParam(':message_text', $messageText, PDO::PARAM_STR);
    $stmt->execute();

    $messageId = $pdo->lastInsertId();

    // Get the inserted message with user details
    $getMessageQuery = "
        SELECT 
            m.message_id,
            m.sender_id,
            m.receiver_id,
            m.msg_text,
            m.msg_timestamp,
            m.msg_status,
            s.first_name as sender_first_name,
            s.last_name as sender_last_name,
            r.first_name as receiver_first_name,
            r.last_name as receiver_last_name
        FROM messages m
        JOIN users s ON m.sender_id = s.user_id
        JOIN users r ON m.receiver_id = r.user_id
        WHERE m.message_id = :message_id
    ";

    $stmt = $pdo->prepare($getMessageQuery);
    $stmt->bindParam(':message_id', $messageId, PDO::PARAM_INT);
    $stmt->execute();
    $message = $stmt->fetch();

    if ($message) {
        $formattedMessage = [
            'messageId' => $message['message_id'],
            'senderId' => $message['sender_id'],
            'receiverId' => $message['receiver_id'],
            'messageText' => $message['msg_text'],
            'timestamp' => $message['msg_timestamp'],
            'status' => $message['msg_status'],
            'isReceiver' => false,
            'senderName' => $message['sender_first_name'] . ' ' . $message['sender_last_name'],
            'receiverName' => $message['receiver_first_name'] . ' ' . $message['receiver_last_name']
        ];

        // Send push notification to receiver
        $notificationTitle = $message['sender_first_name'] . ' ' . $message['sender_last_name'];
        $notificationBody = $messageText;
        
        // Truncate message if too long
        if (strlen($notificationBody) > 100) {
            $notificationBody = substr($notificationBody, 0, 97) . '...';
        }

        // Get receiver's device tokens
        $tokensQuery = "
            SELECT device_token 
            FROM device_tokens 
            WHERE user_id = :receiver_id 
            AND is_active = 1 
            AND last_used > DATE_SUB(NOW(), INTERVAL 30 DAY)
        ";

        $stmt = $pdo->prepare($tokensQuery);
        $stmt->bindParam(':receiver_id', $receiverId, PDO::PARAM_INT);
        $stmt->execute();
        $tokens = $stmt->fetchAll(PDO::FETCH_COLUMN);

        // Send push notification if tokens exist
        $notificationResult = null;
        if (!empty($tokens)) {
            $notificationData = [
                'type' => 'message',
                'message_id' => $messageId,
                'sender_id' => $senderId,
                'sender_name' => $notificationTitle,
                'chat_type' => 'individual'
            ];

            if (count($tokens) == 1) {
                $notificationResult = FCMConfig::sendToDevice(
                    $tokens[0], 
                    $notificationTitle, 
                    $notificationBody, 
                    $notificationData
                );
            } else {
                $notificationResult = FCMConfig::sendToMultipleDevices(
                    $tokens, 
                    $notificationTitle, 
                    $notificationBody, 
                    $notificationData
                );
            }

            // Log notification
            FCMConfig::logNotification(
                $receiverId, 
                'individual_message', 
                $notificationTitle, 
                $notificationBody, 
                $notificationResult
            );
        }

        // Add notification result to response
        $formattedMessage['notification_sent'] = $notificationResult ? $notificationResult['success'] : false;

        sendResponse(true, 'Message sent successfully', $formattedMessage);
    } else {
        sendResponse(false, 'Message sent but could not retrieve details');
    }

} catch (Exception $e) {
    sendResponse(false, 'Error sending message: ' . $e->getMessage());
}
?>
















