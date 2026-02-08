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
    $booking_id = isset($_POST['booking_id']) ? intval($_POST['booking_id']) : 0;
    $user_id = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
    $reason = isset($_POST['reason']) ? $_POST['reason'] : '';
    $details = isset($_POST['details']) ? $_POST['details'] : '';

    if ($booking_id === 0 || $user_id === 0 || empty($reason)) {
        echo json_encode(['success' => false, 'error' => 'Missing required fields.']);
        exit();
    }

    // Check if booking exists and belongs to the user
    $checkSql = "SELECT booking_id FROM bookings WHERE booking_id = ? AND user_id = ?";
    $checkStmt = $pdo->prepare($checkSql);
    $checkStmt->execute([$booking_id, $user_id]);
    
    if (!$checkStmt->fetch()) {
        echo json_encode(['success' => false, 'error' => 'Booking not found or access denied.']);
        exit();
    }

    // Insert termination request
    $sql = "INSERT INTO termination_requests (booking_id, user_id, reason, details, status) VALUES (?, ?, ?, ?, 'Pending')";
    $stmt = $pdo->prepare($sql);
    $stmt->execute([$booking_id, $user_id, $reason, $details]);

    echo json_encode(['success' => true, 'message' => 'Termination request submitted successfully.']);

} catch (PDOException $e) {
    echo json_encode(['success' => false, 'error' => 'Database error: ' . $e->getMessage()]);
}
?>
