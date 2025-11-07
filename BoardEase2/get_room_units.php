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
    
    // Get bhr_id from request
    $bhrId = isset($_POST['bhr_id']) ? intval($_POST['bhr_id']) : (isset($_GET['bhr_id']) ? intval($_GET['bhr_id']) : 0);
    
    if ($bhrId === 0) {
        echo json_encode(array(
            'success' => false,
            'error' => 'Room ID (bhr_id) is required.'
        ));
        exit();
    }
    
    // Fetch room units for this bhr_id
    $sql = "
        SELECT 
            room_id,
            bhr_id,
            room_number,
            status
        FROM room_units
        WHERE bhr_id = ?
        ORDER BY room_number ASC
    ";
    
    $stmt = $pdo->prepare($sql);
    $stmt->execute([$bhrId]);
    $units = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Format the response
    $response = array(
        'success' => true,
        'units' => $units
    );
    
    echo json_encode($response);
    
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

