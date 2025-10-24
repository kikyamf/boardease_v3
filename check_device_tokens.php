<?php
// Check device tokens in database
require_once 'db_helper.php';

header('Content-Type: application/json');

try {
    $db = getDB();
    
    // Check if device_tokens table exists and has data
    $stmt = $db->prepare("SELECT COUNT(*) as count FROM device_tokens");
    $stmt->execute();
    $result = $stmt->fetch();
    
    $total_tokens = $result['count'];
    
    // Get all device tokens
    $stmt = $db->prepare("
        SELECT 
            dt.token_id,
            dt.user_id,
            r.f_name,
            r.l_name,
            dt.device_token,
            dt.device_type,
            dt.is_active,
            dt.created_at
        FROM device_tokens dt
        JOIN users u ON dt.user_id = u.user_id
        JOIN registration r ON u.reg_id = r.reg_id
        ORDER BY dt.created_at DESC
    ");
    $stmt->execute();
    $tokens = $stmt->fetchAll();
    
    // Check specific users from your test
    $test_users = [6, 11]; // David Brown (6) and Namz Baer (11)
    $user_tokens = [];
    
    foreach ($test_users as $user_id) {
        $stmt = $db->prepare("
            SELECT 
                dt.device_token,
                dt.is_active,
                r.f_name,
                r.l_name
            FROM device_tokens dt
            JOIN users u ON dt.user_id = u.user_id
            JOIN registration r ON u.reg_id = r.reg_id
            WHERE dt.user_id = ? AND dt.is_active = 1
        ");
        $stmt->execute([$user_id]);
        $user_token = $stmt->fetch();
        
        $user_tokens[$user_id] = $user_token;
    }
    
    $response = [
        'success' => true,
        'data' => [
            'total_device_tokens' => $total_tokens,
            'all_tokens' => $tokens,
            'test_users_tokens' => $user_tokens,
            'analysis' => [
                'david_brown_token' => $user_tokens[6] ? 'Found' : 'Missing',
                'namz_baer_token' => $user_tokens[11] ? 'Found' : 'Missing',
                'notification_issue' => $total_tokens == 0 ? 'No device tokens registered' : 'Some tokens exist but may not be for test users'
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

echo json_encode($response, JSON_PRETTY_PRINT);
?>























