<?php
// Enable error reporting and logging for debugging
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);
ini_set('error_log', __DIR__ . '/php_errors.log');

// Log start of execution
error_log("=== TERMINATE_BOOKING.PHP START ===");
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

    // Get input (handling both JSON and conventional POST for robustness)
    $json = file_get_contents('php://input');
    $data = json_decode($json, true);
    
    if ($data === null) {
        $data = $_POST;
    }

    if (function_exists('error_log')) {
        error_log("Received Data: " . print_r($data, true));
    }

    $booking_id = isset($data['booking_id']) ? intval($data['booking_id']) : 0;
    $user_id = isset($data['user_id']) ? intval($data['user_id']) : 0;
    $reason = isset($data['reason']) ? trim($data['reason']) : '';
    $details = isset($data['details']) ? trim($data['details']) : '';

    if ($booking_id === 0 || $user_id === 0 || empty($reason)) {
        ob_clean();
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'error' => 'Missing required fields: booking_id, user_id, and reason are required'
        ]);
        ob_end_flush();
        exit;
    }

    // Check if booking exists and belongs to the user
    $checkSql = "SELECT booking_id FROM bookings WHERE booking_id = ? AND user_id = ?";
    $checkStmt = $pdo->prepare($checkSql);
    $checkStmt->execute([$booking_id, $user_id]);
    
    if (!$checkStmt->fetch()) {
        ob_clean();
        http_response_code(403);
        echo json_encode([
            'success' => false,
            'error' => 'Booking not found or access denied.'
        ]);
        ob_end_flush();
        exit;
    }

    // Insert termination request
    $sql = "INSERT INTO termination_requests (booking_id, user_id, reason, details, status) VALUES (?, ?, ?, ?, 'Pending')";
    $stmt = $pdo->prepare($sql);
    $stmt->execute([$booking_id, $user_id, $reason, $details]);

    $response = [
        'success' => true,
        'message' => 'Termination request submitted successfully.'
    ];
    
    ob_clean();
    echo json_encode($response);
    ob_end_flush();

} catch (Exception $e) {
    if (function_exists('error_log')) {
        error_log("Error in terminate_booking.php: " . $e->getMessage());
    }
    
    ob_clean();
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ]);
    ob_end_flush();
}
?>
