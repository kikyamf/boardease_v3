<?php
// Decline Booking API - Declines a pending booking request
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type");

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

require_once 'db_helper.php';
require_once 'auto_notify_booking.php';

$response = [];

try {
    // Get JSON input
    $input = json_decode(file_get_contents('php://input'), true);
    
    // Get parameters
    $booking_id = isset($input['booking_id']) ? intval($input['booking_id']) : 0;
    $owner_id = isset($input['owner_id']) ? intval($input['owner_id']) : 0;
    $reason = isset($input['reason']) ? trim($input['reason']) : 'No reason provided';
    
    if ($booking_id <= 0 || $owner_id <= 0) {
        throw new Exception("Invalid booking_id or owner_id");
    }
    
    $db = getDB();
    
    // Start transaction
    $db->beginTransaction();
    
    try {
        // First, get booking details for notification
        $stmt = $db->prepare("
            SELECT 
                b.user_id as boarder_id,
                b.room_id,
                CONCAT(r.f_name, ' ', r.l_name) as boarder_name,
                ru.room_number as room_name,
                bh.bh_name as boarding_house_name
            FROM bookings b
            JOIN users u ON b.user_id = u.user_id
            JOIN registrations r ON u.reg_id = r.reg_id
            JOIN room_units ru ON b.room_id = ru.room_id
            JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
            JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
            WHERE b.booking_id = ? AND b.booking_status = 'Pending'
        ");
        $stmt->execute([$booking_id]);
        $booking = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$booking) {
            throw new Exception("Booking not found or already processed");
        }
        
        // Update booking status to Declined
        $stmt = $db->prepare("
            UPDATE bookings 
            SET booking_status = 'Declined', 
                notes = CONCAT(COALESCE(notes, ''), '\nDecline reason: ', ?),
                updated_at = NOW() 
            WHERE booking_id = ?
        ");
        $stmt->execute([$reason, $booking_id]);
        
        // Update room status back to Available
        $stmt = $db->prepare("
            UPDATE room_units 
            SET room_status = 'Available' 
            WHERE room_id = ?
        ");
        $stmt->execute([$booking['room_id']]);
        
        // Commit transaction
        $db->commit();
        
        // Send notification to boarder
        $notification_result = AutoNotifyBooking::bookingRejected($booking['boarder_id'], [
            'room_name' => $booking['room_name'],
            'boarding_house_name' => $booking['boarding_house_name'],
            'booking_id' => $booking_id,
            'reason' => $reason
        ]);
        
        $response = [
            'success' => true,
            'message' => 'Booking declined successfully',
            'data' => [
                'booking_id' => $booking_id,
                'status' => 'Declined',
                'boarder_name' => $booking['boarder_name'],
                'room_name' => $booking['room_name'],
                'reason' => $reason,
                'notification_sent' => $notification_result['success'] ?? false
            ]
        ];
        
    } catch (Exception $e) {
        // Rollback transaction on error
        $db->rollback();
        throw $e;
    }
    
} catch (Exception $e) {
    $response = [
        'success' => false,
        'error' => $e->getMessage()
    ];
}

echo json_encode($response, JSON_UNESCAPED_SLASHES);
?>





