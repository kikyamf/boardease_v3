<?php
// BoardEase2/process_change_room_request.php
// Updated to use Live DB credentials and mysqli for production consistency.

// Enable error reporting and logging for debugging
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', __DIR__ . '/php_errors.log');

error_log("=== PROCESS_CHANGE_ROOM_REQUEST.PHP START (Live) ===");

ob_start();

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, User-Agent, Accept');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    ob_clean();
    http_response_code(200);
    exit;
}

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

    // Get input
    $json = file_get_contents('php://input');
    $data = json_decode($json, true) ?? array_merge($_GET, $_POST);

    $request_id = isset($data['request_id']) ? intval($data['request_id']) : 0;
    $action = isset($data['action']) ? $data['action'] : ''; 

    if ($request_id == 0 || empty($action)) {
        throw new Exception("Request ID and Action are required", 400);
    }

    if ($action === 'Approve') {
        $conn->begin_transaction();

        // 1. Get request details
        $stmt = $conn->prepare("SELECT * FROM change_room_requests WHERE change_request_id = ?");
        $stmt->bind_param("i", $request_id);
        $stmt->execute();
        $request = $stmt->get_result()->fetch_assoc();
        $stmt->close();

        if (!$request) {
            throw new Exception("Request not found", 404);
        }

        // 2. Create new booking entry
        // Note: New booking inherits date range and BH ID from the request's context, status is 'Approved'
        $bookingSql = "
            INSERT INTO bookings (room_id, user_id, start_date, end_date, bh_id, booking_status, approval_date)
            SELECT ?, user_id, start_date, end_date, (SELECT bh_id FROM boarding_house_rooms WHERE bhr_id = ?), 'Approved', NOW()
            FROM bookings WHERE booking_id = ?
        ";
        $stmtBooking = $conn->prepare($bookingSql);
        $stmtBooking->bind_param("iii", $request['new_room_id'], $request['new_room_id'], $request['booking_id']);
        $stmtBooking->execute();
        $stmtBooking->close();

        // 3. Update request status
        $updateReqStmt = $conn->prepare("UPDATE change_room_requests SET status = 'Approved' WHERE change_request_id = ?");
        $updateReqStmt->bind_param("i", $request_id);
        $updateReqStmt->execute();
        $updateReqStmt->close();

        $conn->commit();
        
        ob_clean();
        echo json_encode(['success' => true, 'message' => 'Request approved. New booking created with expiration tracking.']);
        ob_end_flush();
    } else {
        // Decline
        $stmt = $conn->prepare("UPDATE change_room_requests SET status = 'Declined' WHERE change_request_id = ?");
        $stmt->bind_param("i", $request_id);
        $stmt->execute();
        $stmt->close();
        
        ob_clean();
        echo json_encode(['success' => true, 'message' => 'Request declined']);
        ob_end_flush();
    }

    $conn->close();

} catch (Exception $e) {
    if (isset($conn) && $conn->connect_errno == 0 && $conn->ping()) {
        $conn->rollback();
        $conn->close();
    }
    ob_clean();
    http_response_code($e->getCode() >= 400 ? $e->getCode() : 500);
    echo json_encode(['success' => false, 'message' => $e->getMessage()]);
    ob_end_flush();
}
?>
