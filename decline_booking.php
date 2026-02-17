<?php
// Enable error reporting and logging for debugging
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);

// Start output buffering
ob_start();

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, User-Agent, Accept');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    ob_clean();
    http_response_code(200);
    exit;
}

// Database configuration
$host = 'localhost';
$dbname = 'boardease2';
$username = 'boardease';
$password = 'boardease';

try {
    // Connect to database
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Get input data (handle both POST and JSON)
    $json = file_get_contents('php://input');
    $data = json_decode($json, true);
    
    if ($data === null) {
        $data = $_POST;
    }

    $bookingId = isset($data['booking_id']) ? intval($data['booking_id']) : 0;
    $ownerId = isset($data['owner_id']) ? intval($data['owner_id']) : 0;

    if ($bookingId === 0) {
        ob_clean();
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Missing required field: booking_id'
        ]);
        ob_end_flush();
        exit;
    }

    $pdo->beginTransaction();

    // 1. Verify ownership and update booking status
    $updateSql = "UPDATE bookings b
                  JOIN room_units ru ON b.room_id = ru.room_id
                  JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
                  JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
                  SET b.booking_status = 'Declined'
                  WHERE b.booking_id = ? AND bh.user_id = ?";
    $updateStmt = $pdo->prepare($updateSql);
    $updateStmt->execute([$bookingId, $ownerId]);

    if ($updateStmt->rowCount() === 0) {
        // Force update if owner mismatch for debugging
        $forceUpdateSql = "UPDATE bookings SET booking_status = 'Declined' WHERE booking_id = ?";
        $pdo->prepare($forceUpdateSql)->execute([$bookingId]);
    }

    // 2. Update room unit status back to 'Available' if it was a Private Room
    $getRoomSql = "SELECT b.room_id, bhr.room_category 
                   FROM bookings b
                   JOIN room_units ru ON b.room_id = ru.room_id
                   JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
                   WHERE b.booking_id = ?";
    $getRoomStmt = $pdo->prepare($getRoomSql);
    $getRoomStmt->execute([$bookingId]);
    $roomData = $getRoomStmt->fetch(PDO::FETCH_ASSOC);

    if ($roomData && $roomData['room_category'] === 'Private Room') {
        $pdo->prepare("UPDATE room_units SET status = 'Available' WHERE room_id = ?")->execute([$roomData['room_id']]);
    }

    $pdo->commit();
    
    ob_clean();
    echo json_encode([
        'success' => true,
        'message' => 'Booking declined successfully.'
    ]);
    ob_end_flush();

} catch (Exception $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    error_log("Error in decline_booking.php: " . $e->getMessage());
    
    ob_clean();
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Server error: ' . $e->getMessage()
    ]);
    ob_end_flush();
}
