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
    
    // Get POST data
    $userId = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
    $bhId = isset($_POST['bh_id']) ? intval($_POST['bh_id']) : 0;
    
    // Validate required fields
    if ($userId === 0 || $bhId === 0) {
        echo json_encode(array(
            'success' => false,
            'error' => 'User ID and Boarding House ID are required.'
        ));
        exit();
    }
    
    // Delete favorite
    $deleteSql = "DELETE FROM boarder_favorites WHERE user_id = ? AND bh_id = ?";
    $deleteStmt = $pdo->prepare($deleteSql);
    $deleteStmt->execute([$userId, $bhId]);
    
    $rowsAffected = $deleteStmt->rowCount();
    
    if ($rowsAffected > 0) {
        echo json_encode(array(
            'success' => true,
            'message' => 'Removed from favorites'
        ));
    } else {
        echo json_encode(array(
            'success' => false,
            'error' => 'Favorite not found'
        ));
    }
    
} catch (PDOException $e) {
    error_log("Database error in remove_favorite.php: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ));
} catch (Exception $e) {
    error_log("Server error in remove_favorite.php: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'error' => 'Server error: ' . $e->getMessage()
    ));
}
?>

