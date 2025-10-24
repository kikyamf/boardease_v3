<?php
// Debug notification badge issue
error_reporting(E_ALL);
ini_set('display_errors', 1);
require_once 'db_helper.php';
header('Content-Type: application/json');

try {
    $user_id = 1;
    
    // Check if user exists
    $db = getDB();
    $stmt = $db->prepare("SELECT user_id FROM users WHERE user_id = ?");
    $stmt->execute([$user_id]);
    $user_exists = $stmt->fetch();
    
    // Check notifications for user
    $stmt = $db->prepare("SELECT COUNT(*) as total FROM notifications WHERE user_id = ?");
    $stmt->execute([$user_id]);
    $total_notifications = $stmt->fetch()['total'];
    
    // Check unread notifications
    $stmt = $db->prepare("SELECT COUNT(*) as unread FROM notifications WHERE user_id = ? AND notif_status = 'unread'");
    $stmt->execute([$user_id]);
    $unread_notifications = $stmt->fetch()['unread'];
    
    // Get recent notifications
    $stmt = $db->prepare("SELECT notif_id, notif_title, notif_status, notif_created_at FROM notifications WHERE user_id = ? ORDER BY notif_created_at DESC LIMIT 5");
    $stmt->execute([$user_id]);
    $recent_notifications = $stmt->fetchAll();
    
    $response = [
        'success' => true,
        'debug_info' => [
            'user_exists' => $user_exists ? true : false,
            'total_notifications' => (int)$total_notifications,
            'unread_notifications' => (int)$unread_notifications,
            'recent_notifications' => $recent_notifications
        ]
    ];
    
} catch (Exception $e) {
    $response = [
        'success' => false,
        'error' => $e->getMessage()
    ];
}

echo json_encode($response, JSON_PRETTY_PRINT);
?>





















