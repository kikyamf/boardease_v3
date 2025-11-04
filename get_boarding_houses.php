<?php
// Enable error logging
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Handle OPTIONS request for CORS
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Database configuration
$host = 'localhost';
$dbname = 'boardease2';
$username = 'boardease';
$password = 'boardease';

try {
    // Create PDO connection with timeout
    $dsn = "mysql:host=$host;dbname=$dbname;charset=utf8";
    $options = array(
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_TIMEOUT => 5, // 5 second timeout
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
    );
    
    $pdo = new PDO($dsn, $username, $password, $options);
    
    // SQL query to get boarding houses with their main image and room prices
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
            bhi.image_path,
            MIN(bhr.price) as min_price,
            MAX(bhr.price) as max_price,
            COUNT(bhr.bhr_id) as total_rooms,
            GROUP_CONCAT(DISTINCT bhr.room_category) as room_categories
        FROM boarding_houses AS bh
        LEFT JOIN boarding_house_images AS bhi ON bh.bh_id = bhi.bh_id
        LEFT JOIN boarding_house_rooms AS bhr ON bh.bh_id = bhr.bh_id
        WHERE bh.status = 'active'
        GROUP BY bh.bh_id
        ORDER BY bh.bh_created_at DESC
    ";
    
    $stmt = $pdo->prepare($sql);
    $stmt->execute();
    $results = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
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
    // Handle database connection errors
    error_log("Database connection error: " . $e->getMessage());
    
    $errorCode = $e->getCode();
    $errorMessage = $e->getMessage();
    
    // Provide more specific error messages
    if ($errorCode == 2002) {
        $errorMessage = "Cannot connect to MySQL server. Please ensure XAMPP MySQL is running.";
    } elseif ($errorCode == 1045) {
        $errorMessage = "Database authentication failed. Check username/password.";
    } elseif ($errorCode == 1049) {
        $errorMessage = "Database 'boardease2' does not exist.";
    }
    
    http_response_code(500);
    echo json_encode(array(
        'success' => false,
        'error' => 'Database error: ' . $errorMessage,
        'error_code' => $errorCode
    ));
} catch (Exception $e) {
    // Handle other errors
    error_log("Server error: " . $e->getMessage());
    http_response_code(500);
    echo json_encode(array(
        'success' => false,
        'error' => 'Server error: ' . $e->getMessage()
    ));
}
?>
