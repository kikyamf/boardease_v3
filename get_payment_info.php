<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Content-Type');

// Database configuration
$host = 'localhost';
$dbname = 'boardease_db';
$username = 'root';
$password = '';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
    echo json_encode([
        'success' => false,
        'message' => 'Database connection failed: ' . $e->getMessage()
    ]);
    exit();
}

// Check if request method is POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode([
        'success' => false,
        'message' => 'Only POST method is allowed'
    ]);
    exit();
}

// Get JSON input
$input = json_decode(file_get_contents('php://input'), true);

// If JSON input is not available, try form data
if (!$input) {
    $input = $_POST;
}

// Validate required fields
if (!isset($input['user_id']) || empty(trim($input['user_id']))) {
    echo json_encode([
        'success' => false,
        'message' => 'User ID is required'
    ]);
    exit();
}

$userId = trim($input['user_id']);

try {
    // Get payment information for the user
    $sql = "SELECT gcash_num, gcash_qr FROM registration WHERE id = :user_id AND status = 'approved'";
    $stmt = $pdo->prepare($sql);
    $stmt->bindParam(':user_id', $userId, PDO::PARAM_INT);
    $stmt->execute();
    
    $user = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$user) {
        echo json_encode([
            'success' => false,
            'message' => 'User not found or not approved'
        ]);
        exit();
    }
    
    // Prepare response data
    $response = [
        'success' => true,
        'message' => 'Payment information retrieved successfully',
        'gcash_number' => $user['gcash_num'] ?: '',
        'qr_code_path' => $user['gcash_qr'] ?: ''
    ];
    
    echo json_encode($response);
    
} catch (PDOException $e) {
    error_log("Database error in get_payment_info.php: " . $e->getMessage());
    echo json_encode([
        'success' => false,
        'message' => 'Database error occurred. Please try again later.'
    ]);
} catch (Exception $e) {
    error_log("General error in get_payment_info.php: " . $e->getMessage());
    echo json_encode([
        'success' => false,
        'message' => 'An error occurred. Please try again later.'
    ]);
}
?>
