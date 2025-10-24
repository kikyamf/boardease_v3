<?php
// Get Maintenance Summary API - Returns maintenance statistics and summary for a user
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
    $period = isset($_GET['period']) ? $_GET['period'] : 'all'; // 'all', 'month', 'year'
    
    if ($user_id <= 0) {
        throw new Exception("Invalid user_id");
    }
    
    $db = getDB();
    
    // Build date filter condition
    $date_condition = "";
    switch ($period) {
        case 'month':
            $date_condition = "AND mr.created_at >= DATE_SUB(NOW(), INTERVAL 1 MONTH)";
            break;
        case 'year':
            $date_condition = "AND mr.created_at >= DATE_SUB(NOW(), INTERVAL 1 YEAR)";
            break;
    }
    
    if ($user_type === 'owner') {
        // Get maintenance summary for owner
        $sql = "SELECT 
                    COUNT(*) as total_requests,
                    SUM(CASE WHEN mr.status = 'Pending' THEN 1 ELSE 0 END) as pending_count,
                    SUM(CASE WHEN mr.status = 'In Progress' THEN 1 ELSE 0 END) as in_progress_count,
                    SUM(CASE WHEN mr.status = 'Completed' THEN 1 ELSE 0 END) as completed_count,
                    SUM(CASE WHEN mr.status = 'Cancelled' THEN 1 ELSE 0 END) as cancelled_count,
                    SUM(CASE WHEN mr.status = 'On Hold' THEN 1 ELSE 0 END) as on_hold_count,
                    SUM(CASE WHEN mr.priority = 'Urgent' THEN 1 ELSE 0 END) as urgent_count,
                    SUM(CASE WHEN mr.priority = 'High' THEN 1 ELSE 0 END) as high_count,
                    SUM(CASE WHEN mr.priority = 'Medium' THEN 1 ELSE 0 END) as medium_count,
                    SUM(CASE WHEN mr.priority = 'Low' THEN 1 ELSE 0 END) as low_count,
                    SUM(CASE WHEN mr.maintenance_type = 'Plumbing' THEN 1 ELSE 0 END) as plumbing_count,
                    SUM(CASE WHEN mr.maintenance_type = 'Electrical' THEN 1 ELSE 0 END) as electrical_count,
                    SUM(CASE WHEN mr.maintenance_type = 'HVAC' THEN 1 ELSE 0 END) as hvac_count,
                    SUM(CASE WHEN mr.maintenance_type = 'Furniture' THEN 1 ELSE 0 END) as furniture_count,
                    SUM(CASE WHEN mr.maintenance_type = 'Appliance' THEN 1 ELSE 0 END) as appliance_count,
                    SUM(CASE WHEN mr.maintenance_type = 'Security' THEN 1 ELSE 0 END) as security_count,
                    SUM(CASE WHEN mr.maintenance_type = 'Cleaning' THEN 1 ELSE 0 END) as cleaning_count,
                    SUM(CASE WHEN mr.maintenance_type = 'Other' THEN 1 ELSE 0 END) as other_count,
                    AVG(CASE WHEN mr.feedback_rating IS NOT NULL THEN mr.feedback_rating ELSE NULL END) as average_rating,
                    SUM(CASE WHEN mr.feedback_rating IS NOT NULL THEN 1 ELSE 0 END) as feedback_count,
                    SUM(CASE WHEN mr.estimated_cost IS NOT NULL THEN mr.estimated_cost ELSE 0 END) as total_estimated_cost,
                    SUM(CASE WHEN mr.actual_cost IS NOT NULL THEN mr.actual_cost ELSE 0 END) as total_actual_cost,
                    AVG(CASE WHEN mr.work_started_date IS NOT NULL AND mr.work_completed_date IS NOT NULL 
                        THEN DATEDIFF(mr.work_completed_date, mr.work_started_date) ELSE NULL END) as average_completion_days,
                    AVG(CASE WHEN mr.work_started_date IS NOT NULL 
                        THEN DATEDIFF(mr.work_started_date, mr.created_at) ELSE NULL END) as average_response_days
                FROM maintenance_requests mr
                WHERE mr.owner_id = ? $date_condition";
        
        $stmt = $db->prepare($sql);
        $stmt->execute([$user_id]);
        
    } else {
        // Get maintenance summary for boarder
        $sql = "SELECT 
                    COUNT(*) as total_requests,
                    SUM(CASE WHEN mr.status = 'Pending' THEN 1 ELSE 0 END) as pending_count,
                    SUM(CASE WHEN mr.status = 'In Progress' THEN 1 ELSE 0 END) as in_progress_count,
                    SUM(CASE WHEN mr.status = 'Completed' THEN 1 ELSE 0 END) as completed_count,
                    SUM(CASE WHEN mr.status = 'Cancelled' THEN 1 ELSE 0 END) as cancelled_count,
                    SUM(CASE WHEN mr.status = 'On Hold' THEN 1 ELSE 0 END) as on_hold_count,
                    SUM(CASE WHEN mr.priority = 'Urgent' THEN 1 ELSE 0 END) as urgent_count,
                    SUM(CASE WHEN mr.priority = 'High' THEN 1 ELSE 0 END) as high_count,
                    SUM(CASE WHEN mr.priority = 'Medium' THEN 1 ELSE 0 END) as medium_count,
                    SUM(CASE WHEN mr.priority = 'Low' THEN 1 ELSE 0 END) as low_count,
                    SUM(CASE WHEN mr.maintenance_type = 'Plumbing' THEN 1 ELSE 0 END) as plumbing_count,
                    SUM(CASE WHEN mr.maintenance_type = 'Electrical' THEN 1 ELSE 0 END) as electrical_count,
                    SUM(CASE WHEN mr.maintenance_type = 'HVAC' THEN 1 ELSE 0 END) as hvac_count,
                    SUM(CASE WHEN mr.maintenance_type = 'Furniture' THEN 1 ELSE 0 END) as furniture_count,
                    SUM(CASE WHEN mr.maintenance_type = 'Appliance' THEN 1 ELSE 0 END) as appliance_count,
                    SUM(CASE WHEN mr.maintenance_type = 'Security' THEN 1 ELSE 0 END) as security_count,
                    SUM(CASE WHEN mr.maintenance_type = 'Cleaning' THEN 1 ELSE 0 END) as cleaning_count,
                    SUM(CASE WHEN mr.maintenance_type = 'Other' THEN 1 ELSE 0 END) as other_count,
                    AVG(CASE WHEN mr.feedback_rating IS NOT NULL THEN mr.feedback_rating ELSE NULL END) as average_rating,
                    SUM(CASE WHEN mr.feedback_rating IS NOT NULL THEN 1 ELSE 0 END) as feedback_count,
                    SUM(CASE WHEN mr.estimated_cost IS NOT NULL THEN mr.estimated_cost ELSE 0 END) as total_estimated_cost,
                    SUM(CASE WHEN mr.actual_cost IS NOT NULL THEN mr.actual_cost ELSE 0 END) as total_actual_cost,
                    AVG(CASE WHEN mr.work_started_date IS NOT NULL AND mr.work_completed_date IS NOT NULL 
                        THEN DATEDIFF(mr.work_completed_date, mr.work_started_date) ELSE NULL END) as average_completion_days,
                    AVG(CASE WHEN mr.work_started_date IS NOT NULL 
                        THEN DATEDIFF(mr.work_started_date, mr.created_at) ELSE NULL END) as average_response_days
                FROM maintenance_requests mr
                WHERE mr.boarder_id = ? $date_condition";
        
        $stmt = $db->prepare($sql);
        $stmt->execute([$user_id]);
    }
    
    $summary = $stmt->fetch(PDO::FETCH_ASSOC);
    
    // Calculate additional metrics
    $total_requests = intval($summary['total_requests']);
    $pending_count = intval($summary['pending_count']);
    $in_progress_count = intval($summary['in_progress_count']);
    $completed_count = intval($summary['completed_count']);
    $cancelled_count = intval($summary['cancelled_count']);
    $on_hold_count = intval($summary['on_hold_count']);
    
    $urgent_count = intval($summary['urgent_count']);
    $high_count = intval($summary['high_count']);
    $medium_count = intval($summary['medium_count']);
    $low_count = intval($summary['low_count']);
    
    $plumbing_count = intval($summary['plumbing_count']);
    $electrical_count = intval($summary['electrical_count']);
    $hvac_count = intval($summary['hvac_count']);
    $furniture_count = intval($summary['furniture_count']);
    $appliance_count = intval($summary['appliance_count']);
    $security_count = intval($summary['security_count']);
    $cleaning_count = intval($summary['cleaning_count']);
    $other_count = intval($summary['other_count']);
    
    $average_rating = floatval($summary['average_rating']);
    $feedback_count = intval($summary['feedback_count']);
    $total_estimated_cost = floatval($summary['total_estimated_cost']);
    $total_actual_cost = floatval($summary['total_actual_cost']);
    $average_completion_days = floatval($summary['average_completion_days']);
    $average_response_days = floatval($summary['average_response_days']);
    
    $completion_rate = $total_requests > 0 ? ($completed_count / $total_requests) * 100 : 0;
    $cancellation_rate = $total_requests > 0 ? ($cancelled_count / $total_requests) * 100 : 0;
    $feedback_rate = $completed_count > 0 ? ($feedback_count / $completed_count) * 100 : 0;
    
    // Get recent maintenance requests (last 5)
    $recent_sql = "SELECT 
                    mr.maintenance_id,
                    mr.title,
                    mr.maintenance_type,
                    mr.priority,
                    mr.status,
                    mr.created_at,
                    CONCAT(r.f_name, ' ', r.l_name) as boarder_name,
                    ru.room_number as room_name,
                    bh.bh_name as boarding_house_name
                FROM maintenance_requests mr
                JOIN users u ON mr.boarder_id = u.user_id
                JOIN registrations r ON u.reg_id = r.reg_id
                JOIN room_units ru ON mr.room_id = ru.room_id
                JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
                JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
                WHERE " . ($user_type === 'owner' ? "mr.owner_id = ?" : "mr.boarder_id = ?") . " $date_condition
                ORDER BY mr.created_at DESC
                LIMIT 5";
    
    $recent_stmt = $db->prepare($recent_sql);
    $recent_stmt->execute([$user_id]);
    
    $recent_requests = [];
    while ($row = $recent_stmt->fetch(PDO::FETCH_ASSOC)) {
        $recent_requests[] = [
            'maintenance_id' => intval($row['maintenance_id']),
            'title' => $row['title'],
            'maintenance_type' => $row['maintenance_type'],
            'priority' => $row['priority'],
            'status' => $row['status'],
            'created_at' => date('Y-m-d H:i:s', strtotime($row['created_at'])),
            'boarder_name' => $row['boarder_name'],
            'room_name' => $row['room_name'],
            'boarding_house_name' => $row['boarding_house_name']
        ];
    }
    
    $response = [
        'success' => true,
        'data' => [
            'summary' => [
                'total_requests' => $total_requests,
                'pending_count' => $pending_count,
                'in_progress_count' => $in_progress_count,
                'completed_count' => $completed_count,
                'cancelled_count' => $cancelled_count,
                'on_hold_count' => $on_hold_count,
                'urgent_count' => $urgent_count,
                'high_count' => $high_count,
                'medium_count' => $medium_count,
                'low_count' => $low_count,
                'plumbing_count' => $plumbing_count,
                'electrical_count' => $electrical_count,
                'hvac_count' => $hvac_count,
                'furniture_count' => $furniture_count,
                'appliance_count' => $appliance_count,
                'security_count' => $security_count,
                'cleaning_count' => $cleaning_count,
                'other_count' => $other_count,
                'average_rating' => round($average_rating, 2),
                'feedback_count' => $feedback_count,
                'total_estimated_cost' => "P" . number_format($total_estimated_cost, 2),
                'total_actual_cost' => "P" . number_format($total_actual_cost, 2),
                'average_completion_days' => round($average_completion_days, 1),
                'average_response_days' => round($average_response_days, 1),
                'completion_rate' => round($completion_rate, 2),
                'cancellation_rate' => round($cancellation_rate, 2),
                'feedback_rate' => round($feedback_rate, 2)
            ],
            'recent_requests' => $recent_requests,
            'period' => $period
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





