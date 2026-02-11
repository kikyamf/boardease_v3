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

    // Get change room requests with boarder info, boarding house info, and room details
    $sql = "SELECT 
                crr.change_request_id,
                crr.booking_id,
                crr.user_id,
                crr.new_room_id,
                crr.new_unit_id,
                crr.reason,
                crr.details,
                crr.status,
                crr.created_at,
                r.first_name as f_name,
                r.last_name as l_name,
                bh.bh_name,
                bhr_old.room_name as old_room_name,
                ru_old.room_number as old_room_number,
                bhr_new.room_name as new_room_name,
                ru_new.room_number as new_room_number
            FROM change_room_requests crr
            -- Join for boarder info
            JOIN users u ON crr.user_id = u.user_id
            JOIN registrations r ON u.reg_id = r.id
            -- Join for current booking room info
            JOIN bookings b ON crr.booking_id = b.booking_id
            JOIN room_units ru_old ON b.room_id = ru_old.room_id
            JOIN boarding_house_rooms bhr_old ON ru_old.bhr_id = bhr_old.bhr_id
            -- Join for requested room info
            JOIN boarding_house_rooms bhr_new ON crr.new_room_id = bhr_new.bhr_id
            LEFT JOIN room_units ru_new ON crr.new_unit_id = ru_new.room_id
            -- Join for BH info
            JOIN boarding_houses bh ON bhr_new.bh_id = bh.bh_id
            WHERE bh.user_id = ? AND crr.status = 'Pending'
            ORDER BY crr.created_at DESC";

    $stmt = $pdo->prepare($sql);
    
    error_log("Executing query for owner_id: " . $owner_id);
    $stmt->execute([$owner_id]);
    $requests = $stmt->fetchAll(PDO::FETCH_ASSOC);
    error_log("Query returned " . count($requests) . " requests");

    $response = [
        'success' => true,
        'data' => [
            'change_room_requests' => $requests
        ]
    ];
    
    ob_clean();
    echo json_encode($response);
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
