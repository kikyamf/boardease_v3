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
    $userIdInput = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
    $bhId = isset($_POST['bh_id']) ? intval($_POST['bh_id']) : 0;
    
    error_log("remove_favorite.php - Received user_id: $userIdInput, bh_id: $bhId");
    
    // Validate required fields
    if ($userIdInput === 0 || $bhId === 0) {
        echo json_encode(array(
            'success' => false,
            'error' => 'User ID and Boarding House ID are required.'
        ));
        exit();
    }
    
    // Map user_id: Try to find users.user_id first
    $userId = $userIdInput;
    $checkUserSql = "SELECT user_id FROM users WHERE user_id = ?";
    $checkUserStmt = $pdo->prepare($checkUserSql);
    $checkUserStmt->execute([$userId]);
    $userExists = $checkUserStmt->fetch(PDO::FETCH_ASSOC);
    
    // If not found in users table, check if it's a registrations.id
    if (!$userExists) {
        $checkRegSql = "SELECT u.user_id FROM users u 
                       INNER JOIN registrations r ON u.reg_id = r.id 
                       WHERE r.id = ?";
        $checkRegStmt = $pdo->prepare($checkRegSql);
        $checkRegStmt->execute([$userIdInput]);
        $mappedUser = $checkRegStmt->fetch(PDO::FETCH_ASSOC);
        
        if ($mappedUser) {
            $userId = $mappedUser['user_id'];
            error_log("remove_favorite.php - Mapped registrations.id $userIdInput to users.user_id $userId");
        }
    }
    
    error_log("remove_favorite.php - Using user_id: $userId for deletion");
    
    // Delete favorite - try with mapped user_id first
    $deleteSql = "DELETE FROM boarder_favorites WHERE user_id = ? AND bh_id = ?";
    $deleteStmt = $pdo->prepare($deleteSql);
    $deleteStmt->execute([$userId, $bhId]);
    
    $rowsAffected = $deleteStmt->rowCount();
    
    // If no rows affected and we mapped the user_id, try with original ID
    if ($rowsAffected === 0 && $userId !== $userIdInput) {
        error_log("remove_favorite.php - No rows deleted with mapped user_id, trying with original: $userIdInput");
        $deleteStmt->execute([$userIdInput, $bhId]);
        $rowsAffected = $deleteStmt->rowCount();
    }
    
    if ($rowsAffected > 0) {
        error_log("remove_favorite.php - Successfully removed favorite");
        echo json_encode(array(
            'success' => true,
            'message' => 'Removed from favorites'
        ));
    } else {
        error_log("remove_favorite.php - Favorite not found for user_id: $userId (original: $userIdInput), bh_id: $bhId");
        echo json_encode(array(
            'success' => false,
            'error' => 'Favorite not found',
            'debug' => array('user_id_tried' => $userId, 'original_user_id' => $userIdInput)
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

