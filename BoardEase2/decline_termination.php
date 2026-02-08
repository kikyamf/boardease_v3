<?php
// Enable error reporting and logging for debugging
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', __DIR__ . '/php_errors.log');

// Log start of execution
error_log("=== DECLINE_TERMINATION.PHP START ===");
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

    // Update termination request status to 'Declined'
    $updateSql = "UPDATE termination_requests tr
                  JOIN bookings b ON tr.booking_id = b.booking_id
                  JOIN room_units ru ON b.room_id = ru.room_id
                  JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
                  JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
                  SET tr.status = 'Declined'
                  WHERE tr.termination_id = ? AND bh.user_id = ?";
    $updateStmt = $pdo->prepare($updateSql);
    $updateStmt->execute([$termination_id, $owner_id]);

    if ($updateStmt->rowCount() === 0) {
        ob_clean();
        http_response_code(403);
        echo json_encode(['success' => false, 'error' => 'Termination request not found or access denied.']);
        ob_end_flush();
        exit;
    }

    $response = [
        'success' => true,
        'message' => 'Termination request declined.'
    ];
    
    ob_clean();
    echo json_encode($response);
    ob_end_flush();

    error_log("Declined termination request $termination_id by owner $owner_id.");

} catch (Exception $e) {
    if (function_exists('error_log')) {
        error_log("Error in decline_termination.php: " . $e->getMessage());
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
