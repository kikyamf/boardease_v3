<?php
// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type, User-Agent, Accept');
    header('Access-Control-Max-Age: 86400');
    http_response_code(200);
    exit;
}

// Start output buffering
ob_start();

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
    // Create PDO connection
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Start transaction
    $pdo->beginTransaction();
    
    // Get POST data - handle both POST and JSON input
    $inputData = [];
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        if (!empty($_POST)) {
            $inputData = $_POST;
        } else {
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
    $totalAmount = isset($inputData['total_amount']) ? floatval($inputData['total_amount']) : 0;
    $numberOfDays = isset($inputData['number_of_days']) ? intval($inputData['number_of_days']) : 0;
    $paymentBreakdownJson = isset($inputData['payment_breakdown']) ? $inputData['payment_breakdown'] : '';
    
    // Validate required fields
    if ($roomId == 0 || $userId == 0 || empty($startDate) || empty($endDate)) {
        ob_clean();
        if ($pdo->inTransaction()) $pdo->rollBack();
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Missing required fields']);
        exit;
    }
    
    // Check if room unit exists (roomId from Android is room_units.room_id)
    $checkRoomUnitSql = "SELECT ru.room_id, ru.bhr_id, bhr.room_category, bhr.capacity, ru.status 
                         FROM room_units ru
                         JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
                         WHERE ru.room_id = :room_id FOR UPDATE";
    $stmt = $pdo->prepare($checkRoomUnitSql);
    $stmt->execute([':room_id' => $roomId]);
    $roomUnit = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$roomUnit) {
        ob_clean();
        if ($pdo->inTransaction()) $pdo->rollBack();
        http_response_code(404);
        echo json_encode(['success' => false, 'message' => 'Room unit not found']);
        exit;
    }
    
    // Check availability for Private Rooms
    if ($roomUnit['room_category'] === 'Private Room' && $roomUnit['status'] !== 'Available') {
        ob_clean();
        if ($pdo->inTransaction()) $pdo->rollBack();
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Room is not available']);
        exit;
    }

    // Check for overlapping bookings
    $checkOverlapSql = "
        SELECT COUNT(*) FROM bookings 
        WHERE room_id = :room_id 
        AND booking_status IN ('Pending', 'Approved', 'Confirmed')
        AND (
            (start_date <= :start_date AND end_date >= :start_date)
            OR (start_date <= :end_date AND end_date >= :end_date)
            OR (start_date >= :start_date AND end_date <= :end_date)
        )
    ";
    $overlapStmt = $pdo->prepare($checkOverlapSql);
    $overlapStmt->execute([
        ':room_id' => $roomId,
        ':start_date' => $startDate,
        ':end_date' => $endDate
    ]);
    
    if ($overlapStmt->fetchColumn() > ($roomUnit['room_category'] === 'Bed Spacer' ? $roomUnit['capacity'] - 1 : 0)) {
        ob_clean();
        if ($pdo->inTransaction()) $pdo->rollBack();
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Room is fully booked for these dates']);
        exit;
    }

    // Identify actual user_id from users table (since Android sends registrations.id often)
    $actualUserId = $userId;
    $checkUserSql = "SELECT user_id FROM users WHERE reg_id = :reg_id OR user_id = :user_id LIMIT 1";
    $checkUserStmt = $pdo->prepare($checkUserSql);
    $checkUserStmt->execute([':reg_id' => $userId, ':user_id' => $userId]);
    $userData = $checkUserStmt->fetch(PDO::FETCH_ASSOC);
    if ($userData) {
        $actualUserId = $userData['user_id'];
    } else {
        // Create user entry if it doesn't exist
        $pdo->prepare("INSERT INTO users (reg_id) VALUES (?)")->execute([$userId]);
        $actualUserId = $pdo->lastInsertId();
    }
    
    // Update Private Room status to Occupied immediately (Stage 1 logic)
    if ($roomUnit['room_category'] === 'Private Room') {
        $pdo->prepare("UPDATE room_units SET status = 'Occupied' WHERE room_id = ?")->execute([$roomId]);
    }

    // Insert booking
    $insertSql = "INSERT INTO bookings (room_id, user_id, start_date, end_date, booking_status, booking_date) 
                  VALUES (:room_id, :user_id, :start_date, :end_date, 'Pending', NOW())";
    $pdo->prepare($insertSql)->execute([
        ':room_id' => $roomId,
        ':user_id' => $actualUserId,
        ':start_date' => $startDate,
        ':end_date' => $endDate
    ]);
    $bookingId = $pdo->lastInsertId();

    // Note: Payment records are NOT created during application submission.
    // They will be created later when the boarder submits payment after owner approval.

    $pdo->commit();
    ob_clean();
    echo json_encode(['success' => true, 'message' => 'Booking applied successfully', 'booking_id' => $bookingId]);
    ob_end_flush();

} catch (Exception $e) {
    if (isset($pdo) && $pdo->inTransaction()) $pdo->rollBack();
    error_log("Error in root create_booking.php: " . $e->getMessage());
    ob_clean();
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Server error: ' . $e->getMessage()]);
    ob_end_flush();
}
