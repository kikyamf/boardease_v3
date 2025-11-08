<?php
// Handle preflight OPTIONS request for CORS
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type, User-Agent, Accept');
    header('Access-Control-Max-Age: 86400');
    http_response_code(200);
    exit();
}

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, User-Agent, Accept');

// Database configuration
$host = 'localhost';
$dbname = 'boardease2';
$username = 'boardease';
$password = 'boardease';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Get booking_id from POST or GET request
    $bookingId = isset($_POST['booking_id']) ? intval($_POST['booking_id']) : (isset($_GET['booking_id']) ? intval($_GET['booking_id']) : 0);
    
    error_log("get_unpaid_payment_breakdowns.php - Received booking_id: $bookingId");
    
    if ($bookingId === 0) {
        echo json_encode(array(
            'success' => false,
            'error' => 'Booking ID is required.'
        ));
        exit();
    }
    
    // Fetch ALL unpaid payment breakdowns for this booking (current and future periods)
    // Show all breakdowns that are not paid, regardless of selection status (allows advance payment)
    // Also dynamically calculate if status should be 'Overdue' based on due_date
    $sql = "
        SELECT 
            pb.breakdown_id,
            pb.booking_id,
            pb.payment_id,
            pb.period_type,
            pb.period_number,
            pb.period_label,
            pb.period_start_date,
            pb.period_end_date,
            pb.amount,
            pb.is_selected,
            pb.is_paid,
            COALESCE(pb.due_date, pb.period_start_date) as due_date,
            CASE 
                WHEN pb.is_paid = 1 THEN 'Paid'
                WHEN pb.payment_status = 'Cancelled' THEN 'Cancelled'
                WHEN COALESCE(pb.due_date, pb.period_start_date) < CURDATE() AND pb.payment_status IN ('Pending', 'Overdue') THEN 'Overdue'
                WHEN COALESCE(pb.due_date, pb.period_start_date) >= CURDATE() 
                     AND COALESCE(pb.due_date, pb.period_start_date) <= DATE_ADD(CURDATE(), INTERVAL 7 DAY) 
                     AND pb.payment_status IN ('Pending', 'Overdue') THEN 'Pending'
                ELSE 'Pending'
            END as payment_status,
            pb.created_at,
            pb.updated_at
        FROM payment_breakdowns pb
        WHERE pb.booking_id = :booking_id
            AND pb.is_paid = 0
            AND (pb.payment_status != 'Cancelled' OR pb.payment_status IS NULL)
        ORDER BY 
            COALESCE(pb.due_date, pb.period_start_date) ASC
    ";
    
    $stmt = $pdo->prepare($sql);
    $stmt->execute([':booking_id' => $bookingId]);
    $results = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    error_log("get_unpaid_payment_breakdowns.php - Found " . count($results) . " unpaid breakdowns for booking_id: $bookingId");
    
    // Format the results
    $breakdowns = array();
    $today = new DateTime();
    $today->setTime(0, 0, 0); // Set to start of day for comparison
    
    foreach ($results as $row) {
        $paymentStatus = $row['payment_status']; // This is already calculated in SQL
        $dueDateStr = $row['due_date'];
        
        // Ensure due_date is set (should already be handled in SQL with COALESCE)
        if (empty($dueDateStr)) {
            $dueDateStr = $row['period_start_date'];
        }
        
        // Log for debugging
        error_log("Breakdown: ID=" . $row['breakdown_id'] . ", Label=" . $row['period_label'] . ", Status=" . $paymentStatus . ", Due=" . $dueDateStr . ", IsPaid=" . $row['is_paid']);
        
        $breakdown = array(
            'breakdown_id' => (int)$row['breakdown_id'],
            'booking_id' => (int)$row['booking_id'],
            'payment_id' => $row['payment_id'] ? (int)$row['payment_id'] : null,
            'period_type' => $row['period_type'],
            'period_number' => (int)$row['period_number'],
            'period_label' => $row['period_label'],
            'period_start_date' => $row['period_start_date'],
            'period_end_date' => $row['period_end_date'],
            'amount' => floatval($row['amount']),
            'is_selected' => (int)$row['is_selected'],
            'is_paid' => (int)$row['is_paid'],
            'due_date' => $dueDateStr,
            'payment_status' => $paymentStatus,
            'created_at' => $row['created_at'],
            'updated_at' => $row['updated_at']
        );
        
        $breakdowns[] = $breakdown;
    }
    
    error_log("get_unpaid_payment_breakdowns.php - Returning " . count($breakdowns) . " unpaid breakdowns for booking_id: $bookingId");
    
    echo json_encode(array(
        'success' => true,
        'data' => $breakdowns,
        'count' => count($breakdowns)
    ));
    
} catch (PDOException $e) {
    error_log("Database error in get_unpaid_payment_breakdowns.php: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ));
} catch (Exception $e) {
    error_log("Server error in get_unpaid_payment_breakdowns.php: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'error' => 'Server error: ' . $e->getMessage()
    ));
}
?>

