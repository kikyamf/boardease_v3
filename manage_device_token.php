<?php
// Register or update device token for FCM
error_reporting(0);
ini_set('display_errors', 0);
ob_start();

require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    // Get POST data
    $user_id = $_POST['user_id'] ?? null;
    $device_token = $_POST['device_token'] ?? null;
    $device_type = $_POST['device_type'] ?? 'android';
    $app_version = $_POST['app_version'] ?? '1.0.0';
    
    // Validate input
    if (!$user_id || !$device_token) {
        throw new Exception('Missing required parameters: user_id, device_token');
    }
    
    $db = getDB();
    
    // Check if user exists and is active
    $stmt = $db->prepare("
        SELECT u.user_id 
        FROM users u 
        JOIN registration r ON u.reg_id = r.reg_id 
        WHERE u.user_id = ? AND u.status = 'Active' AND r.status = 'Approved'
    ");
    $stmt->execute([$user_id]);
    if (!$stmt->fetch()) {
        throw new Exception('User not found or not active');
    }
    
    // Check if device token already exists for this user
    $stmt = $db->prepare("
        SELECT token_id, is_active 
        FROM device_tokens 
        WHERE user_id = ? AND device_token = ?
    ");
    $stmt->execute([$user_id, $device_token]);
    $existing_token = $stmt->fetch();
    
    if ($existing_token) {
        // Update existing token
        if (!$existing_token['is_active']) {
            $stmt = $db->prepare("
                UPDATE device_tokens 
                SET is_active = 1, updated_at = NOW() 
                WHERE token_id = ?
            ");
            $stmt->execute([$existing_token['token_id']]);
        }
        
        $message = 'Device token updated successfully';
    } else {
        // Deactivate old tokens for this user
        $stmt = $db->prepare("
            UPDATE device_tokens 
            SET is_active = 0, updated_at = NOW() 
            WHERE user_id = ? AND is_active = 1
        ");
        $stmt->execute([$user_id]);
        
        // Insert new token
        $stmt = $db->prepare("
            INSERT INTO device_tokens (user_id, device_token, device_type, app_version, is_active, created_at, updated_at) 
            VALUES (?, ?, ?, ?, 1, NOW(), NOW())
        ");
        $stmt->execute([$user_id, $device_token, $device_type, $app_version]);
        
        $message = 'Device token registered successfully';
    }
    
    $response = [
        'success' => true,
        'message' => $message,
        'data' => [
            'user_id' => $user_id,
            'device_token' => $device_token,
            'device_type' => $device_type,
            'app_version' => $app_version,
            'timestamp' => date('Y-m-d H:i:s')
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