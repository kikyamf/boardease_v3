<?php
// Manage Boarder Check-in/Check-out API - Handles check-in and check-out processes
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
    $action = isset($input['action']) ? trim($input['action']) : ''; // 'check_in' or 'check_out'
    $notes = isset($input['notes']) ? trim($input['notes']) : '';
    $deposit_returned = isset($input['deposit_returned']) ? boolval($input['action']) : false;
    $deposit_return_amount = isset($input['deposit_return_amount']) ? floatval($input['deposit_return_amount']) : 0;
    $room_condition = isset($input['room_condition']) ? trim($input['room_condition']) : 'Good';
    $updated_by = isset($input['updated_by']) ? intval($input['updated_by']) : 0;
    
    if ($booking_id <= 0) {
        throw new Exception("Invalid booking_id");
    }
    
    if (!in_array($action, ['check_in', 'check_out'])) {
        throw new Exception("Invalid action. Must be 'check_in' or 'check_out'");
    }
    
    $db = getDB();
    
    // Start transaction
    $db->beginTransaction();
    
    try {
        // First, get booking details
        $stmt = $db->prepare("
            SELECT 
                b.user_id as boarder_id,
                b.room_id,
                b.booking_status,
                b.check_in_date,
                b.check_out_date,
                CONCAT(r.f_name, ' ', r.l_name) as boarder_name,
                ru.room_number as room_name,
                bh.bh_name as boarding_house_name,
                bhr.room_price as amount
            FROM bookings b
            JOIN users u ON b.user_id = u.user_id
            JOIN registrations r ON u.reg_id = r.reg_id
            JOIN room_units ru ON b.room_id = ru.room_id
            JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
            JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
            WHERE b.booking_id = ?
        ");
        $stmt->execute([$booking_id]);
        $booking = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$booking) {
            throw new Exception("Booking not found");
        }
        
        $current_date = date('Y-m-d H:i:s');
        
        if ($action === 'check_in') {
            // Check if already checked in
            if ($booking['check_in_date']) {
                throw new Exception("Boarder is already checked in");
            }
            
            // Update booking with check-in information
            $stmt = $db->prepare("
                UPDATE bookings 
                SET check_in_date = ?,
                    booking_status = 'Active',
                    notes = CONCAT(COALESCE(notes, ''), '\nCheck-in: ', ?),
                    updated_at = NOW() 
                WHERE booking_id = ?
            ");
            $stmt->execute([$current_date, $notes, $booking_id]);
            
            // Update room status to Occupied
            $stmt = $db->prepare("
                UPDATE room_units 
                SET room_status = 'Occupied' 
                WHERE room_id = ?
            ");
            $stmt->execute([$booking['room_id']]);
            
            // Send notification to boarder
            $notification_result = AutoNotifyBooking::bookingApproved($booking['boarder_id'], [
                'room_name' => $booking['room_name'],
                'boarding_house_name' => $booking['boarding_house_name'],
                'booking_id' => $booking_id,
                'check_in_date' => date('Y-m-d', strtotime($current_date))
            ]);
            
            $message = "Boarder checked in successfully";
            
        } else { // check_out
            // Check if already checked out
            if ($booking['check_out_date']) {
                throw new Exception("Boarder is already checked out");
            }
            
            // Check if checked in
            if (!$booking['check_in_date']) {
                throw new Exception("Boarder must be checked in before checking out");
            }
            
            // Update booking with check-out information
            $stmt = $db->prepare("
                UPDATE bookings 
                SET check_out_date = ?,
                    booking_status = 'Completed',
                    completion_notes = ?,
                    deposit_returned = ?,
                    deposit_return_amount = ?,
                    room_condition = ?,
                    notes = CONCAT(COALESCE(notes, ''), '\nCheck-out: ', ?),
                    updated_at = NOW() 
                WHERE booking_id = ?
            ");
            $stmt->execute([
                $current_date, 
                $notes, 
                $deposit_returned, 
                $deposit_return_amount, 
                $room_condition, 
                $notes, 
                $booking_id
            ]);
            
            // Update room status to Available
            $stmt = $db->prepare("
                UPDATE room_units 
                SET room_status = 'Available' 
                WHERE room_id = ?
            ");
            $stmt->execute([$booking['room_id']]);
            
            // Send notification to boarder
            $notification_result = AutoNotifyBooking::bookingCancelled($booking['boarder_id'], [
                'room_name' => $booking['room_name'],
                'boarding_house_name' => $booking['boarding_house_name'],
                'booking_id' => $booking_id,
                'check_out_date' => date('Y-m-d', strtotime($current_date))
            ]);
            
            $message = "Boarder checked out successfully";
        }
        
        // Commit transaction
        $db->commit();
        
        $response = [
            'success' => true,
            'message' => $message,
            'data' => [
                'booking_id' => $booking_id,
                'action' => $action,
                'boarder_name' => $booking['boarder_name'],
                'room_name' => $booking['room_name'],
                'boarding_house_name' => $booking['boarding_house_name'],
                'action_date' => $current_date,
                'notes' => $notes,
                'deposit_returned' => $deposit_returned,
                'deposit_return_amount' => $deposit_return_amount,
                'room_condition' => $room_condition,
                'notification_sent' => $notification_result ? $notification_result['success'] : false
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





