<?php
// Get Boarders Summary API - Returns boarders statistics and summary for a user
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
    $period = isset($_GET['period']) ? $_GET['period'] : 'all'; // 'all', 'month', 'year'
    
    if ($user_id <= 0) {
        throw new Exception("Invalid user_id");
    }
    
    $db = getDB();
    
    // Build date filter condition
    $date_condition = "";
    switch ($period) {
        case 'month':
            $date_condition = "AND b.start_date >= DATE_SUB(NOW(), INTERVAL 1 MONTH)";
            break;
        case 'year':
            $date_condition = "AND b.start_date >= DATE_SUB(NOW(), INTERVAL 1 YEAR)";
            break;
    }
    
    // Get boarders summary for owner
    $sql = "SELECT 
                COUNT(*) as total_boarders,
                SUM(CASE WHEN b.booking_status = 'Approved' AND b.end_date > NOW() THEN 1 ELSE 0 END) as current_boarders,
                SUM(CASE WHEN b.booking_status = 'Completed' THEN 1 ELSE 0 END) as completed_boarders,
                SUM(CASE WHEN b.booking_status = 'Cancelled' THEN 1 ELSE 0 END) as cancelled_boarders,
                SUM(CASE WHEN b.booking_status = 'Approved' AND b.end_date <= DATE_ADD(NOW(), INTERVAL 7 DAY) AND b.end_date > NOW() THEN 1 ELSE 0 END) as checking_out_soon,
                SUM(CASE WHEN b.payment_status = 'Overdue' THEN 1 ELSE 0 END) as payment_overdue,
                SUM(CASE WHEN b.check_in_date IS NOT NULL AND b.check_out_date IS NULL THEN 1 ELSE 0 END) as checked_in,
                SUM(CASE WHEN b.rental_agreement_signed = 1 THEN 1 ELSE 0 END) as agreements_signed,
                AVG(CASE WHEN b.booking_status = 'Completed' THEN DATEDIFF(b.end_date, b.start_date) ELSE NULL END) as average_stay_days,
                SUM(CASE WHEN b.booking_status = 'Approved' AND b.end_date > NOW() THEN bhr.room_price ELSE 0 END) as current_monthly_revenue,
                SUM(CASE WHEN b.payment_status = 'Paid' THEN bhr.room_price ELSE 0 END) as total_paid_revenue,
                SUM(bhr.room_price) as total_expected_revenue,
                MIN(b.start_date) as first_boarder_date,
                MAX(b.start_date) as last_boarder_date
            FROM bookings b
            JOIN room_units ru ON b.room_id = ru.room_id
            JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
            JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
            WHERE bh.user_id = ? $date_condition";
    
    $stmt = $db->prepare($sql);
    $stmt->execute([$user_id]);
    $summary = $stmt->fetch(PDO::FETCH_ASSOC);
    
    // Calculate additional metrics
    $total_boarders = intval($summary['total_boarders']);
    $current_boarders = intval($summary['current_boarders']);
    $completed_boarders = intval($summary['completed_boarders']);
    $cancelled_boarders = intval($summary['cancelled_boarders']);
    $checking_out_soon = intval($summary['checking_out_soon']);
    $payment_overdue = intval($summary['payment_overdue']);
    $checked_in = intval($summary['checked_in']);
    $agreements_signed = intval($summary['agreements_signed']);
    
    $average_stay_days = floatval($summary['average_stay_days']);
    $current_monthly_revenue = floatval($summary['current_monthly_revenue']);
    $total_paid_revenue = floatval($summary['total_paid_revenue']);
    $total_expected_revenue = floatval($summary['total_expected_revenue']);
    
    $occupancy_rate = $total_boarders > 0 ? ($current_boarders / $total_boarders) * 100 : 0;
    $completion_rate = $total_boarders > 0 ? ($completed_boarders / $total_boarders) * 100 : 0;
    $cancellation_rate = $total_boarders > 0 ? ($cancelled_boarders / $total_boarders) * 100 : 0;
    $agreement_rate = $total_boarders > 0 ? ($agreements_signed / $total_boarders) * 100 : 0;
    
    // Get room occupancy details
    $room_sql = "SELECT 
                    COUNT(DISTINCT ru.room_id) as total_rooms,
                    SUM(CASE WHEN ru.room_status = 'Occupied' THEN 1 ELSE 0 END) as occupied_rooms,
                    SUM(CASE WHEN ru.room_status = 'Available' THEN 1 ELSE 0 END) as available_rooms,
                    SUM(CASE WHEN ru.room_status = 'Maintenance' THEN 1 ELSE 0 END) as maintenance_rooms
                FROM room_units ru
                JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
                JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
                WHERE bh.user_id = ?";
    
    $room_stmt = $db->prepare($room_sql);
    $room_stmt->execute([$user_id]);
    $room_summary = $room_stmt->fetch(PDO::FETCH_ASSOC);
    
    $total_rooms = intval($room_summary['total_rooms']);
    $occupied_rooms = intval($room_summary['occupied_rooms']);
    $available_rooms = intval($room_summary['available_rooms']);
    $maintenance_rooms = intval($room_summary['maintenance_rooms']);
    
    $room_occupancy_rate = $total_rooms > 0 ? ($occupied_rooms / $total_rooms) * 100 : 0;
    
    // Get recent boarders (last 5)
    $recent_sql = "SELECT 
                    b.booking_id,
                    CONCAT(r.f_name, ' ', r.l_name) as boarder_name,
                    ru.room_number as room_name,
                    bh.bh_name as boarding_house_name,
                    b.booking_status,
                    b.start_date,
                    b.check_in_date
                FROM bookings b
                JOIN users u ON b.user_id = u.user_id
                JOIN registrations r ON u.reg_id = r.reg_id
                JOIN room_units ru ON b.room_id = ru.room_id
                JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
                JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
                WHERE bh.user_id = ? $date_condition
                ORDER BY b.start_date DESC
                LIMIT 5";
    
    $recent_stmt = $db->prepare($recent_sql);
    $recent_stmt->execute([$user_id]);
    
    $recent_boarders = [];
    while ($row = $recent_stmt->fetch(PDO::FETCH_ASSOC)) {
        $recent_boarders[] = [
            'booking_id' => intval($row['booking_id']),
            'boarder_name' => $row['boarder_name'],
            'room_name' => $row['room_name'],
            'boarding_house_name' => $row['boarding_house_name'],
            'booking_status' => $row['booking_status'],
            'start_date' => date('Y-m-d', strtotime($row['start_date'])),
            'check_in_date' => $row['check_in_date'] ? date('Y-m-d', strtotime($row['check_in_date'])) : null
        ];
    }
    
    $response = [
        'success' => true,
        'data' => [
            'summary' => [
                'total_boarders' => $total_boarders,
                'current_boarders' => $current_boarders,
                'completed_boarders' => $completed_boarders,
                'cancelled_boarders' => $cancelled_boarders,
                'checking_out_soon' => $checking_out_soon,
                'payment_overdue' => $payment_overdue,
                'checked_in' => $checked_in,
                'agreements_signed' => $agreements_signed,
                'average_stay_days' => round($average_stay_days, 1),
                'current_monthly_revenue' => "P" . number_format($current_monthly_revenue, 2),
                'total_paid_revenue' => "P" . number_format($total_paid_revenue, 2),
                'total_expected_revenue' => "P" . number_format($total_expected_revenue, 2),
                'occupancy_rate' => round($occupancy_rate, 2),
                'completion_rate' => round($completion_rate, 2),
                'cancellation_rate' => round($cancellation_rate, 2),
                'agreement_rate' => round($agreement_rate, 2),
                'first_boarder_date' => $summary['first_boarder_date'] ? date('Y-m-d', strtotime($summary['first_boarder_date'])) : null,
                'last_boarder_date' => $summary['last_boarder_date'] ? date('Y-m-d', strtotime($summary['last_boarder_date'])) : null
            ],
            'room_summary' => [
                'total_rooms' => $total_rooms,
                'occupied_rooms' => $occupied_rooms,
                'available_rooms' => $available_rooms,
                'maintenance_rooms' => $maintenance_rooms,
                'room_occupancy_rate' => round($room_occupancy_rate, 2)
            ],
            'recent_boarders' => $recent_boarders,
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





