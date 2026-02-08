<?php
// Handle preflight OPTIONS request for CORS
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Methods: POST, OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type, ngrok-skip-browser-warning, User-Agent, Accept');
    header('Access-Control-Max-Age: 86400');
    http_response_code(200);
    exit();
}

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

// Database configuration
$host = 'localhost';
$dbname = 'boardease2';
$username = 'boardease';
$password = 'boardease';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Get parameters
    $termination_id = isset($_POST['termination_id']) ? intval($_POST['termination_id']) : 0;
    $owner_id = isset($_POST['owner_id']) ? intval($_POST['owner_id']) : 0;

    if ($termination_id === 0 || $owner_id === 0) {
        echo json_encode(['success' => false, 'error' => 'Missing required fields.']);
        exit();
    }

    $pdo->beginTransaction();

    // 1. Update termination request status
    $updateSql = "UPDATE termination_requests tr
                  JOIN bookings b ON tr.booking_id = b.booking_id
                  JOIN room_units ru ON b.room_id = ru.room_id
                  JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
                  JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
                  SET tr.status = 'Approved'
                  WHERE tr.termination_id = ? AND bh.user_id = ?";
    $updateStmt = $pdo->prepare($updateSql);
    $updateStmt->execute([$termination_id, $owner_id]);

    if ($updateStmt->rowCount() === 0) {
        $pdo->rollBack();
        echo json_encode(['success' => false, 'error' => 'Termination request not found or access denied.']);
        exit();
    }

    // 2. Get booking_id and room_id
    $getSql = "SELECT booking_id, user_id FROM termination_requests WHERE termination_id = ?";
    $getStmt = $pdo->prepare($getSql);
    $getStmt->execute([$termination_id]);
    $trData = $getStmt->fetch(PDO::FETCH_ASSOC);
    $booking_id = $trData['booking_id'];
    $boarder_id = $trData['user_id'];

    // 3. Update booking status to 'Cancelled' (or 'Terminated' if exists, but Cancelled is standard here)
    $bookingSql = "UPDATE bookings SET booking_status = 'Cancelled' WHERE booking_id = ?";
    $bookingStmt = $pdo->prepare($bookingSql);
    $bookingStmt->execute([$booking_id]);

    // 4. Update room unit status to 'Available'
    $roomSql = "UPDATE room_units ru
                JOIN bookings b ON ru.room_id = b.room_id
                SET ru.status = 'Available'
                WHERE b.booking_id = ?";
    $roomStmt = $pdo->prepare($roomSql);
    $roomStmt->execute([$booking_id]);

    // 5. Optionally remove from active_boarders
    $activeSql = "DELETE FROM active_boarders WHERE user_id = ? AND boarding_house_id IN (SELECT bh_id FROM boarding_houses WHERE user_id = ?)";
    $activeStmt = $pdo->prepare($activeSql);
    $activeStmt->execute([$boarder_id, $owner_id]);

    $pdo->commit();
    echo json_encode(['success' => true, 'message' => 'Termination approved successfully. Room is now available.']);

} catch (PDOException $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    echo json_encode(['success' => false, 'error' => 'Database error: ' . $e->getMessage()]);
}
?>
