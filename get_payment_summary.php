<?php
// Get Payment Summary API - Returns payment statistics and summary
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
    // Get user_id from request (GET or POST)
    $owner_id = isset($_GET['owner_id']) ? intval($_GET['owner_id']) : 0;
    $period = isset($_GET['period']) ? $_GET['period'] : 'month';
    
    // If not in GET, try POST
    if ($owner_id <= 0) {
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) {
            $input = $_POST;
        }
        $owner_id = isset($input['owner_id']) ? intval($input['owner_id']) : 0;
        $period = isset($input['period']) ? $input['period'] : 'month';
    }
    
    if ($owner_id <= 0) {
        throw new Exception("Invalid owner_id");
    }
    
    $db = getDB();
    
    // Calculate date range based on period
    $dateCondition = "";
    switch ($period) {
        case 'week':
            $dateCondition = "AND b.created_at >= DATE_SUB(NOW(), INTERVAL 1 WEEK)";
            break;
        case 'month':
            $dateCondition = "AND b.created_at >= DATE_SUB(NOW(), INTERVAL 1 MONTH)";
            break;
        case 'year':
            $dateCondition = "AND b.created_at >= DATE_SUB(NOW(), INTERVAL 1 YEAR)";
            break;
        default:
            $dateCondition = "AND b.created_at >= DATE_SUB(NOW(), INTERVAL 1 MONTH)";
    }
    
    // Get payment summary
    $summaryQuery = "
        SELECT 
            COUNT(*) as total_payments,
            SUM(CASE WHEN b.status = 'Unpaid' THEN 1 ELSE 0 END) as pending_payments,
            SUM(CASE WHEN b.status = 'Paid' THEN 1 ELSE 0 END) as paid_payments,
            SUM(CASE WHEN b.status = 'Overdue' THEN 1 ELSE 0 END) as overdue_payments,
            SUM(b.amount_due) as total_amount,
            SUM(CASE WHEN b.status = 'Unpaid' THEN b.amount_due ELSE 0 END) as pending_amount,
            SUM(CASE WHEN b.status = 'Paid' THEN b.amount_due ELSE 0 END) as paid_amount,
            SUM(CASE WHEN b.status = 'Overdue' THEN b.amount_due ELSE 0 END) as overdue_amount
        FROM bills b
        INNER JOIN active_boarders ab ON b.active_id = ab.active_id
        INNER JOIN users u ON ab.user_id = u.user_id
        WHERE ab.boarding_house_id IN (SELECT bh_id FROM boarding_houses WHERE user_id = ?) $dateCondition
    ";
    
    $stmt = $db->prepare($summaryQuery);
    $stmt->execute([$owner_id]);
    $summary = $stmt->fetch(PDO::FETCH_ASSOC);
    
    // Calculate collection rate
    $total_amount = floatval($summary['total_amount']);
    $paid_amount = floatval($summary['paid_amount']);
    $collection_rate = $total_amount > 0 ? ($paid_amount / $total_amount) * 100 : 0;
    
    // Get recent payments
    $recentQuery = "
        SELECT 
            b.bill_id,
            b.amount_due,
            b.status,
            b.created_at,
            CONCAT(r.f_name, ' ', r.l_name) as boarder_name,
            ru.room_number
        FROM bills b
        INNER JOIN active_boarders ab ON b.active_id = ab.active_id
        INNER JOIN users u ON ab.user_id = u.user_id
        INNER JOIN registrations r ON u.reg_id = r.reg_id
        WHERE ab.boarding_house_id IN (SELECT bh_id FROM boarding_houses WHERE user_id = ?) $dateCondition
        ORDER BY b.created_at DESC
        LIMIT 5
    ";
    
    $stmt = $db->prepare($recentQuery);
    $stmt->execute([$owner_id]);
    $recent_payments = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Get status distribution
    $statusQuery = "
        SELECT 
            b.status,
            COUNT(*) as count,
            SUM(b.amount_due) as total_amount
        FROM bills b
        INNER JOIN active_boarders ab ON b.active_id = ab.active_id
        INNER JOIN users u ON ab.user_id = u.user_id
        WHERE ab.boarding_house_id IN (SELECT bh_id FROM boarding_houses WHERE user_id = ?) $dateCondition
        GROUP BY b.status
    ";
    
    $stmt = $db->prepare($statusQuery);
    $stmt->execute([$owner_id]);
    $status_distribution = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    $response = [
        'success' => true,
        'data' => [
            'total_payments' => intval($summary['total_payments']),
            'pending_payments' => intval($summary['pending_payments']),
            'paid_payments' => intval($summary['paid_payments']),
            'overdue_payments' => intval($summary['overdue_payments']),
            'total_amount' => floatval($summary['total_amount']),
            'pending_amount' => floatval($summary['pending_amount']),
            'paid_amount' => floatval($summary['paid_amount']),
            'overdue_amount' => floatval($summary['overdue_amount']),
            'collection_rate' => round($collection_rate, 2),
            'period' => $period,
            'recent_payments' => $recent_payments,
            'status_distribution' => $status_distribution
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