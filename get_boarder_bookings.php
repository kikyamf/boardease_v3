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
$dbname = 'u223444398_boardease';
$username = 'u223444398_userboardease';
$password = '!Boardease2026';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Get user_id from POST or GET request
    $userIdInput = isset($_POST['user_id']) ? intval($_POST['user_id']) : (isset($_GET['user_id']) ? intval($_GET['user_id']) : 0);
    
    error_log("get_boarder_bookings.php - Received user_id: $userIdInput");
    
    if ($userIdInput === 0) {
        echo json_encode(array(
            'success' => false,
            'error' => 'User ID is required.'
        ));
        exit();
    }
    
    // Map user_id to users.user_id if needed (similar to favorites)
    $userId = null;
    
    // Check if it's a users.user_id
    $checkUserSql = "SELECT user_id FROM users WHERE user_id = ?";
    $checkUserStmt = $pdo->prepare($checkUserSql);
    $checkUserStmt->execute([$userIdInput]);
    $userRow = $checkUserStmt->fetch(PDO::FETCH_ASSOC);
    
    if ($userRow) {
        $userId = $userIdInput; // It's already a users.user_id
        error_log("get_boarder_bookings.php - Using users.user_id: $userId");
    } else {
        // Check if it's a registrations.id and try to map
        $checkRegSql = "SELECT u.user_id FROM registrations r 
                       LEFT JOIN users u ON r.id = u.reg_id 
                       WHERE r.id = ?";
        $checkRegStmt = $pdo->prepare($checkRegSql);
        $checkRegStmt->execute([$userIdInput]);
        $regRow = $checkRegStmt->fetch(PDO::FETCH_ASSOC);
        
        if ($regRow && $regRow['user_id']) {
            $userId = $regRow['user_id'];
            error_log("get_boarder_bookings.php - Mapped registrations.id $userIdInput to users.user_id: $userId");
        } else {
            echo json_encode(array(
                'success' => false,
                'error' => "User ID $userIdInput not found in users table."
            ));
            exit();
        }
    }
    
    // Combined query to get all bookings with section classification
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
            bhr.price,
            bhr.bh_id,
            bh.bh_name,
            bh.bh_address,
            bh.bh_description,
            (SELECT bhi.image_path 
             FROM boarding_house_images AS bhi 
             WHERE bhi.bh_id = bh.bh_id 
             ORDER BY bhi.image_id ASC 
             LIMIT 1) as image_path,
            COALESCE(SUM(p.payment_amount), 0) as total_paid,
            COALESCE(SUM(CASE WHEN p.payment_status = 'Completed' THEN p.payment_amount ELSE 0 END), 0) as confirmed_paid,
            bhr.price - COALESCE(SUM(CASE WHEN p.payment_status = 'Completed' THEN p.payment_amount ELSE 0 END), 0) as balance_due,
            CASE 
                WHEN b.booking_status = 'Active' THEN 'Active'
                WHEN b.booking_status IN ('Confirmed', 'Upcoming') AND CURDATE() >= b.start_date AND CURDATE() <= b.end_date THEN 'Check In'
                WHEN b.booking_status IN ('Confirmed', 'Upcoming') AND CURDATE() < b.start_date THEN 'Upcoming'
                ELSE b.booking_status
            END as display_status,
            CASE 
                WHEN b.booking_status IN ('Confirmed', 'Upcoming', 'Active') AND CURDATE() <= b.end_date THEN 'current'
                WHEN b.booking_status IN ('Pending', 'Approved') THEN 'pending'
                WHEN b.booking_status IN ('Completed', 'Cancelled', 'Expired', 'Declined') OR (b.booking_status IN ('Confirmed', 'Upcoming', 'Active') AND CURDATE() > b.end_date) THEN 'history'
                ELSE 'history'
            END as section,
            IF((SELECT COUNT(*) FROM reviews WHERE user_id = b.user_id AND bh_id = bhr.bh_id) > 0, 1, 0) as is_reviewed
        FROM bookings b
        INNER JOIN room_units ru ON b.room_id = ru.room_id
        INNER JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
        INNER JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
        LEFT JOIN payments p ON b.booking_id = p.booking_id
        WHERE b.user_id = ? 
            AND b.booking_status IN ('Pending', 'Confirmed', 'Completed', 'Cancelled', 'Expired', 'Declined', 'Approved', 'Upcoming', 'Active')
        GROUP BY b.booking_id, b.room_id, b.user_id, b.start_date, b.end_date, 
                 b.booking_status, b.booking_date, ru.room_number, bhr.room_category, 
                 bhr.price, bhr.bh_id, bh.bh_name, bh.bh_address, bh.bh_description
        ORDER BY 
            CASE 
                WHEN b.booking_status = 'Active' OR (b.booking_status = 'Confirmed' AND CURDATE() >= b.start_date AND CURDATE() <= b.end_date) THEN 1
                WHEN b.booking_status = 'Upcoming' OR (b.booking_status = 'Confirmed' AND CURDATE() < b.start_date) THEN 2
                WHEN b.booking_status IN ('Pending', 'Approved') THEN 3
                WHEN b.booking_status IN ('Completed', 'Cancelled', 'Expired', 'Declined') OR (b.booking_status IN ('Confirmed', 'Upcoming', 'Active') AND CURDATE() > b.end_date) THEN 4
                ELSE 5
            END,
            b.booking_date DESC
    ";
    
    $stmt = $pdo->prepare($sql);
    $stmt->execute([$userId]);
    $results = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    error_log("get_boarder_bookings.php - Found " . count($results) . " bookings for user_id: $userId");
    
    // Get base URL for images
    $baseUrl = 'http://192.168.1.4/boardease_v3/';
    
    // Separate bookings into sections
    $currentBookings = array();
    $pendingBookings = array();
    $historyBookings = array();
    
    foreach ($results as $row) {
        $booking = array(
            'booking_id' => (int)$row['booking_id'],
            'room_id' => (int)$row['room_id'],
            'user_id' => (int)$row['user_id'],
            'start_date' => $row['start_date'],
            'end_date' => $row['end_date'],
            'booking_status' => $row['booking_status'],
            'booking_date' => $row['booking_date'],
            'room_number' => $row['room_number'],
            'room_category' => $row['room_category'],
            'price' => floatval($row['price']),
            'bh_id' => (int)$row['bh_id'],
            'bh_name' => $row['bh_name'],
            'bh_address' => $row['bh_address'],
            'bh_description' => $row['bh_description'],
            'image_path' => $row['image_path'] ? $baseUrl . $row['image_path'] : null,
            'total_paid' => floatval($row['total_paid']),
            'confirmed_paid' => floatval($row['confirmed_paid']),
            'balance_due' => floatval($row['balance_due']),
            'section' => $row['section'],
            'display_status' => $row['display_status'],
            'is_reviewed' => (int)$row['is_reviewed'] === 1
        );
        
        // Categorize by section
        switch ($row['section']) {
            case 'current':
                $currentBookings[] = $booking;
                break;
            case 'pending':
                $pendingBookings[] = $booking;
                break;
            case 'history':
                // Include both Completed and Cancelled bookings in history
                $historyBookings[] = $booking;
                break;
        }
    }
    
    echo json_encode(array(
        'success' => true,
        'data' => array(
            'current' => $currentBookings,
            'pending' => $pendingBookings,
            'history' => $historyBookings
        ),
        'counts' => array(
            'current' => count($currentBookings),
            'pending' => count($pendingBookings),
            'history' => count($historyBookings)
        )
    ));
    
} catch (PDOException $e) {
    error_log("Database error in get_boarder_bookings.php: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'error' => 'Database error: ' . $e->getMessage()
    ));
} catch (Exception $e) {
    error_log("Server error in get_boarder_bookings.php: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'error' => 'Server error: ' . $e->getMessage()
    ));
}
?>

