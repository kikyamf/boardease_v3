<?php
// BoardEase2/create_booking.php
// Updated to use Live DB credentials and mysqli for consistency.
// Note: Hand-modified to align with mysqli while preserving fixed logic.

// Enable error reporting and logging for debugging
error_reporting(E_ALL);
ini_set('display_errors', 0); 
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

ob_start();

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, ngrok-skip-browser-warning, User-Agent, Accept');

error_log("=== CREATE_BOOKING.PHP ACCESSED (Live) ===");

// Database configuration
define('DB_HOST', 'localhost');
define('DB_USER', 'u223444398_userboardease');
define('DB_PASS', '!Boardease2026');
define('DB_NAME', 'u223444398_boardease');

try {
    $conn = new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME);

    if ($conn->connect_error) {
        throw new Exception("Database connection failed: " . $conn->connect_error);
    }
    
    $conn->begin_transaction();
    
    // Get POST data
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
    
    if ($roomId == 0 || $userId == 0 || empty($startDate) || empty($endDate)) {
        throw new Exception("Missing required fields", 400);
    }
    
    // Resolve actualUserId
    $userSql = "SELECT user_id FROM users WHERE user_id = ? OR reg_id = ? LIMIT 1";
    $userStmt = $conn->prepare($userSql);
    $userStmt->bind_param("ii", $userId, $userId);
    $userStmt->execute();
    $userRes = $userStmt->get_result();
    $userRow = $userRes->fetch_assoc();
    $actualUserId = $userRow ? $userRow['user_id'] : 0;
    $userStmt->close();

    if ($actualUserId == 0) {
        throw new Exception("User not found", 404);
    }

    // NEW CHECK: Prevent boarder from applying for the SAME room if they already have a Pending/Approved application
    $dupSql = "
        SELECT booking_id, booking_status 
        FROM bookings 
        WHERE user_id = ? 
        AND room_id = ? 
        AND booking_status IN ('Pending', 'Approved')
        LIMIT 1
    ";
    $dupStmt = $conn->prepare($dupSql);
    $dupStmt->bind_param("ii", $actualUserId, $roomId);
    $dupStmt->execute();
    $dupRes = $dupStmt->get_result();
    
    if ($duplicate = $dupRes->fetch_assoc()) {
        $statusMsg = $duplicate['booking_status'] === 'Approved' ? 'an approved' : 'a pending';
        throw new Exception("You already have $statusMsg application for this room.", 400);
    }
    $dupStmt->close();

    // Private Room Overlap Check
    $catSql = "SELECT bhr.room_category FROM room_units ru JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id WHERE ru.room_id = ?";
    $catStmt = $conn->prepare($catSql);
    $catStmt->bind_param("i", $roomId);
    $catStmt->execute();
    $catRes = $catStmt->get_result();
    $catRow = $catRes->fetch_assoc();
    $catStmt->close();

    if ($catRow && $catRow['room_category'] === 'Private Room') {
        $overlapSql = "
            SELECT booking_id FROM bookings 
            WHERE room_id = ? 
            AND booking_status IN ('Confirmed', 'Active')
            AND (
                (start_date <= ? AND end_date >= ?)
                OR (start_date <= ? AND end_date >= ?)
                OR (start_date >= ? AND end_date <= ?)
            ) LIMIT 1
        ";
        $ovStmt = $conn->prepare($overlapSql);
        $ovStmt->bind_param("issssss", $roomId, $startDate, $startDate, $endDate, $endDate, $startDate, $endDate);
        $ovStmt->execute();
        if ($ovStmt->get_result()->fetch_assoc()) {
            throw new Exception("Room is already booked for the selected dates", 400);
        }
        $ovStmt->close();
    }
    
    // Insert Booking
    $insertSql = "INSERT INTO bookings (room_id, user_id, start_date, end_date, booking_status, booking_date) VALUES (?, ?, ?, ?, 'Pending', NOW())";
    $insStmt = $conn->prepare($insertSql);
    $insStmt->bind_param("iiss", $roomId, $actualUserId, $startDate, $endDate);
    $insStmt->execute();
    $bookingId = $conn->insert_id;
    $insStmt->close();
    
    $conn->commit();
    $conn->close();
    
    ob_clean();
    echo json_encode(['success' => true, 'message' => 'Application submitted successfully', 'booking_id' => $bookingId]);
    ob_end_flush();
    
} catch (Exception $e) {
    if (isset($conn)) {
        $conn->rollback();
        $conn->close();
    }
    ob_clean();
    http_response_code($e->getCode() >= 400 ? $e->getCode() : 500);
    echo json_encode(['success' => false, 'message' => $e->getMessage()]);
    ob_end_flush();
}
?>
