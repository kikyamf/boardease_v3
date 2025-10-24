<?php
// Get Pending Payments by Month API - Shows who still needs to pay for specific months
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
    $month = isset($_GET['month']) ? $_GET['month'] : date('Y-m'); // Default to current month
    
    // If not in GET, try POST
    if ($owner_id <= 0) {
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) {
            $input = $_POST;
        }
        $owner_id = isset($input['owner_id']) ? intval($input['owner_id']) : 0;
        $month = isset($input['month']) ? $input['month'] : date('Y-m');
    }
    
    if ($owner_id <= 0) {
        throw new Exception("Invalid owner_id");
    }
    
    $db = getDB();
    
    // Get all active boarders for this owner
    $sql = "
        SELECT 
            ab.user_id,
            ab.booking_id,
            CONCAT(r.f_name, ' ', r.l_name) as boarder_name,
            r.email as boarder_email,
            r.phone_number as boarder_phone,
            ru.room_number as room_name,
            bhr.room_name as room_type,
            bhr.room_category as rent_type,
            bhr.room_price as room_price,
            bh.bh_name as boarding_house_name,
            bh.bh_address as boarding_house_address,
            u.profile_picture,
            b.start_date,
            b.end_date,
            b.booking_status,
            -- Check if payment exists for this month
            p.payment_id,
            p.payment_status,
            p.payment_amount,
            p.payment_date,
            p.payment_method,
            p.payment_proof,
            p.receipt_url,
            p.notes
        FROM active_boarders ab
        INNER JOIN users u ON ab.user_id = u.user_id
        INNER JOIN registrations r ON u.reg_id = r.reg_id
        INNER JOIN bookings b ON ab.user_id = b.user_id
        LEFT JOIN room_units ru ON ab.room_id = ru.room_id
        LEFT JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
        LEFT JOIN boarding_houses bh ON ab.boarding_house_id = bh.bh_id
        LEFT JOIN payments p ON ab.user_id = p.user_id AND p.payment_month = ? AND p.booking_id = b.booking_id
        WHERE ab.boarding_house_id IN (SELECT bh_id FROM boarding_houses WHERE user_id = ?)
        AND ab.status = 'Active'
        AND b.booking_status = 'Approved'
        ORDER BY b.start_date DESC
    ";
    
    $stmt = $db->prepare($sql);
    $stmt->execute([$month, $owner_id]);
    
    $pending_payments = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        // Format amount
        $amount = "P" . number_format($row['room_price'], 2);
        
        // Determine payment status
        $payment_status = 'Pending';
        $payment_date = 'Not paid';
        $payment_method = 'Not specified';
        $payment_proof = '';
        $receipt_url = '';
        $notes = 'Monthly rent payment - ' . date('F Y', strtotime($month . '-01'));
        
        if ($row['payment_id']) {
            $payment_status = $row['payment_status'];
            $payment_date = date('Y-m-d H:i', strtotime($row['payment_date']));
            $payment_method = $row['payment_method'];
            $payment_proof = $row['payment_proof'];
            $receipt_url = $row['receipt_url'];
            $notes = $row['notes'];
        }
        
        // Calculate how many days overdue (if pending and past due date)
        $due_date = date('Y-m-05', strtotime($month . '-01')); // Assume 5th of each month is due date
        $days_overdue = 0;
        if ($payment_status === 'Pending' && date('Y-m-d') > $due_date) {
            $days_overdue = (strtotime(date('Y-m-d')) - strtotime($due_date)) / (60 * 60 * 24);
        }
        
        $pending_payments[] = [
            'payment_id' => $row['payment_id'] ? intval($row['payment_id']) : null,
            'booking_id' => intval($row['booking_id']),
            'user_id' => intval($row['user_id']),
            'owner_id' => $owner_id,
            'boarder_name' => $row['boarder_name'],
            'boarder_email' => $row['boarder_email'],
            'boarder_phone' => $row['boarder_phone'],
            'room' => $row['room_name'] . ' - ' . $row['room_type'],
            'room_name' => $row['room_name'],
            'boarding_house_name' => $row['boarding_house_name'],
            'boarding_house_address' => $row['boarding_house_address'],
            'rent_type' => $row['rent_type'],
            'amount_paid' => $payment_status === 'Completed' ? $amount : 'P0.00',
            'total_amount' => $amount,
            'payment_status' => $payment_status,
            'rental_status' => 'Active',
            'payment_date' => $payment_date,
            'payment_method' => $payment_method,
            'payment_proof' => $payment_proof,
            'receipt_url' => $receipt_url,
            'notes' => $notes,
            'payment_month' => $month,
            'due_date' => $due_date,
            'days_overdue' => intval($days_overdue),
            'is_overdue' => $days_overdue > 0,
            'profile_image' => $row['profile_picture'] ?? ''
        ];
    }
    
    // Separate completed and pending payments
    $completed_payments = array_filter($pending_payments, function($payment) {
        return $payment['payment_status'] === 'Completed';
    });
    
    $pending_payments_only = array_filter($pending_payments, function($payment) {
        return $payment['payment_status'] === 'Pending';
    });
    
    $response = [
        'success' => true,
        'data' => [
            'month' => $month,
            'month_name' => date('F Y', strtotime($month . '-01')),
            'all_payments' => $pending_payments,
            'completed_payments' => array_values($completed_payments),
            'pending_payments' => array_values($pending_payments_only),
            'total_boarders' => count($pending_payments),
            'completed_count' => count($completed_payments),
            'pending_count' => count($pending_payments_only),
            'overdue_count' => count(array_filter($pending_payments_only, function($p) { return $p['is_overdue']; }))
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




