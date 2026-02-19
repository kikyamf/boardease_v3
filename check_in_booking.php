<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, User-Agent, Accept');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Database configuration
$host = 'localhost';
$dbname = 'u223444398_boardease';
$username = 'u223444398_userboardease';
$password = '!Boardease2026';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $bookingId = isset($_POST['booking_id']) ? intval($_POST['booking_id']) : 0;

    if ($bookingId === 0) {
        echo json_encode(['success' => false, 'message' => 'Booking ID is required.']);
        exit;
    }

    $pdo->beginTransaction();

    // 1. Get booking details and verify it exists and is eligible for check-in
    $sql = "SELECT room_id, start_date, booking_status FROM bookings WHERE booking_id = ?";
    $stmt = $pdo->prepare($sql);
    $stmt->execute([$bookingId]);
    $booking = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$booking) {
        echo json_encode(['success' => false, 'message' => 'Booking not found.']);
        $pdo->rollBack();
        exit;
    }

    $today = date('Y-m-d');
    if ($today < $booking['start_date']) {
        echo json_encode(['success' => false, 'message' => 'Check-in is only allowed on or after the start date: ' . $booking['start_date']]);
        $pdo->rollBack();
        exit;
    }

    if ($booking['booking_status'] === 'Active') {
        echo json_encode(['success' => false, 'message' => 'Booking is already active.']);
        $pdo->rollBack();
        exit;
    }

    if (!in_array($booking['booking_status'], ['Confirmed', 'Upcoming'])) {
        echo json_encode(['success' => false, 'message' => 'Booking status must be Confirmed or Upcoming to check in. Current status: ' . $booking['booking_status']]);
        $pdo->rollBack();
        exit;
    }

    // 2. Update booking status to Active
    $updateBookingSql = "UPDATE bookings SET booking_status = 'Active' WHERE booking_id = ?";
    $updateBookingStmt = $pdo->prepare($updateBookingSql);
    $updateBookingStmt->execute([$bookingId]);

    // 3. Update room status to Occupied
    $updateRoomSql = "UPDATE room_units SET status = 'Occupied' WHERE room_id = ?";
    $updateRoomStmt = $pdo->prepare($updateRoomSql);
    $updateRoomStmt->execute([$booking['room_id']]);

    $pdo->commit();

    echo json_encode([
        'success' => true,
        'message' => 'Check-in successful. Your booking is now active.'
    ]);

} catch (PDOException $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
