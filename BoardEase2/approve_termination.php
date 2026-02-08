<?php
// Enable error reporting and logging for debugging
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', __DIR__ . '/php_errors.log');

// Log start of execution
error_log("=== APPROVE_TERMINATION.PHP START ===");
error_log("Request Time: " . date('Y-m-d H:i:s'));

// Start output buffering
ob_start();

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
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

    // Ensure termination_requests table exists
    $createTableSql = "CREATE TABLE IF NOT EXISTS termination_requests (
        termination_id INT AUTO_INCREMENT PRIMARY KEY,
        booking_id INT NOT NULL,
        user_id INT NOT NULL,
        reason VARCHAR(255) NOT NULL,
        details TEXT,
        status ENUM('Pending', 'Approved', 'Declined') DEFAULT 'Pending',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
    $pdo->exec($createTableSql);

    // Get input (handling both JSON and conventional POST)
    $json = file_get_contents('php://input');
    $data = json_decode($json, true);
    
    if ($data === null) {
        $data = $_POST;
    }

    if (function_exists('error_log')) {
        error_log("Received Data: " . print_r($data, true));
    }

    $termination_id = isset($data['termination_id']) ? intval($data['termination_id']) : 0;
    $owner_id = isset($data['owner_id']) ? intval($data['owner_id']) : 0;

    if ($termination_id === 0 || $owner_id === 0) {
        ob_clean();
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'error' => 'Missing required fields: termination_id and owner_id are required'
        ]);
        ob_end_flush();
        exit;
    }

    $pdo->beginTransaction();

    // 1. Verify access and update termination request status
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
        ob_clean();
        http_response_code(403);
        echo json_encode(['success' => false, 'error' => 'Termination request not found or access denied.']);
        ob_end_flush();
        exit;
    }

    // 2. Get booking_id, room_id, and user_id
    $getSql = "SELECT tr.booking_id, tr.user_id as boarder_id, b.room_id, bhr.room_category, bhr.capacity
               FROM termination_requests tr
               JOIN bookings b ON tr.booking_id = b.booking_id
               JOIN room_units ru ON b.room_id = ru.room_id
               JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
               WHERE tr.termination_id = ?";
    $getStmt = $pdo->prepare($getSql);
    $getStmt->execute([$termination_id]);
    $trData = $getStmt->fetch(PDO::FETCH_ASSOC);
    
    $booking_id = $trData['booking_id'];
    $boarder_id = $trData['boarder_id'];
    $room_id = $trData['room_id'];
    $room_category = $trData['room_category'];

    // 3. Update booking status to 'Cancelled' (standard for terminated bookings in this system)
    $bookingSql = "UPDATE bookings SET booking_status = 'Cancelled' WHERE booking_id = ?";
    $bookingStmt = $pdo->prepare($bookingSql);
    $bookingStmt->execute([$booking_id]);

    // 4. Update room unit status robustly
    if ($room_category === 'Private Room') {
        $pdo->prepare("UPDATE room_units SET status = 'Available' WHERE room_id = ?")->execute([$room_id]);
    } else if ($room_category === 'Bed Spacer') {
        // Recalculate status for bed spacers
        $cStmt = $pdo->prepare("SELECT COUNT(*) FROM bookings WHERE room_id = ? AND booking_status = 'Confirmed'");
        $cStmt->execute([$room_id]);
        $confirmedCount = $cStmt->fetchColumn(); 
        
        if ($confirmedCount == 0) {
            $pdo->prepare("UPDATE room_units SET status = 'Available' WHERE room_id = ?")->execute([$room_id]);
        } else {
            // Still has remaining boarders
            $pdo->prepare("UPDATE room_units SET status = 'Available(Partially Occupied)' WHERE room_id = ?")->execute([$room_id]);
        }
    }

    // 5. Remove from active_boarders
    // More precise delete: based on user_id and room_id
    $activeSql = "DELETE FROM active_boarders WHERE user_id = ? AND room_id = ?";
    $activeStmt = $pdo->prepare($activeSql);
    $activeStmt->execute([$boarder_id, $room_id]);

    $pdo->commit();
    
    $response = [
        'success' => true,
        'message' => 'Termination approved successfully. Room status updated.'
    ];
    
    ob_clean();
    echo json_encode($response);
    
    // Non-blocking post-processing (logging)
    if (function_exists('fastcgi_finish_request')) {
        fastcgi_finish_request();
    } else {
        ob_end_flush();
    }
    
    error_log("Approved termination $termination_id for booking $booking_id. Room $room_id status updated.");

} catch (Exception $e) {
    if (isset($pdo) && $pdo->inTransaction()) {
        $pdo->rollBack();
    }
    if (function_exists('error_log')) {
        error_log("Error in approve_termination.php: " . $e->getMessage());
    }
    
    ob_clean();
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'error' => 'Server error: ' . $e->getMessage()
    ]);
    ob_end_flush();
}
?>
