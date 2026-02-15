<?php
// Enable error reporting and logging for debugging
error_reporting(E_ALL);
ini_set('display_errors', 0); // Don't display errors to user, but log them
ini_set('log_errors', 1);
ini_set('error_log', __DIR__ . '/php_errors.log');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type, ngrok-skip-browser-warning, User-Agent, Accept');
    header('Access-Control-Max-Age: 86400');
    http_response_code(200);
    exit;
}

// Start output buffering to prevent any unwanted output
ob_start();

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, ngrok-skip-browser-warning, User-Agent, Accept');

// Log that create_booking.php was accessed
error_log("=== CREATE_BOOKING.PHP ACCESSED ===");
error_log("Timestamp: " . date('Y-m-d H:i:s'));
error_log("Request Method: " . $_SERVER['REQUEST_METHOD']);
error_log("Request URI: " . ($_SERVER['REQUEST_URI'] ?? 'N/A'));

// Database configuration
define('DB_HOST', 'localhost');
define('DB_USER', 'u223444398_userboardease');
define('DB_PASS', '!Boardease2026');
define('DB_NAME', 'u223444398_boardease');

$host = DB_HOST;
$dbname = DB_NAME;
$username = DB_USER;
$password = DB_PASS;

try {
    // Create PDO connection
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Start transaction to ensure all operations succeed or fail together
    $pdo->beginTransaction();
    
    // Get POST data - handle both POST and JSON input
    $inputData = [];
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        if (!empty($_POST)) {
            $inputData = $_POST;
        } else {
            // Try to get JSON input
            $jsonInput = file_get_contents('php://input');
            if (!empty($jsonInput)) {
                $decoded = json_decode($jsonInput, true);
                if (json_last_error() === JSON_ERROR_NONE) {
                    $inputData = $decoded;
                }
            }
        }
    }
    
    $roomId = isset($inputData['room_id']) ? intval($inputData['room_id']) : 0;
    $userId = isset($inputData['user_id']) ? intval($inputData['user_id']) : 0;
    $startDate = isset($inputData['start_date']) ? trim($inputData['start_date']) : '';
    $endDate = isset($inputData['end_date']) ? trim($inputData['end_date']) : '';
    $paymentMethod = isset($inputData['payment_method']) ? trim($inputData['payment_method']) : 'Cash';
    $paymentProofBase64 = isset($inputData['payment_proof']) ? trim($inputData['payment_proof']) : '';
    
    // Get calculated payment amount and number of days from Android
    $totalAmount = isset($inputData['total_amount']) ? floatval($inputData['total_amount']) : 0;
    $numberOfDays = isset($inputData['number_of_days']) ? intval($inputData['number_of_days']) : 0;
    $paymentBreakdownJson = isset($inputData['payment_breakdown']) ? $inputData['payment_breakdown'] : '';
    
    // Debug logging
    error_log("create_booking.php - Received data: room_id=$roomId, user_id=$userId, start_date=$startDate, end_date=$endDate, total_amount=$totalAmount, number_of_days=$numberOfDays");
    
    // Validate required fields
    if ($roomId == 0 || $userId == 0 || empty($startDate) || empty($endDate)) {
        ob_clean();
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        http_response_code(400);
        echo json_encode(array(
            'success' => false,
            'message' => 'Missing required fields'
        ));
        ob_end_flush();
        exit;
    }
    
    // Validate date format
    $startDateObj = DateTime::createFromFormat('Y-m-d', $startDate);
    $endDateObj = DateTime::createFromFormat('Y-m-d', $endDate);
    
    if (!$startDateObj || !$endDateObj) {
        ob_clean();
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        http_response_code(400);
        echo json_encode(array(
            'success' => false,
            'message' => 'Invalid date format. Expected YYYY-MM-DD'
        ));
        ob_end_flush();
        exit;
    }
    
    // Validate end date is after start date
    if ($endDateObj <= $startDateObj) {
        ob_clean();
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        http_response_code(400);
        echo json_encode(array(
            'success' => false,
            'message' => 'End date must be after start date'
        ));
        ob_end_flush();
        exit;
    }
    
    // Check if room unit exists in room_units table
    $checkRoomUnitSql = "SELECT room_id, bhr_id, room_number, status FROM room_units WHERE room_id = :room_id FOR UPDATE";
    $checkRoomUnitStmt = $pdo->prepare($checkRoomUnitSql);
    $checkRoomUnitStmt->execute([':room_id' => $roomId]);
    $roomUnit = $checkRoomUnitStmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$roomUnit) {
        ob_clean();
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        http_response_code(404);
        echo json_encode(array(
            'success' => false,
            'message' => 'Room unit not found'
        ));
        ob_end_flush();
        exit;
    }
    
    $bhrId = $roomUnit['bhr_id'];
    $getRoomInfoSql = "SELECT room_category, capacity FROM boarding_house_rooms WHERE bhr_id = :bhr_id";
    $getRoomInfoStmt = $pdo->prepare($getRoomInfoSql);
    $getRoomInfoStmt->execute([':bhr_id' => $bhrId]);
    $roomInfo = $getRoomInfoStmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$roomInfo) {
        ob_clean();
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        http_response_code(404);
        echo json_encode(array(
            'success' => false,
            'message' => 'Room information not found'
        ));
        ob_end_flush();
        exit;
    }
    
    $roomCategory = $roomInfo['room_category'];
    $capacity = intval($roomInfo['capacity']);
    $actualRoomId = $roomId;
    
    // --- USER MODIFICATION START ---
    // STEP 1.5: REMOVED ACTIVE BOOKING CHECK
    // We now allow users to have multiple active/confirmed bookings.
    error_log("Step 1.5 Skipped: Active booking check removed to allow multiple stays.");
    // --- USER MODIFICATION END ---
    
    // NEW CHECK: Prevent boarder from applying for the SAME room if they already have a Pending application for it
    $checkDuplicatePendingSql = "
        SELECT booking_id, booking_status 
        FROM bookings 
        WHERE user_id = (SELECT user_id FROM users WHERE user_id = :user_id OR reg_id = :user_id LIMIT 1) 
        AND room_id = :room_id 
        AND booking_status IN ('Pending', 'Approved')
        LIMIT 1
    ";
    // We need to resolve actualUserId first to be consistent with the original logic
    $checkUserByUserIdSql = "SELECT user_id FROM users WHERE user_id = :user_id OR reg_id = :user_id LIMIT 1";
    $checkUserStmt = $pdo->prepare($checkUserByUserIdSql);
    $checkUserStmt->execute([':user_id' => $userId]);
    $user = $checkUserStmt->fetch(PDO::FETCH_ASSOC);
    $actualUserId = $user ? $user['user_id'] : 0;

    if ($actualUserId > 0) {
        $checkDuplicatePendingStmt = $pdo->prepare($checkDuplicatePendingSql);
        $checkDuplicatePendingStmt->execute([':user_id' => $actualUserId, ':room_id' => $actualRoomId]);
        
        if ($duplicate = $checkDuplicatePendingStmt->fetch()) {
            ob_clean();
            if ($pdo->inTransaction()) {
                $pdo->rollBack();
            }
            http_response_code(400);
            $statusMsg = $duplicate['booking_status'] === 'Approved' ? 'an approved' : 'a pending';
            echo json_encode(array(
                'success' => false,
                'message' => "You already have $statusMsg application for this room."
            ));
            ob_end_flush();
            exit;
        }
    }

    // Step 2: Overlapping check (standard logic)
    if ($roomCategory === 'Private Room') {
        $checkOverlapSql = "
            SELECT b.booking_id 
            FROM bookings b
            WHERE b.room_id = :room_id 
            AND b.booking_status IN ('Confirmed', 'Active')
            AND (
                (b.start_date <= :start_date AND b.end_date >= :start_date)
                OR (b.start_date <= :end_date AND b.end_date >= :end_date)
                OR (b.start_date >= :start_date AND b.end_date <= :end_date)
            )
            LIMIT 1
        ";
        $checkOverlapStmt = $pdo->prepare($checkOverlapSql);
        $checkOverlapStmt->execute([
            ':room_id' => $actualRoomId,
            ':start_date' => $startDate,
            ':end_date' => $endDate
        ]);
        
        if ($checkOverlapStmt->fetch()) {
            ob_clean();
            $pdo->rollBack();
            http_response_code(400);
            echo json_encode(array(
                'success' => false,
                'message' => 'Room is already booked for the selected dates'
            ));
            ob_end_flush();
            exit;
        }
    }
    
    // Step 4: Create booking
    $insertSql = "
        INSERT INTO bookings (
            room_id, 
            user_id, 
            start_date, 
            end_date, 
            booking_status, 
            booking_date
        ) VALUES (
            :room_id,
            :user_id,
            :start_date,
            :end_date,
            'Pending',
            NOW()
        )
    ";
    
    $insertStmt = $pdo->prepare($insertSql);
    $insertStmt->execute([
        ':room_id' => $actualRoomId,
        ':user_id' => $actualUserId,
        ':start_date' => $startDate,
        ':end_date' => $endDate
    ]);
    
    $bookingId = $pdo->lastInsertId();
    
    // Step 8: Commit
    $pdo->commit();
    
    ob_clean();
    http_response_code(200);
    echo json_encode(array(
        'success' => true,
        'message' => 'Application submitted successfully',
        'booking_id' => $bookingId
    ));
    ob_end_flush();
    
} catch (Exception $e) {
    ob_clean();
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    http_response_code(500);
    echo json_encode(array(
        'success' => false,
        'message' => 'Server error: ' . $e->getMessage()
    ));
    ob_end_flush();
}
?>
