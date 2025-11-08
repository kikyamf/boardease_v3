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
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Get POST or GET data
    $userIdInput = isset($_POST['user_id']) ? intval($_POST['user_id']) : (isset($_GET['user_id']) ? intval($_GET['user_id']) : 0);
    $bhId = isset($_POST['bh_id']) ? intval($_POST['bh_id']) : (isset($_GET['bh_id']) ? intval($_GET['bh_id']) : 0);
    
    error_log("remove_favorite_v2.php - Received user_id: $userIdInput, bh_id: $bhId");
    
    // Validate required fields
    if ($userIdInput === 0 || $bhId === 0) {
        echo json_encode(array(
            'success' => false,
            'error' => 'User ID and Boarding House ID are required.'
        ));
        exit();
    }
    
    // Determine which user_id to use (map to registrations.id)
    $userId = null;
    
    // Check if it's a registrations.id
    $checkRegSql = "SELECT id FROM registrations WHERE id = ?";
    $checkRegStmt = $pdo->prepare($checkRegSql);
    $checkRegStmt->execute([$userIdInput]);
    if ($checkRegStmt->fetch()) {
        $userId = $userIdInput; // It's a registrations.id
        error_log("remove_favorite_v2.php - Using registrations.id: $userId");
    } else {
        // Check if it's a users.user_id
        $checkUserSql = "SELECT user_id, reg_id FROM users WHERE user_id = ?";
        $checkUserStmt = $pdo->prepare($checkUserSql);
        $checkUserStmt->execute([$userIdInput]);
        $userRow = $checkUserStmt->fetch(PDO::FETCH_ASSOC);
        if ($userRow) {
            // Map users.user_id to registrations.id
            $userId = $userRow['reg_id'];
            error_log("remove_favorite_v2.php - Mapped users.user_id $userIdInput to registrations.id: $userId");
        } else {
            echo json_encode(array(
                'success' => false,
                'error' => "User ID $userIdInput not found in registrations or users table."
            ));
            exit();
        }
    }
    
    // Delete favorite (using registrations.id)
    $deleteSql = "DELETE FROM boarder_favorites WHERE user_id = ? AND bh_id = ?";
    $deleteStmt = $pdo->prepare($deleteSql);
    $deleteStmt->execute([$userId, $bhId]);
    
    $rowsAffected = $deleteStmt->rowCount();
    
    if ($rowsAffected > 0) {
        error_log("remove_favorite_v2.php - Successfully removed favorite");
        echo json_encode(array(
            'success' => true,
            'message' => 'Removed from favorites'
        ));
    } else {
        error_log("remove_favorite_v2.php - Favorite not found for user_id: $userId, bh_id: $bhId");
        echo json_encode(array(
            'success' => false,
            'error' => 'Favorite not found'
        ));
    }
    
} catch (PDOException $e) {
    error_log("Database error in remove_favorite_v2.php: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ));
} catch (Exception $e) {
    error_log("Server error in remove_favorite_v2.php: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'error' => 'Server error: ' . $e->getMessage()
    ));
}
?>

