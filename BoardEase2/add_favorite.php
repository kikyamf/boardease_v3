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
    
    // Log the request
    error_log("add_favorite.php - Received user_id: $userIdInput, bh_id: $bhId");
    
    // Validate required fields
    if ($userIdInput === 0 || $bhId === 0) {
        echo json_encode(array(
            'success' => false,
            'error' => 'User ID and Boarding House ID are required.',
            'debug' => array('user_id' => $userIdInput, 'bh_id' => $bhId)
        ));
        exit();
    }
    
    // Map user_id: If it's from registrations table, find corresponding users.user_id
    // First, check if it's already a users.user_id
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
            error_log("add_favorite.php - Mapped registrations.id $userIdInput to users.user_id $userId");
        } else {
            // If user doesn't exist in users table, we need to create one or use registrations.id
            // For now, let's try using registrations.id directly (if table allows it)
            // But first, let's check the table structure to see if it accepts registrations.id
            error_log("add_favorite.php - WARNING: User ID $userIdInput not found in users table");
            // Try to use it anyway - if foreign key constraint fails, we'll catch it
        }
    }
    
    error_log("add_favorite.php - Using user_id: $userId for favorite insertion");
    
    // Check if favorite already exists
    $checkSql = "SELECT fav_id FROM boarder_favorites WHERE user_id = ? AND bh_id = ?";
    $checkStmt = $pdo->prepare($checkSql);
    $checkStmt->execute([$userId, $bhId]);
    $existing = $checkStmt->fetch(PDO::FETCH_ASSOC);
    
    if ($existing) {
        // Already favorited
        error_log("add_favorite.php - Favorite already exists for user_id: $userId, bh_id: $bhId");
        echo json_encode(array(
            'success' => true,
            'message' => 'Already in favorites',
            'fav_id' => $existing['fav_id']
        ));
        exit();
    }
    
    // Insert new favorite
    try {
        $insertSql = "INSERT INTO boarder_favorites (user_id, bh_id) VALUES (?, ?)";
        $insertStmt = $pdo->prepare($insertSql);
        $insertStmt->execute([$userId, $bhId]);
        
        $favId = $pdo->lastInsertId();
        
        error_log("add_favorite.php - Successfully added favorite with fav_id: $favId");
        
        echo json_encode(array(
            'success' => true,
            'message' => 'Added to favorites',
            'fav_id' => $favId,
            'debug' => array('user_id_used' => $userId, 'original_user_id' => $userIdInput)
        ));
    } catch (PDOException $insertError) {
        // If foreign key constraint fails, try using registrations.id directly
        error_log("add_favorite.php - Foreign key error: " . $insertError->getMessage());
        
        // Check if table structure allows registrations.id
        // Try with original userId (might be registrations.id)
        if ($userId !== $userIdInput) {
            try {
                $insertSql = "INSERT INTO boarder_favorites (user_id, bh_id) VALUES (?, ?)";
                $insertStmt = $pdo->prepare($insertSql);
                $insertStmt->execute([$userIdInput, $bhId]);
                
                $favId = $pdo->lastInsertId();
                error_log("add_favorite.php - Successfully added favorite using registrations.id: $userIdInput");
                
                echo json_encode(array(
                    'success' => true,
                    'message' => 'Added to favorites',
                    'fav_id' => $favId
                ));
            } catch (PDOException $retryError) {
                throw $insertError; // Throw original error
            }
        } else {
            throw $insertError;
        }
    }
    
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

