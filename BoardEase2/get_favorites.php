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
    
    // Get user_id from POST or GET request (GET for testing in browser)
    $userIdInput = isset($_POST['user_id']) ? intval($_POST['user_id']) : (isset($_GET['user_id']) ? intval($_GET['user_id']) : 0);
    
    error_log("get_favorites.php - Received user_id: $userIdInput");
    
    if ($userIdInput === 0) {
        echo json_encode(array(
            'success' => false,
            'error' => 'User ID is required.'
        ));
        exit();
    }
    
    // Map user_id: Try to find users.user_id first, then check if it's registrations.id
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
            error_log("get_favorites.php - Mapped registrations.id $userIdInput to users.user_id $userId");
        } else {
            error_log("get_favorites.php - WARNING: User ID $userIdInput not found in users table, trying with original ID");
            // Will try with original ID - might work if table uses registrations.id
        }
    }
    
    error_log("get_favorites.php - Using user_id: $userId for query");
    
    // SQL query to get user's favorite boarding houses
    // Try with users.user_id first, if no results, try with registrations.id
    $sql = "
        SELECT 
            bh.bh_id,
            bh.user_id,
            bh.bh_name,
            bh.bh_address,
            bh.bh_description,
            bh.bh_rules,
            bh.number_of_bathroom,
            bh.area,
            bh.build_year,
            bh.status,
            bh.bh_created_at,
            (SELECT bhi.image_path 
             FROM boarding_house_images AS bhi 
             WHERE bhi.bh_id = bh.bh_id 
             ORDER BY bhi.image_id ASC 
             LIMIT 1) as image_path,
            MIN(bhr.price) as min_price,
            MAX(bhr.price) as max_price,
            COUNT(DISTINCT bhr.bhr_id) as total_rooms,
            GROUP_CONCAT(DISTINCT bhr.room_category) as room_categories,
            r.first_name,
            r.middle_name,
            r.last_name,
            r.phone,
            r.email,
            bf.fav_id
        FROM boarder_favorites AS bf
        INNER JOIN boarding_houses AS bh ON bf.bh_id = bh.bh_id
        LEFT JOIN boarding_house_rooms AS bhr ON bh.bh_id = bhr.bh_id
        LEFT JOIN registrations AS r ON bh.user_id = r.id
        WHERE bf.user_id = ? AND bh.status = 'Active'
        GROUP BY bh.bh_id, bf.fav_id
        ORDER BY bf.fav_id DESC
    ";
    
    $stmt = $pdo->prepare($sql);
    $stmt->execute([$userId]);
    $results = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // If no results and we mapped the user_id, try with original ID
    if (empty($results) && $userId !== $userIdInput) {
        error_log("get_favorites.php - No results with mapped user_id, trying with original: $userIdInput");
        $stmt->execute([$userIdInput]);
        $results = $stmt->fetchAll(PDO::FETCH_ASSOC);
        if (!empty($results)) {
            error_log("get_favorites.php - Found results using original user_id: $userIdInput");
        }
    }
    
    error_log("get_favorites.php - Found " . count($results) . " favorites");
    
    // Get base URL for images (use local IP for local development)
    $baseUrl = 'http://192.168.1.4/boardease_v3/';
    
    // Format the response
    $response = array();
    foreach ($results as $row) {
        // Build owner full name
        $ownerName = trim(($row['first_name'] ?? '') . ' ' . ($row['middle_name'] ?? '') . ' ' . ($row['last_name'] ?? ''));
        $ownerName = preg_replace('/\s+/', ' ', $ownerName); // Remove extra spaces
        
        $boardingHouse = array(
            'bh_id' => (int)$row['bh_id'],
            'user_id' => (int)$row['user_id'],
            'bh_name' => $row['bh_name'],
            'bh_address' => $row['bh_address'],
            'bh_description' => $row['bh_description'],
            'bh_rules' => $row['bh_rules'],
            'number_of_bathroom' => $row['number_of_bathroom'],
            'area' => $row['area'],
            'build_year' => $row['build_year'],
            'status' => $row['status'],
            'bh_created_at' => $row['bh_created_at'],
            'image_path' => $row['image_path'] ? $baseUrl . $row['image_path'] : null,
            'min_price' => $row['min_price'] ? (int)$row['min_price'] : null,
            'max_price' => $row['max_price'] ? (int)$row['max_price'] : null,
            'total_rooms' => (int)$row['total_rooms'],
            'room_categories' => $row['room_categories'] ? explode(',', $row['room_categories']) : [],
            // Owner contact information
            'owner_name' => $ownerName ?: null,
            'owner_first_name' => $row['first_name'] ?? null,
            'owner_middle_name' => $row['middle_name'] ?? null,
            'owner_last_name' => $row['last_name'] ?? null,
            'owner_phone' => $row['phone'] ?? null,
            'owner_email' => $row['email'] ?? null,
            'fav_id' => (int)$row['fav_id']
        );
        $response[] = $boardingHouse;
    }
    
    // Return JSON response
    echo json_encode(array(
        'success' => true,
        'data' => $response,
        'count' => count($response)
    ));
    
} catch (PDOException $e) {
    error_log("Database error in get_favorites.php: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ));
} catch (Exception $e) {
    error_log("Server error in get_favorites.php: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'error' => 'Server error: ' . $e->getMessage()
    ));
}
?>

