<?php
// Test user notifications
error_reporting(0);
ini_set('display_errors', 0);
ob_start();
require_once 'db_helper.php';
header('Content-Type: application/json');

try {
    $user_id = $_GET['user_id'] ?? 29; // Default to user_id = 29 for testing
    
    $db = getDB();
    
    // Check if user exists
    $user_stmt = $db->prepare("
        SELECT user_id, status, reg_id 
        FROM users 
        WHERE user_id = ?
    ");
    $user_stmt->execute([$user_id]);
    $user = $user_stmt->fetch();
    
    // Check if notifications exist for this user
    $notif_stmt = $db->prepare("
        SELECT COUNT(*) as count 
        FROM notifications 
        WHERE user_id = ?
    ");
    $notif_stmt->execute([$user_id]);
    $notif_count = $notif_stmt->fetch()['count'];
    
    // Get sample notifications
    $sample_stmt = $db->prepare("
        SELECT notif_id, notif_title, notif_message, notif_type, notif_status, notif_created_at
        FROM notifications 
        WHERE user_id = ?
        ORDER BY notif_created_at DESC 
        LIMIT 5
    ");
    $sample_stmt->execute([$user_id]);
    $sample_notifications = $sample_stmt->fetchAll();
    
    // Check all users in database
    $all_users_stmt = $db->prepare("
        SELECT user_id, status 
        FROM users 
        ORDER BY user_id 
        LIMIT 10
    ");
    $all_users_stmt->execute();
    $all_users = $all_users_stmt->fetchAll();
    
    $response = [
        'success' => true,
        'data' => [
            'requested_user_id' => $user_id,
            'user_exists' => $user ? true : false,
            'user_details' => $user,
            'notification_count' => (int)$notif_count,
            'sample_notifications' => $sample_notifications,
            'all_users_in_db' => $all_users
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
echo json_encode($response, JSON_PRETTY_PRINT);
exit;
?>






















