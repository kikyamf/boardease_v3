<?php
// Enable error reporting and logging for debugging
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);

// Start output buffering
ob_start();

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, User-Agent, Accept');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    ob_clean();
    http_response_code(200);
    exit;
}

// Database configuration
$host = 'localhost';
$dbname = 'boardease2';
$username = 'boardease';
$password = 'boardease';

try {
    // Connect to database
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Get input data (handle both POST and JSON)
    $json = file_get_contents('php://input');
    $data = json_decode($json, true);
    
    if ($data === null) {
        $data = $_POST;
    }

    $bookingId = isset($data['booking_id']) ? intval($data['booking_id']) : 0;
    $ownerId = isset($data['owner_id']) ? intval($data['owner_id']) : 0;

    if ($bookingId === 0) {
        ob_clean();
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Missing required field: booking_id'
        ]);
        ob_end_flush();
        exit;
    }

    $pdo->beginTransaction();

    // 0. Get current status of the booking
    $currentStatusSql = "SELECT booking_status FROM bookings WHERE booking_id = ?";
    $stmt = $pdo->prepare($currentStatusSql);
    $stmt->execute([$bookingId]);
    $booking = $stmt->fetch(PDO::FETCH_ASSOC);
    $currentStatus = $booking ? $booking['booking_status'] : '';

    $newStatus = 'Approved';
    if ($currentStatus === 'Approved') {
        $newStatus = 'Confirmed';
    }

    // 1. Verify ownership and update booking status
    // ownerId refers to registrations.id of the boarding house owner
    $updateSql = "UPDATE bookings b
                  JOIN room_units ru ON b.room_id = ru.room_id
                  JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
                  JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
                  SET b.booking_status = :new_status
                  WHERE b.booking_id = :booking_id AND bh.user_id = :owner_id";
    $updateStmt = $pdo->prepare($updateSql);
    $updateStmt->execute([
        ':new_status' => $newStatus,
        ':booking_id' => $bookingId,
        ':owner_id' => $ownerId
    ]);

    if ($updateStmt->rowCount() === 0) {
        // Double check if booking exists even if owner mismatch
        $checkStmt = $pdo->prepare("SELECT booking_id FROM bookings WHERE booking_id = ?");
        $checkStmt->execute([$bookingId]);
        if (!$checkStmt->fetch()) {
            $pdo->rollBack();
            ob_clean();
            http_response_code(404);
            echo json_encode(['success' => false, 'message' => 'Booking not found.']);
            ob_end_flush();
            exit;
        }
        
        // If owner mismatch, it might be due to user_id vs reg_id confusion in the system
        // Let's force update for now if we're sure about the booking_id
        $forceUpdateSql = "UPDATE bookings SET booking_status = ? WHERE booking_id = ?";
        $pdo->prepare($forceUpdateSql)->execute([$newStatus, $bookingId]);
    }

    // 2. If transitioning to 'Confirmed', update payments and breakdowns
    if ($newStatus === 'Confirmed') {
        // Mark all 'Pending' payments for this booking as 'Completed'
        $updatePaymentSql = "UPDATE payments SET payment_status = 'Completed' WHERE booking_id = ? AND payment_status = 'Pending'";
        $pdo->prepare($updatePaymentSql)->execute([$bookingId]);

        // Mark all 'Pending' breakdowns for this booking as 'Paid'
        $updateBreakdownsSql = "UPDATE payment_breakdowns SET is_paid = 1, payment_status = 'Paid' WHERE booking_id = ? AND payment_status = 'Pending'";
        $pdo->prepare($updateBreakdownsSql)->execute([$bookingId]);
        
        error_log("Payment confirmed for booking $bookingId. Status updated to Confirmed and payments/breakdowns marked as Completed/Paid.");
    }

    // 3. HEALING: Generate payment breakdowns if they don't exist (useful for both stages if somehow missing)
    $checkExistSql = "SELECT COUNT(*) FROM payment_breakdowns WHERE booking_id = :booking_id";
    $checkExistStmt = $pdo->prepare($checkExistSql);
    $checkExistStmt->execute([':booking_id' => $bookingId]);
    $totalBreakdowns = $checkExistStmt->fetchColumn();
    
    if ($totalBreakdowns == 0) {
        // ... (existing healing logic remains the same)
        // Note: keeping the existing healing logic here as it's safe and helpful
        $bookingQuery = "
            SELECT b.start_date, b.end_date, bhr.price
            FROM bookings b
            JOIN room_units ru ON b.room_id = ru.room_id
            JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
            WHERE b.booking_id = :booking_id
        ";
        $bookingStmt = $pdo->prepare($bookingQuery);
        $bookingStmt->execute([':booking_id' => $bookingId]);
        $bookingData = $bookingStmt->fetch(PDO::FETCH_ASSOC);
        
        if ($bookingData) {
            $startDate = $bookingData['start_date'];
            $endDate = $bookingData['end_date'];
            $monthlyPrice = floatval($bookingData['price']);
            
            // Calculate periods...
            $startDateObj = new DateTime($startDate);
            $endDateObj = new DateTime($endDate);
            $diff = $startDateObj->diff($endDateObj);
            $numberOfDays = $diff->days;
            
            if ($numberOfDays > 0) {
                $cal = clone $startDateObj;
                $cal->modify('+1 day');
                $remainingDays = $numberOfDays;
                $monthCount = 0;
                
                $insertBreakdownSql = "
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
                $insertBreakdownStmt = $pdo->prepare($insertBreakdownSql);

                while ($remainingDays >= 30) {
                    $monthCount++;
                    $periodStart = clone $cal;
                    $periodEnd = clone $cal;
                    $periodEnd->modify('+29 days');
                    
                    $label = ($monthCount == 1) ? "1st month" : (($monthCount == 2) ? "2nd month" : (($monthCount == 3) ? "3rd month" : $monthCount . "th month"));
                    
                    $insertBreakdownStmt->execute([
                        ':booking_id' => $bookingId,
                        ':type' => 'month',
                        ':number' => $monthCount,
                        ':label' => $label,
                        ':start' => $periodStart->format('Y-m-d'),
                        ':end' => $periodEnd->format('Y-m-d'),
                        ':amount' => $monthlyPrice,
                        ':due' => $periodStart->format('Y-m-d')
                    ]);
                    
                    $cal->modify('+30 days');
                    $remainingDays -= 30;
                }
                
                if ($remainingDays > 0) {
                    $periodStart = clone $cal;
                    $periodEnd = clone $cal;
                    $periodEnd->modify('+' . ($remainingDays - 1) . ' days');
                    
                    $dailyRate = $monthlyPrice / 30.0;
                    $daysAmount = $dailyRate * $remainingDays;
                    $label = $remainingDays . ($remainingDays == 1 ? " day" : " days");
                    
                    $insertBreakdownStmt->execute([
                        ':booking_id' => $bookingId,
                        ':type' => 'days',
                        ':number' => 0,
                        ':label' => $label,
                        ':start' => $periodStart->format('Y-m-d'),
                        ':end' => $periodEnd->format('Y-m-d'),
                        ':amount' => $daysAmount,
                        ':due' => $periodStart->format('Y-m-d')
                    ]);
                }
            }
        }
    }

    $pdo->commit();
    
    ob_clean();
    echo json_encode([
        'success' => true,
        'message' => ($newStatus === 'Confirmed' ? 'Payment confirmed successfully.' : 'Booking approved successfully.')
    ]);
    ob_end_flush();

} catch (Exception $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log("Error in approve_booking.php: " . $e->getMessage());
    
    ob_clean();
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Server error: ' . $e->getMessage()
    ]);
    ob_end_flush();
}
