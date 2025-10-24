<?php
// Create notification
error_reporting(0);
ini_set('display_errors', 0);
ob_start();
require_once 'db_helper.php';
require_once 'fcm_config.php';
header('Content-Type: application/json');

try {
    // Get POST data
    $user_id = $_POST['user_id'] ?? null;
    $notif_title = $_POST['notif_title'] ?? null;
    $notif_message = $_POST['notif_message'] ?? null;
    $notif_type = $_POST['notif_type'] ?? 'general';
    $send_fcm = $_POST['send_fcm'] ?? true; // Whether to send FCM notification
    
    // Validate input
    if (!$user_id || !$notif_title || !$notif_message) {
        throw new Exception('Missing required parameters: user_id, notif_title, notif_message');
    }
    
    // Validate notification type
    $valid_types = ['booking', 'payment', 'announcement', 'maintenance', 'general'];
    if (!in_array($notif_type, $valid_types)) {
        $notif_type = 'general';
    }
    
    $db = getDB();
    
    // Insert notification into database
    $stmt = $db->prepare("
        INSERT INTO notifications (user_id, notif_title, notif_message, notif_type, notif_status, notif_created_at) 
        VALUES (?, ?, ?, ?, 'unread', NOW())
    ");
    $stmt->execute([$user_id, $notif_title, $notif_message, $notif_type]);
    $notif_id = $db->lastInsertId();
    
    // Get user information for FCM
    $stmt = $db->prepare("
        SELECT u.user_id, r.f_name as first_name, r.l_name as last_name, dt.device_token 
        FROM users u 
        JOIN registration r ON u.reg_id = r.reg_id 
        LEFT JOIN device_tokens dt ON u.user_id = dt.user_id AND dt.is_active = 1 
        WHERE u.user_id = ? AND u.status = 'Active' AND r.status = 'Approved'
    ");
    $stmt->execute([$user_id]);
    $user = $stmt->fetch();
    
    $fcm_sent = false;
    $fcm_result = null;
    
    // Send FCM notification if requested and user has device token
    if ($send_fcm && $user && $user['device_token']) {
        try {
            $fcm_result = FCMConfig::sendToDevice(
                $user['device_token'],
                $notif_title,
                $notif_message,
                [
                    'type' => 'notification',
                    'notif_id' => (string)$notif_id,
                    'notif_type' => $notif_type,
                    'user_id' => (string)$user_id,
                    'timestamp' => date('Y-m-d H:i:s')
                ]
            );
            $fcm_sent = $fcm_result['success'];
        } catch (Exception $e) {
            $fcm_result = [
                'success' => false,
                'error' => $e->getMessage()
            ];
        }
    }
    
    $response = [
        'success' => true,
        'message' => 'Notification created successfully',
        'data' => [
            'notif_id' => $notif_id,
            'user_id' => $user_id,
            'notif_title' => $notif_title,
            'notif_message' => $notif_message,
            'notif_type' => $notif_type,
            'notif_status' => 'unread',
            'notif_created_at' => date('Y-m-d H:i:s'),
            'fcm_sent' => $fcm_sent,
            'fcm_result' => $fcm_result
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
