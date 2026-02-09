<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, User-Agent, Accept');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

$host = 'localhost';
$dbname = 'u223444398_boardease';
$username = 'u223444398_userboardease';
$password = '!Boardease2026';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Ensure table exists
    $sql = "CREATE TABLE IF NOT EXISTS change_room_requests (
        change_request_id INT AUTO_INCREMENT PRIMARY KEY,
        booking_id INT NOT NULL,
        user_id INT NOT NULL,
        new_room_id INT NOT NULL,
        reason VARCHAR(255) NOT NULL,
        details TEXT,
        status ENUM('Pending', 'Approved', 'Declined') DEFAULT 'Pending',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
        FOREIGN KEY (user_id) REFERENCES registrations(id) ON DELETE CASCADE,
        FOREIGN KEY (new_room_id) REFERENCES boarding_house_rooms(bhr_id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
    $pdo->exec($sql);

    $booking_id = isset($_POST['booking_id']) ? intval($_POST['booking_id']) : 0;
    $user_id = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
    $new_room_id = isset($_POST['new_room_id']) ? intval($_POST['new_room_id']) : 0;
    $reason = isset($_POST['reason']) ? trim($_POST['reason']) : '';
    $details = isset($_POST['details']) ? trim($_POST['details']) : '';

    if ($booking_id == 0 || $user_id == 0 || $new_room_id == 0 || empty($reason)) {
        echo json_encode(['success' => false, 'message' => 'Missing required fields']);
        exit;
    }

    $stmt = $pdo->prepare("INSERT INTO change_room_requests (booking_id, user_id, new_room_id, reason, details, status) VALUES (?, ?, ?, ?, ?, 'Pending')");
    $stmt->execute([$booking_id, $user_id, $new_room_id, $reason, $details]);

    echo json_encode(['success' => true, 'message' => 'Change room request submitted successfully']);

} catch (PDOException $e) {
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>
