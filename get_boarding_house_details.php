<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Database configuration
$host = 'localhost';
$dbname = 'boardease2';
$username = 'boardease';
$password = 'boardease';

try {
    // Create PDO connection
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Get boarding house ID from request
    $bhId = isset($_GET['bh_id']) ? (int)$_GET['bh_id'] : 0;
    
    if ($bhId <= 0) {
        echo json_encode(array(
            'success' => false,
            'error' => 'Invalid boarding house ID'
        ));
        exit;
    }
    
    // SQL query to get complete boarding house details
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
            GROUP_CONCAT(DISTINCT bhi.image_path) AS images,
            MIN(bhr.price) AS lowest_price,
            MAX(bhr.price) AS highest_price,
            COUNT(DISTINCT bhr.bhr_id) AS total_rooms,
            GROUP_CONCAT(
                DISTINCT CONCAT(
                    bhr.room_name, '|', 
                    bhr.price, '|', 
                    bhr.capacity, '|', 
                    bhr.room_category, '|',
                    COALESCE(bhr.room_description, ''), '|',
                    bhr.total_rooms
                )
                SEPARATOR ';'
            ) AS rooms
        FROM boarding_houses AS bh
        LEFT JOIN boarding_house_images AS bhi ON bh.bh_id = bhi.bh_id
        LEFT JOIN boarding_house_rooms AS bhr ON bh.bh_id = bhr.bh_id
        WHERE bh.bh_id = ? AND bh.status = 'active'
        GROUP BY bh.bh_id
    ";
    
    $stmt = $pdo->prepare($sql);
    $stmt->execute([$bhId]);
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$result) {
        echo json_encode(array(
            'success' => false,
            'error' => 'Boarding house not found'
        ));
        exit;
    }
    
    // Process images
    $images = array();
    if (!empty($result['images'])) {
        $imagePaths = explode(',', $result['images']);
        foreach ($imagePaths as $imagePath) {
            if (!empty(trim($imagePath))) {
                $images[] = 'https://hookiest-unprotecting-cher.ngrok-free.dev/BoardEase2/' . trim($imagePath);
            }
        }
    }
    
    // Process rooms
    $rooms = array();
    if (!empty($result['rooms'])) {
        $roomData = explode(';', $result['rooms']);
        foreach ($roomData as $room) {
            if (!empty(trim($room))) {
                $roomParts = explode('|', $room);
                if (count($roomParts) >= 6) {
                    $rooms[] = array(
                        'room_name' => $roomParts[0],
                        'price' => (int)$roomParts[1],
                        'capacity' => (int)$roomParts[2],
                        'room_category' => $roomParts[3],
                        'room_description' => $roomParts[4],
                        'total_rooms' => (int)$roomParts[5]
                    );
                }
            }
        }
    }
    
    // Format the response
    $response = array(
        'success' => true,
        'data' => array(
            'bh_id' => (int)$result['bh_id'],
            'user_id' => (int)$result['user_id'],
            'bh_name' => $result['bh_name'],
            'bh_address' => $result['bh_address'],
            'bh_description' => $result['bh_description'],
            'bh_rules' => $result['bh_rules'],
            'number_of_bathroom' => $result['number_of_bathroom'],
            'area' => $result['area'],
            'build_year' => $result['build_year'],
            'status' => $result['status'],
            'bh_created_at' => $result['bh_created_at'],
            'images' => $images,
            'lowest_price' => $result['lowest_price'] ? (int)$result['lowest_price'] : null,
            'highest_price' => $result['highest_price'] ? (int)$result['highest_price'] : null,
            'total_rooms' => (int)$result['total_rooms'],
            'rooms' => $rooms
        )
    );
    
    echo json_encode($response);
    
} catch (PDOException $e) {
    echo json_encode(array(
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ));
} catch (Exception $e) {
    echo json_encode(array(
        'success' => false,
        'error' => 'Server error: ' . $e->getMessage()
    ));
}
?>
