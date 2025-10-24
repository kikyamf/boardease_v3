<?php
// Get Booking Details API - Returns detailed information for a specific booking
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type");

// Handle preflight requests
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

require_once 'db_helper.php';

$response = [];

try {
    // Get booking_id from request
    $booking_id = isset($_GET['booking_id']) ? intval($_GET['booking_id']) : 0;
    
    if ($booking_id <= 0) {
        throw new Exception("Invalid booking_id");
    }
    
    $db = getDB();
    
    // Get detailed booking information
    $sql = "SELECT 
                b.booking_id,
                b.user_id as boarder_id,
                CONCAT(r.f_name, ' ', r.l_name) as boarder_name,
                r.email as boarder_email,
                r.phone_number as boarder_phone,
                ru.room_number as room_name,
                bh.bh_name as boarding_house_name,
                bh.bh_address as boarding_house_address,
                bh.bh_contact as boarding_house_contact,
                b.start_date,
                b.end_date,
                b.booking_date,
                    b.booking_status as status,
                b.notes,
                u.profile_picture,
                ru.room_id,
                bh.bh_id as boarding_house_id,
                bhr.room_category as rent_type,
                bhr.room_price as amount,
                bhr.room_description,
                bhr.room_capacity,
                bhr.room_amenities,
                b.created_at,
                b.updated_at
            FROM bookings b
            JOIN users u ON b.user_id = u.user_id
            JOIN registrations r ON u.reg_id = r.reg_id
            JOIN room_units ru ON b.room_id = ru.room_id
            JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
            JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
            WHERE b.booking_id = ?";
    
    $stmt = $db->prepare($sql);
    $stmt->execute([$booking_id]);
    $row = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$row) {
        throw new Exception("Booking not found");
    }
    
    // Format amount
    $amount = "P" . number_format($row['amount'], 2);
    
    // Format dates
    $start_date = date('Y-m-d', strtotime($row['start_date']));
    $end_date = date('Y-m-d', strtotime($row['end_date']));
    $booking_date = date('Y-m-d', strtotime($row['booking_date']));
    $created_at = date('Y-m-d H:i:s', strtotime($row['created_at']));
    $updated_at = date('Y-m-d H:i:s', strtotime($row['updated_at']));
    
    // Calculate duration
    $start = new DateTime($row['start_date']);
    $end = new DateTime($row['end_date']);
    $duration = $start->diff($end)->days;
    
    $booking_details = [
        'booking_id' => intval($row['booking_id']),
        'boarder_id' => intval($row['boarder_id']),
        'boarder_name' => $row['boarder_name'],
        'boarder_email' => $row['boarder_email'],
        'boarder_phone' => $row['boarder_phone'],
        'room_name' => $row['room_name'],
        'room_description' => $row['room_description'] ?? '',
        'room_capacity' => intval($row['room_capacity']),
        'room_amenities' => $row['room_amenities'] ?? '',
        'boarding_house_name' => $row['boarding_house_name'],
        'boarding_house_address' => $row['boarding_house_address'],
        'boarding_house_contact' => $row['boarding_house_contact'] ?? '',
        'start_date' => $start_date,
        'end_date' => $end_date,
        'booking_date' => $booking_date,
        'duration_days' => $duration,
        'status' => $row['status'],
            'payment_status' => 'Pending', // Default value since payment_status field doesn't exist
        'notes' => $row['notes'] ?? '',
        'profile_image' => $row['profile_picture'] ?? '',
        'room_id' => intval($row['room_id']),
        'boarding_house_id' => intval($row['boarding_house_id']),
        'rent_type' => $row['rent_type'],
        'amount' => $amount,
        'created_at' => $created_at,
        'updated_at' => $updated_at
    ];
    
    $response = [
        'success' => true,
        'data' => [
            'booking_details' => $booking_details
        ]
    ];
    
} catch (Exception $e) {
    $response = [
        'success' => false,
        'error' => $e->getMessage()
    ];
}

echo json_encode($response, JSON_UNESCAPED_SLASHES);
?>

