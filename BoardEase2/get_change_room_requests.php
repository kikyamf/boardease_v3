<?php
// Enable error reporting and logging for debugging
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', __DIR__ . '/php_errors.log');

// Log start of execution
error_log("=== GET_CHANGE_ROOM_REQUESTS.PHP START ===");
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

    $owner_id = isset($data['owner_id']) ? intval($data['owner_id']) : 0;

    if ($owner_id === 0) {
        ob_clean();
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Owner ID is required']);
        ob_end_flush();
        exit;
    }

    $stmt = $pdo->prepare("
        SELECT crr.*, u.user_fname, u.user_lname, 
               bhr_old.room_name as old_room_name, 
               bhr_new.room_name as new_room_name,
               bh_new.bh_name as new_bh_name
        FROM change_room_requests crr
        JOIN users u ON crr.user_id = u.user_id
        JOIN bookings b ON crr.booking_id = b.booking_id
        JOIN boarding_house_rooms bhr_old ON b.room_id = bhr_old.bhr_id
        JOIN boarding_house_rooms bhr_new ON crr.new_room_id = bhr_new.bhr_id
        JOIN boarding_houses bh_new ON bhr_new.bh_id = bh_new.bh_id
        JOIN boarding_houses bh_old ON bhr_old.bh_id = bh_old.bh_id
        WHERE bh_old.user_id = ? AND crr.status = 'Pending'
    ");
    $stmt->execute([$owner_id]);
    $requests = $stmt->fetchAll(PDO::FETCH_ASSOC);

    ob_clean();
    echo json_encode(['success' => true, 'requests' => $requests]);
    ob_end_flush();

} catch (Exception $e) {
    if (function_exists('error_log')) {
        error_log("Error in get_change_room_requests.php: " . $e->getMessage());
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
