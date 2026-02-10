<?php
// Enable error reporting and logging for debugging
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', __DIR__ . '/php_errors.log');

// Log start of execution
error_log("=== GET_AVAILABLE_ROOMS_FOR_TRANSFER.PHP START ===");
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

    $booking_id = isset($data['booking_id']) ? intval($data['booking_id']) : 0;
    $bh_id = isset($data['bh_id']) ? intval($data['bh_id']) : 0;

    if ($booking_id == 0 || $bh_id == 0) {
        ob_clean();
        http_response_code(400);
        echo json_encode(['success' => false, 'message' => 'Booking ID and BH ID are required']);
        ob_end_flush();
        exit;
    }

    // Get the owner of the current BH
    $ownerStmt = $pdo->prepare("SELECT user_id FROM boarding_houses WHERE bh_id = ?");
    $ownerStmt->execute([$bh_id]);
    $owner = $ownerStmt->fetch(PDO::FETCH_ASSOC);
    $owner_id = $owner ? $owner['user_id'] : 0;

    if ($owner_id == 0) {
        ob_clean();
        http_response_code(404);
        echo json_encode(['success' => false, 'message' => 'Owner not found']);
        ob_end_flush();
        exit;
    }

    // 1. Available rooms in current BH (excluding current room)
    $stmt1 = $pdo->prepare("
        SELECT bhr.* 
        FROM boarding_house_rooms bhr
        WHERE bhr.bh_id = ? 
        AND bhr.bhr_id NOT IN (SELECT room_id FROM bookings WHERE booking_id = ?)
    ");
    $stmt1->execute([$bh_id, $booking_id]);
    $currentBhRooms = $stmt1->fetchAll(PDO::FETCH_ASSOC);

    // Fetch units for current BH rooms
    foreach ($currentBhRooms as &$room) {
        $unitStmt = $pdo->prepare("SELECT room_id, room_number, status FROM room_units WHERE bhr_id = ? AND status = 'Available'");
        $unitStmt->execute([$room['bhr_id']]);
        $room['units'] = $unitStmt->fetchAll(PDO::FETCH_ASSOC);
    }

    // 2. Other BHs of the same owner and their available rooms
    $stmt2 = $pdo->prepare("
        SELECT bh.bh_id, bh.bh_name, bhr.bhr_id, bhr.room_name, bhr.room_category, bhr.price, bhr.capacity
        FROM boarding_houses bh
        JOIN boarding_house_rooms bhr ON bh.bh_id = bhr.bh_id
        WHERE bh.user_id = ? AND bh.bh_id != ?
    ");
    $stmt2->execute([$owner_id, $bh_id]);
    $otherBhRooms = $stmt2->fetchAll(PDO::FETCH_ASSOC);

    // Fetch units for other BH rooms
    foreach ($otherBhRooms as &$room) {
        $unitStmt = $pdo->prepare("SELECT room_id, room_number, status FROM room_units WHERE bhr_id = ? AND status = 'Available'");
        $unitStmt->execute([$room['bhr_id']]);
        $room['units'] = $unitStmt->fetchAll(PDO::FETCH_ASSOC);
    }

    ob_clean();
    echo json_encode([
        'success' => true, 
        'data' => [
            'current_bh_name' => '', // Optionally fetch current bh name
            'current_bh_rooms' => $currentBhRooms,
            'other_bh_rooms' => $otherBhRooms
        ]
    ]);
    ob_end_flush();

} catch (Exception $e) {
    if (function_exists('error_log')) {
        error_log("Error in get_available_rooms_for_transfer.php: " . $e->getMessage());
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
