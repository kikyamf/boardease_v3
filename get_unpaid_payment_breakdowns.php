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
$dbname = 'u223444398_boardease';
$username = 'u223444398_userboardease';
$password = '!Boardease2026';

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
    // If a breakdown is linked to a payment with "Pending" status, mark it as "For Approval"
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
            pb.payment_status as db_payment_status,
            CASE 
                WHEN pb.is_paid = 1 THEN 'Paid'
                WHEN pb.payment_status = 'Cancelled' THEN 'Cancelled'
                WHEN pb.payment_id IS NOT NULL AND COALESCE(p.payment_status, '') = 'Pending' THEN 'For Approval'
                WHEN pb.payment_status = 'Overdue' OR (COALESCE(pb.due_date, pb.period_start_date) < CURDATE() AND COALESCE(pb.payment_status, 'Pending') IN ('Pending', 'Overdue')) THEN 'Overdue'
                WHEN COALESCE(pb.payment_status, 'Pending') = 'Pending' AND COALESCE(pb.due_date, pb.period_start_date) >= CURDATE() 
                     AND COALESCE(pb.due_date, pb.period_start_date) <= DATE_ADD(CURDATE(), INTERVAL 7 DAY) THEN 'Pending'
                WHEN pb.payment_status IS NOT NULL AND pb.payment_status != '' THEN pb.payment_status
                ELSE 'Pending'
            END as payment_status,
            pb.created_at,
            pb.updated_at
        FROM payment_breakdowns pb
        LEFT JOIN payments p ON pb.payment_id = p.payment_id
        WHERE pb.booking_id = :booking_id
            AND pb.is_paid = 0
            AND (pb.payment_status != 'Cancelled' OR pb.payment_status IS NULL)
            AND (pb.payment_status != 'Paid' OR pb.payment_status IS NULL)
        ORDER BY 
            COALESCE(pb.due_date, pb.period_start_date) ASC
    ";
    
    $stmt = $pdo->prepare($sql);
    $stmt->execute([':booking_id' => $bookingId]);
    $results = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    error_log("get_unpaid_payment_breakdowns.php - Found " . count($results) . " unpaid breakdowns for booking_id: $bookingId");
    
    // HEALING LOGIC: If no breakdowns found, check if we should generate them
    if (count($results) === 0) {
        error_log("get_unpaid_payment_breakdowns.php - No breakdowns found. Checking if healing is needed for booking_id: $bookingId");
        
        // Check if ANY breakdowns exist (paid or unpaid)
        $checkExistSql = "SELECT COUNT(*) FROM payment_breakdowns WHERE booking_id = :booking_id";
        $checkExistStmt = $pdo->prepare($checkExistSql);
        $checkExistStmt->execute([':booking_id' => $bookingId]);
        $totalBreakdowns = $checkExistStmt->fetchColumn();
        
        if ($totalBreakdowns == 0) {
            error_log("get_unpaid_payment_breakdowns.php - 0 breakdowns exist. Attempting to generate for booking_id: $bookingId");
            
            // Get booking details and room price
            $bookingQuery = "
                SELECT b.start_date, b.end_date, b.booking_status, bhr.price, b.user_id
                FROM bookings b
                JOIN room_units ru ON b.room_id = ru.room_id
                JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
                WHERE b.booking_id = :booking_id
            ";
            $bookingStmt = $pdo->prepare($bookingQuery);
            $bookingStmt->execute([':booking_id' => $bookingId]);
            $bookingData = $bookingStmt->fetch(PDO::FETCH_ASSOC);
            
            if ($bookingData && in_array($bookingData['booking_status'], ['Approved', 'Confirmed', 'Active'])) {
                $startDate = $bookingData['start_date'];
                $endDate = $bookingData['end_date'];
                $monthlyPrice = floatval($bookingData['price']);
                
                error_log("get_unpaid_payment_breakdowns.php - Generating breakdowns for $startDate to $endDate at ₱$monthlyPrice");
                
                // Calculate periods
                $startDateObj = new DateTime($startDate);
                $endDateObj = new DateTime($endDate);
                $diff = $startDateObj->diff($endDateObj);
                $numberOfDays = $diff->days;
                
                if ($numberOfDays > 0) {
                    $cal = clone $startDateObj;
                    // Start from next day (standard logic in app)
                    $cal->modify('+1 day');
                    
                    $remainingDays = $numberOfDays;
                    $monthCount = 0;
                    $breakdownsToInsert = [];
                    
                    // Monthly periods
                    while ($remainingDays >= 30) {
                        $monthCount++;
                        $periodStart = clone $cal;
                        $periodEnd = clone $cal;
                        $periodEnd->modify('+29 days');
                        
                        $label = "";
                        switch ($monthCount) {
                            case 1: $label = "1st month"; break;
                            case 2: $label = "2nd month"; break;
                            case 3: $label = "3rd month"; break;
                            default: $label = $monthCount . "th month"; break;
                        }
                        
                        $breakdownsToInsert[] = [
                            'label' => $label,
                            'type' => 'month',
                            'number' => $monthCount,
                            'amount' => $monthlyPrice,
                            'start' => $periodStart->format('Y-m-d'),
                            'end' => $periodEnd->format('Y-m-d')
                        ];
                        
                        $cal->modify('+30 days');
                        $remainingDays -= 30;
                    }
                    
                    // Remaining days
                    if ($remainingDays > 0) {
                        $periodStart = clone $cal;
                        $periodEnd = clone $cal;
                        $periodEnd->modify('+' . ($remainingDays - 1) . ' days');
                        
                        $dailyRate = $monthlyPrice / 30.0;
                        $daysAmount = $dailyRate * $remainingDays;
                        $label = $remainingDays . ($remainingDays == 1 ? " day" : " days");
                        
                        $breakdownsToInsert[] = [
                            'label' => $label,
                            'type' => 'days',
                            'number' => 0,
                            'amount' => $daysAmount,
                            'start' => $periodStart->format('Y-m-d'),
                            'end' => $periodEnd->format('Y-m-d')
                        ];
                    }
                    
                    // Insert them
                    if (!empty($breakdownsToInsert)) {
                        $insertSql = "
                            INSERT INTO payment_breakdowns (
                                booking_id, period_type, period_number, period_label,
                                period_start_date, period_end_date, amount, is_selected,
                                is_paid, payment_status, due_date
                            ) VALUES (
                                :booking_id, :type, :number, :label,
                                :start, :end, :amount, 1,
                                0, 'Pending', :due
                            )
                        ";
                        $insertStmt = $pdo->prepare($insertSql);
                        
                        foreach ($breakdownsToInsert as $bti) {
                            $insertStmt->execute([
                                ':booking_id' => $bookingId,
                                ':type' => $bti['type'],
                                ':number' => $bti['number'],
                                ':label' => $bti['label'],
                                ':start' => $bti['start'],
                                ':end' => $bti['end'],
                                ':amount' => $bti['amount'],
                                ':due' => $bti['start']
                            ]);
                        }
                        error_log("get_unpaid_payment_breakdowns.php - Successfully generated " . count($breakdownsToInsert) . " breakthroughs");
                        
                        // Re-fetch the results
                        $stmt = $pdo->prepare($sql);
                        $stmt->execute([':booking_id' => $bookingId]);
                        $results = $stmt->fetchAll(PDO::FETCH_ASSOC);
                    }
                }
            } else {
                error_log("get_unpaid_payment_breakdowns.php - Healing skipped. Booking status: " . ($bookingData['booking_status'] ?? 'NOT FOUND'));
            }
        }
    }
    
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
        
        // Log for debugging - also log the raw database value
        $dbStatus = isset($row['db_payment_status']) ? $row['db_payment_status'] : 'NULL';
        error_log("Breakdown: ID=" . $row['breakdown_id'] . ", Label=" . $row['period_label'] . 
                  ", DB Status=[" . $dbStatus . "]" . 
                  ", Calculated Status=[" . $paymentStatus . "]" . 
                  ", Due=" . $dueDateStr . 
                  ", IsPaid=" . $row['is_paid'] . 
                  ", PaymentID=" . ($row['payment_id'] ?? 'NULL'));
        
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

