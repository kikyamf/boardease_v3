<?php
require_once 'db_connection.php';
require_once 'fcm_config.php';

// Get parameters from POST request
$senderId = $_POST['sender_id'] ?? null;
$groupId = $_POST['group_id'] ?? null;
$messageText = $_POST['message_text'] ?? null;

if (!$senderId || !$groupId || !$messageText) {
    sendResponse(false, 'Sender ID, Group ID, and Message Text are required');
}

// Validate user
$sender = validateUser($pdo, $senderId);
if (!$sender) {
    sendResponse(false, 'Invalid sender');
}

// Check if user is member of the group
$checkMembershipQuery = "
    SELECT COUNT(*) as is_member 
    FROM group_members 
    WHERE user_id = :user_id AND gc_id = :group_id
";

$stmt = $pdo->prepare($checkMembershipQuery);
$stmt->bindParam(':user_id', $senderId, PDO::PARAM_INT);
$stmt->bindParam(':group_id', $groupId, PDO::PARAM_INT);
$stmt->execute();
$membership = $stmt->fetch();

if (!$membership['is_member']) {
    sendResponse(false, 'User is not a member of this group');
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
    // Insert group message into database
    $insertQuery = "
        INSERT INTO group_messages (gc_id, sender_id, groupmessage_text, groupmessage_timestamp, groupmessage_status) 
        VALUES (:group_id, :sender_id, :message_text, NOW(), 'Sent')
    ";

    $stmt = $pdo->prepare($insertQuery);
    $stmt->bindParam(':group_id', $groupId, PDO::PARAM_INT);
    $stmt->bindParam(':sender_id', $senderId, PDO::PARAM_INT);
    $stmt->bindParam(':message_text', $messageText, PDO::PARAM_STR);
    $stmt->execute();

    $messageId = $pdo->lastInsertId();

    // Get the inserted message with user details
    $getMessageQuery = "
        SELECT 
            gm.groupmessage_id,
            gm.sender_id,
            gm.gc_id,
            gm.groupmessage_text,
            gm.groupmessage_timestamp,
            gm.groupmessage_status,
            u.first_name as sender_first_name,
            u.last_name as sender_last_name,
            gc.gc_name as group_name
        FROM group_messages gm
        JOIN users u ON gm.sender_id = u.user_id
        JOIN chat_groups gc ON gm.gc_id = gc.gc_id
        WHERE gm.groupmessage_id = :message_id
    ";

    $stmt = $pdo->prepare($getMessageQuery);
    $stmt->bindParam(':message_id', $messageId, PDO::PARAM_INT);
    $stmt->execute();
    $message = $stmt->fetch();

    if ($message) {
        $formattedMessage = [
            'messageId' => $message['groupmessage_id'],
            'senderId' => $message['sender_id'],
            'groupId' => $message['gc_id'],
            'messageText' => $message['groupmessage_text'],
            'timestamp' => $message['groupmessage_timestamp'],
            'status' => $message['groupmessage_status'],
            'isReceiver' => false,
            'senderName' => $message['sender_first_name'] . ' ' . $message['sender_last_name']
        ];

        // Send push notifications to all group members except sender
        $notificationTitle = $message['group_name'];
        $notificationBody = $message['sender_first_name'] . ': ' . $messageText;
        
        // Truncate message if too long
        if (strlen($notificationBody) > 100) {
            $notificationBody = substr($notificationBody, 0, 97) . '...';
        }

        // Get all group members' device tokens (except sender)
        $tokensQuery = "
            SELECT DISTINCT dt.device_token, gm.user_id
            FROM group_members gm
            JOIN device_tokens dt ON gm.user_id = dt.user_id
            WHERE gm.gc_id = :group_id 
            AND gm.user_id != :sender_id
            AND dt.is_active = 1 
            AND dt.last_used > DATE_SUB(NOW(), INTERVAL 30 DAY)
        ";

        $stmt = $pdo->prepare($tokensQuery);
        $stmt->bindParam(':group_id', $groupId, PDO::PARAM_INT);
        $stmt->bindParam(':sender_id', $senderId, PDO::PARAM_INT);
        $stmt->execute();
        $memberTokens = $stmt->fetchAll();

        // Send push notifications
        $notificationResults = [];
        if (!empty($memberTokens)) {
            $tokens = array_column($memberTokens, 'device_token');
            $notificationData = [
                'type' => 'group_message',
                'message_id' => $messageId,
                'sender_id' => $senderId,
                'sender_name' => $message['sender_first_name'] . ' ' . $message['sender_last_name'],
                'group_id' => $groupId,
                'group_name' => $message['group_name'],
                'chat_type' => 'group'
            ];

            $notificationResult = FCMConfig::sendToMultipleDevices(
                $tokens, 
                $notificationTitle, 
                $notificationBody, 
                $notificationData
            );

            // Log notification for each member
            foreach ($memberTokens as $memberToken) {
                FCMConfig::logNotification(
                    $memberToken['user_id'], 
                    'group_message', 
                    $notificationTitle, 
                    $notificationBody, 
                    $notificationResult
                );
            }

            $notificationResults = $notificationResult;
        }

        // Add notification result to response
        $formattedMessage['notification_sent'] = $notificationResults ? $notificationResults['success'] : false;
        $formattedMessage['notification_count'] = count($memberTokens);

        sendResponse(true, 'Group message sent successfully', $formattedMessage);
    } else {
        sendResponse(false, 'Message sent but could not retrieve details');
    }

} catch (Exception $e) {
    sendResponse(false, 'Error sending group message: ' . $e->getMessage());
}
?>
















