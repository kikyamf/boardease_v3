<?php
// Clear test notifications for user 29
error_reporting(0);
ini_set('display_errors', 0);
ob_start();
require_once 'db_helper.php';
header('Content-Type: application/json');

try {
    $user_id = 29; // Target user
    
    $db = getDB();
    
    // Check if user exists
    $user_stmt = $db->prepare("
        SELECT user_id, status, reg_id 
        FROM users 
        WHERE user_id = ?
    ");
    $user_stmt->execute([$user_id]);
    $user = $user_stmt->fetch();
    
    if (!$user) {
        throw new Exception("User $user_id not found");
    }
    
    // Count notifications before clearing
    $count_before_stmt = $db->prepare("
        SELECT COUNT(*) as count 
        FROM notifications 
        WHERE user_id = ?
    ");
    $count_before_stmt->execute([$user_id]);
    $count_before = $count_before_stmt->fetch()['count'];
    
    // Clear test notifications
    $clear_stmt = $db->prepare("
        DELETE FROM notifications 
        WHERE user_id = ? AND notif_type = 'test'
    ");
    $clear_stmt->execute([$user_id]);
    $deleted_count = $clear_stmt->rowCount();
    
    // Count notifications after clearing
    $count_after_stmt = $db->prepare("
        SELECT COUNT(*) as count 
        FROM notifications 
        WHERE user_id = ?
    ");
    $count_after_stmt->execute([$user_id]);
    $count_after = $count_after_stmt->fetch()['count'];
    
    $response = [
        'success' => true,
        'message' => 'Test notifications cleared for user 29',
        'data' => [
            'user_id' => $user_id,
            'notifications_before' => (int)$count_before,
            'notifications_after' => (int)$count_after,
            'test_notifications_deleted' => $deleted_count,
            'remaining_notifications' => (int)$count_after
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






