<?php
// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
    header('Access-Control-Allow-Headers: Content-Type, ngrok-skip-browser-warning');
    header('Access-Control-Max-Age: 86400');
    http_response_code(200);
    exit;
}

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, ngrok-skip-browser-warning');

// Database configuration
$host = 'localhost';
$dbname = 'boardease2';
$username = 'boardease';
$password = 'boardease';

try {
    // Create PDO connection
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // SQL query to get boarding houses with their main image and room prices
    // Using a subquery to get the first image for each boarding house
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
            GROUP_CONCAT(DISTINCT bhr.room_category) as room_categories
        FROM boarding_houses AS bh
        LEFT JOIN boarding_house_rooms AS bhr ON bh.bh_id = bhr.bh_id
        WHERE bh.status = 'Active'
        GROUP BY bh.bh_id
        ORDER BY bh.bh_created_at DESC
    ";
    
    $stmt = $pdo->prepare($sql);
    $stmt->execute();
    $results = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Log for debugging
    error_log("Found " . count($results) . " active boarding houses");
    if (count($results) == 0) {
        // Check if there are any boarding houses at all
        $checkSql = "SELECT COUNT(*) as total, COUNT(CASE WHEN status = 'Active' THEN 1 END) as active_count FROM boarding_houses";
        $checkStmt = $pdo->query($checkSql);
        $checkResult = $checkStmt->fetch(PDO::FETCH_ASSOC);
        error_log("Total boarding houses: " . $checkResult['total'] . ", Active: " . $checkResult['active_count']);
    }
    
    // Format the response
    $response = array();
    foreach ($results as $row) {
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
            'image_path' => $row['image_path'] ? 'https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/' . $row['image_path'] : null,
            'min_price' => $row['min_price'] ? (int)$row['min_price'] : null,
            'max_price' => $row['max_price'] ? (int)$row['max_price'] : null,
            'total_rooms' => (int)$row['total_rooms'],
            'room_categories' => $row['room_categories'] ? explode(',', $row['room_categories']) : []
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
    // Handle database errors
    echo json_encode(array(
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ));
} catch (Exception $e) {
    // Handle other errors
    echo json_encode(array(
        'success' => false,
        'error' => 'Server error: ' . $e->getMessage()
    ));
}
?>
