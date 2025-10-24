<?php
// Get users for group chat creation with boarding house information
error_reporting(0);
ini_set('display_errors', 0);
ob_start();
require_once 'db_helper.php';
header('Content-Type: application/json');

try {
    $current_user_id = $_GET['current_user_id'] ?? null;
    
    if (!$current_user_id) {
        throw new Exception('Missing required parameter: current_user_id');
    }
    
    $db = getDB();
    
    // Get current user's role and boarding house info
    $stmt = $db->prepare("
        SELECT 
            r.role as user_role,
            bh.bh_id as owner_bh_id,
            bh.bh_name as owner_bh_name
        FROM users u
        JOIN registrations r ON u.reg_id = r.id
        LEFT JOIN boarding_houses bh ON u.user_id = bh.user_id
        WHERE u.user_id = ?
    ");
    $stmt->execute([$current_user_id]);
    $current_user = $stmt->fetch();
    
    if (!$current_user) {
        throw new Exception('Current user not found');
    }
    
    // Check if current user is an owner
    if ($current_user['user_role'] !== 'BH Owner') {
        throw new Exception('Only boarding house owners can create group chats');
    }
    
    // Get boarders from the owner's boarding houses only
    $stmt = $db->prepare("
        SELECT 
            u.user_id,
            CONCAT(r.first_name, ' ', r.last_name) as full_name,
            r.role as user_type,
            r.email,
            r.phone,
            r.status,
            bh.bh_name as boarding_house_name,
            bh.bh_address as boarding_house_address,
            bh.bh_id as boarding_house_id,
            CASE 
                WHEN dt.device_token IS NOT NULL AND dt.is_active = 1 THEN 1 
                ELSE 0 
            END as has_device_token,
            CASE 
                WHEN dt.device_token IS NOT NULL AND dt.is_active = 1 THEN 'Online' 
                ELSE 'Offline' 
            END as status_text
        FROM users u
        JOIN registrations r ON u.reg_id = r.id
        JOIN boarding_houses bh ON u.user_id = bh.user_id
        LEFT JOIN device_tokens dt ON u.user_id = dt.user_id AND dt.is_active = 1
        WHERE u.user_id != ? 
        AND r.role = 'Boarder' 
        AND r.status = 'approved'
        AND bh.user_id = ?
        ORDER BY r.first_name ASC
    ");
    
    $stmt->execute([$current_user_id, $current_user_id]);
    $users = $stmt->fetchAll();
    
    // Format users for response
    $formatted_users = [];
    foreach ($users as $user) {
        $user_data = [
            'user_id' => (int)$user['user_id'],
            'full_name' => $user['full_name'],
            'user_type' => $user['user_type'],
            'email' => $user['email'],
            'phone' => $user['phone'],
            'status' => $user['status'],
            'has_device_token' => (bool)$user['has_device_token'],
            'status_text' => $user['status_text']
        ];
        
        // Add boarding house information for boarders
        if ($user['user_type'] === 'Boarder' && !empty($user['boarding_house_name'])) {
            $user_data['boarding_house_name'] = $user['boarding_house_name'];
            $user_data['boarding_house_address'] = $user['boarding_house_address'];
            $user_data['boarding_house_id'] = (int)$user['boarding_house_id'];
        }
        
        $formatted_users[] = $user_data;
    }
    
    $response = [
        'success' => true,
        'data' => [
            'users' => $formatted_users,
            'total_count' => count($formatted_users),
            'current_user_id' => (int)$current_user_id
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
