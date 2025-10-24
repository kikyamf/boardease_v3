<?php
require_once 'db_connection.php';
require_once 'fcm_config.php';

// Get user_id from POST request
$userId = $_POST['user_id'] ?? null;

if (!$userId) {
    sendResponse(false, 'User ID is required');
}

// Validate user
$user = validateUser($pdo, $userId);
if (!$user) {
    sendResponse(false, 'Invalid user');
}

try {
    // Get unread individual messages count
    $individualUnreadQuery = "
        SELECT COUNT(*) as unread_count
        FROM messages 
        WHERE receiver_id = :user_id 
        AND msg_status != 'Read'
    ";

    $stmt = $pdo->prepare($individualUnreadQuery);
    $stmt->bindParam(':user_id', $userId, PDO::PARAM_INT);
    $stmt->execute();
    $individualUnread = $stmt->fetch();

    // Get unread group messages count
    $groupUnreadQuery = "
        SELECT COUNT(*) as unread_count
        FROM group_messages gm
        JOIN group_members gm_member ON gm.gc_id = gm_member.gc_id
        WHERE gm_member.user_id = :user_id 
        AND gm.sender_id != :user_id 
        AND gm.groupmessage_status != 'Read'
    ";

    $stmt = $pdo->prepare($groupUnreadQuery);
    $stmt->bindParam(':user_id', $userId, PDO::PARAM_INT);
    $stmt->execute();
    $groupUnread = $stmt->fetch();

    // Get unread system notifications count
    $systemUnreadQuery = "
        SELECT COUNT(*) as unread_count
        FROM notifications 
        WHERE receiver_id = :user_id 
        AND is_read = 0
        AND created_at > DATE_SUB(NOW(), INTERVAL 7 DAY)
    ";

    $stmt = $pdo->prepare($systemUnreadQuery);
    $stmt->bindParam(':user_id', $userId, PDO::PARAM_INT);
    $stmt->execute();
    $systemUnread = $stmt->fetch();

    // Calculate total unread count
    $totalUnread = (int)$individualUnread['unread_count'] + (int)$groupUnread['unread_count'];

    // Get detailed breakdown
    $unreadBreakdown = [
        'individual' => (int)$individualUnread['unread_count'],
        'group' => (int)$groupUnread['unread_count'],
        'system' => (int)$systemUnread['unread_count'],
        'total' => $totalUnread,
        'last_checked' => date('Y-m-d H:i:s')
    ];

    // If there are unread messages, send a data-only notification to update badge
    if ($totalUnread > 0) {
        // Get user's device tokens
        $tokensQuery = "
            SELECT device_token 
            FROM device_tokens 
            WHERE user_id = :user_id 
            AND is_active = 1 
            AND last_used > DATE_SUB(NOW(), INTERVAL 30 DAY)
        ";

        $stmt = $pdo->prepare($tokensQuery);
        $stmt->bindParam(':user_id', $userId, PDO::PARAM_INT);
        $stmt->execute();
        $tokens = $stmt->fetchAll(PDO::FETCH_COLUMN);

        // Send data-only message to update badge
        if (!empty($tokens)) {
            $badgeData = [
                'type' => 'badge_update',
                'unread_count' => $totalUnread,
                'individual_count' => (int)$individualUnread['unread_count'],
                'group_count' => (int)$groupUnread['unread_count'],
                'timestamp' => date('Y-m-d H:i:s')
            ];

            foreach ($tokens as $token) {
                FCMConfig::sendDataMessage($token, $badgeData);
            }
        }
    }

    sendResponse(true, 'Unread count retrieved successfully', $unreadBreakdown);

} catch (Exception $e) {
    sendResponse(false, 'Error retrieving unread count: ' . $e->getMessage());
}
?>





















