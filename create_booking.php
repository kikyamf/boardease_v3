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
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Get POST data
    $roomId = isset($_POST['room_id']) ? intval($_POST['room_id']) : 0;
    $userId = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
    $startDate = isset($_POST['start_date']) ? trim($_POST['start_date']) : '';
    $endDate = isset($_POST['end_date']) ? trim($_POST['end_date']) : '';
    
    // Validate required fields
    if ($roomId == 0 || $userId == 0 || empty($startDate) || empty($endDate)) {
        echo json_encode(array(
            'success' => false,
            'message' => 'Missing required fields'
        ));
        exit;
    }
    
    // Validate date format
    $startDateObj = DateTime::createFromFormat('Y-m-d', $startDate);
    $endDateObj = DateTime::createFromFormat('Y-m-d', $endDate);
    
    if (!$startDateObj || !$endDateObj) {
        echo json_encode(array(
            'success' => false,
            'message' => 'Invalid date format. Expected YYYY-MM-DD'
        ));
        exit;
    }
    
    // Validate end date is after start date
    if ($endDateObj <= $startDateObj) {
        echo json_encode(array(
            'success' => false,
            'message' => 'End date must be after start date'
        ));
        exit;
    }
    
    // Check if room exists and is available
    $checkRoomSql = "SELECT bhr_id, status FROM boarding_house_rooms WHERE bhr_id = :room_id";
    $checkRoomStmt = $pdo->prepare($checkRoomSql);
    $checkRoomStmt->execute([':room_id' => $roomId]);
    $room = $checkRoomStmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$room) {
        echo json_encode(array(
            'success' => false,
            'message' => 'Room not found'
        ));
        exit;
    }
    
    // Check if user exists
    $checkUserSql = "SELECT id FROM registrations WHERE id = :user_id";
    $checkUserStmt = $pdo->prepare($checkUserSql);
    $checkUserStmt->execute([':user_id' => $userId]);
    $user = $checkUserStmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$user) {
        echo json_encode(array(
            'success' => false,
            'message' => 'User not found'
        ));
        exit;
    }
    
    // Check for overlapping bookings
    $checkOverlapSql = "
        SELECT booking_id 
        FROM bookings 
        WHERE room_id = :room_id 
        AND booking_status IN ('Pending', 'Approved')
        AND (
            (start_date <= :start_date AND end_date >= :start_date)
            OR (start_date <= :end_date AND end_date >= :end_date)
            OR (start_date >= :start_date AND end_date <= :end_date)
        )
    ";
    $checkOverlapStmt = $pdo->prepare($checkOverlapSql);
    $checkOverlapStmt->execute([
        ':room_id' => $roomId,
        ':start_date' => $startDate,
        ':end_date' => $endDate
    ]);
    
    if ($checkOverlapStmt->fetch()) {
        echo json_encode(array(
            'success' => false,
            'message' => 'Room is already booked for the selected dates'
        ));
        exit;
    }
    
    // Insert booking (only fields that exist in bookings table)
    $insertSql = "
        INSERT INTO bookings (
            room_id, 
            user_id, 
            start_date, 
            end_date, 
            booking_status, 
            booking_date
        ) VALUES (
            :room_id,
            :user_id,
            :start_date,
            :end_date,
            'Pending',
            NOW()
        )
    ";
    
    $insertStmt = $pdo->prepare($insertSql);
    $insertStmt->execute([
        ':room_id' => $roomId,
        ':user_id' => $userId,
        ':start_date' => $startDate,
        ':end_date' => $endDate
    ]);
    
    $bookingId = $pdo->lastInsertId();
    
    echo json_encode(array(
        'success' => true,
        'message' => 'Booking created successfully',
        'booking_id' => $bookingId
    ));
    
} catch (PDOException $e) {
    error_log("Database error: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'message' => 'Database error: ' . $e->getMessage()
    ));
} catch (Exception $e) {
    error_log("Server error: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'message' => 'Server error: ' . $e->getMessage()
    ));
}
?>

