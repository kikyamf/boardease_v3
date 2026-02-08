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

    // Update termination request status to 'Declined'
    $updateSql = "UPDATE termination_requests tr
                  JOIN bookings b ON tr.booking_id = b.booking_id
                  JOIN boarding_house_rooms bhr ON b.room_id = bhr.bhr_id
                  JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
                  SET tr.status = 'Declined'
                  WHERE tr.termination_id = ? AND bh.user_id = ?";
    $updateStmt = $pdo->prepare($updateSql);
    $updateStmt->execute([$termination_id, $owner_id]);

    if ($updateStmt->rowCount() === 0) {
        echo json_encode(['success' => false, 'error' => 'Termination request not found or access denied.']);
        exit();
    }

    echo json_encode(['success' => true, 'message' => 'Termination request declined.']);

} catch (PDOException $e) {
    echo json_encode(['success' => false, 'error' => 'Database error: ' . $e->getMessage()]);
}
?>
