<?php
// BoardEase2/check_expirations.php
// Central logic for expiring bookings on Live DB using mysqli

header('Content-Type: application/json');

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
    
    // Start transaction
    $conn->begin_transaction();
    
    $expiredCount = 0;
    
    // 1. Expire PENDING bookings
    // Rules: 3 days after application date OR on the day of the start date
    $sqlPending = "
        UPDATE bookings 
        SET booking_status = 'Expired' 
        WHERE booking_status = 'Pending' 
        AND (
            booking_date <= DATE_SUB(NOW(), INTERVAL 3 DAY) 
            OR start_date <= CURDATE()
        )
    ";
    
    if ($conn->query($sqlPending)) {
        $expiredCount += $conn->affected_rows;
    } else {
        throw new Exception("Error expiring pending bookings: " . $conn->error);
    }
    
    // 2. Expire APPROVED bookings (unpaid)
    // Rules: 3 days after approval date OR on the day of the start date
    $sqlApproved = "
        UPDATE bookings 
        SET booking_status = 'Expired' 
        WHERE booking_status = 'Approved' 
        AND (
            approval_date <= DATE_SUB(NOW(), INTERVAL 3 DAY) 
            OR start_date <= CURDATE()
        )
    ";
    
    if ($conn->query($sqlApproved)) {
        $expiredCount += $conn->affected_rows;
    } else {
        throw new Exception("Error expiring approved bookings: " . $conn->error);
    }
    
    // Commit transaction
    $conn->commit();
    
    echo json_encode(array(
        'success' => true,
        'message' => 'Expiration check complete',
        'expired_bookings_count' => $expiredCount
    ));
    
    error_log("check_expirations.php - Successfully expired $expiredCount bookings.");
    $conn->close();
    
} catch (Exception $e) {
    if (isset($conn)) {
        $conn->rollback();
        $conn->close();
    }
    error_log("Error in check_expirations.php: " . $e->getMessage());
    echo json_encode(array(
        'success' => false,
        'message' => 'Server error: ' . $e->getMessage()
    ));
}
?>
