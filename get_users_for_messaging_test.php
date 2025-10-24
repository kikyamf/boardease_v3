<?php
// Test version of get_users_for_messaging.php
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
    
    // Get current user's role
    $stmt = $db->prepare("
        SELECT 
            r.role as user_role,
            r.first_name,
            r.last_name
        FROM users u
        JOIN registrations r ON u.reg_id = r.id
        WHERE u.user_id = ?
    ");
    $stmt->execute([$current_user_id]);
    $current_user = $stmt->fetch();
    
    if (!$current_user) {
        throw new Exception('Current user not found');
    }
    
    $formatted_users = [];
    
    if ($current_user['user_role'] === 'BH Owner') {
        // OWNER SIDE: Get boarders from their own boarding houses only
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
            FROM active_boarders ab
            JOIN users u ON ab.user_id = u.user_id
            JOIN registrations r ON u.reg_id = r.id
            JOIN boarding_houses bh ON ab.boarding_house_id = bh.bh_id
            LEFT JOIN device_tokens dt ON u.user_id = dt.user_id AND dt.is_active = 1
            WHERE ab.boarding_house_id IN (
                SELECT bh_id FROM boarding_houses WHERE user_id = ?
            )
            AND ab.user_id != ? 
            AND ab.status = 'active'
            AND r.role = 'Boarder' 
            AND r.status = 'approved'
            ORDER BY r.first_name ASC
        ");
        
        $stmt->execute([$current_user_id, $current_user_id]);
        $users = $stmt->fetchAll();
        
        // Format users for response
        foreach ($users as $user) {
            $user_data = [
                'user_id' => (int)$user['user_id'],
                'full_name' => $user['full_name'],
                'user_type' => $user['user_type'],
                'email' => $user['email'],
                'phone' => $user['phone'],
                'status' => $user['status'],
                'has_device_token' => (bool)$user['has_device_token'],
                'status_text' => $user['status_text'],
                'boarding_house_name' => $user['boarding_house_name'],
                'boarding_house_address' => $user['boarding_house_address'],
                'boarding_house_id' => (int)$user['boarding_house_id']
            ];
            
            $formatted_users[] = $user_data;
        }
        
    } else if ($current_user['user_role'] === 'Boarder') {
        // BOARDER SIDE: Get owner and other boarders from the same boarding house
        $stmt = $db->prepare("
            SELECT 
                ab.boarding_house_id,
                bh.bh_name,
                bh.bh_address,
                bh.user_id as owner_id
            FROM active_boarders ab
            JOIN boarding_houses bh ON ab.boarding_house_id = bh.bh_id
            WHERE ab.user_id = ? AND ab.status = 'active'
        ");
        $stmt->execute([$current_user_id]);
        $boarder_bh = $stmt->fetch();
        
        if ($boarder_bh) {
            // Get the owner of the boarding house
            $stmt = $db->prepare("
                SELECT 
                    u.user_id,
                    CONCAT(r.first_name, ' ', r.last_name) as full_name,
                    r.role as user_type,
                    r.email,
                    r.phone,
                    r.status,
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
                LEFT JOIN device_tokens dt ON u.user_id = dt.user_id AND dt.is_active = 1
                WHERE u.user_id = ?
                AND r.role = 'BH Owner'
                AND r.status = 'approved'
            ");
            $stmt->execute([$boarder_bh['owner_id']]);
            $owner = $stmt->fetch();
            
            if ($owner) {
                $formatted_users[] = [
                    'user_id' => (int)$owner['user_id'],
                    'full_name' => $owner['full_name'],
                    'user_type' => $owner['user_type'],
                    'email' => $owner['email'],
                    'phone' => $owner['phone'],
                    'status' => $owner['status'],
                    'has_device_token' => (bool)$owner['has_device_token'],
                    'status_text' => $owner['status_text'],
                    'boarding_house_name' => $boarder_bh['bh_name'],
                    'boarding_house_address' => $boarder_bh['bh_address'],
                    'boarding_house_id' => (int)$boarder_bh['bh_id']
                ];
            }
            
            // Get other boarders from the same boarding house
            $stmt = $db->prepare("
                SELECT 
                    u.user_id,
                    CONCAT(r.first_name, ' ', r.last_name) as full_name,
                    r.role as user_type,
                    r.email,
                    r.phone,
                    r.status,
                    CASE 
                        WHEN dt.device_token IS NOT NULL AND dt.is_active = 1 THEN 1 
                        ELSE 0 
                    END as has_device_token,
                    CASE 
                        WHEN dt.device_token IS NOT NULL AND dt.is_active = 1 THEN 'Online' 
                        ELSE 'Offline' 
                    END as status_text
                FROM active_boarders ab
                JOIN users u ON ab.user_id = u.user_id
                JOIN registrations r ON u.reg_id = r.id
                LEFT JOIN device_tokens dt ON u.user_id = dt.user_id AND dt.is_active = 1
                WHERE ab.boarding_house_id = ? 
                AND ab.user_id != ? 
                AND ab.status = 'active'
                AND r.role = 'Boarder' 
                AND r.status = 'approved'
                ORDER BY r.first_name ASC
            ");
            $stmt->execute([$boarder_bh['boarding_house_id'], $current_user_id]);
            $other_boarders = $stmt->fetchAll();
            
            foreach ($other_boarders as $boarder) {
                $formatted_users[] = [
                    'user_id' => (int)$boarder['user_id'],
                    'full_name' => $boarder['full_name'],
                    'user_type' => $boarder['user_type'],
                    'email' => $boarder['email'],
                    'phone' => $boarder['phone'],
                    'status' => $boarder['status'],
                    'has_device_token' => (bool)$boarder['has_device_token'],
                    'status_text' => $boarder['status_text'],
                    'boarding_house_name' => $boarder_bh['bh_name'],
                    'boarding_house_address' => $boarder_bh['bh_address'],
                    'boarding_house_id' => (int)$boarder_bh['bh_id']
                ];
            }
        }
    }
    
    $response = [
        'success' => true,
        'data' => [
            'users' => $formatted_users,
            'total_count' => count($formatted_users),
            'current_user_id' => (int)$current_user_id,
            'current_user_role' => $current_user['user_role'],
            'has_data' => count($formatted_users) > 0
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




