<?php
require_once 'db_connection.php';

// Get parameters from POST request
$userId = $_POST['user_id'] ?? null;
$limit = $_POST['limit'] ?? 20;
$offset = $_POST['offset'] ?? 0;
$type = $_POST['type'] ?? null; // Filter by type: message, system, custom, etc.

if (!$userId) {
    sendResponse(false, 'User ID is required');
}

// Validate user
$user = validateUser($pdo, $userId);
if (!$user) {
    sendResponse(false, 'Invalid user');
}

try {
    // Build query with optional type filter
    $whereClause = "WHERE n.receiver_id = :user_id";
    $params = [':user_id' => $userId];
    
    if ($type) {
        $whereClause .= " AND n.type = :type";
        $params[':type'] = $type;
    }

    $notificationsQuery = "
        SELECT 
            n.id,
            n.sender_id,
            n.receiver_id,
            n.title,
            n.body,
            n.type,
            n.data,
            n.is_read,
            n.created_at,
            s.first_name as sender_first_name,
            s.last_name as sender_last_name
        FROM notifications n
        LEFT JOIN users s ON n.sender_id = s.user_id
        $whereClause
        ORDER BY n.created_at DESC
        LIMIT :limit OFFSET :offset
    ";

    $stmt = $pdo->prepare($notificationsQuery);
    $stmt->bindParam(':user_id', $userId, PDO::PARAM_INT);
    $stmt->bindParam(':limit', $limit, PDO::PARAM_INT);
    $stmt->bindParam(':offset', $offset, PDO::PARAM_INT);
    
    if ($type) {
        $stmt->bindParam(':type', $type, PDO::PARAM_STR);
    }
    
    $stmt->execute();
    $notifications = $stmt->fetchAll();

    // Format notifications
    $formattedNotifications = [];
    foreach ($notifications as $notification) {
        $data = json_decode($notification['data'], true) ?: [];
        
        $formattedNotifications[] = [
            'id' => $notification['id'],
            'senderId' => $notification['sender_id'],
            'receiverId' => $notification['receiver_id'],
            'title' => $notification['title'],
            'body' => $notification['body'],
            'type' => $notification['type'],
            'data' => $data,
            'isRead' => (bool)$notification['is_read'],
            'createdAt' => $notification['created_at'],
            'senderName' => $notification['sender_first_name'] ? 
                $notification['sender_first_name'] . ' ' . $notification['sender_last_name'] : 
                'System'
        ];
    }

    // Get total count for pagination
    $countQuery = "
        SELECT COUNT(*) as total
        FROM notifications n
        $whereClause
    ";

    $stmt = $pdo->prepare($countQuery);
    $stmt->bindParam(':user_id', $userId, PDO::PARAM_INT);
    
    if ($type) {
        $stmt->bindParam(':type', $type, PDO::PARAM_STR);
    }
    
    $stmt->execute();
    $totalCount = $stmt->fetch()['total'];

    $response = [
        'notifications' => $formattedNotifications,
        'total_count' => (int)$totalCount,
        'has_more' => ($offset + $limit) < $totalCount
    ];

    sendResponse(true, 'Notification history retrieved successfully', $response);

} catch (Exception $e) {
    sendResponse(false, 'Error retrieving notification history: ' . $e->getMessage());
}
?>

























