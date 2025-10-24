<?php
// Search users for messaging
error_reporting(0);
ini_set('display_errors', 0);
ob_start();

require_once '../db_helper.php';

header('Content-Type: application/json');

try {
    $current_user_id = $_GET['current_user_id'] ?? null;
    $search_term = $_GET['search_term'] ?? '';
    $user_type = $_GET['user_type'] ?? ''; // Optional filter by user type
    
    // Validate input
    if (!$current_user_id) {
        throw new Exception('Missing required parameter: current_user_id');
    }
    
    if (empty($search_term)) {
        throw new Exception('Search term cannot be empty');
    }
    
    $db = getDB();
    
    // Get current user's role first
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
    
    // Search users based on role and boarding house relationships
    if ($current_user['user_role'] === 'BH Owner') {
        // OWNER SIDE: Search only boarders from their own boarding houses
        $stmt = $db->prepare("
            SELECT 
                u.user_id,
                r.first_name,
                r.last_name,
                r.role as user_type,
                r.email,
                r.phone as phone,
                u.status,
                u.profile_picture,
                dt.device_token,
                bh.bh_name as boarding_house_name,
                bh.bh_address as boarding_house_address,
                bh.bh_id as boarding_house_id,
                CASE WHEN dt.device_token IS NOT NULL THEN 1 ELSE 0 END as is_online
            FROM users u
            JOIN registrations r ON u.reg_id = r.id
            JOIN active_boarders ab ON u.user_id = ab.user_id
            JOIN boarding_houses bh ON ab.boarding_house_id = bh.bh_id
            LEFT JOIN device_tokens dt ON u.user_id = dt.user_id AND dt.is_active = 1
            WHERE ab.boarding_house_id IN (
                SELECT bh_id FROM boarding_houses WHERE user_id = ?
            )
            AND ab.user_id != ?
            AND ab.status = 'Active'
            AND r.role = 'Boarder'
            AND r.status = 'approved'
            AND (r.first_name LIKE ? OR r.last_name LIKE ? OR CONCAT(r.first_name, ' ', r.last_name) LIKE ? OR r.email LIKE ?)
            ORDER BY 
                CASE 
                    WHEN r.first_name LIKE ? THEN 1
                    WHEN r.last_name LIKE ? THEN 2
                    WHEN CONCAT(r.first_name, ' ', r.last_name) LIKE ? THEN 3
                    ELSE 4
                END,
                is_online DESC,
                r.first_name ASC
            LIMIT 20
        ");
        
        $search_pattern = '%' . $search_term . '%';
        $search_exact = $search_term . '%';
        $stmt->execute([
            $current_user_id, $current_user_id, 
            $search_pattern, $search_pattern, $search_pattern, $search_pattern,
            $search_exact, $search_exact, $search_exact
        ]);
        $users = $stmt->fetchAll();
        
    } else if ($current_user['user_role'] === 'Boarder') {
        // BOARDER SIDE: Search owner and other boarders from same boarding house
        
        // First, find which boarding house this boarder is staying in
        $stmt = $db->prepare("
            SELECT
                ab.boarding_house_id,
                bh.bh_name,
                bh.bh_address,
                bh.user_id as owner_id
            FROM active_boarders ab
            JOIN boarding_houses bh ON ab.boarding_house_id = bh.bh_id
            WHERE ab.user_id = ? AND ab.status = 'Active'
        ");
        $stmt->execute([$current_user_id]);
        $boarder_bh = $stmt->fetch();
        
        if ($boarder_bh) {
            $stmt = $db->prepare("
                SELECT 
                    u.user_id,
                    r.first_name,
                    r.last_name,
                    r.role as user_type,
                    r.email,
                    r.phone as phone,
                    u.status,
                    u.profile_picture,
                    dt.device_token,
                    ? as boarding_house_name,
                    ? as boarding_house_address,
                    ? as boarding_house_id,
                    CASE WHEN dt.device_token IS NOT NULL THEN 1 ELSE 0 END as is_online
                FROM users u
                JOIN registrations r ON u.reg_id = r.id
                LEFT JOIN device_tokens dt ON u.user_id = dt.user_id AND dt.is_active = 1
                WHERE u.user_id != ?
                AND (u.user_id = ? OR 
                    (u.user_id IN (
                        SELECT ab2.user_id 
                        FROM active_boarders ab2 
                        WHERE ab2.boarding_house_id = ? 
                        AND ab2.user_id != ? 
                        AND ab2.status = 'Active'
                    ) AND r.role = 'Boarder' AND r.status = 'approved'))
                AND (r.first_name LIKE ? OR r.last_name LIKE ? OR CONCAT(r.first_name, ' ', r.last_name) LIKE ? OR r.email LIKE ?)
                ORDER BY 
                    CASE 
                        WHEN r.first_name LIKE ? THEN 1
                        WHEN r.last_name LIKE ? THEN 2
                        WHEN CONCAT(r.first_name, ' ', r.last_name) LIKE ? THEN 3
                        ELSE 4
                    END,
                    is_online DESC,
                    r.first_name ASC
                LIMIT 20
            ");
            
            $search_pattern = '%' . $search_term . '%';
            $search_exact = $search_term . '%';
            $stmt->execute([
                $boarder_bh['bh_name'], $boarder_bh['bh_address'], $boarder_bh['bh_id'],
                $current_user_id, $boarder_bh['owner_id'], 
                $boarder_bh['boarding_house_id'], $current_user_id,
                $search_pattern, $search_pattern, $search_pattern, $search_pattern,
                $search_exact, $search_exact, $search_exact
            ]);
            $users = $stmt->fetchAll();
        } else {
            // Boarder has no active boarding house - return empty
            $users = [];
        }
    } else {
        // Unknown role - return empty
        $users = [];
    }
    
    // Format users for response
    $formatted_users = [];
    foreach ($users as $user) {
        $formatted_users[] = [
            'user_id' => $user['user_id'],
            'first_name' => $user['first_name'],
            'last_name' => $user['last_name'],
            'full_name' => $user['first_name'] . ' ' . $user['last_name'],
            'user_type' => $user['user_type'],
            'email' => $user['email'],
            'phone' => $user['phone'],
            'status' => $user['status'],
            'profile_picture' => $user['profile_picture'],
            'has_device_token' => !empty($user['device_token']),
            'is_online' => (bool)$user['is_online'],
            'boarding_house_name' => $user['boarding_house_name'] ?? '',
            'boarding_house_address' => $user['boarding_house_address'] ?? '',
            'boarding_house_id' => $user['boarding_house_id'] ?? null
        ];
    }
    
    $response = [
        'success' => true,
        'data' => [
            'users' => $formatted_users,
            'search_term' => $search_term,
            'user_type_filter' => $user_type,
            'total_count' => count($formatted_users)
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


































