<?php
// BoardEase2/get_boarder_bookings.php
// Updated to use Live DB credentials and mysqli for consistency.

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, ngrok-skip-browser-warning, User-Agent, Accept');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

// Database configuration
define('DB_HOST', 'localhost');
define('DB_USER', 'u223444398_userboardease');
define('DB_PASS', '!Boardease2026');
define('DB_NAME', 'u223444398_boardease');

try {
    $conn = new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME);

    if ($conn->connect_error) {
        echo json_encode(array('success' => false, 'error' => 'Database connection failed: ' . $conn->connect_error));
        exit;
    }
    
    // Get user_id from POST or GET
    $userIdInput = isset($_POST['user_id']) ? intval($_POST['user_id']) : (isset($_GET['user_id']) ? intval($_GET['user_id']) : 0);
    
    if ($userIdInput === 0) {
        echo json_encode(array('success' => false, 'error' => 'User ID is required.'));
        exit;
    }
    
    // Main query to fetch bookings with room and house details
    $sql = "
        SELECT 
            b.booking_id,
            b.room_id,
            b.user_id,
            b.start_date,
            b.end_date,
            b.booking_status,
            b.booking_date,
            ru.room_number,
            bhr.room_category,
            bhr.room_name as bhr_room_name,
            bhr.price,
            bhr.bh_id,
            bh.bh_name,
            bh.bh_address,
            bh.bh_description,
            -- Calculate balance due (Price - Total Completed Payments)
            bhr.price - COALESCE((
                SELECT SUM(p.payment_amount) 
                FROM payments p 
                WHERE p.booking_id = b.booking_id 
                AND p.payment_status = 'Completed'
            ), 0) as balance_due,
            CASE 
                WHEN b.booking_status = 'Confirmed' AND CURDATE() >= b.start_date AND CURDATE() <= b.end_date THEN 'current'
                WHEN b.booking_status IN ('Pending', 'Approved') THEN 'pending'
                WHEN b.booking_status IN ('Completed', 'Cancelled', 'Expired') THEN 'history'
                ELSE 'other'
            END as section
        FROM bookings b
        INNER JOIN room_units ru ON b.room_id = ru.room_id
        INNER JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
        INNER JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
        WHERE b.user_id = ? 
            AND b.booking_status IN ('Pending', 'Approved', 'Confirmed', 'Completed', 'Cancelled', 'Expired')
        ORDER BY 
            CASE 
                WHEN b.booking_status = 'Confirmed' AND CURDATE() >= b.start_date AND CURDATE() <= b.end_date THEN 1
                WHEN b.booking_status IN ('Pending', 'Approved') THEN 2
                WHEN b.booking_status IN ('Completed', 'Cancelled', 'Expired') THEN 3
                ELSE 4
            END,
            b.booking_date DESC
    ";
    
    $stmt = $conn->prepare($sql);
    if (!$stmt) {
        throw new Exception("Prepare failed: " . $conn->error);
    }
    
    $stmt->bind_param("i", $userIdInput);
    $stmt->execute();
    $result = $stmt->get_result();
    
    $currentBookings = array();
    $pendingBookings = array();
    $historyBookings = array();
    $otherBookings = array();
    
    $image_base_url = "http://192.168.1.4/boardease_v3/"; // Keep original image base
    
    while ($booking = $result->fetch_assoc()) {
        // Fetch first image for the boarding house
        $bh_id = $booking['bh_id'];
        $imgSql = "SELECT image_path FROM boarding_house_images WHERE bh_id = ? LIMIT 1";
        $imgStmt = $conn->prepare($imgSql);
        $imgStmt->bind_param("i", $bh_id);
        $imgStmt->execute();
        $imgRes = $imgStmt->get_result();
        $imgRow = $imgRes->fetch_assoc();
        
        $booking['bh_image'] = $imgRow ? $image_base_url . $imgRow['image_path'] : null;
        $imgStmt->close();
        
        // Final categorization
        switch ($booking['section']) {
            case 'current':
                $currentBookings[] = $booking;
                break;
            case 'pending':
                $pendingBookings[] = $booking;
                break;
            case 'history':
                $historyBookings[] = $booking;
                break;
            default:
                $otherBookings[] = $booking;
                break;
        }
    }
    
    echo json_encode(array(
        'success' => true,
        'data' => array(
            'current' => $currentBookings,
            'pending' => $pendingBookings,
            'history' => $historyBookings,
            'other' => $otherBookings
        ),
        'counts' => array(
            'current' => count($currentBookings),
            'pending' => count($pendingBookings),
            'history' => count($historyBookings),
            'other' => count($otherBookings)
        )
    ));
    
    $stmt->close();
    $conn->close();
    
} catch (Exception $e) {
    echo json_encode(array('success' => false, 'error' => $e->getMessage()));
}
?>
