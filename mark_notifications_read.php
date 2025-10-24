<?php
require_once 'db_connection.php';

// Get parameters from POST request
$userId = $_POST['user_id'] ?? null;
$notificationIds = $_POST['notification_ids'] ?? null; // JSON array of notification IDs
$markAll = $_POST['mark_all'] ?? false; // Boolean to mark all as read

if (!$userId) {
    sendResponse(false, 'User ID is required');
}

// Validate user
$user = validateUser($pdo, $userId);
if (!$user) {
    sendResponse(false, 'Invalid user');
}

try {
    if ($markAll) {
        // Mark all notifications as read for the user
        $markAllQuery = "
            UPDATE notifications 
            SET is_read = 1, read_at = NOW() 
            WHERE receiver_id = :user_id 
            AND is_read = 0
        ";

        $stmt = $pdo->prepare($markAllQuery);
        $stmt->bindParam(':user_id', $userId, PDO::PARAM_INT);
        $stmt->execute();
        
        $affectedRows = $stmt->rowCount();
        sendResponse(true, "Marked $affectedRows notifications as read");
        
    } else if ($notificationIds) {
        // Parse notification IDs
        $idsArray = json_decode($notificationIds, true);
        if (!is_array($idsArray) || empty($idsArray)) {
            sendResponse(false, 'Invalid notification IDs format');
        }

        // Validate that all notification IDs belong to the user
        $placeholders = str_repeat('?,', count($idsArray) - 1) . '?';
        $validateQuery = "
            SELECT id 
            FROM notifications 
            WHERE id IN ($placeholders) 
            AND receiver_id = ?
        ";

        $params = array_merge($idsArray, [$userId]);
        $stmt = $pdo->prepare($validateQuery);
        $stmt->execute($params);
        $validIds = $stmt->fetchAll(PDO::FETCH_COLUMN);

        if (count($validIds) != count($idsArray)) {
            sendResponse(false, 'Some notification IDs are invalid or do not belong to the user');
        }

        // Mark specific notifications as read
        $markSpecificQuery = "
            UPDATE notifications 
            SET is_read = 1, read_at = NOW() 
            WHERE id IN ($placeholders) 
            AND receiver_id = ?
        ";

        $stmt = $pdo->prepare($markSpecificQuery);
        $stmt->execute($params);
        
        $affectedRows = $stmt->rowCount();
        sendResponse(true, "Marked $affectedRows notifications as read");
        
    } else {
        sendResponse(false, 'Either notification_ids or mark_all parameter is required');
    }

} catch (Exception $e) {
    sendResponse(false, 'Error marking notifications as read: ' . $e->getMessage());
}
?>

























