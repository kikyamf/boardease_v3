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
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, ngrok-skip-browser-warning, User-Agent, Accept');

// Database configuration
$host = 'localhost';
$dbname = 'boardease2';
$username = 'boardease';
$password = 'boardease';

try {
    // Create PDO connection
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Get user_id from request - could be users.user_id OR registrations.id
    $userId = isset($_POST['user_id']) ? intval($_POST['user_id']) : (isset($_GET['user_id']) ? intval($_GET['user_id']) : 0);
    
    if ($userId === 0) {
        echo json_encode(array(
            'success' => false,
            'error' => 'User ID is required.'
        ));
        exit();
    }
    
    // Try to find by registrations.id first (most common case from boarding house)
    $sql = "SELECT gcash_qr FROM registrations WHERE id = :reg_id";
    $stmt = $pdo->prepare($sql);
    $stmt->execute([':reg_id' => $userId]);
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    
    // If not found, try to get reg_id from users table (if userId is users.user_id)
    if (!$result) {
        $getRegIdSql = "SELECT reg_id FROM users WHERE user_id = :user_id";
        $getRegIdStmt = $pdo->prepare($getRegIdSql);
        $getRegIdStmt->execute([':user_id' => $userId]);
        $userData = $getRegIdStmt->fetch(PDO::FETCH_ASSOC);
        
        if ($userData && $userData['reg_id']) {
            $regId = $userData['reg_id'];
            // Fetch from registrations using reg_id
            $sql = "SELECT gcash_qr FROM registrations WHERE id = :reg_id";
            $stmt = $pdo->prepare($sql);
            $stmt->execute([':reg_id' => $regId]);
            $result = $stmt->fetch(PDO::FETCH_ASSOC);
        }
    }
    
    if ($result) {
        $gcashQr = $result['gcash_qr'] ?? '';
        
        // Return the QR code path
        echo json_encode(array(
            'success' => true,
            'gcash_qr' => $gcashQr
        ));
    } else {
        echo json_encode(array(
            'success' => false,
            'error' => 'GCash QR code not found for this user'
        ));
    }
    
} catch (PDOException $e) {
    error_log("Database error: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ));
} catch (Exception $e) {
    error_log("Server error: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'error' => 'Server error: ' . $e->getMessage()
    ));
}
?>

