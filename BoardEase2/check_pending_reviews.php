<?php
// BoardEase2/check_pending_reviews.php
// Checks if a user has any completed bookings for which they haven't submitted a review.

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, User-Agent, Accept');

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
        throw new Exception("Database connection failed: " . $conn->connect_error);
    }

    $userId = isset($_GET['user_id']) ? intval($_GET['user_id']) : (isset($_POST['user_id']) ? intval($_POST['user_id']) : 0);

    if ($userId === 0) {
        throw new Exception("User ID is required", 400);
    }

    // Query to find completed bookings for which no review exists for that BH by this user
    // We prioritize the most recent completed booking
    $sql = "
        SELECT 
            b.booking_id,
            b.bh_id,
            b.room_id,
            b.start_date,
            b.end_date,
            b.booking_status,
            bh.bh_name as boarding_house_name,
            bh.bh_address as location,
            bhr.room_category,
            ru.room_number,
            bhr.price as monthly_due,
            (SELECT image_path FROM boarding_house_images WHERE bh_id = b.bh_id LIMIT 1) as image_path
        FROM bookings b
        JOIN boarding_houses bh ON b.bh_id = bh.bh_id
        JOIN room_units ru ON b.room_id = ru.room_id
        JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
        WHERE b.user_id = ? 
        AND b.booking_status = 'Completed'
        AND NOT EXISTS (
            SELECT 1 FROM reviews r 
            WHERE r.user_id = b.user_id 
            AND r.bh_id = b.bh_id
        )
        ORDER BY b.end_date DESC
        LIMIT 1
    ";

    $stmt = $conn->prepare($sql);
    if (!$stmt) {
        throw new Exception("Prepare failed: " . $conn->error);
    }

    $stmt->bind_param("i", $userId);
    $stmt->execute();
    $result = $stmt->get_result();

    if ($booking = $result->fetch_assoc()) {
        // Format image path if exists
        if ($booking['image_path']) {
            $booking['image_path'] = "http://192.168.1.4/boardease_v3/" . $booking['image_path'];
        }
        
        echo json_encode([
            'success' => true,
            'has_pending_review' => true,
            'booking' => $booking
        ]);
    } else {
        echo json_encode([
            'success' => true,
            'has_pending_review' => false
        ]);
    }

    $stmt->close();
    $conn->close();

} catch (Exception $e) {
    http_response_code($e->getCode() >= 400 ? $e->getCode() : 500);
    echo json_encode([
        'success' => false, 
        'error' => $e->getMessage()
    ]);
}
?>
