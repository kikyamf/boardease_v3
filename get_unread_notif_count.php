<?php
// Get unread notification count for a user
error_reporting(0);
ini_set('display_errors', 0);
ob_start();
require_once 'db_helper.php';
header('Content-Type: application/json');

try {
    $user_id = $_GET['user_id'] ?? null;
    
    if (!$user_id) {
        throw new Exception('Missing required parameter: user_id');
    }
    
    $db = getDB();
    
    // Get unread count by type
    $stmt = $db->prepare("
        SELECT 
            notif_type,
            COUNT(*) as count
        FROM notifications 
        WHERE user_id = ? AND notif_status = 'unread'
        GROUP BY notif_type
    ");
    $stmt->execute([$user_id]);
    $unread_by_type = $stmt->fetchAll();
    
    // Get total unread count
    $total_stmt = $db->prepare("
        SELECT COUNT(*) as total_unread 
        FROM notifications 
        WHERE user_id = ? AND notif_status = 'unread'
    ");
    $total_stmt->execute([$user_id]);
    $total_unread = $total_stmt->fetch()['total_unread'];
    
    // Format response
    $unread_counts = [
        'booking' => 0,
        'payment' => 0,
        'announcement' => 0,
        'maintenance' => 0,
        'general' => 0
    ];
    
    foreach ($unread_by_type as $item) {
        $unread_counts[$item['notif_type']] = (int)$item['count'];
    }
    
    $response = [
        'success' => true,
        'data' => [
            'total_unread' => (int)$total_unread,
            'unread_by_type' => $unread_counts
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
