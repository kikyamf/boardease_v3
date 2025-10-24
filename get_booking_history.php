<?php
// Get Booking History API - Returns completed/expired/cancelled bookings for a user
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
    // Get user_id from request
    $user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;
    $user_type = isset($_GET['user_type']) ? $_GET['user_type'] : 'owner'; // 'owner' or 'boarder'
    
    if ($user_id <= 0) {
        throw new Exception("Invalid user_id");
    }
    
    $db = getDB();
    
    if ($user_type === 'owner') {
        // Get booking history for owner
        $sql = "SELECT 
                    b.booking_id,
                    b.user_id as boarder_id,
                    CONCAT(r.f_name, ' ', r.l_name) as boarder_name,
                    r.email as boarder_email,
                    r.phone_number as boarder_phone,
                    ru.room_number as room_name,
                    bh.bh_name as boarding_house_name,
                    bh.bh_address as boarding_house_address,
                    b.start_date,
                    b.end_date,
                    b.booking_date,
                    b.booking_status as status,
                    b.notes,
                    u.profile_picture,
                    ru.room_id,
                    bh.bh_id as boarding_house_id,
                    bhr.room_category as rent_type,
                    bhr.room_price as amount
                FROM bookings b
                JOIN users u ON b.user_id = u.user_id
                JOIN registrations r ON u.reg_id = r.reg_id
                JOIN room_units ru ON b.room_id = ru.room_id
                JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
                JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
                WHERE bh.user_id = ? AND b.booking_status IN ('Completed', 'Expired', 'Cancelled', 'Declined')
                ORDER BY b.booking_date DESC";
        
        $stmt = $db->prepare($sql);
        $stmt->execute([$user_id]);
        
    } else {
        // Get booking history for boarder
        $sql = "SELECT 
                    b.booking_id,
                    b.user_id as boarder_id,
                    CONCAT(r.f_name, ' ', r.l_name) as boarder_name,
                    r.email as boarder_email,
                    r.phone_number as boarder_phone,
                    ru.room_number as room_name,
                    bh.bh_name as boarding_house_name,
                    bh.bh_address as boarding_house_address,
                    b.start_date,
                    b.end_date,
                    b.booking_date,
                    b.booking_status as status,
                    b.notes,
                    u.profile_picture,
                    ru.room_id,
                    bh.bh_id as boarding_house_id,
                    bhr.room_category as rent_type,
                    bhr.room_price as amount
                FROM bookings b
                JOIN users u ON b.user_id = u.user_id
                JOIN registrations r ON u.reg_id = r.reg_id
                JOIN room_units ru ON b.room_id = ru.room_id
                JOIN boarding_house_rooms bhr ON ru.bhr_id = bhr.bhr_id
                JOIN boarding_houses bh ON bhr.bh_id = bh.bh_id
                WHERE b.user_id = ? AND b.booking_status IN ('Completed', 'Expired', 'Cancelled', 'Declined')
                ORDER BY b.booking_date DESC";
        
        $stmt = $db->prepare($sql);
        $stmt->execute([$user_id]);
    }
    
    $bookings = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        // Format amount
        $amount = "P" . number_format($row['amount'], 2);
        
        // Format dates
        $start_date = date('Y-m-d', strtotime($row['start_date']));
        $end_date = date('Y-m-d', strtotime($row['end_date']));
        $booking_date = date('Y-m-d', strtotime($row['booking_date']));
        
        $bookings[] = [
            'booking_id' => intval($row['booking_id']),
            'boarder_id' => intval($row['boarder_id']),
            'boarder_name' => $row['boarder_name'],
            'boarder_email' => $row['boarder_email'],
            'boarder_phone' => $row['boarder_phone'],
            'room_name' => $row['room_name'],
            'boarding_house_name' => $row['boarding_house_name'],
            'boarding_house_address' => $row['boarding_house_address'],
            'start_date' => $start_date,
            'end_date' => $end_date,
            'booking_date' => $booking_date,
            'status' => $row['status'],
            'payment_status' => 'Pending', // Default value since payment_status field doesn't exist
            'notes' => $row['notes'] ?? '',
            'profile_image' => $row['profile_picture'] ?? '',
            'room_id' => intval($row['room_id']),
            'boarding_house_id' => intval($row['boarding_house_id']),
            'rent_type' => $row['rent_type'],
            'amount' => $amount
        ];
    }
    
    $response = [
        'success' => true,
        'data' => [
            'booking_history' => $bookings,
            'total_count' => count($bookings)
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

