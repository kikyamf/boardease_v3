<?php
// Enable error reporting and logging for debugging
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', __DIR__ . '/php_errors.log');

// Log start of execution
error_log("=== SUBMIT_CHANGE_ROOM_REQUEST.PHP START ===");
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

    // Ensure new_unit_id column exists
    try {
        $pdo->exec("ALTER TABLE change_room_requests ADD COLUMN new_unit_id INT DEFAULT NULL AFTER new_room_id");
    } catch (Exception $e) {
        // Ignore if column already exists
    }

    // Get input (handling JSON, POST, and GET)
    $json = file_get_contents('php://input');
    $data = json_decode($json, true);
    
    if ($data === null) {
        $data = array_merge($_GET, $_POST);
    }

    if (function_exists('error_log')) {
        error_log("Received Data: " . print_r($data, true));
    }

    $booking_id = isset($data['booking_id']) ? intval($data['booking_id']) : 0;
    $user_id = isset($data['user_id']) ? intval($data['user_id']) : 0;
    $new_room_id = isset($data['new_room_id']) ? intval($data['new_room_id']) : 0;
    $new_unit_id = isset($data['new_unit_id']) ? intval($data['new_unit_id']) : NULL;
    $reason = isset($data['reason']) ? $data['reason'] : '';
    $details = isset($data['details']) ? $data['details'] : '';

    if ($booking_id == 0 || $user_id == 0 || $new_room_id == 0 || empty($reason)) {
        ob_clean();
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Missing required fields: booking_id=' . $booking_id . ', user_id=' . $user_id . ', new_room_id=' . $new_room_id . ', reason=' . $reason]);
        ob_end_flush();
        exit;
    }

    // Insert request
    $stmt = $pdo->prepare("INSERT INTO change_room_requests (booking_id, user_id, new_room_id, new_unit_id, reason, details) VALUES (?, ?, ?, ?, ?, ?)");
    $stmt->execute([$booking_id, $user_id, $new_room_id, $new_unit_id, $reason, $details]);

    ob_clean();
    echo json_encode(['success' => true, 'message' => 'Request submitted successfully']);
    ob_end_flush();

} catch (Exception $e) {
    if (function_exists('error_log')) {
        error_log("Error in submit_change_room_request.php: " . $e->getMessage());
    }
    
    ob_clean();
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Server Error: ' . $e->getMessage(),
        'debug_info' => [
            'error_info' => $pdo->errorInfo(),
            'exception' => $e->getMessage()
        ]
    ]);
    ob_end_flush();
}
?>
