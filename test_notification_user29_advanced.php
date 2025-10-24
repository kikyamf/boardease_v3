<?php
// Advanced test notification for user 29 - Handles FCM pop-up issues
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
    
    // Get device token for FCM
    $token_stmt = $db->prepare("
        SELECT device_token, created_at, updated_at
        FROM device_tokens 
        WHERE user_id = ? AND is_active = 1 
        ORDER BY updated_at DESC 
        LIMIT 1
    ");
    $token_stmt->execute([$user_id]);
    $token_result = $token_stmt->fetch();
    
    if (!$token_result || empty($token_result['device_token'])) {
        throw new Exception("No active device token found for user $user_id");
    }
    
    // Create notification with unique content and timestamp
    $timestamp = date('Y-m-d H:i:s');
    $unique_id = uniqid();
    $notif_title = "Test Notification #$unique_id";
    $notif_message = "This is test notification sent at $timestamp. This should appear as a pop-up notification.";
    $notif_type = "test";
    $notif_status = "unread";
    
    // Insert notification into database
    $insert_stmt = $db->prepare("
        INSERT INTO notifications (user_id, notif_title, notif_message, notif_type, notif_status, notif_created_at) 
        VALUES (?, ?, ?, ?, ?, NOW())
    ");
    $insert_stmt->execute([$user_id, $notif_title, $notif_message, $notif_type, $notif_status]);
    $notif_id = $db->lastInsertId();
    
    // Send FCM notification with advanced configuration
    require_once 'fcm_config.php';
    
    $device_token = $token_result['device_token'];
    
    // Create FCM payload with proper notification structure for pop-ups
    $fcm_payload = [
        'notification' => [
            'title' => $notif_title,
            'body' => $notif_message,
            'sound' => 'default',
            'click_action' => 'FLUTTER_NOTIFICATION_CLICK',
            'icon' => 'ic_notification',
            'color' => '#4CAF50',
            'tag' => 'test_notification_' . $unique_id, // Unique tag to prevent grouping
            'priority' => 'high'
        ],
        'data' => [
            'type' => $notif_type,
            'notif_id' => (string)$notif_id,
            'user_id' => (string)$user_id,
            'timestamp' => $timestamp,
            'unique_id' => $unique_id
        ],
        'priority' => 'high',
        'time_to_live' => 3600,
        'collapse_key' => 'test_notification_' . $unique_id // Prevent collapsing
    ];
    
    // Send using FCMConfig with custom payload
    $fcm_result = FCMConfig::sendToDevice($device_token, $notif_title, $notif_message, [
        'type' => $notif_type,
        'notif_id' => $notif_id,
        'user_id' => $user_id,
        'timestamp' => $timestamp,
        'unique_id' => $unique_id,
        'tag' => 'test_notification_' . $unique_id
    ]);
    
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
        'message' => 'Advanced test notification sent to user 29',
        'data' => [
            'user_id' => $user_id,
            'user_details' => $user,
            'notification_created' => $created_notification,
            'notification_count_after' => (int)$notif_count,
            'fcm_sent' => $fcm_result['success'],
            'fcm_message' => $fcm_result['message'],
            'device_token' => substr($device_token, 0, 20) . '...', // Show partial token
            'device_token_created' => $token_result['created_at'],
            'device_token_updated' => $token_result['updated_at'],
            'unique_id' => $unique_id,
            'timestamp' => $timestamp,
            'fcm_payload' => $fcm_payload,
            'troubleshooting' => [
                'if_no_popup' => 'Try clearing app data or restarting device',
                'if_still_no_popup' => 'Check device notification settings',
                'if_badge_updates' => 'FCM is working, just pop-up issue',
                'next_step' => 'Try sending another notification with different content'
            ]
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






