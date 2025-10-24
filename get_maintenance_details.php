<?php
// Get Maintenance Details API - Returns detailed information for a specific maintenance request
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
    // Get maintenance_id from request
    $maintenance_id = isset($_GET['maintenance_id']) ? intval($_GET['maintenance_id']) : 0;
    
    if ($maintenance_id <= 0) {
        throw new Exception("Invalid maintenance_id");
    }
    
    $db = getDB();
    
    // Get detailed maintenance request information
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
                r.email as boarder_email,
                r.phone_number as boarder_phone,
                r.university,
                r.student_id,
                ru.room_number as room_name,
                ru.room_description,
                ru.room_capacity,
                ru.room_amenities,
                bh.bh_name as boarding_house_name,
                bh.bh_address as boarding_house_address,
                bh.bh_contact as boarding_house_contact,
                CONCAT(owner_r.f_name, ' ', owner_r.l_name) as assigned_to_name,
                owner_r.phone_number as assigned_to_phone,
                owner_r.email as assigned_to_email
            FROM maintenance_requests mr
            JOIN users u ON mr.boarder_id = u.user_id
            JOIN registrations r ON u.reg_id = r.reg_id
            JOIN room_units ru ON mr.room_id = ru.room_id
            JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
            JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
            LEFT JOIN users owner_u ON mr.assigned_to = owner_u.user_id
            LEFT JOIN registrations owner_r ON owner_u.reg_id = owner_r.reg_id
            WHERE mr.maintenance_id = ?";
    
    $stmt = $db->prepare($sql);
    $stmt->execute([$maintenance_id]);
    $row = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$row) {
        throw new Exception("Maintenance request not found");
    }
    
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
    
    // Calculate time metrics
    $created = new DateTime($row['created_at']);
    $today = new DateTime();
    $days_since_request = $created->diff($today)->days;
    
    $work_duration = null;
    if ($work_started_date && $work_completed_date) {
        $started = new DateTime($work_started_date);
        $completed = new DateTime($work_completed_date);
        $work_duration = $started->diff($completed)->days;
    } elseif ($work_started_date) {
        $started = new DateTime($work_started_date);
        $work_duration = $started->diff($today)->days;
    }
    
    // Determine urgency status
    $urgency_status = 'normal';
    if ($row['priority'] === 'Urgent') {
        $urgency_status = 'urgent';
    } elseif ($row['priority'] === 'High' && $days_since_request > 2) {
        $urgency_status = 'overdue';
    } elseif ($days_since_request > 7) {
        $urgency_status = 'overdue';
    }
    
    // Determine status color/icon
    $status_info = [
        'Pending' => ['color' => 'orange', 'icon' => 'clock'],
        'In Progress' => ['color' => 'blue', 'icon' => 'wrench'],
        'Completed' => ['color' => 'green', 'icon' => 'check'],
        'Cancelled' => ['color' => 'red', 'icon' => 'x'],
        'On Hold' => ['color' => 'gray', 'icon' => 'pause']
    ];
    
    $current_status_info = $status_info[$row['status']] ?? ['color' => 'gray', 'icon' => 'question'];
    
    $maintenance_details = [
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
        'assigned_to_phone' => $row['assigned_to_phone'] ?? '',
        'assigned_to_email' => $row['assigned_to_email'] ?? '',
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
        'boarder_email' => $row['boarder_email'] ?? '',
        'boarder_phone' => $row['boarder_phone'] ?? '',
        'university' => $row['university'] ?? '',
        'student_id' => $row['student_id'] ?? '',
        'room_name' => $row['room_name'],
        'room_description' => $row['room_description'] ?? '',
        'room_capacity' => intval($row['room_capacity']),
        'room_amenities' => $row['room_amenities'] ?? '',
        'boarding_house_name' => $row['boarding_house_name'],
        'boarding_house_address' => $row['boarding_house_address'],
        'boarding_house_contact' => $row['boarding_house_contact'] ?? '',
        'request_info' => [
            'days_since_request' => $days_since_request,
            'urgency_status' => $urgency_status,
            'work_duration' => $work_duration,
            'status_info' => $current_status_info
        ]
    ];
    
    $response = [
        'success' => true,
        'data' => [
            'maintenance_details' => $maintenance_details
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





