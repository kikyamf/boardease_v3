<?php
// Get Boarder Details API - Returns detailed information for a specific boarder
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
    
    // Get detailed boarder information
    $sql = "SELECT 
                b.booking_id,
                b.user_id as boarder_id,
                CONCAT(r.f_name, ' ', r.l_name) as boarder_name,
                r.email as boarder_email,
                r.phone_number as boarder_phone,
                r.university,
                r.student_id,
                r.birth_date,
                r.gender,
                r.address,
                r.emergency_contact_name,
                r.emergency_contact_phone,
                r.emergency_contact_relationship,
                ru.room_number as room_name,
                ru.room_id,
                bh.bh_name as boarding_house_name,
                bh.bh_address as boarding_house_address,
                bh.bh_contact as boarding_house_contact,
                b.start_date,
                b.end_date,
                b.booking_date,
                b.booking_status as status,
                b.payment_status,
                b.notes,
                u.profile_picture,
                bh.bh_id as boarding_house_id,
                bhr.room_category as rent_type,
                bhr.room_price as amount,
                bhr.room_description,
                bhr.room_capacity,
                bhr.room_amenities,
                b.payment_due_date,
                b.payment_date,
                b.payment_method,
                b.payment_reference,
                b.payment_notes,
                b.check_in_date,
                b.check_out_date,
                b.rental_agreement_signed,
                b.deposit_amount,
                b.deposit_status,
                b.deposit_returned,
                b.deposit_return_amount,
                b.room_condition,
                b.cancellation_reason,
                b.completion_notes,
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
        throw new Exception("Boarder not found");
    }
    
    // Format amount
    $amount = "P" . number_format($row['amount'], 2);
    $deposit_amount = $row['deposit_amount'] ? "P" . number_format($row['deposit_amount'], 2) : null;
    $deposit_return_amount = $row['deposit_return_amount'] ? "P" . number_format($row['deposit_return_amount'], 2) : null;
    
    // Format dates
    $start_date = date('Y-m-d', strtotime($row['start_date']));
    $end_date = date('Y-m-d', strtotime($row['end_date']));
    $booking_date = date('Y-m-d', strtotime($row['booking_date']));
    $birth_date = $row['birth_date'] ? date('Y-m-d', strtotime($row['birth_date'])) : null;
    $payment_due_date = $row['payment_due_date'] ? date('Y-m-d', strtotime($row['payment_due_date'])) : null;
    $payment_date = $row['payment_date'] ? date('Y-m-d', strtotime($row['payment_date'])) : null;
    $check_in_date = $row['check_in_date'] ? date('Y-m-d H:i:s', strtotime($row['check_in_date'])) : null;
    $check_out_date = $row['check_out_date'] ? date('Y-m-d H:i:s', strtotime($row['check_out_date'])) : null;
    $created_at = date('Y-m-d H:i:s', strtotime($row['created_at']));
    $updated_at = date('Y-m-d H:i:s', strtotime($row['updated_at']));
    
    // Calculate rental duration and progress
    $start = new DateTime($row['start_date']);
    $end = new DateTime($row['end_date']);
    $today = new DateTime();
    
    $total_days = $start->diff($end)->days;
    $days_remaining = $today < $end ? $today->diff($end)->days : 0;
    $days_stayed = $today > $start ? $start->diff($today)->days : 0;
    
    // Calculate rental progress percentage
    $rental_progress = $total_days > 0 ? ($days_stayed / $total_days) * 100 : 0;
    
    // Calculate age
    $age = null;
    if ($birth_date) {
        $birth = new DateTime($birth_date);
        $age = $birth->diff($today)->y;
    }
    
    // Determine boarder status
    $boarder_status = 'active';
    if ($row['payment_status'] === 'Overdue') {
        $boarder_status = 'payment_overdue';
    } elseif ($days_remaining <= 7 && $days_remaining > 0) {
        $boarder_status = 'checking_out_soon';
    } elseif ($days_remaining <= 0) {
        $boarder_status = 'overdue_checkout';
    } elseif ($row['status'] === 'Completed') {
        $boarder_status = 'completed';
    } elseif ($row['status'] === 'Cancelled') {
        $boarder_status = 'cancelled';
    }
    
    $boarder_details = [
        'booking_id' => intval($row['booking_id']),
        'boarder_id' => intval($row['boarder_id']),
        'boarder_name' => $row['boarder_name'],
        'boarder_email' => $row['boarder_email'],
        'boarder_phone' => $row['boarder_phone'],
        'university' => $row['university'] ?? '',
        'student_id' => $row['student_id'] ?? '',
        'birth_date' => $birth_date,
        'age' => $age,
        'gender' => $row['gender'] ?? '',
        'address' => $row['address'] ?? '',
        'emergency_contact_name' => $row['emergency_contact_name'] ?? '',
        'emergency_contact_phone' => $row['emergency_contact_phone'] ?? '',
        'emergency_contact_relationship' => $row['emergency_contact_relationship'] ?? '',
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
        'status' => $row['status'],
        'payment_status' => $row['payment_status'] ?? 'Pending',
        'notes' => $row['notes'] ?? '',
        'profile_image' => $row['profile_picture'] ?? '',
        'room_id' => intval($row['room_id']),
        'boarding_house_id' => intval($row['boarding_house_id']),
        'rent_type' => $row['rent_type'],
        'amount' => $amount,
        'payment_due_date' => $payment_due_date,
        'payment_date' => $payment_date,
        'payment_method' => $row['payment_method'] ?? '',
        'payment_reference' => $row['payment_reference'] ?? '',
        'payment_notes' => $row['payment_notes'] ?? '',
        'check_in_date' => $check_in_date,
        'check_out_date' => $check_out_date,
        'rental_agreement_signed' => $row['rental_agreement_signed'] ?? false,
        'deposit_amount' => $deposit_amount,
        'deposit_status' => $row['deposit_status'] ?? 'Pending',
        'deposit_returned' => $row['deposit_returned'] ?? false,
        'deposit_return_amount' => $deposit_return_amount,
        'room_condition' => $row['room_condition'] ?? '',
        'cancellation_reason' => $row['cancellation_reason'] ?? '',
        'completion_notes' => $row['completion_notes'] ?? '',
        'created_at' => $created_at,
        'updated_at' => $updated_at,
        'rental_info' => [
            'total_days' => $total_days,
            'days_remaining' => $days_remaining,
            'days_stayed' => $days_stayed,
            'rental_progress' => round($rental_progress, 2),
            'boarder_status' => $boarder_status
        ]
    ];
    
    $response = [
        'success' => true,
        'data' => [
            'boarder_details' => $boarder_details
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





