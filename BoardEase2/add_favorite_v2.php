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
    
    error_log("add_favorite_v2.php - Received user_id: $userIdInput, bh_id: $bhId");
    
    // Validate required fields
    if ($userIdInput === 0 || $bhId === 0) {
        echo json_encode(array(
            'success' => false,
            'error' => 'User ID and Boarding House ID are required.'
        ));
        exit();
    }
    
    // Determine which user_id to use
    // Strategy: Try to find registrations.id first (most common), then try users.user_id
    $userId = null;
    
    // Check if it's a registrations.id
    $checkRegSql = "SELECT id FROM registrations WHERE id = ?";
    $checkRegStmt = $pdo->prepare($checkRegSql);
    $checkRegStmt->execute([$userIdInput]);
    if ($checkRegStmt->fetch()) {
        $userId = $userIdInput; // It's a registrations.id
        error_log("add_favorite_v2.php - Using registrations.id: $userId");
    } else {
        // Check if it's a users.user_id
        $checkUserSql = "SELECT user_id, reg_id FROM users WHERE user_id = ?";
        $checkUserStmt = $pdo->prepare($checkUserSql);
        $checkUserStmt->execute([$userIdInput]);
        $userRow = $checkUserStmt->fetch(PDO::FETCH_ASSOC);
        if ($userRow) {
            // Map users.user_id to registrations.id
            $userId = $userRow['reg_id'];
            error_log("add_favorite_v2.php - Mapped users.user_id $userIdInput to registrations.id: $userId");
        } else {
            echo json_encode(array(
                'success' => false,
                'error' => "User ID $userIdInput not found in registrations or users table."
            ));
            exit();
        }
    }
    
    // Validate boarding house exists and is active
    $checkBhSql = "SELECT bh_id, bh_name, status FROM boarding_houses WHERE bh_id = ?";
    $checkBhStmt = $pdo->prepare($checkBhSql);
    $checkBhStmt->execute([$bhId]);
    $boardingHouse = $checkBhStmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$boardingHouse) {
        echo json_encode(array(
            'success' => false,
            'error' => "Boarding house with ID $bhId does not exist."
        ));
        exit();
    }
    
    if ($boardingHouse['status'] !== 'Active') {
        echo json_encode(array(
            'success' => false,
            'error' => "Boarding house '{$boardingHouse['bh_name']}' is not active."
        ));
        exit();
    }
    
    // Check if favorite already exists (using registrations.id)
    $checkSql = "SELECT fav_id FROM boarder_favorites WHERE user_id = ? AND bh_id = ?";
    $checkStmt = $pdo->prepare($checkSql);
    $checkStmt->execute([$userId, $bhId]);
    $existing = $checkStmt->fetch(PDO::FETCH_ASSOC);
    
    if ($existing) {
        echo json_encode(array(
            'success' => true,
            'message' => 'Already in favorites',
            'fav_id' => $existing['fav_id']
        ));
        exit();
    }
    
    // Insert new favorite (using registrations.id)
    $insertSql = "INSERT INTO boarder_favorites (user_id, bh_id) VALUES (?, ?)";
    $insertStmt = $pdo->prepare($insertSql);
    $insertStmt->execute([$userId, $bhId]);
    
    $favId = $pdo->lastInsertId();
    
    error_log("add_favorite_v2.php - Successfully added favorite with fav_id: $favId");
    
    echo json_encode(array(
        'success' => true,
        'message' => 'Added to favorites',
        'fav_id' => $favId
    ));
    
} catch (PDOException $e) {
    error_log("Database error in add_favorite_v2.php: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ));
} catch (Exception $e) {
    error_log("Server error in add_favorite_v2.php: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'error' => 'Server error: ' . $e->getMessage()
    ));
}
?>

