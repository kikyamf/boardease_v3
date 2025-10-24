<?php
// Debug version of get notifications
error_reporting(0);
ini_set('display_errors', 0);
ob_start();
require_once 'db_helper.php';
header('Content-Type: application/json');

try {
    $user_id = $_GET['user_id'] ?? null;
    $limit = $_GET['limit'] ?? 50;
    $offset = $_GET['offset'] ?? 0;
    $notif_type = $_GET['notif_type'] ?? null;
    
    // Debug information
    $debug_info = [
        'received_user_id' => $user_id,
        'received_limit' => $limit,
        'received_offset' => $offset,
        'received_notif_type' => $notif_type,
        'request_method' => $_SERVER['REQUEST_METHOD'],
        'all_get_params' => $_GET
    ];
    
    if (!$user_id) {
        throw new Exception('Missing required parameter: user_id');
    }
    
    $db = getDB();
    
    // Check if user exists in database
    $user_check_stmt = $db->prepare("
        SELECT user_id, status 
        FROM users 
        WHERE user_id = ?
    ");
    $user_check_stmt->execute([$user_id]);
    $user_exists = $user_check_stmt->fetch();
    
    if (!$user_exists) {
        throw new Exception("User with ID $user_id does not exist in database");
    }
    
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
        'debug_info' => $debug_info,
        'user_exists' => $user_exists,
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
        'debug_info' => $debug_info ?? null,
        'data' => null
    ];
}

ob_clean();
echo json_encode($response, JSON_PRETTY_PRINT);
exit;
?>








