<?php
// Get Monthly Payment Status API - Shows progress like "2/2 months paid" or "1/2 months paid"
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
    $status = isset($_GET['status']) ? $_GET['status'] : 'all';
    $month = isset($_GET['month']) ? $_GET['month'] : date('Y-m'); // Default to current month
    
    // If not in GET, try POST
    if ($owner_id <= 0) {
        $input = json_decode(file_get_contents('php://input'), true);
        if (!$input) {
            $input = $_POST;
        }
        $owner_id = isset($input['owner_id']) ? intval($input['owner_id']) : 0;
        $status = isset($input['status']) ? $input['status'] : 'all';
        $month = isset($input['month']) ? $input['month'] : date('Y-m');
    }
    
    if ($owner_id <= 0) {
        throw new Exception("Invalid owner_id");
    }
    
    $db = getDB();
    
    // Build the query based on status filter
    $whereClause = "WHERE ab.boarding_house_id IN (SELECT bh_id FROM boarding_houses WHERE user_id = ?)";
    $params = [$owner_id];
    
    if ($status !== 'all') {
        $whereClause .= " AND p.payment_status = ?";
        $params[] = ucfirst($status);
    }
    
    if ($month !== 'all') {
        $whereClause .= " AND p.payment_month = ?";
        $params[] = $month;
    }
    
    $sql = "
        SELECT 
            p.payment_id,
            p.booking_id,
            p.bill_id,
            p.user_id,
            p.owner_id,
            p.payment_amount,
            p.payment_method,
            p.payment_proof,
            p.payment_status,
            p.payment_date,
            p.receipt_url,
            p.notes,
            p.payment_month,
            p.payment_year,
            p.payment_month_number,
            p.is_monthly_payment,
            p.total_months_required,
            p.months_paid,
            CONCAT(r.f_name, ' ', r.l_name) as boarder_name,
            r.email as boarder_email,
            r.phone_number as boarder_phone,
            ru.room_number as room_name,
            bhr.room_name as room_type,
            bhr.room_category as rent_type,
            bhr.room_price as room_price,
            bh.bh_name as boarding_house_name,
            bh.bh_address as boarding_house_address,
            ab.status as boarder_status,
            u.profile_picture,
            -- Calculate payment progress
            COALESCE(SUM(CASE WHEN p2.payment_status = 'Completed' THEN p2.months_paid ELSE 0 END), 0) as total_months_paid,
            p.total_months_required as total_months_required
        FROM payments p
        INNER JOIN active_boarders ab ON p.user_id = ab.user_id
        INNER JOIN users u ON p.user_id = u.user_id
        INNER JOIN registrations r ON u.reg_id = r.reg_id
        LEFT JOIN room_units ru ON ab.room_id = ru.room_id
        LEFT JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
        LEFT JOIN boarding_houses bh ON ab.boarding_house_id = bh.bh_id
        LEFT JOIN payments p2 ON p.user_id = p2.user_id AND p.booking_id = p2.booking_id
        $whereClause
        GROUP BY p.payment_id, p.user_id, p.booking_id
        ORDER BY p.payment_date DESC
    ";
    
    $stmt = $db->prepare($sql);
    $stmt->execute($params);
    
    $payments = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        // Format amount
        $amount = "P" . number_format($row['payment_amount'], 2);
        
        // Format dates
        $payment_date = date('Y-m-d H:i', strtotime($row['payment_date']));
        
        // Calculate payment progress
        $total_months_paid = intval($row['total_months_paid']);
        $total_months_required = intval($row['total_months_required']) ?: 1;
        
        // Create progress text like "2/2 months paid" or "1/2 months paid"
        $progress_text = $total_months_paid . "/" . $total_months_required . " months paid";
        
        // Determine status color based on progress
        $status_color = 'green'; // Default for completed
        if ($total_months_paid < $total_months_required) {
            $status_color = 'yellow'; // Partial payment
        }
        if ($row['payment_status'] === 'Pending') {
            $status_color = 'red'; // Pending
        }
        
        $payments[] = [
            'payment_id' => intval($row['payment_id']),
            'booking_id' => intval($row['booking_id']),
            'bill_id' => intval($row['bill_id']),
            'user_id' => intval($row['user_id']),
            'owner_id' => intval($row['owner_id']),
            'boarder_name' => $row['boarder_name'],
            'boarder_email' => $row['boarder_email'],
            'boarder_phone' => $row['boarder_phone'],
            'room' => $row['room_name'] . ' - ' . $row['room_type'],
            'room_name' => $row['room_name'],
            'boarding_house_name' => $row['boarding_house_name'],
            'boarding_house_address' => $row['boarding_house_address'],
            'rent_type' => $row['rent_type'],
            'amount_paid' => $amount,
            'total_amount' => $amount,
            'payment_status' => $row['payment_status'],
            'progress_text' => $progress_text,
            'status_color' => $status_color,
            'rental_status' => $row['boarder_status'],
            'payment_date' => $payment_date,
            'payment_method' => $row['payment_method'],
            'payment_proof' => $row['payment_proof'],
            'receipt_url' => $row['receipt_url'],
            'notes' => $row['notes'],
            'payment_month' => $row['payment_month'],
            'total_months_paid' => $total_months_paid,
            'total_months_required' => $total_months_required,
            'profile_image' => $row['profile_picture'] ?? ''
        ];
    }
    
    $response = [
        'success' => true,
        'data' => [
            'payments' => $payments,
            'total_count' => count($payments),
            'month' => $month,
            'status' => $status
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




