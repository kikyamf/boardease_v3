<?php
// Test notification for user 29 - Fixed version
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
    
    // Clear old notifications first to avoid spam
    $clear_stmt = $db->prepare("
        DELETE FROM notifications 
        WHERE user_id = ? AND notif_type = 'test'
    ");
    $clear_stmt->execute([$user_id]);
    
    // Create sample notification with unique content
    $timestamp = date('Y-m-d H:i:s');
    $notif_title = "Test Notification - " . $timestamp;
    $notif_message = "This is a test notification sent at " . $timestamp . ". This should appear as a pop-up notification.";
    $notif_type = "test";
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
        // Send FCM notification with proper configuration
        require_once 'fcm_config.php';
        
        $device_token = $token_result['device_token'];
        
        // Create FCM payload with proper notification structure
        $fcm_payload = [
            'notification' => [
                'title' => $notif_title,
                'body' => $notif_message,
                'sound' => 'default',
                'click_action' => 'FLUTTER_NOTIFICATION_CLICK'
            ],
            'data' => [
                'type' => $notif_type,
                'notif_id' => (string)$notif_id,
                'user_id' => (string)$user_id,
                'timestamp' => $timestamp
            ],
            'priority' => 'high',
            'time_to_live' => 3600
        ];
        
        // Send using FCMConfig
        $fcm_result = FCMConfig::sendToDevice($device_token, $notif_title, $notif_message, [
            'type' => $notif_type,
            'notif_id' => $notif_id,
            'user_id' => $user_id,
            'timestamp' => $timestamp
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
        'message' => 'Test notification created successfully for user 29',
        'data' => [
            'user_id' => $user_id,
            'user_details' => $user,
            'notification_created' => $created_notification,
            'notification_count_after' => (int)$notif_count,
            'fcm_sent' => $fcm_sent,
            'fcm_message' => $fcm_message,
            'device_token_found' => $token_result ? true : false,
            'timestamp' => $timestamp,
            'note' => 'This notification should appear as a pop-up. If it doesn\'t, try clearing app data or restarting the device.'
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






