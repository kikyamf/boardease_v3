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
    
    // Build search query
    $where_conditions = [
        'u.user_id != ?',
        '(r.f_name LIKE ? OR r.l_name LIKE ? OR CONCAT(r.f_name, " ", r.l_name) LIKE ? OR r.email LIKE ?)'
    ];
    
    $params = [
        $current_user_id,
        '%' . $search_term . '%',
        '%' . $search_term . '%',
        '%' . $search_term . '%',
        '%' . $search_term . '%'
    ];
    
    if (!empty($user_type)) {
        $where_conditions[] = 'r.role = ?';
        $params[] = $user_type;
    }
    
    $where_clause = implode(' AND ', $where_conditions);
    
    $stmt = $db->prepare("
        SELECT 
            u.user_id,
            r.f_name as first_name,
            r.l_name as last_name,
            r.role as user_type,
            r.email,
            r.phone_number as phone,
            u.status,
            u.profile_picture,
            dt.device_token,
            CASE WHEN dt.device_token IS NOT NULL THEN 1 ELSE 0 END as is_online
        FROM users u
        JOIN registration r ON u.reg_id = r.reg_id
        LEFT JOIN device_tokens dt ON u.user_id = dt.user_id AND dt.is_active = 1
        WHERE {$where_clause}
        AND u.status = 'Active' AND r.status = 'Approved'
        ORDER BY 
            CASE 
                WHEN r.f_name LIKE ? THEN 1
                WHEN r.l_name LIKE ? THEN 2
                WHEN CONCAT(r.f_name, ' ', r.l_name) LIKE ? THEN 3
                ELSE 4
            END,
            is_online DESC,
            r.f_name ASC
        LIMIT 20
    ");
    
    // Add search term for ordering
    $params[] = $search_term . '%';
    $params[] = $search_term . '%';
    $params[] = $search_term . '%';
    
    $stmt->execute($params);
    $users = $stmt->fetchAll();
    
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
            'is_online' => $user['is_online'],
            'status' => $user['status'],
            'profile_picture' => $user['profile_picture']
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






















