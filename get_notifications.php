<?php
// Get notifications for a user
error_reporting(0);
ini_set('display_errors', 0);
ob_start();
require_once 'db_helper.php';
header('Content-Type: application/json');

try {
    $user_id = $_GET['user_id'] ?? null;
    $limit = $_GET['limit'] ?? 50;
    $offset = $_GET['offset'] ?? 0;
    $notif_type = $_GET['notif_type'] ?? null; // Filter by type
    
    if (!$user_id) {
        throw new Exception('Missing required parameter: user_id');
    }
    
    $db = getDB();
    
    // Build query with optional type filter
    $where_clause = "WHERE user_id = ?";
    $params = [$user_id];
    
    if ($notif_type) {
        $where_clause .= " AND notif_type = ?";
        $params[] = $notif_type;
    }
    
    // Get notifications - build complete query
    $query = "
        SELECT notif_id, user_id, notif_title, notif_message, notif_type, 
               notif_status, notif_created_at
        FROM notifications 
        $where_clause
        ORDER BY notif_created_at DESC 
        LIMIT " . (int)$limit . " OFFSET " . (int)$offset;
    
    $stmt = $db->prepare($query);
    $stmt->execute($params);
    $notifications = $stmt->fetchAll();
    
    // Get total count
    $count_query = "
        SELECT COUNT(*) as total_count 
        FROM notifications 
        $where_clause
    ";
    $count_stmt = $db->prepare($count_query);
    $count_stmt->execute($params);
    $total_count = $count_stmt->fetch()['total_count'];
    
    // Get unread count
    $unread_stmt = $db->prepare("
        SELECT COUNT(*) as unread_count 
        FROM notifications 
        WHERE user_id = ? AND notif_status = 'unread'
    ");
    $unread_stmt->execute([$user_id]);
    $unread_count = $unread_stmt->fetch()['unread_count'];
    
    $response = [
        'success' => true,
        'data' => [
            'notifications' => $notifications,
            'total_count' => (int)$total_count,
            'unread_count' => (int)$unread_count,
            'limit' => (int)$limit,
            'offset' => (int)$offset
        ]
    ];
    
} catch (Exception $e) {
    $response = [
        'success' => false,
        'message' => 'Error: ' . $e->getMessage(),
        'data' => null
    ];
}

ob_clean();
echo json_encode($response);
exit;
?>
