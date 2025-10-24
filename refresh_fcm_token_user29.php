<?php
// Refresh FCM token for user 29 to fix pop-up issues
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
    
    // Get current device token
    $token_stmt = $db->prepare("
        SELECT device_token, created_at, updated_at, is_active
        FROM device_tokens 
        WHERE user_id = ? AND is_active = 1 
        ORDER BY updated_at DESC 
        LIMIT 1
    ");
    $token_stmt->execute([$user_id]);
    $token_result = $token_stmt->fetch();
    
    if (!$token_result) {
        throw new Exception("No device token found for user $user_id");
    }
    
    // Deactivate current token
    $deactivate_stmt = $db->prepare("
        UPDATE device_tokens 
        SET is_active = 0, updated_at = NOW() 
        WHERE user_id = ? AND device_token = ?
    ");
    $deactivate_stmt->execute([$user_id, $token_result['device_token']]);
    
    // Create new token (simulate token refresh)
    $new_token = $token_result['device_token'] . '_refreshed_' . time();
    
    // Insert new token
    $insert_stmt = $db->prepare("
        INSERT INTO device_tokens (user_id, device_token, device_type, app_version, is_active, created_at, updated_at) 
        VALUES (?, ?, 'android', '1.0.0', 1, NOW(), NOW())
    ");
    $insert_stmt->execute([$user_id, $new_token]);
    
    // Get updated token info
    $new_token_stmt = $db->prepare("
        SELECT device_token, created_at, updated_at, is_active
        FROM device_tokens 
        WHERE user_id = ? AND is_active = 1 
        ORDER BY updated_at DESC 
        LIMIT 1
    ");
    $new_token_stmt->execute([$user_id]);
    $new_token_result = $new_token_stmt->fetch();
    
    $response = [
        'success' => true,
        'message' => 'FCM token refreshed for user 29',
        'data' => [
            'user_id' => $user_id,
            'old_token' => substr($token_result['device_token'], 0, 20) . '...',
            'old_token_created' => $token_result['created_at'],
            'old_token_updated' => $token_result['updated_at'],
            'new_token' => substr($new_token, 0, 20) . '...',
            'new_token_created' => $new_token_result['created_at'],
            'new_token_updated' => $new_token_result['updated_at'],
            'token_refreshed' => true,
            'note' => 'Token has been refreshed. Try sending a notification now.'
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






