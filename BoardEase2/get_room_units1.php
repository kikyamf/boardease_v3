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
    
    // Get bhr_id from request
    $bhrId = isset($_POST['bhr_id']) ? intval($_POST['bhr_id']) : (isset($_GET['bhr_id']) ? intval($_GET['bhr_id']) : 0);
    
    if ($bhrId === 0) {
        echo json_encode(array(
            'success' => false,
            'error' => 'Room ID (bhr_id) is required.'
        ));
        exit();
    }
    
    // First, get room category and capacity from boarding_house_rooms
    $roomInfoSql = "SELECT room_category, capacity FROM boarding_house_rooms WHERE bhr_id = ?";
    $roomInfoStmt = $pdo->prepare($roomInfoSql);
    $roomInfoStmt->execute([$bhrId]);
    $roomInfo = $roomInfoStmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$roomInfo) {
        echo json_encode(array(
            'success' => false,
            'error' => 'Room category not found for the given bhr_id.'
        ));
        exit();
    }
    
    $roomCategory = $roomInfo['room_category'];
    $capacity = intval($roomInfo['capacity']);
    
    // Fetch available room units for this bhr_id with different logic based on room category
    if ($roomCategory === 'Private Room') {
        // Private Room: Exclude if there's ANY active booking (Pending or Confirmed)
        $sql = "
            SELECT 
                ru.room_id,
                ru.bhr_id,
                ru.room_number,
                ru.status
            FROM room_units ru
            LEFT JOIN bookings b ON ru.room_id = b.room_id 
                AND b.booking_status IN ('Pending', 'Confirmed')
            WHERE ru.bhr_id = ?
                AND b.room_id IS NULL
            ORDER BY ru.room_number ASC
        ";
        
        $stmt = $pdo->prepare($sql);
        $stmt->execute([$bhrId]);
        $units = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
    } else if ($roomCategory === 'Bed Spacer') {
        // Bed Spacer: Count active bookings per room unit
        // Include room units where active_bookings < capacity
        $sql = "
            SELECT 
                ru.room_id,
                ru.bhr_id,
                ru.room_number,
                ru.status,
                COALESCE(COUNT(b.booking_id), 0) as active_bookings
            FROM room_units ru
            LEFT JOIN bookings b ON ru.room_id = b.room_id 
                AND b.booking_status IN ('Pending', 'Confirmed')
            WHERE ru.bhr_id = ?
            GROUP BY ru.room_id, ru.bhr_id, ru.room_number, ru.status
            HAVING active_bookings < ?
            ORDER BY ru.room_number ASC
        ";
        
        $stmt = $pdo->prepare($sql);
        $stmt->execute([$bhrId, $capacity]);
        $units = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        // Calculate and add capacity information for each unit
        foreach ($units as &$unit) {
            $activeBookings = intval($unit['active_bookings']);
            $unit['available_capacity'] = $capacity - $activeBookings;
            $unit['total_capacity'] = $capacity;
            $unit['occupied_capacity'] = $activeBookings;
            // Remove active_bookings from response (it was only for filtering and calculation)
            unset($unit['active_bookings']);
        }
        unset($unit); // Break reference
        
    } else {
        // Unknown room category - use default logic (exclude if any active booking)
        $sql = "
            SELECT 
                ru.room_id,
                ru.bhr_id,
                ru.room_number,
                ru.status
            FROM room_units ru
            LEFT JOIN bookings b ON ru.room_id = b.room_id 
                AND b.booking_status IN ('Pending', 'Confirmed')
            WHERE ru.bhr_id = ?
                AND b.room_id IS NULL
            ORDER BY ru.room_number ASC
        ";
        
        $stmt = $pdo->prepare($sql);
        $stmt->execute([$bhrId]);
        $units = $stmt->fetchAll(PDO::FETCH_ASSOC);
    }
    
    // Set status to 'Available' for all returned units (since we're filtering by availability)
    foreach ($units as &$unit) {
        $unit['status'] = 'Available';
    }
    unset($unit); // Break reference
    
    // Format the response
    $response = array(
        'success' => true,
        'room_category' => $roomCategory,
        'capacity' => $capacity,
        'units' => $units
    );
    
    echo json_encode($response);
    
} catch (PDOException $e) {
    error_log("Database error in get_room_units1.php: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ));
} catch (Exception $e) {
    error_log("Server error in get_room_units1.php: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'error' => 'Server error: ' . $e->getMessage()
    ));
}
?>

