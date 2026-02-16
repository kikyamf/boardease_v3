<?php
// Enable error reporting and logging for debugging
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', __DIR__ . '/php_errors.log');

// Log start of execution
error_log("=== PROCESS_CHANGE_ROOM_REQUEST.PHP START ===");
error_log("Request Time: " . date('Y-m-d H:i:s'));

// Start output buffering
ob_start();

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, User-Agent, Accept');

// Handle preflight OPTIONS request
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

$host = DB_HOST;
$dbname = DB_NAME;
$username = DB_USER;
$password = DB_PASS;

try {
    // Connect to database
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Get input (handling JSON, POST, and GET)
    $json = file_get_contents('php://input');
    $data = json_decode($json, true);
    
    if ($data === null) {
        $data = array_merge($_GET, $_POST);
    }

    if (function_exists('error_log')) {
        error_log("Received Data: " . print_r($data, true));
    }

    $request_id = isset($data['request_id']) ? intval($data['request_id']) : 0;
    $action = isset($data['action']) ? $data['action'] : ''; // 'Approve' or 'Decline'

    if ($request_id == 0 || empty($action)) {
        ob_clean();
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Request ID and Action are required']);
        ob_end_flush();
        exit;
    }

    if ($action === 'Approve') {
        // 1. Get request details
        $stmt = $pdo->prepare("SELECT * FROM change_room_requests WHERE change_request_id = ?");
        $stmt->execute([$request_id]);
        $request = $stmt->fetch(PDO::FETCH_ASSOC);

        if (!$request) {
            ob_clean();
            http_response_code(404);
            echo json_encode(['success' => false, 'message' => 'Request not found']);
            ob_end_flush();
            exit;
        }

        // 2. Create new booking entry (Status: Approved - requires payment)
        $stmtBooking = $pdo->prepare("
            INSERT INTO bookings (room_id, user_id, start_date, end_date, bh_id, status)
            SELECT ?, user_id, start_date, end_date, (SELECT bh_id FROM boarding_house_rooms WHERE bhr_id = ?), 'Approved'
            FROM bookings WHERE booking_id = ?
        ");
        $stmtBooking->execute([$request['new_room_id'], $request['new_room_id'], $request['booking_id']]);

        // 3. Update request status
        $pdo->prepare("UPDATE change_room_requests SET status = 'Approved' WHERE change_request_id = ?")->execute([$request_id]);

        ob_clean();
        echo json_encode(['success' => true, 'message' => 'Request approved. New booking created.']);
        ob_end_flush();
    } else {
        // Decline
        $pdo->prepare("UPDATE change_room_requests SET status = 'Declined' WHERE change_request_id = ?")->execute([$request_id]);
        
        ob_clean();
        echo json_encode(['success' => true, 'message' => 'Request declined']);
        ob_end_flush();
    }

} catch (Exception $e) {
    if (function_exists('error_log')) {
        error_log("Error in process_change_room_request.php: " . $e->getMessage());
    }
    
    ob_clean();
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Database error: ' . $e->getMessage()
    ]);
    ob_end_flush();
}
?>
