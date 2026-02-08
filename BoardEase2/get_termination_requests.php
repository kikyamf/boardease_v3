<?php
// Handle preflight OPTIONS request for CORS
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
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
    $owner_id = isset($_GET['owner_id']) ? intval($_GET['owner_id']) : 0;

    if ($owner_id === 0) {
        echo json_encode(['success' => false, 'error' => 'Owner ID is required.']);
        exit();
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

    echo json_encode([
        'success' => true,
        'data' => [
            'termination_requests' => $results
        ]
    ]);

} catch (PDOException $e) {
    echo json_encode(['success' => false, 'error' => 'Database error: ' . $e->getMessage()]);
}
?>
