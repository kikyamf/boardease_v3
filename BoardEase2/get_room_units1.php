<?php
// BoardEase2/get_room_units1.php
// Updated to use Live DB credentials and mysqli for production consistency.

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, ngrok-skip-browser-warning, User-Agent, Accept');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    header('Access-Control-Max-Age: 86400');
    http_response_code(200);
    exit();
}

// Database configuration
define('DB_HOST', 'localhost');
define('DB_USER', 'u223444398_userboardease');
define('DB_PASS', '!Boardease2026');
define('DB_NAME', 'u223444398_boardease');

try {
    $conn = new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME);

    if ($conn->connect_error) {
        throw new Exception("Database connection failed: " . $conn->connect_error);
    }
    
    // Get bhr_id from request
    $bhrId = isset($_POST['bhr_id']) ? intval($_POST['bhr_id']) : (isset($_GET['bhr_id']) ? intval($_GET['bhr_id']) : 0);
    
    if ($bhrId === 0) {
        throw new Exception("Room ID (bhr_id) is required.", 400);
    }
    
    // First, get room category and capacity
    $infoSql = "SELECT room_category, capacity FROM boarding_house_rooms WHERE bhr_id = ?";
    $stmt = $conn->prepare($infoSql);
    $stmt->bind_param("i", $bhrId);
    $stmt->execute();
    $roomInfo = $stmt->get_result()->fetch_assoc();
    $stmt->close();
    
    if (!$roomInfo) {
        throw new Exception("Room category not found for the given bhr_id.", 404);
    }
    
    $roomCategory = $roomInfo['room_category'] ?? '';
    $capacity = intval($roomInfo['capacity']);
    $units = array();

    // Fetch units based on category
    if ($roomCategory === 'Private Room') {
        $sql = "
            SELECT ru.room_id, ru.bhr_id, ru.room_number, 'Available' as status
            FROM room_units ru
            WHERE ru.bhr_id = ?
            AND NOT EXISTS (
                SELECT 1 FROM bookings b 
                WHERE b.room_id = ru.room_id 
                AND b.booking_status IN ('Pending', 'Approved', 'Confirmed')
            )
            ORDER BY ru.room_number ASC
        ";
        $stmt = $conn->prepare($sql);
        $stmt->bind_param("i", $bhrId);
        $stmt->execute();
        $res = $stmt->get_result();
        while($row = $res->fetch_assoc()) $units[] = $row;
        $stmt->close();

    } else if ($roomCategory === 'Bed Spacer') {
        $sql = "
            SELECT 
                ru.room_id, ru.bhr_id, ru.room_number, 'Available' as status,
                COALESCE((
                    SELECT COUNT(*) FROM bookings b 
                    WHERE b.room_id = ru.room_id 
                    AND b.booking_status IN ('Pending', 'Approved', 'Confirmed')
                ), 0) as occupied_count
            FROM room_units ru
            WHERE ru.bhr_id = ?
            HAVING occupied_count < ?
            ORDER BY ru.room_number ASC
        ";
        $stmt = $conn->prepare($sql);
        $stmt->bind_param("ii", $bhrId, $capacity);
        $stmt->execute();
        $res = $stmt->get_result();
        while($row = $res->fetch_assoc()) {
            $occupied = intval($row['occupied_count']);
            $row['available_capacity'] = $capacity - $occupied;
            $row['total_capacity'] = $capacity;
            $row['occupied_capacity'] = $occupied;
            unset($row['occupied_count']);
            $units[] = $row;
        }
        $stmt->close();

    } else {
        // Default Logic
        $sql = "
            SELECT ru.room_id, ru.bhr_id, ru.room_number, 'Available' as status
            FROM room_units ru
            WHERE ru.bhr_id = ?
            AND NOT EXISTS (
                SELECT 1 FROM bookings b 
                WHERE b.room_id = ru.room_id 
                AND b.booking_status IN ('Pending', 'Approved', 'Confirmed')
            )
            ORDER BY ru.room_number ASC
        ";
        $stmt = $conn->prepare($sql);
        $stmt->bind_param("i", $bhrId);
        $stmt->execute();
        $res = $stmt->get_result();
        while($row = $res->fetch_assoc()) $units[] = $row;
        $stmt->close();
    }
    
    echo json_encode(array(
        'success' => true,
        'room_category' => $roomCategory,
        'capacity' => $capacity,
        'units' => $units
    ));

    $conn->close();
    
} catch (Exception $e) {
    if (isset($conn) && $conn->connect_errno == 0 && $conn->ping()) {
        $conn->close();
    }
    http_response_code($e->getCode() >= 400 ? $e->getCode() : 500);
    echo json_encode(array('success' => false, 'error' => $e->getMessage()));
}
?>
