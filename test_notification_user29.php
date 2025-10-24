<?php
// Test notification for user 29
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
    
    // Create sample notification
    $notif_title = "Welcome to BoardEase!";
    $notif_message = "Your account has been successfully activated. You can now start exploring boarding houses and managing your bookings.";
    $notif_type = "account";
    $notif_status = "unread";
    
    // Insert notification into database
    $insert_stmt = $db->prepare("
        INSERT INTO notifications (user_id, notif_title, notif_message, notif_type, notif_status, notif_created_at) 
        VALUES (?, ?, ?, ?, ?, NOW())
    ");
    $insert_stmt->execute([$user_id, $notif_title, $notif_message, $notif_type, $notif_status]);
    $notif_id = $db->lastInsertId();
    
    // Get device token for FCM
    $token_stmt = $db->prepare("
        SELECT device_token 
        FROM device_tokens 
        WHERE user_id = ? AND is_active = 1 
        ORDER BY updated_at DESC 
        LIMIT 1
    ");
    $token_stmt->execute([$user_id]);
    $token_result = $token_stmt->fetch();
    
    $fcm_sent = false;
    $fcm_message = "No device token found";
    
    if ($token_result && !empty($token_result['device_token'])) {
        // Send FCM notification
        require_once 'fcm_config.php';
        
        $device_token = $token_result['device_token'];
        $fcm_result = FCMConfig::sendToDevice($device_token, $notif_title, $notif_message, [
            'type' => $notif_type,
            'notif_id' => $notif_id,
            'user_id' => $user_id
        ]);
        
        $fcm_sent = $fcm_result['success'];
        $fcm_message = $fcm_result['message'];
    }
    
    // Get updated notification count
    $count_stmt = $db->prepare("
        SELECT COUNT(*) as count 
        FROM notifications 
        WHERE user_id = ?
    ");
    $count_stmt->execute([$user_id]);
    $notif_count = $count_stmt->fetch()['count'];
    
    // Get the created notification
    $get_notif_stmt = $db->prepare("
        SELECT notif_id, notif_title, notif_message, notif_type, notif_status, notif_created_at
        FROM notifications 
        WHERE notif_id = ?
    ");
    $get_notif_stmt->execute([$notif_id]);
    $created_notification = $get_notif_stmt->fetch();
    
    $response = [
        'success' => true,
        'message' => 'Notification created successfully for user 29',
        'data' => [
            'user_id' => $user_id,
            'user_details' => $user,
            'notification_created' => $created_notification,
            'notification_count_after' => (int)$notif_count,
            'fcm_sent' => $fcm_sent,
            'fcm_message' => $fcm_message,
            'device_token_found' => $token_result ? true : false
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






