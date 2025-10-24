<?php
require_once 'db_connection.php';
require_once 'fcm_config.php';

// Get parameters from POST request
$senderId = $_POST['sender_id'] ?? null;
$targetUserId = $_POST['target_user_id'] ?? null;
$title = $_POST['title'] ?? null;
$body = $_POST['body'] ?? null;
$data = $_POST['data'] ?? null; // JSON string
$type = $_POST['type'] ?? 'custom'; // custom, system, announcement

if (!$senderId || !$targetUserId || !$title || !$body) {
    sendResponse(false, 'Sender ID, Target User ID, Title, and Body are required');
}

// Validate users
$sender = validateUser($pdo, $senderId);
$targetUser = validateUser($pdo, $targetUserId);

if (!$sender || !$targetUser) {
    sendResponse(false, 'Invalid sender or target user');
}

// Validate title and body
$title = trim($title);
$body = trim($body);

if (empty($title) || empty($body)) {
    sendResponse(false, 'Title and Body cannot be empty');
}

if (strlen($title) > 100 || strlen($body) > 500) {
    sendResponse(false, 'Title or Body is too long');
}

try {
    // Parse additional data if provided
    $notificationData = [];
    if ($data) {
        $parsedData = json_decode($data, true);
        if ($parsedData) {
            $notificationData = $parsedData;
        }
    }

    // Add default data
    $notificationData['type'] = $type;
    $notificationData['sender_id'] = $senderId;
    $notificationData['target_user_id'] = $targetUserId;
    $notificationData['timestamp'] = date('Y-m-d H:i:s');

    // Get target user's device tokens
    $tokensQuery = "
        SELECT device_token 
        FROM device_tokens 
        WHERE user_id = :target_user_id 
        AND is_active = 1 
        AND last_used > DATE_SUB(NOW(), INTERVAL 30 DAY)
    ";

    $stmt = $pdo->prepare($tokensQuery);
    $stmt->bindParam(':target_user_id', $targetUserId, PDO::PARAM_INT);
    $stmt->execute();
    $tokens = $stmt->fetchAll(PDO::FETCH_COLUMN);

    // Send push notification
    $notificationResult = null;
    if (!empty($tokens)) {
        if (count($tokens) == 1) {
            $notificationResult = FCMConfig::sendToDevice(
                $tokens[0], 
                $title, 
                $body, 
                $notificationData
            );
        } else {
            $notificationResult = FCMConfig::sendToMultipleDevices(
                $tokens, 
                $title, 
                $body, 
                $notificationData
            );
        }

        // Log notification
        FCMConfig::logNotification(
            $targetUserId, 
            $type, 
            $title, 
            $body, 
            $notificationResult
        );
    }

    // Save notification to database (optional)
    $saveNotificationQuery = "
        INSERT INTO notifications (sender_id, receiver_id, title, body, type, data, created_at) 
        VALUES (:sender_id, :receiver_id, :title, :body, :type, :data, NOW())
    ";

    $stmt = $pdo->prepare($saveNotificationQuery);
    $stmt->bindParam(':sender_id', $senderId, PDO::PARAM_INT);
    $stmt->bindParam(':receiver_id', $targetUserId, PDO::PARAM_INT);
    $stmt->bindParam(':title', $title, PDO::PARAM_STR);
    $stmt->bindParam(':body', $body, PDO::PARAM_STR);
    $stmt->bindParam(':type', $type, PDO::PARAM_STR);
    $stmt->bindParam(':data', json_encode($notificationData), PDO::PARAM_STR);
    $stmt->execute();

    $response = [
        'notification_sent' => $notificationResult ? $notificationResult['success'] : false,
        'device_count' => count($tokens),
        'notification_id' => $pdo->lastInsertId()
    ];

    if ($notificationResult) {
        $response['fcm_response'] = $notificationResult['response'];
    }

    sendResponse(true, 'Custom notification sent successfully', $response);

} catch (Exception $e) {
    sendResponse(false, 'Error sending custom notification: ' . $e->getMessage());
}
?>

























