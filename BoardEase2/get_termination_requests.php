<?php
// Enable error reporting and logging for debugging
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', __DIR__ . '/php_errors.log');

// Log start of execution
error_log("=== GET_TERMINATION_REQUESTS.PHP START ===");
error_log("Request Time: " . date('Y-m-d H:i:s'));

// Start output buffering
ob_start();

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, ngrok-skip-browser-warning');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    ob_clean();
    http_response_code(200);
    exit;
}

// Database configuration
define('DB_HOST', '');
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
        echo json_encode([
            'success' => false,
            'error' => 'Missing required field: owner_id is required'
        ]);
        ob_end_flush();
        exit;
    }

    // Get termination requests for bookings belonging to the owner's boarding houses
    $sql = "SELECT 
                tr.termination_id,
                tr.booking_id,
                tr.user_id as boarder_id,
                tr.reason,
                tr.details,
                tr.status,
                tr.created_at,
                r.f_name,
                r.l_name,
                bh.bh_name,
                ru.room_number,
                bhr.room_name as room_type
            FROM termination_requests tr
            JOIN bookings b ON tr.booking_id = b.booking_id
            JOIN room_units ru ON b.room_id = ru.room_id
            JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
            JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
            JOIN users u ON tr.user_id = u.user_id
            JOIN registration r ON u.reg_id = r.reg_id
            WHERE bh.user_id = ? AND tr.status = 'Pending'
            ORDER BY tr.created_at DESC";

    $stmt = $pdo->prepare($sql);
    $stmt->execute([$owner_id]);
    $results = $stmt->fetchAll(PDO::FETCH_ASSOC);

    $response = [
        'success' => true,
        'data' => [
            'termination_requests' => $results
        ]
    ];
    
    ob_clean();
    echo json_encode($response);
    ob_end_flush();

} catch (Exception $e) {
    if (function_exists('error_log')) {
        error_log("Error in get_termination_requests.php: " . $e->getMessage());
    }
    
    ob_clean();
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ]);
    ob_end_flush();
}
?>
