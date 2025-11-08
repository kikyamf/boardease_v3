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
    
    // Check if favorite already exists
    $checkSql = "SELECT fav_id FROM boarder_favorites WHERE user_id = ? AND bh_id = ?";
    $checkStmt = $pdo->prepare($checkSql);
    $checkStmt->execute([$userId, $bhId]);
    $existing = $checkStmt->fetch(PDO::FETCH_ASSOC);
    
    if ($existing) {
        // Already favorited
        echo json_encode(array(
            'success' => true,
            'message' => 'Already in favorites',
            'fav_id' => $existing['fav_id']
        ));
        exit();
    }
    
    // Insert new favorite
    $insertSql = "INSERT INTO boarder_favorites (user_id, bh_id) VALUES (?, ?)";
    $insertStmt = $pdo->prepare($insertSql);
    $insertStmt->execute([$userId, $bhId]);
    
    $favId = $pdo->lastInsertId();
    
    echo json_encode(array(
        'success' => true,
        'message' => 'Added to favorites',
        'fav_id' => $favId
    ));
    
} catch (PDOException $e) {
    error_log("Database error in add_favorite.php: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ));
} catch (Exception $e) {
    error_log("Server error in add_favorite.php: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'error' => 'Server error: ' . $e->getMessage()
    ));
}
?>

