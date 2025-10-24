<?php
// Get Maintenance Requests API - Returns maintenance requests for owners and boarders
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type");

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

require_once 'db_helper.php';

$response = [];

try {
    // Get user_id from request
    $user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;
    $user_type = isset($_GET['user_type']) ? $_GET['user_type'] : 'owner'; // 'owner' or 'boarder'
    $status_filter = isset($_GET['status']) ? $_GET['status'] : 'all'; // 'all', 'pending', 'in_progress', 'completed', 'cancelled'
    $priority_filter = isset($_GET['priority']) ? $_GET['priority'] : 'all'; // 'all', 'low', 'medium', 'high', 'urgent'
    $type_filter = isset($_GET['type']) ? $_GET['type'] : 'all'; // 'all', 'plumbing', 'electrical', etc.
    $limit = isset($_GET['limit']) ? intval($_GET['limit']) : 50;
    $offset = isset($_GET['offset']) ? intval($_GET['offset']) : 0;
    
    if ($user_id <= 0) {
        throw new Exception("Invalid user_id");
    }
    
    $db = getDB();
    
    // Build status filter condition
    $status_condition = "";
    if ($status_filter !== 'all') {
        $status_condition = "AND mr.status = '" . ucfirst($status_filter) . "'";
    }
    
    // Build priority filter condition
    $priority_condition = "";
    if ($priority_filter !== 'all') {
        $priority_condition = "AND mr.priority = '" . ucfirst($priority_filter) . "'";
    }
    
    // Build type filter condition
    $type_condition = "";
    if ($type_filter !== 'all') {
        $type_condition = "AND mr.maintenance_type = '" . ucfirst($type_filter) . "'";
    }
    
    if ($user_type === 'owner') {
        // Get maintenance requests for owner
        $sql = "SELECT 
                    mr.maintenance_id,
                    mr.boarder_id,
                    mr.room_id,
                    mr.booking_id,
                    mr.owner_id,
                    mr.maintenance_type,
                    mr.priority,
                    mr.title,
                    mr.description,
                    mr.location,
                    mr.images,
                    mr.preferred_date,
                    mr.preferred_time,
                    mr.contact_phone,
                    mr.status,
                    mr.assigned_to,
                    mr.estimated_cost,
                    mr.actual_cost,
                    mr.work_started_date,
                    mr.work_completed_date,
                    mr.notes,
                    mr.feedback_rating,
                    mr.feedback_comment,
                    mr.created_at,
                    mr.updated_at,
                    CONCAT(r.f_name, ' ', r.l_name) as boarder_name,
                    r.phone_number as boarder_phone,
                    ru.room_number as room_name,
                    bh.bh_name as boarding_house_name,
                    bh.bh_address as boarding_house_address,
                    CONCAT(owner_r.f_name, ' ', owner_r.l_name) as assigned_to_name
                FROM maintenance_requests mr
                JOIN users u ON mr.boarder_id = u.user_id
                JOIN registrations r ON u.reg_id = r.reg_id
                JOIN room_units ru ON mr.room_id = ru.room_id
                JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
                JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
                LEFT JOIN users owner_u ON mr.assigned_to = owner_u.user_id
                LEFT JOIN registrations owner_r ON owner_u.reg_id = owner_r.reg_id
                WHERE mr.owner_id = ? $status_condition $priority_condition $type_condition
                ORDER BY 
                    CASE mr.priority 
                        WHEN 'Urgent' THEN 1 
                        WHEN 'High' THEN 2 
                        WHEN 'Medium' THEN 3 
                        WHEN 'Low' THEN 4 
                    END,
                    mr.created_at DESC
                LIMIT ? OFFSET ?";
        
        $stmt = $db->prepare($sql);
        $stmt->execute([$user_id, $limit, $offset]);
        
    } else {
        // Get maintenance requests for boarder
        $sql = "SELECT 
                    mr.maintenance_id,
                    mr.boarder_id,
                    mr.room_id,
                    mr.booking_id,
                    mr.owner_id,
                    mr.maintenance_type,
                    mr.priority,
                    mr.title,
                    mr.description,
                    mr.location,
                    mr.images,
                    mr.preferred_date,
                    mr.preferred_time,
                    mr.contact_phone,
                    mr.status,
                    mr.assigned_to,
                    mr.estimated_cost,
                    mr.actual_cost,
                    mr.work_started_date,
                    mr.work_completed_date,
                    mr.notes,
                    mr.feedback_rating,
                    mr.feedback_comment,
                    mr.created_at,
                    mr.updated_at,
                    CONCAT(r.f_name, ' ', r.l_name) as boarder_name,
                    r.phone_number as boarder_phone,
                    ru.room_number as room_name,
                    bh.bh_name as boarding_house_name,
                    bh.bh_address as boarding_house_address,
                    CONCAT(owner_r.f_name, ' ', owner_r.l_name) as assigned_to_name
                FROM maintenance_requests mr
                JOIN users u ON mr.boarder_id = u.user_id
                JOIN registrations r ON u.reg_id = r.reg_id
                JOIN room_units ru ON mr.room_id = ru.room_id
                JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
                JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
                LEFT JOIN users owner_u ON mr.assigned_to = owner_u.user_id
                LEFT JOIN registrations owner_r ON owner_u.reg_id = owner_r.reg_id
                WHERE mr.boarder_id = ? $status_condition $priority_condition $type_condition
                ORDER BY 
                    CASE mr.priority 
                        WHEN 'Urgent' THEN 1 
                        WHEN 'High' THEN 2 
                        WHEN 'Medium' THEN 3 
                        WHEN 'Low' THEN 4 
                    END,
                    mr.created_at DESC
                LIMIT ? OFFSET ?";
        
        $stmt = $db->prepare($sql);
        $stmt->execute([$user_id, $limit, $offset]);
    }
    
    $maintenance_requests = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        // Parse images
        $images = [];
        if ($row['images']) {
            $images = json_decode($row['images'], true) ?: [];
        }
        
        // Format dates
        $preferred_date = $row['preferred_date'] ? date('Y-m-d', strtotime($row['preferred_date'])) : null;
        $work_started_date = $row['work_started_date'] ? date('Y-m-d H:i:s', strtotime($row['work_started_date'])) : null;
        $work_completed_date = $row['work_completed_date'] ? date('Y-m-d H:i:s', strtotime($row['work_completed_date'])) : null;
        $created_at = date('Y-m-d H:i:s', strtotime($row['created_at']));
        $updated_at = date('Y-m-d H:i:s', strtotime($row['updated_at']));
        
        // Format costs
        $estimated_cost = $row['estimated_cost'] ? "P" . number_format($row['estimated_cost'], 2) : null;
        $actual_cost = $row['actual_cost'] ? "P" . number_format($row['actual_cost'], 2) : null;
        
        // Calculate days since request
        $created = new DateTime($row['created_at']);
        $today = new DateTime();
        $days_since_request = $created->diff($today)->days;
        
        // Determine urgency status
        $urgency_status = 'normal';
        if ($row['priority'] === 'Urgent') {
            $urgency_status = 'urgent';
        } elseif ($row['priority'] === 'High' && $days_since_request > 2) {
            $urgency_status = 'overdue';
        } elseif ($days_since_request > 7) {
            $urgency_status = 'overdue';
        }
        
        $maintenance_requests[] = [
            'maintenance_id' => intval($row['maintenance_id']),
            'boarder_id' => intval($row['boarder_id']),
            'room_id' => intval($row['room_id']),
            'booking_id' => intval($row['booking_id']),
            'owner_id' => intval($row['owner_id']),
            'maintenance_type' => $row['maintenance_type'],
            'priority' => $row['priority'],
            'title' => $row['title'],
            'description' => $row['description'],
            'location' => $row['location'] ?? '',
            'images' => $images,
            'preferred_date' => $preferred_date,
            'preferred_time' => $row['preferred_time'] ?? '',
            'contact_phone' => $row['contact_phone'] ?? '',
            'status' => $row['status'],
            'assigned_to' => $row['assigned_to'] ? intval($row['assigned_to']) : null,
            'assigned_to_name' => $row['assigned_to_name'] ?? '',
            'estimated_cost' => $estimated_cost,
            'actual_cost' => $actual_cost,
            'work_started_date' => $work_started_date,
            'work_completed_date' => $work_completed_date,
            'notes' => $row['notes'] ?? '',
            'feedback_rating' => $row['feedback_rating'] ? intval($row['feedback_rating']) : null,
            'feedback_comment' => $row['feedback_comment'] ?? '',
            'created_at' => $created_at,
            'updated_at' => $updated_at,
            'boarder_name' => $row['boarder_name'],
            'boarder_phone' => $row['boarder_phone'] ?? '',
            'room_name' => $row['room_name'],
            'boarding_house_name' => $row['boarding_house_name'],
            'boarding_house_address' => $row['boarding_house_address'],
            'request_info' => [
                'days_since_request' => $days_since_request,
                'urgency_status' => $urgency_status
            ]
        ];
    }
    
    // Get total count for pagination
    $count_sql = str_replace("LIMIT ? OFFSET ?", "", $sql);
    $count_sql = str_replace("ORDER BY CASE mr.priority WHEN 'Urgent' THEN 1 WHEN 'High' THEN 2 WHEN 'Medium' THEN 3 WHEN 'Low' THEN 4 END, mr.created_at DESC", "", $count_sql);
    $count_stmt = $db->prepare($count_sql);
    $count_stmt->execute([$user_id]);
    $total_count = $count_stmt->rowCount();
    
    $response = [
        'success' => true,
        'data' => [
            'maintenance_requests' => $maintenance_requests,
            'total_count' => $total_count,
            'limit' => $limit,
            'offset' => $offset,
            'has_more' => ($offset + $limit) < $total_count,
            'filters' => [
                'status' => $status_filter,
                'priority' => $priority_filter,
                'type' => $type_filter
            ]
        ]
    ];
    
} catch (Exception $e) {
    $response = [
        'success' => false,
        'error' => $e->getMessage()
    ];
}

echo json_encode($response, JSON_UNESCAPED_SLASHES);
?>





