<?php
// BoardEase2/decline_booking.php
// Script for BH Owners to decline a booking request on Live DB using mysqli

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
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
        echo json_encode(array('success' => false, 'error' => 'Database connection failed: ' . $conn->connect_error));
        exit;
    }
    
    // Get booking_id from request
    $input = json_decode(file_get_contents('php://input'), true);
    $bookingId = isset($input['booking_id']) ? intval($input['booking_id']) : (isset($_POST['booking_id']) ? intval($_POST['booking_id']) : 0);
    
    if ($bookingId <= 0) {
        echo json_encode(['success' => false, 'message' => 'Invalid booking ID']);
        exit;
    }
    
    // Update booking status to 'Cancelled'
    $sql = "UPDATE bookings 
            SET booking_status = 'Cancelled' 
            WHERE booking_id = ? AND booking_status = 'Pending'";
            
    $stmt = $conn->prepare($sql);
    if (!$stmt) {
        throw new Exception("Prepare failed: " . $conn->error);
    }
    
    $stmt->bind_param("i", $bookingId);
    $stmt->execute();
    
    if ($stmt->affected_rows > 0) {
        echo json_encode([
            'success' => true, 
            'message' => 'Booking declined successfully.',
            'booking_id' => $bookingId
        ]);
        error_log("decline_booking.php - Booking ID $bookingId declined on live DB.");
    } else {
        echo json_encode([
            'success' => false, 
            'message' => 'Booking not found or already processed'
        ]);
    }
    
    $stmt->close();
    $conn->close();
    
} catch (Exception $e) {
    error_log("Error in decline_booking.php: " . $e->getMessage());
    echo json_encode(['success' => false, 'message' => 'Error: ' . $e->getMessage()]);
}
?>
